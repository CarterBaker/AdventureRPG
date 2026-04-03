# WorldPipelineDoc

This is a human-readable subsystem manual. It explains responsibilities, collaboration points, and method intent for each class in scope.

Classes covered: **73**

## How to read this manual
- Start with the package flow notes at the top of each class section.
- Use **Who talks to this class** to identify collaborators.
- Use **Method intent** to understand lifecycle and API behavior.

## `core/src/program/bootstrap/worldpipeline/WorldPipeline.java`

**Type:** `class WorldPipeline`
  
**Inheritance/implements:** `extends PipelinePackage`
  
**Package:** `program.bootstrap.worldpipeline`
  
**File size:** 35 lines

**What this class does:** `WorldPipeline` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline`.

**Who this class talks to (direct imports):**
- `program.bootstrap.worldpipeline.biomemanager.BiomeManager`
- `program.bootstrap.worldpipeline.blockmanager.BlockManager`
- `program.bootstrap.worldpipeline.gridmanager.GridManager`
- `program.bootstrap.worldpipeline.worldgenerationmanager.WorldGenerationManager`
- `program.bootstrap.worldpipeline.worlditemplacementsystem.WorldItemPlacementSystem`
- `program.bootstrap.worldpipeline.worlditemrendersystem.WorldItemRenderSystem`
- `program.bootstrap.worldpipeline.worldmanager.WorldManager`
- `program.bootstrap.worldpipeline.worldrendermanager.WorldRenderManager`
- `program.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager`
- `program.core.engine.PipelinePackage`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/biome/BiomeData.java`

**Type:** `class BiomeData`
  
**Inheritance/implements:** `extends DataPackage`
  
**Package:** `program.bootstrap.worldpipeline.biome`
  
**File size:** 42 lines

**What this class does:** `BiomeData` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.biome`.

**Who this class talks to (direct imports):**
- `program.core.engine.DataPackage`
- `program.core.util.mathematics.extras.Color`

**Method intent:**
- `package public BiomeData(String biomeName, short biomeID, Color biomeColor)` — Performs class-specific logic; see call sites and owning manager flow.
- `public String getBiomeName()` — Returns current state/value.
- `public short getBiomeID()` — Returns current state/value.
- `public Color getBiomeColor()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/biome/BiomeHandle.java`

**Type:** `class BiomeHandle`
  
**Inheritance/implements:** `extends HandlePackage`
  
**Package:** `program.bootstrap.worldpipeline.biome`
  
**File size:** 39 lines

**What this class does:** `BiomeHandle` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.biome`.

**Who this class talks to (direct imports):**
- `program.core.engine.HandlePackage`
- `program.core.util.mathematics.extras.Color`

**Method intent:**
- `public void constructor(BiomeData biomeData)` — Engine-side initialization entrypoint invoked post-create.
- `public BiomeData getBiomeData()` — Returns current state/value.
- `public String getBiomeName()` — Returns current state/value.
- `public short getBiomeID()` — Returns current state/value.
- `public Color getBiomeColor()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/biomemanager/BiomeManager.java`

**Type:** `class BiomeManager`
  
**Inheritance/implements:** `extends ManagerPackage`
  
**Package:** `program.bootstrap.worldpipeline.biomemanager`
  
**File size:** 77 lines

**What this class does:** `BiomeManager` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.biomemanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.worldpipeline.biome.BiomeHandle`
- `program.core.engine.ManagerPackage`
- `program.core.util.RegistryUtility`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `package void addBiome(BiomeHandle biomeHandle)` — Registers a child object into manager-owned collections.
- `public void request(String biomeName)` — Triggers on-demand loading or lookup.
- `public boolean hasBiome(String biomeName)` — Boolean existence/availability check.
- `public short getBiomeIDFromBiomeName(String biomeName)` — Returns current state/value.
- `public BiomeHandle getBiomeHandleFromBiomeID(short biomeID)` — Returns current state/value.
- `public BiomeHandle getBiomeHandleFromBiomeName(String biomeName)` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/biomemanager/InternalBuilder.java`

**Type:** `class InternalBuilder`
  
**Inheritance/implements:** `extends BuilderPackage`
  
**Package:** `program.bootstrap.worldpipeline.biomemanager`
  
**File size:** 27 lines

**What this class does:** `InternalBuilder` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.biomemanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.worldpipeline.biome.BiomeData`
- `program.bootstrap.worldpipeline.biome.BiomeHandle`
- `program.core.engine.BuilderPackage`
- `program.core.util.FileUtility`
- `program.core.util.RegistryUtility`
- `program.core.util.mathematics.extras.Color`

**Method intent:**
- `package BiomeHandle build(File file, File root)` — Constructs derived runtime/handle data from source input.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/biomemanager/InternalLoader.java`

**Type:** `class InternalLoader`
  
**Inheritance/implements:** `extends LoaderPackage`
  
**Package:** `program.bootstrap.worldpipeline.biomemanager`
  
**File size:** 80 lines

**What this class does:** `InternalLoader` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.biomemanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.worldpipeline.biome.BiomeHandle`
- `program.core.engine.LoaderPackage`
- `program.core.settings.EngineSetting`
- `program.core.util.FileUtility`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void get()` — Returns current state/value.
- `protected void scan()` — Discovers files/resources for later load.
- `protected void load(File file)` — Parses external data into engine objects.
- `package void request(String biomeName)` — Triggers on-demand loading or lookup.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/block/BlockData.java`

**Type:** `class BlockData`
  
**Inheritance/implements:** `extends DataPackage`
  
**Package:** `program.bootstrap.worldpipeline.block`
  
**File size:** 104 lines

**What this class does:** `BlockData` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.block`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryType`
- `program.core.engine.DataPackage`
- `program.core.util.mathematics.extras.Direction3Vector`

**Method intent:**
- `package public BlockData(String blockName, short blockID, DynamicGeometryType geometry, BlockRotationType rotationType, int materialID, int northTexture, int eastTexture, int southTexture, int westTexture, int upTexture, int downTexture, int breakTier, short requiredToolTypeID, int durability)` — Performs class-specific logic; see call sites and owning manager flow.
- `public String getBlockName()` — Returns current state/value.
- `public short getBlockID()` — Returns current state/value.
- `public DynamicGeometryType getGeometry()` — Returns current state/value.
- `public BlockRotationType getRotationType()` — Returns current state/value.
- `public int getMaterialID()` — Returns current state/value.
- `public int getTextureForFace(Direction3Vector direction)` — Returns current state/value.
- `public int getBreakTier()` — Returns current state/value.
- `public short getRequiredToolTypeID()` — Returns current state/value.
- `public int getDurability()` — Returns current state/value.
- `public boolean isUnbreakable()` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/block/BlockHandle.java`

**Type:** `class BlockHandle`
  
**Inheritance/implements:** `extends HandlePackage`
  
**Package:** `program.bootstrap.worldpipeline.block`
  
**File size:** 68 lines

**What this class does:** `BlockHandle` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.block`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryType`
- `program.core.engine.HandlePackage`
- `program.core.util.mathematics.extras.Direction3Vector`

**Method intent:**
- `public void constructor(BlockData blockData)` — Engine-side initialization entrypoint invoked post-create.
- `public BlockData getBlockData()` — Returns current state/value.
- `public String getBlockName()` — Returns current state/value.
- `public short getBlockID()` — Returns current state/value.
- `public DynamicGeometryType getGeometry()` — Returns current state/value.
- `public BlockRotationType getRotationType()` — Returns current state/value.
- `public int getMaterialID()` — Returns current state/value.
- `public int getTextureForFace(Direction3Vector direction)` — Returns current state/value.
- `public int getBreakTier()` — Returns current state/value.
- `public short getRequiredToolTypeID()` — Returns current state/value.
- `public int getDurability()` — Returns current state/value.
- `public boolean isUnbreakable()` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/block/BlockPaletteHandle.java`

**Type:** `class BlockPaletteHandle`
  
**Inheritance/implements:** `extends HandlePackage`
  
**Package:** `program.bootstrap.worldpipeline.block`
  
**File size:** 281 lines

**What this class does:** `BlockPaletteHandle` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.block`.

**Who this class talks to (direct imports):**
- `program.bootstrap.worldpipeline.util.ChunkCoordinate3Int`
- `program.core.engine.HandlePackage`
- `program.core.settings.EngineSetting`
- `program.core.util.mathematics.extras.Coordinate3Int`

**Method intent:**
- `public void constructor(int paletteAxisSize, int paletteThreshold, short defaultBlockId)` — Engine-side initialization entrypoint invoked post-create.
- `public void clear()` — Performs class-specific logic; see call sites and owning manager flow.
- `private void allocatePackedArray()` — Performs class-specific logic; see call sites and owning manager flow.
- `private int calculateBitsNeeded(int paletteSize)` — Performs class-specific logic; see call sites and owning manager flow.
- `private int readPackedValue(int index)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void writePackedValue(int index, int value)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void expandBits(int newBits)` — Performs class-specific logic; see call sites and owning manager flow.
- `private int readPackedValueFrom(long[] data, int bits, int index)` — Performs class-specific logic; see call sites and owning manager flow.
- `private int getCellIndex(int packedXYZ)` — Returns current state/value.
- `private void convertToDirect()` — Performs class-specific logic; see call sites and owning manager flow.
- `private void setBlockByIndex(int index, short blockId)` — Mutates internal state for this object.
- `private void collapse()` — Performs class-specific logic; see call sites and owning manager flow.
- `public void dumpInteriorBlocks(short airBlockId)` — Performs class-specific logic; see call sites and owning manager flow.
- `public short getBlock(int packedXYZ)` — Returns current state/value.
- `public void setBlock(int packedXYZ, short blockId)` — Mutates internal state for this object.
- `public short getBlock(int x, int y, int z)` — Returns current state/value.
- `public void setBlock(int x, int y, int z, short blockId)` — Mutates internal state for this object.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/block/BlockRotationType.java`

**Type:** `enum BlockRotationType`
  
**Package:** `program.bootstrap.worldpipeline.block`
  
**File size:** 17 lines

**What this class does:** `BlockRotationType` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.block`.

**Method intent:** No concrete methods parsed (interface marker, enum, or data-only declaration).

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/blockmanager/BlockManager.java`

**Type:** `class BlockManager`
  
**Inheritance/implements:** `extends ManagerPackage`
  
**Package:** `program.bootstrap.worldpipeline.blockmanager`
  
**File size:** 82 lines

**What this class does:** `BlockManager` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.blockmanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.worldpipeline.block.BlockHandle`
- `program.core.engine.ManagerPackage`
- `program.core.util.RegistryUtility`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `package void addBlock(BlockHandle blockHandle)` — Registers a child object into manager-owned collections.
- `public void request(String blockName)` — Triggers on-demand loading or lookup.
- `public boolean hasBlock(String blockName)` — Boolean existence/availability check.
- `public int getBlockIDFromBlockName(String blockName)` — Returns current state/value.
- `public BlockHandle getBlockHandleFromBlockID(int blockID)` — Returns current state/value.
- `public BlockHandle getBlockHandleFromBlockName(String blockName)` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/blockmanager/InternalBufferSystem.java`

**Type:** `class InternalBufferSystem`
  
**Inheritance/implements:** `extends SystemPackage`
  
**Package:** `program.bootstrap.worldpipeline.blockmanager`
  
**File size:** 59 lines

**What this class does:** `InternalBufferSystem` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.blockmanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.shaderpipeline.ubo.UBOHandle`
- `program.bootstrap.shaderpipeline.ubomanager.UBOManager`
- `program.core.engine.SystemPackage`
- `program.core.util.mathematics.extras.Direction3Vector`
- `program.core.util.mathematics.vectors.Vector2`

**Method intent:**
- `protected void get()` — Returns current state/value.
- `protected void awake()` — Runs startup-time runtime activation work.
- `private void pushBlockOrientationMap()` — Queues data for downstream systems (often render queues).

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/blockmanager/InternalBuilder.java`

**Type:** `class InternalBuilder`
  
**Inheritance/implements:** `extends BuilderPackage`
  
**Package:** `program.bootstrap.worldpipeline.blockmanager`
  
**File size:** 161 lines

**What this class does:** `InternalBuilder` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.blockmanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryType`
- `program.bootstrap.itempipeline.tooltypemanager.ToolTypeManager`
- `program.bootstrap.shaderpipeline.materialmanager.MaterialManager`
- `program.bootstrap.shaderpipeline.texturemanager.TextureManager`
- `program.bootstrap.worldpipeline.block.BlockData`
- `program.bootstrap.worldpipeline.block.BlockHandle`
- `program.bootstrap.worldpipeline.block.BlockRotationType`
- `program.core.engine.BuilderPackage`
- `program.core.settings.EngineSetting`
- `program.core.util.FileUtility`
- `program.core.util.JsonUtility`
- `program.core.util.RegistryUtility`
- `program.core.util.mathematics.extras.Direction3Vector`

**Method intent:**
- `protected void get()` — Returns current state/value.
- `package ObjectArrayList<BlockHandle> build(File file, File root)` — Constructs derived runtime/handle data from source input.
- `private BlockHandle parseBlock(JsonObject blockJson, String pathPrefix)` — Performs class-specific logic; see call sites and owning manager flow.
- `private DynamicGeometryType parseBlockType(String typeStr)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/blockmanager/InternalLoader.java`

**Type:** `class InternalLoader`
  
**Inheritance/implements:** `extends LoaderPackage`
  
**Package:** `program.bootstrap.worldpipeline.blockmanager`
  
**File size:** 117 lines

**What this class does:** `InternalLoader` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.blockmanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.worldpipeline.block.BlockHandle`
- `program.core.engine.LoaderPackage`
- `program.core.settings.EngineSetting`
- `program.core.util.FileUtility`
- `program.core.util.JsonUtility`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void get()` — Returns current state/value.
- `protected void scan()` — Discovers files/resources for later load.
- `private void preRegisterBlockNames(File file, String resourceName)` — Performs class-specific logic; see call sites and owning manager flow.
- `protected void load(File file)` — Parses external data into engine objects.
- `package void request(String blockName)` — Triggers on-demand loading or lookup.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/chunk/ChunkData.java`

**Type:** `enum ChunkData`
  
**Package:** `program.bootstrap.worldpipeline.chunk`
  
**File size:** 88 lines

**What this class does:** `ChunkData` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.chunk`.

**Who this class talks to (direct imports):**
- `program.bootstrap.worldpipeline.gridslot.GridSlotDetailLevel`

**Method intent:**
- `package  ChunkData(boolean dumpable, GridSlotDetailLevel minimumLevel, String[] requiresNames, String[] leadsToNames)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void link()` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/chunk/ChunkDataSyncContainer.java`

**Type:** `class ChunkDataSyncContainer`
  
**Inheritance/implements:** `extends SyncContainerPackage`
  
**Package:** `program.bootstrap.worldpipeline.chunk`
  
**File size:** 100 lines

**What this class does:** `ChunkDataSyncContainer` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.chunk`.

**Who this class talks to (direct imports):**
- `program.core.engine.SyncContainerPackage`

**Method intent:**
- `public void create()` — Allocates/initializes child systems or resources.
- `public void resetData()` — Performs class-specific logic; see call sites and owning manager flow.
- `public boolean beginWork(int workType)` — Performs class-specific logic; see call sites and owning manager flow.
- `public boolean beginWorkLocked(int workType)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void endWork(int workType)` — Performs class-specific logic; see call sites and owning manager flow.
- `public boolean[] getData()` — Returns current state/value.
- `public boolean hasData(ChunkData dataType)` — Boolean existence/availability check.
- `public boolean setData(ChunkData dataType, boolean value)` — Mutates internal state for this object.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/chunk/ChunkDataUtility.java`

**Type:** `class ChunkDataUtility`
  
**Inheritance/implements:** `extends UtilityPackage`
  
**Package:** `program.bootstrap.worldpipeline.chunk`
  
**File size:** 140 lines

**What this class does:** `ChunkDataUtility` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.chunk`.

**Who this class talks to (direct imports):**
- `program.bootstrap.worldpipeline.gridslot.GridSlotDetailLevel`
- `program.core.engine.UtilityPackage`

**Method intent:**
- `public ChunkData nextToLoad(boolean[] flags, GridSlotDetailLevel slotLevel)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/worldpipeline/chunkstreammanager/ChunkQueueManager.java`.
- `private boolean isNeeded(ChunkData stage, boolean[] flags, GridSlotDetailLevel slotLevel)` — Performs class-specific logic; see call sites and owning manager flow.
- `private boolean isDirectlyRequired(ChunkData stage, GridSlotDetailLevel slotLevel)` — Performs class-specific logic; see call sites and owning manager flow.
- `private boolean requiresMet(ChunkData stage, boolean[] flags)` — Performs class-specific logic; see call sites and owning manager flow.
- `public ChunkData nextToDump(boolean[] flags, GridSlotDetailLevel slotLevel)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/worldpipeline/chunkstreammanager/ChunkQueueManager.java`, `core/src/program/bootstrap/worldpipeline/chunkstreammanager/DumpBranch.java`.
- `private boolean leadsToSafe(ChunkData stage, boolean[] flags)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void cascadeClear(ChunkData stage, boolean[] flags)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/worldpipeline/chunkstreammanager/DumpBranch.java`.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/chunk/ChunkInstance.java`

**Type:** `class ChunkInstance`
  
**Inheritance/implements:** `extends WorldRenderInstance`
  
**Package:** `program.bootstrap.worldpipeline.chunk`
  
**File size:** 149 lines

**What this class does:** `ChunkInstance` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.chunk`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.vao.VAOHandle`
- `program.bootstrap.worldpipeline.subchunk.SubChunkInstance`
- `program.bootstrap.worldpipeline.world.WorldHandle`
- `program.bootstrap.worldpipeline.worlditem.WorldItemInstancePaletteHandle`
- `program.bootstrap.worldpipeline.worldrendermanager.RenderType`
- `program.bootstrap.worldpipeline.worldrendermanager.WorldRenderInstance`
- `program.bootstrap.worldpipeline.worldrendermanager.WorldRenderManager`
- `program.core.settings.EngineSetting`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `public void constructor(WorldRenderManager worldRenderManager, WorldHandle worldHandle, long coordinate, VAOHandle vaoHandle, short airBlockId, short defaultBiomeId, Long2ObjectLinkedOpenHashMap<ChunkInstance> activeChunks)` — Engine-side initialization entrypoint invoked post-create.
- `public void reset()` — Performs class-specific logic; see call sites and owning manager flow.
- `public boolean merge()` — Performs class-specific logic; see call sites and owning manager flow.
- `public ChunkDataSyncContainer getChunkDataSyncContainer()` — Returns current state/value.
- `public SubChunkInstance[] getSubChunks()` — Returns current state/value.
- `public SubChunkInstance getSubChunk(int subChunkCoordinate)` — Returns current state/value.
- `public ChunkNeighborStruct getChunkNeighbors()` — Returns current state/value.
- `public WorldItemInstancePaletteHandle getWorldItemInstancePaletteHandle()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/chunk/ChunkNeighborStruct.java`

**Type:** `class ChunkNeighborStruct`
  
**Inheritance/implements:** `extends StructPackage`
  
**Package:** `program.bootstrap.worldpipeline.chunk`
  
**File size:** 50 lines

**What this class does:** `ChunkNeighborStruct` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.chunk`.

**Who this class talks to (direct imports):**
- `program.bootstrap.worldpipeline.util.WorldWrapUtility`
- `program.core.engine.StructPackage`
- `program.core.util.mathematics.extras.Coordinate2Long`
- `program.core.util.mathematics.extras.Direction2Vector`

**Method intent:**
- `package public ChunkNeighborStruct(long chunkCoordinate, ChunkInstance chunkInstance, Long2ObjectLinkedOpenHashMap<ChunkInstance> activeChunks)` — Performs class-specific logic; see call sites and owning manager flow.
- `public long getNeighborCoordinate(int direction2VectorIndex)` — Returns current state/value.
- `public ChunkInstance getNeighborChunk(int direction2VectorIndex)` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/chunkstreammanager/AssessmentBranch.java`

**Type:** `class AssessmentBranch`
  
**Inheritance/implements:** `extends BranchPackage`
  
**Package:** `program.bootstrap.worldpipeline.chunkstreammanager`
  
**File size:** 34 lines

**What this class does:** `AssessmentBranch` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.chunkstreammanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.worldpipeline.chunk.ChunkData`
- `program.bootstrap.worldpipeline.chunk.ChunkInstance`
- `program.bootstrap.worldpipeline.chunk.ChunkNeighborStruct`
- `program.core.engine.BranchPackage`
- `program.core.util.mathematics.extras.Direction2Vector`

**Method intent:**
- `public void assessChunk(ChunkInstance chunkInstance)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/chunkstreammanager/BatchBranch.java`

**Type:** `class BatchBranch`
  
**Inheritance/implements:** `extends BranchPackage`
  
**Package:** `program.bootstrap.worldpipeline.chunkstreammanager`
  
**File size:** 36 lines

**What this class does:** `BatchBranch` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.chunkstreammanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.worldpipeline.chunk.ChunkData`
- `program.bootstrap.worldpipeline.chunk.ChunkInstance`
- `program.bootstrap.worldpipeline.grid.GridInstance`
- `program.bootstrap.worldpipeline.megastreammanager.MegaStreamManager`
- `program.core.engine.BranchPackage`

**Method intent:**
- `protected void get()` — Returns current state/value.
- `public void batchChunk(ChunkInstance chunkInstance, GridInstance grid)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/chunkstreammanager/BuildBranch.java`

**Type:** `class BuildBranch`
  
**Inheritance/implements:** `extends BranchPackage`
  
**Package:** `program.bootstrap.worldpipeline.chunkstreammanager`
  
**File size:** 55 lines

**What this class does:** `BuildBranch` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.chunkstreammanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryManager`
- `program.bootstrap.geometrypipeline.dynamicgeometrymanager.util.DynamicGeometryAsyncContainer`
- `program.bootstrap.worldpipeline.chunk.ChunkData`
- `program.bootstrap.worldpipeline.chunk.ChunkDataSyncContainer`
- `program.bootstrap.worldpipeline.chunk.ChunkInstance`
- `program.core.engine.BranchPackage`
- `program.core.kernel.thread.ThreadHandle`

**Method intent:**
- `protected void get()` — Returns current state/value.
- `public void buildChunk(ChunkInstance chunkInstance)` — Constructs derived runtime/handle data from source input.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/chunkstreammanager/ChunkQueueItem.java`

**Type:** `enum ChunkQueueItem`
  
**Package:** `program.bootstrap.worldpipeline.chunkstreammanager`
  
**File size:** 13 lines

**What this class does:** `ChunkQueueItem` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.chunkstreammanager`.

**Method intent:** No concrete methods parsed (interface marker, enum, or data-only declaration).

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/chunkstreammanager/ChunkQueueManager.java`

**Type:** `class ChunkQueueManager`
  
**Inheritance/implements:** `extends ManagerPackage`
  
**Package:** `program.bootstrap.worldpipeline.chunkstreammanager`
  
**File size:** 396 lines

**What this class does:** `ChunkQueueManager` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.chunkstreammanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.worldpipeline.biomemanager.BiomeManager`
- `program.bootstrap.worldpipeline.blockmanager.BlockManager`
- `program.bootstrap.worldpipeline.chunk.ChunkData`
- `program.bootstrap.worldpipeline.chunk.ChunkDataSyncContainer`
- `program.bootstrap.worldpipeline.chunk.ChunkDataUtility`
- `program.bootstrap.worldpipeline.chunk.ChunkInstance`
- `program.bootstrap.worldpipeline.grid.GridInstance`
- `program.bootstrap.worldpipeline.gridslot.GridSlotDetailLevel`
- `program.bootstrap.worldpipeline.gridslot.GridSlotHandle`
- `program.bootstrap.worldpipeline.worldrendermanager.WorldRenderManager`
- `program.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager`
- `program.core.engine.ManagerPackage`
- `program.core.settings.EngineSetting`
- `program.core.util.queue.QueueInstance`
- `program.core.util.queue.QueueItemHandle`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void get()` — Returns current state/value.
- `protected void awake()` — Runs startup-time runtime activation work.
- `protected void update()` — Runs frame-step maintenance and logic.
- `package void onGridRebuilt(GridInstance grid)` — Performs class-specific logic; see call sites and owning manager flow.
- `package void onGridRemoved(GridInstance grid)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void executeQueue()` — Performs class-specific logic; see call sites and owning manager flow.
- `private void scanGridSlots(GridInstance grid)` — Discovers files/resources for later load.
- `private void loadQueue(GridInstance grid)` — Parses external data into engine objects.
- `private void assessActiveChunks(GridInstance grid)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void flushActiveChunks(GridInstance grid)` — Performs class-specific logic; see call sites and owning manager flow.
- `private QueueOperation determineQueueOperation(ChunkInstance chunkInstance, GridSlotHandle gridSlotHandle)` — Performs class-specific logic; see call sites and owning manager flow.
- `private QueueOperation toOperation(ChunkData stage)` — Performs class-specific logic; see call sites and owning manager flow.
- `private boolean reserveAsyncWork(ChunkDataSyncContainer syncContainer, QueueOperation operation)` — Performs class-specific logic; see call sites and owning manager flow.
- `package void invalidateChunkBatch(ChunkInstance chunk)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/chunkstreammanager/ChunkStreamManager.java`

**Type:** `class ChunkStreamManager`
  
**Inheritance/implements:** `extends ManagerPackage`
  
**Package:** `program.bootstrap.worldpipeline.chunkstreammanager`
  
**File size:** 82 lines

**What this class does:** `ChunkStreamManager` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.chunkstreammanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.vao.VAOHandle`
- `program.bootstrap.geometrypipeline.vaomanager.VAOManager`
- `program.bootstrap.worldpipeline.chunk.ChunkInstance`
- `program.bootstrap.worldpipeline.grid.GridInstance`
- `program.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager`
- `program.core.engine.ManagerPackage`
- `program.core.settings.EngineSetting`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void get()` — Returns current state/value.
- `protected void start()` — Performs class-specific logic; see call sites and owning manager flow.
- `public void onGridRebuilt(GridInstance grid)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void onGridRemoved(GridInstance grid)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void invalidateChunkBatch(long chunkCoordinate)` — Performs class-specific logic; see call sites and owning manager flow.
- `public VAOHandle getChunkVAO()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/chunkstreammanager/DumpBranch.java`

**Type:** `class DumpBranch`
  
**Inheritance/implements:** `extends BranchPackage`
  
**Package:** `program.bootstrap.worldpipeline.chunkstreammanager`
  
**File size:** 121 lines

**What this class does:** `DumpBranch` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.chunkstreammanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.worldpipeline.blockmanager.BlockManager`
- `program.bootstrap.worldpipeline.chunk.ChunkData`
- `program.bootstrap.worldpipeline.chunk.ChunkDataSyncContainer`
- `program.bootstrap.worldpipeline.chunk.ChunkDataUtility`
- `program.bootstrap.worldpipeline.chunk.ChunkInstance`
- `program.bootstrap.worldpipeline.gridslot.GridSlotHandle`
- `program.bootstrap.worldpipeline.subchunk.SubChunkInstance`
- `program.bootstrap.worldpipeline.worlditemplacementsystem.WorldItemPlacementSystem`
- `program.bootstrap.worldpipeline.worldrendermanager.WorldRenderManager`
- `program.core.engine.BranchPackage`
- `program.core.settings.EngineSetting`

**Method intent:**
- `protected void get()` — Returns current state/value.
- `protected void awake()` — Runs startup-time runtime activation work.
- `public void dumpChunkData(ChunkInstance chunkInstance, GridSlotHandle gridSlotHandle)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void executeDump(ChunkInstance chunkInstance, ChunkData stage)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void dumpGenerationData(ChunkInstance chunkInstance)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void dumpBuildData(ChunkInstance chunkInstance)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void dumpMergeData(ChunkInstance chunkInstance)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void dumpRenderData(ChunkInstance chunkInstance)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void dumpItemData(ChunkInstance chunkInstance)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void dumpItemRenderData(ChunkInstance chunkInstance)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/chunkstreammanager/GenerationBranch.java`

**Type:** `class GenerationBranch`
  
**Inheritance/implements:** `extends BranchPackage`
  
**Package:** `program.bootstrap.worldpipeline.chunkstreammanager`
  
**File size:** 101 lines

**What this class does:** `GenerationBranch` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.chunkstreammanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.worldpipeline.chunk.ChunkData`
- `program.bootstrap.worldpipeline.chunk.ChunkDataSyncContainer`
- `program.bootstrap.worldpipeline.chunk.ChunkInstance`
- `program.bootstrap.worldpipeline.subchunk.SubChunkInstance`
- `program.bootstrap.worldpipeline.worldgenerationmanager.WorldGenerationManager`
- `program.core.engine.BranchPackage`
- `program.core.kernel.thread.ThreadHandle`
- `program.core.settings.EngineSetting`

**Method intent:**
- `protected void get()` — Returns current state/value.
- `public void getNewChunk(ChunkInstance chunkInstance)` — Returns current state/value.
- `private boolean loadChunk(ChunkInstance chunkInstance, ChunkDataSyncContainer container)` — Parses external data into engine objects.
- `private void generateChunk(ChunkInstance chunkInstance, ChunkDataSyncContainer container)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/chunkstreammanager/ItemLoadBranch.java`

**Type:** `class ItemLoadBranch`
  
**Inheritance/implements:** `extends BranchPackage`
  
**Package:** `program.bootstrap.worldpipeline.chunkstreammanager`
  
**File size:** 58 lines

**What this class does:** `ItemLoadBranch` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.chunkstreammanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.worldpipeline.chunk.ChunkData`
- `program.bootstrap.worldpipeline.chunk.ChunkDataSyncContainer`
- `program.bootstrap.worldpipeline.chunk.ChunkInstance`
- `program.bootstrap.worldpipeline.worlditemplacementsystem.WorldItemPlacementSystem`
- `program.core.engine.BranchPackage`
- `program.core.kernel.thread.ThreadHandle`

**Method intent:**
- `protected void get()` — Returns current state/value.
- `public void loadItems(ChunkInstance chunkInstance)` — Parses external data into engine objects.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/chunkstreammanager/ItemRenderBranch.java`

**Type:** `class ItemRenderBranch`
  
**Inheritance/implements:** `extends BranchPackage`
  
**Package:** `program.bootstrap.worldpipeline.chunkstreammanager`
  
**File size:** 53 lines

**What this class does:** `ItemRenderBranch` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.chunkstreammanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.worldpipeline.chunk.ChunkData`
- `program.bootstrap.worldpipeline.chunk.ChunkDataSyncContainer`
- `program.bootstrap.worldpipeline.chunk.ChunkInstance`
- `program.bootstrap.worldpipeline.worlditemplacementsystem.WorldItemPlacementSystem`
- `program.core.engine.BranchPackage`

**Method intent:**
- `protected void get()` — Returns current state/value.
- `public void renderItems(ChunkInstance chunkInstance)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/chunkstreammanager/MergeBranch.java`

**Type:** `class MergeBranch`
  
**Inheritance/implements:** `extends BranchPackage`
  
**Package:** `program.bootstrap.worldpipeline.chunkstreammanager`
  
**File size:** 52 lines

**What this class does:** `MergeBranch` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.chunkstreammanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.worldpipeline.chunk.ChunkData`
- `program.bootstrap.worldpipeline.chunk.ChunkDataSyncContainer`
- `program.bootstrap.worldpipeline.chunk.ChunkInstance`
- `program.core.engine.BranchPackage`
- `program.core.kernel.thread.ThreadHandle`

**Method intent:**
- `protected void get()` — Returns current state/value.
- `public void mergeChunk(ChunkInstance chunkInstance)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/chunkstreammanager/QueueOperation.java`

**Type:** `enum QueueOperation`
  
**Package:** `program.bootstrap.worldpipeline.chunkstreammanager`
  
**File size:** 21 lines

**What this class does:** `QueueOperation` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.chunkstreammanager`.

**Method intent:** No concrete methods parsed (interface marker, enum, or data-only declaration).

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/chunkstreammanager/RenderBranch.java`

**Type:** `class RenderBranch`
  
**Inheritance/implements:** `extends BranchPackage`
  
**Package:** `program.bootstrap.worldpipeline.chunkstreammanager`
  
**File size:** 49 lines

**What this class does:** `RenderBranch` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.chunkstreammanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.worldpipeline.chunk.ChunkData`
- `program.bootstrap.worldpipeline.chunk.ChunkDataSyncContainer`
- `program.bootstrap.worldpipeline.chunk.ChunkInstance`
- `program.bootstrap.worldpipeline.worldrendermanager.WorldRenderManager`
- `program.core.engine.BranchPackage`

**Method intent:**
- `protected void get()` — Returns current state/value.
- `public void renderChunk(ChunkInstance chunkInstance)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/grid/GridInstance.java`

**Type:** `class GridInstance`
  
**Inheritance/implements:** `extends InstancePackage`
  
**Package:** `program.bootstrap.worldpipeline.grid`
  
**File size:** 269 lines

**What this class does:** `GridInstance` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.grid`.

**Who this class talks to (direct imports):**
- `program.bootstrap.entitypipeline.entity.EntityInstance`
- `program.bootstrap.worldpipeline.chunk.ChunkInstance`
- `program.bootstrap.worldpipeline.gridslot.GridSlotHandle`
- `program.bootstrap.worldpipeline.megachunk.MegaChunkInstance`
- `program.bootstrap.worldpipeline.util.WorldWrapUtility`
- `program.bootstrap.worldpipeline.world.WorldHandle`
- `program.bootstrap.worldpipeline.worldrendermanager.RenderType`
- `program.core.engine.InstancePackage`
- `program.core.kernel.window.WindowInstance`
- `program.core.settings.EngineSetting`
- `program.core.util.mathematics.extras.Coordinate2Long`

**Method intent:**
- `public void constructor(EntityInstance focalEntity, WindowInstance windowInstance, int totalSlots, long[] loadOrder, LongOpenHashSet gridCoordinates, Long2ObjectOpenHashMap<GridSlotHandle> gridSlots, float radiusSquared, int maxChunks)` — Engine-side initialization entrypoint invoked post-create.
- `private void rebuildRenderQueue()` — Performs class-specific logic; see call sites and owning manager flow.
- `private void queueChunk(GridSlotHandle slot, long chunkCoordinate)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void queueMega(GridSlotHandle slot, long chunkCoordinate)` — Performs class-specific logic; see call sites and owning manager flow.
- `public boolean updateActiveChunkCoordinate()` — Runs frame-step maintenance and logic.
- `public long getActiveChunkCoordinate()` — Returns current state/value.
- `public GridSlotHandle getNextScanSlot()` — Returns current state/value.
- `public long getChunkCoordinateForSlot(long gridCoordinate)` — Returns current state/value.
- `public long getMegaCoordinateForSlot(long gridCoordinate)` — Returns current state/value.
- `public GridSlotHandle getGridSlotForChunk(long chunkCoordinate)` — Returns current state/value.
- `public EntityInstance getFocalEntity()` — Returns current state/value.
- `public WindowInstance getWindowInstance()` — Returns current state/value.
- `public WorldHandle getWorldHandle()` — Returns current state/value.
- `public int getTotalSlots()` — Returns current state/value.
- `public long[] getLoadOrder()` — Returns current state/value.
- `public long getGridCoordinate(int i)` — Returns current state/value.
- `public LongOpenHashSet getGridCoordinates()` — Returns current state/value.
- `public GridSlotHandle getGridSlot(long gridCoordinate)` — Returns current state/value.
- `public float getRadiusSquared()` — Returns current state/value.
- `public Long2ObjectLinkedOpenHashMap<ChunkInstance> getActiveChunks()` — Returns current state/value.
- `public Long2ObjectLinkedOpenHashMap<MegaChunkInstance> getActiveMegaChunks()` — Returns current state/value.
- `public LongLinkedOpenHashSet getLoadRequests()` — Returns current state/value.
- `public LongLinkedOpenHashSet getUnloadRequests()` — Returns current state/value.
- `public Long2ObjectLinkedOpenHashMap<GridSlotHandle> getChunkRenderQueue()` — Returns current state/value.
- `public Long2ObjectLinkedOpenHashMap<GridSlotHandle> getMegaRenderQueue()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/gridmanager/GridBuildSystem.java`

**Type:** `class GridBuildSystem`
  
**Inheritance/implements:** `extends SystemPackage`
  
**Package:** `program.bootstrap.worldpipeline.gridmanager`
  
**File size:** 235 lines

**What this class does:** `GridBuildSystem` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.gridmanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.entitypipeline.entity.EntityInstance`
- `program.bootstrap.shaderpipeline.ubo.UBOHandle`
- `program.bootstrap.shaderpipeline.ubo.UBOInstance`
- `program.bootstrap.shaderpipeline.ubomanager.UBOManager`
- `program.bootstrap.worldpipeline.grid.GridInstance`
- `program.bootstrap.worldpipeline.gridslot.GridSlotDetailLevel`
- `program.bootstrap.worldpipeline.gridslot.GridSlotHandle`
- `program.core.engine.SystemPackage`
- `program.core.kernel.window.WindowInstance`
- `program.core.settings.EngineSetting`
- `program.core.util.mathematics.extras.Coordinate2Long`
- `program.core.util.mathematics.vectors.Vector2`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void get()` — Returns current state/value.
- `package GridInstance buildGrid(EntityInstance focalEntity, WindowInstance windowInstance)` — Constructs derived runtime/handle data from source input.
- `private float calculateRadius()` — Performs class-specific logic; see call sites and owning manager flow.
- `private long[] assignLoadOrder(float radius)` — Performs class-specific logic; see call sites and owning manager flow.
- `private Long2ObjectOpenHashMap<GridSlotHandle> createGridSlotHandles(LongOpenHashSet gridCoordinates, GridInstance gridInstance)` — Allocates/initializes child systems or resources.
- `private GridSlotHandle createGridSlotHandle(long gridCoordinate, UBOInstance slotUBO, float chunkDistanceFromCenter, float chunkAngleFromCenter, float megaDistanceFromCenter, float megaAngleFromCenter, GridSlotDetailLevel detailLevel, GridInstance gridInstance)` — Allocates/initializes child systems or resources.
- `private void populateCoveredSlots(Long2ObjectOpenHashMap<GridSlotHandle> gridSlots)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/gridmanager/GridManager.java`

**Type:** `class GridManager`
  
**Inheritance/implements:** `extends ManagerPackage`
  
**Package:** `program.bootstrap.worldpipeline.gridmanager`
  
**File size:** 30 lines

**What this class does:** `GridManager` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.gridmanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.entitypipeline.entity.EntityInstance`
- `program.bootstrap.worldpipeline.grid.GridInstance`
- `program.core.engine.ManagerPackage`
- `program.core.kernel.window.WindowInstance`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `public GridInstance buildGrid(EntityInstance focalEntity, WindowInstance windowInstance)` — Constructs derived runtime/handle data from source input.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/gridslot/GridSlotDetailLevel.java`

**Type:** `enum GridSlotDetailLevel`
  
**Package:** `program.bootstrap.worldpipeline.gridslot`
  
**File size:** 35 lines

**What this class does:** `GridSlotDetailLevel` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.gridslot`.

**Who this class talks to (direct imports):**
- `program.bootstrap.worldpipeline.worldrendermanager.RenderType`

**Method intent:**
- `package  GridSlotDetailLevel(int level, int maxChunkDistance, RenderType renderMode)` — Performs class-specific logic; see call sites and owning manager flow.
- `public GridSlotDetailLevel getDetailLevelForDistance(float absoluteChunkDistance)` — Returns current state/value. Called via static reference from: `core/src/program/bootstrap/worldpipeline/gridmanager/GridBuildSystem.java`.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/gridslot/GridSlotHandle.java`

**Type:** `class GridSlotHandle`
  
**Inheritance/implements:** `extends HandlePackage`
  
**Package:** `program.bootstrap.worldpipeline.gridslot`
  
**File size:** 101 lines

**What this class does:** `GridSlotHandle` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.gridslot`.

**Who this class talks to (direct imports):**
- `program.bootstrap.shaderpipeline.ubo.UBOInstance`
- `program.bootstrap.worldpipeline.grid.GridInstance`
- `program.core.engine.HandlePackage`
- `program.core.util.mathematics.extras.Coordinate2Long`

**Method intent:**
- `public void constructor(long gridCoordinate, UBOInstance slotUBO, float chunkDistanceFromCenter, float chunkAngleFromCenter, float megaDistanceFromCenter, float megaAngleFromCenter, GridSlotDetailLevel detailLevel, GridInstance gridInstance)` — Engine-side initialization entrypoint invoked post-create.
- `public long getChunkCoordinate()` — Returns current state/value.
- `public long getMegaCoordinate()` — Returns current state/value.
- `public long getGridCoordinate()` — Returns current state/value.
- `public UBOInstance getSlotUBO()` — Returns current state/value.
- `public float getChunkDistanceFromCenter()` — Returns current state/value.
- `public float getChunkAngleFromCenter()` — Returns current state/value.
- `public float getMegaDistanceFromCenter()` — Returns current state/value.
- `public float getMegaAngleFromCenter()` — Returns current state/value.
- `public GridSlotDetailLevel getDetailLevel()` — Returns current state/value.
- `public ObjectArrayList<GridSlotHandle> getCoveredSlots()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/megachunk/MegaBatchStruct.java`

**Type:** `class MegaBatchStruct`
  
**Inheritance/implements:** `extends StructPackage`
  
**Package:** `program.bootstrap.worldpipeline.megachunk`
  
**File size:** 109 lines

**What this class does:** `MegaBatchStruct` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.megachunk`.

**Who this class talks to (direct imports):**
- `program.bootstrap.worldpipeline.chunk.ChunkInstance`
- `program.core.engine.StructPackage`
- `program.core.util.mathematics.extras.Coordinate2Long`

**Method intent:**
- `package public MegaBatchStruct()` — Performs class-specific logic; see call sites and owning manager flow.
- `public void constructor(long megaChunkCoordinate, int megaScale)` — Engine-side initialization entrypoint invoked post-create.
- `public void reset()` — Performs class-specific logic; see call sites and owning manager flow.
- `public boolean registerChunk(ChunkInstance chunkInstance)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void updateChunk(long coord, ChunkInstance chunkInstance)` — Runs frame-step maintenance and logic.
- `public void recordMerged(long coordinate)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void clearMerged()` — Performs class-specific logic; see call sites and owning manager flow.
- `public boolean isReadyToRender()` — Performs class-specific logic; see call sites and owning manager flow.
- `public boolean isEmpty()` — Performs class-specific logic; see call sites and owning manager flow.
- `public long getMegaChunkCoordinate()` — Returns current state/value.
- `public Long2ObjectOpenHashMap<ChunkInstance> getBatchedChunks()` — Returns current state/value.
- `public ObjectArrayList<ChunkInstance> getBatchedChunkList()` — Returns current state/value.
- `public ChunkInstance getBatchedChunk(long chunkCoordinate)` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/megachunk/MegaChunkInstance.java`

**Type:** `class MegaChunkInstance`
  
**Inheritance/implements:** `extends WorldRenderInstance`
  
**Package:** `program.bootstrap.worldpipeline.megachunk`
  
**File size:** 177 lines

**What this class does:** `MegaChunkInstance` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.megachunk`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.vao.VAOHandle`
- `program.bootstrap.worldpipeline.chunk.ChunkInstance`
- `program.bootstrap.worldpipeline.world.WorldHandle`
- `program.bootstrap.worldpipeline.worldrendermanager.RenderType`
- `program.bootstrap.worldpipeline.worldrendermanager.WorldRenderInstance`
- `program.bootstrap.worldpipeline.worldrendermanager.WorldRenderManager`
- `program.core.settings.EngineSetting`
- `program.core.util.mathematics.extras.Coordinate2Long`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `public void constructor(WorldRenderManager worldRenderManager, WorldHandle worldHandle, long megaChunkCoordinate, VAOHandle vaoHandle, int megaScale)` — Engine-side initialization entrypoint invoked post-create.
- `public void reset()` — Performs class-specific logic; see call sites and owning manager flow.
- `public boolean batchAndMerge(ChunkInstance chunkInstance)` — Performs class-specific logic; see call sites and owning manager flow.
- `private boolean mergeChunk(ChunkInstance chunkInstance)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void finalizeGeometry()` — Performs class-specific logic; see call sites and owning manager flow.
- `public MegaDataSyncContainer getMegaDataSyncContainer()` — Returns current state/value.
- `public boolean isReadyToRender()` — Performs class-specific logic; see call sites and owning manager flow.
- `public Long2ObjectOpenHashMap<ChunkInstance> getBatchedChunks()` — Returns current state/value.
- `public ObjectArrayList<ChunkInstance> getBatchedChunkList()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/megachunk/MegaData.java`

**Type:** `enum MegaData`
  
**Package:** `program.bootstrap.worldpipeline.megachunk`
  
**File size:** 68 lines

**What this class does:** `MegaData` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.megachunk`.

**Who this class talks to (direct imports):**
- `program.bootstrap.worldpipeline.gridslot.GridSlotDetailLevel`

**Method intent:**
- `package  MegaData(boolean dumpable, GridSlotDetailLevel maximumLevel, String[] requiresNames, String[] leadsToNames)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void link()` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/megachunk/MegaDataSyncContainer.java`

**Type:** `class MegaDataSyncContainer`
  
**Inheritance/implements:** `extends SyncContainerPackage`
  
**Package:** `program.bootstrap.worldpipeline.megachunk`
  
**File size:** 61 lines

**What this class does:** `MegaDataSyncContainer` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.megachunk`.

**Who this class talks to (direct imports):**
- `program.core.engine.SyncContainerPackage`

**Method intent:**
- `public void create()` — Allocates/initializes child systems or resources.
- `public void resetData()` — Performs class-specific logic; see call sites and owning manager flow.
- `public boolean[] getData()` — Returns current state/value.
- `public boolean hasData(MegaData dataType)` — Boolean existence/availability check.
- `public boolean setData(MegaData dataType, boolean value)` — Mutates internal state for this object.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/megachunk/MegaDataUtility.java`

**Type:** `class MegaDataUtility`
  
**Inheritance/implements:** `extends UtilityPackage`
  
**Package:** `program.bootstrap.worldpipeline.megachunk`
  
**File size:** 120 lines

**What this class does:** `MegaDataUtility` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.megachunk`.

**Who this class talks to (direct imports):**
- `program.bootstrap.worldpipeline.gridslot.GridSlotDetailLevel`
- `program.core.engine.UtilityPackage`

**Method intent:**
- `public MegaData nextToLoad(boolean[] flags, GridSlotDetailLevel slotLevel)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/worldpipeline/megastreammanager/MegaQueueManager.java`.
- `private boolean isNeeded(MegaData stage, GridSlotDetailLevel slotLevel)` — Performs class-specific logic; see call sites and owning manager flow.
- `private boolean requiresMet(MegaData stage, boolean[] flags)` — Performs class-specific logic; see call sites and owning manager flow.
- `public MegaData nextToDump(boolean[] flags, GridSlotDetailLevel slotLevel)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/worldpipeline/megastreammanager/MegaQueueManager.java`.
- `private boolean leadsToSafe(MegaData stage, boolean[] flags)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void cascadeClear(MegaData stage, boolean[] flags)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/megastreammanager/MegaAssessBranch.java`

**Type:** `class MegaAssessBranch`
  
**Inheritance/implements:** `extends BranchPackage`
  
**Package:** `program.bootstrap.worldpipeline.megastreammanager`
  
**File size:** 32 lines

**What this class does:** `MegaAssessBranch` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.megastreammanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.worldpipeline.megachunk.MegaChunkInstance`
- `program.bootstrap.worldpipeline.megachunk.MegaData`
- `program.bootstrap.worldpipeline.megachunk.MegaDataSyncContainer`
- `program.core.engine.BranchPackage`

**Method intent:**
- `public void assessMega(MegaChunkInstance mega)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/megastreammanager/MegaDumpBranch.java`

**Type:** `class MegaDumpBranch`
  
**Inheritance/implements:** `extends BranchPackage`
  
**Package:** `program.bootstrap.worldpipeline.megastreammanager`
  
**File size:** 82 lines

**What this class does:** `MegaDumpBranch` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.megastreammanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.worldpipeline.chunk.ChunkData`
- `program.bootstrap.worldpipeline.chunk.ChunkDataSyncContainer`
- `program.bootstrap.worldpipeline.chunk.ChunkInstance`
- `program.bootstrap.worldpipeline.megachunk.MegaChunkInstance`
- `program.bootstrap.worldpipeline.megachunk.MegaData`
- `program.bootstrap.worldpipeline.megachunk.MegaDataSyncContainer`
- `program.bootstrap.worldpipeline.worldrendermanager.WorldRenderManager`
- `program.core.engine.BranchPackage`

**Method intent:**
- `protected void get()` — Returns current state/value.
- `public void dumpMega(MegaChunkInstance mega, MegaDataSyncContainer sync, long megaCoord)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void clearChunkBatchFlags(MegaChunkInstance mega)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/megastreammanager/MegaMergeBranch.java`

**Type:** `class MegaMergeBranch`
  
**Inheritance/implements:** `extends BranchPackage`
  
**Package:** `program.bootstrap.worldpipeline.megastreammanager`
  
**File size:** 52 lines

**What this class does:** `MegaMergeBranch` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.megastreammanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.worldpipeline.chunk.ChunkInstance`
- `program.bootstrap.worldpipeline.megachunk.MegaChunkInstance`
- `program.bootstrap.worldpipeline.megachunk.MegaData`
- `program.bootstrap.worldpipeline.megachunk.MegaDataSyncContainer`
- `program.core.engine.BranchPackage`

**Method intent:**
- `protected void get()` — Returns current state/value.
- `public void mergeChunkIntoMega(ChunkInstance chunkInstance, MegaChunkInstance mega)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/megastreammanager/MegaQueueManager.java`

**Type:** `class MegaQueueManager`
  
**Inheritance/implements:** `extends ManagerPackage`
  
**Package:** `program.bootstrap.worldpipeline.megastreammanager`
  
**File size:** 351 lines

**What this class does:** `MegaQueueManager` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.megastreammanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.worldpipeline.chunk.ChunkData`
- `program.bootstrap.worldpipeline.chunk.ChunkDataSyncContainer`
- `program.bootstrap.worldpipeline.chunk.ChunkInstance`
- `program.bootstrap.worldpipeline.chunkstreammanager.ChunkStreamManager`
- `program.bootstrap.worldpipeline.grid.GridInstance`
- `program.bootstrap.worldpipeline.gridslot.GridSlotDetailLevel`
- `program.bootstrap.worldpipeline.gridslot.GridSlotHandle`
- `program.bootstrap.worldpipeline.megachunk.MegaChunkInstance`
- `program.bootstrap.worldpipeline.megachunk.MegaData`
- `program.bootstrap.worldpipeline.megachunk.MegaDataSyncContainer`
- `program.bootstrap.worldpipeline.megachunk.MegaDataUtility`
- `program.bootstrap.worldpipeline.worldrendermanager.WorldRenderManager`
- `program.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager`
- `program.core.engine.ManagerPackage`
- `program.core.settings.EngineSetting`
- `program.core.util.mathematics.extras.Coordinate2Long`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void get()` — Returns current state/value.
- `protected void update()` — Runs frame-step maintenance and logic.
- `package void onGridRebuilt(GridInstance grid)` — Performs class-specific logic; see call sites and owning manager flow.
- `package void onGridRemoved(GridInstance grid)` — Performs class-specific logic; see call sites and owning manager flow.
- `private int computeMegaMax(GridInstance grid)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void flushActiveMegas(GridInstance grid)` — Performs class-specific logic; see call sites and owning manager flow.
- `package void batchChunk(ChunkInstance chunkInstance, GridInstance grid)` — Performs class-specific logic; see call sites and owning manager flow.
- `private MegaChunkInstance createMega(long megaCoord, GridInstance grid, int megaMax, Long2ObjectLinkedOpenHashMap<MegaChunkInstance> activeMegaChunks)` — Allocates/initializes child systems or resources.
- `private MegaChunkInstance configureMega(MegaChunkInstance mega, long megaCoord, GridInstance grid)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void assessActiveMegas(GridInstance grid)` — Performs class-specific logic; see call sites and owning manager flow.
- `private MegaQueueOperation determineOperation(MegaDataSyncContainer sync, GridSlotHandle gridSlotHandle)` — Performs class-specific logic; see call sites and owning manager flow.
- `private MegaQueueOperation toOperation(MegaData stage)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void unloadMega(MegaChunkInstance mega, long megaCoord, int megaMax)` — Performs class-specific logic; see call sites and owning manager flow.
- `package void invalidateMegaForChunk(long chunkCoordinate)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void clearChunkBatchFlags(MegaChunkInstance mega)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/megastreammanager/MegaQueueOperation.java`

**Type:** `enum MegaQueueOperation`
  
**Package:** `program.bootstrap.worldpipeline.megastreammanager`
  
**File size:** 14 lines

**What this class does:** `MegaQueueOperation` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.megastreammanager`.

**Method intent:** No concrete methods parsed (interface marker, enum, or data-only declaration).

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/megastreammanager/MegaRenderBranch.java`

**Type:** `class MegaRenderBranch`
  
**Inheritance/implements:** `extends BranchPackage`
  
**Package:** `program.bootstrap.worldpipeline.megastreammanager`
  
**File size:** 81 lines

**What this class does:** `MegaRenderBranch` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.megastreammanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.worldpipeline.chunk.ChunkData`
- `program.bootstrap.worldpipeline.chunk.ChunkDataSyncContainer`
- `program.bootstrap.worldpipeline.chunk.ChunkInstance`
- `program.bootstrap.worldpipeline.megachunk.MegaChunkInstance`
- `program.bootstrap.worldpipeline.megachunk.MegaData`
- `program.bootstrap.worldpipeline.megachunk.MegaDataSyncContainer`
- `program.bootstrap.worldpipeline.worldrendermanager.WorldRenderManager`
- `program.core.engine.BranchPackage`

**Method intent:**
- `protected void get()` — Returns current state/value.
- `public void renderMega(MegaChunkInstance mega, MegaDataSyncContainer sync)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/megastreammanager/MegaStreamManager.java`

**Type:** `class MegaStreamManager`
  
**Inheritance/implements:** `extends ManagerPackage`
  
**Package:** `program.bootstrap.worldpipeline.megastreammanager`
  
**File size:** 44 lines

**What this class does:** `MegaStreamManager` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.megastreammanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.worldpipeline.chunk.ChunkInstance`
- `program.bootstrap.worldpipeline.grid.GridInstance`
- `program.core.engine.ManagerPackage`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `public void onGridRebuilt(GridInstance grid)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void onGridRemoved(GridInstance grid)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void batchChunk(ChunkInstance chunkInstance, GridInstance grid)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void invalidateMegaForChunk(long chunkCoordinate)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/subchunk/SubChunkInstance.java`

**Type:** `class SubChunkInstance`
  
**Inheritance/implements:** `extends WorldRenderInstance`
  
**Package:** `program.bootstrap.worldpipeline.subchunk`
  
**File size:** 107 lines

**What this class does:** `SubChunkInstance` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.subchunk`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.vao.VAOHandle`
- `program.bootstrap.worldpipeline.block.BlockPaletteHandle`
- `program.bootstrap.worldpipeline.world.WorldHandle`
- `program.bootstrap.worldpipeline.worlditem.WorldItemPaletteHandle`
- `program.bootstrap.worldpipeline.worldrendermanager.RenderType`
- `program.bootstrap.worldpipeline.worldrendermanager.WorldRenderInstance`
- `program.bootstrap.worldpipeline.worldrendermanager.WorldRenderManager`
- `program.core.settings.EngineSetting`
- `program.core.util.mathematics.extras.Coordinate3Int`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `public void constructor(WorldRenderManager worldRenderManager, WorldHandle worldHandle, long coordinate, VAOHandle vaoHandle, short airBlockId, short defaultBiomeId)` — Engine-side initialization entrypoint invoked post-create.
- `public void reset()` — Performs class-specific logic; see call sites and owning manager flow.
- `public BlockPaletteHandle getBiomePaletteHandle()` — Returns current state/value.
- `public BlockPaletteHandle getBlockPaletteHandle()` — Returns current state/value.
- `public BlockPaletteHandle getBlockRotationPaletteHandle()` — Returns current state/value.
- `public WorldItemPaletteHandle getWorldItemPaletteHandle()` — Returns current state/value.
- `public short getBlock(int x, int y, int z)` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/util/ChunkCoordinate3Int.java`

**Type:** `class ChunkCoordinate3Int`
  
**Inheritance/implements:** `extends UtilityPackage`
  
**Package:** `program.bootstrap.worldpipeline.util`
  
**File size:** 236 lines

**What this class does:** `ChunkCoordinate3Int` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.util`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.dynamicgeometrymanager.util.VertBlockNeighbor3Vector`
- `program.core.engine.UtilityPackage`
- `program.core.settings.EngineSetting`
- `program.core.util.mathematics.extras.Coordinate3Int`
- `program.core.util.mathematics.extras.Direction3Vector`

**Method intent:**
- `package private ChunkCoordinate3Int()` — Performs class-specific logic; see call sites and owning manager flow.
- `private void flattenBlockCoordinates()` — Performs class-specific logic; see call sites and owning manager flow.
- `private void flattenInteriorBlockCoordinates()` — Performs class-specific logic; see call sites and owning manager flow.
- `public int[] getBlockCoordinates()` — Returns current state/value.
- `public int getBlockCoordinate(int index)` — Returns current state/value. Called via static reference from: `core/src/program/bootstrap/geometrypipeline/dynamicgeometrymanager/InternalBuildManager.java`.
- `public int[] getInteriorBlockCoordinates()` — Returns current state/value. Called via static reference from: `core/src/program/bootstrap/worldpipeline/block/BlockPaletteHandle.java`.
- `public int getInteriorBlockCoordinate(int index)` — Returns current state/value.
- `public int getIndex(int packed)` — Returns current state/value. Called via static reference from: `core/src/program/bootstrap/geometrypipeline/dynamicgeometrymanager/FullGeometryBranch.java`.
- `public int convertToBlockSpace(int vertPacked, VertBlockNeighbor3Vector direction)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/geometrypipeline/dynamicgeometrymanager/FullGeometryBranch.java`.
- `public int getNeighbor(int packed, Direction3Vector direction)` — Returns current state/value.
- `public int getNeighborAndWrap(int packed, Direction3Vector direction)` — Returns current state/value. Called via static reference from: `core/src/program/bootstrap/geometrypipeline/dynamicgeometrymanager/FullGeometryBranch.java`.
- `public int getNeighborWithOffset(int packed, Direction3Vector tangent, int offset)` — Returns current state/value. Called via static reference from: `core/src/program/bootstrap/geometrypipeline/dynamicgeometrymanager/FullGeometryBranch.java`.
- `public boolean isAtEdge(int packed, Direction3Vector direction)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/geometrypipeline/dynamicgeometrymanager/FullGeometryBranch.java`.
- `public boolean isAtEdge(int packed, VertBlockNeighbor3Vector direction)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/geometrypipeline/dynamicgeometrymanager/FullGeometryBranch.java`.
- `public int convertToVertSpace(int packed, Direction3Vector direction)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/geometrypipeline/dynamicgeometrymanager/FullGeometryBranch.java`.
- `public int getNeighborFromVert(int vertPacked, VertBlockNeighbor3Vector neighbor)` — Returns current state/value. Called via static reference from: `core/src/program/bootstrap/geometrypipeline/dynamicgeometrymanager/FullGeometryBranch.java`.
- `public int getVertCoordinateFromOffset(int vertPacked, Direction3Vector direction, int offset)` — Returns current state/value. Called via static reference from: `core/src/program/bootstrap/geometrypipeline/dynamicgeometrymanager/FullGeometryBranch.java`.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/util/WorldPositionStruct.java`

**Type:** `class WorldPositionStruct`
  
**Inheritance/implements:** `extends StructPackage`
  
**Package:** `program.bootstrap.worldpipeline.util`
  
**File size:** 31 lines

**What this class does:** `WorldPositionStruct` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.util`.

**Who this class talks to (direct imports):**
- `program.core.engine.StructPackage`
- `program.core.util.mathematics.extras.Coordinate2Long`
- `program.core.util.mathematics.vectors.Vector3`

**Method intent:**
- `public Vector3 getPosition()` — Returns current state/value.
- `public void setPosition(Vector3 position)` — Mutates internal state for this object.
- `public long getChunkCoordinate()` — Returns current state/value.
- `public void setChunkCoordinate(long chunkCoordinate)` — Mutates internal state for this object.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/util/WorldPositionUtility.java`

**Type:** `class WorldPositionUtility`
  
**Inheritance/implements:** `extends UtilityPackage`
  
**Package:** `program.bootstrap.worldpipeline.util`
  
**File size:** 98 lines

**What this class does:** `WorldPositionUtility` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.util`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryType`
- `program.bootstrap.worldpipeline.block.BlockHandle`
- `program.bootstrap.worldpipeline.blockmanager.BlockManager`
- `program.bootstrap.worldpipeline.chunk.ChunkInstance`
- `program.bootstrap.worldpipeline.subchunk.SubChunkInstance`
- `program.bootstrap.worldpipeline.world.WorldHandle`
- `program.core.engine.UtilityPackage`
- `program.core.settings.EngineSetting`
- `program.core.util.mathematics.extras.Coordinate2Long`
- `program.core.util.mathematics.vectors.Vector2Int`

**Method intent:**
- `public long getRandomChunk(WorldHandle worldHandle)` — Returns current state/value. Called via static reference from: `core/src/program/bootstrap/entitypipeline/entitymanager/EntityManager.java`.
- `public int findSafeSpawnHeight(ChunkInstance chunkInstance, BlockManager blockManager, int blockX, int totalY, int blockZ)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/entitypipeline/playermanager/PlayerManager.java`.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/util/WorldWrapUtility.java`

**Type:** `class WorldWrapUtility`
  
**Inheritance/implements:** `extends UtilityPackage`
  
**Package:** `program.bootstrap.worldpipeline.util`
  
**File size:** 47 lines

**What this class does:** `WorldWrapUtility` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.util`.

**Who this class talks to (direct imports):**
- `program.bootstrap.worldpipeline.world.WorldHandle`
- `program.core.engine.UtilityPackage`
- `program.core.settings.EngineSetting`
- `program.core.util.mathematics.extras.Coordinate2Long`
- `program.core.util.mathematics.vectors.Vector2Int`
- `program.core.util.mathematics.vectors.Vector3`

**Method intent:**
- `public Vector3 wrapAroundChunk(Vector3 input)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/physicspipeline/movementmanager/MovementManager.java`.
- `public long wrapAroundWorld(WorldHandle worldHandle, long input)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/physicspipeline/movementmanager/MovementManager.java`, `core/src/program/bootstrap/worldpipeline/chunk/ChunkNeighborStruct.java`, `core/src/program/bootstrap/worldpipeline/grid/GridInstance.java`.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/world/WorldData.java`

**Type:** `class WorldData`
  
**Inheritance/implements:** `extends DataPackage`
  
**Package:** `program.bootstrap.worldpipeline.world`
  
**File size:** 101 lines

**What this class does:** `WorldData` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.world`.

**Who this class talks to (direct imports):**
- `program.core.util.image.Pixmap`
- `program.core.engine.DataPackage`
- `program.core.util.mathematics.vectors.Vector2Int`
- `program.core.util.mathematics.vectors.Vector3`

**Method intent:**
- `package public WorldData(String worldName, int worldID, Pixmap world, Vector2Int worldScale, float gravityMultiplier, Vector3 gravityDirection, float daysPerDay, String calendarName)` — Performs class-specific logic; see call sites and owning manager flow.
- `public String getWorldName()` — Returns current state/value.
- `public int getWorldID()` — Returns current state/value.
- `public Pixmap getWorld()` — Returns current state/value.
- `public Vector2Int getWorldScale()` — Returns current state/value.
- `public float getGravityMultiplier()` — Returns current state/value.
- `public Vector3 getGravityDirection()` — Returns current state/value.
- `public float getDaysPerDay()` — Returns current state/value.
- `public String getCalendarName()` — Returns current state/value.
- `public long getWorldEpochStart()` — Returns current state/value.
- `public void setWorldEpochStart(long worldEpochStart)` — Mutates internal state for this object.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/world/WorldHandle.java`

**Type:** `class WorldHandle`
  
**Inheritance/implements:** `extends HandlePackage`
  
**Package:** `program.bootstrap.worldpipeline.world`
  
**File size:** 70 lines

**What this class does:** `WorldHandle` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.world`.

**Who this class talks to (direct imports):**
- `program.core.util.image.Pixmap`
- `program.core.engine.HandlePackage`
- `program.core.util.mathematics.vectors.Vector2Int`
- `program.core.util.mathematics.vectors.Vector3`

**Method intent:**
- `public void constructor(WorldData data)` — Engine-side initialization entrypoint invoked post-create.
- `public WorldData getWorldData()` — Returns current state/value.
- `public String getWorldName()` — Returns current state/value.
- `public int getWorldID()` — Returns current state/value.
- `public Pixmap getWorld()` — Returns current state/value.
- `public Vector2Int getWorldScale()` — Returns current state/value.
- `public float getGravityMultiplier()` — Returns current state/value.
- `public Vector3 getGravityDirection()` — Returns current state/value.
- `public float getDaysPerDay()` — Returns current state/value.
- `public String getCalendarName()` — Returns current state/value.
- `public long getWorldEpochStart()` — Returns current state/value.
- `public void setWorldEpochStart(long worldEpochStart)` — Mutates internal state for this object.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/worldgenerationmanager/WorldGenerationManager.java`

**Type:** `class WorldGenerationManager`
  
**Inheritance/implements:** `extends ManagerPackage`
  
**Package:** `program.bootstrap.worldpipeline.worldgenerationmanager`
  
**File size:** 109 lines

**What this class does:** `WorldGenerationManager` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.worldgenerationmanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.worldpipeline.biomemanager.BiomeManager`
- `program.bootstrap.worldpipeline.block.BlockPaletteHandle`
- `program.bootstrap.worldpipeline.blockmanager.BlockManager`
- `program.bootstrap.worldpipeline.subchunk.SubChunkInstance`
- `program.core.engine.ManagerPackage`
- `program.core.settings.EngineSetting`
- `program.core.util.OpenSimplex2`
- `program.core.util.mathematics.extras.Coordinate2Long`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void get()` — Returns current state/value.
- `protected void awake()` — Runs startup-time runtime activation work.
- `public void setSeed(long seed)` — Mutates internal state for this object.
- `public long getSeed()` — Returns current state/value.
- `public boolean generateSubChunk(long chunkCoordinate, SubChunkInstance subChunkInstance)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/worlditem/WorldItemCompositeInstance.java`

**Type:** `class WorldItemCompositeInstance`
  
**Inheritance/implements:** `extends InstancePackage`
  
**Package:** `program.bootstrap.worldpipeline.worlditem`
  
**File size:** 34 lines

**What this class does:** `WorldItemCompositeInstance` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.worlditem`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.compositebuffer.CompositeBufferInstance`
- `program.bootstrap.shaderpipeline.material.MaterialInstance`
- `program.core.engine.InstancePackage`

**Method intent:**
- `public void constructor(MaterialInstance material, CompositeBufferInstance compositeBuffer)` — Engine-side initialization entrypoint invoked post-create.
- `public MaterialInstance getMaterial()` — Returns current state/value.
- `public CompositeBufferInstance getCompositeBuffer()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/worlditem/WorldItemInstance.java`

**Type:** `class WorldItemInstance`
  
**Inheritance/implements:** `extends InstancePackage`
  
**Package:** `program.bootstrap.worldpipeline.worlditem`
  
**File size:** 67 lines

**What this class does:** `WorldItemInstance` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.worlditem`.

**Who this class talks to (direct imports):**
- `program.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle`
- `program.core.engine.InstancePackage`

**Method intent:**
- `public void constructor(ItemDefinitionHandle itemDefinitionHandle, long chunkCoordinate, int packedBlockCoordinate, long packedPosition, int packedItem)` — Engine-side initialization entrypoint invoked post-create.
- `public ItemDefinitionHandle getItemDefinitionHandle()` — Returns current state/value.
- `public long getChunkCoordinate()` — Returns current state/value.
- `public int getPackedBlockCoordinate()` — Returns current state/value.
- `public long getPackedPosition()` — Returns current state/value.
- `public int getPackedItem()` — Returns current state/value.
- `public int getInstanceSlot()` — Returns current state/value.
- `public void setInstanceSlot(int slot)` — Mutates internal state for this object.
- `public void clearInstanceSlot()` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/worlditem/WorldItemInstancePaletteHandle.java`

**Type:** `class WorldItemInstancePaletteHandle`
  
**Inheritance/implements:** `extends HandlePackage`
  
**Package:** `program.bootstrap.worldpipeline.worlditem`
  
**File size:** 73 lines

**What this class does:** `WorldItemInstancePaletteHandle` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.worlditem`.

**Who this class talks to (direct imports):**
- `program.core.engine.HandlePackage`

**Method intent:**
- `public void constructor()` — Engine-side initialization entrypoint invoked post-create.
- `public void addItem(WorldItemInstance item)` — Registers a child object into manager-owned collections.
- `public void removeItem(WorldItemInstance item)` — Unregisters and tears down child references.
- `public void clear()` — Performs class-specific logic; see call sites and owning manager flow.
- `public ObjectArrayList<WorldItemInstance> getItems()` — Returns current state/value.
- `public ObjectArrayList<WorldItemInstance> getItemsAtBlock(int packedBlockCoordinate)` — Returns current state/value.
- `public boolean hasItemsAtBlock(int packedBlockCoordinate)` — Boolean existence/availability check.
- `public boolean isEmpty()` — Performs class-specific logic; see call sites and owning manager flow.
- `public int size()` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/worlditem/WorldItemPaletteHandle.java`

**Type:** `class WorldItemPaletteHandle`
  
**Inheritance/implements:** `extends HandlePackage`
  
**Package:** `program.bootstrap.worldpipeline.worlditem`
  
**File size:** 43 lines

**What this class does:** `WorldItemPaletteHandle` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.worlditem`.

**Who this class talks to (direct imports):**
- `program.core.engine.HandlePackage`

**Method intent:**
- `public void constructor()` — Engine-side initialization entrypoint invoked post-create.
- `public void addItem(WorldItemStruct item)` — Registers a child object into manager-owned collections.
- `public void removeItem(WorldItemStruct item)` — Unregisters and tears down child references.
- `public void clear()` — Performs class-specific logic; see call sites and owning manager flow.
- `public ObjectArrayList<WorldItemStruct> getItems()` — Returns current state/value.
- `public boolean isEmpty()` — Performs class-specific logic; see call sites and owning manager flow.
- `public int size()` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/worlditem/WorldItemStruct.java`

**Type:** `class WorldItemStruct`
  
**Inheritance/implements:** `extends StructPackage`
  
**Package:** `program.bootstrap.worldpipeline.worlditem`
  
**File size:** 14 lines

**What this class does:** `WorldItemStruct` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.worlditem`.

**Who this class talks to (direct imports):**
- `program.core.engine.StructPackage`

**Method intent:**
- `package public WorldItemStruct(long packedPosition, int packedItem)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/worlditemplacementsystem/WorldItemPlacementSystem.java`

**Type:** `class WorldItemPlacementSystem`
  
**Inheritance/implements:** `extends ManagerPackage`
  
**Package:** `program.bootstrap.worldpipeline.worlditemplacementsystem`
  
**File size:** 170 lines

**What this class does:** `WorldItemPlacementSystem` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.worlditemplacementsystem`.

**Who this class talks to (direct imports):**
- `program.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle`
- `program.bootstrap.itempipeline.itemdefinitionmanager.ItemDefinitionManager`
- `program.bootstrap.worldpipeline.chunk.ChunkInstance`
- `program.bootstrap.worldpipeline.subchunk.SubChunkInstance`
- `program.bootstrap.worldpipeline.worlditem.WorldItemInstance`
- `program.bootstrap.worldpipeline.worlditem.WorldItemInstancePaletteHandle`
- `program.bootstrap.worldpipeline.worlditem.WorldItemPaletteHandle`
- `program.bootstrap.worldpipeline.worlditem.WorldItemStruct`
- `program.bootstrap.worldpipeline.worlditemrendersystem.WorldItemRenderSystem`
- `program.core.engine.ManagerPackage`
- `program.core.settings.EngineSetting`
- `program.core.util.mathematics.extras.Coordinate3Int`
- `program.core.util.mathematics.extras.Coordinate4Long`

**Method intent:**
- `protected void get()` — Returns current state/value.
- `public void buildChunkInstances(ChunkInstance chunk, long chunkCoordinate)` — Constructs derived runtime/handle data from source input.
- `public void pushChunkToRenderer(ChunkInstance chunk, long chunkCoordinate)` — Queues data for downstream systems (often render queues).
- `public void pullChunkFromRenderer(long chunkCoordinate)` — Performs class-specific logic; see call sites and owning manager flow.
- `public WorldItemInstance placeItem(ChunkInstance chunk, int subChunkCoordinate, long packedPosition, int packedItem, ItemDefinitionHandle def)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void removeItem(ChunkInstance chunk, WorldItemInstance instance)` — Unregisters and tears down child references.
- `private WorldItemInstance buildInstance(WorldItemStruct struct, long chunkCoordinate)` — Constructs derived runtime/handle data from source input.
- `private WorldItemInstance buildInstance(WorldItemStruct struct, long chunkCoordinate, ItemDefinitionHandle def)` — Constructs derived runtime/handle data from source input.
- `private void removeMatchingStruct(SubChunkInstance subChunk, long packedPosition, int packedItem)` — Unregisters and tears down child references.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/worlditemrendersystem/WorldItemRenderSystem.java`

**Type:** `class WorldItemRenderSystem`
  
**Inheritance/implements:** `extends ManagerPackage`
  
**Package:** `program.bootstrap.worldpipeline.worlditemrendersystem`
  
**File size:** 214 lines

**What this class does:** `WorldItemRenderSystem` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.worlditemrendersystem`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.compositebuffer.CompositeBufferInstance`
- `program.bootstrap.geometrypipeline.compositebuffermanager.CompositeBufferManager`
- `program.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle`
- `program.bootstrap.renderpipeline.rendermanager.RenderManager`
- `program.bootstrap.shaderpipeline.material.MaterialInstance`
- `program.bootstrap.shaderpipeline.materialmanager.MaterialManager`
- `program.bootstrap.worldpipeline.worlditem.WorldItemCompositeInstance`
- `program.bootstrap.worldpipeline.worlditem.WorldItemInstance`
- `program.core.engine.ManagerPackage`
- `program.core.kernel.window.WindowInstance`
- `program.core.kernel.windowmanager.WindowManager`
- `program.core.settings.EngineSetting`
- `program.core.util.mathematics.extras.Coordinate2Long`
- `program.core.util.mathematics.extras.Coordinate4Long`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void get()` — Returns current state/value.
- `protected void update()` — Runs frame-step maintenance and logic.
- `public void push(long chunkCoordinate, ObjectArrayList<WorldItemInstance> items)` — Queues data for downstream systems (often render queues).
- `public void pull(long chunkCoordinate)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void addItem(WorldItemInstance instance, long chunkCoordinate)` — Registers a child object into manager-owned collections.
- `public void removeItem(WorldItemInstance instance)` — Unregisters and tears down child references.
- `private void addToBuffer(WorldItemInstance instance, int chunkX, int chunkZ)` — Registers a child object into manager-owned collections.
- `private void removeFromBuffer(WorldItemInstance instance)` — Unregisters and tears down child references.
- `private WorldItemCompositeInstance getOrCreateComposite(ItemDefinitionHandle def)` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/worldmanager/InternalBuilder.java`

**Type:** `class InternalBuilder`
  
**Inheritance/implements:** `extends BuilderPackage`
  
**Package:** `program.bootstrap.worldpipeline.worldmanager`
  
**File size:** 98 lines

**What this class does:** `InternalBuilder` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.worldmanager`.

**Who this class talks to (direct imports):**
- `program.core.util.image.Pixmap`
- `program.bootstrap.worldpipeline.world.WorldData`
- `program.bootstrap.worldpipeline.world.WorldHandle`
- `program.core.engine.BuilderPackage`
- `program.core.settings.EngineSetting`
- `program.core.util.FileUtility`
- `program.core.util.JsonUtility`
- `program.core.util.RegistryUtility`
- `program.core.util.mathematics.vectors.Vector2Int`
- `program.core.util.mathematics.vectors.Vector3`

**Method intent:**
- `package WorldHandle build(File file, File root, String worldName)` — Constructs derived runtime/handle data from source input.
- `private File resolveCompanionJson(File pngFile)` — Performs class-specific logic; see call sites and owning manager flow.
- `private Vector2Int calculateWorldScale(Pixmap pixmap)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/worldmanager/InternalLoader.java`

**Type:** `class InternalLoader`
  
**Inheritance/implements:** `extends LoaderPackage`
  
**Package:** `program.bootstrap.worldpipeline.worldmanager`
  
**File size:** 87 lines

**What this class does:** `InternalLoader` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.worldmanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.worldpipeline.world.WorldHandle`
- `program.core.engine.LoaderPackage`
- `program.core.settings.EngineSetting`
- `program.core.util.FileUtility`

**Method intent:**
- `protected void scan()` — Discovers files/resources for later load.
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void get()` — Returns current state/value.
- `protected void load(File file)` — Parses external data into engine objects.
- `package void request(String worldName)` — Triggers on-demand loading or lookup.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/worldmanager/WorldManager.java`

**Type:** `class WorldManager`
  
**Inheritance/implements:** `extends ManagerPackage`
  
**Package:** `program.bootstrap.worldpipeline.worldmanager`
  
**File size:** 96 lines

**What this class does:** `WorldManager` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.worldmanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.worldpipeline.world.WorldHandle`
- `program.core.engine.ManagerPackage`
- `program.core.settings.EngineSetting`
- `program.core.util.RegistryUtility`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `package void addWorld(String worldName, WorldHandle worldHandle)` — Registers a child object into manager-owned collections.
- `public boolean hasWorld(String worldName)` — Boolean existence/availability check.
- `public int getWorldIDFromWorldName(String worldName)` — Returns current state/value.
- `public WorldHandle getWorldHandleFromWorldID(int worldID)` — Returns current state/value.
- `public WorldHandle getWorldHandleFromWorldName(String worldName)` — Returns current state/value.
- `public WorldHandle getActiveWorld()` — Returns current state/value.
- `public void setActiveWorld(String worldName)` — Mutates internal state for this object.
- `public void request(String worldName)` — Triggers on-demand loading or lookup.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/worldrendermanager/FrustumCullingSystem.java`

**Type:** `class FrustumCullingSystem`
  
**Inheritance/implements:** `extends SystemPackage`
  
**Package:** `program.bootstrap.worldpipeline.worldrendermanager`
  
**File size:** 147 lines

**What this class does:** `FrustumCullingSystem` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.worldrendermanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.worldpipeline.grid.GridInstance`
- `program.bootstrap.worldpipeline.gridslot.GridSlotHandle`
- `program.core.engine.SystemPackage`
- `program.core.kernel.window.WindowInstance`
- `program.core.settings.EngineSetting`
- `program.core.util.camera.CameraInstance`

**Method intent:**
- `protected void awake()` — Runs startup-time runtime activation work.
- `package void refresh(GridInstance grid)` — Performs class-specific logic; see call sites and owning manager flow.
- `package boolean isChunkVisible(GridSlotHandle slot)` — Performs class-specific logic; see call sites and owning manager flow.
- `package boolean isMegaVisible(GridSlotHandle slot)` — Performs class-specific logic; see call sites and owning manager flow.
- `private boolean isWithinAngle(float slotAngle, float tolerance)` — Performs class-specific logic; see call sites and owning manager flow.
- `private float getCameraAngle(CameraInstance camera)` — Returns current state/value.
- `private float getDiagonalHalfFov(CameraInstance camera)` — Returns current state/value.
- `private float getAbsPitch(CameraInstance camera)` — Returns current state/value.
- `private float getPitchT(float absPitch, float power)` — Returns current state/value.
- `private float getEffectiveHalfAngle(float halfFov, float t)` — Returns current state/value.
- `private float getPitchMaxDistanceSq(float t)` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/worldrendermanager/RenderOperation.java`

**Type:** `enum RenderOperation`
  
**Package:** `program.bootstrap.worldpipeline.worldrendermanager`
  
**File size:** 15 lines

**What this class does:** `RenderOperation` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.worldrendermanager`.

**Method intent:** No concrete methods parsed (interface marker, enum, or data-only declaration).

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/worldrendermanager/RenderType.java`

**Type:** `enum RenderType`
  
**Package:** `program.bootstrap.worldpipeline.worldrendermanager`
  
**File size:** 15 lines

**What this class does:** `RenderType` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.worldrendermanager`.

**Method intent:** No concrete methods parsed (interface marker, enum, or data-only declaration).

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/worldrendermanager/WorldRenderInstance.java`

**Type:** `class WorldRenderInstance`
  
**Inheritance/implements:** `extends InstancePackage`
  
**Package:** `program.bootstrap.worldpipeline.worldrendermanager`
  
**File size:** 73 lines

**What this class does:** `WorldRenderInstance` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.worldrendermanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.dynamicpacket.DynamicPacketInstance`
- `program.bootstrap.geometrypipeline.vao.VAOHandle`
- `program.bootstrap.worldpipeline.world.WorldHandle`
- `program.core.engine.InstancePackage`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `public void constructor(WorldRenderManager worldRenderSystem, WorldHandle worldHandle, RenderType renderType, long coordinate, VAOHandle vaoHandle)` — Engine-side initialization entrypoint invoked post-create.
- `public void dispose()` — Releases owned resources and unregisters state.
- `public WorldHandle getWorldHandle()` — Returns current state/value.
- `public long getCoordinate()` — Returns current state/value.
- `public DynamicPacketInstance getDynamicPacketInstance()` — Returns current state/value.
- `protected DynamicPacketInstance getDynamicPacket()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/worldrendermanager/WorldRenderManager.java`

**Type:** `class WorldRenderManager`
  
**Inheritance/implements:** `extends ManagerPackage`
  
**Package:** `program.bootstrap.worldpipeline.worldrendermanager`
  
**File size:** 277 lines

**What this class does:** `WorldRenderManager` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.worldrendermanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.dynamicmodel.DynamicModelHandle`
- `program.bootstrap.geometrypipeline.dynamicpacket.DynamicPacketInstance`
- `program.bootstrap.geometrypipeline.dynamicpacket.DynamicPacketState`
- `program.bootstrap.geometrypipeline.model.ModelInstance`
- `program.bootstrap.geometrypipeline.modelmanager.ModelManager`
- `program.bootstrap.renderpipeline.rendermanager.RenderManager`
- `program.bootstrap.shaderpipeline.material.MaterialInstance`
- `program.bootstrap.shaderpipeline.materialmanager.MaterialManager`
- `program.bootstrap.shaderpipeline.ubo.UBOInstance`
- `program.bootstrap.worldpipeline.grid.GridInstance`
- `program.bootstrap.worldpipeline.gridslot.GridSlotHandle`
- `program.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager`
- `program.core.engine.ManagerPackage`
- `program.core.kernel.window.WindowInstance`
- `program.core.settings.EngineSetting`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void get()` — Returns current state/value.
- `protected void lateUpdate()` — Performs class-specific logic; see call sites and owning manager flow.
- `private void renderWorld()` — Performs class-specific logic; see call sites and owning manager flow.
- `private void renderGridMegas(GridInstance grid, WindowInstance window)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void renderGridChunks(GridInstance grid, WindowInstance window)` — Performs class-specific logic; see call sites and owning manager flow.
- `public boolean addChunkInstance(WorldRenderInstance worldRenderInstance)` — Registers a child object into manager-owned collections.
- `public boolean addMegaInstance(WorldRenderInstance worldRenderInstance)` — Registers a child object into manager-owned collections.
- `private boolean hasGridSlotForChunk(long coordinate)` — Boolean existence/availability check.
- `private ObjectArrayList<ModelInstance> buildModelList(WorldRenderInstance worldRenderInstance)` — Constructs derived runtime/handle data from source input.
- `public void removeChunkInstance(long coordinate)` — Unregisters and tears down child references.
- `public void removeMegaInstance(long coordinate)` — Unregisters and tears down child references.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/worldstreammanager/WorldStreamManager.java`

**Type:** `class WorldStreamManager`
  
**Inheritance/implements:** `extends ManagerPackage`
  
**Package:** `program.bootstrap.worldpipeline.worldstreammanager`
  
**File size:** 121 lines

**What this class does:** `WorldStreamManager` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.worldstreammanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.entitypipeline.entity.EntityInstance`
- `program.bootstrap.worldpipeline.chunk.ChunkInstance`
- `program.bootstrap.worldpipeline.chunkstreammanager.ChunkStreamManager`
- `program.bootstrap.worldpipeline.grid.GridInstance`
- `program.bootstrap.worldpipeline.gridmanager.GridManager`
- `program.bootstrap.worldpipeline.megastreammanager.MegaStreamManager`
- `program.bootstrap.worldpipeline.world.WorldHandle`
- `program.core.engine.ManagerPackage`
- `program.core.kernel.window.WindowInstance`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void get()` — Returns current state/value.
- `protected void update()` — Runs frame-step maintenance and logic. Called via static reference from: `core/src/program/bootstrap/worldpipeline/chunkstreammanager/ChunkStreamManager.java`.
- `public GridInstance createGrid(EntityInstance focalEntity, WindowInstance windowInstance)` — Allocates/initializes child systems or resources.
- `public void removeGrid(GridInstance grid)` — Unregisters and tears down child references.
- `public void rebuildGrid(GridInstance grid)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void invalidateChunkBatch(long chunkCoordinate)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void invalidateMegaForChunk(long chunkCoordinate)` — Performs class-specific logic; see call sites and owning manager flow.
- `public ObjectArrayList<GridInstance> getGrids()` — Returns current state/value.
- `public boolean hasGrids()` — Boolean existence/availability check.
- `public ChunkInstance getChunkInstance(long chunkCoordinate)` — Returns current state/value.
- `public WorldHandle getActiveWorldHandle()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.
