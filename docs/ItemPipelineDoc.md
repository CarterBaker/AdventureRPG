# ItemPipelineDoc

This is a human-readable subsystem manual. It explains responsibilities, collaboration points, and method intent for each class in scope.

Classes covered: **15**

## How to read this manual
- Start with the package flow notes at the top of each class section.
- Use **Who talks to this class** to identify collaborators.
- Use **Method intent** to understand lifecycle and API behavior.

## `core/src/program/bootstrap/itempipeline/ItemPipeline.java`

**Type:** `class ItemPipeline`
  
**Inheritance/implements:** `extends PipelinePackage`
  
**Package:** `program.bootstrap.itempipeline`
  
**File size:** 22 lines

**What this class does:** `ItemPipeline` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.itempipeline`.

**Who this class talks to (direct imports):**
- `program.bootstrap.itempipeline.itemdefinitionmanager.ItemDefinitionManager`
- `program.bootstrap.itempipeline.itemrotationmanager.ItemRotationManager`
- `program.bootstrap.itempipeline.tooltypemanager.ToolTypeManager`
- `program.core.engine.PipelinePackage`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/itempipeline/backpack/BackpackInstance.java`

**Type:** `class BackpackInstance`
  
**Inheritance/implements:** `extends InstancePackage`
  
**Package:** `program.bootstrap.itempipeline.backpack`
  
**File size:** 48 lines

**What this class does:** `BackpackInstance` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.itempipeline.backpack`.

**Who this class talks to (direct imports):**
- `program.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle`
- `program.core.engine.InstancePackage`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `public void addItem(ItemDefinitionHandle item)` — Registers a child object into manager-owned collections.
- `public void removeItem(ItemDefinitionHandle item)` — Unregisters and tears down child references.
- `public ObjectArrayList<ItemDefinitionHandle> getItems()` — Returns current state/value.
- `public boolean isEmpty()` — Performs class-specific logic; see call sites and owning manager flow.
- `public int size()` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/itempipeline/itemdefinition/ItemDefinitionData.java`

**Type:** `class ItemDefinitionData`
  
**Inheritance/implements:** `extends DataPackage`
  
**Package:** `program.bootstrap.itempipeline.itemdefinition`
  
**File size:** 81 lines

**What this class does:** `ItemDefinitionData` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.itempipeline.itemdefinition`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.mesh.MeshHandle`
- `program.core.engine.DataPackage`

**Method intent:**
- `package public ItemDefinitionData(String itemName, int itemID, float weight, boolean twoHanded, boolean isBackpack, MeshHandle meshHandle, int materialID)` — Performs class-specific logic; see call sites and owning manager flow.
- `public String getItemName()` — Returns current state/value.
- `public int getItemID()` — Returns current state/value.
- `public float getWeight()` — Returns current state/value.
- `public boolean isTwoHanded()` — Performs class-specific logic; see call sites and owning manager flow.
- `public boolean isBackpack()` — Performs class-specific logic; see call sites and owning manager flow.
- `public MeshHandle getMeshHandle()` — Returns current state/value.
- `public int getMaterialID()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/itempipeline/itemdefinition/ItemDefinitionHandle.java`

**Type:** `class ItemDefinitionHandle`
  
**Inheritance/implements:** `extends HandlePackage`
  
**Package:** `program.bootstrap.itempipeline.itemdefinition`
  
**File size:** 66 lines

**What this class does:** `ItemDefinitionHandle` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.itempipeline.itemdefinition`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.mesh.MeshHandle`
- `program.core.engine.HandlePackage`

**Method intent:**
- `public void constructor(ItemDefinitionData itemDefinitionData)` — Engine-side initialization entrypoint invoked post-create.
- `public ItemDefinitionData getItemDefinitionData()` — Returns current state/value.
- `public String getItemName()` — Returns current state/value.
- `public int getItemID()` — Returns current state/value.
- `public short getNameShort()` — Returns current state/value.
- `public short getEnchantShort()` — Returns current state/value.
- `public float getWeight()` — Returns current state/value.
- `public boolean isTwoHanded()` — Performs class-specific logic; see call sites and owning manager flow.
- `public boolean isBackpack()` — Performs class-specific logic; see call sites and owning manager flow.
- `public MeshHandle getMeshHandle()` — Returns current state/value.
- `public int getMaterialID()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/itempipeline/itemdefinitionmanager/InternalBuilder.java`

**Type:** `class InternalBuilder`
  
**Inheritance/implements:** `extends BuilderPackage`
  
**Package:** `program.bootstrap.itempipeline.itemdefinitionmanager`
  
**File size:** 96 lines

**What this class does:** `InternalBuilder` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.itempipeline.itemdefinitionmanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.mesh.MeshHandle`
- `program.bootstrap.geometrypipeline.meshmanager.MeshManager`
- `program.bootstrap.geometrypipeline.modelmanager.ModelManager`
- `program.bootstrap.itempipeline.itemdefinition.ItemDefinitionData`
- `program.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle`
- `program.bootstrap.itempipeline.util.ItemRegistryUtility`
- `program.bootstrap.shaderpipeline.materialmanager.MaterialManager`
- `program.core.engine.BuilderPackage`
- `program.core.settings.EngineSetting`
- `program.core.util.FileUtility`
- `program.core.util.JsonUtility`

**Method intent:**
- `protected void get()` — Returns current state/value.
- `package ObjectArrayList<ItemDefinitionHandle> build(File jsonFile, File root)` — Constructs derived runtime/handle data from source input.
- `private ItemDefinitionHandle parseItem(JsonObject itemJson, String pathPrefix)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/itempipeline/itemdefinitionmanager/InternalLoader.java`

**Type:** `class InternalLoader`
  
**Inheritance/implements:** `extends LoaderPackage`
  
**Package:** `program.bootstrap.itempipeline.itemdefinitionmanager`
  
**File size:** 92 lines

**What this class does:** `InternalLoader` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.itempipeline.itemdefinitionmanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle`
- `program.core.engine.LoaderPackage`
- `program.core.settings.EngineSetting`
- `program.core.util.FileUtility`

**Method intent:**
- `protected void scan()` — Discovers files/resources for later load.
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void get()` — Returns current state/value.
- `protected void load(File file)` — Parses external data into engine objects.
- `package void request(String itemName)` — Triggers on-demand loading or lookup.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/itempipeline/itemdefinitionmanager/ItemDefinitionManager.java`

**Type:** `class ItemDefinitionManager`
  
**Inheritance/implements:** `extends ManagerPackage`
  
**Package:** `program.bootstrap.itempipeline.itemdefinitionmanager`
  
**File size:** 85 lines

**What this class does:** `ItemDefinitionManager` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.itempipeline.itemdefinitionmanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle`
- `program.core.engine.ManagerPackage`
- `program.core.util.RegistryUtility`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `package void addItem(ItemDefinitionHandle item)` — Registers a child object into manager-owned collections.
- `public boolean hasItem(String itemName)` — Boolean existence/availability check.
- `public int getItemIDFromItemName(String itemName)` — Returns current state/value.
- `public ItemDefinitionHandle getItemHandleFromItemID(int itemID)` — Returns current state/value.
- `public ItemDefinitionHandle getItemHandleFromItemName(String itemName)` — Returns current state/value.
- `public void request(String itemName)` — Triggers on-demand loading or lookup.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/itempipeline/itemrotationmanager/InternalBufferSystem.java`

**Type:** `class InternalBufferSystem`
  
**Inheritance/implements:** `extends SystemPackage`
  
**Package:** `program.bootstrap.itempipeline.itemrotationmanager`
  
**File size:** 118 lines

**What this class does:** `InternalBufferSystem` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.itempipeline.itemrotationmanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.shaderpipeline.ubo.UBOHandle`
- `program.bootstrap.shaderpipeline.ubomanager.UBOManager`
- `program.core.engine.SystemPackage`
- `program.core.util.mathematics.extras.Direction3Vector`
- `program.core.util.mathematics.matrices.Matrix4`

**Method intent:**
- `protected void get()` — Returns current state/value.
- `protected void awake()` — Runs startup-time runtime activation work.
- `private void pushItemRotationData()` — Queues data for downstream systems (often render queues).
- `private Matrix4 buildRotation(Direction3Vector face, int spin)` — Constructs derived runtime/handle data from source input.
- `private Matrix4 faceRotation(Direction3Vector face)` — Performs class-specific logic; see call sites and owning manager flow.
- `private Matrix4 rotX(float deg)` — Performs class-specific logic; see call sites and owning manager flow.
- `private Matrix4 rotZ(float deg)` — Performs class-specific logic; see call sites and owning manager flow.
- `private Matrix4 axisRotation(float ax, float ay, float az, float deg)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/itempipeline/itemrotationmanager/ItemRotationManager.java`

**Type:** `class ItemRotationManager`
  
**Inheritance/implements:** `extends ManagerPackage`
  
**Package:** `program.bootstrap.itempipeline.itemrotationmanager`
  
**File size:** 11 lines

**What this class does:** `ItemRotationManager` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.itempipeline.itemrotationmanager`.

**Who this class talks to (direct imports):**
- `program.core.engine.ManagerPackage`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/itempipeline/tooltype/ToolTypeData.java`

**Type:** `class ToolTypeData`
  
**Inheritance/implements:** `extends DataPackage`
  
**Package:** `program.bootstrap.itempipeline.tooltype`
  
**File size:** 48 lines

**What this class does:** `ToolTypeData` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.itempipeline.tooltype`.

**Who this class talks to (direct imports):**
- `program.core.engine.DataPackage`

**Method intent:**
- `package public ToolTypeData(String toolTypeName, short toolTypeID, String defaultModelPath)` — Performs class-specific logic; see call sites and owning manager flow.
- `public String getToolTypeName()` — Returns current state/value.
- `public short getToolTypeID()` — Returns current state/value.
- `public String getDefaultModelPath()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/itempipeline/tooltype/ToolTypeHandle.java`

**Type:** `class ToolTypeHandle`
  
**Inheritance/implements:** `extends HandlePackage`
  
**Package:** `program.bootstrap.itempipeline.tooltype`
  
**File size:** 40 lines

**What this class does:** `ToolTypeHandle` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.itempipeline.tooltype`.

**Who this class talks to (direct imports):**
- `program.core.engine.HandlePackage`

**Method intent:**
- `public void constructor(ToolTypeData toolTypeData)` — Engine-side initialization entrypoint invoked post-create.
- `public ToolTypeData getToolTypeData()` — Returns current state/value.
- `public String getToolTypeName()` — Returns current state/value.
- `public short getToolTypeID()` — Returns current state/value.
- `public String getDefaultModelPath()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/itempipeline/tooltypemanager/InternalBuilder.java`

**Type:** `class InternalBuilder`
  
**Inheritance/implements:** `extends BuilderPackage`
  
**Package:** `program.bootstrap.itempipeline.tooltypemanager`
  
**File size:** 57 lines

**What this class does:** `InternalBuilder` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.itempipeline.tooltypemanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.itempipeline.tooltype.ToolTypeData`
- `program.bootstrap.itempipeline.tooltype.ToolTypeHandle`
- `program.core.engine.BuilderPackage`
- `program.core.util.FileUtility`
- `program.core.util.JsonUtility`
- `program.core.util.RegistryUtility`

**Method intent:**
- `package ObjectArrayList<ToolTypeHandle> build(File jsonFile, File root)` — Constructs derived runtime/handle data from source input.
- `private ToolTypeHandle parseTool(JsonObject toolJson, String pathPrefix)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/itempipeline/tooltypemanager/InternalLoader.java`

**Type:** `class InternalLoader`
  
**Inheritance/implements:** `extends LoaderPackage`
  
**Package:** `program.bootstrap.itempipeline.tooltypemanager`
  
**File size:** 117 lines

**What this class does:** `InternalLoader` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.itempipeline.tooltypemanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.itempipeline.tooltype.ToolTypeHandle`
- `program.core.engine.LoaderPackage`
- `program.core.settings.EngineSetting`
- `program.core.util.FileUtility`
- `program.core.util.JsonUtility`

**Method intent:**
- `protected void scan()` — Discovers files/resources for later load.
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void get()` — Returns current state/value.
- `private void preRegisterToolTypeNames(File file, String resourceName)` — Performs class-specific logic; see call sites and owning manager flow.
- `protected void load(File file)` — Parses external data into engine objects.
- `package void request(String toolTypeName)` — Triggers on-demand loading or lookup.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/itempipeline/tooltypemanager/ToolTypeManager.java`

**Type:** `class ToolTypeManager`
  
**Inheritance/implements:** `extends ManagerPackage`
  
**Package:** `program.bootstrap.itempipeline.tooltypemanager`
  
**File size:** 86 lines

**What this class does:** `ToolTypeManager` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.itempipeline.tooltypemanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.itempipeline.tooltype.ToolTypeHandle`
- `program.core.engine.ManagerPackage`
- `program.core.util.RegistryUtility`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `package void addToolType(ToolTypeHandle tool)` — Registers a child object into manager-owned collections.
- `public boolean hasToolType(String toolTypeName)` — Boolean existence/availability check.
- `public short getToolTypeIDFromToolTypeName(String toolTypeName)` — Returns current state/value.
- `public ToolTypeHandle getToolTypeHandleFromToolTypeID(short toolTypeID)` — Returns current state/value.
- `public ToolTypeHandle getToolTypeHandleFromToolTypeName(String toolTypeName)` — Returns current state/value.
- `public void request(String toolTypeName)` — Triggers on-demand loading or lookup.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/itempipeline/util/ItemRegistryUtility.java`

**Type:** `class ItemRegistryUtility`
  
**Inheritance/implements:** `extends EngineUtility`
  
**Package:** `program.bootstrap.itempipeline.util`
  
**File size:** 50 lines

**What this class does:** `ItemRegistryUtility` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.itempipeline.util`.

**Who this class talks to (direct imports):**
- `program.core.engine.EngineUtility`
- `program.core.settings.EngineSetting`

**Method intent:**
- `public int toItemIntID(String name)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/itempipeline/itemdefinitionmanager/InternalBuilder.java`.
- `public boolean isCollision(String incomingName, String existingName, int id)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.
