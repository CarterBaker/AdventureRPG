# EntityPipelineDoc

_Generated: 2026-04-03_

## Overview
EntityPipelineDoc covers 21 classes across 9 packages. This reference follows EngineContext naming/lifecycle conventions and summarizes class responsibilities by suffix rules.

## Integration Notes
- `core.engine` referenced via 20 imports.
- `core.kernel` referenced via 1 imports.

## Package Breakdown

### `entitypipeline`

| Class | Role |
|---|---|
| `EntityPipeline` | Pipeline registration entry point that wires dependency order. |

### `entitypipeline.behavior`

| Class | Role |
|---|---|
| `BehaviorData` | Raw data payload struct used by handles/instances. |
| `BehaviorHandle` | Persistent manager-registered wrapper around data. |

### `entitypipeline.behaviormanager`

| Class | Role |
|---|---|
| `BehaviorManager` | Owns registration, lifecycle, and public retrieval. |
| `InternalBuilder` | Loader-owned internal builder helper. |
| `InternalLoader` | Manager-created internal loader helper. |

### `entitypipeline.entity`

| Class | Role |
|---|---|
| `EntityData` | Raw data payload struct used by handles/instances. |
| `EntityHandle` | Persistent manager-registered wrapper around data. |
| `EntityInstance` | Runtime mutable clone of a handle. |
| `EntityState` | Specialized support class for this subsystem. |
| `EntityStateHandle` | Persistent manager-registered wrapper around data. |

### `entitypipeline.entitymanager`

| Class | Role |
|---|---|
| `EntityManager` | Owns registration, lifecycle, and public retrieval. |
| `InternalBuilder` | Loader-owned internal builder helper. |
| `InternalLoader` | Manager-created internal loader helper. |

### `entitypipeline.inventory`

| Class | Role |
|---|---|
| `InventoryHandle` | Persistent manager-registered wrapper around data. |

### `entitypipeline.placementmanager`

| Class | Role |
|---|---|
| `BlockBranch` | Manager-owned internal computation branch. |
| `ItemBranch` | Manager-owned internal computation branch. |
| `PlacementManager` | Owns registration, lifecycle, and public retrieval. |

### `entitypipeline.playermanager`

| Class | Role |
|---|---|
| `InternalBufferSystem` | GPU buffer helper used by manager updates. |
| `PlayerManager` | Owns registration, lifecycle, and public retrieval. |

### `entitypipeline.statistics`

| Class | Role |
|---|---|
| `StatisticsHandle` | Persistent manager-registered wrapper around data. |

## Class Role Summary

| Class | Package | Role |
|---|---|---|
| `BehaviorData` | `program.bootstrap.entitypipeline.behavior` | Raw data payload struct used by handles/instances. |
| `BehaviorHandle` | `program.bootstrap.entitypipeline.behavior` | Persistent manager-registered wrapper around data. |
| `BehaviorManager` | `program.bootstrap.entitypipeline.behaviormanager` | Owns registration, lifecycle, and public retrieval. |
| `BlockBranch` | `program.bootstrap.entitypipeline.placementmanager` | Manager-owned internal computation branch. |
| `EntityData` | `program.bootstrap.entitypipeline.entity` | Raw data payload struct used by handles/instances. |
| `EntityHandle` | `program.bootstrap.entitypipeline.entity` | Persistent manager-registered wrapper around data. |
| `EntityInstance` | `program.bootstrap.entitypipeline.entity` | Runtime mutable clone of a handle. |
| `EntityManager` | `program.bootstrap.entitypipeline.entitymanager` | Owns registration, lifecycle, and public retrieval. |
| `EntityPipeline` | `program.bootstrap.entitypipeline` | Pipeline registration entry point that wires dependency order. |
| `EntityState` | `program.bootstrap.entitypipeline.entity` | Specialized support class for this subsystem. |
| `EntityStateHandle` | `program.bootstrap.entitypipeline.entity` | Persistent manager-registered wrapper around data. |
| `InternalBufferSystem` | `program.bootstrap.entitypipeline.playermanager` | GPU buffer helper used by manager updates. |
| `InternalBuilder` | `program.bootstrap.entitypipeline.behaviormanager` | Loader-owned internal builder helper. |
| `InternalBuilder` | `program.bootstrap.entitypipeline.entitymanager` | Loader-owned internal builder helper. |
| `InternalLoader` | `program.bootstrap.entitypipeline.behaviormanager` | Manager-created internal loader helper. |
| `InternalLoader` | `program.bootstrap.entitypipeline.entitymanager` | Manager-created internal loader helper. |
| `InventoryHandle` | `program.bootstrap.entitypipeline.inventory` | Persistent manager-registered wrapper around data. |
| `ItemBranch` | `program.bootstrap.entitypipeline.placementmanager` | Manager-owned internal computation branch. |
| `PlacementManager` | `program.bootstrap.entitypipeline.placementmanager` | Owns registration, lifecycle, and public retrieval. |
| `PlayerManager` | `program.bootstrap.entitypipeline.playermanager` | Owns registration, lifecycle, and public retrieval. |
| `StatisticsHandle` | `program.bootstrap.entitypipeline.statistics` | Persistent manager-registered wrapper around data. |

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
