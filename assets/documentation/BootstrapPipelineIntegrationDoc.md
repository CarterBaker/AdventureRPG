# BootstrapPipelineIntegrationDoc

_Updated: 2026-04-03_

## Why this file exists

Per-pipeline class inventories are useful, but they do not explain **how the pipelines depend on each other**.
This document is the dependency and communication map for bootstrap.

---

## 1) Assembly order and why it is that order

`BootstrapAssembly` creates pipelines in this exact sequence:

1. Geometry
2. Shader
3. Render
4. Item
5. Physics
6. Input
7. Entity
8. World
9. Calendar
10. Lighting
11. Menu

The order front-loads rendering primitives before gameplay/state systems.
By the time runtime wakes, world/player/menu can resolve their managers safely.

---

## 2) Pipeline roles (practical)

## Geometry
GPU-facing geometry resources (VBO/IBO/VAO, mesh/model assembly, dynamic/composite geometry).

## Shader
Shader programs, UBOs, textures, materials, sprites, render passes.

## Render
Camera + render queue orchestration and final draw flush integration.

## Item
Tool types, item definitions, rotation logic, inventory-support structures.

## Physics
Movement/raycast services consumed by entity/world gameplay systems.

## Input
Raw input collection and state query API used by runtime player input.

## Entity
Behaviors/entities/player ownership and per-window player lookup/spawn logic.

## World
World/block/biome defs, generation, stream management, world rendering systems.

## Calendar
Calendar + clock state (world time model).

## Lighting
Natural light management (sun/moon blending and related directional state).

## Menu
Fonts, menu definitions, menu events/branches, UI raycast and input locking.

---

## 3) Runtime-facing dependencies

At runtime, the most important cross-pipeline dependency chain is:

- `PlayerSystem` -> `PlayerManager` (Entity)
- `WorldSystem` -> `WorldStreamManager` + player lookup (World + Entity)
- `SkySystem` -> `PassManager` (Shader)
- `PlayerInputSystem` -> `InputSystem` + `MenuManager` + `InventoryBranch` + `PlayerManager` (Input + Menu + Entity)

This is the minimal chain that must be healthy for "spawn + control + world + UI" to work.

---

## 4) Known synchronization points

- Menu input lock gates player input writes.
- World stream grid creation depends on spawned player for current window.
- Render pass push can happen from multiple systems, but final flush is centralized.

---

## 5) If adding a new pipeline

1. Add to `BootstrapAssembly` in the correct dependency position.
2. Keep manager creation order explicit inside the new pipeline.
3. Resolve external dependencies only in `get()`.
4. Add runtime system wiring only if behavior must execute every frame.
5. Update this file with dependency impact and startup ordering notes.
