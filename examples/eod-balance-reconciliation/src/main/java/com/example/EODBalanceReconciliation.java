package com.example;

import com.example.model.Transaction;
import com.example.model.EnrichedTransaction;
import com.example.model.ReconcileReport;
import com.example.model.DailyReport;
import com.example.service.EnrichmentService;

import java.time.Duration;

import org.apache.flink.api.common.RuntimeExecutionMode;
import org.apache.flink.api.common.eventtime.SerializableTimestampAssigner;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.AggregateFunction;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.functions.ReduceFunction;
import org.apache.flink.api.common.serialization.SimpleStringEncoder;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.connector.file.src.FileSource;
import org.apache.flink.connector.file.src.reader.TextLineInputFormat;
import org.apache.flink.connector.file.sink.FileSink;
import org.apache.flink.core.fs.Path;
import org.apache.flink.core.io.SimpleVersionedSerializer;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.filesystem.BucketAssigner;
import org.apache.flink.streaming.api.functions.sink.filesystem.OutputFileConfig;
import org.apache.flink.streaming.api.functions.sink.filesystem.bucketassigners.SimpleVersionedStringSerializer;
import org.apache.flink.streaming.api.functions.sink.filesystem.rollingpolicies.OnCheckpointRollingPolicy;
import org.apache.flink.streaming.api.windowing.assigners.ProcessingTimeSessionWindows;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EODBalanceReconciliation {
    private static final Logger LOG = LoggerFactory.getLogger(EODBalanceReconciliation.class);

    public static void main(String[] args) throws Exception {
        // Set up the execution environment
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        // Enable checkpointing to ensure all data is processed before job completion
        // env.enableCheckpointing(60 * 1000);
        env.setParallelism(3);
        env.setRuntimeMode(RuntimeExecutionMode.BATCH);
        // Define input and output paths
        Path inputPath = new Path("/usr/local/flink/eod-balance-reconciliation/resources/input");
        Path outputPath = new Path("/usr/local/flink/eod-balance-reconciliation/resources/output");
        
        // Create a file source to monitor the input directory
        FileSource<String> source = FileSource
                .forRecordStreamFormat(new TextLineInputFormat(), inputPath)
                // .monitorContinuously(Duration.ofMinutes(1))
                .build();
        
        // Create the data stream from the file source
        DataStream<String> inputStream = env.fromSource(
                source,
                WatermarkStrategy.noWatermarks(),
                "File Source"
        ).uid("file-source");
        
        // Extract date from filename and parse CSV data
        DataStream<Transaction> transactions = inputStream
                .filter(line -> !line.startsWith("transaction_id"))
                .uid("filter-transaction-id") // Skip header
                .map(new MapFunction<String, Transaction>() {
                    @Override
                    public Transaction map(String line) throws Exception {
                        // Parse CSV line into Transaction object
                        String[] fields = line.split(",");
                        if (fields.length < 5) {
                            throw new Exception("Invalid CSV format: " + line);
                        }
                        
                        return new Transaction(
                                fields[0].trim(),                      // transaction_id
                                fields[1].trim(),                      // account_id
                                fields[2].trim(),                      // line_item
                                Double.parseDouble(fields[3].trim()),  // amount
                                fields[4].trim()                       // transaction_date
                        );
                    }
                })
                .uid("map-to-transaction");

        // Assign timestamps and watermarks based on transaction date
        DataStream<Transaction> timestampedTransactions = transactions
                .assignTimestampsAndWatermarks(
                    WatermarkStrategy
                        .<Transaction>forBoundedOutOfOrderness(Duration.ofDays(1))
                        .withTimestampAssigner(new SerializableTimestampAssigner<Transaction>() {
                            @Override
                            public long extractTimestamp(Transaction transaction, long recordTimestamp) {
                                // Convert transaction date (YYYY-MM-DD) to epoch milliseconds
                                try {
                                    LocalDate date = LocalDate.parse(transaction.getTransactionDate());
                                    long timestamp = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
                                    LOG.info("Extracted timestamp for transaction: " + transaction.getTransactionId() + " is " + timestamp);
                                    return timestamp;
                                } catch (Exception e) {
                                    // Fallback to current time if date parsing fails
                                    System.err.println("Error parsing date: " + transaction.getTransactionDate());
                                    return Instant.now().toEpochMilli();
                                }
                            }
                        })
                ).uid("assign-timestamps-and-watermarks");

        // Enrich transactions with additional data from external API
        EnrichmentService enrichmentService = new EnrichmentService();
        DataStream<EnrichedTransaction> enrichedTransactions = timestampedTransactions
                .map(new MapFunction<Transaction, EnrichedTransaction>() {
                    @Override
                    public EnrichedTransaction map(Transaction transaction) throws Exception {
                        return enrichmentService.enrichTransaction(transaction);
                    }
                })
                .uid("enrich-transaction");
        
        // Group by line item within each day and reduce
        DataStream<DailyReport> dailyReports = enrichedTransactions
                .keyBy(new KeySelector<EnrichedTransaction, String>() {
                    @Override
                    public String getKey(EnrichedTransaction transaction) throws Exception {
                        // Key by both date and line item to ensure proper grouping
                        return transaction.getTransactionDate() + "-" + transaction.getLineItem();
                    }
                })
                .reduce(new ReduceFunction<EnrichedTransaction>() {
                    @Override
                    public EnrichedTransaction reduce(EnrichedTransaction t1, EnrichedTransaction t2) throws Exception {
                        // Combine transactions with the same line item and date
                        return new EnrichedTransaction(
                                "AGGREGATED",
                                "AGGREGATED",
                                t1.getLineItem(),
                                t1.getAmount() + t2.getAmount(),
                                t1.getTransactionDate(),
                                t1.getDepartment(),
                                t1.getCategory()
                        );
                    }
                })
                .uid("reduce-enriched-transaction")
                .map(new MapFunction<EnrichedTransaction, DailyReport>() {
                    @Override
                    public DailyReport map(EnrichedTransaction transaction) throws Exception {
                        return new DailyReport(
                                transaction.getLineItem(),
                                transaction.getAmount(),
                                transaction.getDepartment(),
                                transaction.getCategory(),
                                transaction.getTransactionDate()
                        );
                    }
                })
                .uid("map-to-daily-report");
        
        // Group by date and aggregate into a single ReconcileReport
        SingleOutputStreamOperator<ReconcileReport> reconcileReports = dailyReports
                .keyBy(DailyReport::getReportDate)
                .window(ProcessingTimeSessionWindows.withGap(Duration.ofMinutes(1)))
                .aggregate(new AggregateFunction<DailyReport, ReconcileReport, ReconcileReport>() {
                    @Override
                    public ReconcileReport createAccumulator() {
                        return new ReconcileReport();
                    }
        
                    @Override
                    public ReconcileReport add(DailyReport value, ReconcileReport accumulator) {
                        accumulator.addDailyReport(value);
                        return accumulator;
                    }
        
                    @Override
                    public ReconcileReport getResult(ReconcileReport accumulator) {
                        return accumulator;
                    }
        
                    @Override
                    public ReconcileReport merge(ReconcileReport a, ReconcileReport b) {
                        a.getDailyReports().addAll(b.getDailyReports());
                        return a;
                    }
                })
                .uid("aggregate-reconcile-report");

        // Convert ReconcileReport to String
        SingleOutputStreamOperator<String> csvReports = reconcileReports
                .map(ReconcileReport::toCsv)
                .uid("map-to-csv");
        
        // Configure the file sink with a custom bucket assigner
        FileSink<String> sink = FileSink
                .forRowFormat(outputPath, new SimpleStringEncoder<String>("UTF-8"))
                .withBucketAssigner(new BucketAssigner<String, String>() {
                    @Override
                    public String getBucketId(String element, Context context) {
                        // Extract the date from the CSV line
                        String[] fields = element.split("\n")[1].split(",");
                        if (fields.length >= 5) {
                            String dateStr = fields[4].trim(); // Assuming the date is in the 5th column
                            // Convert date format from YYYY-MM-DD to YYYY/MM/DD or any desired format
                            return dateStr.replace("-", "/");
                        }
                        return "unknown";
                    }

                    @Override
                    public SimpleVersionedSerializer<String> getSerializer() {
                        return SimpleVersionedStringSerializer.INSTANCE;
                    }
                })
                .withOutputFileConfig(
                    OutputFileConfig.builder()
                        .withPartPrefix("reconcile_report")
                        .withPartSuffix(".csv")
                        .build()
                )
                .withRollingPolicy(OnCheckpointRollingPolicy.build())
                .build();
        
        // Write to sink
        csvReports.sinkTo(sink).uid("sink-file");
        
        // Execute the job
        env.execute("EOD Balance Reconciliation");
    }
} 