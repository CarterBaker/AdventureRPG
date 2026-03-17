# AdventureRPG — Documentation Context

---

## For You — Pasting a Pipeline Into a Conversation

When you want Claude to document or analyze a full pipeline, paste the following in this order in a single message:

1. **This context file** — paste the full contents of `EngineDocumentationContext.md` first.
2. **`EngineContext.md`** — paste the full engine rules/conventions doc second.
3. **The pipeline source** — paste the relevant section of `ProjectIndex.docx` (the classes and packages for that pipeline only, e.g. everything under `shaderpipeline/`).
4. **Your request** — end with a clear instruction, e.g.:
   > "Create a pipeline DOCX for the Shader Pipeline using the classes listed above."

**Optimal format for pasting source:**
- Paste the raw class/package tree exactly as it appears in the project index.
- Do not summarize or compress it — Claude needs every class name to produce accurate output.
- If pasting multiple pipelines at once, separate them with a header line like `--- SHADER PIPELINE ---`.

---

## For You — Generating Index.txt

Open the VSCode terminal (`Ctrl+` ` `). It opens at your workspace root automatically.

**The command to run:**

```bat
tree /F > Index.txt
```

No path argument — just run it from the project root and it outputs the full tree from there. `/F` includes file names. The `/A` flag is optional but recommended so the output uses plain ASCII characters instead of box-drawing chars.

```bat
tree /F /A > Index.txt
```

**If it does not overwrite an existing Index.txt** — close the file in VSCode first. Windows locks open files and the write will silently fail if it is open in an editor tab. Close it, re-run the command, then reopen it.

**Verify it worked** — the file should be several hundred lines long with nested folder structure visible.

Once generated, paste the full contents of `Index.txt` into the chat and say:
> "Create an Index DOCX from this."

---

## For Claude — Creating a Pipeline DOCX

When the user pastes a pipeline (classes + package tree) and requests a DOCX, follow this process exactly.

### What a Pipeline DOCX Contains

Each pipeline DOCX is a **structured reference document** for a single pipeline (e.g. `ShaderPipeline`, `WorldPipeline`). It must include:

1. **Cover / Title** — Pipeline name, engine name (`TerraArcana Engine`), section label (`Bootstrap — com.internal.bootstrap`)
2. **Overview paragraph** — 2–4 sentences describing what the pipeline owns and is responsible for. Derive this from the class names and engine context rules.
3. **Package breakdown** — one section per sub-package. For each:
   - Sub-package heading (e.g. `shadermanager/`)
   - Table with two columns: **Class** and **Role**
   - Role is derived from the naming convention rules in `EngineContext.md` (e.g. `XManager` → owns registration/lifecycle, `XLoader` → scans directory + batch loads, `XBuilder` → parses JSON + creates Handles, etc.)
4. **Class Role Summary Table** — full flat list of every class in the pipeline with its package and role. Sorted alphabetically by class name.
5. **Naming Convention Reference** — a compact table of the suffix → base class → role rules, copied from `EngineContext.md`. Placed as an appendix.

### DOCX Formatting Rules

- US Letter, 1-inch margins
- Font: Arial throughout
- Heading 1: Pipeline name (e.g. `Shader Pipeline`)
- Heading 2: Sub-package names (e.g. `shadermanager/`)
- Heading 3: Any further breakdown if needed
- Tables: light header shading (`D5E8F0`), `ShadingType.CLEAR`, borders `CCCCCC`, cell padding `top:80 bottom:80 left:120 right:120`
- Column widths always in DXA, always sum to content width (9360 for Letter with 1" margins)
- No unicode bullets — use `LevelFormat.BULLET` if lists are needed
- Class names always in `monospace` bold inside table cells: `new TextRun({ text: "ClassName", bold: true, font: "Courier New" })`

### Deriving Roles From Class Names

Use the suffix rules from `EngineContext.md` directly:

| Suffix | Role Description |
|---|---|
| `XData` | Raw data payload. Constructed with `new`. Held inside a Handle or Instance. |
| `XHandle` | Persistent, manager-registered wrapper. Lives start to shutdown. |
| `XInstance` | Runtime clone of a Handle. Safe to mutate and dispose. |
| `XManager` | Owns registration, lifecycle, and public access for a system. |
| `XLoader` | Scans directory, batch-processes files. Bootstrap-only, self-destructs. |
| `XBuilder` | Parses external data (JSON), drives Handle creation. Bootstrap-only. |
| `XBranch` | Internal computation unit owned by a Manager. Same package as manager. |
| `XSystem` | Self-contained single-job helper. No routing, one specific job. |
| `XStruct` | No lifecycle. Holds data, passed around freely. Use `new`. |
| `GLSLUtility` | Stateless OpenGL helper. All static methods. Package-private. |
| `InternalBuilder` | Nested builder owned by a Loader. Never exposed externally. |
| `InternalLoader` | Loader created inside a Manager's `create()`. Auto-released when done. |
| `InternalBufferSystem` | Owns GPU buffer management for a Manager. |

---

## For Claude — Creating an Index DOCX

When the user pastes the output of `tree src /F /A` (an `Index.txt`), create an **Index DOCX** with:

1. **Title page** — "TerraArcana Engine — Project Index", date
2. **Table of Contents** — auto-generated from Heading 1/2 entries
3. **One Heading 1 section per top-level package** (e.g. `com.internal.core`, `com.internal.bootstrap`, `com.internal.runtime`)
4. **One Heading 2 per sub-package** within each section
5. **A table per sub-package** — two columns: **Class** and **Package Path**
6. **Pending Items section** — if any `TODO`, `FIXME`, rename notes, or `→` annotations appear in the source, collect them into a final section

Parse the tree output as follows:
- Lines ending in `.java` → strip `.java` → that is the class name
- The folder path above it is the package path
- Group by the second-level folder name (the pipeline name, e.g. `shaderpipeline`)

---

## Document Naming Conventions

All document file names use **PascalCase**. No spaces, no hyphens, no underscores.

| Document Type | Extension | Naming Pattern | Example |
|---|---|---|---|
| Pipeline reference | `.docx` | `[PipelineName]PipelineDoc.docx` | `ShaderPipelineDoc.docx` |
| Project index | `.docx` | `ProjectIndex.docx` | `ProjectIndex.docx` |
| Engine context / rules | `.md` | `[Topic]Context.md` | `EngineContext.md` |
| Documentation context (this file) | `.md` | `[Topic]DocumentationContext.md` | `EngineDocumentationContext.md` |
| Cleanup / refactor plans | `.md` | `[Topic]CleanupPlan.md` | `EngineCleanupPlanV3.md` |
| Raw tree output for Claude | `.txt` | `Index.txt` | `Index.txt` |

### Rules
- Version suffixes go at the end, no separator: `V2`, `V3` — e.g. `EngineCleanupPlanV3.md`
- Pipeline names match the package folder name in PascalCase: `shaderpipeline/` → `ShaderPipeline`
- Context files (`.md`) are for Claude and for you to read in chat
- Reference documents (`.docx`) are the deliverables — always created via the DOCX skill
