# KernelSystemsDoc

_Generated: 2026-04-03_

## Overview
KernelSystemsDoc covers 12 classes across 5 packages. This reference follows EngineContext naming/lifecycle conventions and summarizes class responsibilities by suffix rules.

## Integration Notes
- `core.engine` referenced via 15 imports.
- `core.kernel` referenced via 9 imports.
- `renderpipeline` referenced via 2 imports.
- `geometrypipeline` referenced via 1 imports.

## Package Breakdown

### `kernel.syncconsumer`

| Class | Role |
|---|---|
| `AsyncStructConsumer` | Specialized support class for this subsystem. |
| `AsyncStructConsumerMulti` | Specialized support class for this subsystem. |
| `BiSyncAsyncConsumer` | Specialized support class for this subsystem. |
| `SyncStructConsumer` | Specialized support class for this subsystem. |

### `kernel.thread`

| Class | Role |
|---|---|
| `ThreadHandle` | Persistent manager-registered wrapper around data. |

### `kernel.threadmanager`

| Class | Role |
|---|---|
| `InternalBuilder` | Loader-owned internal builder helper. |
| `InternalLoader` | Manager-created internal loader helper. |
| `InternalThreadManager` | Owns registration, lifecycle, and public retrieval. |
| `NamedThreadFactory` | Specialized support class for this subsystem. |

### `kernel.window`

| Class | Role |
|---|---|
| `WindowData` | Raw data payload struct used by handles/instances. |
| `WindowInstance` | Runtime mutable clone of a handle. |

### `kernel.windowmanager`

| Class | Role |
|---|---|
| `WindowManager` | Owns registration, lifecycle, and public retrieval. |

## Class Role Summary

| Class | Package | Role |
|---|---|---|
| `AsyncStructConsumer` | `program.core.kernel.syncconsumer` | Specialized support class for this subsystem. |
| `AsyncStructConsumerMulti` | `program.core.kernel.syncconsumer` | Specialized support class for this subsystem. |
| `BiSyncAsyncConsumer` | `program.core.kernel.syncconsumer` | Specialized support class for this subsystem. |
| `InternalBuilder` | `program.core.kernel.threadmanager` | Loader-owned internal builder helper. |
| `InternalLoader` | `program.core.kernel.threadmanager` | Manager-created internal loader helper. |
| `InternalThreadManager` | `program.core.kernel.threadmanager` | Owns registration, lifecycle, and public retrieval. |
| `NamedThreadFactory` | `program.core.kernel.threadmanager` | Specialized support class for this subsystem. |
| `SyncStructConsumer` | `program.core.kernel.syncconsumer` | Specialized support class for this subsystem. |
| `ThreadHandle` | `program.core.kernel.thread` | Persistent manager-registered wrapper around data. |
| `WindowData` | `program.core.kernel.window` | Raw data payload struct used by handles/instances. |
| `WindowInstance` | `program.core.kernel.window` | Runtime mutable clone of a handle. |
| `WindowManager` | `program.core.kernel.windowmanager` | Owns registration, lifecycle, and public retrieval. |

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
