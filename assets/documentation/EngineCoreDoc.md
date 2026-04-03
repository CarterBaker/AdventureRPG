# EngineCoreDoc

_Generated: 2026-04-03_

## Overview
EngineCoreDoc covers 24 classes across 1 packages. This reference follows EngineContext naming/lifecycle conventions and summarizes class responsibilities by suffix rules.

## Integration Notes
- `core.kernel` referenced via 18 imports.
- `renderpipeline` referenced via 2 imports.
- `BootstrapAssembly` referenced via 1 imports.
- `runtime` referenced via 1 imports.
- `core.engine` referenced via 1 imports.

## Package Breakdown

### `engine`

| Class | Role |
|---|---|
| `AssemblyPackage` | Specialized support class for this subsystem. |
| `AsyncContainerPackage` | Specialized support class for this subsystem. |
| `BranchPackage` | Specialized support class for this subsystem. |
| `BuilderPackage` | Specialized support class for this subsystem. |
| `ContextPackage` | Specialized support class for this subsystem. |
| `DataPackage` | Specialized support class for this subsystem. |
| `EditorEngine` | Specialized support class for this subsystem. |
| `EnginePackage` | Specialized support class for this subsystem. |
| `EngineState` | Specialized support class for this subsystem. |
| `EngineUtility` | Static helper utility. |
| `GameEngine` | Specialized support class for this subsystem. |
| `HandlePackage` | Specialized support class for this subsystem. |
| `InstancePackage` | Specialized support class for this subsystem. |
| `LoaderPackage` | Specialized support class for this subsystem. |
| `Main` | Specialized support class for this subsystem. |
| `MainEditor` | Specialized support class for this subsystem. |
| `ManagerPackage` | Specialized support class for this subsystem. |
| `PipelinePackage` | Specialized support class for this subsystem. |
| `StructPackage` | Specialized support class for this subsystem. |
| `SyncContainerPackage` | Specialized support class for this subsystem. |
| `SystemContext` | Runtime context that groups frame-executed systems. |
| `SystemPackage` | Specialized support class for this subsystem. |
| `UtilityPackage` | Specialized support class for this subsystem. |
| `WindowPlatform` | Specialized support class for this subsystem. |

## Class Role Summary

| Class | Package | Role |
|---|---|---|
| `AssemblyPackage` | `program.core.engine` | Specialized support class for this subsystem. |
| `AsyncContainerPackage` | `program.core.engine` | Specialized support class for this subsystem. |
| `BranchPackage` | `program.core.engine` | Specialized support class for this subsystem. |
| `BuilderPackage` | `program.core.engine` | Specialized support class for this subsystem. |
| `ContextPackage` | `program.core.engine` | Specialized support class for this subsystem. |
| `DataPackage` | `program.core.engine` | Specialized support class for this subsystem. |
| `EditorEngine` | `program.core.engine` | Specialized support class for this subsystem. |
| `EnginePackage` | `program.core.engine` | Specialized support class for this subsystem. |
| `EngineState` | `program.core.engine` | Specialized support class for this subsystem. |
| `EngineUtility` | `program.core.engine` | Static helper utility. |
| `GameEngine` | `program.core.engine` | Specialized support class for this subsystem. |
| `HandlePackage` | `program.core.engine` | Specialized support class for this subsystem. |
| `InstancePackage` | `program.core.engine` | Specialized support class for this subsystem. |
| `LoaderPackage` | `program.core.engine` | Specialized support class for this subsystem. |
| `Main` | `program.core.engine` | Specialized support class for this subsystem. |
| `MainEditor` | `program.core.engine` | Specialized support class for this subsystem. |
| `ManagerPackage` | `program.core.engine` | Specialized support class for this subsystem. |
| `PipelinePackage` | `program.core.engine` | Specialized support class for this subsystem. |
| `StructPackage` | `program.core.engine` | Specialized support class for this subsystem. |
| `SyncContainerPackage` | `program.core.engine` | Specialized support class for this subsystem. |
| `SystemContext` | `program.core.engine` | Runtime context that groups frame-executed systems. |
| `SystemPackage` | `program.core.engine` | Specialized support class for this subsystem. |
| `UtilityPackage` | `program.core.engine` | Specialized support class for this subsystem. |
| `WindowPlatform` | `program.core.engine` | Specialized support class for this subsystem. |

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
