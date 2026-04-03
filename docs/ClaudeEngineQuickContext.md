# ClaudeEngineQuickContext

Use this as a fast paste-in context for engine-aware assistance.

## Architecture in 30 seconds
- `EnginePackage` is the root lifecycle manager (`bootstrap -> create -> get -> awake -> update -> draw`).
- `GameEngine` boots `BootstrapAssembly`, then binds `RuntimeContext` to the main window.
- Pipelines are `PipelinePackage` registries; managers/systems do work and expose typed APIs.
- Data flow convention: `Loader -> Builder -> Data -> Handle -> Manager registry -> Instance clone (runtime)`.

## Active bootstrap pipelines
1. `CalendarPipeline` - world time/calendar state.
2. `EntityPipeline` - entities, behaviors, player spawn.
3. `GeometryPipeline` - VAO/VBO/IBO, mesh/model assembly, dynamic/composite geometry.
4. `InputPipeline` - input polling and input state exposure.
5. `ItemPipeline` - tool types, item definitions, rotations/backpack helpers.
6. `LightingPipeline` - natural light and directional light blending.
7. `MenuPipeline` - fonts, events, raycast, menu runtime.
8. `PhysicsPipeline` - movement + raycast/collision helpers.
9. `RenderPipeline` - camera + render batch flush.
10. `ShaderPipeline` - shader, pass, texture, material, sprite, UBO systems.
11. `WorldPipeline` - world/block/biome + generation + stream/render/item systems.

## Runtime + kernel additions
- Runtime classes live in `program.runtime` and run frame logic for world/player/menu/lighting/input orchestration.
- Kernel classes live in `program.core.kernel` and provide threading, sync consumers, and window management.
- Non-editor platform glue (`core.app`, `core.backends`, `core.graphics`, `core.input`, `core.settings`) hosts launch, settings, and backend integration.

## Cross-system communication pattern
- Pipelines communicate through manager retrieval in `get()` phase, not raw globals.
- `WorldStreamManager` updates before `WorldRenderManager`; render consumes current stream state.
- `RenderManager` is the final flush point for draw calls pushed by runtime systems.

## Naming + lifecycle rules to follow
- Suffix must match base type (`XManager`, `XHandle`, `XData`, `XSystem`, etc.).
- Engine-managed objects are created with `create(Class)`; only structs/data use `new`.
- `get()` is the only legal phase for resolving cross-system references.
- Use explicit registry maps (`name -> id`, `id -> handle`), never ad-hoc maps.
