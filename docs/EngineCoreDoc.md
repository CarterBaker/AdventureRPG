# EngineCoreDoc

This is a human-readable subsystem manual. It explains responsibilities, collaboration points, and method intent for each class in scope.

Classes covered: **24**

## How to read this manual
- Start with the package flow notes at the top of each class section.
- Use **Who talks to this class** to identify collaborators.
- Use **Method intent** to understand lifecycle and API behavior.

## `core/src/program/core/engine/AssemblyPackage.java`

**Type:** `class AssemblyPackage`
  
**Inheritance/implements:** `extends ManagerPackage`
  
**Package:** `program.core.engine`
  
**File size:** 11 lines

**What this class does:** `AssemblyPackage` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.core.engine`.

**Method intent:** No concrete methods parsed (interface marker, enum, or data-only declaration).

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/core/engine/AsyncContainerPackage.java`

**Type:** `class AsyncContainerPackage`
  
**Inheritance/implements:** `extends InstancePackage`
  
**Package:** `program.core.engine`
  
**File size:** 91 lines

**What this class does:** `AsyncContainerPackage` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.core.engine`.

**Method intent:**
- `package protected AsyncContainerPackage()` — Performs class-specific logic; see call sites and owning manager flow.
- `private Object createThreadInstance()` — Allocates/initializes child systems or resources.
- `public void reset()` — Performs class-specific logic; see call sites and owning manager flow.
- `public final <T extends AsyncContainerPackage> T getInstance()` — Returns current state/value.
- `public final void removeInstance()` — Unregisters and tears down child references.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/core/engine/BranchPackage.java`

**Type:** `class BranchPackage`
  
**Inheritance/implements:** `extends ManagerPackage`
  
**Package:** `program.core.engine`
  
**File size:** 17 lines

**What this class does:** `BranchPackage` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.core.engine`.

**Method intent:** No concrete methods parsed (interface marker, enum, or data-only declaration).

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/core/engine/BuilderPackage.java`

**Type:** `class BuilderPackage`
  
**Inheritance/implements:** `extends SystemPackage`
  
**Package:** `program.core.engine`
  
**File size:** 24 lines

**What this class does:** `BuilderPackage` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.core.engine`.

**Method intent:** No concrete methods parsed (interface marker, enum, or data-only declaration).

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/core/engine/ContextPackage.java`

**Type:** `class ContextPackage`
  
**Inheritance/implements:** `extends ManagerPackage`
  
**Package:** `program.core.engine`
  
**File size:** 80 lines

**What this class does:** `ContextPackage` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.core.engine`.

**Who this class talks to (direct imports):**
- `program.core.kernel.window.WindowInstance`

**Method intent:**
- `package protected ContextPackage()` — Performs class-specific logic; see call sites and owning manager flow.
- `package boolean verifyContext(SystemContext targetContext)` — Performs class-specific logic; see call sites and owning manager flow.
- `protected <T extends SystemPackage> T registerSystem(T systemPackage)` — Performs class-specific logic; see call sites and owning manager flow.
- `package <T> T getLocal(Class<T> type)` — Returns current state/value.
- `public WindowInstance getWindow()` — Returns current state/value.
- `package void setWindow(WindowInstance window)` — Mutates internal state for this object.
- `public boolean hasWindow()` — Boolean existence/availability check.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/core/engine/DataPackage.java`

**Type:** `class DataPackage`
  
**Inheritance/implements:** `extends StructPackage`
  
**Package:** `program.core.engine`
  
**File size:** 13 lines

**What this class does:** `DataPackage` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.core.engine`.

**Method intent:** No concrete methods parsed (interface marker, enum, or data-only declaration).

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/core/engine/EditorEngine.java`

**Type:** `class EditorEngine`
  
**Inheritance/implements:** `extends EnginePackage`
  
**Package:** `program.core.engine`
  
**File size:** 63 lines

**What this class does:** `EditorEngine` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.core.engine`.

**Who this class talks to (direct imports):**
- `program.bootstrap.renderpipeline.rendermanager.RenderManager`
- `program.core.kernel.windowmanager.WindowManager`
- `program.editor.runtime.RuntimeContext`

**Method intent:**
- `protected void bootstrap()` — Performs class-specific logic; see call sites and owning manager flow.
- `protected void get()` — Returns current state/value.
- `protected void awake()` — Runs startup-time runtime activation work.
- `package void draw()` — Flushes or submits rendering work.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/core/engine/EnginePackage.java`

**Type:** `class EnginePackage`
  
**Inheritance/implements:** `extends ManagerPackage`
  
**Package:** `program.core.engine`
  
**File size:** 678 lines

**What this class does:** `EnginePackage` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.core.engine`.

**Who this class talks to (direct imports):**
- `program.core.app.CoreContext`
- `program.core.app.Game`
- `program.core.app.Screen`
- `program.core.kernel.syncconsumer.AsyncStructConsumer`
- `program.core.kernel.syncconsumer.AsyncStructConsumerMulti`
- `program.core.kernel.syncconsumer.BiSyncAsyncConsumer`
- `program.core.kernel.syncconsumer.SyncStructConsumer`
- `program.core.kernel.thread.ThreadHandle`
- `program.core.kernel.threadmanager.InternalThreadManager`
- `program.core.kernel.window.WindowData`
- `program.core.kernel.window.WindowInstance`
- `program.core.kernel.windowmanager.WindowManager`
- `program.core.settings.EngineSetting`
- `program.core.settings.Settings`
- `program.core.util.camera.CameraData`
- `program.core.util.camera.CameraInstance`
- `program.core.util.camera.OrthographicCameraData`
- `program.core.util.camera.OrthographicCameraInstance`

**Method intent:**
- `package  EnginePackage()` — Performs class-specific logic; see call sites and owning manager flow.
- `package  EngineStruct(Settings settings, Game game, File path, Gson gson, WindowPlatform windowPlatform)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void setupConstructor(Settings settings, Game game, File path, Gson gson, WindowPlatform windowPlatform)` — Mutates internal state for this object. Called via static reference from: `core/src/program/core/engine/EnginePackage.java`, `core/src/program/core/engine/Main.java`, `core/src/program/core/engine/MainEditor.java`.
- `package final EngineState getEngineState()` — Returns current state/value.
- `package final void setInternalState(EngineState target)` — Mutates internal state for this object.
- `package final void setContext(SystemContext targetContext)` — Mutates internal state for this object.
- `package boolean verifyContext(SystemContext targetContext)` — Performs class-specific logic; see call sites and owning manager flow.
- `protected <T extends SystemPackage> T registerSystem(T systemPackage)` — Performs class-specific logic; see call sites and owning manager flow.
- `package final void clearGarbage()` — Performs class-specific logic; see call sites and owning manager flow.
- `package final <T> T get(boolean bypass, Class<T> type)` — Returns current state/value.
- `public final <T> T getUnchecked(Class<T> type)` — Returns current state/value.
- `public <T extends ContextPackage> T createContext(Class<T> contextClass, WindowInstance window)` — Allocates/initializes child systems or resources. Called via static reference from: `core/src/program/core/engine/ContextPackage.java`, `core/src/program/runtime/RuntimeContext.java`.
- `public void destroyContext(ContextPackage context)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void flushPendingContexts()` — Performs class-specific logic; see call sites and owning manager flow.
- `private void cacheContextArray()` — Performs class-specific logic; see call sites and owning manager flow.
- `protected final ThreadHandle getThreadHandleFromThreadName(String threadName)` — Returns current state/value.
- `protected final Future<?> executeAsync(ThreadHandle handle, Runnable task)` — Performs class-specific logic; see call sites and owning manager flow.
- `protected final <T extends AsyncContainerPackage> Future<?> executeAsync(ThreadHandle handle, T asyncStruct, AsyncStructConsumer<T> consumer)` — Performs class-specific logic; see call sites and owning manager flow.
- `protected final Future<?> executeAsync(ThreadHandle handle, AsyncStructConsumerMulti consumer, AsyncContainerPackage... asyncStructs)` — Performs class-specific logic; see call sites and owning manager flow.
- `protected final <T extends SyncContainerPackage> Future<?> executeAsync(ThreadHandle handle, T syncStruct, SyncStructConsumer<T> consumer)` — Performs class-specific logic; see call sites and owning manager flow.
- `protected final <T extends AsyncContainerPackage, S extends SyncContainerPackage> Future<?> executeAsync(ThreadHandle handle, T asyncStruct, S syncStruct, BiSyncAsyncConsumer<T, S> consumer)` — Performs class-specific logic; see call sites and owning manager flow.
- `package final void execute()` — Performs class-specific logic; see call sites and owning manager flow.
- `private final void kernelCycle()` — Performs class-specific logic; see call sites and owning manager flow.
- `private final void bootstrapCycle()` — Performs class-specific logic; see call sites and owning manager flow.
- `private final void preFrameCycle(EngineState nextState)` — Performs class-specific logic; see call sites and owning manager flow.
- `private final void createCycle()` — Allocates/initializes child systems or resources.
- `private final void startCycle()` — Performs class-specific logic; see call sites and owning manager flow.
- `private final void updateCycle()` — Runs frame-step maintenance and logic.
- `private final void exitCycle()` — Performs class-specific logic; see call sites and owning manager flow.
- `private void internalKernel()` — Performs class-specific logic; see call sites and owning manager flow.
- `private final void kernel()` — Performs class-specific logic; see call sites and owning manager flow.
- `private final void internalBootstrap()` — Performs class-specific logic; see call sites and owning manager flow.
- `package void bootstrap()` — Performs class-specific logic; see call sites and owning manager flow.
- `protected final void internalCreate()` — Performs class-specific logic; see call sites and owning manager flow.
- `private final void preGet()` — Performs class-specific logic; see call sites and owning manager flow.
- `protected final void internalGet()` — Performs class-specific logic; see call sites and owning manager flow.
- `private final void preAwake()` — Performs class-specific logic; see call sites and owning manager flow.
- `protected final void internalAwake()` — Performs class-specific logic; see call sites and owning manager flow.
- `private final void preRelease()` — Performs class-specific logic; see call sites and owning manager flow.
- `protected final void internalRelease()` — Performs class-specific logic; see call sites and owning manager flow.
- `protected final void internalStart()` — Performs class-specific logic; see call sites and owning manager flow.
- `protected final void internalUpdate()` — Performs class-specific logic; see call sites and owning manager flow.
- `protected final void internalFixedUpdate()` — Performs class-specific logic; see call sites and owning manager flow.
- `protected final void internalLateUpdate()` — Performs class-specific logic; see call sites and owning manager flow.
- `protected final void internalRender()` — Performs class-specific logic; see call sites and owning manager flow.
- `private final void internalDraw()` — Performs class-specific logic; see call sites and owning manager flow.
- `package void draw()` — Flushes or submits rendering work.
- `protected final void internalDispose()` — Performs class-specific logic; see call sites and owning manager flow.
- `private final void setFrameCount()` — Mutates internal state for this object.
- `package final void setDeltaTime(float delta)` — Mutates internal state for this object.
- `private final void createGameWindow()` — Allocates/initializes child systems or resources.
- `public CameraInstance createCamera(float fov, float width, float height)` — Allocates/initializes child systems or resources.
- `public OrthographicCameraInstance createOrthographicCamera(float width, float height)` — Allocates/initializes child systems or resources.
- `public final EngineState getState()` — Returns current state/value.
- `public final long getFrameCount()` — Returns current state/value.
- `public final float getDeltaTime()` — Returns current state/value.
- `public final long getTime()` — Returns current state/value.
- `public final void closeGame()` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/core/engine/EngineState.java`

**Type:** `enum EngineState`
  
**Package:** `program.core.engine`
  
**File size:** 17 lines

**What this class does:** `EngineState` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.core.engine`.

**Method intent:** No concrete methods parsed (interface marker, enum, or data-only declaration).

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/core/engine/EngineUtility.java`

**Type:** `class EngineUtility`
  
**Package:** `program.core.engine`
  
**File size:** 123 lines

**What this class does:** `EngineUtility` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.core.engine`.

**Method intent:**
- `protected final void debug()` — Performs class-specific logic; see call sites and owning manager flow.
- `protected final void debug(Object input)` — Performs class-specific logic; see call sites and owning manager flow.
- `protected final void timeStampDebug(Object input)` — Performs class-specific logic; see call sites and owning manager flow.
- `protected final void log(Object input)` — Performs class-specific logic; see call sites and owning manager flow.
- `protected final void errorLog(Object input)` — Performs class-specific logic; see call sites and owning manager flow.
- `protected final void timeStampLog(Object input)` — Performs class-specific logic; see call sites and owning manager flow.
- `protected final <T> T throwException()` — Performs class-specific logic; see call sites and owning manager flow.
- `protected final <T> T throwException(String message)` — Performs class-specific logic; see call sites and owning manager flow.
- `protected final <T> T throwException(Throwable cause)` — Performs class-specific logic; see call sites and owning manager flow.
- `protected final <T> T throwException(String message, Throwable cause)` — Performs class-specific logic; see call sites and owning manager flow.
- `protected final <T> T throwException(Object input)` — Performs class-specific logic; see call sites and owning manager flow.
- `protected final <T> T throwException(Object input, Throwable cause)` — Performs class-specific logic; see call sites and owning manager flow.
- `private final String timeStamp()` — Performs class-specific logic; see call sites and owning manager flow.
- `package public InternalException(String message)` — Performs class-specific logic; see call sites and owning manager flow.
- `package public InternalException(String message, Throwable cause)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void logFatal(String message, Throwable cause)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/core/engine/GameEngine.java`

**Type:** `class GameEngine`
  
**Inheritance/implements:** `extends EnginePackage`
  
**Package:** `program.core.engine`
  
**File size:** 59 lines

**What this class does:** `GameEngine` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.core.engine`.

**Who this class talks to (direct imports):**
- `program.bootstrap.BootstrapAssembly`
- `program.bootstrap.renderpipeline.rendermanager.RenderManager`
- `program.core.kernel.windowmanager.WindowManager`
- `program.runtime.RuntimeContext`

**Method intent:**
- `protected void bootstrap()` — Performs class-specific logic; see call sites and owning manager flow.
- `protected void get()` — Returns current state/value.
- `protected void awake()` — Runs startup-time runtime activation work.
- `package void draw()` — Flushes or submits rendering work.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/core/engine/HandlePackage.java`

**Type:** `class HandlePackage`
  
**Inheritance/implements:** `extends InstancePackage`
  
**Package:** `program.core.engine`
  
**File size:** 15 lines

**What this class does:** `HandlePackage` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.core.engine`.

**Method intent:** No concrete methods parsed (interface marker, enum, or data-only declaration).

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/core/engine/InstancePackage.java`

**Type:** `class InstancePackage`
  
**Inheritance/implements:** `extends EngineUtility`
  
**Package:** `program.core.engine`
  
**File size:** 156 lines

**What this class does:** `InstancePackage` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.core.engine`.

**Method intent:**
- `package public InstancePackage()` — Performs class-specific logic; see call sites and owning manager flow.
- `package  CreationStruct(EnginePackage internal, SystemPackage owner)` — Performs class-specific logic; see call sites and owning manager flow.
- `package void setupConstructor(EnginePackage internal, SystemPackage owner)` — Mutates internal state for this object. Called via static reference from: `core/src/program/core/engine/AsyncContainerPackage.java`, `core/src/program/core/engine/SystemPackage.java`.
- `package final SystemContext getContext()` — Returns current state/value.
- `package final boolean verifyProcess(SystemContext targetContext)` — Performs class-specific logic; see call sites and owning manager flow.
- `package final void requestContext(SystemContext targetContext)` — Triggers on-demand loading or lookup.
- `protected final <T extends InstancePackage> T create(Class<T> instanceClass)` — Allocates/initializes child systems or resources.
- `protected final <T> T get(Class<T> instanceClass)` — Returns current state/value.
- `package void internalCreate()` — Performs class-specific logic; see call sites and owning manager flow.
- `protected void create()` — Allocates/initializes child systems or resources.
- `package void internalGet()` — Performs class-specific logic; see call sites and owning manager flow.
- `protected void get()` — Returns current state/value.
- `package void internalAwake()` — Performs class-specific logic; see call sites and owning manager flow.
- `protected void awake()` — Runs startup-time runtime activation work.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/core/engine/LoaderPackage.java`

**Type:** `class LoaderPackage`
  
**Inheritance/implements:** `extends ManagerPackage`
  
**Package:** `program.core.engine`
  
**File size:** 241 lines

**What this class does:** `LoaderPackage` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.core.engine`.

**Who this class talks to (direct imports):**
- `program.core.settings.EngineSetting`

**Method intent:**
- `package protected LoaderPackage()` — Parses external data into engine objects.
- `package void internalCreate()` — Performs class-specific logic; see call sites and owning manager flow.
- `private void internalScan()` — Performs class-specific logic; see call sites and owning manager flow.
- `protected File directory()` — Performs class-specific logic; see call sites and owning manager flow.
- `protected void scan()` — Discovers files/resources for later load.
- `package void internalUpdate()` — Performs class-specific logic; see call sites and owning manager flow.
- `protected void load(File file)` — Parses external data into engine objects.
- `protected void onComplete()` — Performs class-specific logic; see call sites and owning manager flow.
- `protected final void request(File file)` — Triggers on-demand loading or lookup.
- `package void internalRelease()` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/core/engine/Main.java`

**Type:** `class Main`
  
**Inheritance/implements:** `extends Game`
  
**Package:** `program.core.engine`
  
**File size:** 103 lines

**What this class does:** `Main` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.core.engine`.

**Who this class talks to (direct imports):**
- `program.core.app.CoreContext`
- `program.core.app.Game`
- `program.core.settings.Settings`

**Method intent:**
- `package public Main(File GAME_DIRECTORY, Settings settings, Gson gson, WindowPlatform windowPlatform)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void create()` — Allocates/initializes child systems or resources.
- `public void render()` — Performs class-specific logic; see call sites and owning manager flow.
- `public void dispose()` — Releases owned resources and unregisters state.
- `private void setDeltaTime()` — Mutates internal state for this object.
- `private void handleSettingsFile()` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/core/engine/MainEditor.java`

**Type:** `class MainEditor`
  
**Inheritance/implements:** `extends Game`
  
**Package:** `program.core.engine`
  
**File size:** 98 lines

**What this class does:** `MainEditor` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.core.engine`.

**Who this class talks to (direct imports):**
- `program.core.app.CoreContext`
- `program.core.app.Game`
- `program.core.settings.Settings`

**Method intent:**
- `package public MainEditor(File GAME_DIRECTORY, Settings settings, Gson gson, WindowPlatform windowPlatform)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void create()` — Allocates/initializes child systems or resources.
- `public void render()` — Performs class-specific logic; see call sites and owning manager flow.
- `public void dispose()` — Releases owned resources and unregisters state.
- `private void setDeltaTime()` — Mutates internal state for this object.
- `private void handleSettingsFile()` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/core/engine/ManagerPackage.java`

**Type:** `class ManagerPackage`
  
**Inheritance/implements:** `extends SystemPackage`
  
**Package:** `program.core.engine`
  
**File size:** 312 lines

**What this class does:** `ManagerPackage` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.core.engine`.

**Who this class talks to (direct imports):**
- `program.core.settings.Settings`

**Method intent:**
- `package protected ManagerPackage(Settings settings)` — Performs class-specific logic; see call sites and owning manager flow.
- `package protected ManagerPackage()` — Performs class-specific logic; see call sites and owning manager flow.
- `protected <T extends EngineUtility> T create(Class<T> systemClass)` — Allocates/initializes child systems or resources.
- `protected <T> T get(Class<T> type)` — Returns current state/value.
- `protected void requestFromLoader(File file)` — Triggers on-demand loading or lookup.
- `package <T extends SystemPackage> T createSystem(Class<T> systemClass)` — Allocates/initializes child systems or resources.
- `protected <T extends SystemPackage> T registerSystem(T systemPackage)` — Performs class-specific logic; see call sites and owning manager flow.
- `public <T extends SystemPackage> T release(Class<T> systemClass)` — Performs class-specific logic; see call sites and owning manager flow.
- `package void internalRelease(Class<?> systemClass)` — Performs class-specific logic; see call sites and owning manager flow.
- `package void clearGarbage()` — Performs class-specific logic; see call sites and owning manager flow.
- `package void internalCreate()` — Performs class-specific logic; see call sites and owning manager flow.
- `package void internalGet()` — Performs class-specific logic; see call sites and owning manager flow.
- `package void internalAwake()` — Performs class-specific logic; see call sites and owning manager flow.
- `package void internalRelease()` — Performs class-specific logic; see call sites and owning manager flow.
- `package void internalStart()` — Performs class-specific logic; see call sites and owning manager flow.
- `package void internalUpdate()` — Performs class-specific logic; see call sites and owning manager flow.
- `package void internalFixedUpdate()` — Performs class-specific logic; see call sites and owning manager flow.
- `package void internalLateUpdate()` — Performs class-specific logic; see call sites and owning manager flow.
- `package void internalRender()` — Performs class-specific logic; see call sites and owning manager flow.
- `package void internalDispose()` — Performs class-specific logic; see call sites and owning manager flow.
- `package final void cacheSubSystems()` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/core/engine/PipelinePackage.java`

**Type:** `class PipelinePackage`
  
**Inheritance/implements:** `extends ManagerPackage`
  
**Package:** `program.core.engine`
  
**File size:** 11 lines

**What this class does:** `PipelinePackage` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.core.engine`.

**Method intent:** No concrete methods parsed (interface marker, enum, or data-only declaration).

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/core/engine/StructPackage.java`

**Type:** `class StructPackage`
  
**Inheritance/implements:** `extends EngineUtility`
  
**Package:** `program.core.engine`
  
**File size:** 14 lines

**What this class does:** `StructPackage` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.core.engine`.

**Method intent:** No concrete methods parsed (interface marker, enum, or data-only declaration).

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/core/engine/SyncContainerPackage.java`

**Type:** `class SyncContainerPackage`
  
**Inheritance/implements:** `extends InstancePackage`
  
**Package:** `program.core.engine`
  
**File size:** 58 lines

**What this class does:** `SyncContainerPackage` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.core.engine`.

**Method intent:**
- `package protected SyncContainerPackage()` — Performs class-specific logic; see call sites and owning manager flow.
- `public final boolean tryAcquire()` — Performs class-specific logic; see call sites and owning manager flow.
- `public final void acquire()` — Performs class-specific logic; see call sites and owning manager flow.
- `public final void release()` — Performs class-specific logic; see call sites and owning manager flow.
- `public final boolean isInUse()` — Performs class-specific logic; see call sites and owning manager flow.
- `public final boolean isLocked()` — Performs class-specific logic; see call sites and owning manager flow.
- `public void reset()` — Performs class-specific logic; see call sites and owning manager flow.
- `public final <T extends SyncContainerPackage> T getInstance()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/core/engine/SystemContext.java`

**Type:** `enum SystemContext`
  
**Package:** `program.core.engine`
  
**File size:** 105 lines

**What this class does:** `SystemContext` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.core.engine`.

**Who this class talks to (direct imports):**
- `program.core.engine.EngineUtility.InternalException`

**Method intent:**
- `package  SystemContext(String... entryPoints)` — Performs class-specific logic; see call sites and owning manager flow.
- `package boolean canEnterFrom(int current)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/core/engine/SystemPackage.java`

**Type:** `class SystemPackage`
  
**Inheritance/implements:** `extends EngineUtility`
  
**Package:** `program.core.engine`
  
**File size:** 335 lines

**What this class does:** `SystemPackage` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.core.engine`.

**Who this class talks to (direct imports):**
- `program.core.kernel.syncconsumer.AsyncStructConsumer`
- `program.core.kernel.syncconsumer.AsyncStructConsumerMulti`
- `program.core.kernel.syncconsumer.BiSyncAsyncConsumer`
- `program.core.kernel.syncconsumer.SyncStructConsumer`
- `program.core.kernel.thread.ThreadHandle`
- `program.core.settings.Settings`

**Method intent:**
- `package protected SystemPackage(Settings settings)` — Performs class-specific logic; see call sites and owning manager flow.
- `package protected SystemPackage()` — Performs class-specific logic; see call sites and owning manager flow.
- `package  SystemStruct(Settings settings, EnginePackage internal, ManagerPackage local)` — Performs class-specific logic; see call sites and owning manager flow.
- `package void setupConstructor(Settings settings, EnginePackage internal, ManagerPackage local)` — Mutates internal state for this object. Called via static reference from: `core/src/program/core/engine/EnginePackage.java`, `core/src/program/core/engine/ManagerPackage.java`.
- `package final SystemContext getContext()` — Returns current state/value.
- `package void setContext(SystemContext targetContext)` — Mutates internal state for this object.
- `package boolean verifyContext(SystemContext targetContext)` — Performs class-specific logic; see call sites and owning manager flow.
- `protected <T extends EngineUtility> T create(Class<T> targetClass)` — Allocates/initializes child systems or resources.
- `package final <T extends InstancePackage> T createInstance(Class<T> instanceClass)` — Allocates/initializes child systems or resources.
- `protected <T> T get(Class<T> type)` — Returns current state/value. Called via static reference from: `core/src/program/core/engine/ManagerPackage.java`.
- `protected ThreadHandle getThreadHandleFromThreadName(String threadName)` — Returns current state/value.
- `protected Future<?> executeAsync(ThreadHandle handle, Runnable task)` — Performs class-specific logic; see call sites and owning manager flow.
- `protected <T extends AsyncContainerPackage> Future<?> executeAsync(ThreadHandle handle, T asyncStruct, AsyncStructConsumer<T> consumer)` — Performs class-specific logic; see call sites and owning manager flow.
- `protected Future<?> executeAsync(ThreadHandle handle, AsyncStructConsumerMulti consumer, AsyncContainerPackage... asyncStructs)` — Performs class-specific logic; see call sites and owning manager flow.
- `protected <T extends SyncContainerPackage> Future<?> executeAsync(ThreadHandle handle, T syncStruct, SyncStructConsumer<T> consumer)` — Performs class-specific logic; see call sites and owning manager flow.
- `protected <T extends AsyncContainerPackage, S extends SyncContainerPackage> Future<?> executeAsync(ThreadHandle handle, T asyncStruct, S syncStruct, BiSyncAsyncConsumer<T, S> consumer)` — Performs class-specific logic; see call sites and owning manager flow.
- `package void internalCreate()` — Performs class-specific logic; see call sites and owning manager flow.
- `protected void create()` — Allocates/initializes child systems or resources.
- `package void internalGet()` — Performs class-specific logic; see call sites and owning manager flow.
- `protected void get()` — Returns current state/value. Called via static reference from: `core/src/program/core/engine/ManagerPackage.java`.
- `package void internalAwake()` — Performs class-specific logic; see call sites and owning manager flow.
- `protected void awake()` — Runs startup-time runtime activation work.
- `package void internalRelease()` — Performs class-specific logic; see call sites and owning manager flow.
- `protected void release()` — Performs class-specific logic; see call sites and owning manager flow.
- `package void internalStart()` — Performs class-specific logic; see call sites and owning manager flow.
- `protected void start()` — Performs class-specific logic; see call sites and owning manager flow.
- `package void internalUpdate()` — Performs class-specific logic; see call sites and owning manager flow.
- `protected void update()` — Runs frame-step maintenance and logic.
- `package void internalFixedUpdate()` — Performs class-specific logic; see call sites and owning manager flow.
- `protected void fixedUpdate()` — Performs class-specific logic; see call sites and owning manager flow.
- `package void internalLateUpdate()` — Performs class-specific logic; see call sites and owning manager flow.
- `protected void lateUpdate()` — Performs class-specific logic; see call sites and owning manager flow.
- `package void internalRender()` — Performs class-specific logic; see call sites and owning manager flow.
- `protected void render()` — Performs class-specific logic; see call sites and owning manager flow.
- `package void internalDispose()` — Performs class-specific logic; see call sites and owning manager flow.
- `protected void dispose()` — Releases owned resources and unregisters state.
- `protected final void debugContext()` — Performs class-specific logic; see call sites and owning manager flow.
- `protected final void debugContext(Object input)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/core/engine/UtilityPackage.java`

**Type:** `class UtilityPackage`
  
**Package:** `program.core.engine`
  
**File size:** 77 lines

**What this class does:** `UtilityPackage` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.core.engine`.

**Method intent:**
- `protected void debug()` — Performs class-specific logic; see call sites and owning manager flow.
- `protected void debug(Object input)` — Performs class-specific logic; see call sites and owning manager flow.
- `protected void timeStampDebug(Object input)` — Performs class-specific logic; see call sites and owning manager flow.
- `protected void log(Object input)` — Performs class-specific logic; see call sites and owning manager flow.
- `protected void errorLog(Object input)` — Performs class-specific logic; see call sites and owning manager flow.
- `protected void timeStampLog(Object input)` — Performs class-specific logic; see call sites and owning manager flow.
- `public <T> T throwException()` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/menupipeline/element/ElementOrigin.java`, `core/src/program/core/backends/lwjgl3/Lwjgl3Application.java`, `core/src/program/core/backends/lwjgl3/Lwjgl3GL.java`, `core/src/program/core/engine/Main.java`, `core/src/program/core/engine/MainEditor.java`.
- `public <T> T throwException(String message)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/menupipeline/element/ElementOrigin.java`, `core/src/program/core/backends/lwjgl3/Lwjgl3Application.java`, `core/src/program/core/backends/lwjgl3/Lwjgl3GL.java`, `core/src/program/core/engine/Main.java`, `core/src/program/core/engine/MainEditor.java`.
- `public <T> T throwException(Throwable cause)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/menupipeline/element/ElementOrigin.java`, `core/src/program/core/backends/lwjgl3/Lwjgl3Application.java`, `core/src/program/core/backends/lwjgl3/Lwjgl3GL.java`, `core/src/program/core/engine/Main.java`, `core/src/program/core/engine/MainEditor.java`.
- `public <T> T throwException(String message, Throwable cause)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/menupipeline/element/ElementOrigin.java`, `core/src/program/core/backends/lwjgl3/Lwjgl3Application.java`, `core/src/program/core/backends/lwjgl3/Lwjgl3GL.java`, `core/src/program/core/engine/Main.java`, `core/src/program/core/engine/MainEditor.java`.
- `public <T> T throwException(Object input)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/menupipeline/element/ElementOrigin.java`, `core/src/program/core/backends/lwjgl3/Lwjgl3Application.java`, `core/src/program/core/backends/lwjgl3/Lwjgl3GL.java`, `core/src/program/core/engine/Main.java`, `core/src/program/core/engine/MainEditor.java`.
- `public <T> T throwException(Object input, Throwable cause)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/menupipeline/element/ElementOrigin.java`, `core/src/program/core/backends/lwjgl3/Lwjgl3Application.java`, `core/src/program/core/backends/lwjgl3/Lwjgl3GL.java`, `core/src/program/core/engine/Main.java`, `core/src/program/core/engine/MainEditor.java`.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/core/engine/WindowPlatform.java`

**Type:** `interface WindowPlatform`
  
**Package:** `program.core.engine`
  
**File size:** 26 lines

**What this class does:** `WindowPlatform` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.core.engine`.

**Who this class talks to (direct imports):**
- `program.core.kernel.window.WindowInstance`

**Method intent:** No concrete methods parsed (interface marker, enum, or data-only declaration).

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.
