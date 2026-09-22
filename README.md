# AdventureRPG Engine

A custom game engine written in Java, built on top of LWJGL for raw GPU access. The engine was designed from scratch with a strict architectural contract — every system has a defined role, a defined lifetime, and a defined place in the hierarchy. There are no shortcuts and no framework magic. LibGDX was the original base; everything above the GPU layer was rewritten, and the libGDX layer was eventually stripped out entirely.

---

## What it is

This is not a wrapped engine. It is a ground-up implementation of:

- **A custom lifecycle and registry system** — kernel, bootstrap, create, get, awake, release, start, update, fixed update, late update, render, draw, dispose. Each phase is enforced at runtime. Systems that try to resolve dependencies outside `get()` or register outside `create()` throw immediately with a clear error.
- **A strict class hierarchy** — `ManagerPackage`, `SystemPackage`, `BranchPackage`, `ContextPackage`, `HandlePackage`, `InstancePackage`, `LoaderPackage`, `BuilderPackage`. Every class name encodes its role. No exceptions.
- **A context system** — runtime contexts attach to windows and run independently. The same `RuntimeContext` code runs in the standalone game paired with the main window, or inside the editor paired with a preview tab or detached OS window. Closing a context in the editor tears down only that preview. Closing it in the game exits the engine.
- **A menu pipeline** — UI elements defined in JSON, resolved at runtime through a raycast hit system that reflects button actions by class and method name. Actions are routed to registered branch systems.
- **A material and shader system** — shaders are parsed and managed through the engine's asset pipeline. Materials are JSON-driven.
- **A mesh generation and definition system** — meshes are defined externally and constructed through the engine's loader and builder pipeline. VAO, VBO, and IBO management is handled internally.
- **World streaming** — chunk-based world loading and unloading driven by player position. Designed to be seamless.
- **A thread pipeline** — async work is submitted through typed thread handles. No raw `Thread` usage anywhere in application code.
- **An editor** — a separate `EditorEngine` implementation that bootstraps both the game pipeline and the editor-specific pipeline. Supports multiple runtime preview windows and tabs, each running full `RuntimeContext` instances independently.
- **Everything JSON-driven** — meshes, materials, shaders, UI layouts, world definitions, behaviors. The engine reads, parses, and registers them all through a consistent loader/builder pattern.
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

## Structures

Structures live in `assets/structures/` and are named by their path without the extension (for example `settlements/Village`). They load through the usual loader/builder pair: batched from boot, and on demand the moment generation asks for one by name. Placement is a pure function of the world seed, so every chunk and every worker agree on where things are.

`"type"` is required and picks the body section:

| Type | Body | What it is |
|---|---|---|
| `STRUCTURE` | `template` | One block template. It can place itself, or only appear inside a layout. |
| `SETTLEMENT` | `layout` | Streets with buildings lining them and fill structures in between. Surface by default. |
| `DUNGEON` | `layout` | Same generator, but underground by default: flat corridors plus an entrance passage up to the surface. |
| `ROAD` | `road` | The blocks and shape rules for a path. Referenced by `road_link` and by layout streets. |

**Shared, optional on every placeable type**
- `"elevation"`: `SURFACE`, `UNDERGROUND` or `ABSOLUTE`.
- `"spawn"`: procedural placement. Fields are `biomes` (empty means any), `frequency` (chance per cell), `spacing_chunks` (cell size), `min_height`/`max_height`, `depth` `[min, max]` (underground only), `max_slope` and `allow_water`. Leave out `spawn` to place a structure only at its locations or inside layouts.
- `"locations"`: hand-placed spots, either `{ "x", "z" }` in blocks or `{ "pixel": [px, pz] }` on the world map. Optional `"y"` and `"rotation"` (0-3). A location always spawns and wins every overlap.
- `"road_link"`: `{ "road": "roads/DirtRoad", "max_distance", "max_connections" }`. Joins the road network by linking to the nearest connectable neighbours. Leave it out for isolated structures.

**`template`**: `"layers"` run bottom to top. Each layer is a list of rows (row 0 is the front, which faces the street or road), and each row is read along +X. Every character maps through `"palette"` to a block name (`"air"` is allowed). A space means "leave the terrain alone". `"anchor"` `[x, y, z]` is the cell placed on the ground point, and its `y` is the floor layer. `"foundation"` fills down to the ground under solid bottom cells.

**`road`**: `width`, `surface` (a block name or weighted `[{ "block", "weight" }]`), `edge`, `foundation`, `clearance`, `max_fill`, `max_grade`, `smoothing`, `tunnel` `{ height, wall, ceiling }`, `bridge` `{ deck, rail, support, support_spacing }` and `always_tunnel` (for enclosed dungeon corridors). Routes come from an A* search over the terrain, so they wind and switch back up slopes. Heights are grade-limited, and a point becomes a bridge when the road ends up well above the ground and a tunnel when it ends up well below.

**`layout`**: `street` (a ROAD), `radius`, `main_street_length`, `branches` `{ count, length, depth }`, `lot_spacing`, `lot_setback`, `landmarks` (placed first, nearest the centre, once each), `buildings` and `fill` (weighted `{ "structure", "weight" }`), `fill_attempts`, `entrance`, `entrance_road` and `entrance_length`.

---

## Project modules

- `core` — all engine and application logic.
- `lwjgl3` — desktop launcher. Drives the LWJGL application loop and delegates to the engine.

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