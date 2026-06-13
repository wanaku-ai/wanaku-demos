# Troubleshooting Guide

This guide covers the most common Wanaku demo failures: wrong version, missing
configuration, port conflicts, and auth or endpoint mismatch.

## Demo starts, but version is wrong

If the demo works but behavior does not match the guide:

- confirm you are on the branch or tag that matches the walkthrough
- rerun setup from the demo directory, not from repo root
- compare your local `README.md` with the versioned demo docs

## Demo does not start

Start with the basics:

- inspect terminal output for missing environment variables
- check whether the expected port is already in use
- restart the dev process after changing config files

Most startup failures are setup drift, not code bugs.

## Capability or demo changes do not appear

If you edited files and nothing changes in the UI:

- rebuild the app or restart the local process
- confirm you changed the files for the demo you are actually running
- clear any generated or cached assets if the demo renders from them

## Auth or endpoint lookup fails

When a demo depends on Keycloak, OpenShift routes, or another service:

- verify the URL is reachable from the current shell
- confirm the token, secret, or route host matches current cluster state
- check for stale copied values from an earlier setup run

## Integration call fails

If one service can start but requests fail later:

- verify the target service is healthy
- compare request shape with what the demo expects
- check whether the error is a timeout, 404, or auth failure

## Which demo should I debug first?

If you are unsure where failure originates:

1. start with smallest demo in same topic family
2. confirm baseline behavior
3. move to advanced demo once simple flow works

This avoids debugging too many moving parts at once.
