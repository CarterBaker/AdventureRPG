# ItemPipelineDoc

_Generated: 2026-04-03_

## Overview
ItemPipelineDoc covers 15 classes across 8 packages. This reference follows EngineContext naming/lifecycle conventions and summarizes class responsibilities by suffix rules.

## Integration Notes
- `core.engine` referenced via 15 imports.

## Package Breakdown

### `itempipeline`

| Class | Role |
|---|---|
| `ItemPipeline` | Pipeline registration entry point that wires dependency order. |

### `itempipeline.backpack`

| Class | Role |
|---|---|
| `BackpackInstance` | Runtime mutable clone of a handle. |

### `itempipeline.itemdefinition`

| Class | Role |
|---|---|
| `ItemDefinitionData` | Raw data payload struct used by handles/instances. |
| `ItemDefinitionHandle` | Persistent manager-registered wrapper around data. |

### `itempipeline.itemdefinitionmanager`

| Class | Role |
|---|---|
| `InternalBuilder` | Loader-owned internal builder helper. |
| `InternalLoader` | Manager-created internal loader helper. |
| `ItemDefinitionManager` | Owns registration, lifecycle, and public retrieval. |

### `itempipeline.itemrotationmanager`

| Class | Role |
|---|---|
| `InternalBufferSystem` | GPU buffer helper used by manager updates. |
| `ItemRotationManager` | Owns registration, lifecycle, and public retrieval. |

### `itempipeline.tooltype`

| Class | Role |
|---|---|
| `ToolTypeData` | Raw data payload struct used by handles/instances. |
| `ToolTypeHandle` | Persistent manager-registered wrapper around data. |

### `itempipeline.tooltypemanager`

| Class | Role |
|---|---|
| `InternalBuilder` | Loader-owned internal builder helper. |
| `InternalLoader` | Manager-created internal loader helper. |
| `ToolTypeManager` | Owns registration, lifecycle, and public retrieval. |

### `itempipeline.util`

| Class | Role |
|---|---|
| `ItemRegistryUtility` | Static helper utility. |

## Class Role Summary

| Class | Package | Role |
|---|---|---|
| `BackpackInstance` | `program.bootstrap.itempipeline.backpack` | Runtime mutable clone of a handle. |
| `InternalBufferSystem` | `program.bootstrap.itempipeline.itemrotationmanager` | GPU buffer helper used by manager updates. |
| `InternalBuilder` | `program.bootstrap.itempipeline.itemdefinitionmanager` | Loader-owned internal builder helper. |
| `InternalBuilder` | `program.bootstrap.itempipeline.tooltypemanager` | Loader-owned internal builder helper. |
| `InternalLoader` | `program.bootstrap.itempipeline.itemdefinitionmanager` | Manager-created internal loader helper. |
| `InternalLoader` | `program.bootstrap.itempipeline.tooltypemanager` | Manager-created internal loader helper. |
| `ItemDefinitionData` | `program.bootstrap.itempipeline.itemdefinition` | Raw data payload struct used by handles/instances. |
| `ItemDefinitionHandle` | `program.bootstrap.itempipeline.itemdefinition` | Persistent manager-registered wrapper around data. |
| `ItemDefinitionManager` | `program.bootstrap.itempipeline.itemdefinitionmanager` | Owns registration, lifecycle, and public retrieval. |
| `ItemPipeline` | `program.bootstrap.itempipeline` | Pipeline registration entry point that wires dependency order. |
| `ItemRegistryUtility` | `program.bootstrap.itempipeline.util` | Static helper utility. |
| `ItemRotationManager` | `program.bootstrap.itempipeline.itemrotationmanager` | Owns registration, lifecycle, and public retrieval. |
| `ToolTypeData` | `program.bootstrap.itempipeline.tooltype` | Raw data payload struct used by handles/instances. |
| `ToolTypeHandle` | `program.bootstrap.itempipeline.tooltype` | Persistent manager-registered wrapper around data. |
| `ToolTypeManager` | `program.bootstrap.itempipeline.tooltypemanager` | Owns registration, lifecycle, and public retrieval. |

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
