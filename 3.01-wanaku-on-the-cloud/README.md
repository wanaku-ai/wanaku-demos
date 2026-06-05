# Wanaku on the Cloud

This guide walks you through deploying Wanaku on OpenShift (or Kubernetes) in a production-like configuration. By the end, you will have:

- **Keycloak** providing OIDC authentication
- The **Wanaku Operator** managing custom resources
- A **Wanaku Router** serving as the MCP entry point
- An **HTTP Capability** ready to handle HTTP-based tools

```
┌──────────────────────────────┐
│     Keycloak (OIDC)          │
│     :8080 / Route            │
└──────────────┬───────────────┘
               │
     ┌─────────▼──────────┐
     │  Wanaku Operator   │
     │  (manages CRs)     │
     └─────────┬──────────┘
               │
    ┌──────────▼──────────┐
    │   Wanaku Router     │
    │   (MCP Server)      │
    └──────────┬──────────┘
               │
    ┌──────────▼──────────┐
    │   HTTP Capability   │
    │   (tool service)    │
    └─────────────────────┘
```

## What You Will Need

- `oc` (or `kubectl`) CLI, logged into your cluster
- `helm` v3+
- `jq`
- The [Wanaku CLI](https://github.com/wanaku-ai/wanaku/releases) installed and in your `PATH`

## Step 1: Create a Namespace

Create a dedicated namespace for the demo:

```shell
oc new-project wanaku-demo
```

Or, if the namespace already exists:

```shell
oc project wanaku-demo
```

## Step 2: Deploy Keycloak

Keycloak provides OIDC authentication for the Wanaku Router and its capabilities.

Create a file called `keycloak.yaml` with the following content:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  labels:
    app: keycloak
  name: keycloak
spec:
  replicas: 1
  selector:
    matchLabels:
      app: keycloak
  template:
    metadata:
      labels:
        app: keycloak
    spec:
      containers:
        - name: keycloak
          image: quay.io/keycloak/keycloak:26.6.1
          imagePullPolicy: Always
          ports:
            - containerPort: 8080
              protocol: TCP
          env:
            - name: KC_BOOTSTRAP_ADMIN_USERNAME
              value: "admin"
            - name: KC_BOOTSTRAP_ADMIN_PASSWORD
              value: "admin"
          args:
            - "start-dev"
          volumeMounts:
            - name: keycloak-data
              mountPath: /opt/keycloak/data
      volumes:
        - name: keycloak-data
          persistentVolumeClaim:
            claimName: keycloak-data-pvc
---
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: keycloak-data-pvc
  labels:
    app: keycloak
spec:
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 1Gi
---
apiVersion: v1
kind: Service
metadata:
  labels:
    app: keycloak
  name: keycloak
spec:
  ports:
    - name: 8080-tcp
      protocol: TCP
      port: 8080
      targetPort: 8080
  selector:
    app: keycloak
  sessionAffinity: None
  type: ClusterIP
---
apiVersion: route.openshift.io/v1
kind: Route
metadata:
  labels:
    app: keycloak
  name: keycloak
spec:
  port:
    targetPort: 8080-tcp
  to:
    kind: Service
    name: keycloak
    weight: 100
  wildcardPolicy: None
```

Apply it:

```shell
oc apply -f keycloak.yaml
```

This deploys Keycloak with a persistent volume, a `ClusterIP` service, and an OpenShift `Route`. The default admin credentials are `admin`/`admin`.

Wait for Keycloak to be ready:

```shell
oc wait --for=condition=ready pod -l app=keycloak --timeout=300s
```

Verify Keycloak is running:

```shell
oc get pods -l app=keycloak
```

## Step 3: Configure Authentication

Once Keycloak is ready, download the Wanaku realm configuration and use the Wanaku CLI to create the realm:

```shell
curl -sLO https://raw.githubusercontent.com/wanaku-ai/wanaku/wanaku-0.1.3/deploy/auth/wanaku-config.json

KEYCLOAK_HOST=$(oc get route keycloak -o jsonpath='{.spec.host}')

wanaku admin realm create \
  --keycloak-url "http://${KEYCLOAK_HOST}" \
  --admin-username admin \
  --admin-password \
  --config wanaku-config.json \
  --plain
```

Then, retrieve the OIDC client secret:

```shell
OIDC_SECRET=$(WANAKU_ADMIN_USERNAME=admin WANAKU_ADMIN_PASSWORD=admin \
  wanaku admin credentials show \
  --keycloak-url "http://${KEYCLOAK_HOST}" \
  --client-id wanaku-service \
  --show-secret --plain | cut -d ' ' -f 3)

echo "OIDC Secret: ${OIDC_SECRET}"
```

Save this secret — you will need it for the router and capability specs.

## Step 4: Deploy the Wanaku Operator

First, download the Helm chart from the Wanaku repository:

```shell
curl -sL https://github.com/wanaku-ai/wanaku/archive/refs/tags/wanaku-0.1.3.tar.gz | \
  tar xz --strip-components=4 wanaku-wanaku-0.1.3/apps/wanaku-operator/deploy/helm/wanaku-operator
```

This extracts the `wanaku-operator` Helm chart into the current directory.

Then, install the operator:

```shell
NAMESPACE=$(oc project -q)
helm install wanaku-operator \
  ./wanaku-operator \
  --namespace "${NAMESPACE}" \
  --set operatorNamespace="${NAMESPACE}"
```

Wait for it to become available:

```shell
oc wait deployment/wanaku-operator \
  --for=condition=Available \
  --timeout=120s
```

## Step 5: Create the Wanaku Router

The router is defined as a `WanakuRouter` custom resource. Create a file called `wanaku-router.yaml`, replacing `<KEYCLOAK_HOST>` with the value from Step 3:

```yaml
apiVersion: "wanaku.ai/v1alpha1"
kind: WanakuRouter
metadata:
  name: wanaku-router
spec:
  auth:
    authServer: http://<KEYCLOAK_HOST>
    authProxy: "auto"
  router:
    image: quay.io/wanaku/wanaku-router-backend:0.1.3
    imagePullPolicy: Always
```

Apply it:

```shell
oc apply -f wanaku-router.yaml
```

Wait for the router to be ready:

```shell
oc wait wanakurouter/wanaku-router \
  --for=condition=Ready \
  --timeout=120s
```

## Step 6: Deploy the HTTP Capability

The HTTP capability enables the router to execute HTTP-based tools. Create a file called `wanaku-http-capability.yaml`, using the same `<KEYCLOAK_HOST>` from the previous step and `<OIDC_SECRET>` with the secret obtained from `wanaku admin credentials show`:

```yaml
apiVersion: "wanaku.ai/v1alpha1"
kind: WanakuCapability
metadata:
  name: wanaku-http-capability
spec:
  auth:
    authServer: http://<KEYCLOAK_HOST>
    authProxy: "auto"
  secrets:
    oidcCredentialsSecret: <OIDC_SECRET>
  routerRef: wanaku-router
  capabilities:
    - name: wanaku-http
      image: quay.io/wanaku/wanaku-tool-service-http:0.1.3
```

Apply it:

```shell
oc apply -f wanaku-http-capability.yaml
```

Wait for it to be ready:

```shell
oc wait wanakucapabilities/wanaku-http-capability \
  --for=condition=Ready \
  --timeout=120s
```

## Step 7: Verify the Deployment

Get the Wanaku Router host:

```shell
export WANAKU_HOST="http://$(oc get route wanaku-router -o jsonpath='{.spec.host}')"
```

List available tools, resources, and capabilities:

```shell
wanaku tools list --host $WANAKU_HOST
wanaku resources list --host $WANAKU_HOST
wanaku capabilities list --host $WANAKU_HOST
```

Add a sample HTTP tool and test it:

```shell
wanaku tools add --host $WANAKU_HOST \
  -n "meow-facts" \
  --description "Retrieve random facts about cats" \
  --uri "https://meowfacts.herokuapp.com?count={count or 1}" \
  --type http \
  --property "count:int,The count of facts to retrieve" \
  --required count
```

Inspect MCP traffic:

```shell
npx @modelcontextprotocol/inspector
```

## Cleanup

Remove resources in reverse order:

```shell
oc delete wanakucapabilities/wanaku-http-capability
oc delete wanakurouter/wanaku-router
helm uninstall wanaku-operator
oc delete -f keycloak.yaml
```

## What's Next?

- [Camel Integration Capability](../3.02-camel-integration-capability/README.md) (demo 3.02) — deploy the Camel Integration Capability on Kubernetes/OpenShift to handle routing and transformation

If you find a bug, please [report it](https://github.com/wanaku-ai/wanaku/issues).
To get in touch with the community, visit the [Wanaku project](https://github.com/wanaku-ai/wanaku).
