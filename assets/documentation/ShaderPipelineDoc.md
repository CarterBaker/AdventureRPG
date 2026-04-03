# ShaderPipelineDoc

This is a human-readable subsystem manual. It explains responsibilities, collaboration points, and method intent for each class in scope.

Classes covered: **98**

## How to read this manual
- Start with the package flow notes at the top of each class section.
- Use **Who talks to this class** to identify collaborators.
- Use **Method intent** to understand lifecycle and API behavior.

## `core/src/program/bootstrap/shaderpipeline/ShaderPipeline.java`

**Type:** `class ShaderPipeline`
  
**Inheritance/implements:** `extends PipelinePackage`
  
**Package:** `program.bootstrap.shaderpipeline`
  
**File size:** 25 lines

**What this class does:** `ShaderPipeline` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline`.

**Who this class talks to (direct imports):**
- `program.bootstrap.shaderpipeline.materialmanager.MaterialManager`
- `program.bootstrap.shaderpipeline.passmanager.PassManager`
- `program.bootstrap.shaderpipeline.shadermanager.ShaderManager`
- `program.bootstrap.shaderpipeline.spritemanager.SpriteManager`
- `program.bootstrap.shaderpipeline.texturemanager.TextureManager`
- `program.bootstrap.shaderpipeline.ubomanager.UBOManager`
- `program.core.engine.PipelinePackage`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/material/MaterialData.java`

**Type:** `class MaterialData`
  
**Inheritance/implements:** `extends DataPackage`
  
**Package:** `program.bootstrap.shaderpipeline.material`
  
**File size:** 127 lines

**What this class does:** `MaterialData` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.material`.

**Who this class talks to (direct imports):**
- `program.bootstrap.shaderpipeline.shader.ShaderHandle`
- `program.bootstrap.shaderpipeline.ubo.UBOHandle`
- `program.bootstrap.shaderpipeline.ubo.UBOInstance`
- `program.bootstrap.shaderpipeline.uniforms.UniformStruct`
- `program.core.engine.DataPackage`

**Method intent:**
- `package public MaterialData(String materialName, int materialID, ShaderHandle shaderHandle, Object2ObjectOpenHashMap<String, UBOHandle> sourceUBOs, Object2ObjectOpenHashMap<String, UniformStruct<?>> uniforms)` — Performs class-specific logic; see call sites and owning manager flow.
- `package public MaterialData(MaterialData source)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void setUBO(UBOInstance ubo)` — Mutates internal state for this object.
- `public <T> void setUniform(String uniformName, T value)` — Mutates internal state for this object.
- `public String getMaterialName()` — Returns current state/value.
- `public int getMaterialID()` — Returns current state/value.
- `public ShaderHandle getShaderHandle()` — Returns current state/value.
- `public Object2ObjectOpenHashMap<String, UBOHandle> getSourceUBOs()` — Returns current state/value.
- `public Int2ObjectOpenHashMap<UBOInstance> getInstanceUBOs()` — Returns current state/value.
- `public UBOInstance getInstanceUBO(int bindingPoint)` — Returns current state/value.
- `public Object2ObjectOpenHashMap<String, UniformStruct<?>> getUniforms()` — Returns current state/value.
- `public ObjectArrayList<String> getUniformKeys()` — Returns current state/value.
- `public UniformStruct<?> getUniform(String uniformName)` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/material/MaterialHandle.java`

**Type:** `class MaterialHandle`
  
**Inheritance/implements:** `extends HandlePackage`
  
**Package:** `program.bootstrap.shaderpipeline.material`
  
**File size:** 80 lines

**What this class does:** `MaterialHandle` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.material`.

**Who this class talks to (direct imports):**
- `program.bootstrap.shaderpipeline.shader.ShaderHandle`
- `program.bootstrap.shaderpipeline.ubo.UBOHandle`
- `program.bootstrap.shaderpipeline.ubo.UBOInstance`
- `program.bootstrap.shaderpipeline.uniforms.UniformStruct`
- `program.core.engine.HandlePackage`

**Method intent:**
- `public void constructor(MaterialData data)` — Engine-side initialization entrypoint invoked post-create.
- `public void setUBO(UBOInstance ubo)` — Mutates internal state for this object.
- `public <T> void setUniform(String uniformName, T value)` — Mutates internal state for this object.
- `public MaterialData getMaterialData()` — Returns current state/value.
- `public String getMaterialName()` — Returns current state/value.
- `public int getMaterialID()` — Returns current state/value.
- `public ShaderHandle getShaderHandle()` — Returns current state/value.
- `public Object2ObjectOpenHashMap<String, UBOHandle> getSourceUBOs()` — Returns current state/value.
- `public Int2ObjectOpenHashMap<UBOInstance> getInstanceUBOs()` — Returns current state/value.
- `public UBOInstance getInstanceUBO(int bindingPoint)` — Returns current state/value.
- `public Object2ObjectOpenHashMap<String, UniformStruct<?>> getUniforms()` — Returns current state/value.
- `public ObjectArrayList<String> getUniformKeys()` — Returns current state/value.
- `public UniformStruct<?> getUniform(String uniformName)` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/material/MaterialInstance.java`

**Type:** `class MaterialInstance`
  
**Inheritance/implements:** `extends InstancePackage`
  
**Package:** `program.bootstrap.shaderpipeline.material`
  
**File size:** 80 lines

**What this class does:** `MaterialInstance` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.material`.

**Who this class talks to (direct imports):**
- `program.bootstrap.shaderpipeline.shader.ShaderHandle`
- `program.bootstrap.shaderpipeline.ubo.UBOHandle`
- `program.bootstrap.shaderpipeline.ubo.UBOInstance`
- `program.bootstrap.shaderpipeline.uniforms.UniformStruct`
- `program.core.engine.InstancePackage`

**Method intent:**
- `public void constructor(MaterialData data)` — Engine-side initialization entrypoint invoked post-create.
- `public void setUBO(UBOInstance ubo)` — Mutates internal state for this object.
- `public <T> void setUniform(String uniformName, T value)` — Mutates internal state for this object.
- `public MaterialData getMaterialData()` — Returns current state/value.
- `public String getMaterialName()` — Returns current state/value.
- `public int getMaterialID()` — Returns current state/value.
- `public ShaderHandle getShaderHandle()` — Returns current state/value.
- `public Object2ObjectOpenHashMap<String, UBOHandle> getSourceUBOs()` — Returns current state/value.
- `public Int2ObjectOpenHashMap<UBOInstance> getInstanceUBOs()` — Returns current state/value.
- `public UBOInstance getInstanceUBO(int bindingPoint)` — Returns current state/value.
- `public Object2ObjectOpenHashMap<String, UniformStruct<?>> getUniforms()` — Returns current state/value.
- `public ObjectArrayList<String> getUniformKeys()` — Returns current state/value.
- `public UniformStruct<?> getUniform(String uniformName)` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/materialmanager/InternalBuilder.java`

**Type:** `class InternalBuilder`
  
**Inheritance/implements:** `extends BuilderPackage`
  
**Package:** `program.bootstrap.shaderpipeline.materialmanager`
  
**File size:** 138 lines

**What this class does:** `InternalBuilder` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.materialmanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.shaderpipeline.material.MaterialData`
- `program.bootstrap.shaderpipeline.material.MaterialHandle`
- `program.bootstrap.shaderpipeline.shader.ShaderHandle`
- `program.bootstrap.shaderpipeline.texturemanager.TextureManager`
- `program.bootstrap.shaderpipeline.ubo.UBOHandle`
- `program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformType`
- `program.bootstrap.shaderpipeline.uniforms.UniformUtility`
- `program.bootstrap.shaderpipeline.shadermanager.ShaderManager`
- `program.bootstrap.shaderpipeline.ubomanager.UBOManager`
- `program.core.engine.BuilderPackage`
- `program.core.util.JsonUtility`
- `program.core.util.RegistryUtility`

**Method intent:**
- `protected void get()` — Returns current state/value.
- `package void build(File file, String materialName)` — Constructs derived runtime/handle data from source input.
- `private boolean isSamplerType(UniformType type)` — Performs class-specific logic; see call sites and owning manager flow.
- `private int resolveTextureHandle(String textureName, String uniformName, String materialName)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/materialmanager/InternalLoader.java`

**Type:** `class InternalLoader`
  
**Inheritance/implements:** `extends LoaderPackage`
  
**Package:** `program.bootstrap.shaderpipeline.materialmanager`
  
**File size:** 89 lines

**What this class does:** `InternalLoader` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.materialmanager`.

**Who this class talks to (direct imports):**
- `program.core.engine.LoaderPackage`
- `program.core.settings.EngineSetting`
- `program.core.util.FileUtility`

**Method intent:**
- `protected void scan()` — Discovers files/resources for later load.
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void get()` — Returns current state/value.
- `protected void load(File file)` — Parses external data into engine objects.
- `package void request(String materialName)` — Triggers on-demand loading or lookup.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/materialmanager/MaterialManager.java`

**Type:** `class MaterialManager`
  
**Inheritance/implements:** `extends ManagerPackage`
  
**Package:** `program.bootstrap.shaderpipeline.materialmanager`
  
**File size:** 95 lines

**What this class does:** `MaterialManager` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.materialmanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.shaderpipeline.material.MaterialData`
- `program.bootstrap.shaderpipeline.material.MaterialHandle`
- `program.bootstrap.shaderpipeline.material.MaterialInstance`
- `program.core.engine.ManagerPackage`
- `program.core.util.RegistryUtility`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `package void addMaterial(String materialName, MaterialHandle handle)` — Registers a child object into manager-owned collections.
- `public void request(String materialName)` — Triggers on-demand loading or lookup.
- `public boolean hasMaterial(String materialName)` — Boolean existence/availability check.
- `public int getMaterialIDFromMaterialName(String materialName)` — Returns current state/value.
- `public MaterialHandle getMaterialHandleFromMaterialID(int materialID)` — Returns current state/value.
- `public MaterialHandle getMaterialHandleFromMaterialName(String materialName)` — Returns current state/value.
- `public MaterialInstance cloneMaterial(String materialName)` — Performs class-specific logic; see call sites and owning manager flow.
- `public MaterialInstance cloneMaterial(int materialID)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/pass/PassData.java`

**Type:** `class PassData`
  
**Inheritance/implements:** `extends DataPackage`
  
**Package:** `program.bootstrap.shaderpipeline.pass`
  
**File size:** 71 lines

**What this class does:** `PassData` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.pass`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.mesh.MeshHandle`
- `program.bootstrap.geometrypipeline.model.ModelInstance`
- `program.bootstrap.shaderpipeline.material.MaterialInstance`
- `program.core.engine.DataPackage`

**Method intent:**
- `package public PassData(String passName, int passID, MeshHandle meshHandle, MaterialInstance material, ModelInstance modelInstance)` — Performs class-specific logic; see call sites and owning manager flow.
- `public <T> void setUniform(String uniformName, T value)` — Mutates internal state for this object.
- `public String getPassName()` — Returns current state/value.
- `public int getPassID()` — Returns current state/value.
- `public MeshHandle getMeshHandle()` — Returns current state/value.
- `public MaterialInstance getMaterial()` — Returns current state/value.
- `public ModelInstance getModelInstance()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/pass/PassHandle.java`

**Type:** `class PassHandle`
  
**Inheritance/implements:** `extends HandlePackage`
  
**Package:** `program.bootstrap.shaderpipeline.pass`
  
**File size:** 62 lines

**What this class does:** `PassHandle` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.pass`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.mesh.MeshHandle`
- `program.bootstrap.geometrypipeline.model.ModelInstance`
- `program.bootstrap.shaderpipeline.material.MaterialInstance`
- `program.bootstrap.shaderpipeline.ubo.UBOInstance`
- `program.core.engine.HandlePackage`

**Method intent:**
- `public void constructor(PassData passData)` — Engine-side initialization entrypoint invoked post-create.
- `public void setUBO(UBOInstance ubo)` — Mutates internal state for this object.
- `public <T> void setUniform(String uniformName, T value)` — Mutates internal state for this object.
- `public PassData getPassData()` — Returns current state/value.
- `public String getPassName()` — Returns current state/value.
- `public int getPassID()` — Returns current state/value.
- `public MeshHandle getMeshHandle()` — Returns current state/value.
- `public MaterialInstance getMaterial()` — Returns current state/value.
- `public ModelInstance getModelInstance()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/pass/PassInstance.java`

**Type:** `class PassInstance`
  
**Inheritance/implements:** `extends InstancePackage`
  
**Package:** `program.bootstrap.shaderpipeline.pass`
  
**File size:** 61 lines

**What this class does:** `PassInstance` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.pass`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.mesh.MeshHandle`
- `program.bootstrap.geometrypipeline.model.ModelInstance`
- `program.bootstrap.shaderpipeline.material.MaterialInstance`
- `program.bootstrap.shaderpipeline.ubo.UBOInstance`
- `program.core.engine.InstancePackage`

**Method intent:**
- `public void constructor(PassData passData)` — Engine-side initialization entrypoint invoked post-create.
- `public void setUBO(UBOInstance ubo)` — Mutates internal state for this object.
- `public <T> void setUniform(String uniformName, T value)` — Mutates internal state for this object.
- `public PassData getPassData()` — Returns current state/value.
- `public String getPassName()` — Returns current state/value.
- `public int getPassID()` — Returns current state/value.
- `public MeshHandle getMeshHandle()` — Returns current state/value.
- `public MaterialInstance getMaterial()` — Returns current state/value.
- `public ModelInstance getModelInstance()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/passmanager/InternalBuilder.java`

**Type:** `class InternalBuilder`
  
**Inheritance/implements:** `extends BuilderPackage`
  
**Package:** `program.bootstrap.shaderpipeline.passmanager`
  
**File size:** 64 lines

**What this class does:** `InternalBuilder` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.passmanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.mesh.MeshHandle`
- `program.bootstrap.geometrypipeline.meshmanager.MeshManager`
- `program.bootstrap.geometrypipeline.model.ModelInstance`
- `program.bootstrap.shaderpipeline.material.MaterialInstance`
- `program.bootstrap.shaderpipeline.materialmanager.MaterialManager`
- `program.bootstrap.shaderpipeline.pass.PassData`
- `program.bootstrap.shaderpipeline.pass.PassHandle`
- `program.core.engine.BuilderPackage`
- `program.core.util.JsonUtility`
- `program.core.util.RegistryUtility`

**Method intent:**
- `protected void get()` — Returns current state/value.
- `package PassHandle build(File file, String passName)` — Constructs derived runtime/handle data from source input.
- `private MeshHandle getMeshHandleFromJson(JsonObject json)` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/passmanager/InternalLoader.java`

**Type:** `class InternalLoader`
  
**Inheritance/implements:** `extends LoaderPackage`
  
**Package:** `program.bootstrap.shaderpipeline.passmanager`
  
**File size:** 81 lines

**What this class does:** `InternalLoader` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.passmanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.shaderpipeline.pass.PassHandle`
- `program.core.engine.LoaderPackage`
- `program.core.settings.EngineSetting`
- `program.core.util.FileUtility`

**Method intent:**
- `protected void scan()` — Discovers files/resources for later load.
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void get()` — Returns current state/value.
- `protected void load(File file)` — Parses external data into engine objects.
- `package void request(String passName)` — Triggers on-demand loading or lookup.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/passmanager/PassManager.java`

**Type:** `class PassManager`
  
**Inheritance/implements:** `extends ManagerPackage`
  
**Package:** `program.bootstrap.shaderpipeline.passmanager`
  
**File size:** 136 lines

**What this class does:** `PassManager` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.passmanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.model.ModelInstance`
- `program.bootstrap.renderpipeline.rendermanager.RenderManager`
- `program.bootstrap.shaderpipeline.material.MaterialInstance`
- `program.bootstrap.shaderpipeline.materialmanager.MaterialManager`
- `program.bootstrap.shaderpipeline.pass.PassData`
- `program.bootstrap.shaderpipeline.pass.PassHandle`
- `program.bootstrap.shaderpipeline.pass.PassInstance`
- `program.core.engine.ManagerPackage`
- `program.core.kernel.window.WindowInstance`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void get()` — Returns current state/value.
- `package void addPassHandle(PassHandle handle)` — Registers a child object into manager-owned collections.
- `public void pushPass(PassHandle pass, int depth)` — Queues data for downstream systems (often render queues).
- `public void pushPass(PassInstance pass, int depth)` — Queues data for downstream systems (often render queues).
- `public void pushPass(PassHandle pass, int depth, WindowInstance window)` — Queues data for downstream systems (often render queues).
- `public void pushPass(PassInstance pass, int depth, WindowInstance window)` — Queues data for downstream systems (often render queues).
- `public void request(String passName)` — Triggers on-demand loading or lookup.
- `public boolean hasPass(String passName)` — Boolean existence/availability check.
- `public int getPassIDFromPassName(String passName)` — Returns current state/value.
- `public PassHandle getPassHandleFromPassID(int passID)` — Returns current state/value.
- `public PassHandle getPassHandleFromPassName(String passName)` — Returns current state/value.
- `public PassInstance clonePass(int passID)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/shaderpipeline/pass/PassHandle.java`, `core/src/program/bootstrap/shaderpipeline/pass/PassInstance.java`.
- `public PassInstance clonePass(String passName)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/shaderpipeline/pass/PassHandle.java`, `core/src/program/bootstrap/shaderpipeline/pass/PassInstance.java`.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/shader/ShaderData.java`

**Type:** `class ShaderData`
  
**Inheritance/implements:** `extends DataPackage`
  
**Package:** `program.bootstrap.shaderpipeline.shader`
  
**File size:** 68 lines

**What this class does:** `ShaderData` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.shader`.

**Who this class talks to (direct imports):**
- `program.bootstrap.shaderpipeline.uniforms.UniformStruct`
- `program.core.engine.DataPackage`

**Method intent:**
- `package public ShaderData(String shaderName, int shaderID, int gpuHandle)` — Performs class-specific logic; see call sites and owning manager flow.
- `package void addCompiledUniform(String name, UniformStruct<?> uniform)` — Registers a child object into manager-owned collections.
- `package void addCompiledUBOBlockName(String blockName)` — Registers a child object into manager-owned collections.
- `public String getShaderName()` — Returns current state/value.
- `public int getShaderID()` — Returns current state/value.
- `public int getGpuHandle()` — Returns current state/value.
- `public Object2ObjectOpenHashMap<String, UniformStruct<?>> getCompiledUniforms()` — Returns current state/value.
- `public ObjectArrayList<String> getCompiledUBOBlockNames()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/shader/ShaderHandle.java`

**Type:** `class ShaderHandle`
  
**Inheritance/implements:** `extends HandlePackage`
  
**Package:** `program.bootstrap.shaderpipeline.shader`
  
**File size:** 61 lines

**What this class does:** `ShaderHandle` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.shader`.

**Who this class talks to (direct imports):**
- `program.bootstrap.shaderpipeline.uniforms.UniformStruct`
- `program.core.engine.HandlePackage`

**Method intent:**
- `public void constructor(ShaderData shaderData)` — Engine-side initialization entrypoint invoked post-create.
- `public void addCompiledUniform(String name, UniformStruct<?> uniform)` — Registers a child object into manager-owned collections.
- `public void addCompiledUBOBlockName(String blockName)` — Registers a child object into manager-owned collections.
- `public ShaderData getShaderData()` — Returns current state/value.
- `public String getShaderName()` — Returns current state/value.
- `public int getShaderID()` — Returns current state/value.
- `public int getGpuHandle()` — Returns current state/value.
- `public Object2ObjectOpenHashMap<String, UniformStruct<?>> getCompiledUniforms()` — Returns current state/value.
- `public ObjectArrayList<String> getCompiledUBOBlockNames()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/shader/ShaderSourceStruct.java`

**Type:** `class ShaderSourceStruct`
  
**Inheritance/implements:** `extends StructPackage`
  
**Package:** `program.bootstrap.shaderpipeline.shader`
  
**File size:** 119 lines

**What this class does:** `ShaderSourceStruct` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.shader`.

**Who this class talks to (direct imports):**
- `program.bootstrap.shaderpipeline.uniforms.UniformData`
- `program.core.engine.StructPackage`

**Method intent:**
- `package public ShaderSourceStruct(ShaderType shaderType, String shaderName, File shaderFile)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void setVersion(String version)` — Mutates internal state for this object.
- `public void addDirectInclude(ShaderSourceStruct include)` — Registers a child object into manager-owned collections.
- `public void addBufferBlockName(String blockName)` — Registers a child object into manager-owned collections.
- `public void addUniformDeclaration(UniformData uniform)` — Registers a child object into manager-owned collections.
- `public void setVert(ShaderSourceStruct vert)` — Mutates internal state for this object.
- `public void setFrag(ShaderSourceStruct frag)` — Mutates internal state for this object.
- `public void addFlattenedInclude(ShaderSourceStruct include)` — Registers a child object into manager-owned collections.
- `public ShaderType getShaderType()` — Returns current state/value.
- `public String getShaderName()` — Returns current state/value.
- `public File getShaderFile()` — Returns current state/value.
- `public String getVersion()` — Returns current state/value.
- `public ObjectArrayList<ShaderSourceStruct> getDirectIncludes()` — Returns current state/value.
- `public ObjectArrayList<String> getBufferBlockNames()` — Returns current state/value.
- `public ObjectArrayList<UniformData> getUniformDeclarations()` — Returns current state/value.
- `public ShaderSourceStruct getVert()` — Returns current state/value.
- `public ShaderSourceStruct getFrag()` — Returns current state/value.
- `public ObjectArrayList<ShaderSourceStruct> getFlattenedIncludes()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/shader/ShaderType.java`

**Type:** `enum ShaderType`
  
**Package:** `program.bootstrap.shaderpipeline.shader`
  
**File size:** 13 lines

**What this class does:** `ShaderType` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.shader`.

**Method intent:** No concrete methods parsed (interface marker, enum, or data-only declaration).

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/shadermanager/FileParserUtility.java`

**Type:** `class FileParserUtility`
  
**Inheritance/implements:** `extends UtilityPackage`
  
**Package:** `program.bootstrap.shaderpipeline.shadermanager`
  
**File size:** 172 lines

**What this class does:** `FileParserUtility` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.shadermanager`.

**Who this class talks to (direct imports):**
- `program.core.engine.UtilityPackage`

**Method intent:**
- `package String convertFileToRawText(File file)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/shaderpipeline/shadermanager/GLSLUtility.java`, `core/src/program/bootstrap/shaderpipeline/shadermanager/InternalBuilder.java`.
- `package ObjectArrayList<String> convertRawTextToArray(String rawText)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/shaderpipeline/shadermanager/InternalBuilder.java`.
- `private String stripLineComments(String text)` — Performs class-specific logic; see call sites and owning manager flow.
- `package String stripBlockComments(String text)` — Performs class-specific logic; see call sites and owning manager flow.
- `package boolean lineStartsWith(String line, String token)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/shaderpipeline/shadermanager/InternalBuilder.java`.
- `package int countCharInString(String str, char target)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/shaderpipeline/shadermanager/InternalBuilder.java`.
- `package int extractBufferBinding(String line)` — Performs class-specific logic; see call sites and owning manager flow.
- `package int findLastTypeDelimiter(String declaration)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/shaderpipeline/shadermanager/InternalBuilder.java`.
- `package int parseIntOrDefault(String str, int defaultValue)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/shaderpipeline/shadermanager/InternalBuilder.java`.
- `package String extractPayloadAfterToken(String line, String token)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/shaderpipeline/shadermanager/InternalBuilder.java`.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/shadermanager/GLSLUtility.java`

**Type:** `class GLSLUtility`
  
**Inheritance/implements:** `extends UtilityPackage`
  
**Package:** `program.bootstrap.shaderpipeline.shadermanager`
  
**File size:** 156 lines

**What this class does:** `GLSLUtility` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.shadermanager`.

**Who this class talks to (direct imports):**
- `program.core.app.CoreContext`
- `program.core.util.graphics.gl.GL20`
- `program.core.util.graphics.gl.GL30`
- `program.core.util.memory.BufferUtils`
- `program.bootstrap.shaderpipeline.shader.ShaderSourceStruct`
- `program.core.engine.UtilityPackage`
- `program.core.settings.EngineSetting`

**Method intent:**
- `package int createShaderProgram(ShaderSourceStruct assembly)` — Allocates/initializes child systems or resources. Called via static reference from: `core/src/program/bootstrap/shaderpipeline/shadermanager/InternalLoader.java`.
- `private int compileShaderFromSource(int type, String source, String shaderName)` — Performs class-specific logic; see call sites and owning manager flow.
- `package String preprocessShaderSource(ShaderSourceStruct assembly, ShaderSourceStruct source)` — Performs class-specific logic; see call sites and owning manager flow.
- `private String stripDirectives(String source)` — Performs class-specific logic; see call sites and owning manager flow.
- `package int getUniformLocation(int programHandle, String uniformName)` — Returns current state/value. Called via static reference from: `core/src/program/bootstrap/shaderpipeline/shadermanager/InternalLoader.java`.
- `package void bindUniformBlock(int programHandle, String blockName, int bindingPoint)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/renderpipeline/compositerendersystem/CompositeRenderSystem.java`, `core/src/program/bootstrap/shaderpipeline/shadermanager/ShaderManager.java`.
- `package void deleteShaderProgram(int programHandle)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/shaderpipeline/shadermanager/ShaderManager.java`.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/shadermanager/InternalBuilder.java`

**Type:** `class InternalBuilder`
  
**Inheritance/implements:** `extends BuilderPackage`
  
**Package:** `program.bootstrap.shaderpipeline.shadermanager`
  
**File size:** 292 lines

**What this class does:** `InternalBuilder` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.shadermanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.shaderpipeline.shader.ShaderSourceStruct`
- `program.bootstrap.shaderpipeline.shader.ShaderType`
- `program.bootstrap.shaderpipeline.uniforms.UniformData`
- `program.bootstrap.shaderpipeline.uniforms.UniformType`
- `program.core.engine.BuilderPackage`
- `program.core.settings.EngineSetting`
- `program.core.util.JsonUtility`

**Method intent:**
- `protected void get()` — Returns current state/value.
- `package void parseShaderFile(ShaderSourceStruct source)` — Performs class-specific logic; see call sites and owning manager flow.
- `private String parseVersionInfo(ObjectArrayList<String> lines)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void parseUniforms(ShaderSourceStruct source, ObjectArrayList<String> lines)` — Performs class-specific logic; see call sites and owning manager flow.
- `private boolean isUniformBlockStart(String line)` — Performs class-specific logic; see call sites and owning manager flow.
- `private String parseUniformBlockName(String line)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void parseUniformDeclaration(String declaration, ShaderSourceStruct source)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void parseVariableNames(String namesStr, UniformType uniformType, ShaderSourceStruct source)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void parseIncludes(ShaderSourceStruct source, ObjectArrayList<String> lines)` — Performs class-specific logic; see call sites and owning manager flow.
- `package ShaderSourceStruct buildAssembly(File jsonFile)` — Constructs derived runtime/handle data from source input.
- `private ShaderSourceStruct collectIncludes(ShaderSourceStruct assembly)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void collectPostOrder(ShaderSourceStruct assembly, ShaderSourceStruct source, ObjectArrayList<ShaderSourceStruct> visited)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/shadermanager/InternalLoader.java`

**Type:** `class InternalLoader`
  
**Inheritance/implements:** `extends LoaderPackage`
  
**Package:** `program.bootstrap.shaderpipeline.shadermanager`
  
**File size:** 227 lines

**What this class does:** `InternalLoader` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.shadermanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.shaderpipeline.shader.ShaderData`
- `program.bootstrap.shaderpipeline.shader.ShaderHandle`
- `program.bootstrap.shaderpipeline.shader.ShaderSourceStruct`
- `program.bootstrap.shaderpipeline.shader.ShaderType`
- `program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformData`
- `program.bootstrap.shaderpipeline.uniforms.UniformStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformUtility`
- `program.core.engine.LoaderPackage`
- `program.core.settings.EngineSetting`
- `program.core.util.FileUtility`
- `program.core.util.RegistryUtility`

**Method intent:**
- `protected void scan()` — Discovers files/resources for later load.
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void get()` — Returns current state/value.
- `protected void awake()` — Runs startup-time runtime activation work.
- `private void categorizeFile(File file)` — Performs class-specific logic; see call sites and owning manager flow.
- `protected void load(File file)` — Parses external data into engine objects.
- `package void request(String shaderName)` — Triggers on-demand loading or lookup.
- `private ShaderHandle assembleShader(ShaderSourceStruct assembly)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void assembleBuffers(ShaderHandle shader, ShaderSourceStruct assembly)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void addBuffersFromSource(ShaderHandle shader, ShaderSourceStruct source)` — Registers a child object into manager-owned collections.
- `private void assembleUniforms(ShaderHandle shader, ShaderSourceStruct assembly)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void addUniformsFromSource(ShaderHandle shader, ShaderSourceStruct source)` — Registers a child object into manager-owned collections.
- `private void addUniform(ShaderHandle shader, UniformData uniformData)` — Registers a child object into manager-owned collections.
- `package ShaderSourceStruct getSourceStruct(String key)` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/shadermanager/ShaderManager.java`

**Type:** `class ShaderManager`
  
**Inheritance/implements:** `extends ManagerPackage`
  
**Package:** `program.bootstrap.shaderpipeline.shadermanager`
  
**File size:** 97 lines

**What this class does:** `ShaderManager` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.shadermanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.shaderpipeline.shader.ShaderHandle`
- `program.bootstrap.shaderpipeline.ubo.UBOHandle`
- `program.bootstrap.shaderpipeline.ubomanager.UBOManager`
- `program.core.engine.ManagerPackage`
- `program.core.util.RegistryUtility`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void get()` — Returns current state/value.
- `protected void dispose()` — Releases owned resources and unregisters state.
- `package void addShaderHandle(ShaderHandle handle)` — Registers a child object into manager-owned collections.
- `package void bindShaderToUBO(ShaderHandle shader, String blockName)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void request(String shaderName)` — Triggers on-demand loading or lookup.
- `public boolean hasShader(String shaderName)` — Boolean existence/availability check.
- `public int getShaderIDFromShaderName(String shaderName)` — Returns current state/value.
- `public ShaderHandle getShaderHandleFromShaderID(int shaderID)` — Returns current state/value.
- `public ShaderHandle getShaderHandleFromShaderName(String shaderName)` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/sprite/SpriteData.java`

**Type:** `class SpriteData`
  
**Inheritance/implements:** `extends DataPackage`
  
**Package:** `program.bootstrap.shaderpipeline.sprite`
  
**File size:** 116 lines

**What this class does:** `SpriteData` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.sprite`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.model.ModelInstance`
- `program.bootstrap.shaderpipeline.ubo.UBOInstance`
- `program.core.engine.DataPackage`

**Method intent:**
- `package public SpriteData(String name, int gpuHandle, int width, int height, float borderLeft, float borderBottom, float borderRight, float borderTop, ModelInstance modelInstance)` — Performs class-specific logic; see call sites and owning manager flow.
- `package public SpriteData(SpriteData source, ModelInstance modelInstance, UBOInstance sliceData)` — Performs class-specific logic; see call sites and owning manager flow.
- `public String getName()` — Returns current state/value.
- `public int getGpuHandle()` — Returns current state/value.
- `public int getWidth()` — Returns current state/value.
- `public int getHeight()` — Returns current state/value.
- `public float getBorderLeft()` — Returns current state/value.
- `public float getBorderBottom()` — Returns current state/value.
- `public float getBorderRight()` — Returns current state/value.
- `public float getBorderTop()` — Returns current state/value.
- `public ModelInstance getModelInstance()` — Returns current state/value.
- `public UBOInstance getSliceData()` — Returns current state/value.
- `public boolean hasSlice()` — Boolean existence/availability check.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/sprite/SpriteHandle.java`

**Type:** `class SpriteHandle`
  
**Inheritance/implements:** `extends HandlePackage`
  
**Package:** `program.bootstrap.shaderpipeline.sprite`
  
**File size:** 69 lines

**What this class does:** `SpriteHandle` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.sprite`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.model.ModelInstance`
- `program.core.engine.HandlePackage`

**Method intent:**
- `public void constructor(SpriteData spriteData)` — Engine-side initialization entrypoint invoked post-create.
- `public SpriteData getSpriteData()` — Returns current state/value.
- `public String getName()` — Returns current state/value.
- `public int getGpuHandle()` — Returns current state/value.
- `public int getWidth()` — Returns current state/value.
- `public int getHeight()` — Returns current state/value.
- `public float getBorderLeft()` — Returns current state/value.
- `public float getBorderBottom()` — Returns current state/value.
- `public float getBorderRight()` — Returns current state/value.
- `public float getBorderTop()` — Returns current state/value.
- `public boolean hasSlice()` — Boolean existence/availability check.
- `public ModelInstance getModelInstance()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/sprite/SpriteInstance.java`

**Type:** `class SpriteInstance`
  
**Inheritance/implements:** `extends InstancePackage`
  
**Package:** `program.bootstrap.shaderpipeline.sprite`
  
**File size:** 54 lines

**What this class does:** `SpriteInstance` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.sprite`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.model.ModelInstance`
- `program.bootstrap.shaderpipeline.ubo.UBOInstance`
- `program.core.engine.InstancePackage`

**Method intent:**
- `public void constructor(SpriteData spriteData)` — Engine-side initialization entrypoint invoked post-create.
- `public SpriteData getSpriteData()` — Returns current state/value.
- `public String getName()` — Returns current state/value.
- `public int getGpuHandle()` — Returns current state/value.
- `public int getWidth()` — Returns current state/value.
- `public int getHeight()` — Returns current state/value.
- `public ModelInstance getModelInstance()` — Returns current state/value.
- `public UBOInstance getSliceData()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/spritemanager/GLSLUtility.java`

**Type:** `class GLSLUtility`
  
**Inheritance/implements:** `extends UtilityPackage`
  
**Package:** `program.bootstrap.shaderpipeline.spritemanager`
  
**File size:** 62 lines

**What this class does:** `GLSLUtility` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.spritemanager`.

**Who this class talks to (direct imports):**
- `program.core.app.CoreContext`
- `program.core.util.graphics.gl.GL20`
- `program.core.util.image.Pixmap`
- `program.core.engine.UtilityPackage`
- `program.core.util.PixmapUtility`

**Method intent:**
- `package int pushSprite(BufferedImage image)` — Queues data for downstream systems (often render queues). Called via static reference from: `core/src/program/bootstrap/shaderpipeline/spritemanager/InternalLoader.java`.
- `package void deleteSprite(int handle)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/shaderpipeline/spritemanager/SpriteManager.java`.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/spritemanager/InternalBuilder.java`

**Type:** `class InternalBuilder`
  
**Inheritance/implements:** `extends BuilderPackage`
  
**Package:** `program.bootstrap.shaderpipeline.spritemanager`
  
**File size:** 61 lines

**What this class does:** `InternalBuilder` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.spritemanager`.

**Who this class talks to (direct imports):**
- `program.core.engine.BuilderPackage`
- `program.core.util.JsonUtility`

**Method intent:**
- `package BufferedImage loadImage(File file)` — Parses external data into engine objects.
- `package float[] parseCompanionBorder(File imageFile)` — Performs class-specific logic; see call sites and owning manager flow.
- `private File getCompanionJson(File imageFile)` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/spritemanager/InternalLoader.java`

**Type:** `class InternalLoader`
  
**Inheritance/implements:** `extends LoaderPackage`
  
**Package:** `program.bootstrap.shaderpipeline.spritemanager`
  
**File size:** 151 lines

**What this class does:** `InternalLoader` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.spritemanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.mesh.MeshHandle`
- `program.bootstrap.geometrypipeline.meshmanager.MeshManager`
- `program.bootstrap.geometrypipeline.model.ModelInstance`
- `program.bootstrap.geometrypipeline.modelmanager.ModelManager`
- `program.bootstrap.shaderpipeline.material.MaterialInstance`
- `program.bootstrap.shaderpipeline.materialmanager.MaterialManager`
- `program.bootstrap.shaderpipeline.sprite.SpriteData`
- `program.bootstrap.shaderpipeline.sprite.SpriteHandle`
- `program.core.engine.LoaderPackage`
- `program.core.settings.EngineSetting`
- `program.core.util.FileUtility`

**Method intent:**
- `protected void scan()` — Discovers files/resources for later load.
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void get()` — Returns current state/value.
- `protected void awake()` — Runs startup-time runtime activation work.
- `protected void load(File file)` — Parses external data into engine objects.
- `package void request(String spriteName)` — Triggers on-demand loading or lookup.
- `package MeshHandle getDefaultMeshHandle()` — Returns current state/value.
- `package int getDefaultMaterialID()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/spritemanager/SpriteManager.java`

**Type:** `class SpriteManager`
  
**Inheritance/implements:** `extends ManagerPackage`
  
**Package:** `program.bootstrap.shaderpipeline.spritemanager`
  
**File size:** 148 lines

**What this class does:** `SpriteManager` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.spritemanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.model.ModelInstance`
- `program.bootstrap.geometrypipeline.modelmanager.ModelManager`
- `program.bootstrap.shaderpipeline.material.MaterialInstance`
- `program.bootstrap.shaderpipeline.materialmanager.MaterialManager`
- `program.bootstrap.shaderpipeline.sprite.SpriteData`
- `program.bootstrap.shaderpipeline.sprite.SpriteHandle`
- `program.bootstrap.shaderpipeline.sprite.SpriteInstance`
- `program.bootstrap.shaderpipeline.ubo.UBOHandle`
- `program.bootstrap.shaderpipeline.ubo.UBOInstance`
- `program.bootstrap.shaderpipeline.ubomanager.UBOManager`
- `program.core.engine.ManagerPackage`
- `program.core.util.RegistryUtility`
- `program.core.util.mathematics.vectors.Vector2`
- `program.core.util.mathematics.vectors.Vector4`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void get()` — Returns current state/value.
- `protected void dispose()` — Releases owned resources and unregisters state.
- `package void addSpriteHandle(String spriteName, SpriteHandle handle)` — Registers a child object into manager-owned collections.
- `public void request(String spriteName)` — Triggers on-demand loading or lookup.
- `public boolean hasSprite(String spriteName)` — Boolean existence/availability check.
- `public int getSpriteIDFromSpriteName(String spriteName)` — Returns current state/value.
- `public SpriteHandle getSpriteHandleFromSpriteID(int spriteID)` — Returns current state/value.
- `public SpriteHandle getSpriteHandleFromSpriteName(String spriteName)` — Returns current state/value.
- `public SpriteInstance cloneSprite(String spriteName)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/shaderpipeline/sprite/SpriteHandle.java`, `core/src/program/bootstrap/shaderpipeline/sprite/SpriteInstance.java`.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/texture/TextureArrayStruct.java`

**Type:** `class TextureArrayStruct`
  
**Inheritance/implements:** `extends StructPackage`
  
**Package:** `program.bootstrap.shaderpipeline.texture`
  
**File size:** 98 lines

**What this class does:** `TextureArrayStruct` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.texture`.

**Who this class talks to (direct imports):**
- `program.core.engine.StructPackage`

**Method intent:**
- `package public TextureArrayStruct(int id, String name, int atlasPixelSize, TextureAtlasStruct[] textureArray)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void registerFoundAlias(int aliasId)` — Performs class-specific logic; see call sites and owning manager flow.
- `public IntSet getFoundAliasIds()` — Returns current state/value.
- `public void registerTile(TextureTileStruct tile)` — Performs class-specific logic; see call sites and owning manager flow.
- `public Object2ObjectOpenHashMap<String, TextureTileStruct> getTileCoordinateMap()` — Returns current state/value.
- `public void clearAtlases()` — Performs class-specific logic; see call sites and owning manager flow.
- `public BufferedImage[] getRawImageArray()` — Returns current state/value.
- `public int getID()` — Returns current state/value.
- `public String getName()` — Returns current state/value.
- `public int getAtlasPixelSize()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/texture/TextureAtlasStruct.java`

**Type:** `class TextureAtlasStruct`
  
**Inheritance/implements:** `extends StructPackage`
  
**Package:** `program.bootstrap.shaderpipeline.texture`
  
**File size:** 40 lines

**What this class does:** `TextureAtlasStruct` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.texture`.

**Who this class talks to (direct imports):**
- `program.core.engine.StructPackage`

**Method intent:**
- `package public TextureAtlasStruct(int atlasSize, BufferedImage atlas)` — Performs class-specific logic; see call sites and owning manager flow.
- `package void clearImage()` — Performs class-specific logic; see call sites and owning manager flow.
- `public int getAtlasSize()` — Returns current state/value.
- `package BufferedImage getAtlas()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/texture/TextureData.java`

**Type:** `class TextureData`
  
**Inheritance/implements:** `extends DataPackage`
  
**Package:** `program.bootstrap.shaderpipeline.texture`
  
**File size:** 110 lines

**What this class does:** `TextureData` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.texture`.

**Who this class talks to (direct imports):**
- `program.core.engine.DataPackage`

**Method intent:**
- `package public TextureData(String tileName, int tileID, int arrayID, String arrayName, int gpuHandle, int atlasPixelSize, int tileWidth, int tileHeight, float u0, float v0, float u1, float v1)` — Performs class-specific logic; see call sites and owning manager flow.
- `public String getTileName()` — Returns current state/value.
- `public int getTileID()` — Returns current state/value.
- `public int getArrayID()` — Returns current state/value.
- `public String getArrayName()` — Returns current state/value.
- `public int getGpuHandle()` — Returns current state/value.
- `public int getAtlasPixelSize()` — Returns current state/value.
- `public int getTileWidth()` — Returns current state/value.
- `public int getTileHeight()` — Returns current state/value.
- `public float getU0()` — Returns current state/value.
- `public float getV0()` — Returns current state/value.
- `public float getU1()` — Returns current state/value.
- `public float getV1()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/texture/TextureHandle.java`

**Type:** `class TextureHandle`
  
**Inheritance/implements:** `extends HandlePackage`
  
**Package:** `program.bootstrap.shaderpipeline.texture`
  
**File size:** 75 lines

**What this class does:** `TextureHandle` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.texture`.

**Who this class talks to (direct imports):**
- `program.core.engine.HandlePackage`

**Method intent:**
- `public void constructor(TextureData textureData)` — Engine-side initialization entrypoint invoked post-create.
- `public TextureData getTextureData()` — Returns current state/value.
- `public String getTileName()` — Returns current state/value.
- `public int getTileID()` — Returns current state/value.
- `public int getArrayID()` — Returns current state/value.
- `public String getArrayName()` — Returns current state/value.
- `public int getGpuHandle()` — Returns current state/value.
- `public int getAtlasPixelSize()` — Returns current state/value.
- `public int getTileWidth()` — Returns current state/value.
- `public int getTileHeight()` — Returns current state/value.
- `public float getU0()` — Returns current state/value.
- `public float getV0()` — Returns current state/value.
- `public float getU1()` — Returns current state/value.
- `public float getV1()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/texture/TextureTileStruct.java`

**Type:** `class TextureTileStruct`
  
**Inheritance/implements:** `extends AtlasTileData`
  
**Package:** `program.bootstrap.shaderpipeline.texture`
  
**File size:** 76 lines

**What this class does:** `TextureTileStruct` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.texture`.

**Who this class talks to (direct imports):**
- `program.core.util.atlas.AtlasTileData`

**Method intent:**
- `package public TextureTileStruct(int id, String name, String atlas, int aliasCount)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void setImage(BufferedImage image, int layer)` — Mutates internal state for this object.
- `public BufferedImage getImage(int layer)` — Returns current state/value.
- `public void clearImages()` — Performs class-specific logic; see call sites and owning manager flow.
- `public int getID()` — Returns current state/value.
- `public String getName()` — Returns current state/value.
- `package String getAtlas()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/texturemanager/AliasLibrarySystem.java`

**Type:** `class AliasLibrarySystem`
  
**Inheritance/implements:** `extends SystemPackage`
  
**Package:** `program.bootstrap.shaderpipeline.texturemanager`
  
**File size:** 139 lines

**What this class does:** `AliasLibrarySystem` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.texturemanager`.

**Who this class talks to (direct imports):**
- `program.core.engine.SystemPackage`
- `program.core.settings.EngineSetting`
- `program.core.util.FileUtility`
- `program.core.util.JsonUtility`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `public void loadAliases()` — Parses external data into engine objects.
- `private void loadAliasFile(File file)` — Parses external data into engine objects.
- `private void ensureCapacity(int requiredCapacity)` — Performs class-specific logic; see call sites and owning manager flow.
- `public AliasStruct[] getAllAliases()` — Returns current state/value.
- `public int getAliasCount()` — Returns current state/value.
- `public int get(String aliasVariation)` — Returns current state/value.
- `public int getOrDefault(String aliasVariation)` — Returns current state/value.
- `public AliasStruct getAlias(int aliasId)` — Returns current state/value.
- `public Color getDefaultColor(int id)` — Returns current state/value.
- `public String getUniformName(int aliasId)` — Returns current state/value.
- `public boolean hasAlias(String aliasVariation)` — Boolean existence/availability check.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/texturemanager/AliasStruct.java`

**Type:** `class AliasStruct`
  
**Inheritance/implements:** `extends StructPackage`
  
**Package:** `program.bootstrap.shaderpipeline.texturemanager`
  
**File size:** 40 lines

**What this class does:** `AliasStruct` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.texturemanager`.

**Who this class talks to (direct imports):**
- `program.core.engine.StructPackage`

**Method intent:**
- `package public AliasStruct(String aliasType, Color defaultColor, String uniformName)` — Performs class-specific logic; see call sites and owning manager flow.
- `package String getAliasType()` — Returns current state/value.
- `public Color getAliasColor()` — Returns current state/value.
- `public String getUniformName()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/texturemanager/GLSLUtility.java`

**Type:** `class GLSLUtility`
  
**Inheritance/implements:** `extends UtilityPackage`
  
**Package:** `program.bootstrap.shaderpipeline.texturemanager`
  
**File size:** 96 lines

**What this class does:** `GLSLUtility` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.texturemanager`.

**Who this class talks to (direct imports):**
- `program.core.app.CoreContext`
- `program.core.util.graphics.gl.GL20`
- `program.core.util.graphics.gl.GL30`
- `program.core.util.image.Pixmap`
- `program.core.engine.UtilityPackage`
- `program.core.settings.EngineSetting`
- `program.core.util.PixmapUtility`

**Method intent:**
- `package int pushTextureArray(BufferedImage[] layers)` — Queues data for downstream systems (often render queues). Called via static reference from: `core/src/program/bootstrap/shaderpipeline/texturemanager/InternalLoader.java`.
- `package void deleteTextureArray(int handle)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/shaderpipeline/texturemanager/TextureManager.java`.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/texturemanager/InternalBuilder.java`

**Type:** `class InternalBuilder`
  
**Inheritance/implements:** `extends BuilderPackage`
  
**Package:** `program.bootstrap.shaderpipeline.texturemanager`
  
**File size:** 187 lines

**What this class does:** `InternalBuilder` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.texturemanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.shaderpipeline.texture.TextureArrayStruct`
- `program.bootstrap.shaderpipeline.texture.TextureAtlasStruct`
- `program.bootstrap.shaderpipeline.texture.TextureTileStruct`
- `program.core.engine.BuilderPackage`
- `program.core.util.AtlasUtility`
- `program.core.util.FileUtility`
- `program.core.util.RegistryUtility`

**Method intent:**
- `protected void get()` — Returns current state/value.
- `package TextureArrayStruct build(List<File> imageFiles, File sourceDirectory, String arrayName)` — Constructs derived runtime/handle data from source input.
- `private LinkedHashMap<String, TextureTileStruct> createTextureTiles(List<File> imageFiles, File sourceDirectory, String arrayName)` — Allocates/initializes child systems or resources.
- `private LinkedHashMap<String, TextureTileStruct> organizeTextureTiles(LinkedHashMap<String, TextureTileStruct> tileMap)` — Performs class-specific logic; see call sites and owning manager flow.
- `private TextureAtlasStruct[] compositeAtlasLayers(ObjectArrayList<TextureTileStruct> tiles, int atlasPixelSize)` — Performs class-specific logic; see call sites and owning manager flow.
- `private TextureArrayStruct createTextureArray(LinkedHashMap<String, TextureTileStruct> tileMap, String arrayName, int atlasPixelSize, TextureAtlasStruct[] atlasLayers)` — Allocates/initializes child systems or resources.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/texturemanager/InternalLoader.java`

**Type:** `class InternalLoader`
  
**Inheritance/implements:** `extends LoaderPackage`
  
**Package:** `program.bootstrap.shaderpipeline.texturemanager`
  
**File size:** 165 lines

**What this class does:** `InternalLoader` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.texturemanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.shaderpipeline.texture.TextureArrayStruct`
- `program.bootstrap.shaderpipeline.texture.TextureTileStruct`
- `program.bootstrap.shaderpipeline.ubo.UBOHandle`
- `program.bootstrap.shaderpipeline.ubomanager.UBOManager`
- `program.core.engine.LoaderPackage`
- `program.core.settings.EngineSetting`
- `program.core.util.FileUtility`
- `program.core.util.mathematics.vectors.Vector2`

**Method intent:**
- `protected void scan()` — Discovers files/resources for later load.
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void get()` — Returns current state/value.
- `protected void awake()` — Runs startup-time runtime activation work.
- `protected void load(File directory)` — Parses external data into engine objects.
- `package void request(String arrayName)` — Triggers on-demand loading or lookup.
- `private void seedUBO(String arrayName, TextureArrayStruct arrayStruct)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void pushToGPU(TextureArrayStruct arrayStruct)` — Queues data for downstream systems (often render queues).
- `private void clearHeapImages(TextureArrayStruct arrayStruct)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/texturemanager/TextureManager.java`

**Type:** `class TextureManager`
  
**Inheritance/implements:** `extends ManagerPackage`
  
**Package:** `program.bootstrap.shaderpipeline.texturemanager`
  
**File size:** 153 lines

**What this class does:** `TextureManager` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.texturemanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.shaderpipeline.texture.TextureArrayStruct`
- `program.bootstrap.shaderpipeline.texture.TextureData`
- `program.bootstrap.shaderpipeline.texture.TextureHandle`
- `program.bootstrap.shaderpipeline.texture.TextureTileStruct`
- `program.core.engine.ManagerPackage`
- `program.core.util.RegistryUtility`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void dispose()` — Releases owned resources and unregisters state.
- `package void registerTile(TextureTileStruct tile, float u0, float v0, float u1, float v1, TextureArrayStruct array, int gpuHandle)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void request(String arrayName)` — Triggers on-demand loading or lookup.
- `public boolean hasTexture(String textureName)` — Boolean existence/availability check.
- `public int getTileIDFromTextureName(String textureName)` — Returns current state/value.
- `public TextureHandle getTextureHandleFromTileID(int tileID)` — Returns current state/value.
- `public TextureHandle getTextureHandleFromTextureName(String textureName)` — Returns current state/value.
- `public boolean hasArray(String arrayName)` — Boolean existence/availability check.
- `public int getArrayIDFromArrayName(String arrayName)` — Returns current state/value.
- `public TextureHandle getTextureHandleFromArrayID(int arrayID)` — Returns current state/value.
- `public TextureHandle getTextureHandleFromArrayName(String arrayName)` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/ubo/UBOData.java`

**Type:** `class UBOData`
  
**Inheritance/implements:** `extends DataPackage`
  
**Package:** `program.bootstrap.shaderpipeline.ubo`
  
**File size:** 146 lines

**What this class does:** `UBOData` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.ubo`.

**Who this class talks to (direct imports):**
- `program.core.util.memory.BufferUtils`
- `program.bootstrap.shaderpipeline.uniforms.UniformData`
- `program.bootstrap.shaderpipeline.uniforms.UniformStruct`
- `program.core.engine.DataPackage`
- `program.core.settings.EngineSetting`

**Method intent:**
- `package public UBOData(String blockName, int requestedBinding)` — Performs class-specific logic; see call sites and owning manager flow.
- `package public UBOData(UBOData source, int newGpuHandle)` — Performs class-specific logic; see call sites and owning manager flow.
- `package void addUniformDeclaration(UniformData uniform)` — Registers a child object into manager-owned collections.
- `package void initRuntime(int bufferID, int gpuHandle, int bindingPoint, int totalSizeBytes)` — Performs class-specific logic; see call sites and owning manager flow.
- `package void addCompiledUniform(String name, UniformStruct<?> uniform)` — Registers a child object into manager-owned collections.
- `package void updateUniform(String name, Object value)` — Runs frame-step maintenance and logic.
- `public ByteBuffer getStagingBuffer()` — Returns current state/value.
- `public String getBlockName()` — Returns current state/value.
- `public int getRequestedBinding()` — Returns current state/value.
- `public ObjectArrayList<UniformData> getUniformDeclarations()` — Returns current state/value.
- `public int getBufferID()` — Returns current state/value.
- `public int getGpuHandle()` — Returns current state/value.
- `public int getBindingPoint()` — Returns current state/value.
- `public int getTotalSizeBytes()` — Returns current state/value.
- `public ObjectArrayList<String> getUniformKeys()` — Returns current state/value.
- `public UniformStruct<?> getCompiledUniform(String name)` — Returns current state/value.
- `public Object2ObjectOpenHashMap<String, UniformStruct<?>> getCompiledUniforms()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/ubo/UBOHandle.java`

**Type:** `class UBOHandle`
  
**Inheritance/implements:** `extends HandlePackage`
  
**Package:** `program.bootstrap.shaderpipeline.ubo`
  
**File size:** 94 lines

**What this class does:** `UBOHandle` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.ubo`.

**Who this class talks to (direct imports):**
- `program.bootstrap.shaderpipeline.uniforms.UniformData`
- `program.bootstrap.shaderpipeline.uniforms.UniformStruct`
- `program.core.engine.HandlePackage`

**Method intent:**
- `public void constructor(UBOData uboData)` — Engine-side initialization entrypoint invoked post-create.
- `public void addUniformDeclaration(UniformData uniform)` — Registers a child object into manager-owned collections.
- `public void initRuntime(int bufferID, int gpuHandle, int bindingPoint, int totalSizeBytes)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void addCompiledUniform(String name, UniformStruct<?> uniform)` — Registers a child object into manager-owned collections.
- `public void updateUniform(String name, Object value)` — Runs frame-step maintenance and logic.
- `public UBOData getUBOData()` — Returns current state/value.
- `public String getBlockName()` — Returns current state/value.
- `public int getRequestedBinding()` — Returns current state/value.
- `public ObjectArrayList<UniformData> getUniformDeclarations()` — Returns current state/value.
- `public int getBufferID()` — Returns current state/value.
- `public int getGpuHandle()` — Returns current state/value.
- `public int getBindingPoint()` — Returns current state/value.
- `public int getTotalSizeBytes()` — Returns current state/value.
- `public ObjectArrayList<String> getUniformKeys()` — Returns current state/value.
- `public UniformStruct<?> getCompiledUniform(String name)` — Returns current state/value.
- `public Object2ObjectOpenHashMap<String, UniformStruct<?>> getCompiledUniforms()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/ubo/UBOInstance.java`

**Type:** `class UBOInstance`
  
**Inheritance/implements:** `extends InstancePackage`
  
**Package:** `program.bootstrap.shaderpipeline.ubo`
  
**File size:** 68 lines

**What this class does:** `UBOInstance` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.ubo`.

**Who this class talks to (direct imports):**
- `program.bootstrap.shaderpipeline.uniforms.UniformStruct`
- `program.core.engine.InstancePackage`

**Method intent:**
- `public void constructor(UBOData uboData)` — Engine-side initialization entrypoint invoked post-create.
- `public void addCompiledUniform(String name, UniformStruct<?> uniform)` — Registers a child object into manager-owned collections.
- `public void updateUniform(String name, Object value)` — Runs frame-step maintenance and logic.
- `public UBOData getUBOData()` — Returns current state/value.
- `public String getBlockName()` — Returns current state/value.
- `public int getGpuHandle()` — Returns current state/value.
- `public int getBindingPoint()` — Returns current state/value.
- `public int getTotalSizeBytes()` — Returns current state/value.
- `public ObjectArrayList<String> getUniformKeys()` — Returns current state/value.
- `public Object2ObjectOpenHashMap<String, UniformStruct<?>> getCompiledUniforms()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/ubomanager/GLSLUtility.java`

**Type:** `class GLSLUtility`
  
**Inheritance/implements:** `extends UtilityPackage`
  
**Package:** `program.bootstrap.shaderpipeline.ubomanager`
  
**File size:** 50 lines

**What this class does:** `GLSLUtility` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.ubomanager`.

**Who this class talks to (direct imports):**
- `program.core.app.CoreContext`
- `program.core.util.graphics.gl.GL20`
- `program.core.util.graphics.gl.GL30`
- `program.core.util.memory.BufferUtils`
- `program.core.engine.UtilityPackage`

**Method intent:**
- `package int createUniformBuffer()` — Allocates/initializes child systems or resources. Called via static reference from: `core/src/program/bootstrap/shaderpipeline/ubomanager/UBOManager.java`.
- `package void allocateUniformBuffer(int buffer, int sizeBytes)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/shaderpipeline/ubomanager/UBOManager.java`.
- `package void bindUniformBufferBase(int buffer, int bindingPoint)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/shaderpipeline/ubomanager/UBOManager.java`.
- `package void updateUniformBuffer(int buffer, int offset, ByteBuffer data)` — Runs frame-step maintenance and logic. Called via static reference from: `core/src/program/bootstrap/shaderpipeline/ubomanager/UBOManager.java`.
- `package void deleteUniformBuffer(int buffer)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/shaderpipeline/ubomanager/UBOManager.java`.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/ubomanager/InternalBuilder.java`

**Type:** `class InternalBuilder`
  
**Inheritance/implements:** `extends BuilderPackage`
  
**Package:** `program.bootstrap.shaderpipeline.ubomanager`
  
**File size:** 89 lines

**What this class does:** `InternalBuilder` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.ubomanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.shaderpipeline.ubo.UBOData`
- `program.bootstrap.shaderpipeline.ubo.UBOHandle`
- `program.bootstrap.shaderpipeline.uniforms.UniformData`
- `program.bootstrap.shaderpipeline.uniforms.UniformType`
- `program.core.engine.BuilderPackage`
- `program.core.util.JsonUtility`

**Method intent:**
- `protected void get()` — Returns current state/value.
- `package UBOHandle parse(File file)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void parseUniforms(JsonObject json, UBOHandle handle, String blockName)` — Performs class-specific logic; see call sites and owning manager flow.
- `private UniformType parseUniformType(String blockName, String uniformName, String raw)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/ubomanager/InternalLoader.java`

**Type:** `class InternalLoader`
  
**Inheritance/implements:** `extends LoaderPackage`
  
**Package:** `program.bootstrap.shaderpipeline.ubomanager`
  
**File size:** 79 lines

**What this class does:** `InternalLoader` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.ubomanager`.

**Who this class talks to (direct imports):**
- `program.core.engine.LoaderPackage`
- `program.core.settings.EngineSetting`
- `program.core.util.FileUtility`

**Method intent:**
- `protected void scan()` — Discovers files/resources for later load.
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void get()` — Returns current state/value.
- `protected void load(File file)` — Parses external data into engine objects.
- `package void request(String blockName)` — Triggers on-demand loading or lookup.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/ubomanager/UBOManager.java`

**Type:** `class UBOManager`
  
**Inheritance/implements:** `extends ManagerPackage`
  
**Package:** `program.bootstrap.shaderpipeline.ubomanager`
  
**File size:** 248 lines

**What this class does:** `UBOManager` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.ubomanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.shaderpipeline.ubo.UBOData`
- `program.bootstrap.shaderpipeline.ubo.UBOHandle`
- `program.bootstrap.shaderpipeline.ubo.UBOInstance`
- `program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformData`
- `program.bootstrap.shaderpipeline.uniforms.UniformStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformUtility`
- `program.core.engine.ManagerPackage`
- `program.core.util.RegistryUtility`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void dispose()` — Releases owned resources and unregisters state.
- `package void buildBuffer(UBOHandle handle)` — Constructs derived runtime/handle data from source input. Called via static reference from: `core/src/program/bootstrap/shaderpipeline/ubo/UBOData.java`.
- `public UBOInstance createUBOInstance(UBOHandle handle)` — Allocates/initializes child systems or resources. Called via static reference from: `core/src/program/bootstrap/shaderpipeline/ubo/UBOHandle.java`, `core/src/program/bootstrap/shaderpipeline/ubo/UBOInstance.java`.
- `public void destroyInstance(UBOInstance instance)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/shaderpipeline/ubo/UBOInstance.java`.
- `public void push(UBOHandle handle)` — Queues data for downstream systems (often render queues).
- `public void push(UBOInstance instance)` — Queues data for downstream systems (often render queues).
- `private int resolveBinding(int requestedBinding, String blockName)` — Performs class-specific logic; see call sites and owning manager flow.
- `private int allocateBindingPoint()` — Performs class-specific logic; see call sites and owning manager flow.
- `private void releaseBindingPoint(int binding)` — Performs class-specific logic; see call sites and owning manager flow.
- `private int computeStd140BufferSize(ObjectArrayList<UniformData> uniformDeclarations)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void populateUniforms(UBOHandle handle)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void request(String blockName)` — Triggers on-demand loading or lookup.
- `public boolean hasUBO(String uboName)` — Boolean existence/availability check.
- `public int getUBOIDFromUBOName(String uboName)` — Returns current state/value.
- `public UBOHandle getUBOHandleFromUBOID(int uboID)` — Returns current state/value.
- `public UBOHandle getUBOHandleFromUBOName(String uboName)` — Returns current state/value.
- `public UBOHandle findUBOHandle(String blockName)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/UniformAttributeStruct.java`

**Type:** `class UniformAttributeStruct`
  
**Inheritance/implements:** `<T> extends StructPackage`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms`
  
**File size:** 127 lines

**What this class does:** `UniformAttributeStruct` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms`.

**Who this class talks to (direct imports):**
- `program.core.util.memory.BufferUtils`
- `program.core.engine.StructPackage`

**Method intent:**
- `package protected UniformAttributeStruct(UniformType type, T defaultValue)` — Performs class-specific logic; see call sites and owning manager flow.
- `package protected UniformAttributeStruct(UniformType type, int count, T defaultValue)` — Performs class-specific logic; see call sites and owning manager flow.
- `private int computeUBOBufferSize(UniformType type, int count)` — Performs class-specific logic; see call sites and owning manager flow.
- `public final ByteBuffer getByteBuffer()` — Returns current state/value.
- `protected final void writeToBuffer(ByteBuffer buffer, T value)` — Performs class-specific logic; see call sites and owning manager flow.
- `public boolean isSampler()` — Performs class-specific logic; see call sites and owning manager flow.
- `public void bindTexture(int unit)` — Performs class-specific logic; see call sites and owning manager flow.
- `package final void push(int handle)` — Queues data for downstream systems (often render queues).
- `public UniformAttributeStruct<T> clone()` — Performs class-specific logic; see call sites and owning manager flow.
- `public final void set(T value)` — Mutates internal state for this object.
- `public final void setObject(Object value)` — Mutates internal state for this object.
- `protected void applyObject(Object value)` — Performs class-specific logic; see call sites and owning manager flow.
- `public T getValue()` — Returns current state/value.
- `public UniformType getUniformType()` — Returns current state/value.
- `public int getCount()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/UniformData.java`

**Type:** `class UniformData`
  
**Inheritance/implements:** `extends DataPackage`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms`
  
**File size:** 48 lines

**What this class does:** `UniformData` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms`.

**Who this class talks to (direct imports):**
- `program.core.engine.DataPackage`

**Method intent:**
- `package public UniformData(UniformType uniformType, String uniformName)` — Performs class-specific logic; see call sites and owning manager flow.
- `package public UniformData(UniformType uniformType, String uniformName, int count)` — Performs class-specific logic; see call sites and owning manager flow.
- `public UniformType getUniformType()` — Returns current state/value.
- `public String getUniformName()` — Returns current state/value.
- `public int getCount()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/UniformStruct.java`

**Type:** `class UniformStruct`
  
**Inheritance/implements:** `<T> extends StructPackage`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms`
  
**File size:** 58 lines

**What this class does:** `UniformStruct` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms`.

**Who this class talks to (direct imports):**
- `program.core.engine.StructPackage`

**Method intent:**
- `package public UniformStruct(int uniformHandle, UniformAttributeStruct<T> attribute)` — Performs class-specific logic; see call sites and owning manager flow.
- `package public UniformStruct(int uniformHandle, int offset, UniformAttributeStruct<T> attribute)` — Performs class-specific logic; see call sites and owning manager flow.
- `public final void push()` — Queues data for downstream systems (often render queues).
- `public UniformStruct<T> clone()` — Performs class-specific logic; see call sites and owning manager flow.
- `public int getUniformHandle()` — Returns current state/value.
- `public int getOffset()` — Returns current state/value.
- `public UniformAttributeStruct<T> attribute()` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/UniformType.java`

**Type:** `enum UniformType`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms`
  
**File size:** 262 lines

**What this class does:** `UniformType` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms`.

**Method intent:**
- `package  FLOAT("float", 4, 4)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void writeElement(ByteBuffer buffer, Object value)` — Performs class-specific logic; see call sites and owning manager flow.
- `package  DOUBLE("double", 8, 8)` — Performs class-specific logic; see call sites and owning manager flow.
- `package  INT("int", 4, 4)` — Performs class-specific logic; see call sites and owning manager flow.
- `package  BOOL("bool", 4, 4)` — Performs class-specific logic; see call sites and owning manager flow.
- `package  VECTOR2("vec2", 8, 8)` — Performs class-specific logic; see call sites and owning manager flow.
- `package  VECTOR3("vec3", 16, 12)` — Performs class-specific logic; see call sites and owning manager flow.
- `package  VECTOR4("vec4", 16, 16)` — Performs class-specific logic; see call sites and owning manager flow.
- `package  VECTOR2_DOUBLE("dvec2", 16, 16)` — Performs class-specific logic; see call sites and owning manager flow.
- `package  VECTOR3_DOUBLE("dvec3", 32, 24)` — Performs class-specific logic; see call sites and owning manager flow.
- `package  VECTOR4_DOUBLE("dvec4", 32, 32)` — Performs class-specific logic; see call sites and owning manager flow.
- `package  VECTOR2_INT("ivec2", 8, 8)` — Performs class-specific logic; see call sites and owning manager flow.
- `package  VECTOR3_INT("ivec3", 16, 12)` — Performs class-specific logic; see call sites and owning manager flow.
- `package  VECTOR4_INT("ivec4", 16, 16)` — Performs class-specific logic; see call sites and owning manager flow.
- `package  VECTOR2_BOOLEAN("bvec2", 8, 8)` — Performs class-specific logic; see call sites and owning manager flow.
- `package  VECTOR3_BOOLEAN("bvec3", 16, 12)` — Performs class-specific logic; see call sites and owning manager flow.
- `package  VECTOR4_BOOLEAN("bvec4", 16, 16)` — Performs class-specific logic; see call sites and owning manager flow.
- `package  MATRIX2("mat2", 16, 32)` — Performs class-specific logic; see call sites and owning manager flow.
- `package  MATRIX3("mat3", 16, 48)` — Performs class-specific logic; see call sites and owning manager flow.
- `package  MATRIX4("mat4", 16, 64)` — Performs class-specific logic; see call sites and owning manager flow.
- `package  MATRIX2_DOUBLE("dmat2", 32, 64)` — Performs class-specific logic; see call sites and owning manager flow.
- `package  MATRIX3_DOUBLE("dmat3", 32, 96)` — Performs class-specific logic; see call sites and owning manager flow.
- `package  MATRIX4_DOUBLE("dmat4", 32, 128)` — Performs class-specific logic; see call sites and owning manager flow.
- `package  SAMPLE_IMAGE_2D("sampler2D", 4, 4)` — Performs class-specific logic; see call sites and owning manager flow.
- `package  SAMPLE_IMAGE_2D_ARRAY("sampler2DArray", 4, 4)` — Performs class-specific logic; see call sites and owning manager flow.
- `package  UniformType(String glslName, int std140Alignment, int std140Size)` — Performs class-specific logic; see call sites and owning manager flow.
- `public String getGLSLName()` — Returns current state/value.
- `public int getStd140Alignment()` — Returns current state/value.
- `public int getStd140Size()` — Returns current state/value.
- `public UniformType fromString(String glslName)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/shaderpipeline/shadermanager/InternalBuilder.java`.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/UniformUtility.java`

**Type:** `class UniformUtility`
  
**Inheritance/implements:** `extends UtilityPackage`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms`
  
**File size:** 444 lines

**What this class does:** `UniformUtility` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms`.

**Who this class talks to (direct imports):**
- `program.core.engine.UtilityPackage`

**Method intent:**
- `package private UniformUtility()` — Performs class-specific logic; see call sites and owning manager flow.
- `public int align(int offset, int alignment)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/shaderpipeline/ubomanager/UBOManager.java`, `core/src/program/bootstrap/shaderpipeline/uniforms/UniformAttributeStruct.java`.
- `public int getStd140Alignment(UniformData ud)` — Returns current state/value. Called via static reference from: `core/src/program/bootstrap/shaderpipeline/ubomanager/UBOManager.java`.
- `public int getStd140Size(UniformData ud)` — Returns current state/value. Called via static reference from: `core/src/program/bootstrap/shaderpipeline/ubomanager/UBOManager.java`.
- `public UniformAttributeStruct<?> createUniformAttribute(UniformData ud)` — Allocates/initializes child systems or resources. Called via static reference from: `core/src/program/bootstrap/shaderpipeline/shadermanager/InternalLoader.java`, `core/src/program/bootstrap/shaderpipeline/ubomanager/UBOManager.java`.
- `public void applyFromJsonObject(UniformAttributeStruct<?> attribute, String uniformName, JsonObject uniformData)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/shaderpipeline/materialmanager/InternalBuilder.java`.
- `public void applySingle(UniformAttributeStruct<?> attribute, String type, JsonElement value)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void applyArray(UniformAttributeStruct<?> attribute, String type, JsonElement valueElement)` — Performs class-specific logic; see call sites and owning manager flow.
- `public Vector2 parseVector2(JsonArray array)` — Performs class-specific logic; see call sites and owning manager flow.
- `public Vector2Double parseVector2Double(JsonArray array)` — Performs class-specific logic; see call sites and owning manager flow.
- `public Vector2Int parseVector2Int(JsonArray array)` — Performs class-specific logic; see call sites and owning manager flow.
- `public Vector2Boolean parseVector2Boolean(JsonArray array)` — Performs class-specific logic; see call sites and owning manager flow.
- `public Vector3 parseVector3(JsonArray array)` — Performs class-specific logic; see call sites and owning manager flow.
- `public Vector3Double parseVector3Double(JsonArray array)` — Performs class-specific logic; see call sites and owning manager flow.
- `public Vector3Int parseVector3Int(JsonArray array)` — Performs class-specific logic; see call sites and owning manager flow.
- `public Vector3Boolean parseVector3Boolean(JsonArray array)` — Performs class-specific logic; see call sites and owning manager flow.
- `public Vector4 parseVector4(JsonArray array)` — Performs class-specific logic; see call sites and owning manager flow.
- `public Vector4Double parseVector4Double(JsonArray array)` — Performs class-specific logic; see call sites and owning manager flow.
- `public Vector4Int parseVector4Int(JsonArray array)` — Performs class-specific logic; see call sites and owning manager flow.
- `public Vector4Boolean parseVector4Boolean(JsonArray array)` — Performs class-specific logic; see call sites and owning manager flow.
- `public Matrix2 parseMatrix2(JsonArray array)` — Performs class-specific logic; see call sites and owning manager flow.
- `public Matrix3 parseMatrix3(JsonArray array)` — Performs class-specific logic; see call sites and owning manager flow.
- `public Matrix4 parseMatrix4(JsonArray array)` — Performs class-specific logic; see call sites and owning manager flow.
- `public Matrix2Double parseMatrix2Double(JsonArray array)` — Performs class-specific logic; see call sites and owning manager flow.
- `public Matrix3Double parseMatrix3Double(JsonArray array)` — Performs class-specific logic; see call sites and owning manager flow.
- `public Matrix4Double parseMatrix4Double(JsonArray array)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/matrices/Matrix2DoubleUniform.java`

**Type:** `class Matrix2DoubleUniform`
  
**Inheritance/implements:** `extends UniformAttributeStruct<Matrix2Double>`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms.matrices`
  
**File size:** 40 lines

**What this class does:** `Matrix2DoubleUniform` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms.matrices`.

**Who this class talks to (direct imports):**
- `program.core.app.CoreContext`
- `program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformType`
- `program.core.util.mathematics.matrices.Matrix2Double`

**Method intent:**
- `package public Matrix2DoubleUniform()` — Performs class-specific logic; see call sites and owning manager flow.
- `public UniformAttributeStruct<?> createDefault()` — Allocates/initializes child systems or resources.
- `protected void push(int handle, Matrix2Double value)` — Queues data for downstream systems (often render queues).
- `protected void applyValue(Matrix2Double value)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/matrices/Matrix2Uniform.java`

**Type:** `class Matrix2Uniform`
  
**Inheritance/implements:** `extends UniformAttributeStruct<Matrix2>`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms.matrices`
  
**File size:** 40 lines

**What this class does:** `Matrix2Uniform` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms.matrices`.

**Who this class talks to (direct imports):**
- `program.core.app.CoreContext`
- `program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformType`
- `program.core.util.mathematics.matrices.Matrix2`

**Method intent:**
- `package public Matrix2Uniform()` — Performs class-specific logic; see call sites and owning manager flow.
- `public UniformAttributeStruct<?> createDefault()` — Allocates/initializes child systems or resources.
- `protected void push(int handle, Matrix2 value)` — Queues data for downstream systems (often render queues).
- `protected void applyValue(Matrix2 value)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/matrices/Matrix3DoubleUniform.java`

**Type:** `class Matrix3DoubleUniform`
  
**Inheritance/implements:** `extends UniformAttributeStruct<Matrix3Double>`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms.matrices`
  
**File size:** 39 lines

**What this class does:** `Matrix3DoubleUniform` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms.matrices`.

**Who this class talks to (direct imports):**
- `program.core.app.CoreContext`
- `program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformType`
- `program.core.util.mathematics.matrices.Matrix3`
- `program.core.util.mathematics.matrices.Matrix3Double`

**Method intent:**
- `package public Matrix3DoubleUniform()` — Performs class-specific logic; see call sites and owning manager flow.
- `public UniformAttributeStruct<?> createDefault()` — Allocates/initializes child systems or resources.
- `protected void push(int handle, Matrix3Double value)` — Queues data for downstream systems (often render queues).
- `protected void applyValue(Matrix3Double value)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/matrices/Matrix3Uniform.java`

**Type:** `class Matrix3Uniform`
  
**Inheritance/implements:** `extends UniformAttributeStruct<Object>`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms.matrices`
  
**File size:** 44 lines

**What this class does:** `Matrix3Uniform` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms.matrices`.

**Who this class talks to (direct imports):**
- `program.core.app.CoreContext`
- `program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformType`
- `program.core.util.mathematics.matrices.Matrix3`

**Method intent:**
- `package public Matrix3Uniform()` — Performs class-specific logic; see call sites and owning manager flow.
- `public UniformAttributeStruct<?> createDefault()` — Allocates/initializes child systems or resources.
- `protected void push(int handle, Object value)` — Queues data for downstream systems (often render queues).
- `protected void applyValue(Object value)` — Performs class-specific logic; see call sites and owning manager flow.
- `protected void applyObject(Object value)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/matrices/Matrix4DoubleUniform.java`

**Type:** `class Matrix4DoubleUniform`
  
**Inheritance/implements:** `extends UniformAttributeStruct<Matrix4Double>`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms.matrices`
  
**File size:** 39 lines

**What this class does:** `Matrix4DoubleUniform` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms.matrices`.

**Who this class talks to (direct imports):**
- `program.core.app.CoreContext`
- `program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformType`
- `program.core.util.mathematics.matrices.Matrix4`
- `program.core.util.mathematics.matrices.Matrix4Double`

**Method intent:**
- `package public Matrix4DoubleUniform()` — Performs class-specific logic; see call sites and owning manager flow.
- `public UniformAttributeStruct<?> createDefault()` — Allocates/initializes child systems or resources.
- `protected void push(int handle, Matrix4Double value)` — Queues data for downstream systems (often render queues).
- `protected void applyValue(Matrix4Double value)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/matrices/Matrix4Uniform.java`

**Type:** `class Matrix4Uniform`
  
**Inheritance/implements:** `extends UniformAttributeStruct<Object>`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms.matrices`
  
**File size:** 44 lines

**What this class does:** `Matrix4Uniform` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms.matrices`.

**Who this class talks to (direct imports):**
- `program.core.app.CoreContext`
- `program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformType`
- `program.core.util.mathematics.matrices.Matrix4`

**Method intent:**
- `package public Matrix4Uniform()` — Performs class-specific logic; see call sites and owning manager flow.
- `public UniformAttributeStruct<?> createDefault()` — Allocates/initializes child systems or resources.
- `protected void push(int handle, Object value)` — Queues data for downstream systems (often render queues).
- `protected void applyValue(Object value)` — Performs class-specific logic; see call sites and owning manager flow.
- `protected void applyObject(Object value)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/matrixArrays/Matrix2ArrayUniform.java`

**Type:** `class Matrix2ArrayUniform`
  
**Inheritance/implements:** `extends UniformAttributeStruct<Object[]>`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms.matrixArrays`
  
**File size:** 54 lines

**What this class does:** `Matrix2ArrayUniform` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms.matrixArrays`.

**Who this class talks to (direct imports):**
- `program.core.app.CoreContext`
- `program.core.util.memory.BufferUtils`
- `program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformType`
- `program.core.util.mathematics.matrices.Matrix2`

**Method intent:**
- `package public Matrix2ArrayUniform(int elementCount)` — Performs class-specific logic; see call sites and owning manager flow.
- `public UniformAttributeStruct<?> createDefault()` — Allocates/initializes child systems or resources.
- `protected void push(int handle, Object[] value)` — Queues data for downstream systems (often render queues).
- `protected void applyValue(Object[] value)` — Performs class-specific logic; see call sites and owning manager flow.
- `public int elementCount()` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/matrixArrays/Matrix2DoubleArrayUniform.java`

**Type:** `class Matrix2DoubleArrayUniform`
  
**Inheritance/implements:** `extends UniformAttributeStruct<Object[]>`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms.matrixArrays`
  
**File size:** 54 lines

**What this class does:** `Matrix2DoubleArrayUniform` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms.matrixArrays`.

**Who this class talks to (direct imports):**
- `program.core.app.CoreContext`
- `program.core.util.memory.BufferUtils`
- `program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformType`
- `program.core.util.mathematics.matrices.Matrix2Double`

**Method intent:**
- `package public Matrix2DoubleArrayUniform(int elementCount)` — Performs class-specific logic; see call sites and owning manager flow.
- `public UniformAttributeStruct<?> createDefault()` — Allocates/initializes child systems or resources.
- `protected void push(int handle, Object[] value)` — Queues data for downstream systems (often render queues).
- `protected void applyValue(Object[] value)` — Performs class-specific logic; see call sites and owning manager flow.
- `public int elementCount()` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/matrixArrays/Matrix3ArrayUniform.java`

**Type:** `class Matrix3ArrayUniform`
  
**Inheritance/implements:** `extends UniformAttributeStruct<Object[]>`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms.matrixArrays`
  
**File size:** 57 lines

**What this class does:** `Matrix3ArrayUniform` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms.matrixArrays`.

**Who this class talks to (direct imports):**
- `program.core.app.CoreContext`
- `program.core.util.memory.BufferUtils`
- `program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformType`
- `program.core.util.mathematics.matrices.Matrix3`

**Method intent:**
- `package public Matrix3ArrayUniform(int elementCount)` — Performs class-specific logic; see call sites and owning manager flow.
- `public UniformAttributeStruct<?> createDefault()` — Allocates/initializes child systems or resources.
- `protected void push(int handle, Object[] value)` — Queues data for downstream systems (often render queues).
- `protected void applyValue(Object[] value)` — Performs class-specific logic; see call sites and owning manager flow.
- `protected void applyObject(Object value)` — Performs class-specific logic; see call sites and owning manager flow.
- `public int elementCount()` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/matrixArrays/Matrix3DoubleArrayUniform.java`

**Type:** `class Matrix3DoubleArrayUniform`
  
**Inheritance/implements:** `extends UniformAttributeStruct<Object[]>`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms.matrixArrays`
  
**File size:** 52 lines

**What this class does:** `Matrix3DoubleArrayUniform` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms.matrixArrays`.

**Who this class talks to (direct imports):**
- `program.core.app.CoreContext`
- `program.core.util.memory.BufferUtils`
- `program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformType`
- `program.core.util.mathematics.matrices.Matrix3Double`

**Method intent:**
- `package public Matrix3DoubleArrayUniform(int elementCount)` — Performs class-specific logic; see call sites and owning manager flow.
- `public UniformAttributeStruct<?> createDefault()` — Allocates/initializes child systems or resources.
- `protected void push(int handle, Object[] value)` — Queues data for downstream systems (often render queues).
- `protected void applyValue(Object[] value)` — Performs class-specific logic; see call sites and owning manager flow.
- `public int elementCount()` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/matrixArrays/Matrix4ArrayUniform.java`

**Type:** `class Matrix4ArrayUniform`
  
**Inheritance/implements:** `extends UniformAttributeStruct<Object[]>`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms.matrixArrays`
  
**File size:** 57 lines

**What this class does:** `Matrix4ArrayUniform` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms.matrixArrays`.

**Who this class talks to (direct imports):**
- `program.core.app.CoreContext`
- `program.core.util.memory.BufferUtils`
- `program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformType`
- `program.core.util.mathematics.matrices.Matrix4`

**Method intent:**
- `package public Matrix4ArrayUniform(int elementCount)` — Performs class-specific logic; see call sites and owning manager flow.
- `public UniformAttributeStruct<?> createDefault()` — Allocates/initializes child systems or resources.
- `protected void push(int handle, Object[] value)` — Queues data for downstream systems (often render queues).
- `protected void applyValue(Object[] value)` — Performs class-specific logic; see call sites and owning manager flow.
- `protected void applyObject(Object value)` — Performs class-specific logic; see call sites and owning manager flow.
- `public int elementCount()` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/matrixArrays/Matrix4DoubleArrayUniform.java`

**Type:** `class Matrix4DoubleArrayUniform`
  
**Inheritance/implements:** `extends UniformAttributeStruct<Object[]>`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms.matrixArrays`
  
**File size:** 52 lines

**What this class does:** `Matrix4DoubleArrayUniform` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms.matrixArrays`.

**Who this class talks to (direct imports):**
- `program.core.app.CoreContext`
- `program.core.util.memory.BufferUtils`
- `program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformType`
- `program.core.util.mathematics.matrices.Matrix4Double`

**Method intent:**
- `package public Matrix4DoubleArrayUniform(int elementCount)` — Performs class-specific logic; see call sites and owning manager flow.
- `public UniformAttributeStruct<?> createDefault()` — Allocates/initializes child systems or resources.
- `protected void push(int handle, Object[] value)` — Queues data for downstream systems (often render queues).
- `protected void applyValue(Object[] value)` — Performs class-specific logic; see call sites and owning manager flow.
- `public int elementCount()` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/samplers/SampleImage2DArrayUniform.java`

**Type:** `class SampleImage2DArrayUniform`
  
**Inheritance/implements:** `extends UniformAttributeStruct<Integer>`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms.samplers`
  
**File size:** 44 lines

**What this class does:** `SampleImage2DArrayUniform` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms.samplers`.

**Who this class talks to (direct imports):**
- `program.core.app.CoreContext`
- `program.core.util.graphics.gl.GL20`
- `program.core.util.graphics.gl.GL30`
- `program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformType`

**Method intent:**
- `package public SampleImage2DArrayUniform()` — Performs class-specific logic; see call sites and owning manager flow.
- `public UniformAttributeStruct<?> createDefault()` — Allocates/initializes child systems or resources.
- `public boolean isSampler()` — Performs class-specific logic; see call sites and owning manager flow.
- `public void bindTexture(int unit)` — Performs class-specific logic; see call sites and owning manager flow.
- `protected void push(int handle, Integer value)` — Queues data for downstream systems (often render queues).
- `protected void applyValue(Integer value)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/samplers/SampleImage2DUniform.java`

**Type:** `class SampleImage2DUniform`
  
**Inheritance/implements:** `extends UniformAttributeStruct<Integer>`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms.samplers`
  
**File size:** 43 lines

**What this class does:** `SampleImage2DUniform` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms.samplers`.

**Who this class talks to (direct imports):**
- `program.core.app.CoreContext`
- `program.core.util.graphics.gl.GL20`
- `program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformType`

**Method intent:**
- `package public SampleImage2DUniform()` — Performs class-specific logic; see call sites and owning manager flow.
- `public UniformAttributeStruct<?> createDefault()` — Allocates/initializes child systems or resources.
- `public boolean isSampler()` — Performs class-specific logic; see call sites and owning manager flow.
- `public void bindTexture(int unit)` — Performs class-specific logic; see call sites and owning manager flow.
- `protected void push(int handle, Integer value)` — Queues data for downstream systems (often render queues).
- `protected void applyValue(Integer value)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/scalarArrays/BooleanArrayUniform.java`

**Type:** `class BooleanArrayUniform`
  
**Inheritance/implements:** `extends UniformAttributeStruct<Object[]>`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms.scalarArrays`
  
**File size:** 48 lines

**What this class does:** `BooleanArrayUniform` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms.scalarArrays`.

**Who this class talks to (direct imports):**
- `program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformType`

**Method intent:**
- `package public BooleanArrayUniform(int elementCount)` — Performs class-specific logic; see call sites and owning manager flow.
- `public UniformAttributeStruct<?> createDefault()` — Allocates/initializes child systems or resources.
- `protected void push(int handle, Object[] value)` — Queues data for downstream systems (often render queues).
- `protected void applyValue(Object[] value)` — Performs class-specific logic; see call sites and owning manager flow.
- `public int elementCount()` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/scalarArrays/DoubleArrayUniform.java`

**Type:** `class DoubleArrayUniform`
  
**Inheritance/implements:** `extends UniformAttributeStruct<Object[]>`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms.scalarArrays`
  
**File size:** 48 lines

**What this class does:** `DoubleArrayUniform` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms.scalarArrays`.

**Who this class talks to (direct imports):**
- `program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformType`

**Method intent:**
- `package public DoubleArrayUniform(int elementCount)` — Performs class-specific logic; see call sites and owning manager flow.
- `public UniformAttributeStruct<?> createDefault()` — Allocates/initializes child systems or resources.
- `protected void push(int handle, Object[] value)` — Queues data for downstream systems (often render queues).
- `protected void applyValue(Object[] value)` — Performs class-specific logic; see call sites and owning manager flow.
- `public int elementCount()` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/scalarArrays/FloatArrayUniform.java`

**Type:** `class FloatArrayUniform`
  
**Inheritance/implements:** `extends UniformAttributeStruct<Object[]>`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms.scalarArrays`
  
**File size:** 48 lines

**What this class does:** `FloatArrayUniform` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms.scalarArrays`.

**Who this class talks to (direct imports):**
- `program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformType`

**Method intent:**
- `package public FloatArrayUniform(int elementCount)` — Performs class-specific logic; see call sites and owning manager flow.
- `public UniformAttributeStruct<?> createDefault()` — Allocates/initializes child systems or resources.
- `protected void push(int handle, Object[] value)` — Queues data for downstream systems (often render queues).
- `protected void applyValue(Object[] value)` — Performs class-specific logic; see call sites and owning manager flow.
- `public int elementCount()` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/scalarArrays/IntegerArrayUniform.java`

**Type:** `class IntegerArrayUniform`
  
**Inheritance/implements:** `extends UniformAttributeStruct<Object[]>`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms.scalarArrays`
  
**File size:** 48 lines

**What this class does:** `IntegerArrayUniform` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms.scalarArrays`.

**Who this class talks to (direct imports):**
- `program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformType`

**Method intent:**
- `package public IntegerArrayUniform(int elementCount)` — Performs class-specific logic; see call sites and owning manager flow.
- `public UniformAttributeStruct<?> createDefault()` — Allocates/initializes child systems or resources.
- `protected void push(int handle, Object[] value)` — Queues data for downstream systems (often render queues).
- `protected void applyValue(Object[] value)` — Performs class-specific logic; see call sites and owning manager flow.
- `public int elementCount()` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/scalars/BooleanUniform.java`

**Type:** `class BooleanUniform`
  
**Inheritance/implements:** `extends UniformAttributeStruct<Boolean>`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms.scalars`
  
**File size:** 28 lines

**What this class does:** `BooleanUniform` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms.scalars`.

**Who this class talks to (direct imports):**
- `program.core.app.CoreContext`
- `program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformType`

**Method intent:**
- `package public BooleanUniform()` — Performs class-specific logic; see call sites and owning manager flow.
- `public UniformAttributeStruct<?> createDefault()` — Allocates/initializes child systems or resources.
- `protected void push(int handle, Boolean value)` — Queues data for downstream systems (often render queues).
- `protected void applyValue(Boolean value)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/scalars/DoubleUniform.java`

**Type:** `class DoubleUniform`
  
**Inheritance/implements:** `extends UniformAttributeStruct<Double>`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms.scalars`
  
**File size:** 28 lines

**What this class does:** `DoubleUniform` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms.scalars`.

**Who this class talks to (direct imports):**
- `program.core.app.CoreContext`
- `program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformType`

**Method intent:**
- `package public DoubleUniform()` — Performs class-specific logic; see call sites and owning manager flow.
- `public UniformAttributeStruct<?> createDefault()` — Allocates/initializes child systems or resources.
- `protected void push(int handle, Double value)` — Queues data for downstream systems (often render queues).
- `protected void applyValue(Double value)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/scalars/FloatUniform.java`

**Type:** `class FloatUniform`
  
**Inheritance/implements:** `extends UniformAttributeStruct<Float>`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms.scalars`
  
**File size:** 28 lines

**What this class does:** `FloatUniform` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms.scalars`.

**Who this class talks to (direct imports):**
- `program.core.app.CoreContext`
- `program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformType`

**Method intent:**
- `package public FloatUniform()` — Performs class-specific logic; see call sites and owning manager flow.
- `public UniformAttributeStruct<?> createDefault()` — Allocates/initializes child systems or resources.
- `protected void push(int handle, Float value)` — Queues data for downstream systems (often render queues).
- `protected void applyValue(Float value)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/scalars/IntegerUniform.java`

**Type:** `class IntegerUniform`
  
**Inheritance/implements:** `extends UniformAttributeStruct<Integer>`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms.scalars`
  
**File size:** 28 lines

**What this class does:** `IntegerUniform` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms.scalars`.

**Who this class talks to (direct imports):**
- `program.core.app.CoreContext`
- `program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformType`

**Method intent:**
- `package public IntegerUniform()` — Performs class-specific logic; see call sites and owning manager flow.
- `public UniformAttributeStruct<?> createDefault()` — Allocates/initializes child systems or resources.
- `protected void push(int handle, Integer value)` — Queues data for downstream systems (often render queues).
- `protected void applyValue(Integer value)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/vectorarrays/Vector2ArrayUniform.java`

**Type:** `class Vector2ArrayUniform`
  
**Inheritance/implements:** `extends UniformAttributeStruct<Object[]>`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms.vectorarrays`
  
**File size:** 66 lines

**What this class does:** `Vector2ArrayUniform` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms.vectorarrays`.

**Who this class talks to (direct imports):**
- `program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformType`
- `program.core.util.mathematics.vectors.Vector2`

**Method intent:**
- `package public Vector2ArrayUniform(int elementCount)` — Performs class-specific logic; see call sites and owning manager flow.
- `public UniformAttributeStruct<?> createDefault()` — Allocates/initializes child systems or resources.
- `protected void push(int handle, Object[] value)` — Queues data for downstream systems (often render queues).
- `protected void applyValue(Object[] value)` — Performs class-specific logic; see call sites and owning manager flow.
- `protected void applyObject(Object value)` — Performs class-specific logic; see call sites and owning manager flow.
- `public int elementCount()` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/vectorarrays/Vector2BooleanArrayUniform.java`

**Type:** `class Vector2BooleanArrayUniform`
  
**Inheritance/implements:** `extends UniformAttributeStruct<Object[]>`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms.vectorarrays`
  
**File size:** 52 lines

**What this class does:** `Vector2BooleanArrayUniform` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms.vectorarrays`.

**Who this class talks to (direct imports):**
- `program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformType`
- `program.core.util.mathematics.vectors.Vector2Boolean`

**Method intent:**
- `package public Vector2BooleanArrayUniform(int elementCount)` — Performs class-specific logic; see call sites and owning manager flow.
- `public UniformAttributeStruct<?> createDefault()` — Allocates/initializes child systems or resources.
- `protected void push(int handle, Object[] value)` — Queues data for downstream systems (often render queues).
- `protected void applyValue(Object[] value)` — Performs class-specific logic; see call sites and owning manager flow.
- `public int elementCount()` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/vectorarrays/Vector2DoubleArrayUniform.java`

**Type:** `class Vector2DoubleArrayUniform`
  
**Inheritance/implements:** `extends UniformAttributeStruct<Object[]>`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms.vectorarrays`
  
**File size:** 52 lines

**What this class does:** `Vector2DoubleArrayUniform` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms.vectorarrays`.

**Who this class talks to (direct imports):**
- `program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformType`
- `program.core.util.mathematics.vectors.Vector2Double`

**Method intent:**
- `package public Vector2DoubleArrayUniform(int elementCount)` — Performs class-specific logic; see call sites and owning manager flow.
- `public UniformAttributeStruct<?> createDefault()` — Allocates/initializes child systems or resources.
- `protected void push(int handle, Object[] value)` — Queues data for downstream systems (often render queues).
- `protected void applyValue(Object[] value)` — Performs class-specific logic; see call sites and owning manager flow.
- `public int elementCount()` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/vectorarrays/Vector2IntArrayUniform.java`

**Type:** `class Vector2IntArrayUniform`
  
**Inheritance/implements:** `extends UniformAttributeStruct<Object[]>`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms.vectorarrays`
  
**File size:** 52 lines

**What this class does:** `Vector2IntArrayUniform` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms.vectorarrays`.

**Who this class talks to (direct imports):**
- `program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformType`
- `program.core.util.mathematics.vectors.Vector2Int`

**Method intent:**
- `package public Vector2IntArrayUniform(int elementCount)` — Performs class-specific logic; see call sites and owning manager flow.
- `public UniformAttributeStruct<?> createDefault()` — Allocates/initializes child systems or resources.
- `protected void push(int handle, Object[] value)` — Queues data for downstream systems (often render queues).
- `protected void applyValue(Object[] value)` — Performs class-specific logic; see call sites and owning manager flow.
- `public int elementCount()` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/vectorarrays/Vector3ArrayUniform.java`

**Type:** `class Vector3ArrayUniform`
  
**Inheritance/implements:** `extends UniformAttributeStruct<Object[]>`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms.vectorarrays`
  
**File size:** 68 lines

**What this class does:** `Vector3ArrayUniform` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms.vectorarrays`.

**Who this class talks to (direct imports):**
- `program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformType`
- `program.core.util.mathematics.vectors.Vector3`

**Method intent:**
- `package public Vector3ArrayUniform(int elementCount)` — Performs class-specific logic; see call sites and owning manager flow.
- `public UniformAttributeStruct<?> createDefault()` — Allocates/initializes child systems or resources.
- `protected void push(int handle, Object[] value)` — Queues data for downstream systems (often render queues).
- `protected void applyValue(Object[] value)` — Performs class-specific logic; see call sites and owning manager flow.
- `protected void applyObject(Object value)` — Performs class-specific logic; see call sites and owning manager flow.
- `public int elementCount()` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/vectorarrays/Vector3BooleanArrayUniform.java`

**Type:** `class Vector3BooleanArrayUniform`
  
**Inheritance/implements:** `extends UniformAttributeStruct<Object[]>`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms.vectorarrays`
  
**File size:** 53 lines

**What this class does:** `Vector3BooleanArrayUniform` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms.vectorarrays`.

**Who this class talks to (direct imports):**
- `program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformType`
- `program.core.util.mathematics.vectors.Vector3Boolean`

**Method intent:**
- `package public Vector3BooleanArrayUniform(int elementCount)` — Performs class-specific logic; see call sites and owning manager flow.
- `public UniformAttributeStruct<?> createDefault()` — Allocates/initializes child systems or resources.
- `protected void push(int handle, Object[] value)` — Queues data for downstream systems (often render queues).
- `protected void applyValue(Object[] value)` — Performs class-specific logic; see call sites and owning manager flow.
- `public int elementCount()` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/vectorarrays/Vector3DoubleArrayUniform.java`

**Type:** `class Vector3DoubleArrayUniform`
  
**Inheritance/implements:** `extends UniformAttributeStruct<Object[]>`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms.vectorarrays`
  
**File size:** 53 lines

**What this class does:** `Vector3DoubleArrayUniform` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms.vectorarrays`.

**Who this class talks to (direct imports):**
- `program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformType`
- `program.core.util.mathematics.vectors.Vector3Double`

**Method intent:**
- `package public Vector3DoubleArrayUniform(int elementCount)` — Performs class-specific logic; see call sites and owning manager flow.
- `public UniformAttributeStruct<?> createDefault()` — Allocates/initializes child systems or resources.
- `protected void push(int handle, Object[] value)` — Queues data for downstream systems (often render queues).
- `protected void applyValue(Object[] value)` — Performs class-specific logic; see call sites and owning manager flow.
- `public int elementCount()` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/vectorarrays/Vector3IntArrayUniform.java`

**Type:** `class Vector3IntArrayUniform`
  
**Inheritance/implements:** `extends UniformAttributeStruct<Object[]>`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms.vectorarrays`
  
**File size:** 53 lines

**What this class does:** `Vector3IntArrayUniform` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms.vectorarrays`.

**Who this class talks to (direct imports):**
- `program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformType`
- `program.core.util.mathematics.vectors.Vector3Int`

**Method intent:**
- `package public Vector3IntArrayUniform(int elementCount)` — Performs class-specific logic; see call sites and owning manager flow.
- `public UniformAttributeStruct<?> createDefault()` — Allocates/initializes child systems or resources.
- `protected void push(int handle, Object[] value)` — Queues data for downstream systems (often render queues).
- `protected void applyValue(Object[] value)` — Performs class-specific logic; see call sites and owning manager flow.
- `public int elementCount()` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/vectorarrays/Vector4ArrayUniform.java`

**Type:** `class Vector4ArrayUniform`
  
**Inheritance/implements:** `extends UniformAttributeStruct<Object[]>`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms.vectorarrays`
  
**File size:** 70 lines

**What this class does:** `Vector4ArrayUniform` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms.vectorarrays`.

**Who this class talks to (direct imports):**
- `program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformType`
- `program.core.util.mathematics.vectors.Vector4`

**Method intent:**
- `package public Vector4ArrayUniform(int elementCount)` — Performs class-specific logic; see call sites and owning manager flow.
- `public UniformAttributeStruct<?> createDefault()` — Allocates/initializes child systems or resources.
- `protected void push(int handle, Object[] value)` — Queues data for downstream systems (often render queues).
- `protected void applyValue(Object[] value)` — Performs class-specific logic; see call sites and owning manager flow.
- `protected void applyObject(Object value)` — Performs class-specific logic; see call sites and owning manager flow.
- `public int elementCount()` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/vectorarrays/Vector4BooleanArrayUniform.java`

**Type:** `class Vector4BooleanArrayUniform`
  
**Inheritance/implements:** `extends UniformAttributeStruct<Object[]>`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms.vectorarrays`
  
**File size:** 54 lines

**What this class does:** `Vector4BooleanArrayUniform` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms.vectorarrays`.

**Who this class talks to (direct imports):**
- `program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformType`
- `program.core.util.mathematics.vectors.Vector4Boolean`

**Method intent:**
- `package public Vector4BooleanArrayUniform(int elementCount)` — Performs class-specific logic; see call sites and owning manager flow.
- `public UniformAttributeStruct<?> createDefault()` — Allocates/initializes child systems or resources.
- `protected void push(int handle, Object[] value)` — Queues data for downstream systems (often render queues).
- `protected void applyValue(Object[] value)` — Performs class-specific logic; see call sites and owning manager flow.
- `public int elementCount()` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/vectorarrays/Vector4DoubleArrayUniform.java`

**Type:** `class Vector4DoubleArrayUniform`
  
**Inheritance/implements:** `extends UniformAttributeStruct<Object[]>`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms.vectorarrays`
  
**File size:** 54 lines

**What this class does:** `Vector4DoubleArrayUniform` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms.vectorarrays`.

**Who this class talks to (direct imports):**
- `program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformType`
- `program.core.util.mathematics.vectors.Vector4Double`

**Method intent:**
- `package public Vector4DoubleArrayUniform(int elementCount)` — Performs class-specific logic; see call sites and owning manager flow.
- `public UniformAttributeStruct<?> createDefault()` — Allocates/initializes child systems or resources.
- `protected void push(int handle, Object[] value)` — Queues data for downstream systems (often render queues).
- `protected void applyValue(Object[] value)` — Performs class-specific logic; see call sites and owning manager flow.
- `public int elementCount()` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/vectorarrays/Vector4IntArrayUniform.java`

**Type:** `class Vector4IntArrayUniform`
  
**Inheritance/implements:** `extends UniformAttributeStruct<Object[]>`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms.vectorarrays`
  
**File size:** 54 lines

**What this class does:** `Vector4IntArrayUniform` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms.vectorarrays`.

**Who this class talks to (direct imports):**
- `program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformType`
- `program.core.util.mathematics.vectors.Vector4Int`

**Method intent:**
- `package public Vector4IntArrayUniform(int elementCount)` — Performs class-specific logic; see call sites and owning manager flow.
- `public UniformAttributeStruct<?> createDefault()` — Allocates/initializes child systems or resources.
- `protected void push(int handle, Object[] value)` — Queues data for downstream systems (often render queues).
- `protected void applyValue(Object[] value)` — Performs class-specific logic; see call sites and owning manager flow.
- `public int elementCount()` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/vectors/Vector2BooleanUniform.java`

**Type:** `class Vector2BooleanUniform`
  
**Inheritance/implements:** `extends UniformAttributeStruct<Vector2Boolean>`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms.vectors`
  
**File size:** 29 lines

**What this class does:** `Vector2BooleanUniform` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms.vectors`.

**Who this class talks to (direct imports):**
- `program.core.app.CoreContext`
- `program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformType`
- `program.core.util.mathematics.vectors.Vector2Boolean`

**Method intent:**
- `package public Vector2BooleanUniform()` — Performs class-specific logic; see call sites and owning manager flow.
- `public UniformAttributeStruct<?> createDefault()` — Allocates/initializes child systems or resources.
- `protected void push(int handle, Vector2Boolean value)` — Queues data for downstream systems (often render queues).
- `protected void applyValue(Vector2Boolean value)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/vectors/Vector2DoubleUniform.java`

**Type:** `class Vector2DoubleUniform`
  
**Inheritance/implements:** `extends UniformAttributeStruct<Vector2Double>`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms.vectors`
  
**File size:** 29 lines

**What this class does:** `Vector2DoubleUniform` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms.vectors`.

**Who this class talks to (direct imports):**
- `program.core.app.CoreContext`
- `program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformType`
- `program.core.util.mathematics.vectors.Vector2Double`

**Method intent:**
- `package public Vector2DoubleUniform()` — Performs class-specific logic; see call sites and owning manager flow.
- `public UniformAttributeStruct<?> createDefault()` — Allocates/initializes child systems or resources.
- `protected void push(int handle, Vector2Double value)` — Queues data for downstream systems (often render queues).
- `protected void applyValue(Vector2Double value)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/vectors/Vector2IntUniform.java`

**Type:** `class Vector2IntUniform`
  
**Inheritance/implements:** `extends UniformAttributeStruct<Vector2Int>`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms.vectors`
  
**File size:** 29 lines

**What this class does:** `Vector2IntUniform` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms.vectors`.

**Who this class talks to (direct imports):**
- `program.core.app.CoreContext`
- `program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformType`
- `program.core.util.mathematics.vectors.Vector2Int`

**Method intent:**
- `package public Vector2IntUniform()` — Performs class-specific logic; see call sites and owning manager flow.
- `public UniformAttributeStruct<?> createDefault()` — Allocates/initializes child systems or resources.
- `protected void push(int handle, Vector2Int value)` — Queues data for downstream systems (often render queues).
- `protected void applyValue(Vector2Int value)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/vectors/Vector2Uniform.java`

**Type:** `class Vector2Uniform`
  
**Inheritance/implements:** `extends UniformAttributeStruct<Object>`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms.vectors`
  
**File size:** 35 lines

**What this class does:** `Vector2Uniform` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms.vectors`.

**Who this class talks to (direct imports):**
- `program.core.app.CoreContext`
- `program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformType`
- `program.core.util.mathematics.vectors.Vector2`

**Method intent:**
- `package public Vector2Uniform()` — Performs class-specific logic; see call sites and owning manager flow.
- `public UniformAttributeStruct<?> createDefault()` — Allocates/initializes child systems or resources.
- `protected void push(int handle, Object value)` — Queues data for downstream systems (often render queues).
- `protected void applyValue(Object value)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/vectors/Vector3BooleanUniform.java`

**Type:** `class Vector3BooleanUniform`
  
**Inheritance/implements:** `extends UniformAttributeStruct<Vector3Boolean>`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms.vectors`
  
**File size:** 29 lines

**What this class does:** `Vector3BooleanUniform` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms.vectors`.

**Who this class talks to (direct imports):**
- `program.core.app.CoreContext`
- `program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformType`
- `program.core.util.mathematics.vectors.Vector3Boolean`

**Method intent:**
- `package public Vector3BooleanUniform()` — Performs class-specific logic; see call sites and owning manager flow.
- `public UniformAttributeStruct<?> createDefault()` — Allocates/initializes child systems or resources.
- `protected void push(int handle, Vector3Boolean value)` — Queues data for downstream systems (often render queues).
- `protected void applyValue(Vector3Boolean value)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/vectors/Vector3DoubleUniform.java`

**Type:** `class Vector3DoubleUniform`
  
**Inheritance/implements:** `extends UniformAttributeStruct<Vector3Double>`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms.vectors`
  
**File size:** 29 lines

**What this class does:** `Vector3DoubleUniform` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms.vectors`.

**Who this class talks to (direct imports):**
- `program.core.app.CoreContext`
- `program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformType`
- `program.core.util.mathematics.vectors.Vector3Double`

**Method intent:**
- `package public Vector3DoubleUniform()` — Performs class-specific logic; see call sites and owning manager flow.
- `public UniformAttributeStruct<?> createDefault()` — Allocates/initializes child systems or resources.
- `protected void push(int handle, Vector3Double value)` — Queues data for downstream systems (often render queues).
- `protected void applyValue(Vector3Double value)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/vectors/Vector3IntUniform.java`

**Type:** `class Vector3IntUniform`
  
**Inheritance/implements:** `extends UniformAttributeStruct<Vector3Int>`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms.vectors`
  
**File size:** 29 lines

**What this class does:** `Vector3IntUniform` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms.vectors`.

**Who this class talks to (direct imports):**
- `program.core.app.CoreContext`
- `program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformType`
- `program.core.util.mathematics.vectors.Vector3Int`

**Method intent:**
- `package public Vector3IntUniform()` — Performs class-specific logic; see call sites and owning manager flow.
- `public UniformAttributeStruct<?> createDefault()` — Allocates/initializes child systems or resources.
- `protected void push(int handle, Vector3Int value)` — Queues data for downstream systems (often render queues).
- `protected void applyValue(Vector3Int value)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/vectors/Vector3Uniform.java`

**Type:** `class Vector3Uniform`
  
**Inheritance/implements:** `extends UniformAttributeStruct<Object>`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms.vectors`
  
**File size:** 35 lines

**What this class does:** `Vector3Uniform` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms.vectors`.

**Who this class talks to (direct imports):**
- `program.core.app.CoreContext`
- `program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformType`
- `program.core.util.mathematics.vectors.Vector3`

**Method intent:**
- `package public Vector3Uniform()` — Performs class-specific logic; see call sites and owning manager flow.
- `public UniformAttributeStruct<?> createDefault()` — Allocates/initializes child systems or resources.
- `protected void push(int handle, Object value)` — Queues data for downstream systems (often render queues).
- `protected void applyValue(Object value)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/vectors/Vector4BooleanUniform.java`

**Type:** `class Vector4BooleanUniform`
  
**Inheritance/implements:** `extends UniformAttributeStruct<Vector4Boolean>`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms.vectors`
  
**File size:** 29 lines

**What this class does:** `Vector4BooleanUniform` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms.vectors`.

**Who this class talks to (direct imports):**
- `program.core.app.CoreContext`
- `program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformType`
- `program.core.util.mathematics.vectors.Vector4Boolean`

**Method intent:**
- `package public Vector4BooleanUniform()` — Performs class-specific logic; see call sites and owning manager flow.
- `public UniformAttributeStruct<?> createDefault()` — Allocates/initializes child systems or resources.
- `protected void push(int handle, Vector4Boolean value)` — Queues data for downstream systems (often render queues).
- `protected void applyValue(Vector4Boolean value)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/vectors/Vector4DoubleUniform.java`

**Type:** `class Vector4DoubleUniform`
  
**Inheritance/implements:** `extends UniformAttributeStruct<Vector4Double>`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms.vectors`
  
**File size:** 29 lines

**What this class does:** `Vector4DoubleUniform` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms.vectors`.

**Who this class talks to (direct imports):**
- `program.core.app.CoreContext`
- `program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformType`
- `program.core.util.mathematics.vectors.Vector4Double`

**Method intent:**
- `package public Vector4DoubleUniform()` — Performs class-specific logic; see call sites and owning manager flow.
- `public UniformAttributeStruct<?> createDefault()` — Allocates/initializes child systems or resources.
- `protected void push(int handle, Vector4Double value)` — Queues data for downstream systems (often render queues).
- `protected void applyValue(Vector4Double value)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/vectors/Vector4IntUniform.java`

**Type:** `class Vector4IntUniform`
  
**Inheritance/implements:** `extends UniformAttributeStruct<Vector4Int>`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms.vectors`
  
**File size:** 29 lines

**What this class does:** `Vector4IntUniform` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms.vectors`.

**Who this class talks to (direct imports):**
- `program.core.app.CoreContext`
- `program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformType`
- `program.core.util.mathematics.vectors.Vector4Int`

**Method intent:**
- `package public Vector4IntUniform()` — Performs class-specific logic; see call sites and owning manager flow.
- `public UniformAttributeStruct<?> createDefault()` — Allocates/initializes child systems or resources.
- `protected void push(int handle, Vector4Int value)` — Queues data for downstream systems (often render queues).
- `protected void applyValue(Vector4Int value)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/vectors/Vector4Uniform.java`

**Type:** `class Vector4Uniform`
  
**Inheritance/implements:** `extends UniformAttributeStruct<Object>`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms.vectors`
  
**File size:** 35 lines

**What this class does:** `Vector4Uniform` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms.vectors`.

**Who this class talks to (direct imports):**
- `program.core.app.CoreContext`
- `program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformType`
- `program.core.util.mathematics.vectors.Vector4`

**Method intent:**
- `package public Vector4Uniform()` — Performs class-specific logic; see call sites and owning manager flow.
- `public UniformAttributeStruct<?> createDefault()` — Allocates/initializes child systems or resources.
- `protected void push(int handle, Object value)` — Queues data for downstream systems (often render queues).
- `protected void applyValue(Object value)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.
