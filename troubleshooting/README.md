# Troubleshooting Guide

This guide covers common issues you may encounter when working with Wanaku and how to resolve them.

## CLI and Router Issues

**Problem: `wanaku: command not found`**

The CLI is not on your PATH. After installing via `get-wanaku.sh`, add `$HOME/bin` to your PATH:

```bash
echo 'export PATH="$HOME/bin:$PATH"' >> ~/.bashrc
source ~/.bashrc
```

**Problem: `wanaku start local` fails to start**

Check that Java 21 or later is installed: `java -version`. If the version is lower than 21, install Java 21 and set `JAVA_HOME`.

**Problem: Cannot connect to router after `wanaku start local`**

The router starts on port 9000. Verify it is running:

```bash
curl http://localhost:9000/q/health/ready
```

If this fails, check the logs in `~/.wanaku/logs/router.log`.

## Tool and Capability Issues

**Problem: `wanaku tool list` shows no tools**

Add tools first with `wanaku tool add`, or check that your service catalog is deployed.

**Problem: Tool invocation fails with `no capability registered`**

Start the required capability service:

```bash
wanaku start local --services http,exec
```

**Problem: HTTP tool returns `connection refused`**

The target URL is not reachable from the machine running Wanaku. Check network connectivity and firewall rules.

## Kubernetes Issues

**Problem: Wanaku pods are in `CrashLoopBackOff`**

Check pod logs: `kubectl logs -l app=wanaku-router -n wanaku`

Common causes: missing configuration, wrong image tag, or insufficient resources.

**Problem: Capabilities not registering with the router**

Ensure capability pods can reach the router service on port 9000. Check that `WANAKU_ROUTER_HOST` is set correctly.

## Authentication Issues

**Problem: `401 Unauthorized` on all requests**

Authentication is enabled but no token is provided. See the [Authentication Deep Dive](../3.03-authentication-deep-dive/README.md) guide.

**Problem: `403 Forbidden` with a valid token**

The client lacks required roles. Check the Keycloak client role assignments.

## Getting More Help

- Check [Wanaku GitHub Issues](https://github.com/wanaku-ai/wanaku/issues) for known issues
- Enable debug logging: `wanaku start local --log-level DEBUG`
