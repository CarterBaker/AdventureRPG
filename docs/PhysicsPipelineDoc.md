# PhysicsPipelineDoc

This is a human-readable subsystem manual. It explains responsibilities, collaboration points, and method intent for each class in scope.

Classes covered: **11**

## How to read this manual
- Start with the package flow notes at the top of each class section.
- Use **Who talks to this class** to identify collaborators.
- Use **Method intent** to understand lifecycle and API behavior.

## `core/src/program/bootstrap/physicspipeline/PhysicsPipeline.java`

**Type:** `class PhysicsPipeline`
  
**Inheritance/implements:** `extends PipelinePackage`
  
**Package:** `program.bootstrap.physicspipeline`
  
**File size:** 18 lines

**What this class does:** `PhysicsPipeline` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.physicspipeline`.

**Who this class talks to (direct imports):**
- `program.bootstrap.physicspipeline.movementmanager.MovementManager`
- `program.bootstrap.physicspipeline.raycastmanager.RaycastManager`
- `program.core.engine.PipelinePackage`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/physicspipeline/movementmanager/BlockCollisionBranch.java`

**Type:** `class BlockCollisionBranch`
  
**Inheritance/implements:** `extends BranchPackage`
  
**Package:** `program.bootstrap.physicspipeline.movementmanager`
  
**File size:** 144 lines

**What this class does:** `BlockCollisionBranch` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.physicspipeline.movementmanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.entitypipeline.entity.EntityInstance`
- `program.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryType`
- `program.bootstrap.physicspipeline.util.BlockCompositionStruct`
- `program.bootstrap.worldpipeline.block.BlockHandle`
- `program.bootstrap.worldpipeline.blockmanager.BlockManager`
- `program.bootstrap.worldpipeline.chunk.ChunkInstance`
- `program.bootstrap.worldpipeline.subchunk.SubChunkInstance`
- `program.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager`
- `program.core.engine.BranchPackage`
- `program.core.settings.EngineSetting`
- `program.core.util.mathematics.extras.Coordinate3Int`
- `program.core.util.mathematics.extras.Direction3Vector`
- `program.core.util.mathematics.vectors.Vector3`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void get()` — Returns current state/value.
- `package void calculate(Vector3 position, Vector3 movement, EntityInstance entity)` — Performs class-specific logic; see call sites and owning manager flow.
- `private boolean hasCollisionInDirection(BlockCompositionStruct blockCompositionStruct, Direction3Vector direction, float axisPosition, float axisSize, float axisMovement)` — Boolean existence/availability check.
- `private boolean isColliding(BlockHandle block, Direction3Vector direction, float axisPosition, float axisSize, float axisMovement)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/physicspipeline/movementmanager/GravityBranch.java`

**Type:** `class GravityBranch`
  
**Inheritance/implements:** `extends BranchPackage`
  
**Package:** `program.bootstrap.physicspipeline.movementmanager`
  
**File size:** 138 lines

**What this class does:** `GravityBranch` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.physicspipeline.movementmanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.entitypipeline.behavior.BehaviorHandle`
- `program.bootstrap.entitypipeline.entity.EntityInstance`
- `program.bootstrap.entitypipeline.entity.EntityState`
- `program.bootstrap.entitypipeline.entity.EntityStateHandle`
- `program.bootstrap.entitypipeline.statistics.StatisticsHandle`
- `program.bootstrap.inputpipeline.input.InputHandle`
- `program.bootstrap.worldpipeline.world.WorldHandle`
- `program.core.engine.BranchPackage`
- `program.core.settings.EngineSetting`
- `program.core.util.mathematics.vectors.Vector3`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `package void calculate(Vector3 movement, EntityInstance entity)` — Performs class-specific logic; see call sites and owning manager flow.
- `package void postCollision(Vector3 pre, Vector3 post, EntityInstance entity)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/physicspipeline/movementmanager/MovementBranch.java`

**Type:** `class MovementBranch`
  
**Inheritance/implements:** `extends BranchPackage`
  
**Package:** `program.bootstrap.physicspipeline.movementmanager`
  
**File size:** 106 lines

**What this class does:** `MovementBranch` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.physicspipeline.movementmanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.entitypipeline.entity.EntityInstance`
- `program.bootstrap.entitypipeline.entity.EntityState`
- `program.bootstrap.entitypipeline.entity.EntityStateHandle`
- `program.bootstrap.entitypipeline.statistics.StatisticsHandle`
- `program.bootstrap.inputpipeline.input.InputHandle`
- `program.core.engine.BranchPackage`
- `program.core.settings.EngineSetting`
- `program.core.util.mathematics.vectors.Vector2`
- `program.core.util.mathematics.vectors.Vector3`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `package void calculate(Vector3 movement, EntityInstance entity)` — Performs class-specific logic; see call sites and owning manager flow.
- `private float selectSpeed(EntityState state, StatisticsHandle stats)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/physicspipeline/movementmanager/MovementManager.java`

**Type:** `class MovementManager`
  
**Inheritance/implements:** `extends ManagerPackage`
  
**Package:** `program.bootstrap.physicspipeline.movementmanager`
  
**File size:** 123 lines

**What this class does:** `MovementManager` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.physicspipeline.movementmanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.entitypipeline.entity.EntityInstance`
- `program.bootstrap.worldpipeline.util.WorldPositionStruct`
- `program.bootstrap.worldpipeline.util.WorldWrapUtility`
- `program.core.engine.ManagerPackage`
- `program.core.settings.EngineSetting`
- `program.core.util.mathematics.extras.Coordinate2Long`
- `program.core.util.mathematics.vectors.Vector3`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `public void move(EntityInstance entity)` — Performs class-specific logic; see call sites and owning manager flow.
- `private long updateChunkCoordinateFrom(Vector3 position, int chunkCoordinateX, int chunkCoordinateY)` — Runs frame-step maintenance and logic.
- `private int calculateChunkCoordinateAxisFrom(float axis)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/physicspipeline/raycastmanager/BlockCastBranch.java`

**Type:** `class BlockCastBranch`
  
**Inheritance/implements:** `extends BranchPackage`
  
**Package:** `program.bootstrap.physicspipeline.raycastmanager`
  
**File size:** 179 lines

**What this class does:** `BlockCastBranch` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.physicspipeline.raycastmanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryType`
- `program.bootstrap.physicspipeline.util.BlockCastStruct`
- `program.bootstrap.worldpipeline.block.BlockHandle`
- `program.bootstrap.worldpipeline.blockmanager.BlockManager`
- `program.bootstrap.worldpipeline.chunk.ChunkInstance`
- `program.bootstrap.worldpipeline.subchunk.SubChunkInstance`
- `program.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager`
- `program.core.engine.BranchPackage`
- `program.core.settings.EngineSetting`
- `program.core.util.mathematics.extras.Coordinate2Long`
- `program.core.util.mathematics.extras.Coordinate3Int`
- `program.core.util.mathematics.extras.Direction3Vector`
- `program.core.util.mathematics.vectors.Vector3`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void get()` — Returns current state/value.
- `package void cast(long chunkCoordinate, Vector3 rayOrigin, Vector3 direction, float maxDistance, BlockCastStruct out)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/physicspipeline/raycastmanager/RaycastManager.java`

**Type:** `class RaycastManager`
  
**Inheritance/implements:** `extends ManagerPackage`
  
**Package:** `program.bootstrap.physicspipeline.raycastmanager`
  
**File size:** 62 lines

**What this class does:** `RaycastManager` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.physicspipeline.raycastmanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.physicspipeline.util.BlockCastStruct`
- `program.bootstrap.physicspipeline.util.ScreenRayStruct`
- `program.core.engine.ManagerPackage`
- `program.core.util.mathematics.vectors.Vector3`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void update()` — Runs frame-step maintenance and logic.
- `public void castBlock(long chunkCoordinate, Vector3 rayOrigin, Vector3 direction, float maxDistance, BlockCastStruct out)` — Performs class-specific logic; see call sites and owning manager flow.
- `public ScreenRayStruct getScreenRay()` — Returns current state/value.
- `public boolean hasScreenRay()` — Boolean existence/availability check.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/physicspipeline/raycastmanager/ScreenCastBranch.java`

**Type:** `class ScreenCastBranch`
  
**Inheritance/implements:** `extends BranchPackage`
  
**Package:** `program.bootstrap.physicspipeline.raycastmanager`
  
**File size:** 52 lines

**What this class does:** `ScreenCastBranch` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.physicspipeline.raycastmanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.inputpipeline.inputsystem.InputSystem`
- `program.bootstrap.physicspipeline.util.ScreenRayStruct`
- `program.core.engine.BranchPackage`
- `program.core.kernel.windowmanager.WindowManager`

**Method intent:**
- `protected void get()` — Returns current state/value.
- `package boolean cast(ScreenRayStruct out)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/physicspipeline/util/BlockCastStruct.java`

**Type:** `class BlockCastStruct`
  
**Inheritance/implements:** `extends StructPackage`
  
**Package:** `program.bootstrap.physicspipeline.util`
  
**File size:** 125 lines

**What this class does:** `BlockCastStruct` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.physicspipeline.util`.

**Who this class talks to (direct imports):**
- `program.bootstrap.worldpipeline.block.BlockHandle`
- `program.core.engine.StructPackage`
- `program.core.util.mathematics.extras.Direction3Vector`

**Method intent:**
- `public boolean isHit()` — Performs class-specific logic; see call sites and owning manager flow.
- `public void setHit(boolean hit)` — Mutates internal state for this object.
- `public float getDistance()` — Returns current state/value.
- `public void setDistance(float distance)` — Mutates internal state for this object.
- `public Direction3Vector getHitFace()` — Returns current state/value.
- `public void setHitFace(Direction3Vector hitFace)` — Mutates internal state for this object.
- `public long getChunkCoordinate()` — Returns current state/value.
- `public void setChunkCoordinate(long chunkCoordinate)` — Mutates internal state for this object.
- `public int getSubChunkY()` — Returns current state/value.
- `public void setSubChunkY(int subChunkY)` — Mutates internal state for this object.
- `public BlockHandle getBlock()` — Returns current state/value.
- `public void setBlock(BlockHandle block)` — Mutates internal state for this object.
- `public int getBlockX()` — Returns current state/value.
- `public void setBlockX(int blockX)` — Mutates internal state for this object.
- `public int getBlockY()` — Returns current state/value.
- `public void setBlockY(int blockY)` — Mutates internal state for this object.
- `public int getBlockZ()` — Returns current state/value.
- `public void setBlockZ(int blockZ)` — Mutates internal state for this object.
- `public int getHitSubX()` — Returns current state/value.
- `public void setHitSubX(int hitSubX)` — Mutates internal state for this object.
- `public int getHitSubY()` — Returns current state/value.
- `public void setHitSubY(int hitSubY)` — Mutates internal state for this object.
- `public int getHitSubZ()` — Returns current state/value.
- `public void setHitSubZ(int hitSubZ)` — Mutates internal state for this object.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/physicspipeline/util/BlockCompositionStruct.java`

**Type:** `class BlockCompositionStruct`
  
**Inheritance/implements:** `extends StructPackage`
  
**Package:** `program.bootstrap.physicspipeline.util`
  
**File size:** 171 lines

**What this class does:** `BlockCompositionStruct` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.physicspipeline.util`.

**Who this class talks to (direct imports):**
- `program.core.engine.StructPackage`
- `program.core.settings.EngineSetting`
- `program.core.util.mathematics.extras.Coordinate2Long`
- `program.core.util.mathematics.extras.Coordinate3Int`
- `program.core.util.mathematics.extras.Direction3Vector`
- `program.core.util.mathematics.vectors.Vector3`
- `program.core.util.mathematics.vectors.Vector3Int`

**Method intent:**
- `package public BlockCompositionStruct()` — Performs class-specific logic; see call sites and owning manager flow.
- `public void updateBlockComposition(Vector3Int blockComposition, Vector3 currentPosition, long chunkCoordinate)` — Runs frame-step maintenance and logic.
- `private void buildBlockComposition(Vector3Int blockComposition, long chunkCoordinate)` — Constructs derived runtime/handle data from source input.
- `private void buildAdjacentBlocks(Vector3Int blockComposition, long chunkCoordinate)` — Constructs derived runtime/handle data from source input.
- `private void addBlockToMap(int blockX, int blockY, int blockZ, long chunkCoordinate, Int2LongOpenHashMap map)` — Registers a child object into manager-owned collections.
- `public Int2LongOpenHashMap getBlockCompositionMap()` — Returns current state/value.
- `public Int2LongOpenHashMap getAllBlocksForSide(Direction3Vector direction)` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/physicspipeline/util/ScreenRayStruct.java`

**Type:** `class ScreenRayStruct`
  
**Inheritance/implements:** `extends StructPackage`
  
**Package:** `program.bootstrap.physicspipeline.util`
  
**File size:** 69 lines

**What this class does:** `ScreenRayStruct` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.physicspipeline.util`.

**Who this class talks to (direct imports):**
- `program.core.engine.StructPackage`

**Method intent:**
- `package public ScreenRayStruct()` — Performs class-specific logic; see call sites and owning manager flow.
- `public void init(int windowID, float screenX, float screenY, float screenW, float screenH)` — Performs class-specific logic; see call sites and owning manager flow.
- `public int getWindowID()` — Returns current state/value.
- `public float getScreenX()` — Returns current state/value.
- `public float getScreenY()` — Returns current state/value.
- `public float getScreenW()` — Returns current state/value.
- `public float getScreenH()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.
