# GeometryPipelineDoc

This is a human-readable subsystem manual. It explains responsibilities, collaboration points, and method intent for each class in scope.

Classes covered: **45**

## How to read this manual
- Start with the package flow notes at the top of each class section.
- Use **Who talks to this class** to identify collaborators.
- Use **Method intent** to understand lifecycle and API behavior.

## `core/src/program/bootstrap/geometrypipeline/GeometryPipeline.java`

**Type:** `class GeometryPipeline`
  
**Inheritance/implements:** `extends PipelinePackage`
  
**Package:** `program.bootstrap.geometrypipeline`
  
**File size:** 30 lines

**What this class does:** `GeometryPipeline` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.geometrypipeline`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.compositebuffermanager.CompositeBufferManager`
- `program.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryManager`
- `program.bootstrap.geometrypipeline.ibomanager.IBOManager`
- `program.bootstrap.geometrypipeline.meshmanager.MeshManager`
- `program.bootstrap.geometrypipeline.modelmanager.ModelManager`
- `program.bootstrap.geometrypipeline.vaomanager.VAOManager`
- `program.bootstrap.geometrypipeline.vbomanager.VBOManager`
- `program.core.engine.PipelinePackage`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/geometrypipeline/compositebuffer/CompositeBufferData.java`

**Type:** `class CompositeBufferData`
  
**Inheritance/implements:** `extends DataPackage`
  
**Package:** `program.bootstrap.geometrypipeline.compositebuffer`
  
**File size:** 161 lines

**What this class does:** `CompositeBufferData` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.geometrypipeline.compositebuffer`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.mesh.MeshHandle`
- `program.core.engine.DataPackage`
- `program.core.settings.EngineSetting`

**Method intent:**
- `package public CompositeBufferData(MeshHandle meshHandle, int[] instanceAttrSizes)` — Performs class-specific logic; see call sites and owning manager flow.
- `public int getCompositeVAO()` — Returns current state/value.
- `public void setCompositeVAO(int compositeVAO)` — Mutates internal state for this object.
- `public int getInstanceVBO()` — Returns current state/value.
- `public void setInstanceVBO(int instanceVBO)` — Mutates internal state for this object.
- `public MeshHandle getMeshHandle()` — Returns current state/value.
- `public void setMeshHandle(MeshHandle meshHandle)` — Mutates internal state for this object.
- `public int[] getInstanceAttrSizes()` — Returns current state/value.
- `public void setInstanceAttrSizes(int[] instanceAttrSizes)` — Mutates internal state for this object.
- `public int getFloatsPerInstance()` — Returns current state/value.
- `public void setFloatsPerInstance(int floatsPerInstance)` — Mutates internal state for this object.
- `public int getIndexCount()` — Returns current state/value.
- `public void setIndexCount(int indexCount)` — Mutates internal state for this object.
- `public float[] getInstanceData()` — Returns current state/value.
- `public void setInstanceData(float[] instanceData)` — Mutates internal state for this object.
- `public int getInstanceCount()` — Returns current state/value.
- `public void setInstanceCount(int instanceCount)` — Mutates internal state for this object.
- `public int getMaxInstances()` — Returns current state/value.
- `public void setMaxInstances(int maxInstances)` — Mutates internal state for this object.
- `public int getCpuVersion()` — Returns current state/value.
- `public void setCpuVersion(int cpuVersion)` — Mutates internal state for this object.
- `public int getUploadedVersion()` — Returns current state/value.
- `public void setUploadedVersion(int uploadedVersion)` — Mutates internal state for this object.
- `public boolean isNeedsGpuRealloc()` — Performs class-specific logic; see call sites and owning manager flow.
- `public void setNeedsGpuRealloc(boolean needsGpuRealloc)` — Mutates internal state for this object.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/geometrypipeline/compositebuffer/CompositeBufferInstance.java`

**Type:** `class CompositeBufferInstance`
  
**Inheritance/implements:** `extends InstancePackage`
  
**Package:** `program.bootstrap.geometrypipeline.compositebuffer`
  
**File size:** 159 lines

**What this class does:** `CompositeBufferInstance` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.geometrypipeline.compositebuffer`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.mesh.MeshHandle`
- `program.core.engine.InstancePackage`

**Method intent:**
- `public void constructor(CompositeBufferData compositeBufferData)` — Engine-side initialization entrypoint invoked post-create.
- `public CompositeBufferData getCompositeBufferData()` — Returns current state/value.
- `public int getCompositeVAO()` — Returns current state/value.
- `public void setCompositeVAO(int vao)` — Mutates internal state for this object.
- `public int getInstanceVBO()` — Returns current state/value.
- `public void setInstanceVBO(int vbo)` — Mutates internal state for this object.
- `public MeshHandle getMeshHandle()` — Returns current state/value.
- `public int[] getInstanceAttrSizes()` — Returns current state/value.
- `public int getFloatsPerInstance()` — Returns current state/value.
- `public int getIndexCount()` — Returns current state/value.
- `public float[] getInstanceData()` — Returns current state/value.
- `public int getInstanceCount()` — Returns current state/value.
- `public int getMaxInstances()` — Returns current state/value.
- `public boolean needsUpload()` — Performs class-specific logic; see call sites and owning manager flow.
- `public boolean needsGpuRealloc()` — Performs class-specific logic; see call sites and owning manager flow.
- `public boolean isEmpty()` — Performs class-specific logic; see call sites and owning manager flow.
- `public int addInstance(float[] data)` — Registers a child object into manager-owned collections.
- `public void updateInstance(int index, float[] data)` — Runs frame-step maintenance and logic.
- `public int removeInstance(int index)` — Unregisters and tears down child references.
- `public void clear()` — Performs class-specific logic; see call sites and owning manager flow.
- `private void grow()` — Performs class-specific logic; see call sites and owning manager flow.
- `public void markUploaded()` — Performs class-specific logic; see call sites and owning manager flow.
- `public void clearNeedsGpuRealloc()` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/geometrypipeline/compositebuffermanager/CompositeBufferManager.java`

**Type:** `class CompositeBufferManager`
  
**Inheritance/implements:** `extends ManagerPackage`
  
**Package:** `program.bootstrap.geometrypipeline.compositebuffermanager`
  
**File size:** 60 lines

**What this class does:** `CompositeBufferManager` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.geometrypipeline.compositebuffermanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.compositebuffer.CompositeBufferData`
- `program.bootstrap.geometrypipeline.compositebuffer.CompositeBufferInstance`
- `program.bootstrap.geometrypipeline.mesh.MeshHandle`
- `program.core.engine.ManagerPackage`

**Method intent:**
- `public void constructor(CompositeBufferInstance buffer, MeshHandle meshHandle, int[] instanceAttrSizes)` — Engine-side initialization entrypoint invoked post-create.
- `public void grow(CompositeBufferInstance buffer)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void dispose(CompositeBufferInstance buffer)` — Releases owned resources and unregisters state.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/geometrypipeline/compositebuffermanager/GLSLUtility.java`

**Type:** `class GLSLUtility`
  
**Package:** `program.bootstrap.geometrypipeline.compositebuffermanager`
  
**File size:** 106 lines

**What this class does:** `GLSLUtility` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.geometrypipeline.compositebuffermanager`.

**Who this class talks to (direct imports):**
- `program.core.app.CoreContext`
- `program.core.util.graphics.gl.GL20`
- `program.core.util.graphics.gl.GL30`

**Method intent:**
- `package int createDynamicInstanceVBO(int maxInstances, int floatsPerInstance)` — Allocates/initializes child systems or resources. Called via static reference from: `core/src/program/bootstrap/geometrypipeline/compositebuffermanager/CompositeBufferManager.java`, `core/src/program/bootstrap/renderpipeline/compositerendersystem/CompositeRenderSystem.java`.
- `package int createInstancedVAO(int meshVBOHandle, int[] meshAttrSizes, int meshIBOHandle, int instanceVBOHandle, int[] instanceAttrSizes)` — Allocates/initializes child systems or resources. Called via static reference from: `core/src/program/bootstrap/geometrypipeline/compositebuffermanager/CompositeBufferManager.java`, `core/src/program/bootstrap/renderpipeline/compositerendersystem/CompositeRenderSystem.java`.
- `package void deleteBuffer(int handle)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/geometrypipeline/compositebuffermanager/CompositeBufferManager.java`, `core/src/program/bootstrap/renderpipeline/compositerendersystem/CompositeRenderSystem.java`.
- `package void deleteVAO(int handle)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/geometrypipeline/compositebuffermanager/CompositeBufferManager.java`, `core/src/program/bootstrap/renderpipeline/compositerendersystem/CompositeRenderSystem.java`.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/geometrypipeline/dynamicgeometrymanager/ComplexGeometryBranch.java`

**Type:** `class ComplexGeometryBranch`
  
**Inheritance/implements:** `extends BranchPackage`
  
**Package:** `program.bootstrap.geometrypipeline.dynamicgeometrymanager`
  
**File size:** 42 lines

**What this class does:** `ComplexGeometryBranch` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.geometrypipeline.dynamicgeometrymanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.dynamicpacket.DynamicPacketInstance`
- `program.bootstrap.worldpipeline.biome.BiomeHandle`
- `program.bootstrap.worldpipeline.block.BlockHandle`
- `program.bootstrap.worldpipeline.block.BlockPaletteHandle`
- `program.bootstrap.worldpipeline.chunk.ChunkInstance`
- `program.bootstrap.worldpipeline.subchunk.SubChunkInstance`
- `program.core.engine.BranchPackage`
- `program.core.util.mathematics.extras.Color`
- `program.core.util.mathematics.extras.Direction3Vector`

**Method intent:**
- `package boolean assembleQuads(ChunkInstance chunkInstance, SubChunkInstance subChunkInstance, BlockPaletteHandle biomePaletteHandle, BlockPaletteHandle blockPaletteHandle, BlockPaletteHandle rotationPaletteHandle, DynamicPacketInstance dynamicPacketInstance, int xyz, Direction3Vector direction3Vector, BiomeHandle biomeHandle, BlockHandle blockHandle, Int2ObjectOpenHashMap<FloatArrayList> verts, BitSet accumulatedBatch, BitSet batchReturn, Color[] vertColors)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/geometrypipeline/dynamicgeometrymanager/DynamicGeometryManager.java`

**Type:** `class DynamicGeometryManager`
  
**Inheritance/implements:** `extends ManagerPackage`
  
**Package:** `program.bootstrap.geometrypipeline.dynamicgeometrymanager`
  
**File size:** 86 lines

**What this class does:** `DynamicGeometryManager` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.geometrypipeline.dynamicgeometrymanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.dynamicgeometrymanager.util.DynamicGeometryAsyncContainer`
- `program.bootstrap.geometrypipeline.dynamicmodel.DynamicModelHandle`
- `program.bootstrap.menupipeline.fonts.GlyphMetricStruct`
- `program.bootstrap.worldpipeline.chunk.ChunkInstance`
- `program.bootstrap.worldpipeline.subchunk.SubChunkInstance`
- `program.core.engine.ManagerPackage`
- `program.core.settings.EngineSetting`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `public boolean build(DynamicGeometryAsyncContainer dynamicGeometryAsyncContainer, ChunkInstance chunkInstance)` — Constructs derived runtime/handle data from source input.
- `public boolean buildSubChunk(DynamicGeometryAsyncContainer dynamicGeometryAsyncContainer, ChunkInstance chunkInstance, int subChunkIndex)` — Constructs derived runtime/handle data from source input.
- `public void buildGlyphModel(DynamicModelHandle model, GlyphMetricStruct glyph, int atlasPixelSize)` — Constructs derived runtime/handle data from source input.
- `public DynamicGeometryAsyncContainer getDynamicGeometryAsyncInstance()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/geometrypipeline/dynamicgeometrymanager/DynamicGeometryType.java`

**Type:** `enum DynamicGeometryType`
  
**Package:** `program.bootstrap.geometrypipeline.dynamicgeometrymanager`
  
**File size:** 15 lines

**What this class does:** `DynamicGeometryType` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.geometrypipeline.dynamicgeometrymanager`.

**Method intent:** No concrete methods parsed (interface marker, enum, or data-only declaration).

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/geometrypipeline/dynamicgeometrymanager/FontGeometryBranch.java`

**Type:** `class FontGeometryBranch`
  
**Inheritance/implements:** `extends BranchPackage`
  
**Package:** `program.bootstrap.geometrypipeline.dynamicgeometrymanager`
  
**File size:** 67 lines

**What this class does:** `FontGeometryBranch` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.geometrypipeline.dynamicgeometrymanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.dynamicmodel.DynamicModelHandle`
- `program.bootstrap.menupipeline.fonts.GlyphMetricStruct`
- `program.core.engine.BranchPackage`
- `program.core.settings.EngineSetting`

**Method intent:**
- `package void buildGlyphModel(DynamicModelHandle model, GlyphMetricStruct glyph, int atlasPixelSize)` — Constructs derived runtime/handle data from source input.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/geometrypipeline/dynamicgeometrymanager/FullGeometryBranch.java`

**Type:** `class FullGeometryBranch`
  
**Inheritance/implements:** `extends BranchPackage`
  
**Package:** `program.bootstrap.geometrypipeline.dynamicgeometrymanager`
  
**File size:** 619 lines

**What this class does:** `FullGeometryBranch` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.geometrypipeline.dynamicgeometrymanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.dynamicgeometrymanager.util.VertBlockNeighbor3Vector`
- `program.bootstrap.geometrypipeline.dynamicpacket.DynamicPacketInstance`
- `program.bootstrap.shaderpipeline.texture.TextureHandle`
- `program.bootstrap.shaderpipeline.texturemanager.TextureManager`
- `program.bootstrap.worldpipeline.biome.BiomeHandle`
- `program.bootstrap.worldpipeline.biomemanager.BiomeManager`
- `program.bootstrap.worldpipeline.block.BlockHandle`
- `program.bootstrap.worldpipeline.block.BlockPaletteHandle`
- `program.bootstrap.worldpipeline.block.BlockRotationType`
- `program.bootstrap.worldpipeline.blockmanager.BlockManager`
- `program.bootstrap.worldpipeline.chunk.ChunkInstance`
- `program.bootstrap.worldpipeline.chunk.ChunkNeighborStruct`
- `program.bootstrap.worldpipeline.subchunk.SubChunkInstance`
- `program.bootstrap.worldpipeline.util.ChunkCoordinate3Int`
- `program.core.engine.BranchPackage`
- `program.core.settings.EngineSetting`
- `program.core.util.mathematics.extras.Color`
- `program.core.util.mathematics.extras.Coordinate3Int`
- `program.core.util.mathematics.extras.Direction2Vector`
- `program.core.util.mathematics.extras.Direction3Vector`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void get()` — Returns current state/value.
- `package boolean assembleQuads(ChunkInstance chunkInstance, SubChunkInstance subChunkInstance, BlockPaletteHandle biomePaletteHandle, BlockPaletteHandle blockPaletteHandle, BlockPaletteHandle rotationPaletteHandle, DynamicPacketInstance dynamicPacketInstance, int xyz, Direction3Vector direction3Vector, BiomeHandle biomeHandle, BlockHandle blockHandle, Int2ObjectOpenHashMap<FloatArrayList> verts, BitSet accumulatedBatch, BitSet batchReturn, Color[] vertColors)` — Performs class-specific logic; see call sites and owning manager flow.
- `private boolean blockHasFace(ChunkInstance chunkInstance, SubChunkInstance subChunkInstance, int xyz, Direction3Vector direction3Vector, BiomeHandle biomeHandle, BlockHandle blockHandle)` — Performs class-specific logic; see call sites and owning manager flow.
- `private SubChunkInstance getComparativeSubChunkInstance(ChunkInstance chunkInstance, SubChunkInstance subChunkInstance, int xyz, Direction3Vector direction3Vector)` — Returns current state/value.
- `private boolean compareNeighbor(BlockHandle blockHandleA, BlockHandle blockHandleB)` — Performs class-specific logic; see call sites and owning manager flow.
- `private boolean assembleQuad(ChunkInstance chunkInstance, SubChunkInstance subChunkInstance, BlockPaletteHandle biomePaletteHandle, BlockPaletteHandle blockPaletteHandle, BlockPaletteHandle rotationPaletteHandle, DynamicPacketInstance dynamicPacketInstance, int xyz, Direction3Vector direction3Vector, BiomeHandle biomeHandle, BlockHandle blockHandle, Int2ObjectOpenHashMap<FloatArrayList> verts, BitSet accumulatedBatch, BitSet batchReturn, Color[] vertColors)` — Performs class-specific logic; see call sites and owning manager flow.
- `private boolean tryExpand(ChunkInstance chunkInstance, SubChunkInstance subChunkInstance, BlockPaletteHandle biomePaletteHandle, BlockPaletteHandle blockPaletteHandle, BlockPaletteHandle rotationPaletteHandle, int xyz, Direction3Vector direction3Vector, Direction3Vector expandDirection, Direction3Vector tangentDirection, int currentSize, int tangentSize, BiomeHandle biomeHandle, BlockHandle blockHandle, short baseOrientation, BitSet accumulatedBatch, BitSet batchReturn)` — Performs class-specific logic; see call sites and owning manager flow.
- `private boolean compareNext(BiomeHandle biomeHandleA, BiomeHandle biomeHandleB, BlockHandle blockHandleA, BlockHandle blockHandleB, short orientationA, short orientationB)` — Performs class-specific logic; see call sites and owning manager flow.
- `private boolean prepareFace(ChunkInstance chunkInstance, SubChunkInstance subChunkInstance, BlockPaletteHandle biomePaletteHandle, BlockPaletteHandle blockPaletteHandle, BlockPaletteHandle rotationPaletteHandle, DynamicPacketInstance dynamicPacketInstance, int xyz, byte sizeA, byte sizeB, Direction3Vector direction3Vector, Direction3Vector tangentDirectionA, Direction3Vector tangentDirectionB, BiomeHandle biomeHandle, BlockHandle blockHandle, Int2ObjectOpenHashMap<FloatArrayList> verts, Color[] vertColors)` — Performs class-specific logic; see call sites and owning manager flow.
- `private int resolveOrientation(BlockPaletteHandle rotationPaletteHandle, int xyz)` — Performs class-specific logic; see call sites and owning manager flow.
- `private int resolveTextureID(BlockHandle blockHandle, Direction3Vector worldFace, int orientation)` — Performs class-specific logic; see call sites and owning manager flow.
- `private int resolveEncodedFace(BlockHandle blockHandle, Direction3Vector worldFace, int orientation)` — Performs class-specific logic; see call sites and owning manager flow.
- `private float getVertColor(ChunkInstance chunkInstance, SubChunkInstance subChunkInstance, int vertXYZ, Color[] vertColors)` — Returns current state/value.
- `private SubChunkInstance getComparativeSubChunkInstance(ChunkInstance chunkInstance, SubChunkInstance subChunkInstance, int xyz, VertBlockNeighbor3Vector vertBlockNeighbor3Vector)` — Returns current state/value.
- `private float blendColors(Color[] vertColors)` — Performs class-specific logic; see call sites and owning manager flow.
- `private boolean finalizeFace(Int2ObjectOpenHashMap<FloatArrayList> verts, DynamicPacketInstance dynamicPacketInstance, Direction3Vector direction3Vector, int materialId, TextureHandle textureHandle, int vert0XYZ, int vert1XYZ, int vert2XYZ, int vert3XYZ, float vert0Color, float vert1Color, float vert2Color, float vert3Color, int encodedFace)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/geometrypipeline/dynamicgeometrymanager/InternalBuildManager.java`

**Type:** `class InternalBuildManager`
  
**Inheritance/implements:** `extends ManagerPackage`
  
**Package:** `program.bootstrap.geometrypipeline.dynamicgeometrymanager`
  
**File size:** 237 lines

**What this class does:** `InternalBuildManager` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.geometrypipeline.dynamicgeometrymanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.dynamicgeometrymanager.util.DynamicGeometryAsyncContainer`
- `program.bootstrap.geometrypipeline.dynamicmodel.DynamicModelHandle`
- `program.bootstrap.geometrypipeline.dynamicpacket.DynamicPacketInstance`
- `program.bootstrap.menupipeline.fonts.GlyphMetricStruct`
- `program.bootstrap.worldpipeline.biome.BiomeHandle`
- `program.bootstrap.worldpipeline.biomemanager.BiomeManager`
- `program.bootstrap.worldpipeline.block.BlockHandle`
- `program.bootstrap.worldpipeline.block.BlockPaletteHandle`
- `program.bootstrap.worldpipeline.blockmanager.BlockManager`
- `program.bootstrap.worldpipeline.chunk.ChunkInstance`
- `program.bootstrap.worldpipeline.subchunk.SubChunkInstance`
- `program.bootstrap.worldpipeline.util.ChunkCoordinate3Int`
- `program.core.engine.ManagerPackage`
- `program.core.util.mathematics.extras.Color`
- `program.core.util.mathematics.extras.Direction3Vector`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void get()` — Returns current state/value.
- `package boolean build(DynamicGeometryAsyncContainer dynamicGeometryAsyncContainer, ChunkInstance chunkInstance, SubChunkInstance subChunkInstance)` — Constructs derived runtime/handle data from source input.
- `private boolean assembleQuads(DynamicGeometryType geometry, ChunkInstance chunkInstance, SubChunkInstance subChunkInstance, BlockPaletteHandle biomePaletteHandle, BlockPaletteHandle blockPaletteHandle, BlockPaletteHandle rotationPaletteHandle, DynamicPacketInstance dynamicPacketInstance, int xyz, Direction3Vector direction3Vector, BiomeHandle biomeHandle, BlockHandle blockHandle, Int2ObjectOpenHashMap<FloatArrayList> verts, BitSet accumulatedBatch, BitSet batchReturn, Color[] vertColors)` — Performs class-specific logic; see call sites and owning manager flow.
- `package void buildGlyphModel(DynamicModelHandle model, GlyphMetricStruct glyph, int atlasPixelSize)` — Constructs derived runtime/handle data from source input.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/geometrypipeline/dynamicgeometrymanager/LiquidGeometryBranch.java`

**Type:** `class LiquidGeometryBranch`
  
**Inheritance/implements:** `extends BranchPackage`
  
**Package:** `program.bootstrap.geometrypipeline.dynamicgeometrymanager`
  
**File size:** 42 lines

**What this class does:** `LiquidGeometryBranch` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.geometrypipeline.dynamicgeometrymanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.dynamicpacket.DynamicPacketInstance`
- `program.bootstrap.worldpipeline.biome.BiomeHandle`
- `program.bootstrap.worldpipeline.block.BlockHandle`
- `program.bootstrap.worldpipeline.block.BlockPaletteHandle`
- `program.bootstrap.worldpipeline.chunk.ChunkInstance`
- `program.bootstrap.worldpipeline.subchunk.SubChunkInstance`
- `program.core.engine.BranchPackage`
- `program.core.util.mathematics.extras.Color`
- `program.core.util.mathematics.extras.Direction3Vector`

**Method intent:**
- `package boolean assembleQuads(ChunkInstance chunkInstance, SubChunkInstance subChunkInstance, BlockPaletteHandle biomePaletteHandle, BlockPaletteHandle blockPaletteHandle, BlockPaletteHandle rotationPaletteHandle, DynamicPacketInstance dynamicPacketInstance, int xyz, Direction3Vector direction3Vector, BiomeHandle biomeHandle, BlockHandle blockHandle, Int2ObjectOpenHashMap<FloatArrayList> verts, BitSet accumulatedBatch, BitSet batchReturn, Color[] vertColors)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/geometrypipeline/dynamicgeometrymanager/PartialGeometryBranch.java`

**Type:** `class PartialGeometryBranch`
  
**Inheritance/implements:** `extends BranchPackage`
  
**Package:** `program.bootstrap.geometrypipeline.dynamicgeometrymanager`
  
**File size:** 42 lines

**What this class does:** `PartialGeometryBranch` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.geometrypipeline.dynamicgeometrymanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.dynamicpacket.DynamicPacketInstance`
- `program.bootstrap.worldpipeline.biome.BiomeHandle`
- `program.bootstrap.worldpipeline.block.BlockHandle`
- `program.bootstrap.worldpipeline.block.BlockPaletteHandle`
- `program.bootstrap.worldpipeline.chunk.ChunkInstance`
- `program.bootstrap.worldpipeline.subchunk.SubChunkInstance`
- `program.core.engine.BranchPackage`
- `program.core.util.mathematics.extras.Color`
- `program.core.util.mathematics.extras.Direction3Vector`

**Method intent:**
- `package boolean assembleQuads(ChunkInstance chunkInstance, SubChunkInstance subChunkInstance, BlockPaletteHandle biomePaletteHandle, BlockPaletteHandle blockPaletteHandle, BlockPaletteHandle rotationPaletteHandle, DynamicPacketInstance dynamicPacketInstance, int xyz, Direction3Vector direction3Vector, BiomeHandle biomeHandle, BlockHandle blockHandle, Int2ObjectOpenHashMap<FloatArrayList> verts, BitSet accumulatedBatch, BitSet batchReturn, Color[] vertColors)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/geometrypipeline/dynamicgeometrymanager/util/DynamicGeometryAsyncContainer.java`

**Type:** `class DynamicGeometryAsyncContainer`
  
**Inheritance/implements:** `extends AsyncContainerPackage`
  
**Package:** `program.bootstrap.geometrypipeline.dynamicgeometrymanager.util`
  
**File size:** 69 lines

**What this class does:** `DynamicGeometryAsyncContainer` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.geometrypipeline.dynamicgeometrymanager.util`.

**Who this class talks to (direct imports):**
- `program.core.engine.AsyncContainerPackage`
- `program.core.util.mathematics.extras.Color`
- `program.core.util.mathematics.extras.Direction3Vector`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `public void reset()` — Performs class-specific logic; see call sites and owning manager flow.
- `public Int2ObjectOpenHashMap<FloatArrayList> getVerts()` — Returns current state/value.
- `public BitSet[] getDirectionalBatches()` — Returns current state/value.
- `public BitSet getBatchReturn()` — Returns current state/value.
- `public Color[] getVertColors()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/geometrypipeline/dynamicgeometrymanager/util/VertBlockNeighbor3Vector.java`

**Type:** `enum VertBlockNeighbor3Vector`
  
**Package:** `program.bootstrap.geometrypipeline.dynamicgeometrymanager.util`
  
**File size:** 71 lines

**What this class does:** `VertBlockNeighbor3Vector` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.geometrypipeline.dynamicgeometrymanager.util`.

**Who this class talks to (direct imports):**
- `program.core.util.mathematics.extras.Coordinate2Long`
- `program.core.util.mathematics.extras.Coordinate3Int`
- `program.core.util.mathematics.extras.Coordinate3Long`
- `program.core.util.mathematics.extras.Direction2Vector`

**Method intent:**
- `package  VertBlockNeighbor3Vector(int x, int y, int z)` — Performs class-specific logic; see call sites and owning manager flow.
- `public Direction2Vector to2D()` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/geometrypipeline/dynamicmodel/DynamicModelHandle.java`

**Type:** `class DynamicModelHandle`
  
**Inheritance/implements:** `extends HandlePackage`
  
**Package:** `program.bootstrap.geometrypipeline.dynamicmodel`
  
**File size:** 167 lines

**What this class does:** `DynamicModelHandle` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.geometrypipeline.dynamicmodel`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.vao.VAOHandle`
- `program.core.engine.HandlePackage`
- `program.core.settings.EngineSetting`

**Method intent:**
- `public void constructor(int materialID, VAOHandle vaoHandle)` — Engine-side initialization entrypoint invoked post-create.
- `public int tryAddVertices(FloatArrayList sourceVerts, int offset, int length)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void addQuadVertices(FloatArrayList sourceVerts)` — Registers a child object into manager-owned collections.
- `public void mergeWithOffset(DynamicModelHandle source, int[] offsetIndices, float[] offsets)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void appendQuadIndices(int baseVertex, int quadCount)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void clear()` — Performs class-specific logic; see call sites and owning manager flow.
- `public int getMaterialID()` — Returns current state/value.
- `public VAOHandle getVAOHandle()` — Returns current state/value.
- `public FloatArrayList getVertices()` — Returns current state/value.
- `public ShortArrayList getIndices()` — Returns current state/value.
- `public boolean isEmpty()` — Performs class-specific logic; see call sites and owning manager flow.
- `public int getVertexCount()` — Returns current state/value.
- `public boolean isFull()` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/geometrypipeline/dynamicpacket/DynamicPacketInstance.java`

**Type:** `class DynamicPacketInstance`
  
**Inheritance/implements:** `extends InstancePackage`
  
**Package:** `program.bootstrap.geometrypipeline.dynamicpacket`
  
**File size:** 193 lines

**What this class does:** `DynamicPacketInstance` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.geometrypipeline.dynamicpacket`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.dynamicmodel.DynamicModelHandle`
- `program.bootstrap.geometrypipeline.vao.VAOHandle`
- `program.core.engine.InstancePackage`

**Method intent:**
- `public void constructor(VAOHandle vaoHandle)` — Engine-side initialization entrypoint invoked post-create.
- `public boolean tryLock()` — Performs class-specific logic; see call sites and owning manager flow.
- `public void setReady()` — Mutates internal state for this object.
- `public void unlock()` — Performs class-specific logic; see call sites and owning manager flow.
- `public boolean addVertices(int materialId, FloatArrayList vertList)` — Registers a child object into manager-owned collections.
- `public boolean merge(DynamicPacketInstance other, int[] offsetIndices, float[] offsets)` — Performs class-specific logic; see call sites and owning manager flow.
- `private FloatArrayList applyOffset(FloatArrayList vertices, int[] offsetIndices, float[] offsets)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void clear()` — Performs class-specific logic; see call sites and owning manager flow.
- `public DynamicPacketState getState()` — Returns current state/value.
- `public boolean hasModels()` — Boolean existence/availability check.
- `public Int2ObjectOpenHashMap<ObjectArrayList<DynamicModelHandle>> getMaterialID2ModelCollection()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/geometrypipeline/dynamicpacket/DynamicPacketState.java`

**Type:** `enum DynamicPacketState`
  
**Package:** `program.bootstrap.geometrypipeline.dynamicpacket`
  
**File size:** 14 lines

**What this class does:** `DynamicPacketState` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.geometrypipeline.dynamicpacket`.

**Method intent:** No concrete methods parsed (interface marker, enum, or data-only declaration).

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/geometrypipeline/ibo/IBOData.java`

**Type:** `class IBOData`
  
**Inheritance/implements:** `extends DataPackage`
  
**Package:** `program.bootstrap.geometrypipeline.ibo`
  
**File size:** 35 lines

**What this class does:** `IBOData` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.geometrypipeline.ibo`.

**Who this class talks to (direct imports):**
- `program.core.engine.DataPackage`

**Method intent:**
- `package public IBOData(int indexHandle, int indexCount)` — Performs class-specific logic; see call sites and owning manager flow.
- `public int getIndexHandle()` — Returns current state/value.
- `public int getIndexCount()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/geometrypipeline/ibo/IBOHandle.java`

**Type:** `class IBOHandle`
  
**Inheritance/implements:** `extends HandlePackage`
  
**Package:** `program.bootstrap.geometrypipeline.ibo`
  
**File size:** 29 lines

**What this class does:** `IBOHandle` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.geometrypipeline.ibo`.

**Who this class talks to (direct imports):**
- `program.core.engine.HandlePackage`

**Method intent:**
- `public void constructor(IBOData iboData)` — Engine-side initialization entrypoint invoked post-create.
- `public IBOData getIBOData()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/geometrypipeline/ibo/IBOInstance.java`

**Type:** `class IBOInstance`
  
**Inheritance/implements:** `extends InstancePackage`
  
**Package:** `program.bootstrap.geometrypipeline.ibo`
  
**File size:** 29 lines

**What this class does:** `IBOInstance` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.geometrypipeline.ibo`.

**Who this class talks to (direct imports):**
- `program.core.engine.InstancePackage`

**Method intent:**
- `public void constructor(IBOData iboData)` — Engine-side initialization entrypoint invoked post-create.
- `public IBOData getIBOData()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/geometrypipeline/ibomanager/GLSLUtility.java`

**Type:** `class GLSLUtility`
  
**Package:** `program.bootstrap.geometrypipeline.ibomanager`
  
**File size:** 74 lines

**What this class does:** `GLSLUtility` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.geometrypipeline.ibomanager`.

**Who this class talks to (direct imports):**
- `program.core.app.CoreContext`
- `program.core.util.graphics.gl.GL20`
- `program.core.util.graphics.gl.GL30`
- `program.bootstrap.geometrypipeline.ibo.IBOData`
- `program.bootstrap.geometrypipeline.ibo.IBOHandle`
- `program.bootstrap.geometrypipeline.ibo.IBOInstance`
- `program.bootstrap.geometrypipeline.vao.VAOInstance`

**Method intent:**
- `package IBOHandle uploadIndexData(VAOInstance vaoInstance, IBOHandle iboHandle, short[] indices)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/geometrypipeline/ibomanager/IBOManager.java`, `core/src/program/bootstrap/geometrypipeline/ibomanager/InternalBuilder.java`.
- `package IBOInstance uploadIndexData(VAOInstance vaoInstance, IBOInstance iboInstance, short[] indices)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/geometrypipeline/ibomanager/IBOManager.java`, `core/src/program/bootstrap/geometrypipeline/ibomanager/InternalBuilder.java`.
- `private IBOData upload(VAOInstance vaoInstance, short[] indices)` — Performs class-specific logic; see call sites and owning manager flow.
- `package void removeIndexData(IBOData iboData)` — Unregisters and tears down child references. Called via static reference from: `core/src/program/bootstrap/geometrypipeline/ibomanager/IBOManager.java`.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/geometrypipeline/ibomanager/IBOManager.java`

**Type:** `class IBOManager`
  
**Inheritance/implements:** `extends ManagerPackage`
  
**Package:** `program.bootstrap.geometrypipeline.ibomanager`
  
**File size:** 127 lines

**What this class does:** `IBOManager` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.geometrypipeline.ibomanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.ibo.IBOData`
- `program.bootstrap.geometrypipeline.ibo.IBOHandle`
- `program.bootstrap.geometrypipeline.ibo.IBOInstance`
- `program.bootstrap.geometrypipeline.meshmanager.MeshManager`
- `program.bootstrap.geometrypipeline.vao.VAOInstance`
- `program.core.engine.ManagerPackage`
- `program.core.util.RegistryUtility`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void get()` — Returns current state/value.
- `package void registerIBO(String resourceName, IBOHandle handle)` — Performs class-specific logic; see call sites and owning manager flow.
- `public IBOHandle addIBOFromData(String resourceName, short[] indices, VAOInstance vaoInstance)` — Registers a child object into manager-owned collections.
- `public boolean hasIBO(String iboName)` — Boolean existence/availability check.
- `public short getIBOIDFromIBOName(String iboName)` — Returns current state/value.
- `public IBOHandle getIBOHandleFromIBOID(short iboID)` — Returns current state/value.
- `public IBOHandle getIBOHandleFromIBOName(String iboName)` — Returns current state/value.
- `public IBOHandle getIBOHandleDirect(String iboName)` — Returns current state/value.
- `public IBOInstance createIBOInstance(VAOInstance vaoInstance, ShortArrayList indices)` — Allocates/initializes child systems or resources.
- `public void removeIBO(IBOData iboData)` — Unregisters and tears down child references.
- `public void removeIBO(IBOHandle iboHandle)` — Unregisters and tears down child references.
- `public void removeIBOInstance(IBOInstance iboInstance)` — Unregisters and tears down child references. Called via static reference from: `core/src/program/bootstrap/geometrypipeline/ibo/IBOInstance.java`.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/geometrypipeline/ibomanager/InternalBuilder.java`

**Type:** `class InternalBuilder`
  
**Inheritance/implements:** `extends BuilderPackage`
  
**Package:** `program.bootstrap.geometrypipeline.ibomanager`
  
**File size:** 149 lines

**What this class does:** `InternalBuilder` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.geometrypipeline.ibomanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.ibo.IBOHandle`
- `program.bootstrap.geometrypipeline.vao.VAOInstance`
- `program.core.engine.BuilderPackage`
- `program.core.util.JsonUtility`

**Method intent:**
- `protected void get()` — Returns current state/value.
- `public void build(String resourceName, File file, Map<String, File> registry, VAOInstance vaoInstance)` — Constructs derived runtime/handle data from source input.
- `private void resolveRef(String refName, String sourceResourceName, File sourceFile, Map<String, File> registry, VAOInstance vaoInstance)` — Performs class-specific logic; see call sites and owning manager flow.
- `private IBOHandle buildFromData(JsonArray indicesArray, VAOInstance vaoInstance, File file)` — Constructs derived runtime/handle data from source input.
- `private boolean hasQuadEntries(JsonObject json)` — Boolean existence/availability check.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/geometrypipeline/mesh/MeshData.java`

**Type:** `class MeshData`
  
**Inheritance/implements:** `extends DataPackage`
  
**Package:** `program.bootstrap.geometrypipeline.mesh`
  
**File size:** 68 lines

**What this class does:** `MeshData` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.geometrypipeline.mesh`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.ibo.IBOData`
- `program.bootstrap.geometrypipeline.vao.VAOData`
- `program.bootstrap.geometrypipeline.vbo.VBOData`
- `program.core.engine.DataPackage`

**Method intent:**
- `package public MeshData(VAOData vaoData, VBOData vboData, IBOData iboData)` — Performs class-specific logic; see call sites and owning manager flow.
- `public VAOData getVAOData()` — Returns current state/value.
- `public VBOData getVBOData()` — Returns current state/value.
- `public IBOData getIBOData()` — Returns current state/value.
- `public int getAttributeHandle()` — Returns current state/value.
- `public int getVertStride()` — Returns current state/value.
- `public int getVertexHandle()` — Returns current state/value.
- `public int getVertexCount()` — Returns current state/value.
- `public int getIndexHandle()` — Returns current state/value.
- `public int getIndexCount()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/geometrypipeline/mesh/MeshHandle.java`

**Type:** `class MeshHandle`
  
**Inheritance/implements:** `extends HandlePackage`
  
**Package:** `program.bootstrap.geometrypipeline.mesh`
  
**File size:** 76 lines

**What this class does:** `MeshHandle` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.geometrypipeline.mesh`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.ibo.IBOHandle`
- `program.bootstrap.geometrypipeline.vao.VAOInstance`
- `program.bootstrap.geometrypipeline.vbo.VBOHandle`
- `program.core.engine.HandlePackage`

**Method intent:**
- `public void constructor(VAOInstance vaoInstance, VBOHandle vboHandle, IBOHandle iboHandle)` — Engine-side initialization entrypoint invoked post-create.
- `public VAOInstance getVAOInstance()` — Returns current state/value.
- `public VBOHandle getVBOHandle()` — Returns current state/value.
- `public IBOHandle getIBOHandle()` — Returns current state/value.
- `public MeshData getMeshData()` — Returns current state/value.
- `public int getAttributeHandle()` — Returns current state/value.
- `public int[] getAttrSizes()` — Returns current state/value.
- `public int getVertexHandle()` — Returns current state/value.
- `public int getIndexHandle()` — Returns current state/value.
- `public int getIndexCount()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/geometrypipeline/mesh/MeshInstance.java`

**Type:** `class MeshInstance`
  
**Inheritance/implements:** `extends InstancePackage`
  
**Package:** `program.bootstrap.geometrypipeline.mesh`
  
**File size:** 75 lines

**What this class does:** `MeshInstance` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.geometrypipeline.mesh`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.ibo.IBOInstance`
- `program.bootstrap.geometrypipeline.vao.VAOInstance`
- `program.bootstrap.geometrypipeline.vbo.VBOInstance`
- `program.core.engine.InstancePackage`

**Method intent:**
- `public void constructor(VAOInstance vaoInstance, VBOInstance vboInstance, IBOInstance iboInstance)` — Engine-side initialization entrypoint invoked post-create.
- `public VAOInstance getVAOInstance()` — Returns current state/value.
- `public VBOInstance getVBOInstance()` — Returns current state/value.
- `public IBOInstance getIBOInstance()` — Returns current state/value.
- `public MeshData getMeshData()` — Returns current state/value.
- `public int getAttributeHandle()` — Returns current state/value.
- `public int[] getAttrSizes()` — Returns current state/value.
- `public int getVertexHandle()` — Returns current state/value.
- `public int getIndexHandle()` — Returns current state/value.
- `public int getIndexCount()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/geometrypipeline/meshmanager/InternalBuilder.java`

**Type:** `class InternalBuilder`
  
**Inheritance/implements:** `extends BuilderPackage`
  
**Package:** `program.bootstrap.geometrypipeline.meshmanager`
  
**File size:** 291 lines

**What this class does:** `InternalBuilder` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.geometrypipeline.meshmanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.ibo.IBOHandle`
- `program.bootstrap.geometrypipeline.ibomanager.IBOManager`
- `program.bootstrap.geometrypipeline.mesh.MeshHandle`
- `program.bootstrap.geometrypipeline.vao.VAOInstance`
- `program.bootstrap.geometrypipeline.vbo.VBOHandle`
- `program.bootstrap.geometrypipeline.vbomanager.VBOManager`
- `program.bootstrap.shaderpipeline.texture.TextureHandle`
- `program.bootstrap.shaderpipeline.texturemanager.TextureManager`
- `program.core.engine.BuilderPackage`
- `program.core.util.FileUtility`
- `program.core.util.JsonUtility`

**Method intent:**
- `protected void get()` — Returns current state/value.
- `package MeshHandle buildMeshHandle(File root, File file, VAOInstance vaoInstance)` — Constructs derived runtime/handle data from source input.
- `private boolean hasQuadEntries(JsonObject json)` — Boolean existence/availability check.
- `private QuadExpansionStruct expandVBO(JsonObject json, VAOInstance vaoInstance, File file)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void expandQuad(JsonObject quadObj, FloatArrayList vertices, ShortArrayList quadIndices, int baseVertex, int vertStride, VAOInstance vaoInstance, File file)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void validateVAOUVCompatibility(VAOInstance vaoInstance, File file)` — Performs class-specific logic; see call sites and owning manager flow.
- `private float[][] resolveLocalUVs(JsonObject quadObj, File file)` — Performs class-specific logic; see call sites and owning manager flow.
- `private float snapUV(float local, float tileMin, float tileMax, int tilePixelSize)` — Performs class-specific logic; see call sites and owning manager flow.
- `private boolean hasValidElement(JsonObject json, String key)` — Boolean existence/availability check.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/geometrypipeline/meshmanager/InternalLoader.java`

**Type:** `class InternalLoader`
  
**Inheritance/implements:** `extends LoaderPackage`
  
**Package:** `program.bootstrap.geometrypipeline.meshmanager`
  
**File size:** 125 lines

**What this class does:** `InternalLoader` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.geometrypipeline.meshmanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.mesh.MeshHandle`
- `program.bootstrap.geometrypipeline.vao.VAOHandle`
- `program.bootstrap.geometrypipeline.vao.VAOInstance`
- `program.bootstrap.geometrypipeline.vaomanager.VAOManager`
- `program.core.engine.LoaderPackage`
- `program.core.settings.EngineSetting`
- `program.core.util.FileUtility`

**Method intent:**
- `protected void scan()` — Discovers files/resources for later load.
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void get()` — Returns current state/value.
- `protected void load(File file)` — Parses external data into engine objects.
- `package void request(String resourceName)` — Triggers on-demand loading or lookup.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/geometrypipeline/meshmanager/MeshManager.java`

**Type:** `class MeshManager`
  
**Inheritance/implements:** `extends ManagerPackage`
  
**Package:** `program.bootstrap.geometrypipeline.meshmanager`
  
**File size:** 128 lines

**What this class does:** `MeshManager` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.geometrypipeline.meshmanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.ibo.IBOInstance`
- `program.bootstrap.geometrypipeline.ibomanager.IBOManager`
- `program.bootstrap.geometrypipeline.mesh.MeshData`
- `program.bootstrap.geometrypipeline.mesh.MeshHandle`
- `program.bootstrap.geometrypipeline.mesh.MeshInstance`
- `program.bootstrap.geometrypipeline.vao.VAOHandle`
- `program.bootstrap.geometrypipeline.vao.VAOInstance`
- `program.bootstrap.geometrypipeline.vaomanager.VAOManager`
- `program.bootstrap.geometrypipeline.vbo.VBOInstance`
- `program.bootstrap.geometrypipeline.vbomanager.VBOManager`
- `program.core.engine.ManagerPackage`
- `program.core.util.RegistryUtility`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void get()` — Returns current state/value.
- `package void addMeshHandle(String meshName, MeshHandle meshHandle)` — Registers a child object into manager-owned collections.
- `public void request(String resourceName)` — Triggers on-demand loading or lookup.
- `public boolean hasMesh(String meshName)` — Boolean existence/availability check.
- `public int getMeshIDFromMeshName(String meshName)` — Returns current state/value.
- `public MeshHandle getMeshHandleFromMeshID(int meshID)` — Returns current state/value.
- `public MeshHandle getMeshHandleFromMeshName(String meshName)` — Returns current state/value.
- `public MeshInstance createMesh(VAOHandle vaoTemplate, FloatArrayList vertices, ShortArrayList indices)` — Allocates/initializes child systems or resources.
- `public void removeMesh(MeshData meshData)` — Unregisters and tears down child references. Called via static reference from: `core/src/program/bootstrap/geometrypipeline/mesh/MeshInstance.java`.
- `public void removeMesh(MeshHandle meshHandle)` — Unregisters and tears down child references. Called via static reference from: `core/src/program/bootstrap/geometrypipeline/mesh/MeshInstance.java`.
- `public void removeMesh(MeshInstance meshInstance)` — Unregisters and tears down child references. Called via static reference from: `core/src/program/bootstrap/geometrypipeline/mesh/MeshInstance.java`.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/geometrypipeline/meshmanager/QuadExpansionStruct.java`

**Type:** `class QuadExpansionStruct`
  
**Inheritance/implements:** `extends StructPackage`
  
**Package:** `program.bootstrap.geometrypipeline.meshmanager`
  
**File size:** 23 lines

**What this class does:** `QuadExpansionStruct` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.geometrypipeline.meshmanager`.

**Who this class talks to (direct imports):**
- `program.core.engine.StructPackage`

**Method intent:**
- `package  QuadExpansionStruct(float[] vertices, short[] indices)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/geometrypipeline/model/ModelInstance.java`

**Type:** `class ModelInstance`
  
**Inheritance/implements:** `extends InstancePackage`
  
**Package:** `program.bootstrap.geometrypipeline.model`
  
**File size:** 61 lines

**What this class does:** `ModelInstance` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.geometrypipeline.model`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.mesh.MeshData`
- `program.bootstrap.shaderpipeline.material.MaterialInstance`
- `program.core.engine.InstancePackage`

**Method intent:**
- `public void constructor(MeshData meshData, MaterialInstance material)` — Engine-side initialization entrypoint invoked post-create.
- `public MeshData getMeshData()` — Returns current state/value.
- `public MaterialInstance getMaterial()` — Returns current state/value.
- `public int getVAO()` — Returns current state/value.
- `public int getVertStride()` — Returns current state/value.
- `public int getVBO()` — Returns current state/value.
- `public int getVertCount()` — Returns current state/value.
- `public int getIBO()` — Returns current state/value.
- `public int getIndexCount()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/geometrypipeline/modelmanager/ModelManager.java`

**Type:** `class ModelManager`
  
**Inheritance/implements:** `extends ManagerPackage`
  
**Package:** `program.bootstrap.geometrypipeline.modelmanager`
  
**File size:** 111 lines

**What this class does:** `ModelManager` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.geometrypipeline.modelmanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.mesh.MeshData`
- `program.bootstrap.geometrypipeline.mesh.MeshHandle`
- `program.bootstrap.geometrypipeline.mesh.MeshInstance`
- `program.bootstrap.geometrypipeline.meshmanager.MeshManager`
- `program.bootstrap.geometrypipeline.model.ModelInstance`
- `program.bootstrap.geometrypipeline.vao.VAOHandle`
- `program.bootstrap.shaderpipeline.material.MaterialInstance`
- `program.bootstrap.shaderpipeline.materialmanager.MaterialManager`
- `program.core.engine.ManagerPackage`

**Method intent:**
- `protected void get()` — Returns current state/value.
- `public ModelInstance createModel(MeshData meshData, int materialID)` — Allocates/initializes child systems or resources.
- `public ModelInstance createModel(MeshData meshData, MaterialInstance material)` — Allocates/initializes child systems or resources.
- `public ModelInstance createModel(MeshHandle meshHandle, int materialID)` — Allocates/initializes child systems or resources.
- `public ModelInstance createModel(MeshHandle meshHandle, MaterialInstance material)` — Allocates/initializes child systems or resources.
- `public ModelInstance createModel(MeshInstance meshInstance, int materialID)` — Allocates/initializes child systems or resources.
- `public ModelInstance createModel(MeshInstance meshInstance, MaterialInstance material)` — Allocates/initializes child systems or resources.
- `public ModelInstance createModel(ModelInstance modelInstance, int materialID)` — Allocates/initializes child systems or resources.
- `public ModelInstance createModel(ModelInstance modelInstance, MaterialInstance material)` — Allocates/initializes child systems or resources.
- `public ModelInstance createModel(VAOHandle vaoTemplate, FloatArrayList vertices, ShortArrayList indices, int materialID)` — Allocates/initializes child systems or resources.
- `public ModelInstance createModel(VAOHandle vaoTemplate, FloatArrayList vertices, ShortArrayList indices, MaterialInstance material)` — Allocates/initializes child systems or resources.
- `private ModelInstance buildModel(MeshData meshData, MaterialInstance material)` — Constructs derived runtime/handle data from source input.
- `public void removeMesh(MeshInstance meshInstance)` — Unregisters and tears down child references.
- `public void removeMesh(ModelInstance modelInstance)` — Unregisters and tears down child references.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/geometrypipeline/vao/VAOData.java`

**Type:** `class VAOData`
  
**Inheritance/implements:** `extends DataPackage`
  
**Package:** `program.bootstrap.geometrypipeline.vao`
  
**File size:** 46 lines

**What this class does:** `VAOData` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.geometrypipeline.vao`.

**Who this class talks to (direct imports):**
- `program.core.engine.DataPackage`

**Method intent:**
- `package public VAOData(int attributeHandle, int[] attrSizes)` — Performs class-specific logic; see call sites and owning manager flow.
- `public int getAttributeHandle()` — Returns current state/value.
- `public int getVertStride()` — Returns current state/value.
- `public int[] getAttrSizes()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/geometrypipeline/vao/VAOHandle.java`

**Type:** `class VAOHandle`
  
**Inheritance/implements:** `extends HandlePackage`
  
**Package:** `program.bootstrap.geometrypipeline.vao`
  
**File size:** 29 lines

**What this class does:** `VAOHandle` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.geometrypipeline.vao`.

**Who this class talks to (direct imports):**
- `program.core.engine.HandlePackage`

**Method intent:**
- `public void constructor(int[] attrSizes)` — Engine-side initialization entrypoint invoked post-create.
- `public VAOData getVAOData()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/geometrypipeline/vao/VAOInstance.java`

**Type:** `class VAOInstance`
  
**Inheritance/implements:** `extends InstancePackage`
  
**Package:** `program.bootstrap.geometrypipeline.vao`
  
**File size:** 29 lines

**What this class does:** `VAOInstance` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.geometrypipeline.vao`.

**Who this class talks to (direct imports):**
- `program.core.engine.InstancePackage`

**Method intent:**
- `public void constructor(VAOData vaoData)` — Engine-side initialization entrypoint invoked post-create.
- `public VAOData getVAOData()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/geometrypipeline/vaomanager/GLSLUtility.java`

**Type:** `class GLSLUtility`
  
**Package:** `program.bootstrap.geometrypipeline.vaomanager`
  
**File size:** 132 lines

**What this class does:** `GLSLUtility` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.geometrypipeline.vaomanager`.

**Who this class talks to (direct imports):**
- `program.core.app.CoreContext`
- `program.core.util.graphics.gl.GL20`
- `program.core.util.graphics.gl.GL30`
- `program.bootstrap.geometrypipeline.vao.VAOData`
- `program.bootstrap.geometrypipeline.vao.VAOHandle`
- `program.bootstrap.geometrypipeline.vao.VAOInstance`

**Method intent:**
- `package VAOInstance createVAOInstance(VAOInstance vaoInstance, VAOHandle template)` — Allocates/initializes child systems or resources. Called via static reference from: `core/src/program/bootstrap/geometrypipeline/vaomanager/VAOManager.java`.
- `package int cloneVAO(int[] attrSizes, int vertexHandle, int indexHandle)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/geometrypipeline/vaomanager/VAOManager.java`.
- `private VAOData createData(int[] attrSizes)` — Allocates/initializes child systems or resources.
- `package void removeVAOData(VAOData vaoData)` — Unregisters and tears down child references. Called via static reference from: `core/src/program/bootstrap/geometrypipeline/vaomanager/VAOManager.java`.
- `package void removeVAOInstance(VAOInstance vaoInstance)` — Unregisters and tears down child references. Called via static reference from: `core/src/program/bootstrap/geometrypipeline/vaomanager/VAOManager.java`.
- `package void removeVAOHandle(int vao)` — Unregisters and tears down child references. Called via static reference from: `core/src/program/bootstrap/geometrypipeline/vaomanager/VAOManager.java`.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/geometrypipeline/vaomanager/InternalBuilder.java`

**Type:** `class InternalBuilder`
  
**Inheritance/implements:** `extends BuilderPackage`
  
**Package:** `program.bootstrap.geometrypipeline.vaomanager`
  
**File size:** 112 lines

**What this class does:** `InternalBuilder` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.geometrypipeline.vaomanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.vao.VAOHandle`
- `program.core.engine.BuilderPackage`
- `program.core.util.JsonUtility`

**Method intent:**
- `protected void get()` — Returns current state/value.
- `public void build(String resourceName, File file, Map<String, File> registry)` — Constructs derived runtime/handle data from source input.
- `private void resolveRef(String refName, File sourceFile, Map<String, File> registry)` — Performs class-specific logic; see call sites and owning manager flow.
- `private VAOHandle buildLayout(JsonArray jsonArray, File file)` — Constructs derived runtime/handle data from source input.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/geometrypipeline/vaomanager/VAOManager.java`

**Type:** `class VAOManager`
  
**Inheritance/implements:** `extends ManagerPackage`
  
**Package:** `program.bootstrap.geometrypipeline.vaomanager`
  
**File size:** 180 lines

**What this class does:** `VAOManager` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.geometrypipeline.vaomanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.meshmanager.MeshManager`
- `program.bootstrap.geometrypipeline.mesh.MeshData`
- `program.bootstrap.geometrypipeline.vao.VAOData`
- `program.bootstrap.geometrypipeline.vao.VAOHandle`
- `program.bootstrap.geometrypipeline.vao.VAOInstance`
- `program.core.engine.ManagerPackage`
- `program.core.util.RegistryUtility`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void get()` — Returns current state/value.
- `package void registerVAO(String resourceName, VAOHandle handle)` — Performs class-specific logic; see call sites and owning manager flow.
- `public boolean hasVAO(String vaoName)` — Boolean existence/availability check.
- `public short getVAOIDFromVAOName(String vaoName)` — Returns current state/value.
- `public VAOHandle getVAOHandleFromVAOID(short vaoID)` — Returns current state/value.
- `public VAOHandle getVAOHandleFromVAOName(String vaoName)` — Returns current state/value.
- `public VAOHandle getVAOHandleDirect(String vaoName)` — Returns current state/value.
- `public VAOInstance createVAOInstance(VAOHandle template)` — Allocates/initializes child systems or resources.
- `public int getVAOForWindow(MeshData meshData, int windowID)` — Returns current state/value.
- `public void removeWindowVAOs(int windowID)` — Unregisters and tears down child references.
- `public void removeSourceVAOClones(int sourceVAO)` — Unregisters and tears down child references.
- `public void removeVAOData(VAOData vaoData)` — Unregisters and tears down child references.
- `public void removeVAOInstance(VAOInstance vaoInstance)` — Unregisters and tears down child references. Called via static reference from: `core/src/program/bootstrap/geometrypipeline/vao/VAOInstance.java`.
- `private long composeWindowKey(int sourceVAO, int windowID)` — Performs class-specific logic; see call sites and owning manager flow.
- `private int extractWindowID(long key)` — Performs class-specific logic; see call sites and owning manager flow.
- `private int extractSourceVAO(long key)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/geometrypipeline/vbo/VBOData.java`

**Type:** `class VBOData`
  
**Inheritance/implements:** `extends DataPackage`
  
**Package:** `program.bootstrap.geometrypipeline.vbo`
  
**File size:** 35 lines

**What this class does:** `VBOData` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.geometrypipeline.vbo`.

**Who this class talks to (direct imports):**
- `program.core.engine.DataPackage`

**Method intent:**
- `package public VBOData(int vertexHandle, int vertexCount)` — Performs class-specific logic; see call sites and owning manager flow.
- `public int getVertexHandle()` — Returns current state/value.
- `public int getVertexCount()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/geometrypipeline/vbo/VBOHandle.java`

**Type:** `class VBOHandle`
  
**Inheritance/implements:** `extends HandlePackage`
  
**Package:** `program.bootstrap.geometrypipeline.vbo`
  
**File size:** 29 lines

**What this class does:** `VBOHandle` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.geometrypipeline.vbo`.

**Who this class talks to (direct imports):**
- `program.core.engine.HandlePackage`

**Method intent:**
- `public void constructor(VBOData vboData)` — Engine-side initialization entrypoint invoked post-create.
- `public VBOData getVBOData()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/geometrypipeline/vbo/VBOInstance.java`

**Type:** `class VBOInstance`
  
**Inheritance/implements:** `extends InstancePackage`
  
**Package:** `program.bootstrap.geometrypipeline.vbo`
  
**File size:** 29 lines

**What this class does:** `VBOInstance` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.geometrypipeline.vbo`.

**Who this class talks to (direct imports):**
- `program.core.engine.InstancePackage`

**Method intent:**
- `public void constructor(VBOData vboData)` — Engine-side initialization entrypoint invoked post-create.
- `public VBOData getVBOData()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/geometrypipeline/vbomanager/GLSLUtility.java`

**Type:** `class GLSLUtility`
  
**Package:** `program.bootstrap.geometrypipeline.vbomanager`
  
**File size:** 88 lines

**What this class does:** `GLSLUtility` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.geometrypipeline.vbomanager`.

**Who this class talks to (direct imports):**
- `program.core.app.CoreContext`
- `program.core.util.graphics.gl.GL20`
- `program.core.util.graphics.gl.GL30`
- `program.bootstrap.geometrypipeline.vao.VAOData`
- `program.bootstrap.geometrypipeline.vao.VAOInstance`
- `program.bootstrap.geometrypipeline.vbo.VBOData`
- `program.bootstrap.geometrypipeline.vbo.VBOHandle`
- `program.bootstrap.geometrypipeline.vbo.VBOInstance`

**Method intent:**
- `package VBOHandle uploadVertexData(VAOInstance vaoInstance, VBOHandle vboHandle, float[] vertices)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/geometrypipeline/vbomanager/InternalBuilder.java`, `core/src/program/bootstrap/geometrypipeline/vbomanager/VBOManager.java`.
- `package VBOInstance uploadVertexData(VAOInstance vaoInstance, VBOInstance vboInstance, float[] vertices)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/geometrypipeline/vbomanager/InternalBuilder.java`, `core/src/program/bootstrap/geometrypipeline/vbomanager/VBOManager.java`.
- `private VBOData upload(VAOInstance vaoInstance, float[] vertices)` — Performs class-specific logic; see call sites and owning manager flow.
- `package void removeVertexData(VBOData vboData)` — Unregisters and tears down child references. Called via static reference from: `core/src/program/bootstrap/geometrypipeline/vbomanager/VBOManager.java`.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/geometrypipeline/vbomanager/InternalBuilder.java`

**Type:** `class InternalBuilder`
  
**Inheritance/implements:** `extends BuilderPackage`
  
**Package:** `program.bootstrap.geometrypipeline.vbomanager`
  
**File size:** 140 lines

**What this class does:** `InternalBuilder` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.geometrypipeline.vbomanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.vao.VAOInstance`
- `program.bootstrap.geometrypipeline.vbo.VBOHandle`
- `program.core.engine.BuilderPackage`
- `program.core.util.JsonUtility`

**Method intent:**
- `protected void get()` — Returns current state/value.
- `public void build(String resourceName, File file, Map<String, File> registry, VAOInstance vaoInstance)` — Constructs derived runtime/handle data from source input.
- `private void resolveRef(String refName, String sourceResourceName, File sourceFile, Map<String, File> registry, VAOInstance vaoInstance)` — Performs class-specific logic; see call sites and owning manager flow.
- `private VBOHandle buildFromData(JsonArray verticesArray, VAOInstance vaoInstance, File file)` — Constructs derived runtime/handle data from source input.
- `private boolean containsQuadObjects(JsonArray array)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/geometrypipeline/vbomanager/VBOManager.java`

**Type:** `class VBOManager`
  
**Inheritance/implements:** `extends ManagerPackage`
  
**Package:** `program.bootstrap.geometrypipeline.vbomanager`
  
**File size:** 127 lines

**What this class does:** `VBOManager` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.geometrypipeline.vbomanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.meshmanager.MeshManager`
- `program.bootstrap.geometrypipeline.vao.VAOInstance`
- `program.bootstrap.geometrypipeline.vbo.VBOData`
- `program.bootstrap.geometrypipeline.vbo.VBOHandle`
- `program.bootstrap.geometrypipeline.vbo.VBOInstance`
- `program.core.engine.ManagerPackage`
- `program.core.util.RegistryUtility`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void get()` — Returns current state/value.
- `package void registerVBO(String resourceName, VBOHandle handle)` — Performs class-specific logic; see call sites and owning manager flow.
- `public VBOHandle addVBOFromData(String resourceName, float[] vertices, VAOInstance vaoInstance)` — Registers a child object into manager-owned collections.
- `public boolean hasVBO(String vboName)` — Boolean existence/availability check.
- `public short getVBOIDFromVBOName(String vboName)` — Returns current state/value.
- `public VBOHandle getVBOHandleFromVBOID(short vboID)` — Returns current state/value.
- `public VBOHandle getVBOHandleFromVBOName(String vboName)` — Returns current state/value.
- `public VBOHandle getVBOHandleDirect(String vboName)` — Returns current state/value.
- `public VBOInstance createVBOInstance(VAOInstance vaoInstance, FloatArrayList vertices)` — Allocates/initializes child systems or resources.
- `public void removeVBO(VBOData vboData)` — Unregisters and tears down child references.
- `public void removeVBO(VBOHandle vboHandle)` — Unregisters and tears down child references.
- `public void removeVBOInstance(VBOInstance vboInstance)` — Unregisters and tears down child references. Called via static reference from: `core/src/program/bootstrap/geometrypipeline/vbo/VBOInstance.java`.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.
