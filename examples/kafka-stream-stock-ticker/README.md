# Kafka Stream Stock Ticker with Apache Flink

A real-time trade processing application built with Apache Flink that consumes stock trade events from Kafka and processes them into enriched trade data.

## Overview

This application demonstrates how to:
- Consume trade events from a Kafka topic
- Process and enrich trade data in real-time using Apache Flink
- Write processed trades to files organized by date and symbol
- Handle checkpointing for fault tolerance

## Architecture

The application consists of:
- Kafka source for consuming trade events
- Flink processing pipeline for data enrichment
- File sink with dynamic partitioning by date and symbol

## Prerequisites

- Java 17 or later
- Apache Flink 1.20.1
- Apache Kafka
- Maven

## Project Structure

- `TradeProcessor.java`: Main Flink application
- `TradeEvent.java`: POJO for incoming trade events
- `EnrichedTrade.java`: Enriched trade data with additional calculations

## Features

- Real-time trade processing
- Trade enrichment with:
  - Total value calculation
  - Trading session determination (MORNING/AFTERNOON/AFTER_HOURS)
- Fault-tolerant processing with checkpointing
- Scalable processing with parallelism support
- CSV output with dynamic partitioning

## Configuration

The application uses the following configurations:
- Kafka bootstrap servers: `kafka:29092`
- Topic: `trade-events`
- Consumer group: `trade-processor`
- Parallelism: 3
- Checkpointing interval: 60 seconds
- Output path: `/usr/local/flink/resources/output`

## Output Format

Processed trades are written as CSV files with the following format:

```
symbol,price,quantity,tradeId,timestamp,totalValue,tradingSession
```

Files are organized in directories by date and symbol:

```
output/
└── YYYY-MM-DD/
    └── SYMBOL/
        └── trades_.csv
```

## Development Environment

The project includes a complete development environment using DevContainers with:
- Apache Flink JobManager and TaskManager
- Kafka and Kafka UI
- Java development tools
- VS Code configurations

## Building and Running

1. Build the project:

```bash
mvn clean install
```

2. Submit to Flink:

```bash
flink run -m jobmanager:8081 target/flink-trade-processor-1.0-SNAPSHOT.jar
```

3. Go to Kafka UI @ http://localhost:8080. Submit a trade

```json
{"symbol":"AAPL","price":152.30,"quantity":150,"tradeId":"AAPL_3","timestamp":1646441460000}
```

4. Check the output in the `output` directory


## Monitoring

- Flink Dashboard: http://localhost:8081
- Kafka UI: http://localhost:8080
