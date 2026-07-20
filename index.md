---
layout: home

hero:
  name: "Wanaku MCP Router Demos"
  tagline: Learn step-by-step how to use Wanaku.

  actions:
    - theme: brand
      text: Getting Started Guide
      link: ./1.01-your-first-tool/README

features:
  - title: "Chapter 1: Understanding MCP and Wanaku"
    details: Learn the concepts behind the Model Context Protocol and how Wanaku extends it with routing, namespaces, and forwarding
    link: ./1.00-understanding-mcp-and-wanaku/README
  - title: "Chapter 1: Getting Started with Wanaku"
    details: Install the Wanaku CLI, start a local instance, add a tool, and use it from an AI agent
    link: ./1.01-your-first-tool/README
  - title: "Chapter 1: Basic Wanaku Operations"
    details: Manage tools, resources, prompts, and forwards — the four core MCP primitives
    link: ./1.02-basic-wanaku-operations/README
  - title: "Chapter 2: Introduction to Capabilities"
    details: Understand how capabilities work in Wanaku — built-in HTTP and Exec, the Camel Integration Capability, and custom extensions
    link: ./2.01-introduction-to-capabilities/README
  - title: "Chapter 2: Service Catalogs"
    details: Package Apache Camel routes as MCP tools and deploy them to the Wanaku router
    link: ./2.02-service-catalogs/README
  - title: "Chapter 2: Service Templates"
    details: Use reusable, parameterized blueprints to create Service Catalogs quickly
    link: ./2.03-service-templates/README
  - title: "Chapter 2: Organizing with Namespaces"
    details: Use Wanaku namespaces to isolate tools and resources across environments or teams
    link: ./2.04-organizing-with-namespaces/README
  - title: "Chapter 2: Aggregating MCP Servers with Forwards"
    details: Expose tools from upstream MCP servers through a single Wanaku router endpoint using forwards
    link: ./2.05-aggregating-mcp-servers-with-forwards/README
  - title: "Chapter 2: Creating Custom Capabilities"
    details: Scaffold new tool and resource capability services with `wanaku capabilities create`
    link: ./2.06-creating-custom-capabilities/README
  - title: "Chapter 3: Wanaku on the Cloud"
    details: Deploy Wanaku on OpenShift with Keycloak authentication and the Wanaku Operator
    link: ./3.01-wanaku-on-the-cloud/README
  - title: "Chapter 3: Camel Integration Capability"
    details: Run Apache Camel workloads on Wanaku using the Camel Integration Capability
    link: ./3.02-camel-integration-capability/README
  - title: "Chapter 3: Authentication Deep Dive"
    details: Understand how OAuth2 and OIDC work in Wanaku — from concepts to troubleshooting
    link: ./3.03-authentication-deep-dive/README
  - title: "Chapter 4: Building a Plain Java Capability"
    details: Create a simple "echo" capability for Wanaku using the Java Capabilities SDK
    link: ./4.01-plain-java-capability/README
  - title: "Chapter 4: Exposing Existing Camel Routes"
    details: Expose existing Apache Camel routes as MCP tools via the Wanaku plugin
    link: ./4.02-exposing-existing-routes/sample-routes/camel-core-examples/cat-facts-example/README
  - title: "Chapter 5: Camel Assistant"
    details: Build an expert AI agent for Apache Camel with LangFlow and Wanaku
    link: ./5.01-camel-assistant/README
---
