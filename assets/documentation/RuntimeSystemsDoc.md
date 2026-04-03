# RuntimeSystemsDoc

_Generated: 2026-04-03_

## Overview
RuntimeSystemsDoc covers 6 classes across 6 packages. This reference follows EngineContext naming/lifecycle conventions and summarizes class responsibilities by suffix rules.

## Integration Notes
- `core.engine` referenced via 6 imports.
- `runtime` referenced via 5 imports.
- `entitypipeline` referenced via 4 imports.
- `menupipeline` referenced via 3 imports.
- `inputpipeline` referenced via 2 imports.
- `shaderpipeline` referenced via 2 imports.
- `worldpipeline` referenced via 1 imports.

## Package Breakdown

### ``

| Class | Role |
|---|---|
| `RuntimeContext` | Runtime context that groups frame-executed systems. |

### `input`

| Class | Role |
|---|---|
| `PlayerInputSystem` | Single-job helper system for focused tasks. |

### `lighting`

| Class | Role |
|---|---|
| `SkySystem` | Single-job helper system for focused tasks. |

### `menu`

| Class | Role |
|---|---|
| `MenuSystem` | Single-job helper system for focused tasks. |

### `player`

| Class | Role |
|---|---|
| `PlayerSystem` | Single-job helper system for focused tasks. |

### `world`

| Class | Role |
|---|---|
| `WorldSystem` | Single-job helper system for focused tasks. |

## Class Role Summary

| Class | Package | Role |
|---|---|---|
| `MenuSystem` | `program.runtime.menu` | Single-job helper system for focused tasks. |
| `PlayerInputSystem` | `program.runtime.input` | Single-job helper system for focused tasks. |
| `PlayerSystem` | `program.runtime.player` | Single-job helper system for focused tasks. |
| `RuntimeContext` | `program.runtime` | Runtime context that groups frame-executed systems. |
| `SkySystem` | `program.runtime.lighting` | Single-job helper system for focused tasks. |
| `WorldSystem` | `program.runtime.world` | Single-job helper system for focused tasks. |

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
