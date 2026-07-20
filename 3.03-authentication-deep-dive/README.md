---
title: "Authentication deep dive"
description: "Understand how OAuth2 and OIDC work in Wanaku — from concepts to troubleshooting."
---

# Authentication deep dive

Demo 3.01 walked you through deploying Keycloak and pasting secrets into YAML files. It worked, but
it left a gap: *why* those secrets exist, *what* happens when a capability sends a request to the
router, and *what* to do when authentication breaks. This demo fills that gap.

You will not deploy anything new here. Instead, you will trace the authentication flow end-to-end,
understand the moving parts, and learn how to diagnose the failures that inevitably come up in
production.

## What You Will Learn

- What OAuth2 and OIDC are, and why Wanaku uses them
- How the client credentials grant works (the grant type Wanaku capabilities use)
- How a capability authenticates with the router at registration time
- What happens to tokens after they are issued (lifecycle, auto-refresh, expiry)
- How to diagnose and fix common authentication failures

## What You Will Need

- Familiarity with [demo 3.01](../3.01-wanaku-on-the-cloud/README.md) (you should have completed it, or at least read through it)
- A running Wanaku deployment with Keycloak (from demo 3.01) is helpful for the troubleshooting section, but not required for the conceptual content

## OAuth2 and OIDC in 60 seconds

**OAuth2** is a protocol that lets one application access resources on behalf of another, without
sharing passwords. Instead of handing over credentials, the application gets a short-lived *token*
that proves it has permission.

**OpenID Connect (OIDC)** is a layer on top of OAuth2 that adds identity. While OAuth2 answers "is
this application allowed to do this?", OIDC also answers "who is this user?".

Wanaku uses both:

- **OIDC** for human users accessing the web UI and CLI — you log in with a username and password,
  and Keycloak issues a token that identifies you.
- **OAuth2 client credentials** for machine-to-machine communication — capabilities authenticate
  with the router using a client ID and secret, no human involved.

The rest of this demo focuses on the machine-to-machine side, because that is where most
authentication issues in Wanaku show up.

## Why Wanaku needs authentication

Without authentication, any service on the network could register itself as a capability, inject
tools into the router, or modify existing ones. In a production environment, that is a security
problem.

Wanaku's security model covers four areas:

| Area | What it protects | Who authenticates |
|------|-----------------|-------------------|
| **API protection** | Management operations (add/remove tools, resources, configuration) | Human users via OIDC |
| **UI access control** | The Wanaku web console | Human users via OIDC |
| **Service authentication** | Capability registration and heartbeats | Capability services via client credentials |
| **MCP authentication** | MCP tool and resource access | MCP clients via OAuth2 code grant or automatic client registration |

> [!NOTE]
> Wanaku can also run without authentication (`wanaku.http.auth=none`). This is useful for local
> development with `wanaku start local`, but should never be used in production.

## The client credentials grant

When a capability starts up, it needs to prove to the router that it is a legitimate service. It
does this using the **client credentials grant** — an OAuth2 flow designed for machine-to-machine
communication where no human is involved.

Here is what happens:

```
┌─────────────────┐                              ┌──────────────┐
│   Capability    │                              │   Keycloak   │
│   Service       │                              │   (OIDC)     │
└────────┬────────┘                              └──────┬───────┘
         │                                              │
         │  1. POST /token                              │
         │     grant_type=client_credentials            │
         │     client_id=wanaku-service                 │
         │     client_secret=<secret>                   │
         ├─────────────────────────────────────────────►│
         │                                              │
         │  2. 200 OK                                   │
         │     { access_token, expires_in }             │
         │◄─────────────────────────────────────────────┤
         │                                              │
         ▼                                              ▼
```

1. The capability sends its client ID and secret to Keycloak's token endpoint.
2. Keycloak validates the credentials and returns an access token (a JWT).

The capability then includes this token in every request to the router:

```
Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...
```

The router validates the token by checking its signature against Keycloak's public keys. If the
token is valid and not expired, the request goes through.

### Where the secret comes from

In demo 3.01, you ran `wanaku admin credentials show` to retrieve the OIDC client secret. That
secret lives in Keycloak, associated with the `wanaku-service` client in the `wanaku` realm.

When you pasted it into the `WanakuCapability` custom resource:

```yaml
spec:
  secrets:
    oidcCredentialsSecret: <OIDC_SECRET>
```

The Wanaku Operator injected it into the capability pod as the Quarkus configuration property
`quarkus.oidc-client.credentials.secret`. At startup, the capability used this secret to request
its first token.

> [!IMPORTANT]
> The client secret is the single most sensitive piece of configuration in a Wanaku deployment. If
> it leaks, an attacker can register arbitrary capabilities with the router. Treat it like a
> database password.

## Capability-to-router authentication flow

Now that you understand how tokens are obtained, here is the full registration flow — what happens
when a capability starts up and connects to the router.

```
┌─────────────┐          ┌──────────────┐          ┌──────────────┐
│  Capability │          │   Keycloak   │          │    Router    │
│  Service    │          │   (OIDC)     │          │              │
└──────┬──────┘          └──────┬───────┘          └──────┬───────┘
       │                        │                         │
       │  Phase 1: Get token    │                         │
       ├───────────────────────►│                         │
       │◄───────────────────────┤                         │
       │  (access_token)        │                         │
       │                        │                         │
       │  Phase 2: Register     │                         │
       │  POST /api/v1/management/discovery               │
       │  Authorization: Bearer {token}                   │
       ├─────────────────────────────────────────────────►│
       │                                                  │
       │  200 OK (assigned service ID)                    │
       │◄─────────────────────────────────────────────────┤
       │                                                  │
       │  Phase 3: Heartbeat (every N seconds)            │
       │  POST /api/v1/management/discovery/heartbeats    │
       │  Authorization: Bearer {token}                   │
       ├─────────────────────────────────────────────────►│
       │◄─────────────────────────────────────────────────┤
       │                                                  │
       ▼                                                  ▼
```

**Phase 1 — Token acquisition.** The capability sends its client credentials to Keycloak and gets
back a JWT access token.

**Phase 2 — Registration.** The capability calls the router's discovery API with the token in the
`Authorization` header. The router validates the token, registers the service, and assigns it a
unique ID. The capability persists this ID locally so it can re-register with the same identity
after a restart.

**Phase 3 — Heartbeat.** The capability sends periodic pings to the router's `/heartbeats`
endpoint. Each ping includes the bearer token, so token validity is continuously verified.

> [!NOTE]
> The heartbeat endpoint is deprecated. The router now uses a `PeriodicHealthCheckService` that
> actively checks capability health server-side, rather than relying on client-initiated pings.
> Existing capabilities that send heartbeats still work, but new implementations should not
> depend on this endpoint.

There is also a deregistration phase:

- **Deregistration** — on shutdown, the capability calls `DELETE /api/v1/management/discovery`
  to deregister itself so the router immediately knows it is gone. If a capability crashes
  without deregistering, the router's periodic health check will eventually detect its absence.

## Token lifecycle

Tokens do not last forever. Understanding their lifecycle prevents a class of subtle bugs where
everything works initially but breaks hours later.

### How tokens expire

When Keycloak issues a token, it includes an `expires_in` field (typically 300 seconds / 5 minutes
by default). After that time, the token is no longer valid, and any request using it gets a
`401 Unauthorized` response.

### Auto-refresh

The Wanaku Capabilities SDK handles token renewal automatically via the `ServiceAuthenticator`
class. Here is what happens:

1. The SDK receives a token with an `expires_in` of 300 seconds.
2. **30 seconds before expiry**, the SDK requests a new token using the client credentials grant.
3. The new token replaces the old one seamlessly — no requests fail during the swap.

```
Time ──────────────────────────────────────────────────────►

Token A issued          Renew at 4:30          Token B issued
     │                       │                       │
     ├───────────────────────┼───────────────────────┼──────
     │    Token A valid      │  Token A still valid  │  Token B valid
     │                       │  (but SDK renews)     │
     0s                    270s                     300s
```

> [!NOTE]
> The default Keycloak configuration for the `wanaku-service` client has
> `client_credentials.use_refresh_token` set to `false`. This means the client credentials grant
> does not return a refresh token — the SDK always re-authenticates with the client ID and secret.
> The SDK does support refresh tokens if your Keycloak configuration enables them, but that is not
> the default.

> [!TIP]
> If you are building a custom capability with the Java SDK, you do not need to manage token
> renewal yourself. The `ServiceAuthenticator` handles it internally. If you are building in
> another language, implement the same 30-second-before-expiry renewal logic.

### What happens on renewal failure

If the renewal fails (Keycloak is down, the client secret was rotated, or a network issue), the
SDK throws a `ServiceAuthException`. The current token remains in use until it expires, but once
it does, all requests fail with `401`. Check your capability logs for authentication errors if
you see intermittent failures.

## MCP client authentication

When an MCP client (an AI agent) connects to the Wanaku Router, it authenticates differently from
capabilities. MCP clients use the **authorization code grant** (with user interaction) or
**automatic client registration**.

### Authorization code grant

This is the standard browser-based flow:

1. The MCP client redirects the user to Keycloak's login page.
2. The user enters their username and password.
3. Keycloak redirects back to the client with an authorization code.
4. The client exchanges the code for an access token.

The default Keycloak realm provides these settings for MCP clients:

- **Client ID:** `mcp-client`
- **Required scope:** `openid`
- **Optional scope:** `wanaku-mcp-client` (must be explicitly requested)

> [!IMPORTANT]
> The `wanaku-mcp-client` scope is optional, not a default. Your MCP client must explicitly
> request it in the authorization request. Without it, the token will lack the
> `wanaku-mcp-client` audience and namespace-scoped requests will fail with `401`.

### Automatic client registration

Wanaku also supports automatic client registration, where an MCP client can register itself
without pre-configuration in Keycloak. Access is granted per-namespace — if a client changes
the namespace it is accessing, it must request a new client ID and grant.

> [!IMPORTANT]
> When using automatic client registration, access is scoped to a single namespace. Switching
> namespaces requires a new registration.

## CLI authentication

The Wanaku CLI supports authenticated access to the router API. This is how you, as an
administrator, authenticate when managing tools, resources, and capabilities.

```shell
# Log in with username and password through the router's OIDC proxy
wanaku auth login \
  --auth-server http://localhost:8080 \
  --username alice \
  --password

# Check your authentication status
wanaku auth status

# All subsequent commands include the token automatically
wanaku tools list

# Log out when done
wanaku auth logout
```

Credentials are stored in `~/.wanaku/credentials`. Set file permissions to `600` on Unix systems
to prevent unauthorized access.

> [!TIP]
> For CI/CD scripts, use `--token` with an environment variable instead of storing credentials:
> `wanaku tools list --token $WANAKU_API_TOKEN`

## Troubleshooting authentication failures

### 401 Unauthorized on capability registration

**Symptoms:** The capability fails to start, logs show `401` responses from the router.

**Common causes:**

1. **Wrong client secret.** The secret in the capability's configuration does not match Keycloak.
   Verify it:

   ```shell
   wanaku admin credentials show \
     --client-id wanaku-service \
     --show-secret
   ```

   Compare the output with what your capability is using.

2. **Token issuer mismatch.** Keycloak's `KC_HOSTNAME` does not match the URL the router uses to
   validate tokens. This happens when Keycloak is accessed via different URLs internally and
   externally. The token says it was issued by `https://keycloak.example.com`, but the router
   expects tokens from `http://keycloak:8080`.

   Fix: set `KC_HOSTNAME` on the Keycloak deployment to match the external route:

   ```shell
   KEYCLOAK_HOST=$(oc get route keycloak -o jsonpath='{.spec.host}')
   oc set env deployment/keycloak \
     KC_HOSTNAME="${KEYCLOAK_HOST}" \
     KC_HOSTNAME_STRICT=false
   ```

3. **Wrong auth server URL.** The capability is pointing to the wrong Keycloak instance or realm.
   Check the `quarkus.oidc-client.auth-server-url` property — it should be
   `http://<keycloak-host>/realms/wanaku`.

### Token expiry after hours of working fine

**Symptoms:** Everything works initially, then capabilities start getting `401` errors after a few
hours.

**Common causes:**

1. **Client secret was rotated.** If someone regenerated the `wanaku-service` client secret in
   Keycloak, the capability's stored secret no longer works. The next token renewal fails and
   subsequent requests get `401`. Re-deploy the capability with the new secret.

2. **Keycloak restarted without persistent storage.** If Keycloak loses its data (no persistent
   volume), all tokens and sessions are invalidated. Verify that Keycloak has a
   `PersistentVolumeClaim` (demo 3.01 includes one).

3. **Clock skew.** JWT validation is time-sensitive. If the system clocks on Keycloak, the router,
   or the capability are more than a few seconds apart, tokens can appear expired before they
   actually are. Ensure NTP is running on all nodes.

### Capability registers but tools are not visible

**Symptoms:** `wanaku capabilities list` shows the capability as active, but `wanaku tools list`
does not show its tools.

**This is usually not an auth problem.** It means the capability registered successfully
(authentication worked) but did not publish its tools. Check:

- The capability's route and rules files are correctly loaded (for CIC)
- The rules file lists the tools you expect
- The capability logs for route-loading errors

### "Account is not fully set up" error

**Symptoms:** CLI login with `wanaku auth login` fails with `ServiceAuthException: Account is not
fully set up`.

**Cause:** The Keycloak user's email is not marked as verified. Keycloak requires email
verification for OIDC login.

**Fix:** Create users with the `--verified` flag (the default in `wanaku admin users add`), or
verify the email in Keycloak's admin console.

### Connection refused to auth server

**Symptoms:** Capability logs show connection refused or timeout errors when contacting Keycloak.

**Check:**

1. Keycloak is running: `oc get pods -l app=keycloak`
2. The Keycloak service exists: `oc get svc keycloak`
3. The capability can reach Keycloak (same namespace, or correct service DNS)
4. The auth server URL uses the internal service name (e.g., `http://keycloak:8080`), not the
   external route URL, for pod-to-pod communication within the cluster

## What's Next?

- [Building a Java capability](../4.01-plain-java-capability/README.md) (demo 4.01) — build your own capability service from scratch using Java and the Wanaku Capabilities SDK, including the authentication setup you now understand

If you find a bug, please [report it](https://github.com/wanaku-ai/wanaku/issues).
To get in touch with the community, visit the [Wanaku project](https://github.com/wanaku-ai/wanaku).
