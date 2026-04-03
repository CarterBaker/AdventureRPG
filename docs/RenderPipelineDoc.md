# RenderPipelineDoc

This is a human-readable subsystem manual. It explains responsibilities, collaboration points, and method intent for each class in scope.

Classes covered: **13**

## How to read this manual
- Start with the package flow notes at the top of each class section.
- Use **Who talks to this class** to identify collaborators.
- Use **Method intent** to understand lifecycle and API behavior.

## `core/src/program/bootstrap/renderpipeline/RenderPipeline.java`

**Type:** `class RenderPipeline`
  
**Inheritance/implements:** `extends PipelinePackage`
  
**Package:** `program.bootstrap.renderpipeline`
  
**File size:** 21 lines

**What this class does:** `RenderPipeline` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.renderpipeline`.

**Who this class talks to (direct imports):**
- `program.bootstrap.renderpipeline.cameramanager.CameraManager`
- `program.bootstrap.renderpipeline.rendermanager.RenderManager`
- `program.core.engine.PipelinePackage`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/renderpipeline/cameramanager/CameraBufferSystem.java`

**Type:** `class CameraBufferSystem`
  
**Inheritance/implements:** `extends SystemPackage`
  
**Package:** `program.bootstrap.renderpipeline.cameramanager`
  
**File size:** 82 lines

**What this class does:** `CameraBufferSystem` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.renderpipeline.cameramanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.shaderpipeline.ubo.UBOHandle`
- `program.bootstrap.shaderpipeline.ubomanager.UBOManager`
- `program.core.engine.SystemPackage`
- `program.core.kernel.window.WindowInstance`
- `program.core.settings.EngineSetting`
- `program.core.util.camera.CameraInstance`
- `program.core.util.camera.OrthographicCameraInstance`

**Method intent:**
- `protected void get()` — Returns current state/value.
- `protected void awake()` — Runs startup-time runtime activation work.
- `package void pushForWindow(WindowInstance window)` — Queues data for downstream systems (often render queues).
- `private void pushPerspective(WindowInstance window)` — Queues data for downstream systems (often render queues).
- `private void pushOrtho(WindowInstance window)` — Queues data for downstream systems (often render queues).

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/renderpipeline/cameramanager/CameraManager.java`

**Type:** `class CameraManager`
  
**Inheritance/implements:** `extends ManagerPackage`
  
**Package:** `program.bootstrap.renderpipeline.cameramanager`
  
**File size:** 63 lines

**What this class does:** `CameraManager` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.renderpipeline.cameramanager`.

**Who this class talks to (direct imports):**
- `program.core.engine.InstancePackage`
- `program.core.engine.ManagerPackage`
- `program.core.kernel.window.WindowInstance`
- `program.core.kernel.windowmanager.WindowManager`
- `program.core.util.camera.CameraData`
- `program.core.util.camera.CameraInstance`
- `program.core.util.camera.OrthographicCameraData`
- `program.core.util.camera.OrthographicCameraInstance`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void get()` — Returns current state/value.
- `public void pushCamera(WindowInstance window)` — Queues data for downstream systems (often render queues). Called via static reference from: `core/src/program/bootstrap/renderpipeline/cameramanager/CameraBufferSystem.java`.
- `public ObjectLinkedOpenHashSet<CameraInstance> getCameraInstances()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/renderpipeline/compositebatch/CompositeBatchStruct.java`

**Type:** `class CompositeBatchStruct`
  
**Inheritance/implements:** `extends StructPackage`
  
**Package:** `program.bootstrap.renderpipeline.compositebatch`
  
**File size:** 98 lines

**What this class does:** `CompositeBatchStruct` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.renderpipeline.compositebatch`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.compositebuffer.CompositeBufferInstance`
- `program.bootstrap.shaderpipeline.material.MaterialInstance`
- `program.bootstrap.shaderpipeline.ubo.UBOHandle`
- `program.bootstrap.shaderpipeline.uniforms.UniformStruct`
- `program.core.engine.StructPackage`

**Method intent:**
- `package public CompositeBatchStruct(MaterialInstance material)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void add(CompositeBufferInstance buffer)` — Registers a child object into manager-owned collections.
- `public void clear()` — Performs class-specific logic; see call sites and owning manager flow.
- `public boolean isEmpty()` — Performs class-specific logic; see call sites and owning manager flow.
- `public MaterialInstance getMaterial()` — Returns current state/value.
- `public ObjectArrayList<CompositeBufferInstance> getBuffers()` — Returns current state/value.
- `public UBOHandle[] getCachedSourceUBOs()` — Returns current state/value.
- `public UniformStruct<?>[] getCachedUniforms()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/renderpipeline/compositerendersystem/CompositeRenderSystem.java`

**Type:** `class CompositeRenderSystem`
  
**Inheritance/implements:** `extends SystemPackage`
  
**Package:** `program.bootstrap.renderpipeline.compositerendersystem`
  
**File size:** 271 lines

**What this class does:** `CompositeRenderSystem` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.renderpipeline.compositerendersystem`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.compositebuffer.CompositeBufferInstance`
- `program.bootstrap.renderpipeline.compositebatch.CompositeBatchStruct`
- `program.bootstrap.shaderpipeline.material.MaterialInstance`
- `program.bootstrap.shaderpipeline.ubo.UBOHandle`
- `program.bootstrap.shaderpipeline.uniforms.UniformStruct`
- `program.core.engine.SystemPackage`
- `program.core.kernel.window.WindowInstance`
- `program.core.settings.EngineSetting`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `public void submit(MaterialInstance material, CompositeBufferInstance buffer, WindowInstance window)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void draw(WindowInstance window)` — Flushes or submits rendering work.
- `private void drawBuffer(CompositeBufferInstance buffer, int windowID)` — Flushes or submits rendering work.
- `private void upload(CompositeBufferInstance buffer, WindowBufferGpuState gpuState)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void bindMaterial(CompositeBatchStruct batch)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void ensureUploadBuffer(int floatCount)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void ensureGpuObjects(CompositeBufferInstance buffer, WindowBufferGpuState gpuState)` — Performs class-specific logic; see call sites and owning manager flow.
- `private WindowCompositeState getOrCreateCompositeState(int windowID)` — Returns current state/value.
- `private WindowBufferGpuState getOrCreateGpuState(CompositeBufferInstance buffer, int windowID)` — Returns current state/value.
- `public void removeWindow(int windowID)` — Unregisters and tears down child references.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/renderpipeline/compositerendersystem/GLSLUtility.java`

**Type:** `class GLSLUtility`
  
**Inheritance/implements:** `extends UtilityPackage`
  
**Package:** `program.bootstrap.renderpipeline.compositerendersystem`
  
**File size:** 147 lines

**What this class does:** `GLSLUtility` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.renderpipeline.compositerendersystem`.

**Who this class talks to (direct imports):**
- `program.core.app.CoreContext`
- `program.core.util.graphics.gl.GL20`
- `program.core.util.graphics.gl.GL30`
- `program.core.engine.UtilityPackage`
- `program.core.settings.EngineSetting`

**Method intent:**
- `package void enableDepth()` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/renderpipeline/compositerendersystem/CompositeRenderSystem.java`, `core/src/program/bootstrap/renderpipeline/rendermanager/RenderSystem.java`.
- `package void updateInstanceVBO(int vbo, FloatBuffer data, int floatCount)` — Runs frame-step maintenance and logic. Called via static reference from: `core/src/program/bootstrap/renderpipeline/compositerendersystem/CompositeRenderSystem.java`.
- `package int createDynamicInstanceVBO(int maxInstances, int floatsPerInstance)` — Allocates/initializes child systems or resources. Called via static reference from: `core/src/program/bootstrap/geometrypipeline/compositebuffermanager/CompositeBufferManager.java`, `core/src/program/bootstrap/renderpipeline/compositerendersystem/CompositeRenderSystem.java`.
- `package int createInstancedVAO(int meshVBOHandle, int[] meshAttrSizes, int meshIBOHandle, int instanceVBOHandle, int[] instanceAttrSizes)` — Allocates/initializes child systems or resources. Called via static reference from: `core/src/program/bootstrap/geometrypipeline/compositebuffermanager/CompositeBufferManager.java`, `core/src/program/bootstrap/renderpipeline/compositerendersystem/CompositeRenderSystem.java`.
- `package void drawElementsInstanced(int vao, int indexCount, int instanceCount)` — Flushes or submits rendering work. Called via static reference from: `core/src/program/bootstrap/renderpipeline/compositerendersystem/CompositeRenderSystem.java`.
- `package void useShader(int shaderHandle)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/renderpipeline/compositerendersystem/CompositeRenderSystem.java`, `core/src/program/bootstrap/renderpipeline/rendermanager/RenderSystem.java`.
- `package void bindUniformBlock(int shaderProgram, String blockName, int bindingPoint)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/renderpipeline/compositerendersystem/CompositeRenderSystem.java`, `core/src/program/bootstrap/shaderpipeline/shadermanager/ShaderManager.java`.
- `package void bindUniformBuffer(int bindingPoint, int gpuHandle)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/renderpipeline/compositerendersystem/CompositeRenderSystem.java`, `core/src/program/bootstrap/renderpipeline/rendermanager/RenderSystem.java`.
- `package void deleteBuffer(int handle)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/geometrypipeline/compositebuffermanager/CompositeBufferManager.java`, `core/src/program/bootstrap/renderpipeline/compositerendersystem/CompositeRenderSystem.java`.
- `package void deleteVAO(int handle)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/geometrypipeline/compositebuffermanager/CompositeBufferManager.java`, `core/src/program/bootstrap/renderpipeline/compositerendersystem/CompositeRenderSystem.java`.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/renderpipeline/renderbatch/RenderBatchStruct.java`

**Type:** `class RenderBatchStruct`
  
**Inheritance/implements:** `extends StructPackage`
  
**Package:** `program.bootstrap.renderpipeline.renderbatch`
  
**File size:** 77 lines

**What this class does:** `RenderBatchStruct` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.renderpipeline.renderbatch`.

**Who this class talks to (direct imports):**
- `program.bootstrap.renderpipeline.rendercall.RenderCallStruct`
- `program.bootstrap.shaderpipeline.material.MaterialInstance`
- `program.bootstrap.shaderpipeline.ubo.UBOHandle`
- `program.core.engine.StructPackage`

**Method intent:**
- `package public RenderBatchStruct(MaterialInstance material)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void addRenderCall(RenderCallStruct renderCall)` — Registers a child object into manager-owned collections.
- `public void clear()` — Performs class-specific logic; see call sites and owning manager flow.
- `public boolean isEmpty()` — Performs class-specific logic; see call sites and owning manager flow.
- `public MaterialInstance getRepresentativeMaterial()` — Returns current state/value.
- `public ObjectArrayList<RenderCallStruct> getRenderCalls()` — Returns current state/value.
- `public UBOHandle[] getCachedSourceUBOs()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/renderpipeline/rendercall/RenderCallStruct.java`

**Type:** `class RenderCallStruct`
  
**Inheritance/implements:** `extends StructPackage`
  
**Package:** `program.bootstrap.renderpipeline.rendercall`
  
**File size:** 72 lines

**What this class does:** `RenderCallStruct` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.renderpipeline.rendercall`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.model.ModelInstance`
- `program.bootstrap.renderpipeline.util.MaskStruct`
- `program.bootstrap.shaderpipeline.material.MaterialInstance`
- `program.bootstrap.shaderpipeline.ubo.UBOInstance`
- `program.bootstrap.shaderpipeline.uniforms.UniformStruct`
- `program.core.engine.StructPackage`

**Method intent:**
- `public void init(ModelInstance modelInstance, MaskStruct mask)` — Performs class-specific logic; see call sites and owning manager flow.
- `public ModelInstance getModelInstance()` — Returns current state/value.
- `public MaterialInstance getMaterialInstance()` — Returns current state/value.
- `public UniformStruct<?>[] getCachedUniforms()` — Returns current state/value.
- `public UBOInstance[] getCachedInstanceUBOs()` — Returns current state/value.
- `public MaskStruct getMask()` — Returns current state/value.
- `public boolean hasMask()` — Boolean existence/availability check.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/renderpipeline/rendermanager/GLSLUtility.java`

**Type:** `class GLSLUtility`
  
**Inheritance/implements:** `extends UtilityPackage`
  
**Package:** `program.bootstrap.renderpipeline.rendermanager`
  
**File size:** 128 lines

**What this class does:** `GLSLUtility` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.renderpipeline.rendermanager`.

**Who this class talks to (direct imports):**
- `program.core.app.CoreContext`
- `program.core.util.graphics.gl.GL20`
- `program.core.util.graphics.gl.GL30`
- `program.core.engine.UtilityPackage`
- `program.core.settings.EngineSetting`

**Method intent:**
- `package void clearBuffer()` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/renderpipeline/rendermanager/RenderSystem.java`.
- `package void clearDepthBuffer()` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/renderpipeline/rendermanager/RenderSystem.java`.
- `package void setViewport(int width, int height)` — Mutates internal state for this object. Called via static reference from: `core/src/program/bootstrap/renderpipeline/rendermanager/RenderSystem.java`.
- `package void enableDepth()` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/renderpipeline/compositerendersystem/CompositeRenderSystem.java`, `core/src/program/bootstrap/renderpipeline/rendermanager/RenderSystem.java`.
- `package void disableDepth()` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/renderpipeline/rendermanager/RenderSystem.java`.
- `package void enableBlending()` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/renderpipeline/rendermanager/RenderSystem.java`.
- `package void disableBlending()` — Performs class-specific logic; see call sites and owning manager flow.
- `package void enableCulling()` — Performs class-specific logic; see call sites and owning manager flow.
- `package void disableCulling()` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/renderpipeline/rendermanager/RenderSystem.java`.
- `package void enableScissor(int x, int y, int w, int h)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/renderpipeline/rendermanager/RenderSystem.java`.
- `package void disableScissor()` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/renderpipeline/rendermanager/RenderSystem.java`.
- `package void useShader(int shaderHandle)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/renderpipeline/compositerendersystem/CompositeRenderSystem.java`, `core/src/program/bootstrap/renderpipeline/rendermanager/RenderSystem.java`.
- `package void bindVAO(int vaoHandle)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/renderpipeline/rendermanager/RenderSystem.java`.
- `package void unbindVAO()` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/renderpipeline/rendermanager/RenderSystem.java`.
- `package void drawElements(int indexCount)` — Flushes or submits rendering work. Called via static reference from: `core/src/program/bootstrap/renderpipeline/rendermanager/RenderSystem.java`.
- `package void bindUniformBuffer(int bindingPoint, int gpuHandle)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/renderpipeline/compositerendersystem/CompositeRenderSystem.java`, `core/src/program/bootstrap/renderpipeline/rendermanager/RenderSystem.java`.
- `package void bindUniformBlockToProgram(int shaderProgram, String blockName, int bindingPoint)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/renderpipeline/rendermanager/RenderSystem.java`.
- `package void updateUniformBuffer(int gpuHandle, int offset, java.nio.ByteBuffer data)` — Runs frame-step maintenance and logic. Called via static reference from: `core/src/program/bootstrap/shaderpipeline/ubomanager/UBOManager.java`.
- `package void swapBuffers(long nativeHandle)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/renderpipeline/rendermanager/RenderManager.java`

**Type:** `class RenderManager`
  
**Inheritance/implements:** `extends ManagerPackage`
  
**Package:** `program.bootstrap.renderpipeline.rendermanager`
  
**File size:** 107 lines

**What this class does:** `RenderManager` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.renderpipeline.rendermanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.compositebuffer.CompositeBufferInstance`
- `program.bootstrap.geometrypipeline.model.ModelInstance`
- `program.bootstrap.renderpipeline.compositerendersystem.CompositeRenderSystem`
- `program.bootstrap.renderpipeline.cameramanager.CameraManager`
- `program.bootstrap.renderpipeline.util.MaskStruct`
- `program.bootstrap.shaderpipeline.material.MaterialInstance`
- `program.core.engine.ManagerPackage`
- `program.core.kernel.window.WindowInstance`
- `program.core.kernel.windowmanager.WindowManager`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void get()` — Returns current state/value.
- `public void draw()` — Flushes or submits rendering work.
- `public void draw(WindowInstance window)` — Flushes or submits rendering work.
- `public void pushRenderCall(ModelInstance modelInstance, int depth)` — Queues data for downstream systems (often render queues).
- `public void pushRenderCall(ModelInstance modelInstance, int depth, MaskStruct mask)` — Queues data for downstream systems (often render queues).
- `public void pushRenderCall(ModelInstance modelInstance, int depth, WindowInstance window)` — Queues data for downstream systems (often render queues).
- `public void pushRenderCall(ModelInstance modelInstance, int depth, MaskStruct mask, WindowInstance window)` — Queues data for downstream systems (often render queues).
- `public void pushCompositeCall(MaterialInstance material, CompositeBufferInstance buffer)` — Queues data for downstream systems (often render queues).
- `public void pushCompositeCall(MaterialInstance material, CompositeBufferInstance buffer, WindowInstance window)` — Queues data for downstream systems (often render queues).
- `public void removeWindowResources(WindowInstance window)` — Unregisters and tears down child references.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/renderpipeline/rendermanager/RenderQueueHandle.java`

**Type:** `class RenderQueueHandle`
  
**Inheritance/implements:** `extends HandlePackage`
  
**Package:** `program.bootstrap.renderpipeline.rendermanager`
  
**File size:** 53 lines

**What this class does:** `RenderQueueHandle` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.renderpipeline.rendermanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.renderpipeline.renderbatch.RenderBatchStruct`
- `program.bootstrap.renderpipeline.rendercall.RenderCallStruct`
- `program.core.engine.HandlePackage`
- `program.core.settings.EngineSetting`

**Method intent:**
- `public void constructor()` — Engine-side initialization entrypoint invoked post-create.
- `package RenderCallStruct nextCall()` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/renderpipeline/rendermanager/RenderSystem.java`

**Type:** `class RenderSystem`
  
**Inheritance/implements:** `extends SystemPackage`
  
**Package:** `program.bootstrap.renderpipeline.rendermanager`
  
**File size:** 247 lines

**What this class does:** `RenderSystem` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.renderpipeline.rendermanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.compositebuffer.CompositeBufferInstance`
- `program.bootstrap.geometrypipeline.mesh.MeshData`
- `program.bootstrap.geometrypipeline.model.ModelInstance`
- `program.bootstrap.geometrypipeline.vaomanager.VAOManager`
- `program.bootstrap.renderpipeline.compositerendersystem.CompositeRenderSystem`
- `program.bootstrap.renderpipeline.renderbatch.RenderBatchStruct`
- `program.bootstrap.renderpipeline.rendercall.RenderCallStruct`
- `program.bootstrap.renderpipeline.util.MaskStruct`
- `program.bootstrap.shaderpipeline.material.MaterialInstance`
- `program.bootstrap.shaderpipeline.ubo.UBOHandle`
- `program.bootstrap.shaderpipeline.ubo.UBOInstance`
- `program.bootstrap.shaderpipeline.uniforms.UniformStruct`
- `program.core.engine.SystemPackage`
- `program.core.kernel.window.WindowInstance`

**Method intent:**
- `protected void get()` — Returns current state/value.
- `protected void awake()` — Runs startup-time runtime activation work.
- `package void draw(WindowInstance window)` — Flushes or submits rendering work.
- `private void bindMaterial(MaterialInstance material, int depth)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void bindSourceUBOs(RenderBatchStruct batch)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void pushInstanceUBOs(RenderCallStruct renderCall)` — Queues data for downstream systems (often render queues).
- `private void pushInstanceUniforms(RenderCallStruct renderCall)` — Queues data for downstream systems (often render queues).
- `private void drawBatchedRenderCall(RenderCallStruct renderCall, WindowInstance window)` — Flushes or submits rendering work.
- `package void pushCompositeCall(MaterialInstance material, CompositeBufferInstance buffer, WindowInstance window)` — Queues data for downstream systems (often render queues).
- `package void removeWindowResources(WindowInstance window)` — Unregisters and tears down child references.
- `package void pushRenderCall(ModelInstance modelInstance, int depth, MaskStruct mask, WindowInstance window)` — Queues data for downstream systems (often render queues).
- `private void insertDepthSorted(RenderQueueHandle queue, int depth)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/renderpipeline/util/MaskStruct.java`

**Type:** `class MaskStruct`
  
**Inheritance/implements:** `extends StructPackage`
  
**Package:** `program.bootstrap.renderpipeline.util`
  
**File size:** 49 lines

**What this class does:** `MaskStruct` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.renderpipeline.util`.

**Who this class talks to (direct imports):**
- `program.core.engine.StructPackage`

**Method intent:**
- `package public MaskStruct()` — Performs class-specific logic; see call sites and owning manager flow.
- `public void set(int x, int y, int w, int h)` — Mutates internal state for this object.
- `public int getX()` — Returns current state/value.
- `public int getY()` — Returns current state/value.
- `public int getW()` — Returns current state/value.
- `public int getH()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.
