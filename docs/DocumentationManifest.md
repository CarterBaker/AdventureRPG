# Documentation Manifest

Updated on 2026-04-03.

## Primary manuals (read in this order)
1. `CompleteEngineDoc.md` — full file-by-file manual for all Java source files (`core/src/program` + `lwjgl3/src`).
2. `EngineCoreDoc.md` — engine root lifecycle, context/window model, and registry/state behavior.
3. `KernelSystemsDoc.md` — threading/window substrate and async/sync execution contracts.
4. `RuntimeSystemsDoc.md` — runtime system startup/update behavior and player/menu/world/input interactions.
5. `BootstrapPipelineIntegrationDoc.md` — pipeline ordering and cross-pipeline runtime dependency map.

## Detailed subsystem manuals
- `CorePlatformSystemsDoc.md`
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

## Short copy/paste context for Claude
- `ClaudeEngineQuickContext.md`

## Optional DOCX export
```bash
python assets/documentation/scripts/export_markdown_to_docx.py
```
Output goes to `assets/documentation/build-docx/*.docx`.
