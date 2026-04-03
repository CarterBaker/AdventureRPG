# ClaudeEngineQuickContext

Use this as a short bootstrapping context for engine-aware assistance.

## Engine lifecycle (practical)
- `EnginePackage` is the root orchestrator: registry, lifecycle enforcement, context creation/pairing, timing.
- `GameEngine` wires bootstrap + runtime: creates `BootstrapAssembly`, resolves `WindowManager`/`RenderManager`, creates `RuntimeContext`, calls `renderManager.draw()`.
- `get()` phase is where cross-system references are resolved.

## Runtime execution
- `RuntimeContext` creates `SkySystem`, `PlayerSystem`, `MenuSystem`, `WorldSystem`, `PlayerInputSystem`.
- All runtime systems route by `context.getWindow()` (window-aware behavior, multi-window safe).
- Input flow: `PlayerInputSystem` -> `InputSystem` + `MenuManager` lock + `PlayerManager` input writes.

## Kernel substrate
- `InternalThreadManager` + loader/builder own named executors and async/sync execution helpers.
- `WindowManager` owns main/detached window lifecycle, pending opens, active window tracking.
- `WindowInstance` binds window data, render queue, cameras, and optional context pairing.

## Bootstrap dependency shape
- `BootstrapAssembly` order: Geometry -> Shader -> Render -> Item -> Physics -> Input -> Entity -> World -> Calendar -> Lighting -> Menu.
- Runtime-critical chain: Player spawn (Entity) + grid create (World) + pass push (Shader) + input/menu arbitration (Input+Menu+Entity).

## Use these manuals for full detail
- `CompleteEngineDoc.md` (file-by-file full manual)
- `EngineCoreDoc.md`, `KernelSystemsDoc.md`, `RuntimeSystemsDoc.md`
- Pipeline manuals (`*PipelineDoc.md`) for package-level behavior and API intent.
