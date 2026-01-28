#!/bin/bash

# Camel Catalog MCP Server OpenShift Deployment Script
set -e

echo "Deploying Camel Catalog MCP Server to OpenShift..."

# Apply all manifests in order
echo "Deploying Camel Catalog MCP Server..."
oc apply -f camel-catalog-deployment.yaml

echo "Waiting for Camel Catalog MCP Server to be ready..."
oc wait --for=condition=ready pod -l app=camel-catalog-mcp --timeout=300s

echo "Deployment complete!"
echo ""
echo "Access Camel Catalog MCP Server at:"
echo "- Internal: http://camel-catalog-mcp.<namespace>.svc.cluster.local:8010"
echo "- External: http://$(oc get route camel-catalog-mcp -o jsonpath='{.spec.host}')"
echo ""
echo "To check status:"
echo "oc get pods"
echo "oc get services"
echo "oc get routes"