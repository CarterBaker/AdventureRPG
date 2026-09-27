# AdventureRPG Engine

A custom game engine written in Java, built on top of LWJGL for raw GPU access. The engine was designed from scratch with a strict architectural contract — every system has a defined role, a defined lifetime, and a defined place in the hierarchy. There are no shortcuts and no framework magic. LibGDX was the original base; everything above the GPU layer was rewritten, and the libGDX layer was eventually stripped out entirely.

---

## What it is

This is not a wrapped engine. It is a ground-up implementation of:

- **A custom lifecycle and registry system** — kernel, bootstrap, create, get, awake, release, start, update, fixed update, late update, render, draw, dispose. Each phase is enforced at runtime. Systems that try to resolve dependencies outside `get()` or register outside `create()` throw immediately with a clear error.
- **A strict class hierarchy** — `ManagerPackage`, `SystemPackage`, `BranchPackage`, `ContextPackage`, `HandlePackage`, `InstancePackage`, `LoaderPackage`, `BuilderPackage`. Every class name encodes its role. No exceptions.
- **A context system** — runtime contexts attach to windows and run independently. The same `RuntimeContext` code runs in the standalone game paired with the main window, or inside the editor paired with a preview tab or detached OS window. Closing a context in the editor tears down only that preview. Closing it in the game exits the engine.
- **A menu pipeline** — UI elements defined in ARPG data files, resolved at runtime through a raycast hit system that reflects button actions by class and method name. Actions are routed to registered branch systems.
- **A material and shader system** — shaders are parsed and managed through the engine's asset pipeline. Materials are data-driven.
- **A mesh generation and definition system** — meshes are defined externally and constructed through the engine's loader and builder pipeline. VAO, VBO, and IBO management is handled internally.
- **World streaming** — chunk-based world loading and unloading driven by player position. Designed to be seamless.
- **A thread pipeline** — async work is submitted through typed thread handles. No raw `Thread` usage anywhere in application code.
- **An editor** — a separate `EditorEngine` implementation that bootstraps both the game pipeline and the editor-specific pipeline. Supports multiple runtime preview windows and tabs, each running full `RuntimeContext` instances independently.
- **Everything data-driven, sealed in `.arpg` files** — meshes, materials, shaders, UI layouts, world definitions, behaviors, settings, saves. Every data file is a compact binary document guarded by a hardware-accelerated CRC32C checksum, decoded in place with no copying. All reads and writes go through `ArpgUtility`, and a corrupted or truncated file is rejected instead of loaded. The engine registers everything through a consistent loader/builder pattern. There is no Gson or other JSON library.
- **All constants in `EngineSetting`** — no magic numbers in logic code. Every tuning value, path, limit, and named constant lives in one place.

---

## Architecture overview

```
EnginePackage                  ← master registry, game loop, lifecycle root
├── KernelAssembly             ← thread pipeline, window manager, core infrastructure
├── BootstrapAssembly          ← render pipeline, asset loaders, menu pipeline
└── ContextPackage             ← attaches to a window, owns runtime systems
    ├── MenuEventsManager      ← button action handlers
    ├── MenuSystem             ← UI state, hit testing, element rendering
    ├── WorldSystem            ← chunk streaming, entity management
    ├── PlayerSystem           ← input, movement, camera
    └── ...
```

Systems are registered into a global engine registry or a context-local registry depending on where they are created. Context-local lookups check the local registry first before falling back to the engine registry. Systems are never instantiated with `new` — only through `create(Class)`.

---

## Project modules

- `core` — all engine and application logic.
- `lwjgl3` — desktop launcher. Drives the LWJGL application loop and delegates to the engine.

---

## Naming conventions

All code and assets follow standard Java naming, so paths match exactly on every platform and in git, which is
case-sensitive even where the filesystem is not:

- **Folders and packages** are all lowercase: `core/src/engine/util/arpg`, `assets/processingpasses`.
- **Files** are PascalCase: `SeasonManager.java`, `Spring.arpg`, `FBOs.arpg`, `Button.png`. Acronyms stay
  upper case (`UBOs.arpg`, `DebugUV.arpg`). A texture's variant suffix follows one underscore (`Skin_A.png`).
- **A file's name is its resource name**, so a reference in data must match its case exactly: a calendar season
  `"Spring"` loads `seasons/Spring.arpg`.
- **To change only the case of a name**, rename in two steps (`Spring.arpg` → `SpringTemp.arpg` → `Spring.arpg`)
  or use `git mv`. A direct case-only rename on Windows or macOS is invisible to git.

---

## Getting started

This project uses [Gradle](https://gradle.org/) with the included wrapper.

```
Windows:      gradlew.bat <task>
macOS/Linux:  ./gradlew <task>
```

## Common tasks

| Task | Description |
|---|---|
| `lwjgl3:run` | Run the desktop application |
| `lwjgl3:jar` | Build a runnable JAR (`lwjgl3/build/libs`) |
| `lwjgl3:arpg` | ARPG data tool: `encode`, `decode`, `convert`, `export`, `verify` |
| `build` | Compile and package all modules |
| `clean` | Remove build output |

Scope any task to a specific module with `<module>:<task>` — for example, `core:clean`.

## Helpful flags

| Flag | Description |
|---|---|
| `--daemon` | Use the Gradle daemon |
| `--offline` | Use cached dependencies only |
| `--refresh-dependencies` | Re-resolve all dependencies |
| `--continue` | Continue past task failures |