#!/bin/bash
# Start a local Kafka instance for testing

set -e

echo "Starting local Kafka instance..."

# Check if Docker is available
if ! command -v docker &> /dev/null; then
    echo "Error: Docker not found. Please install Docker first."
    exit 1
fi

# Check if Kafka container already exists
if docker ps -a --format '{{.Names}}' | grep -q '^kafka-demo$'; then
    echo "Kafka container 'kafka-demo' already exists."
    echo "Starting existing container..."
    docker start kafka-demo
else
    echo "Creating new Kafka container..."
    docker run -d --name kafka-demo \
      -p 9092:9092 \
      -e KAFKA_NODE_ID=1 \
      -e KAFKA_PROCESS_ROLES=broker,controller \
      -e KAFKA_LISTENERS=PLAINTEXT://:9092,CONTROLLER://:9093 \
      -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092 \
      -e KAFKA_CONTROLLER_LISTENER_NAMES=CONTROLLER \
      -e KAFKA_LISTENER_SECURITY_PROTOCOL_MAP=CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT \
      -e KAFKA_CONTROLLER_QUORUM_VOTERS=1@localhost:9093 \
      -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 \
      -e KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR=1 \
      -e KAFKA_TRANSACTION_STATE_LOG_MIN_ISR=1 \
      -e KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS=0 \
      -e KAFKA_NUM_PARTITIONS=1 \
      apache/kafka:latest
fi

echo ""
echo "Kafka started successfully!"
echo ""
echo "Kafka is available at: localhost:9092"
echo ""
echo "To stop Kafka, run:"
echo "  docker stop kafka-demo"
echo ""
echo "To remove the Kafka container, run:"
echo "  docker stop kafka-demo && docker rm kafka-demo"
