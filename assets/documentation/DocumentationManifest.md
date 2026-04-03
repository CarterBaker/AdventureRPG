# Documentation Manifest

Updated on 2026-04-03 after architecture cleanup.

## Primary architecture docs (human-written)

These are the docs to read first. They explain behavior and system communication, not just class inventories.

1. `EngineCoreDoc.md`
   - Engine lifecycle, context/window model, bootstrap relationship, debugging path.
2. `BootstrapPipelineIntegrationDoc.md`
   - Pipeline dependency order and cross-pipeline runtime contracts.
3. `RuntimeSystemsDoc.md`
   - Runtime startup/update behavior and window-aware system interactions.
4. `KernelSystemsDoc.md`
   - Threading + window kernel substrate and operational guardrails.
5. `CorePlatformSystemsDoc.md`
   - App/backend/input/graphics/settings support layer overview.

## Reference inventories

Detailed per-pipeline references are still available:
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

## Quick paste context

- `ClaudeEngineQuickContext.md`

## Optional DOCX export (local only)

```bash
python assets/documentation/scripts/export_markdown_to_docx.py
```

Output:
- `assets/documentation/build-docx/*.docx`
