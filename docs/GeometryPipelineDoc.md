# GeometryPipelineDoc

_Generated: 2026-04-03_

## Overview
GeometryPipelineDoc covers 45 classes across 17 packages. This reference follows EngineContext naming/lifecycle conventions and summarizes class responsibilities by suffix rules.

## Integration Notes
- `core.engine` referenced via 38 imports.

## Package Breakdown

### `geometrypipeline`

| Class | Role |
|---|---|
| `GeometryPipeline` | Pipeline registration entry point that wires dependency order. |

### `geometrypipeline.compositebuffer`

| Class | Role |
|---|---|
| `CompositeBufferData` | Raw data payload struct used by handles/instances. |
| `CompositeBufferInstance` | Runtime mutable clone of a handle. |

### `geometrypipeline.compositebuffermanager`

| Class | Role |
|---|---|
| `CompositeBufferManager` | Owns registration, lifecycle, and public retrieval. |
| `GLSLUtility` | Stateless OpenGL helper utility for shader-side logic. |

### `geometrypipeline.dynamicgeometrymanager`

| Class | Role |
|---|---|
| `ComplexGeometryBranch` | Manager-owned internal computation branch. |
| `DynamicGeometryManager` | Owns registration, lifecycle, and public retrieval. |
| `DynamicGeometryType` | Enum/type descriptor used for branching. |
| `FontGeometryBranch` | Manager-owned internal computation branch. |
| `FullGeometryBranch` | Manager-owned internal computation branch. |
| `InternalBuildManager` | Owns registration, lifecycle, and public retrieval. |
| `LiquidGeometryBranch` | Manager-owned internal computation branch. |
| `PartialGeometryBranch` | Manager-owned internal computation branch. |

### `geometrypipeline.dynamicgeometrymanager.util`

| Class | Role |
|---|---|
| `DynamicGeometryAsyncContainer` | Specialized support class for this subsystem. |
| `VertBlockNeighbor3Vector` | Specialized support class for this subsystem. |

### `geometrypipeline.dynamicmodel`

| Class | Role |
|---|---|
| `DynamicModelHandle` | Persistent manager-registered wrapper around data. |

### `geometrypipeline.dynamicpacket`

| Class | Role |
|---|---|
| `DynamicPacketInstance` | Runtime mutable clone of a handle. |
| `DynamicPacketState` | Specialized support class for this subsystem. |

### `geometrypipeline.ibo`

| Class | Role |
|---|---|
| `IBOData` | Raw data payload struct used by handles/instances. |
| `IBOHandle` | Persistent manager-registered wrapper around data. |
| `IBOInstance` | Runtime mutable clone of a handle. |

### `geometrypipeline.ibomanager`

| Class | Role |
|---|---|
| `GLSLUtility` | Stateless OpenGL helper utility for shader-side logic. |
| `IBOManager` | Owns registration, lifecycle, and public retrieval. |
| `InternalBuilder` | Loader-owned internal builder helper. |

### `geometrypipeline.mesh`

| Class | Role |
|---|---|
| `MeshData` | Raw data payload struct used by handles/instances. |
| `MeshHandle` | Persistent manager-registered wrapper around data. |
| `MeshInstance` | Runtime mutable clone of a handle. |

### `geometrypipeline.meshmanager`

| Class | Role |
|---|---|
| `InternalBuilder` | Loader-owned internal builder helper. |
| `InternalLoader` | Manager-created internal loader helper. |
| `MeshManager` | Owns registration, lifecycle, and public retrieval. |
| `QuadExpansionStruct` | Lightweight struct without engine lifecycle. |

### `geometrypipeline.model`

| Class | Role |
|---|---|
| `ModelInstance` | Runtime mutable clone of a handle. |

### `geometrypipeline.modelmanager`

| Class | Role |
|---|---|
| `ModelManager` | Owns registration, lifecycle, and public retrieval. |

### `geometrypipeline.vao`

| Class | Role |
|---|---|
| `VAOData` | Raw data payload struct used by handles/instances. |
| `VAOHandle` | Persistent manager-registered wrapper around data. |
| `VAOInstance` | Runtime mutable clone of a handle. |

### `geometrypipeline.vaomanager`

| Class | Role |
|---|---|
| `GLSLUtility` | Stateless OpenGL helper utility for shader-side logic. |
| `InternalBuilder` | Loader-owned internal builder helper. |
| `VAOManager` | Owns registration, lifecycle, and public retrieval. |

### `geometrypipeline.vbo`

| Class | Role |
|---|---|
| `VBOData` | Raw data payload struct used by handles/instances. |
| `VBOHandle` | Persistent manager-registered wrapper around data. |
| `VBOInstance` | Runtime mutable clone of a handle. |

### `geometrypipeline.vbomanager`

| Class | Role |
|---|---|
| `GLSLUtility` | Stateless OpenGL helper utility for shader-side logic. |
| `InternalBuilder` | Loader-owned internal builder helper. |
| `VBOManager` | Owns registration, lifecycle, and public retrieval. |

## Class Role Summary

| Class | Package | Role |
|---|---|---|
| `ComplexGeometryBranch` | `program.bootstrap.geometrypipeline.dynamicgeometrymanager` | Manager-owned internal computation branch. |
| `CompositeBufferData` | `program.bootstrap.geometrypipeline.compositebuffer` | Raw data payload struct used by handles/instances. |
| `CompositeBufferInstance` | `program.bootstrap.geometrypipeline.compositebuffer` | Runtime mutable clone of a handle. |
| `CompositeBufferManager` | `program.bootstrap.geometrypipeline.compositebuffermanager` | Owns registration, lifecycle, and public retrieval. |
| `DynamicGeometryAsyncContainer` | `program.bootstrap.geometrypipeline.dynamicgeometrymanager.util` | Specialized support class for this subsystem. |
| `DynamicGeometryManager` | `program.bootstrap.geometrypipeline.dynamicgeometrymanager` | Owns registration, lifecycle, and public retrieval. |
| `DynamicGeometryType` | `program.bootstrap.geometrypipeline.dynamicgeometrymanager` | Enum/type descriptor used for branching. |
| `DynamicModelHandle` | `program.bootstrap.geometrypipeline.dynamicmodel` | Persistent manager-registered wrapper around data. |
| `DynamicPacketInstance` | `program.bootstrap.geometrypipeline.dynamicpacket` | Runtime mutable clone of a handle. |
| `DynamicPacketState` | `program.bootstrap.geometrypipeline.dynamicpacket` | Specialized support class for this subsystem. |
| `FontGeometryBranch` | `program.bootstrap.geometrypipeline.dynamicgeometrymanager` | Manager-owned internal computation branch. |
| `FullGeometryBranch` | `program.bootstrap.geometrypipeline.dynamicgeometrymanager` | Manager-owned internal computation branch. |
| `GLSLUtility` | `program.bootstrap.geometrypipeline.compositebuffermanager` | Stateless OpenGL helper utility for shader-side logic. |
| `GLSLUtility` | `program.bootstrap.geometrypipeline.ibomanager` | Stateless OpenGL helper utility for shader-side logic. |
| `GLSLUtility` | `program.bootstrap.geometrypipeline.vaomanager` | Stateless OpenGL helper utility for shader-side logic. |
| `GLSLUtility` | `program.bootstrap.geometrypipeline.vbomanager` | Stateless OpenGL helper utility for shader-side logic. |
| `GeometryPipeline` | `program.bootstrap.geometrypipeline` | Pipeline registration entry point that wires dependency order. |
| `IBOData` | `program.bootstrap.geometrypipeline.ibo` | Raw data payload struct used by handles/instances. |
| `IBOHandle` | `program.bootstrap.geometrypipeline.ibo` | Persistent manager-registered wrapper around data. |
| `IBOInstance` | `program.bootstrap.geometrypipeline.ibo` | Runtime mutable clone of a handle. |
| `IBOManager` | `program.bootstrap.geometrypipeline.ibomanager` | Owns registration, lifecycle, and public retrieval. |
| `InternalBuildManager` | `program.bootstrap.geometrypipeline.dynamicgeometrymanager` | Owns registration, lifecycle, and public retrieval. |
| `InternalBuilder` | `program.bootstrap.geometrypipeline.ibomanager` | Loader-owned internal builder helper. |
| `InternalBuilder` | `program.bootstrap.geometrypipeline.meshmanager` | Loader-owned internal builder helper. |
| `InternalBuilder` | `program.bootstrap.geometrypipeline.vaomanager` | Loader-owned internal builder helper. |
| `InternalBuilder` | `program.bootstrap.geometrypipeline.vbomanager` | Loader-owned internal builder helper. |
| `InternalLoader` | `program.bootstrap.geometrypipeline.meshmanager` | Manager-created internal loader helper. |
| `LiquidGeometryBranch` | `program.bootstrap.geometrypipeline.dynamicgeometrymanager` | Manager-owned internal computation branch. |
| `MeshData` | `program.bootstrap.geometrypipeline.mesh` | Raw data payload struct used by handles/instances. |
| `MeshHandle` | `program.bootstrap.geometrypipeline.mesh` | Persistent manager-registered wrapper around data. |
| `MeshInstance` | `program.bootstrap.geometrypipeline.mesh` | Runtime mutable clone of a handle. |
| `MeshManager` | `program.bootstrap.geometrypipeline.meshmanager` | Owns registration, lifecycle, and public retrieval. |
| `ModelInstance` | `program.bootstrap.geometrypipeline.model` | Runtime mutable clone of a handle. |
| `ModelManager` | `program.bootstrap.geometrypipeline.modelmanager` | Owns registration, lifecycle, and public retrieval. |
| `PartialGeometryBranch` | `program.bootstrap.geometrypipeline.dynamicgeometrymanager` | Manager-owned internal computation branch. |
| `QuadExpansionStruct` | `program.bootstrap.geometrypipeline.meshmanager` | Lightweight struct without engine lifecycle. |
| `VAOData` | `program.bootstrap.geometrypipeline.vao` | Raw data payload struct used by handles/instances. |
| `VAOHandle` | `program.bootstrap.geometrypipeline.vao` | Persistent manager-registered wrapper around data. |
| `VAOInstance` | `program.bootstrap.geometrypipeline.vao` | Runtime mutable clone of a handle. |
| `VAOManager` | `program.bootstrap.geometrypipeline.vaomanager` | Owns registration, lifecycle, and public retrieval. |
| `VBOData` | `program.bootstrap.geometrypipeline.vbo` | Raw data payload struct used by handles/instances. |
| `VBOHandle` | `program.bootstrap.geometrypipeline.vbo` | Persistent manager-registered wrapper around data. |
| `VBOInstance` | `program.bootstrap.geometrypipeline.vbo` | Runtime mutable clone of a handle. |
| `VBOManager` | `program.bootstrap.geometrypipeline.vbomanager` | Owns registration, lifecycle, and public retrieval. |
| `VertBlockNeighbor3Vector` | `program.bootstrap.geometrypipeline.dynamicgeometrymanager.util` | Specialized support class for this subsystem. |

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
