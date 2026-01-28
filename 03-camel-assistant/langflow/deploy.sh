#!/bin/bash

# Langflow OpenShift Deployment Script
set -e

echo "Deploying Langflow to OpenShift..."

# Apply all manifests in order
echo "Creating persistent volume claims..."
oc apply -f persistent-volumes.yaml

echo "Creating secrets..."
oc apply -f secrets.yaml

echo "Deploying PostgreSQL..."
oc apply -f postgres-deployment.yaml

echo "Waiting for PostgreSQL to be ready..."
oc wait --for=condition=ready pod -l app=postgres --timeout=300s

echo "Deploying Langflow..."
oc apply -f langflow-deployment.yaml
oc apply -f langflow-service.yaml

echo "Waiting for Langflow to be ready..."
oc wait --for=condition=ready pod -l app=langflow --timeout=300s

echo "Deployment complete!"
echo ""
echo "Access Langflow at:"
echo "- Internal: http://langflow.<namespace>.svc.cluster.local:7860"
echo "- External: http://$(oc get route langflow -o jsonpath='{.spec.host}')"
echo ""
echo "Default credentials:"
echo "- Username: admin"
echo "- Password: changeme"
echo ""
echo "To check status:"
echo "oc get pods"
echo "oc get services"
echo "oc get routes"