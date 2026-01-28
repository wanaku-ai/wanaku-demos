#!/bin/bash

# Keycloak OpenShift Deployment Script
set -e

echo "Deploying Keycloak to OpenShift..."

# Apply all manifests
echo "Deploying Keycloak..."
oc apply -f keycloak.yaml

echo "Waiting for Keycloak to be ready..."
oc wait --for=condition=ready pod -l app=keycloak --timeout=300s

echo "Deployment complete!"
echo ""
echo "Access Keycloak at:"
echo "- Internal: http://keycloak.<namespace>.svc.cluster.local:8080"
echo "- External: http://$(oc get route keycloak -o jsonpath='{.spec.host}')"
echo ""
echo "To check status:"
echo "oc get pods"
echo "oc get services"
echo "oc get routes"