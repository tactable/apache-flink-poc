package com.example;

import com.example.model.TradeEvent;
import com.example.model.EnrichedTrade;

import java.text.SimpleDateFormat;
// import java.time.Duration;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.SimpleStringEncoder;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
// import org.apache.flink.configuration.MemorySize;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.connector.file.sink.FileSink;
import org.apache.flink.core.fs.Path;
import org.apache.flink.core.io.SimpleVersionedSerializer;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.filesystem.BucketAssigner;
import org.apache.flink.streaming.api.functions.sink.filesystem.OutputFileConfig;
import org.apache.flink.streaming.api.functions.sink.filesystem.bucketassigners.SimpleVersionedStringSerializer;
// import org.apache.flink.streaming.api.functions.sink.filesystem.rollingpolicies.DefaultRollingPolicy;
import org.apache.flink.streaming.api.functions.sink.filesystem.rollingpolicies.OnCheckpointRollingPolicy;

import com.fasterxml.jackson.databind.ObjectMapper;

public class TradeProcessor {
    public static void main(String[] args) throws Exception {
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        env.setParallelism(3);
        // Checkpointing every minute
        env.enableCheckpointing(60 * 1000);
        // Create Kafka source
        KafkaSource<String> source = KafkaSource.<String>builder()
                .setBootstrapServers("kafka:29092")
                .setTopics("trade-events")
                .setGroupId("trade-processor")
                .setStartingOffsets(OffsetsInitializer.committedOffsets())
                .setValueOnlyDeserializer(new SimpleStringSchema())
                // Add message size configurations
                .setProperty("max.partition.fetch.bytes", "10485760")  // 10MB
                .setProperty("fetch.message.max.bytes", "10485760")    // 10MB
                .setProperty("message.max.bytes", "10485760")          // 10MB
                // Other properties
                .setProperty("enable.auto.commit", "true")
                .setProperty("auto.commit.interval.ms", "1000")
                .build();

        // Create output directory if it doesn't exist
        Path outputPath = new Path("/usr/local/flink/kafka-stream-stock-ticker/resources/output");
        // outputPath.getFileSystem().mkdirs(outputPath);

        // Configure FileSink with rolling policy
        FileSink<String> sink = FileSink
                        .forRowFormat(outputPath, new SimpleStringEncoder<String>("UTF-8"))
                        .withBucketAssigner(new BucketAssigner<String, String>() {
                            @Override
                            public String getBucketId(String element, Context context) {
                                String symbol = element.split(",")[0];
                                long timestamp = context.currentProcessingTime();
                                String date = new SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date(timestamp));
                                return date + "/" + symbol;
                            }

                            @Override
                            public SimpleVersionedSerializer<String> getSerializer() {
                                return SimpleVersionedStringSerializer.INSTANCE;
                            }
                        })
                        // .withRollingPolicy(
                        //     DefaultRollingPolicy.builder()
                        //         .withRolloverInterval(Duration.ofMinutes(15))
                        //         .withInactivityInterval(Duration.ofMinutes(5))
                        //         .withMaxPartSize(MemorySize.ofMebiBytes(1024))
                        //         .build())
                        .withRollingPolicy(
                            // In Streaming mode, the rolling policy is triggered by the checkpoint
                            OnCheckpointRollingPolicy.build()
                        )
                        .withOutputFileConfig(
                            OutputFileConfig
                            .builder()
                            .withPartPrefix("trades")
                            .withPartSuffix(".csv")
                            .build()
                        )
                        .build();
    

        // Process the stream
        DataStream<String> stream = env.fromSource(source, WatermarkStrategy.noWatermarks(), "Kafka Source");
        
        ObjectMapper mapper = new ObjectMapper();

        stream.map(json -> {
                System.out.println("Processing trade: " + json);
                return mapper.readValue(json, TradeEvent.class);
            })
            .keyBy(trade -> trade.getSymbol())  
            .map(trade -> {
                EnrichedTrade enrichedTrade = new EnrichedTrade(trade);
                System.out.println("Enriched trade: " + enrichedTrade);
                return enrichedTrade;
            })
            .map(Object::toString)
            .sinkTo(sink);

        // Execute the job
        env.execute("Trade Processor");
    }
} 