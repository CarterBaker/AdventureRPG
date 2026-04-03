# PhysicsPipelineDoc

_Generated: 2026-04-03_

## Overview
PhysicsPipelineDoc covers 11 classes across 4 packages. This reference follows EngineContext naming/lifecycle conventions and summarizes class responsibilities by suffix rules.

## Integration Notes
- `core.engine` referenced via 11 imports.
- `core.kernel` referenced via 1 imports.

## Package Breakdown

### `physicspipeline`

| Class | Role |
|---|---|
| `PhysicsPipeline` | Pipeline registration entry point that wires dependency order. |

### `physicspipeline.movementmanager`

| Class | Role |
|---|---|
| `BlockCollisionBranch` | Manager-owned internal computation branch. |
| `GravityBranch` | Manager-owned internal computation branch. |
| `MovementBranch` | Manager-owned internal computation branch. |
| `MovementManager` | Owns registration, lifecycle, and public retrieval. |

### `physicspipeline.raycastmanager`

| Class | Role |
|---|---|
| `BlockCastBranch` | Manager-owned internal computation branch. |
| `RaycastManager` | Owns registration, lifecycle, and public retrieval. |
| `ScreenCastBranch` | Manager-owned internal computation branch. |

### `physicspipeline.util`

| Class | Role |
|---|---|
| `BlockCastStruct` | Lightweight struct without engine lifecycle. |
| `BlockCompositionStruct` | Lightweight struct without engine lifecycle. |
| `ScreenRayStruct` | Lightweight struct without engine lifecycle. |

## Class Role Summary

| Class | Package | Role |
|---|---|---|
| `BlockCastBranch` | `program.bootstrap.physicspipeline.raycastmanager` | Manager-owned internal computation branch. |
| `BlockCastStruct` | `program.bootstrap.physicspipeline.util` | Lightweight struct without engine lifecycle. |
| `BlockCollisionBranch` | `program.bootstrap.physicspipeline.movementmanager` | Manager-owned internal computation branch. |
| `BlockCompositionStruct` | `program.bootstrap.physicspipeline.util` | Lightweight struct without engine lifecycle. |
| `GravityBranch` | `program.bootstrap.physicspipeline.movementmanager` | Manager-owned internal computation branch. |
| `MovementBranch` | `program.bootstrap.physicspipeline.movementmanager` | Manager-owned internal computation branch. |
| `MovementManager` | `program.bootstrap.physicspipeline.movementmanager` | Owns registration, lifecycle, and public retrieval. |
| `PhysicsPipeline` | `program.bootstrap.physicspipeline` | Pipeline registration entry point that wires dependency order. |
| `RaycastManager` | `program.bootstrap.physicspipeline.raycastmanager` | Owns registration, lifecycle, and public retrieval. |
| `ScreenCastBranch` | `program.bootstrap.physicspipeline.raycastmanager` | Manager-owned internal computation branch. |
| `ScreenRayStruct` | `program.bootstrap.physicspipeline.util` | Lightweight struct without engine lifecycle. |

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
