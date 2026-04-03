# RenderPipelineDoc

_Generated: 2026-04-03_

## Overview
RenderPipelineDoc covers 13 classes across 8 packages. This reference follows EngineContext naming/lifecycle conventions and summarizes class responsibilities by suffix rules.

## Integration Notes
- `core.engine` referenced via 14 imports.
- `core.kernel` referenced via 7 imports.

## Package Breakdown

### `renderpipeline`

| Class | Role |
|---|---|
| `RenderPipeline` | Pipeline registration entry point that wires dependency order. |

### `renderpipeline.cameramanager`

| Class | Role |
|---|---|
| `CameraBufferSystem` | Single-job helper system for focused tasks. |
| `CameraManager` | Owns registration, lifecycle, and public retrieval. |

### `renderpipeline.compositebatch`

| Class | Role |
|---|---|
| `CompositeBatchStruct` | Lightweight struct without engine lifecycle. |

### `renderpipeline.compositerendersystem`

| Class | Role |
|---|---|
| `CompositeRenderSystem` | Single-job helper system for focused tasks. |
| `GLSLUtility` | Stateless OpenGL helper utility for shader-side logic. |

### `renderpipeline.renderbatch`

| Class | Role |
|---|---|
| `RenderBatchStruct` | Lightweight struct without engine lifecycle. |

### `renderpipeline.rendercall`

| Class | Role |
|---|---|
| `RenderCallStruct` | Lightweight struct without engine lifecycle. |

### `renderpipeline.rendermanager`

| Class | Role |
|---|---|
| `GLSLUtility` | Stateless OpenGL helper utility for shader-side logic. |
| `RenderManager` | Owns registration, lifecycle, and public retrieval. |
| `RenderQueueHandle` | Persistent manager-registered wrapper around data. |
| `RenderSystem` | Single-job helper system for focused tasks. |

### `renderpipeline.util`

| Class | Role |
|---|---|
| `MaskStruct` | Lightweight struct without engine lifecycle. |

## Class Role Summary

| Class | Package | Role |
|---|---|---|
| `CameraBufferSystem` | `program.bootstrap.renderpipeline.cameramanager` | Single-job helper system for focused tasks. |
| `CameraManager` | `program.bootstrap.renderpipeline.cameramanager` | Owns registration, lifecycle, and public retrieval. |
| `CompositeBatchStruct` | `program.bootstrap.renderpipeline.compositebatch` | Lightweight struct without engine lifecycle. |
| `CompositeRenderSystem` | `program.bootstrap.renderpipeline.compositerendersystem` | Single-job helper system for focused tasks. |
| `GLSLUtility` | `program.bootstrap.renderpipeline.compositerendersystem` | Stateless OpenGL helper utility for shader-side logic. |
| `GLSLUtility` | `program.bootstrap.renderpipeline.rendermanager` | Stateless OpenGL helper utility for shader-side logic. |
| `MaskStruct` | `program.bootstrap.renderpipeline.util` | Lightweight struct without engine lifecycle. |
| `RenderBatchStruct` | `program.bootstrap.renderpipeline.renderbatch` | Lightweight struct without engine lifecycle. |
| `RenderCallStruct` | `program.bootstrap.renderpipeline.rendercall` | Lightweight struct without engine lifecycle. |
| `RenderManager` | `program.bootstrap.renderpipeline.rendermanager` | Owns registration, lifecycle, and public retrieval. |
| `RenderPipeline` | `program.bootstrap.renderpipeline` | Pipeline registration entry point that wires dependency order. |
| `RenderQueueHandle` | `program.bootstrap.renderpipeline.rendermanager` | Persistent manager-registered wrapper around data. |
| `RenderSystem` | `program.bootstrap.renderpipeline.rendermanager` | Single-job helper system for focused tasks. |

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
