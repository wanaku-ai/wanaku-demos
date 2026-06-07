# Authentication Deep Dive

This guide covers authentication in Wanaku in detail — how to enable it, configure clients, and secure your deployment.

## What You Will Learn

- How Wanaku authentication works
- Enabling authentication in a local deployment
- Configuring OAuth2/OIDC with Keycloak
- Passing credentials from an AI agent to Wanaku
- Troubleshooting common authentication errors

## What You Will Need

- **Wanaku CLI** installed (download from [releases page](https://github.com/wanaku-ai/wanaku/releases))
- **Docker** or **Podman** for running Keycloak locally
- Basic familiarity with OAuth2 concepts (client ID, client secret, token endpoint)

## Step 1: Understanding Wanaku Authentication

By default, `wanaku start local` runs without authentication — any client can connect. For production deployments you should enable **OAuth2/OIDC** authentication, which integrates with any standard identity provider (Keycloak, Okta, Auth0, Azure AD, etc.).

When authentication is enabled:

- Clients must obtain a bearer token from the identity provider
- All requests to the Wanaku router must include an `Authorization: Bearer <token>` header
- The router validates the token on each request

## Step 2: Starting Keycloak Locally

For this demo we use Keycloak in development mode:

```bash
docker run -d \
  --name keycloak-dev \
    -p 8180:8080 \
      -e KEYCLOAK_ADMIN=admin \
        -e KEYCLOAK_ADMIN_PASSWORD=admin \
          quay.io/keycloak/keycloak:latest \
            start-dev
            ```

            Wait 30 seconds for Keycloak to start, then open http://localhost:8180 and log in with `admin`/`admin`.

            ## Step 3: Creating a Realm and Client in Keycloak

            1. Create a new realm called `wanaku`
            2. Inside the realm, create a client with these settings:
               - **Client ID**: `wanaku-cli`
                  - **Client authentication**: On
                     - **Authorization**: Off
                        - **Authentication flow**: Service accounts roles
                        3. Note the **client secret** from the Credentials tab

                        ## Step 4: Starting Wanaku with Authentication

                        Pass the OIDC issuer URL when starting Wanaku:

                        ```bash
                        wanaku start local \
                          --auth-enabled \
                            --oidc-issuer http://localhost:8180/realms/wanaku
                            ```

                            Wanaku will now require a valid bearer token on all MCP requests.

                            ## Step 5: Obtaining a Token

                            Request a token from Keycloak using the client credentials flow:

                            ```bash
                            TOKEN=$(curl -s \
                              -d "client_id=wanaku-cli" \
                                -d "client_secret=YOUR_CLIENT_SECRET" \
                                  -d "grant_type=client_credentials" \
                                    http://localhost:8180/realms/wanaku/protocol/openid-connect/token \
                                      | jq -r .access_token)
                                      ```

                                      ## Step 6: Using the Token with the Wanaku CLI

                                      Pass the token when running CLI commands:

                                      ```bash
                                      wanaku tool list --token "$TOKEN"
                                      ```

                                      Or configure the CLI to use the token automatically:

                                      ```bash
                                      wanaku config set --auth-token "$TOKEN"
                                      wanaku tool list
                                      ```

                                      ## Troubleshooting

                                      **401 Unauthorized**: The token is missing or expired. Request a new token and retry.

                                      **403 Forbidden**: The token is valid but the client does not have the required roles. Check the Keycloak client role assignments.

                                      **Connection refused on 8180**: Keycloak is still starting. Wait 30 seconds and retry.

                                      ## What's Next?

                                      - [4.01 Plain Java Capability](../4.01-plain-java-capability/README.md) — build a custom capability in Java
                                      - [4.02 Exposing Existing Routes](../4.02-exposing-existing-routes/README.md) — wrap existing Camel routes

                                      If you find a bug, please report it. To get in touch with the community, visit the [Wanaku project](https://github.com/wanaku-ai/wanaku).
