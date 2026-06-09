# Troubleshooting Guide

This guide collects common problems people hit while running the Wanaku demos
locally and the first checks that usually resolve them.

## Demo builds but does not start

Checks:

- confirm you are using the Wanaku version expected by the demo branch
- re-run the setup steps from the specific demo directory
- inspect the terminal output for missing environment variables or ports already in use

## Capability changes do not appear

If you edited a capability or template and nothing changes:

- rebuild the project or restart the dev process
- confirm you updated the files used by the demo you are currently running
- check whether cached generated assets need to be regenerated

## Service-to-service integration fails

Start with:

- endpoint URL
- credentials / local auth config
- container or local process health
- expected request payload shape

Many integration problems are simple wiring mismatches rather than framework bugs.

## Local AI provider errors

When a demo uses a model provider or local inference runtime:

- verify the provider is reachable
- verify required model names are available locally
- confirm API keys or local tokens are loaded in the current shell

## Which demo should I debug first?

If you are unsure where a failure originates:

1. start with the smallest demo in the same topic family
2. confirm its baseline behavior
3. move to the more advanced demo once the simpler flow works

This reduces false assumptions caused by debugging too many moving parts at once.
