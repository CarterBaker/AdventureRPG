# EntityPipelineDoc

This is a human-readable subsystem manual. It explains responsibilities, collaboration points, and method intent for each class in scope.

Classes covered: **21**

## How to read this manual
- Start with the package flow notes at the top of each class section.
- Use **Who talks to this class** to identify collaborators.
- Use **Method intent** to understand lifecycle and API behavior.

## `core/src/program/bootstrap/entitypipeline/EntityPipeline.java`

**Type:** `class EntityPipeline`
  
**Inheritance/implements:** `extends PipelinePackage`
  
**Package:** `program.bootstrap.entitypipeline`
  
**File size:** 22 lines

**What this class does:** `EntityPipeline` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.entitypipeline`.

**Who this class talks to (direct imports):**
- `program.bootstrap.entitypipeline.behaviormanager.BehaviorManager`
- `program.bootstrap.entitypipeline.entitymanager.EntityManager`
- `program.bootstrap.entitypipeline.playermanager.PlayerManager`
- `program.core.engine.PipelinePackage`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/entitypipeline/behavior/BehaviorData.java`

**Type:** `class BehaviorData`
  
**Inheritance/implements:** `extends DataPackage`
  
**Package:** `program.bootstrap.entitypipeline.behavior`
  
**File size:** 48 lines

**What this class does:** `BehaviorData` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.entitypipeline.behavior`.

**Who this class talks to (direct imports):**
- `program.core.engine.DataPackage`

**Method intent:**
- `package public BehaviorData(String behaviorName, short behaviorID, float jumpDuration)` — Performs class-specific logic; see call sites and owning manager flow.
- `public String getBehaviorName()` — Returns current state/value.
- `public short getBehaviorID()` — Returns current state/value.
- `public float getJumpDuration()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/entitypipeline/behavior/BehaviorHandle.java`

**Type:** `class BehaviorHandle`
  
**Inheritance/implements:** `extends HandlePackage`
  
**Package:** `program.bootstrap.entitypipeline.behavior`
  
**File size:** 40 lines

**What this class does:** `BehaviorHandle` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.entitypipeline.behavior`.

**Who this class talks to (direct imports):**
- `program.core.engine.HandlePackage`

**Method intent:**
- `public void constructor(BehaviorData behaviorData)` — Engine-side initialization entrypoint invoked post-create.
- `public BehaviorData getBehaviorData()` — Returns current state/value.
- `public String getBehaviorName()` — Returns current state/value.
- `public short getBehaviorID()` — Returns current state/value.
- `public float getJumpDuration()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/entitypipeline/behaviormanager/BehaviorManager.java`

**Type:** `class BehaviorManager`
  
**Inheritance/implements:** `extends ManagerPackage`
  
**Package:** `program.bootstrap.entitypipeline.behaviormanager`
  
**File size:** 72 lines

**What this class does:** `BehaviorManager` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.entitypipeline.behaviormanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.entitypipeline.behavior.BehaviorHandle`
- `program.core.engine.ManagerPackage`
- `program.core.util.RegistryUtility`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `package void addBehavior(BehaviorHandle handle)` — Registers a child object into manager-owned collections.
- `public boolean hasBehavior(String behaviorName)` — Boolean existence/availability check.
- `public short getBehaviorIDFromBehaviorName(String behaviorName)` — Returns current state/value.
- `public BehaviorHandle getBehaviorHandleFromBehaviorID(short behaviorID)` — Returns current state/value.
- `public BehaviorHandle getBehaviorHandleFromBehaviorName(String behaviorName)` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/entitypipeline/behaviormanager/InternalBuilder.java`

**Type:** `class InternalBuilder`
  
**Inheritance/implements:** `extends BuilderPackage`
  
**Package:** `program.bootstrap.entitypipeline.behaviormanager`
  
**File size:** 37 lines

**What this class does:** `InternalBuilder` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.entitypipeline.behaviormanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.entitypipeline.behavior.BehaviorData`
- `program.bootstrap.entitypipeline.behavior.BehaviorHandle`
- `program.core.engine.BuilderPackage`
- `program.core.settings.EngineSetting`
- `program.core.util.JsonUtility`
- `program.core.util.RegistryUtility`

**Method intent:**
- `package BehaviorHandle build(File file, String behaviorName)` — Constructs derived runtime/handle data from source input.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/entitypipeline/behaviormanager/InternalLoader.java`

**Type:** `class InternalLoader`
  
**Inheritance/implements:** `extends LoaderPackage`
  
**Package:** `program.bootstrap.entitypipeline.behaviormanager`
  
**File size:** 90 lines

**What this class does:** `InternalLoader` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.entitypipeline.behaviormanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.entitypipeline.behavior.BehaviorHandle`
- `program.core.engine.LoaderPackage`
- `program.core.settings.EngineSetting`
- `program.core.util.FileUtility`

**Method intent:**
- `protected void scan()` — Discovers files/resources for later load.
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void get()` — Returns current state/value.
- `protected void load(File file)` — Parses external data into engine objects.
- `package void request(String behaviorName)` — Triggers on-demand loading or lookup.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/entitypipeline/entity/EntityData.java`

**Type:** `class EntityData`
  
**Inheritance/implements:** `extends DataPackage`
  
**Package:** `program.bootstrap.entitypipeline.entity`
  
**File size:** 89 lines

**What this class does:** `EntityData` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.entitypipeline.entity`.

**Who this class talks to (direct imports):**
- `program.core.engine.DataPackage`
- `program.core.util.mathematics.vectors.Vector3`

**Method intent:**
- `package public EntityData(Vector3 sizeMin, Vector3 sizeMax, float weightMin, float weightMax, float eyeLevel, String behaviorName)` — Performs class-specific logic; see call sites and owning manager flow.
- `public Vector3 getSizeMin()` — Returns current state/value.
- `public Vector3 getSizeMax()` — Returns current state/value.
- `public float getWeightMin()` — Returns current state/value.
- `public float getWeightMax()` — Returns current state/value.
- `public float getEyeLevel()` — Returns current state/value.
- `public String getBehaviorName()` — Returns current state/value.
- `public Vector3 getRandomSize()` — Returns current state/value.
- `public float getRandomWeight()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/entitypipeline/entity/EntityHandle.java`

**Type:** `class EntityHandle`
  
**Inheritance/implements:** `extends HandlePackage`
  
**Package:** `program.bootstrap.entitypipeline.entity`
  
**File size:** 54 lines

**What this class does:** `EntityHandle` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.entitypipeline.entity`.

**Who this class talks to (direct imports):**
- `program.core.engine.HandlePackage`
- `program.core.util.mathematics.vectors.Vector3`

**Method intent:**
- `public void constructor(EntityData entityData)` — Engine-side initialization entrypoint invoked post-create.
- `public EntityData getEntityData()` — Returns current state/value.
- `public float getWeightMin()` — Returns current state/value.
- `public float getWeightMax()` — Returns current state/value.
- `public float getEyeLevel()` — Returns current state/value.
- `public String getBehaviorName()` — Returns current state/value.
- `public Vector3 getRandomSize()` — Returns current state/value.
- `public float getRandomWeight()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/entitypipeline/entity/EntityInstance.java`

**Type:** `class EntityInstance`
  
**Inheritance/implements:** `extends InstancePackage`
  
**Package:** `program.bootstrap.entitypipeline.entity`
  
**File size:** 168 lines

**What this class does:** `EntityInstance` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.entitypipeline.entity`.

**Who this class talks to (direct imports):**
- `program.bootstrap.entitypipeline.behavior.BehaviorHandle`
- `program.bootstrap.entitypipeline.inventory.InventoryHandle`
- `program.bootstrap.entitypipeline.statistics.StatisticsHandle`
- `program.bootstrap.inputpipeline.input.InputHandle`
- `program.bootstrap.physicspipeline.util.BlockCompositionStruct`
- `program.bootstrap.worldpipeline.util.WorldPositionStruct`
- `program.bootstrap.worldpipeline.world.WorldHandle`
- `program.core.engine.InstancePackage`
- `program.core.util.mathematics.vectors.Vector3`
- `program.core.util.mathematics.vectors.Vector3Int`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `public void constructor(EntityData entityData, WorldHandle worldHandle, BehaviorHandle behaviorHandle, Vector3 position, long chunkCoordinate, Vector3 size, float weight)` — Engine-side initialization entrypoint invoked post-create.
- `private void setEntitySize(Vector3 size)` — Mutates internal state for this object.
- `public void updateBlockComposition()` — Runs frame-step maintenance and logic.
- `public EntityData getEntityData()` — Returns current state/value.
- `public WorldHandle getWorldHandle()` — Returns current state/value.
- `public BehaviorHandle getBehaviorHandle()` — Returns current state/value.
- `public void setBehaviorHandle(BehaviorHandle behaviorHandle)` — Mutates internal state for this object.
- `public EntityStateHandle getEntityStateHandle()` — Returns current state/value.
- `public StatisticsHandle getStatisticsHandle()` — Returns current state/value.
- `public InventoryHandle getInventoryHandle()` — Returns current state/value.
- `public InputHandle getInputHandle()` — Returns current state/value.
- `public WorldPositionStruct getWorldPositionStruct()` — Returns current state/value.
- `public Vector3Int getBlockComposition()` — Returns current state/value.
- `public BlockCompositionStruct getBlockCompositionStruct()` — Returns current state/value.
- `public Vector3 getSize()` — Returns current state/value.
- `public void setSize(Vector3 size)` — Mutates internal state for this object.
- `public float getWeight()` — Returns current state/value.
- `public float getEyeHeight()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/entitypipeline/entity/EntityState.java`

**Type:** `enum EntityState`
  
**Package:** `program.bootstrap.entitypipeline.entity`
  
**File size:** 16 lines

**What this class does:** `EntityState` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.entitypipeline.entity`.

**Method intent:** No concrete methods parsed (interface marker, enum, or data-only declaration).

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/entitypipeline/entity/EntityStateHandle.java`

**Type:** `class EntityStateHandle`
  
**Inheritance/implements:** `extends HandlePackage`
  
**Package:** `program.bootstrap.entitypipeline.entity`
  
**File size:** 69 lines

**What this class does:** `EntityStateHandle` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.entitypipeline.entity`.

**Who this class talks to (direct imports):**
- `program.core.engine.HandlePackage`
- `program.core.util.mathematics.vectors.Vector2`
- `program.core.util.mathematics.vectors.Vector3`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `public EntityState getMovementState()` — Returns current state/value.
- `public void setMovementState(EntityState movementState)` — Mutates internal state for this object.
- `public Vector3 getGravityVelocity()` — Returns current state/value.
- `public Vector2 getHorizontalVelocity()` — Returns current state/value.
- `public long getJumpStartTime()` — Returns current state/value.
- `public void setJumpStartTime(long jumpStartTime)` — Mutates internal state for this object.
- `public boolean isGrounded()` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/entitypipeline/entitymanager/EntityManager.java`

**Type:** `class EntityManager`
  
**Inheritance/implements:** `extends ManagerPackage`
  
**Package:** `program.bootstrap.entitypipeline.entitymanager`
  
**File size:** 114 lines

**What this class does:** `EntityManager` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.entitypipeline.entitymanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.entitypipeline.behavior.BehaviorHandle`
- `program.bootstrap.entitypipeline.behaviormanager.BehaviorManager`
- `program.bootstrap.entitypipeline.entity.EntityData`
- `program.bootstrap.entitypipeline.entity.EntityHandle`
- `program.bootstrap.entitypipeline.entity.EntityInstance`
- `program.bootstrap.worldpipeline.util.WorldPositionUtility`
- `program.bootstrap.worldpipeline.world.WorldHandle`
- `program.bootstrap.worldpipeline.worldmanager.WorldManager`
- `program.core.engine.ManagerPackage`
- `program.core.util.RegistryUtility`
- `program.core.util.mathematics.vectors.Vector3`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void get()` — Returns current state/value.
- `package void addEntityTemplate(String templateName, EntityHandle entityHandle)` — Registers a child object into manager-owned collections.
- `public boolean hasTemplate(String templateName)` — Boolean existence/availability check.
- `public int getTemplateIDFromTemplateName(String templateName)` — Returns current state/value.
- `public EntityHandle getEntityHandleFromTemplateID(int templateID)` — Returns current state/value.
- `public EntityHandle getEntityHandleFromTemplateName(String templateName)` — Returns current state/value.
- `public EntityInstance spawnEntity(EntityHandle entityHandle)` — Creates runtime entity/camera/player state for active context. Called via static reference from: `core/src/program/bootstrap/entitypipeline/entity/EntityInstance.java`.
- `public EntityInstance spawnEntity(String templateName)` — Creates runtime entity/camera/player state for active context. Called via static reference from: `core/src/program/bootstrap/entitypipeline/entity/EntityInstance.java`.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/entitypipeline/entitymanager/InternalBuilder.java`

**Type:** `class InternalBuilder`
  
**Inheritance/implements:** `extends BuilderPackage`
  
**Package:** `program.bootstrap.entitypipeline.entitymanager`
  
**File size:** 101 lines

**What this class does:** `InternalBuilder` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.entitypipeline.entitymanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.entitypipeline.entity.EntityData`
- `program.bootstrap.entitypipeline.entity.EntityHandle`
- `program.core.engine.BuilderPackage`
- `program.core.settings.EngineSetting`
- `program.core.util.JsonUtility`
- `program.core.util.mathematics.vectors.Vector3`

**Method intent:**
- `package EntityHandle build(File file)` — Constructs derived runtime/handle data from source input.
- `private Vector3 parseSizeMin(JsonObject json)` — Performs class-specific logic; see call sites and owning manager flow.
- `private Vector3 parseSizeMax(JsonObject json)` — Performs class-specific logic; see call sites and owning manager flow.
- `private float parseWeightMin(JsonObject json)` — Performs class-specific logic; see call sites and owning manager flow.
- `private float parseWeightMax(JsonObject json)` — Performs class-specific logic; see call sites and owning manager flow.
- `private float parseEyeLevel(JsonObject json)` — Performs class-specific logic; see call sites and owning manager flow.
- `private String parseBehaviorName(JsonObject json, File file)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/entitypipeline/entitymanager/InternalLoader.java`

**Type:** `class InternalLoader`
  
**Inheritance/implements:** `extends LoaderPackage`
  
**Package:** `program.bootstrap.entitypipeline.entitymanager`
  
**File size:** 90 lines

**What this class does:** `InternalLoader` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.entitypipeline.entitymanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.entitypipeline.entity.EntityHandle`
- `program.core.engine.LoaderPackage`
- `program.core.settings.EngineSetting`
- `program.core.util.FileUtility`

**Method intent:**
- `protected void scan()` — Discovers files/resources for later load.
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void get()` — Returns current state/value.
- `protected void load(File file)` — Parses external data into engine objects.
- `package void request(String templateName)` — Triggers on-demand loading or lookup.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/entitypipeline/inventory/InventoryHandle.java`

**Type:** `class InventoryHandle`
  
**Inheritance/implements:** `extends HandlePackage`
  
**Package:** `program.bootstrap.entitypipeline.inventory`
  
**File size:** 59 lines

**What this class does:** `InventoryHandle` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.entitypipeline.inventory`.

**Who this class talks to (direct imports):**
- `program.bootstrap.itempipeline.backpack.BackpackInstance`
- `program.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle`
- `program.core.engine.HandlePackage`

**Method intent:**
- `public ItemDefinitionHandle getMainHand()` — Returns current state/value.
- `public void setMainHand(ItemDefinitionHandle mainHand)` — Mutates internal state for this object.
- `public boolean hasMainHand()` — Boolean existence/availability check.
- `public ItemDefinitionHandle getOffHand()` — Returns current state/value.
- `public void setOffHand(ItemDefinitionHandle offHand)` — Mutates internal state for this object.
- `public boolean hasOffHand()` — Boolean existence/availability check.
- `public BackpackInstance getBackpack()` — Returns current state/value.
- `public void setBackpack(BackpackInstance backpack)` — Mutates internal state for this object.
- `public boolean hasBackpack()` — Boolean existence/availability check.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/entitypipeline/placementmanager/BlockBranch.java`

**Type:** `class BlockBranch`
  
**Inheritance/implements:** `extends BranchPackage`
  
**Package:** `program.bootstrap.entitypipeline.placementmanager`
  
**File size:** 234 lines

**What this class does:** `BlockBranch` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.entitypipeline.placementmanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.entitypipeline.entity.EntityInstance`
- `program.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryManager`
- `program.bootstrap.geometrypipeline.dynamicgeometrymanager.util.DynamicGeometryAsyncContainer`
- `program.bootstrap.physicspipeline.util.BlockCastStruct`
- `program.bootstrap.worldpipeline.block.BlockHandle`
- `program.bootstrap.worldpipeline.blockmanager.BlockManager`
- `program.bootstrap.worldpipeline.chunk.ChunkInstance`
- `program.bootstrap.worldpipeline.subchunk.SubChunkInstance`
- `program.bootstrap.worldpipeline.worldrendermanager.WorldRenderManager`
- `program.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager`
- `program.core.engine.BranchPackage`
- `program.core.settings.EngineSetting`
- `program.core.util.mathematics.extras.Coordinate2Long`
- `program.core.util.mathematics.extras.Coordinate3Int`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void get()` — Returns current state/value.
- `protected void awake()` — Runs startup-time runtime activation work.
- `package boolean tryBreak(EntityInstance entity, BlockCastStruct castStruct)` — Performs class-specific logic; see call sites and owning manager flow.
- `package void resetBreakTarget()` — Performs class-specific logic; see call sites and owning manager flow.
- `private int getBreakTier(EntityInstance entity)` — Returns current state/value.
- `private boolean isCorrectTool(EntityInstance entity, BlockHandle block)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void rebuildAffected(ChunkInstance chunk, long chunkCoordinate, int blockX, int blockY, int blockZ, int subChunkY)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void rebuildSubChunk(ChunkInstance chunk, int subChunkY)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void rebuildNeighbour(int chunkX, int chunkZ, int subChunkY)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void mergeAndRender(ChunkInstance chunk, long chunkCoordinate)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void writeBlock(ChunkInstance chunk, int blockX, int blockY, int blockZ, int subChunkY, short blockID)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/entitypipeline/placementmanager/ItemBranch.java`

**Type:** `class ItemBranch`
  
**Inheritance/implements:** `extends BranchPackage`
  
**Package:** `program.bootstrap.entitypipeline.placementmanager`
  
**File size:** 143 lines

**What this class does:** `ItemBranch` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.entitypipeline.placementmanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.entitypipeline.entity.EntityInstance`
- `program.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle`
- `program.bootstrap.physicspipeline.util.BlockCastStruct`
- `program.bootstrap.worldpipeline.chunk.ChunkInstance`
- `program.bootstrap.worldpipeline.worlditemplacementsystem.WorldItemPlacementSystem`
- `program.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager`
- `program.core.engine.BranchPackage`
- `program.core.settings.EngineSetting`
- `program.core.util.mathematics.extras.Coordinate2Long`
- `program.core.util.mathematics.extras.Coordinate4Long`
- `program.core.util.mathematics.extras.Direction3Vector`
- `program.core.util.mathematics.vectors.Vector3`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void get()` — Returns current state/value.
- `package boolean place(EntityInstance entity, Vector3 direction, BlockCastStruct castStruct)` — Performs class-specific logic; see call sites and owning manager flow.
- `private int resolveItemOrientation(Direction3Vector hitFace, Vector3 cameraDirection)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/entitypipeline/placementmanager/PlacementManager.java`

**Type:** `class PlacementManager`
  
**Inheritance/implements:** `extends ManagerPackage`
  
**Package:** `program.bootstrap.entitypipeline.placementmanager`
  
**File size:** 116 lines

**What this class does:** `PlacementManager` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.entitypipeline.placementmanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.entitypipeline.entity.EntityInstance`
- `program.bootstrap.physicspipeline.raycastmanager.RaycastManager`
- `program.bootstrap.physicspipeline.util.BlockCastStruct`
- `program.bootstrap.worldpipeline.util.WorldPositionStruct`
- `program.core.engine.ManagerPackage`
- `program.core.settings.EngineSetting`
- `program.core.util.mathematics.vectors.Vector3`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void get()` — Returns current state/value.
- `public void update(EntityInstance entity, Vector3 origin, Vector3 direction, boolean breakAction, boolean placeAction)` — Runs frame-step maintenance and logic.
- `private boolean handleBreakAction(EntityInstance entity, BlockCastStruct castStruct)` — Performs class-specific logic; see call sites and owning manager flow.
- `private boolean handlePlaceAction(EntityInstance entity, Vector3 direction, BlockCastStruct castStruct)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/entitypipeline/playermanager/InternalBufferSystem.java`

**Type:** `class InternalBufferSystem`
  
**Inheritance/implements:** `extends SystemPackage`
  
**Package:** `program.bootstrap.entitypipeline.playermanager`
  
**File size:** 50 lines

**What this class does:** `InternalBufferSystem` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.entitypipeline.playermanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.shaderpipeline.ubo.UBOHandle`
- `program.bootstrap.shaderpipeline.ubomanager.UBOManager`
- `program.bootstrap.worldpipeline.util.WorldPositionStruct`
- `program.core.engine.SystemPackage`
- `program.core.settings.EngineSetting`
- `program.core.util.mathematics.extras.Coordinate2Long`

**Method intent:**
- `protected void get()` — Returns current state/value.
- `protected void awake()` — Runs startup-time runtime activation work.
- `package void updatePlayerPosition(WorldPositionStruct playerPosition)` — Runs frame-step maintenance and logic.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/entitypipeline/playermanager/PlayerManager.java`

**Type:** `class PlayerManager`
  
**Inheritance/implements:** `extends ManagerPackage`
  
**Package:** `program.bootstrap.entitypipeline.playermanager`
  
**File size:** 220 lines

**What this class does:** `PlayerManager` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.entitypipeline.playermanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.entitypipeline.entity.EntityInstance`
- `program.bootstrap.entitypipeline.entity.EntityState`
- `program.bootstrap.entitypipeline.entity.EntityStateHandle`
- `program.bootstrap.entitypipeline.entitymanager.EntityManager`
- `program.bootstrap.entitypipeline.placementmanager.PlacementManager`
- `program.bootstrap.inputpipeline.input.InputHandle`
- `program.bootstrap.inputpipeline.inputsystem.InputSystem`
- `program.bootstrap.physicspipeline.movementmanager.MovementManager`
- `program.bootstrap.worldpipeline.blockmanager.BlockManager`
- `program.bootstrap.worldpipeline.chunk.ChunkData`
- `program.bootstrap.worldpipeline.chunk.ChunkInstance`
- `program.bootstrap.worldpipeline.util.WorldPositionStruct`
- `program.bootstrap.worldpipeline.util.WorldPositionUtility`
- `program.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager`
- `program.core.engine.ManagerPackage`
- `program.core.kernel.window.WindowInstance`
- `program.core.settings.EngineSetting`
- `program.core.util.camera.CameraInstance`
- `program.core.util.mathematics.vectors.Vector3`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void get()` — Returns current state/value.
- `protected void update()` — Runs frame-step maintenance and logic.
- `public EntityInstance spawnPlayer(WindowInstance window)` — Creates runtime entity/camera/player state for active context.
- `private void calculatePlayerPosition(int windowID, EntityInstance player, CameraInstance camera)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void writeMovementState(EntityInstance player)` — Performs class-specific logic; see call sites and owning manager flow.
- `private boolean verifyPlayerPosition(EntityInstance player, WorldPositionStruct worldPositionStruct)` — Performs class-specific logic; see call sites and owning manager flow.
- `public EntityInstance getPlayerForWindow(int windowID)` — Returns current state/value.
- `public boolean hasPlayerForWindow(int windowID)` — Boolean existence/availability check.
- `public CameraInstance getCameraForWindow(int windowID)` — Returns current state/value.
- `public WorldPositionStruct getPlayerPositionForWindow(int windowID)` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/entitypipeline/statistics/StatisticsHandle.java`

**Type:** `class StatisticsHandle`
  
**Inheritance/implements:** `extends HandlePackage`
  
**Package:** `program.bootstrap.entitypipeline.statistics`
  
**File size:** 83 lines

**What this class does:** `StatisticsHandle` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.entitypipeline.statistics`.

**Who this class talks to (direct imports):**
- `program.core.engine.HandlePackage`
- `program.core.settings.EngineSetting`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `public float getWalkSpeed()` — Returns current state/value.
- `public void setWalkSpeed(float walkSpeed)` — Mutates internal state for this object.
- `public float getMovementSpeed()` — Returns current state/value.
- `public void setMovementSpeed(float movementSpeed)` — Mutates internal state for this object.
- `public float getSprintSpeed()` — Returns current state/value.
- `public void setSprintSpeed(float sprintSpeed)` — Mutates internal state for this object.
- `public float getJumpHeight()` — Returns current state/value.
- `public void setJumpHeight(float jumpHeight)` — Mutates internal state for this object.
- `public float getReach()` — Returns current state/value.
- `public void setReach(float reach)` — Mutates internal state for this object.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.
