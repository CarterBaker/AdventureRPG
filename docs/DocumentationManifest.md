# Documentation Manifest

Generated on 2026-04-03.

## Why this is Markdown-first
The repo/PR workflow rejects large binary uploads. To keep PRs reviewable and mergeable,
all architecture references are stored as text-first `*.md` source files. DOCX files are
now generated locally from these Markdown sources when needed.

## Engine document source
- `EngineCoreDoc.md` — core engine architecture, class-role breakdown, integration notes.

## Pipeline document sources
- `CalendarPipelineDoc.md`
- `EntityPipelineDoc.md`
- `GeometryPipelineDoc.md`
- `InputPipelineDoc.md`
- `ItemPipelineDoc.md`
- `LightingPipelineDoc.md`
- `MenuPipelineDoc.md`
- `PhysicsPipelineDoc.md`
- `RenderPipelineDoc.md`
- `ShaderPipelineDoc.md`
- `WorldPipelineDoc.md`

## Additional system documents (non-editor)
- `RuntimeSystemsDoc.md` — runtime systems and context orchestration.
- `KernelSystemsDoc.md` — thread/window/sync kernel system surface.
- `CorePlatformSystemsDoc.md` — app/backends/graphics/input/settings support layers.

## Local DOCX export (not committed)
Run:

```bash
python assets/documentation/scripts/export_markdown_to_docx.py
```

Generated files are written to:
- `assets/documentation/build-docx/*.docx`

## Quick context file
- `ClaudeEngineQuickContext.md` — concise copy/paste context for Claude with architecture summary, active pipelines, communication flow, and naming/lifecycle rules.
