# KernelSystemsDoc

This is a human-readable subsystem manual. It explains responsibilities, collaboration points, and method intent for each class in scope.

Classes covered: **12**

## How to read this manual
- Start with the package flow notes at the top of each class section.
- Use **Who talks to this class** to identify collaborators.
- Use **Method intent** to understand lifecycle and API behavior.

## `core/src/program/core/kernel/SyncConsumer/AsyncStructConsumer.java`

**Type:** `interface AsyncStructConsumer`
  
**Inheritance/implements:** `<T extends AsyncContainerPackage>`
  
**Package:** `program.core.kernel.syncconsumer`
  
**File size:** 8 lines

**What this class does:** `AsyncStructConsumer` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.core.kernel.syncconsumer`.

**Who this class talks to (direct imports):**
- `program.core.engine.AsyncContainerPackage`

**Method intent:** No concrete methods parsed (interface marker, enum, or data-only declaration).

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/core/kernel/SyncConsumer/AsyncStructConsumerMulti.java`

**Type:** `interface AsyncStructConsumerMulti`
  
**Package:** `program.core.kernel.syncconsumer`
  
**File size:** 8 lines

**What this class does:** `AsyncStructConsumerMulti` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.core.kernel.syncconsumer`.

**Who this class talks to (direct imports):**
- `program.core.engine.AsyncContainerPackage`

**Method intent:** No concrete methods parsed (interface marker, enum, or data-only declaration).

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/core/kernel/SyncConsumer/BiSyncAsyncConsumer.java`

**Type:** `interface BiSyncAsyncConsumer`
  
**Inheritance/implements:** `<T extends AsyncContainerPackage, S extends SyncContainerPackage>`
  
**Package:** `program.core.kernel.syncconsumer`
  
**File size:** 9 lines

**What this class does:** `BiSyncAsyncConsumer` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.core.kernel.syncconsumer`.

**Who this class talks to (direct imports):**
- `program.core.engine.AsyncContainerPackage`
- `program.core.engine.SyncContainerPackage`

**Method intent:** No concrete methods parsed (interface marker, enum, or data-only declaration).

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/core/kernel/SyncConsumer/SyncStructConsumer.java`

**Type:** `interface SyncStructConsumer`
  
**Inheritance/implements:** `<T extends SyncContainerPackage>`
  
**Package:** `program.core.kernel.syncconsumer`
  
**File size:** 8 lines

**What this class does:** `SyncStructConsumer` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.core.kernel.syncconsumer`.

**Who this class talks to (direct imports):**
- `program.core.engine.SyncContainerPackage`

**Method intent:** No concrete methods parsed (interface marker, enum, or data-only declaration).

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/core/kernel/thread/ThreadHandle.java`

**Type:** `class ThreadHandle`
  
**Inheritance/implements:** `extends HandlePackage`
  
**Package:** `program.core.kernel.thread`
  
**File size:** 58 lines

**What this class does:** `ThreadHandle` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.core.kernel.thread`.

**Who this class talks to (direct imports):**
- `program.core.engine.HandlePackage`

**Method intent:**
- `public void constructor(String threadName, int threadSize, ExecutorService executor)` — Engine-side initialization entrypoint invoked post-create.
- `public void dispose()` — Releases owned resources and unregisters state.
- `public String getThreadName()` — Returns current state/value.
- `public int getThreadSize()` — Returns current state/value.
- `public ExecutorService getExecutor()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/core/kernel/threadmanager/InternalBuilder.java`

**Type:** `class InternalBuilder`
  
**Inheritance/implements:** `extends BuilderPackage`
  
**Package:** `program.core.kernel.threadmanager`
  
**File size:** 21 lines

**What this class does:** `InternalBuilder` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.core.kernel.threadmanager`.

**Who this class talks to (direct imports):**
- `program.core.engine.BuilderPackage`
- `program.core.kernel.thread.ThreadHandle`

**Method intent:**
- `package ThreadHandle build(String threadName, int threadSize)` — Constructs derived runtime/handle data from source input.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/core/kernel/threadmanager/InternalLoader.java`

**Type:** `class InternalLoader`
  
**Inheritance/implements:** `extends LoaderPackage`
  
**Package:** `program.core.kernel.threadmanager`
  
**File size:** 124 lines

**What this class does:** `InternalLoader` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.core.kernel.threadmanager`.

**Who this class talks to (direct imports):**
- `program.core.engine.LoaderPackage`
- `program.core.kernel.thread.ThreadHandle`
- `program.core.settings.EngineSetting`
- `program.core.util.FileUtility`
- `program.core.util.JsonUtility`

**Method intent:**
- `protected void scan()` — Discovers files/resources for later load.
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void get()` — Returns current state/value.
- `private void preRegisterThreadNames(File file, String resourceName)` — Performs class-specific logic; see call sites and owning manager flow.
- `protected void load(File file)` — Parses external data into engine objects.
- `package void request(String threadName)` — Triggers on-demand loading or lookup.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/core/kernel/threadmanager/InternalThreadManager.java`

**Type:** `class InternalThreadManager`
  
**Inheritance/implements:** `extends ManagerPackage`
  
**Package:** `program.core.kernel.threadmanager`
  
**File size:** 136 lines

**What this class does:** `InternalThreadManager` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.core.kernel.threadmanager`.

**Who this class talks to (direct imports):**
- `program.core.engine.AsyncContainerPackage`
- `program.core.engine.ManagerPackage`
- `program.core.engine.SyncContainerPackage`
- `program.core.kernel.syncconsumer.AsyncStructConsumer`
- `program.core.kernel.syncconsumer.AsyncStructConsumerMulti`
- `program.core.kernel.syncconsumer.BiSyncAsyncConsumer`
- `program.core.kernel.syncconsumer.SyncStructConsumer`
- `program.core.kernel.thread.ThreadHandle`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void dispose()` — Releases owned resources and unregisters state.
- `public void request(String resourceName)` — Triggers on-demand loading or lookup.
- `package void addThreadHandle(String threadName, ThreadHandle threadHandle)` — Registers a child object into manager-owned collections.
- `public ThreadHandle getThreadHandleFromThreadName(String threadName)` — Returns current state/value.
- `public Future<?> executeAsync(ThreadHandle handle, Runnable task)` — Performs class-specific logic; see call sites and owning manager flow.
- `public <T extends AsyncContainerPackage> Future<?> executeAsync(ThreadHandle handle, T asyncStruct, AsyncStructConsumer<T> consumer)` — Performs class-specific logic; see call sites and owning manager flow.
- `public Future<?> executeAsync(ThreadHandle handle, AsyncStructConsumerMulti consumer, AsyncContainerPackage... asyncStructs)` — Performs class-specific logic; see call sites and owning manager flow.
- `public <T extends SyncContainerPackage> Future<?> executeAsync(ThreadHandle handle, T syncStruct, SyncStructConsumer<T> consumer)` — Performs class-specific logic; see call sites and owning manager flow.
- `public <T extends AsyncContainerPackage, S extends SyncContainerPackage> Future<?> executeAsync(ThreadHandle handle, T asyncStruct, S syncStruct, BiSyncAsyncConsumer<T, S> consumer)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/core/kernel/threadmanager/NamedThreadFactory.java`

**Type:** `class NamedThreadFactory`
  
**Inheritance/implements:** `implements ThreadFactory`
  
**Package:** `program.core.kernel.threadmanager`
  
**File size:** 21 lines

**What this class does:** `NamedThreadFactory` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.core.kernel.threadmanager`.

**Method intent:**
- `package  NamedThreadFactory(String baseName)` — Performs class-specific logic; see call sites and owning manager flow.
- `public Thread newThread(Runnable r)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/core/kernel/window/WindowData.java`

**Type:** `class WindowData`
  
**Inheritance/implements:** `extends DataPackage`
  
**Package:** `program.core.kernel.window`
  
**File size:** 63 lines

**What this class does:** `WindowData` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.core.kernel.window`.

**Who this class talks to (direct imports):**
- `program.core.engine.DataPackage`

**Method intent:**
- `package public WindowData(int windowID, int width, int height, String title)` — Performs class-specific logic; see call sites and owning manager flow.
- `public int getWindowID()` — Returns current state/value.
- `public String getTitle()` — Returns current state/value.
- `public int getWidth()` — Returns current state/value.
- `public void setWidth(int width)` — Mutates internal state for this object.
- `public int getHeight()` — Returns current state/value.
- `public void setHeight(int height)` — Mutates internal state for this object.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/core/kernel/window/WindowInstance.java`

**Type:** `class WindowInstance`
  
**Inheritance/implements:** `extends InstancePackage implements Screen, ApplicationListener`
  
**Package:** `program.core.kernel.window`
  
**File size:** 205 lines

**What this class does:** `WindowInstance` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.core.kernel.window`.

**Who this class talks to (direct imports):**
- `program.core.app.ApplicationListener`
- `program.core.app.Screen`
- `program.bootstrap.geometrypipeline.vaomanager.VAOManager`
- `program.bootstrap.renderpipeline.rendermanager.RenderManager`
- `program.bootstrap.renderpipeline.rendermanager.RenderQueueHandle`
- `program.core.engine.ContextPackage`
- `program.core.engine.InstancePackage`
- `program.core.kernel.windowmanager.WindowManager`
- `program.core.util.camera.CameraInstance`
- `program.core.util.camera.OrthographicCameraInstance`

**Method intent:**
- `public void constructor(WindowData windowData)` — Engine-side initialization entrypoint invoked post-create.
- `protected void get()` — Returns current state/value.
- `protected void awake()` — Runs startup-time runtime activation work.
- `public void create()` — Allocates/initializes child systems or resources.
- `public void render()` — Performs class-specific logic; see call sites and owning manager flow.
- `public void resize(int width, int height)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void pause()` — Performs class-specific logic; see call sites and owning manager flow.
- `public void resume()` — Performs class-specific logic; see call sites and owning manager flow.
- `public void dispose()` — Releases owned resources and unregisters state.
- `public void render(float delta)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void show()` — Performs class-specific logic; see call sites and owning manager flow.
- `public void hide()` — Performs class-specific logic; see call sites and owning manager flow.
- `public ContextPackage getContext()` — Returns current state/value.
- `public void setContext(ContextPackage context)` — Mutates internal state for this object.
- `public boolean hasContext()` — Boolean existence/availability check.
- `public void setPendingContextType(Class<? extends ContextPackage> pendingContextType)` — Mutates internal state for this object.
- `public long getNativeHandle()` — Returns current state/value.
- `public void setNativeHandle(long nativeHandle)` — Mutates internal state for this object.
- `public boolean hasNativeHandle()` — Boolean existence/availability check.
- `public CameraInstance getActiveCamera()` — Returns current state/value.
- `public void setActiveCamera(CameraInstance activeCamera)` — Mutates internal state for this object.
- `public OrthographicCameraInstance getOrthoCamera()` — Returns current state/value.
- `public void setOrthoCamera(OrthographicCameraInstance orthoCamera)` — Mutates internal state for this object.
- `public WindowData getWindowData()` — Returns current state/value.
- `public RenderQueueHandle getRenderQueueHandle()` — Returns current state/value.
- `public int getWindowID()` — Returns current state/value.
- `public int getWidth()` — Returns current state/value.
- `public int getHeight()` — Returns current state/value.
- `public String getTitle()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/core/kernel/windowmanager/WindowManager.java`

**Type:** `class WindowManager`
  
**Inheritance/implements:** `extends ManagerPackage`
  
**Package:** `program.core.kernel.windowmanager`
  
**File size:** 119 lines

**What this class does:** `WindowManager` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.core.kernel.windowmanager`.

**Who this class talks to (direct imports):**
- `program.core.engine.ManagerPackage`
- `program.core.kernel.window.WindowInstance`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void update()` — Runs frame-step maintenance and logic.
- `protected void dispose()` — Releases owned resources and unregisters state.
- `public void registerMainWindow(WindowInstance window)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void registerDetachedWindow(WindowInstance window)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void removeWindow(WindowInstance window)` — Unregisters and tears down child references.
- `public int issueWindowID()` — Performs class-specific logic; see call sites and owning manager flow.
- `public WindowInstance getMainWindow()` — Returns current state/value.
- `public WindowInstance getActiveWindow()` — Returns current state/value.
- `public void setActiveWindow(WindowInstance window)` — Mutates internal state for this object.
- `public ObjectArrayList<WindowInstance> getWindows()` — Returns current state/value.
- `public boolean hasMainWindow()` — Boolean existence/availability check.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.
