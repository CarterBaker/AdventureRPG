# MenuPipelineDoc

_Generated: 2026-04-03_

## Overview
MenuPipelineDoc covers 35 classes across 11 packages. This reference follows EngineContext naming/lifecycle conventions and summarizes class responsibilities by suffix rules.

## Integration Notes
- `core.engine` referenced via 30 imports.
- `core.kernel` referenced via 4 imports.

## Package Breakdown

### `menupipeline`

| Class | Role |
|---|---|
| `MenuPipeline` | Pipeline registration entry point that wires dependency order. |

### `menupipeline.element`

| Class | Role |
|---|---|
| `ElementData` | Raw data payload struct used by handles/instances. |
| `ElementHandle` | Persistent manager-registered wrapper around data. |
| `ElementInstance` | Runtime mutable clone of a handle. |
| `ElementOrigin` | Specialized support class for this subsystem. |
| `ElementPlacementStruct` | Lightweight struct without engine lifecycle. |
| `ElementType` | Enum/type descriptor used for branching. |

### `menupipeline.fontmanager`

| Class | Role |
|---|---|
| `FontManager` | Owns registration, lifecycle, and public retrieval. |
| `FontRasterizerUtility` | Static helper utility. |
| `GLSLUtility` | Stateless OpenGL helper utility for shader-side logic. |
| `InternalBuilder` | Loader-owned internal builder helper. |
| `InternalLoader` | Manager-created internal loader helper. |

### `menupipeline.fonts`

| Class | Role |
|---|---|
| `FontHandle` | Persistent manager-registered wrapper around data. |
| `FontInstance` | Runtime mutable clone of a handle. |
| `FontTileData` | Raw data payload struct used by handles/instances. |
| `GlyphMetricStruct` | Lightweight struct without engine lifecycle. |

### `menupipeline.menu`

| Class | Role |
|---|---|
| `MenuData` | Raw data payload struct used by handles/instances. |
| `MenuHandle` | Persistent manager-registered wrapper around data. |
| `MenuInstance` | Runtime mutable clone of a handle. |

### `menupipeline.menueventsmanager`

| Class | Role |
|---|---|
| `MenuEventsManager` | Owns registration, lifecycle, and public retrieval. |

### `menupipeline.menueventsmanager.menus`

| Class | Role |
|---|---|
| `InventoryBranch` | Manager-owned internal computation branch. |
| `MainMenuBranch` | Manager-owned internal computation branch. |

### `menupipeline.menueventsmanager.util`

| Class | Role |
|---|---|
| `GenericButtonBranch` | Manager-owned internal computation branch. |

### `menupipeline.menumanager`

| Class | Role |
|---|---|
| `ElementSystem` | Single-job helper system for focused tasks. |
| `FileParserUtility` | Static helper utility. |
| `InternalBuilder` | Loader-owned internal builder helper. |
| `InternalLoader` | Manager-created internal loader helper. |
| `MenuManager` | Owns registration, lifecycle, and public retrieval. |

### `menupipeline.raycastsystem`

| Class | Role |
|---|---|
| `RaycastSystem` | Single-job helper system for focused tasks. |

### `menupipeline.util`

| Class | Role |
|---|---|
| `DimensionValue` | Specialized support class for this subsystem. |
| `DimensionVector2` | Specialized support class for this subsystem. |
| `LayoutStruct` | Lightweight struct without engine lifecycle. |
| `MenuAwareAction` | Specialized support class for this subsystem. |
| `StackDirection` | Specialized support class for this subsystem. |
| `TextAlign` | Specialized support class for this subsystem. |

## Class Role Summary

| Class | Package | Role |
|---|---|---|
| `DimensionValue` | `program.bootstrap.menupipeline.util` | Specialized support class for this subsystem. |
| `DimensionVector2` | `program.bootstrap.menupipeline.util` | Specialized support class for this subsystem. |
| `ElementData` | `program.bootstrap.menupipeline.element` | Raw data payload struct used by handles/instances. |
| `ElementHandle` | `program.bootstrap.menupipeline.element` | Persistent manager-registered wrapper around data. |
| `ElementInstance` | `program.bootstrap.menupipeline.element` | Runtime mutable clone of a handle. |
| `ElementOrigin` | `program.bootstrap.menupipeline.element` | Specialized support class for this subsystem. |
| `ElementPlacementStruct` | `program.bootstrap.menupipeline.element` | Lightweight struct without engine lifecycle. |
| `ElementSystem` | `program.bootstrap.menupipeline.menumanager` | Single-job helper system for focused tasks. |
| `ElementType` | `program.bootstrap.menupipeline.element` | Enum/type descriptor used for branching. |
| `FileParserUtility` | `program.bootstrap.menupipeline.menumanager` | Static helper utility. |
| `FontHandle` | `program.bootstrap.menupipeline.fonts` | Persistent manager-registered wrapper around data. |
| `FontInstance` | `program.bootstrap.menupipeline.fonts` | Runtime mutable clone of a handle. |
| `FontManager` | `program.bootstrap.menupipeline.fontmanager` | Owns registration, lifecycle, and public retrieval. |
| `FontRasterizerUtility` | `program.bootstrap.menupipeline.fontmanager` | Static helper utility. |
| `FontTileData` | `program.bootstrap.menupipeline.fonts` | Raw data payload struct used by handles/instances. |
| `GLSLUtility` | `program.bootstrap.menupipeline.fontmanager` | Stateless OpenGL helper utility for shader-side logic. |
| `GenericButtonBranch` | `program.bootstrap.menupipeline.menueventsmanager.util` | Manager-owned internal computation branch. |
| `GlyphMetricStruct` | `program.bootstrap.menupipeline.fonts` | Lightweight struct without engine lifecycle. |
| `InternalBuilder` | `program.bootstrap.menupipeline.fontmanager` | Loader-owned internal builder helper. |
| `InternalBuilder` | `program.bootstrap.menupipeline.menumanager` | Loader-owned internal builder helper. |
| `InternalLoader` | `program.bootstrap.menupipeline.fontmanager` | Manager-created internal loader helper. |
| `InternalLoader` | `program.bootstrap.menupipeline.menumanager` | Manager-created internal loader helper. |
| `InventoryBranch` | `program.bootstrap.menupipeline.menueventsmanager.menus` | Manager-owned internal computation branch. |
| `LayoutStruct` | `program.bootstrap.menupipeline.util` | Lightweight struct without engine lifecycle. |
| `MainMenuBranch` | `program.bootstrap.menupipeline.menueventsmanager.menus` | Manager-owned internal computation branch. |
| `MenuAwareAction` | `program.bootstrap.menupipeline.util` | Specialized support class for this subsystem. |
| `MenuData` | `program.bootstrap.menupipeline.menu` | Raw data payload struct used by handles/instances. |
| `MenuEventsManager` | `program.bootstrap.menupipeline.menueventsmanager` | Owns registration, lifecycle, and public retrieval. |
| `MenuHandle` | `program.bootstrap.menupipeline.menu` | Persistent manager-registered wrapper around data. |
| `MenuInstance` | `program.bootstrap.menupipeline.menu` | Runtime mutable clone of a handle. |
| `MenuManager` | `program.bootstrap.menupipeline.menumanager` | Owns registration, lifecycle, and public retrieval. |
| `MenuPipeline` | `program.bootstrap.menupipeline` | Pipeline registration entry point that wires dependency order. |
| `RaycastSystem` | `program.bootstrap.menupipeline.raycastsystem` | Single-job helper system for focused tasks. |
| `StackDirection` | `program.bootstrap.menupipeline.util` | Specialized support class for this subsystem. |
| `TextAlign` | `program.bootstrap.menupipeline.util` | Specialized support class for this subsystem. |

## Naming Convention Reference

| Suffix | Meaning |
|---|---|
| `Data` | Raw data payload struct used by handles/instances. |
| `Handle` | Persistent manager-registered wrapper around data. |
| `Instance` | Runtime mutable clone of a handle. |
| `Manager` | Owns registration, lifecycle, and public retrieval. |
| `Loader` | Bootstrap loader that scans files and requests builds. |
| `Builder` | Bootstrap builder that parses source data into handles. |
| `Branch` | Manager-owned internal computation branch. |
| `System` | Single-job helper system for focused tasks. |
| `Struct` | Lightweight struct without engine lifecycle. |
