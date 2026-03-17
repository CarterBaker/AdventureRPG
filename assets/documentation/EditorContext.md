# AdventureRPG — Editor Context

---

## What Has Been Built

### Entry Points
- `MainEditor extends Game` — mirrors `Main`, routes through `EditorEngine`
- `EditorEngine extends EnginePackage` — concrete editor engine, bootstraps game foundation then editor shell
- `Lwjgl3LauncherEditor` — desktop launcher for the editor, mirrors `Lwjgl3Launcher`
- `editor.bat` — launches `gradlew :lwjgl3:runEditor`

### Engine Changes
- `EnginePackage` — `Main` reference replaced with `Game` so both `Main` and `MainEditor` are valid entry points. `getMain()` removed.
- `setupConstructor(Settings, Game, File, Gson)` — accepts any `Game` subclass now

### Bootstrap Structure
```
EditorEngine.bootstrap()
├── com.internal.bootstrap.BootstrapAssembly    ← full game foundation, identical to game
└── com.internal.editor.bootstrap.BootstrapAssembly  ← editor-specific bootstrap (stub, ready to populate)
```

### Runtime Structure
```
com.internal.runtime.RuntimeContext extends ContextPackage
├── SkySystem       ← pushes sky pass each frame
├── PlayerSystem    ← calls playerManager.spawnPlayer()
├── MenuSystem      ← calls mainMenuBranch.openMenu()
└── WorldSystem     ← calls worldStreamManager.createGrid(player)

com.internal.editor.runtime.RuntimeContext extends ContextPackage
└── (stub, ready to populate)
```

### BootstrapAssembly (Game)
```java
package com.internal.bootstrap;
// Pipelines: GeometryPipeline, ShaderPipeline, RenderPipeline, ItemPipeline,
//            WorldPipeline, PhysicsPipeline, InputPipeline, EntityPipeline,
//            CalendarPipeline, LightingPipeline, MenuPipeline
// draw() delegates to renderPipeline.draw()
```

### BootstrapAssembly (Editor)
```java
package com.internal.editor.bootstrap;
// Stub — ready to populate with editor-only pipelines
```

### ContextPackage Role
`ContextPackage extends ManagerPackage` is the base for all runtime contexts. A context is a collection
of runtime systems that populate a render pass. It is NOT a render target descriptor — it is what runs
inside a window each frame. The main game's `RuntimeContext`, the editor's `RuntimeContext`, and any
future panel contexts all extend `ContextPackage`.

Each window owns a `ContextPackage`. When a window's render loop fires, it runs its context's systems
(which push render calls) then calls `renderSystem.draw(windowHandle)` to flush them to that window.

### Camera Decoupling
- `CameraManager` — pure factory and registry. No longer drives rotation or holds `InputSystem` reference. `update()` removed entirely. `createCamera()` no longer auto-sets main. Main camera is null until explicitly registered.
- `PlayerManager` — creates its own camera in `spawnPlayer()`, sets it as main, drives rotation and position directly. `spawnPlayer()` is now an explicit public method rather than running in `awake()`.

### World Streaming Refactor
The world streaming system was redesigned to support multiple focal points (grids), enabling the editor
to stream world data around a free cam independently of the player.

**Key structural changes:**
- `WorldStreamManager` — single public entry point for all world streaming. Owns the grid registry. `createGrid(EntityInstance)`, `removeGrid`, `rebuildGrid` are the only public lifecycle methods.
- `GridInstance` — owns `activeChunks`, `activeMegaChunks`, `loadRequests`, `unloadRequests`, and the `EntityInstance` focal point. World handle comes from the entity directly.
- `GridManager` — pure factory. No longer holds a single grid. `buildGrid(EntityInstance)` only.
- `ChunkStreamManager` — internal, owned by `WorldStreamManager`. No `PlayerManager` reference.
- `MegaStreamManager` — internal, owned by `WorldStreamManager`. Per-grid operations.
- `ChunkQueueManager` / `MegaQueueManager` — all operations now take `GridInstance` as parameter. Pools are shared across all grids.
- `BatchBranch` — updated to pass `GridInstance` through to `MegaStreamManager.batchChunk()`

**Systems updated to use `WorldStreamManager` instead of `ChunkStreamManager`:**
- `WorldRenderManager`, `BlockCastBranch`, `BlockCollisionBranch`, `PlayerManager`, `BlockBranch`, `ItemBranch`

### New Base Packages
- `ContextPackage extends ManagerPackage` — base for all runtime contexts. A context owns the systems that run inside a window each frame.
- `AssemblyPackage extends ManagerPackage` — base for arbitrary groupings of managers/systems/pipelines with no particular relationship beyond always being created together.

---

## Current State

- Game boots and runs correctly
- Editor boots without crashing
- World streaming is grid-based and supports multiple focal points
- Camera is fully decoupled from input — owner drives it
- Runtime systems cleanly separated into `RuntimeContext` — bootstrap is untouched
- `WorldPipeline` registration order: `WorldStreamManager` before `WorldRenderManager`
- `RenderSystem` not yet context-aware — this is the immediate next work

---

## Architecture Decisions

### Editor Is A Superset Of The Game
The editor runs the full game bootstrap. Nothing is stripped. The simulation runs. The editor shell is
a layer on top — it does not replace the game, it wraps it.

### No Mode Flags
No `if (isEditor)` checks anywhere. Systems are either designed to handle null state gracefully, or the
editor provides its own runtime counterparts that register themselves the same way game systems do.

### Single Public Streaming Entry Point
Nothing outside `worldstreammanager` package calls `ChunkStreamManager` or `MegaStreamManager` directly.
`WorldStreamManager` is the only public facade.

### Camera Ownership
Whoever needs a camera calls `cameraManager.createCamera()` and owns what they get. `InternalBufferSystem`
null-guards on `mainCamera` — no camera registered means nothing pushes to GPU, which is correct.

### Context Is Runtime, Not Render Target
`ContextPackage` is the collection of systems that run inside a window — not a descriptor of a render
surface. The render surface question is handled separately at the `RenderSystem` level via window handles.
This separation keeps the two concerns clean: what runs vs where it draws.

### One RenderSystem — Always
There is one `RenderSystem` in the entire engine. It is a `SystemPackage` and the engine only allows
one of each. All systems push render calls to it as they always have. The only change needed is that
`draw()` accepts a window target so it knows which window's framebuffer to flush to.

### Multi-Window Via RenderSystem Overloads
```java
renderSystem.draw()                    // main window — uses WindowInstance, renders to screen
renderSystem.draw(WindowInstance w)    // specific detached window
```
Viewport dimensions come from whichever window is passed. LibGDX/LWJGL3 handles making that window's
GL context current before its render callback fires — we do not manage context switching manually.

### Windows Are Instances Not Handles
`WindowInstance` wraps a `WindowData` struct. The main game window is the single persistent reference.
Additional detached windows are registered at runtime in `WindowManager` and are `WindowInstance` objects —
not Handles, because they are created and destroyed at runtime rather than loaded at bootstrap.

---

## Multi-Window — How LibGDX LWJGL3 Actually Works

```java
((Lwjgl3Application) Gdx.app).newWindow(applicationListener, windowConfig);
```

Each window receives its own `ApplicationListener`. LibGDX calls `render()` on each window's listener
every frame and automatically makes that window's GL context current before doing so. We do not call
`makeCurrent()` anywhere. The only thing a render pass needs from a window is its viewport dimensions.

Panels within a window (scene view, game preview) that need their own render target will use FBOs.
That is separate from multi-window and is addressed when the panel system is built.

---

## Immediate Next Steps

### 1. Window System
Build `WindowData` → `WindowInstance` → `WindowManager`.
- `WindowData` holds width, height, title
- `WindowManager` owns the registry, wraps `Lwjgl3Application.newWindow()` on creation
- `WindowInstance` wraps `WindowData`, delegates getters through it per engine convention

### 2. RenderSystem — Surgical Change
- Remove `windowInstance` field (currently grabbed from `internal.getWindowInstance()`)
- Add `draw(WindowInstance window)` overload
- `draw()` with no args resolves the main window from `WindowManager` and delegates
- Both paths run the identical batch loop — only viewport dimensions differ

### 3. Editor Free Cam
- `EditorCameraSystem` — creates a camera, sets it as main, drives free cam movement
- Lives in `com.internal.editor.runtime`
- Reads mouse/keyboard independently of `InputSystem` (game-specific)
- Creates a grid via `worldStreamManager.createGrid(editorEntity)` where `editorEntity` is a
  lightweight invisible entity with just a world position

### 4. Editor RuntimeContext Population
```
com.internal.editor.runtime.RuntimeContext
└── EditorCameraSystem    ← free cam, sets main camera, creates streaming grid
```

### 5. Dock Shell — Phase 1 Foundation
Build in this exact order, nothing else until each step is solid:
1. `DockRenderSystem` — draws quads, borders, text to screen coords via `RenderSystem`
2. `DockNodeData` / `DockNodeInstance` — binary split tree node
3. `DockManager` — builds, walks, mutates the tree
4. `TabGroupHandle` / `TabGroupInstance` — tab bar data
5. `TabGroupManager` — tab switching, drag-out detection
6. `PanelManager` — panel registry, abstract `PanelSystem`
7. `FloatingWindowManager` — detached OS windows via `WindowManager`
8. `DockInputSystem` — splitter drag, tab drag, drop zones
9. `LayoutManager` — serialize/deserialize tree to named JSON profiles
10. `EditorUIPipeline` — wires all of the above, registered in editor bootstrap

### 6. Panel System Design
Each panel is a `SystemPackage` subclass that implements:
```java
public abstract void drawPanel(int x, int y, int width, int height);
```
Panels receive a pixel rect each frame and draw into it. They reach into whatever managers they need
directly. They do not own cameras or pipelines.

First panels to build (in order):
- `ScenePanel` — renders world via editor free cam into its rect (FBO)
- `HierarchyPanel` — entity list
- `InspectorPanel` — selected entity properties
- `AssetBrowserPanel` — taps MeshManager, TextureManager etc.

### 7. Play Panel
When the play panel is focused:
- `playerManager.spawnPlayer()` is called if no player exists
- Input routes to `PlayerManager` / `InputSystem`
- Game camera renders into the panel rect (FBO)

When unfocused:
- Input returns to editor free cam
- Game camera becomes secondary

---

## Multi-Grid Roadmap (Future)

### World Preview In Editor
- Editor free cam entity registered as a grid focal point
- Streams world around free cam position independently of player
- Same generation, same data, different streaming origin

### Multiple Simultaneous Views
The `WorldStreamManager` grid loop already supports this. Adding a second grid is one `createGrid()` call.
Panel FBOs gate this — each view needs its own render target.

### Split Screen (Modder Feature)
Not officially supported but architecturally possible. Two grids, two cameras, two windows or two FBOs.
Modders could wire this up without engine changes.

---

## Naming Convention Additions

| Class Name | Must Extend |
|---|---|
| `XContext` | `ContextPackage` |
| `XAssembly` | `AssemblyPackage` |

These follow the same strict last-word rule as all other engine packages.

---

## Files Changed In This Session (Original)

| File | Change |
|---|---|
| `EnginePackage` | `Main` → `Game`, removed `getMain()` |
| `GameEngine` | Now uses `BootstrapAssembly` + `RuntimeContext` |
| `EditorEngine` | New — editor-specific engine |
| `MainEditor` | New — editor entry point |
| `Lwjgl3LauncherEditor` | New — desktop launcher |
| `editor.bat` | New — launch script |
| `build.gradle (lwjgl3)` | Added `runEditor` task with Java 21 toolchain |
| `CameraManager` | Stripped to pure factory, no input coupling |
| `PlayerManager` | Owns camera, `spawnPlayer()` explicit |
| `WorldPipeline` | `WorldStreamManager` before `WorldRenderManager` |
| `WorldStreamManager` | New — single streaming facade |
| `ChunkStreamManager` | Internal, per-grid |
| `MegaStreamManager` | Internal, per-grid |
| `ChunkQueueManager` | Per-grid operations |
| `MegaQueueManager` | Per-grid operations |
| `GridInstance` | Owns all chunk state, entity focal point |
| `GridBuildSystem` | Takes `EntityInstance` |
| `GridManager` | Pure factory |
| `WorldRenderManager` | Uses `WorldStreamManager`, loops all grids |
| `BlockCastBranch` | Uses `WorldStreamManager` |
| `BlockCollisionBranch` | Uses `WorldStreamManager` |
| `BlockBranch` | Uses `WorldStreamManager` |
| `ItemBranch` | Uses `WorldStreamManager` |
| `BatchBranch` | Takes `GridInstance` |
| `RuntimeContext (game)` | New structure — `SkySystem`, `PlayerSystem`, `MenuSystem`, `WorldSystem` |
| `RuntimeContext (editor)` | New stub |
| `PlayerSystem` | New |
| `MenuSystem` | New |
| `WorldSystem` | New |
| `SkySystem` | Renamed from `Sky`, moved to `runtime/lighting` |
| `ContextPackage` | New base package — runtime context base |
| `AssemblyPackage` | New base package — pipeline grouping base |

## Files Pending (Next Session)

| File | Change |
|---|---|
| `WindowData` | New — width, height, title payload |
| `WindowInstance` | New — runtime window wrapper |
| `WindowManager` | New — registry, wraps `Lwjgl3Application.newWindow()` |
| `RenderSystem` | Remove `windowInstance`, add `draw(WindowInstance)` overload |
| `EditorCameraSystem` | New — free cam, sets main camera, creates streaming grid |
| `RuntimeContext (editor)` | Populate with `EditorCameraSystem` |
