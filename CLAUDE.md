# Wanaku Demos

## Demo Organization

Demos use a `chapter.demo` numbering scheme (e.g., `1.01`, `2.02`).

| Chapter | Focus                                                                                    | Environment          |
|---------|------------------------------------------------------------------------------------------|----------------------|
| 1.xx    | Getting Started — tools, resources, prompts, forwards                                    | `wanaku start local` |
| 2.xx    | Working with Capabilities — HTTP, exec, Camel Integration Capability, service catalogs   | `wanaku start local` |
| 3.xx    | Production & Cloud — Operator, Keycloak, OpenShift deployments                           | Cloud/Kubernetes     |
| 4.xx    | Extending Wanaku — building new capabilities with Java/Quarkus, exposing existing routes | `wanaku start local` |
| 5.xx    | Advanced Use Cases — Camel Assistant, experimental features                              | Varies               |

When adding a new demo, place it in the correct chapter and use the next available number (e.g., if `2.02` exists, the next chapter 2 demo is `2.03`).
