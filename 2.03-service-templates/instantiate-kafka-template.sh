#!/bin/bash
# Instantiate the built-in Kafka template with example parameters

set -e

KAFKA_BROKERS="${KAFKA_BROKERS:-localhost:9092}"
REQUEST_TOPIC="${REQUEST_TOPIC:-ai.requests}"
RESPONSE_TOPIC="${RESPONSE_TOPIC:-ai.responses}"
TIMEOUT_MS="${TIMEOUT_MS:-30000}"
GROUP_ID="${GROUP_ID:-wanaku-demo-group}"

echo "Instantiating Kafka template with the following parameters:"
echo "  Brokers: ${KAFKA_BROKERS}"
echo "  Request Topic: ${REQUEST_TOPIC}"
echo "  Response Topic: ${RESPONSE_TOPIC}"
echo "  Timeout: ${TIMEOUT_MS}ms"
echo "  Consumer Group: ${GROUP_ID}"
echo ""

# Check if wanaku CLI is available
if ! command -v wanaku &> /dev/null; then
    echo "Error: wanaku CLI not found. Please install it from:"
    echo "https://github.com/wanaku-ai/wanaku/releases"
    exit 1
fi

# Instantiate the template
wanaku service template instantiate \
  --name kafka-demo \
  --property kafka.brokers="${KAFKA_BROKERS}" \
  --property kafka.request.topic="${REQUEST_TOPIC}" \
  --property kafka.response.topic="${RESPONSE_TOPIC}" \
  --property kafka.reply.timeout-ms="${TIMEOUT_MS}" \
  --property kafka.response.group-id="${GROUP_ID}"

echo ""
echo "Kafka template instantiated successfully!"
echo ""
echo "To verify, run:"
echo "  wanaku tools list | grep kafka"
