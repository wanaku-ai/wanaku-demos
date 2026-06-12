# Security Best Practices for Wanaku

This guide outlines security best practices when deploying and operating Wanaku in production environments.

## What You Will Learn

- How to configure authentication and authorization in Wanaku
- How to use TLS/HTTPS to secure communications
- How to apply the principle of least privilege to tool and resource access
- How to manage secrets and credentials safely
- How to monitor and audit Wanaku activity

## What You Will Need

- A running Wanaku instance (local or Kubernetes)
- Basic understanding of authentication concepts
- Access to Wanaku configuration files

## Step 1: Enable Authentication

Wanaku supports OAuth2/OIDC-based authentication. To enable it:

- Set the auth provider in your Wanaku configuration:
  ```yaml
    auth:
        enabled: true
            provider: oidc
                issuer-url: https://your-idp.example.com
                    client-id: wanaku
                      ```
                      - When auth is enabled, all MCP client connections require a valid bearer token
                      - Do NOT pass `--client-id` when auth is disabled — it has no effect and may cause confusion

                      ## Step 2: Use TLS for All Communications

                      All communications between Wanaku components and MCP clients should use TLS:

                      - Enable HTTPS on the Wanaku server:
                        ```yaml
                          quarkus:
                              http:
                                    ssl-port: 8443
                                          ssl:
                                                  certificate:
                                                            key-store-file: /path/to/keystore.jks
                                                                      key-store-password: ${KEYSTORE_PASSWORD}
                                                                        ```
                                                                        - Use trusted certificates from a CA in production
                                                                        - Avoid self-signed certificates in production environments
                                                                        - In Kubernetes, use cert-manager or your cloud provider's certificate management

                                                                        ## Step 3: Apply Least Privilege

                                                                        Limit what each identity can access:

                                                                        - Create dedicated service accounts for each MCP client or tool provider
                                                                        - Grant only the permissions needed for each client's use case
                                                                        - Review and rotate credentials regularly
                                                                        - Remove unused tool registrations and capability routes promptly

                                                                        ## Step 4: Manage Secrets Safely

                                                                        Never store secrets in plain text:

                                                                        - Use environment variables or a secrets manager (Vault, AWS Secrets Manager, etc.) for sensitive values
                                                                        - In Kubernetes, use Secrets instead of ConfigMaps for credentials:
                                                                          ```yaml
                                                                            env:
                                                                              - name: KEYSTORE_PASSWORD
                                                                                  valueFrom:
                                                                                        secretKeyRef:
                                                                                                name: wanaku-tls
                                                                                                        key: keystore-password
                                                                                                          ```
                                                                                                          - Rotate secrets periodically and after any suspected exposure
                                                                                                          - Avoid logging sensitive values — check your log configuration
                                                                                                          
                                                                                                          ## Step 5: Monitor and Audit Activity
                                                                                                          
                                                                                                          Track what is happening in your Wanaku instance:
                                                                                                          
                                                                                                          - Enable audit logging to record tool invocations and resource access
                                                                                                          - Forward logs to a centralized SIEM or log aggregation platform
                                                                                                          - Set up alerts for unusual activity patterns (e.g., excessive errors, unexpected clients)
                                                                                                          - Periodically review who has access and what tools are registered
                                                                                                          
                                                                                                          ## What's Next?
                                                                                                          
                                                                                                          - Review the [Monitoring Wanaku in Production](../5.02-monitoring-wanaku-in-production/README.md) guide for observability setup
                                                                                                          - See the [Authentication Deep Dive](../3.03-authentication-deep-dive/README.md) for advanced auth configuration
                                                                                                          - Consult the [Troubleshooting Guide](../troubleshooting/README.md) if you encounter issues
