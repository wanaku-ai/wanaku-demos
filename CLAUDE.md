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

## Demo Documentation Standards

Every demo README must follow this structure to maintain consistency across the repository. These standards are derived from the existing demo documentation and are non-negotiable for new demos.

### Required Section Order

Each demo README must contain these sections in this exact order:

1. **Title** (H1)
2. **Introduction/Context** (paragraph)
3. **What You Will Learn** (H2 section)
4. **What You Will Need** (H2 section)
5. **Main Content** (steps, architecture, explanations)
6. **What's Next?** (H2 section)
7. **Closing Matter** (troubleshooting, bug reporting, community links)

### 1. Title (H1)

- Must be a clear, descriptive title that matches the demo's purpose
- Use sentence case (capitalize first word and proper nouns only)
- Examples:
  - `# Getting Started with Wanaku`
  - `# Managing Tools, Resources, Prompts, and Forwards`
  - `# Service Catalogs Demo`

### 2. Introduction/Context

- Starts immediately after the title, before any other sections
- 1-3 paragraphs explaining what the demo does and why it matters
- Sets the stage without getting into prerequisites or step-by-step instructions
- Uses plain, direct language that answers "what is this about?"
- Example opening: "This guide walks you through creating and deploying a Service Catalog — a way to package Apache Camel routes as tools that AI agents can use through Wanaku."

### 3. What You Will Learn

- **Must** use the heading `## What You Will Learn` exactly
- Appears before "What You Will Need"
- Bullet list (using `-` or `*`) of concrete learning outcomes
- Each item should describe a specific skill or knowledge the user will gain
- Use "how to" phrasing where appropriate
- Example:
  ```markdown
  ## What You Will Learn
  
  - How to create a Service Catalog with Apache Camel routes
  - How to expose Camel routes as MCP tools
  - How to deploy a Service Catalog to the Wanaku router
  ```

### 4. What You Will Need

- **Must** use the heading `## What You Will Need` exactly
- Appears after "What You Will Learn"
- Bullet list of prerequisites
- Include:
  - Software/tools required (with versions if critical)
  - Prior demos that must be completed (with links)
  - Cluster access, credentials, or other environmental prerequisites
  - Link to download pages or installation guides where appropriate
- Examples:
  - `- Wanaku CLI installed (download from [releases page](https://github.com/wanaku-ai/wanaku/releases/tag/v0.1.3))`
  - `- Java 21 or later`
  - `You must have completed [demo 1.01](../1.01-your-first-tool/README.md).`

### 5. Main Content Structure

#### Step Numbering

- Use `## Step N: Descriptive Title` for sequential instructions
- Number steps sequentially from 1
- Step titles should be action-oriented (verbs): "Install the CLI", "Deploy Keycloak", "Verify the Tools"
- Nested steps use H3 (`###`) with descriptive titles, not numbers
- Sub-steps within a step use ordered lists (`1.`, `2.`, etc.)

#### Architecture Diagrams

- When relevant, include ASCII art diagrams showing component relationships
- Place diagrams **before** the steps that deploy those components
- Use consistent box-drawing characters
- Example from the cloud demo:
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
  ```

#### Code Blocks

- Use fenced code blocks with language tags
- Shell commands: ` ```shell ` or ` ```bash `
- YAML: ` ```yaml `
- JSON: ` ```json `
- Java: ` ```java `
- Code blocks showing output use ` ```text ` or ` ```shell ` with a preceding line like "Expected output:" or "Output:"
- Multi-line shell commands use `\` for line continuation
- Example:
  ````markdown
  ```shell
  mvn -B archetype:generate \
      -DarchetypeGroupId=ai.wanaku.sdk \
      -DarchetypeArtifactId=capabilities-archetypes-java-tool \
      -DarchetypeVersion=0.1.0
  ```
  ````

#### Callout Blocks

Use markdown blockquotes with admonition syntax for notes, tips, warnings:

- `> [!NOTE]` — supplementary information that doesn't block progress
- `> [!TIP]` — helpful shortcuts or best practices
- `> [!IMPORTANT]` — critical information the user must not overlook
- `> [!WARNING]` — warns about dangerous operations or common mistakes

Examples:
```markdown
> [!TIP]
> You can enable additional capability services with the `--services` option.

> [!NOTE]
> For this example to work with a live API, you would typically need an API key.

> [!IMPORTANT]
> Make sure you set the **URL** field, not the `Host` field.
```

#### Tables

- Use tables to compare options, list parameters, or summarize structured data
- Always include a header row
- Keep cells concise — use descriptions, not paragraphs
- Example:
  ```markdown
  | Route ID | What It Does |
  |----------|-------------|
  | `get-employee-information` | Calls `GET /employee/{id}/information` |
  | `get-employee-reviews` | Calls `GET /employee/{id}/reviews` |
  ```

#### Code Highlights

- Use **bold** for UI elements the user must click or find: "Click **Upload**", "Navigate to **Projects**"
- Use inline code (backticks) for:
  - File names: `index.properties`, `wanaku-router.yaml`
  - CLI commands or flags: `wanaku tools list`, `--services exec`
  - Environment variables: `MCP_ECHO`, `OIDC_SECRET`
  - Endpoints/URIs: `http://localhost:8080`, `direct:wanaku`
  - JSON keys or YAML fields: `service.catalog`, `ROUTES_PATH`

#### Placeholder Conventions

- Wrap placeholders in `<angle-brackets>`: `<your-registry>`, `<KEYCLOAK_HOST>`
- Always explain what the user should replace it with, either inline or in a preceding sentence
- Example: "Replace `<your-registry>` with your container registry (e.g., `quay.io/your-username`)."

#### Links

- Use descriptive link text, not "click here" or raw URLs
- Internal demo links use relative paths: `[demo 1.01](../1.01-your-first-tool/README.md)`
- External links use full URLs with descriptive anchor text: `[releases page](https://github.com/wanaku-ai/wanaku/releases/tag/v0.1.3)`
- Prefer inline links over reference-style links
- Link to prior demos when they provide necessary context

#### Verification Steps

- After deployment steps, include a verification command
- Show expected output so the user knows what success looks like
- Use a pattern like:
  ```markdown
  Verify the installation:
  
  ```shell
  wanaku --version
  ```
  ```

#### Testing/Validation Sections

- Include concrete test scenarios where applicable
- Provide example queries or commands the user can run to validate the demo
- Example: "Then ask it a question like: > How much is 100 USD in CZK?"

### 6. What's Next?

- **Must** use the heading `## What's Next?` exactly
- Appears after the main content, before troubleshooting/closing sections
- Bullet list of related demos (with links) or next logical steps
- First item usually points to the next sequential demo in the chapter
- Example:
  ```markdown
  ## What's Next?
  
  - [Introduction to Capabilities](../2.01-introduction-to-capabilities/README.md) (demo 2.01)
  ```

### 7. Closing Matter

Appears at the very end of the document, after "What's Next?"

- Include a **Troubleshooting** section (H2) if the demo has known gotchas or common errors
  - Format as H3 subsections per issue: `### Service Catalog deployment fails`
  - Provide diagnostic steps or solutions
- Include a **Cleanup** or **Shutdown** section (H2) if the demo deployed cloud resources
  - Provide commands to remove everything in reverse deployment order
- **Always** end with these two lines:
  ```markdown
  If you find a bug, please [report it](https://github.com/wanaku-ai/wanaku/issues).
  To get in touch with the community, visit the [Wanaku project](https://github.com/wanaku-ai/wanaku).
  ```

### Voice and Tone

- **Active voice only**. "You deploy Keycloak" not "Keycloak is deployed by you."
- **Second person ("you")**. Address the reader directly.
- **Imperative mood for instructions**. "Run this command." "Create a file." "Replace the placeholder."
- **No corporate speak**. Avoid "leverage", "utilize", "facilitate" — use plain verbs.
- **Concrete over abstract**. Prefer specific examples and real commands over vague descriptions.
- **Honest about tradeoffs**. If something is a workaround, say so. Example: "In this demo, you copy files manually — in production, you'd use a service catalog."

### Formatting Conventions

- Use **sentence case for headings**, not Title Case
- One blank line before and after headings
- One blank line before and after code blocks
- Ordered lists for sequential steps within a step
- Unordered lists (with `-`) for non-sequential items
- No trailing whitespace at end of lines
- End files with a single newline

### Optional Sections

These sections are optional but should follow established patterns if included:

- **Architecture Overview** (H2) — appears early, before steps. Shows how components fit together.
- **Understanding the Pieces** (H2) — appears before deployment steps. Explains configuration files in detail.
- **Part N: Title** (H2) — for demos with distinct phases (e.g., "Part 1: Using a Built-in Template")
- **Expandable details** — use `<details>` blocks for optional deep-dives that would otherwise clutter the main flow

### Version and Release References

- Always reference specific versions when linking to releases or Docker images
- Example: `v0.1.3`, `wanaku-0.1.3`, `quay.io/wanaku/wanaku-router-backend:0.1.3`
- If version is environment-specific, explain how to determine the correct version
- Link to release pages where users can find the latest version: `[releases page](https://github.com/wanaku-ai/wanaku/releases/tag/v0.1.3)`

### Content That Should NOT Appear

- Do not include "Author" or "Last Updated" metadata
- Do not include table-of-contents navigation (the structure is self-evident from headings)
- Do not include "Introduction" or "Overview" as explicit H2 headings — the opening paragraphs serve this purpose
- Do not recap what the user just did at the end — the "What's Next?" section points forward, not backward
- Do not include installation instructions for widely-available tools (`kubectl`, `helm`, `git`) — link to official installation guides instead

### New Demo Checklist

Before submitting a new demo README, verify:

- [ ] Title is clear and uses sentence case
- [ ] Introduction explains what and why before any prerequisites
- [ ] "What You Will Learn" section exists and lists concrete outcomes
- [ ] "What You Will Need" section exists with all prerequisites
- [ ] Steps are numbered sequentially with descriptive titles
- [ ] Code blocks use appropriate language tags
- [ ] Placeholders are wrapped in `<angle-brackets>` and explained
- [ ] Verification/testing steps are included where appropriate
- [ ] "What's Next?" section exists and points to related demos
- [ ] Ends with bug reporting and community links
- [ ] Uses active voice and imperative mood
- [ ] No corporate jargon or vague language
