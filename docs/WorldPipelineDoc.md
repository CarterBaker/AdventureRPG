# WorldPipelineDoc

_Generated: 2026-04-03_

## Overview
WorldPipelineDoc covers 73 classes across 22 packages. This reference follows EngineContext naming/lifecycle conventions and summarizes class responsibilities by suffix rules.

## Integration Notes
- `core.engine` referenced via 61 imports.
- `core.kernel` referenced via 12 imports.

## Package Breakdown

### `worldpipeline`

| Class | Role |
|---|---|
| `WorldPipeline` | Pipeline registration entry point that wires dependency order. |

### `worldpipeline.biome`

| Class | Role |
|---|---|
| `BiomeData` | Raw data payload struct used by handles/instances. |
| `BiomeHandle` | Persistent manager-registered wrapper around data. |

### `worldpipeline.biomemanager`

| Class | Role |
|---|---|
| `BiomeManager` | Owns registration, lifecycle, and public retrieval. |
| `InternalBuilder` | Loader-owned internal builder helper. |
| `InternalLoader` | Manager-created internal loader helper. |

### `worldpipeline.block`

| Class | Role |
|---|---|
| `BlockData` | Raw data payload struct used by handles/instances. |
| `BlockHandle` | Persistent manager-registered wrapper around data. |
| `BlockPaletteHandle` | Persistent manager-registered wrapper around data. |
| `BlockRotationType` | Enum/type descriptor used for branching. |

### `worldpipeline.blockmanager`

| Class | Role |
|---|---|
| `BlockManager` | Owns registration, lifecycle, and public retrieval. |
| `InternalBufferSystem` | GPU buffer helper used by manager updates. |
| `InternalBuilder` | Loader-owned internal builder helper. |
| `InternalLoader` | Manager-created internal loader helper. |

### `worldpipeline.chunk`

| Class | Role |
|---|---|
| `ChunkData` | Raw data payload struct used by handles/instances. |
| `ChunkDataSyncContainer` | Specialized support class for this subsystem. |
| `ChunkDataUtility` | Static helper utility. |
| `ChunkInstance` | Runtime mutable clone of a handle. |
| `ChunkNeighborStruct` | Lightweight struct without engine lifecycle. |

### `worldpipeline.chunkstreammanager`

| Class | Role |
|---|---|
| `AssessmentBranch` | Manager-owned internal computation branch. |
| `BatchBranch` | Manager-owned internal computation branch. |
| `BuildBranch` | Manager-owned internal computation branch. |
| `ChunkQueueItem` | Specialized support class for this subsystem. |
| `ChunkQueueManager` | Owns registration, lifecycle, and public retrieval. |
| `ChunkStreamManager` | Owns registration, lifecycle, and public retrieval. |
| `DumpBranch` | Manager-owned internal computation branch. |
| `GenerationBranch` | Manager-owned internal computation branch. |
| `ItemLoadBranch` | Manager-owned internal computation branch. |
| `ItemRenderBranch` | Manager-owned internal computation branch. |
| `MergeBranch` | Manager-owned internal computation branch. |
| `QueueOperation` | Specialized support class for this subsystem. |
| `RenderBranch` | Manager-owned internal computation branch. |

### `worldpipeline.grid`

| Class | Role |
|---|---|
| `GridInstance` | Runtime mutable clone of a handle. |

### `worldpipeline.gridmanager`

| Class | Role |
|---|---|
| `GridBuildSystem` | Single-job helper system for focused tasks. |
| `GridManager` | Owns registration, lifecycle, and public retrieval. |

### `worldpipeline.gridslot`

| Class | Role |
|---|---|
| `GridSlotDetailLevel` | Specialized support class for this subsystem. |
| `GridSlotHandle` | Persistent manager-registered wrapper around data. |

### `worldpipeline.megachunk`

| Class | Role |
|---|---|
| `MegaBatchStruct` | Lightweight struct without engine lifecycle. |
| `MegaChunkInstance` | Runtime mutable clone of a handle. |
| `MegaData` | Raw data payload struct used by handles/instances. |
| `MegaDataSyncContainer` | Specialized support class for this subsystem. |
| `MegaDataUtility` | Static helper utility. |

### `worldpipeline.megastreammanager`

| Class | Role |
|---|---|
| `MegaAssessBranch` | Manager-owned internal computation branch. |
| `MegaDumpBranch` | Manager-owned internal computation branch. |
| `MegaMergeBranch` | Manager-owned internal computation branch. |
| `MegaQueueManager` | Owns registration, lifecycle, and public retrieval. |
| `MegaQueueOperation` | Specialized support class for this subsystem. |
| `MegaRenderBranch` | Manager-owned internal computation branch. |
| `MegaStreamManager` | Owns registration, lifecycle, and public retrieval. |

### `worldpipeline.subchunk`

| Class | Role |
|---|---|
| `SubChunkInstance` | Runtime mutable clone of a handle. |

### `worldpipeline.util`

| Class | Role |
|---|---|
| `ChunkCoordinate3Int` | Specialized support class for this subsystem. |
| `WorldPositionStruct` | Lightweight struct without engine lifecycle. |
| `WorldPositionUtility` | Static helper utility. |
| `WorldWrapUtility` | Static helper utility. |

### `worldpipeline.world`

| Class | Role |
|---|---|
| `WorldData` | Raw data payload struct used by handles/instances. |
| `WorldHandle` | Persistent manager-registered wrapper around data. |

### `worldpipeline.worldgenerationmanager`

| Class | Role |
|---|---|
| `WorldGenerationManager` | Owns registration, lifecycle, and public retrieval. |

### `worldpipeline.worlditem`

| Class | Role |
|---|---|
| `WorldItemCompositeInstance` | Runtime mutable clone of a handle. |
| `WorldItemInstance` | Runtime mutable clone of a handle. |
| `WorldItemInstancePaletteHandle` | Persistent manager-registered wrapper around data. |
| `WorldItemPaletteHandle` | Persistent manager-registered wrapper around data. |
| `WorldItemStruct` | Lightweight struct without engine lifecycle. |

### `worldpipeline.worlditemplacementsystem`

| Class | Role |
|---|---|
| `WorldItemPlacementSystem` | Single-job helper system for focused tasks. |

### `worldpipeline.worlditemrendersystem`

| Class | Role |
|---|---|
| `WorldItemRenderSystem` | Single-job helper system for focused tasks. |

### `worldpipeline.worldmanager`

| Class | Role |
|---|---|
| `InternalBuilder` | Loader-owned internal builder helper. |
| `InternalLoader` | Manager-created internal loader helper. |
| `WorldManager` | Owns registration, lifecycle, and public retrieval. |

### `worldpipeline.worldrendermanager`

| Class | Role |
|---|---|
| `FrustumCullingSystem` | Single-job helper system for focused tasks. |
| `RenderOperation` | Specialized support class for this subsystem. |
| `RenderType` | Enum/type descriptor used for branching. |
| `WorldRenderInstance` | Runtime mutable clone of a handle. |
| `WorldRenderManager` | Owns registration, lifecycle, and public retrieval. |

### `worldpipeline.worldstreammanager`

| Class | Role |
|---|---|
| `WorldStreamManager` | Owns registration, lifecycle, and public retrieval. |

## Class Role Summary

| Class | Package | Role |
|---|---|---|
| `AssessmentBranch` | `program.bootstrap.worldpipeline.chunkstreammanager` | Manager-owned internal computation branch. |
| `BatchBranch` | `program.bootstrap.worldpipeline.chunkstreammanager` | Manager-owned internal computation branch. |
| `BiomeData` | `program.bootstrap.worldpipeline.biome` | Raw data payload struct used by handles/instances. |
| `BiomeHandle` | `program.bootstrap.worldpipeline.biome` | Persistent manager-registered wrapper around data. |
| `BiomeManager` | `program.bootstrap.worldpipeline.biomemanager` | Owns registration, lifecycle, and public retrieval. |
| `BlockData` | `program.bootstrap.worldpipeline.block` | Raw data payload struct used by handles/instances. |
| `BlockHandle` | `program.bootstrap.worldpipeline.block` | Persistent manager-registered wrapper around data. |
| `BlockManager` | `program.bootstrap.worldpipeline.blockmanager` | Owns registration, lifecycle, and public retrieval. |
| `BlockPaletteHandle` | `program.bootstrap.worldpipeline.block` | Persistent manager-registered wrapper around data. |
| `BlockRotationType` | `program.bootstrap.worldpipeline.block` | Enum/type descriptor used for branching. |
| `BuildBranch` | `program.bootstrap.worldpipeline.chunkstreammanager` | Manager-owned internal computation branch. |
| `ChunkCoordinate3Int` | `program.bootstrap.worldpipeline.util` | Specialized support class for this subsystem. |
| `ChunkData` | `program.bootstrap.worldpipeline.chunk` | Raw data payload struct used by handles/instances. |
| `ChunkDataSyncContainer` | `program.bootstrap.worldpipeline.chunk` | Specialized support class for this subsystem. |
| `ChunkDataUtility` | `program.bootstrap.worldpipeline.chunk` | Static helper utility. |
| `ChunkInstance` | `program.bootstrap.worldpipeline.chunk` | Runtime mutable clone of a handle. |
| `ChunkNeighborStruct` | `program.bootstrap.worldpipeline.chunk` | Lightweight struct without engine lifecycle. |
| `ChunkQueueItem` | `program.bootstrap.worldpipeline.chunkstreammanager` | Specialized support class for this subsystem. |
| `ChunkQueueManager` | `program.bootstrap.worldpipeline.chunkstreammanager` | Owns registration, lifecycle, and public retrieval. |
| `ChunkStreamManager` | `program.bootstrap.worldpipeline.chunkstreammanager` | Owns registration, lifecycle, and public retrieval. |
| `DumpBranch` | `program.bootstrap.worldpipeline.chunkstreammanager` | Manager-owned internal computation branch. |
| `FrustumCullingSystem` | `program.bootstrap.worldpipeline.worldrendermanager` | Single-job helper system for focused tasks. |
| `GenerationBranch` | `program.bootstrap.worldpipeline.chunkstreammanager` | Manager-owned internal computation branch. |
| `GridBuildSystem` | `program.bootstrap.worldpipeline.gridmanager` | Single-job helper system for focused tasks. |
| `GridInstance` | `program.bootstrap.worldpipeline.grid` | Runtime mutable clone of a handle. |
| `GridManager` | `program.bootstrap.worldpipeline.gridmanager` | Owns registration, lifecycle, and public retrieval. |
| `GridSlotDetailLevel` | `program.bootstrap.worldpipeline.gridslot` | Specialized support class for this subsystem. |
| `GridSlotHandle` | `program.bootstrap.worldpipeline.gridslot` | Persistent manager-registered wrapper around data. |
| `InternalBufferSystem` | `program.bootstrap.worldpipeline.blockmanager` | GPU buffer helper used by manager updates. |
| `InternalBuilder` | `program.bootstrap.worldpipeline.biomemanager` | Loader-owned internal builder helper. |
| `InternalBuilder` | `program.bootstrap.worldpipeline.blockmanager` | Loader-owned internal builder helper. |
| `InternalBuilder` | `program.bootstrap.worldpipeline.worldmanager` | Loader-owned internal builder helper. |
| `InternalLoader` | `program.bootstrap.worldpipeline.biomemanager` | Manager-created internal loader helper. |
| `InternalLoader` | `program.bootstrap.worldpipeline.blockmanager` | Manager-created internal loader helper. |
| `InternalLoader` | `program.bootstrap.worldpipeline.worldmanager` | Manager-created internal loader helper. |
| `ItemLoadBranch` | `program.bootstrap.worldpipeline.chunkstreammanager` | Manager-owned internal computation branch. |
| `ItemRenderBranch` | `program.bootstrap.worldpipeline.chunkstreammanager` | Manager-owned internal computation branch. |
| `MegaAssessBranch` | `program.bootstrap.worldpipeline.megastreammanager` | Manager-owned internal computation branch. |
| `MegaBatchStruct` | `program.bootstrap.worldpipeline.megachunk` | Lightweight struct without engine lifecycle. |
| `MegaChunkInstance` | `program.bootstrap.worldpipeline.megachunk` | Runtime mutable clone of a handle. |
| `MegaData` | `program.bootstrap.worldpipeline.megachunk` | Raw data payload struct used by handles/instances. |
| `MegaDataSyncContainer` | `program.bootstrap.worldpipeline.megachunk` | Specialized support class for this subsystem. |
| `MegaDataUtility` | `program.bootstrap.worldpipeline.megachunk` | Static helper utility. |
| `MegaDumpBranch` | `program.bootstrap.worldpipeline.megastreammanager` | Manager-owned internal computation branch. |
| `MegaMergeBranch` | `program.bootstrap.worldpipeline.megastreammanager` | Manager-owned internal computation branch. |
| `MegaQueueManager` | `program.bootstrap.worldpipeline.megastreammanager` | Owns registration, lifecycle, and public retrieval. |
| `MegaQueueOperation` | `program.bootstrap.worldpipeline.megastreammanager` | Specialized support class for this subsystem. |
| `MegaRenderBranch` | `program.bootstrap.worldpipeline.megastreammanager` | Manager-owned internal computation branch. |
| `MegaStreamManager` | `program.bootstrap.worldpipeline.megastreammanager` | Owns registration, lifecycle, and public retrieval. |
| `MergeBranch` | `program.bootstrap.worldpipeline.chunkstreammanager` | Manager-owned internal computation branch. |
| `QueueOperation` | `program.bootstrap.worldpipeline.chunkstreammanager` | Specialized support class for this subsystem. |
| `RenderBranch` | `program.bootstrap.worldpipeline.chunkstreammanager` | Manager-owned internal computation branch. |
| `RenderOperation` | `program.bootstrap.worldpipeline.worldrendermanager` | Specialized support class for this subsystem. |
| `RenderType` | `program.bootstrap.worldpipeline.worldrendermanager` | Enum/type descriptor used for branching. |
| `SubChunkInstance` | `program.bootstrap.worldpipeline.subchunk` | Runtime mutable clone of a handle. |
| `WorldData` | `program.bootstrap.worldpipeline.world` | Raw data payload struct used by handles/instances. |
| `WorldGenerationManager` | `program.bootstrap.worldpipeline.worldgenerationmanager` | Owns registration, lifecycle, and public retrieval. |
| `WorldHandle` | `program.bootstrap.worldpipeline.world` | Persistent manager-registered wrapper around data. |
| `WorldItemCompositeInstance` | `program.bootstrap.worldpipeline.worlditem` | Runtime mutable clone of a handle. |
| `WorldItemInstance` | `program.bootstrap.worldpipeline.worlditem` | Runtime mutable clone of a handle. |
| `WorldItemInstancePaletteHandle` | `program.bootstrap.worldpipeline.worlditem` | Persistent manager-registered wrapper around data. |
| `WorldItemPaletteHandle` | `program.bootstrap.worldpipeline.worlditem` | Persistent manager-registered wrapper around data. |
| `WorldItemPlacementSystem` | `program.bootstrap.worldpipeline.worlditemplacementsystem` | Single-job helper system for focused tasks. |
| `WorldItemRenderSystem` | `program.bootstrap.worldpipeline.worlditemrendersystem` | Single-job helper system for focused tasks. |
| `WorldItemStruct` | `program.bootstrap.worldpipeline.worlditem` | Lightweight struct without engine lifecycle. |
| `WorldManager` | `program.bootstrap.worldpipeline.worldmanager` | Owns registration, lifecycle, and public retrieval. |
| `WorldPipeline` | `program.bootstrap.worldpipeline` | Pipeline registration entry point that wires dependency order. |
| `WorldPositionStruct` | `program.bootstrap.worldpipeline.util` | Lightweight struct without engine lifecycle. |
| `WorldPositionUtility` | `program.bootstrap.worldpipeline.util` | Static helper utility. |
| `WorldRenderInstance` | `program.bootstrap.worldpipeline.worldrendermanager` | Runtime mutable clone of a handle. |
| `WorldRenderManager` | `program.bootstrap.worldpipeline.worldrendermanager` | Owns registration, lifecycle, and public retrieval. |
| `WorldStreamManager` | `program.bootstrap.worldpipeline.worldstreammanager` | Owns registration, lifecycle, and public retrieval. |
| `WorldWrapUtility` | `program.bootstrap.worldpipeline.util` | Static helper utility. |

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
