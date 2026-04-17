package engine.root;

import java.io.File;
import java.time.Instant;
import java.util.concurrent.Future;

import com.google.gson.Gson;

import application.kernel.threadpipeline.syncconsumer.AsyncStructConsumer;
import application.kernel.threadpipeline.syncconsumer.AsyncStructConsumerMulti;
import application.kernel.threadpipeline.syncconsumer.BiSyncAsyncConsumer;
import application.kernel.threadpipeline.syncconsumer.SyncStructConsumer;
import application.kernel.threadpipeline.thread.ThreadHandle;
import application.kernel.windowpipeline.window.WindowData;
import application.kernel.windowpipeline.window.WindowInstance;
import engine.assets.camera.CameraInstance;
import engine.assets.camera.OrthographicCameraInstance;
import engine.settings.Settings;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class EnginePackage extends ManagerPackage {

    /*
     * Root of the system hierarchy. Drives the core game loop, global state
     * machine, and master system registry. Owns bootstrap, context management,
     * frame timing, and lifecycle propagation to all registered systems.
     */

    // Core
    static final ThreadLocal<EngineStruct> ENGINE_STRUCT = new ThreadLocal<>();

    // Root
    public final File path;
    public final Gson gson;
    public final WindowPlatform windowPlatform;

    // Internal
    private EngineState engineState;

    // System Management
    Object2ObjectLinkedOpenHashMap<Class<?>, SystemPackage> internalRegistry;

    // Context Management
    private ObjectArrayList<ContextPackage> pendingContextList;
    private ObjectArrayList<ContextPackage> activeContextList;
    private ContextPackage[] contextArray;

    // Timing
    private long frameCount;
    private float deltaTime;
    private float fixedInterval;
    private float elapsedTime;
    private int maxSteps;
    private long frameTimeMillis;

    // Internal \\

    EnginePackage() {

        super(ENGINE_STRUCT.get() != null ? ENGINE_STRUCT.get().settings : null);

        EngineStruct data = ENGINE_STRUCT.get();

        if (data == null)
            throwException("Engine must be created via EnginePackage.setupConstructor()");

        // Root
        this.path = data.path;
        this.gson = data.gson;
        this.windowPlatform = data.windowPlatform;

        // Internal
        this.engineState = EngineState.KERNEL;
        EngineUtility.engine = this;

        // System Management
        this.internalRegistry = new Object2ObjectLinkedOpenHashMap<>();

        // Context Management
        this.pendingContextList = new ObjectArrayList<>();
        this.activeContextList = new ObjectArrayList<>();
        this.contextArray = new ContextPackage[0];

        // Timing
        this.fixedInterval = EngineSetting.FIXED_TIME_STEP;
        this.maxSteps = 5;
    }

    static final class EngineStruct extends StructPackage {

        /*
         * Carries all root references through to EnginePackage during
         * construction. Bypasses constructor timing issues in one shot.
         */

        // Internal
        final Settings settings;
        final File path;
        final Gson gson;
        final WindowPlatform windowPlatform;

        // Internal \\

        EngineStruct(
                Settings settings,
                File path,
                Gson gson,
                WindowPlatform windowPlatform) {

            // Identity
            this.settings = settings;
            this.path = path;
            this.gson = gson;
            this.windowPlatform = windowPlatform;
        }
    }

    public static void setupConstructor(
            Settings settings,
            File path,
            Gson gson,
            WindowPlatform windowPlatform) {
        ENGINE_STRUCT.set(
                new EngineStruct(
                        settings,
                        path,
                        gson,
                        windowPlatform));
    }

    // Engine State \\

    final EngineState getEngineState() {
        return this.engineState;
    }

    final void setInternalState(EngineState target) {
        this.engineState = target;
    }

    // System Context \\

    @Override
    final void setContext(SystemContext targetContext) {
        this.internalContext = targetContext;
    }

    @Override
    boolean verifyContext(SystemContext targetContext) {
        this.setContext(targetContext);
        return true;
    }

    // System Registry \\

    protected <T extends SystemPackage> T registerSystem(T systemPackage) {
        this.internalRegistry.put(systemPackage.getClass(), systemPackage);
        this.systemCollection.add(systemPackage);
        return systemPackage;
    }

    // System Release \\

    @Override
    final void clearGarbage() {

        if (this.garbageCollection.isEmpty())
            return;

        for (Class<?> systemClass : garbageCollection) {
            SystemPackage systemPackage = this.internal.internalRegistry.get(systemClass);
            this.internalRegistry.remove(systemClass);
            this.systemCollection.remove(systemPackage);
        }

        this.garbageCollection.clear();
        this.cacheSubSystems();
    }

    // System Retrieval \\

    @SuppressWarnings("unchecked")
    final <T> T get(boolean bypass, Class<T> type) {

        if (!bypass && this.getContext() != SystemContext.GET)
            throwException(
                    "Get called outside GET phase.\n" +
                            "Requested: " + type.getSimpleName() + "\n" +
                            "Current process: " + this.getContext());

        T systemPackage = (T) this.internalRegistry.get(type);

        if (systemPackage == null)
            throwException(
                    "System not found in registry.\n" +
                            "Requested type: " + type.getSimpleName() + "\n" +
                            "This system either doesn't exist, hasn't been created yet, or was already released.");

        return systemPackage;
    }

    public final <T> T getUnchecked(Class<T> type) {
        return get(true, type);
    }

    // Context Management \\

    public <T extends ContextPackage> T createContext(Class<T> contextClass, WindowInstance window) {

        if (window == null)
            throwException("createContext() requires a WindowInstance — null was passed.");

        if (window.hasContext())
            throwException(
                    "Window is already paired with a context.\n" +
                            "Window ID: " + window.getWindowID() + "\n" +
                            "Existing context: " + window.getContext().getClass().getSimpleName());

        try {
            SystemPackage.setupConstructor(this.settings, this, this);

            var constructor = contextClass.getDeclaredConstructor();
            T context = constructor.newInstance();

            context.pendingStart = true;
            context.setWindow(window);
            window.setContext(context);

            this.pendingContextList.add(context);
            this.windowPlatform.makeContextCurrent(window);

            primeContextLifecycle(context);

            return context;
        } catch (Exception e) {
            throw new InternalException("Failed to create context: " + contextClass.getSimpleName(), e);
        } finally {
            this.windowPlatform.restoreMainContext();
            SystemPackage.SYSTEM_STRUCT.remove();
        }
    }

    private void primeContextLifecycle(ContextPackage context) {

        SystemContext previousContext = this.internalContext;
        this.windowPlatform.makeContextCurrent(context.getWindow());
        EngineUtility.windowManager.beginContextWindow(context.getWindow());

        try {
            this.internalContext = SystemContext.CREATE;
            context.internalCreate();

            this.internalContext = SystemContext.GET;
            context.internalGet();

            this.internalContext = SystemContext.AWAKE;
            context.internalAwake();

            this.internalContext = SystemContext.RELEASE;
            context.internalRelease();
        }

        finally {
            EngineUtility.windowManager.endContextWindow();
            this.windowPlatform.restoreMainContext();
            this.internalContext = previousContext;
        }
    }

    public void destroyContext(ContextPackage context) {

        context.internalDispose();

        WindowInstance window = context.getWindow();
        if (window != null)
            window.setContext(null);

        this.pendingContextList.remove(context);
        this.activeContextList.remove(context);
        this.cacheContextArray();
    }

    private void flushPendingContexts() {

        if (this.pendingContextList.isEmpty())
            return;

        for (int i = 0; i < this.pendingContextList.size(); i++) {

            ContextPackage context = this.pendingContextList.get(i);
            context.pendingStart = false;
            this.windowPlatform.makeContextCurrent(context.getWindow());
            EngineUtility.windowManager.beginContextWindow(context.getWindow());

            try {
                context.internalStart();
            }

            finally {
                EngineUtility.windowManager.endContextWindow();
                this.windowPlatform.restoreMainContext();
            }

            this.activeContextList.add(context);
        }

        this.pendingContextList.clear();
        this.cacheContextArray();
    }

    private void cacheContextArray() {
        this.contextArray = this.activeContextList.toArray(new ContextPackage[0]);
    }

    // Thread Management \\

    @Override
    protected final ThreadHandle getThreadHandleFromThreadName(String threadName) {
        return EngineUtility.getThreadHandle(threadName);
    }

    // Submit \\

    @Override
    protected final Future<?> executeAsync(ThreadHandle handle, Runnable task) {
        return EngineUtility.executeAsync(handle, task);
    }

    @Override
    protected final <T extends AsyncContainerPackage> Future<?> executeAsync(
            ThreadHandle handle,
            T asyncStruct,
            AsyncStructConsumer<T> consumer) {
        return EngineUtility.executeAsync(handle, asyncStruct, consumer);
    }

    @Override
    protected final Future<?> executeAsync(
            ThreadHandle handle,
            AsyncStructConsumerMulti consumer,
            AsyncContainerPackage... asyncStructs) {
        return EngineUtility.executeAsync(handle, consumer, asyncStructs);
    }

    @Override
    protected final <T extends SyncContainerPackage> Future<?> executeAsync(
            ThreadHandle handle,
            T syncStruct,
            SyncStructConsumer<T> consumer) {
        return EngineUtility.executeAsync(handle, syncStruct, consumer);
    }

    @Override
    protected final <T extends AsyncContainerPackage, S extends SyncContainerPackage> Future<?> executeAsync(
            ThreadHandle handle,
            T asyncStruct,
            S syncStruct,
            BiSyncAsyncConsumer<T, S> consumer) {
        return EngineUtility.executeAsync(handle, asyncStruct, syncStruct, consumer);
    }

    // Game State \\

    public final void execute(float delta) {
        this.deltaTime = delta;
        execute();
    }

    private void execute() {
        switch (engineState) {
            case KERNEL -> this.kernelCycle();
            case BOOTSTRAP -> this.bootstrapCycle();
            case CREATE -> this.createCycle();
            case START -> this.startCycle();
            case UPDATE -> this.updateCycle();
            case EXIT -> this.exitCycle();
        }
    }

    private final void kernelCycle() {
        this.internalKernel();
        preFrameCycle(EngineState.BOOTSTRAP);
    }

    private final void bootstrapCycle() {
        this.internalBootstrap();
        this.createGameWindow();
        preFrameCycle(EngineState.CREATE);
    }

    private final void preFrameCycle(EngineState nextState) {
        this.preGet();
        this.preAwake();
        this.preRelease();
        this.setInternalState(nextState);
        this.execute();
    }

    private final void createCycle() {
        this.internalCreate();
        this.internalGet();
        this.internalAwake();
        this.internalRelease();
        this.setInternalState(EngineState.START);
    }

    private final void startCycle() {
        this.internalStart();
        this.setInternalState(EngineState.UPDATE);
    }

    private final void updateCycle() {
        this.flushPendingContexts();
        this.internalUpdate();
        this.internalFixedUpdate();
        this.internalLateUpdate();
        this.internalRender();
        this.internalDraw();
    }

    private final void exitCycle() {
        this.internalDispose();
    }

    // Kernel \\

    private void internalKernel() {

        this.setContext(SystemContext.KERNEL);

        kernel();

        this.cacheSubSystems();

        this.setContext(SystemContext.CREATE);

        for (int i = 0; i < this.systemArray.length; i++)
            this.systemArray[i].internalCreate();
    }

    protected void kernel() {
    }

    // Bootstrap \\

    private final void internalBootstrap() {

        this.setContext(SystemContext.BOOTSTRAP);

        this.bootstrap();

        this.cacheSubSystems();

        this.setContext(SystemContext.CREATE);

        for (int i = 0; i < this.systemArray.length; i++)
            this.systemArray[i].internalCreate();
    }

    protected void bootstrap() {
    }

    // Create \\

    @Override
    protected final void internalCreate() {
        this.setContext(SystemContext.CREATE);
        super.internalCreate();
    }

    // Get \\

    private final void preGet() {

        this.setContext(SystemContext.GET);

        for (int i = 0; i < this.systemArray.length; i++)
            this.systemArray[i].internalGet();
    }

    @Override
    protected final void internalGet() {
        this.setContext(SystemContext.GET);
        super.internalGet();
    }

    // Awake \\

    private final void preAwake() {

        this.setContext(SystemContext.AWAKE);

        for (int i = 0; i < this.systemArray.length; i++)
            this.systemArray[i].internalAwake();
    }

    @Override
    protected final void internalAwake() {
        this.setContext(SystemContext.AWAKE);
        super.internalAwake();
    }

    // Release \\

    private final void preRelease() {

        this.setContext(SystemContext.RELEASE);

        for (int i = 0; i < this.systemArray.length; i++)
            this.systemArray[i].internalRelease();

        this.clearGarbage();
    }

    @Override
    protected final void internalRelease() {
        this.setContext(SystemContext.RELEASE);
        super.internalRelease();
    }

    // Start \\

    @Override
    protected final void internalStart() {
        this.setContext(SystemContext.START);
        super.internalStart();
    }

    // Update \\

    @Override
    protected final void internalUpdate() {

        this.frameTimeMillis = Instant.now().toEpochMilli();

        this.setContext(SystemContext.UPDATE);

        super.internalUpdate();

        for (int i = 0; i < this.contextArray.length; i++) {
            EngineUtility.windowManager.beginContextWindow(this.contextArray[i].getWindow());
            this.contextArray[i].internalUpdate();
        }

        EngineUtility.windowManager.endContextWindow();
    }

    // Fixed Update \\

    @Override
    protected final void internalFixedUpdate() {

        this.elapsedTime += deltaTime;
        int steps = 0;

        while (this.elapsedTime >= this.fixedInterval && steps < this.maxSteps) {

            this.elapsedTime -= fixedInterval;
            steps++;

            this.setContext(SystemContext.FIXED_UPDATE);

            super.internalFixedUpdate();

            for (int i = 0; i < this.contextArray.length; i++) {
                EngineUtility.windowManager.beginContextWindow(this.contextArray[i].getWindow());
                this.contextArray[i].internalFixedUpdate();
            }

            EngineUtility.windowManager.endContextWindow();
        }
    }

    // Late Update \\

    @Override
    protected final void internalLateUpdate() {

        this.setContext(SystemContext.LATE_UPDATE);

        super.internalLateUpdate();

        for (int i = 0; i < this.contextArray.length; i++) {
            EngineUtility.windowManager.beginContextWindow(this.contextArray[i].getWindow());
            this.contextArray[i].internalLateUpdate();
        }

        EngineUtility.windowManager.endContextWindow();
    }

    // Render \\

    @Override
    protected final void internalRender() {

        this.setContext(SystemContext.RENDER);

        super.internalRender();

        for (int i = 0; i < this.contextArray.length; i++) {
            EngineUtility.windowManager.beginContextWindow(this.contextArray[i].getWindow());
            this.contextArray[i].internalRender();
        }

        EngineUtility.windowManager.endContextWindow();
    }

    // Draw \\

    private final void internalDraw() {
        this.setContext(SystemContext.DRAW);
        this.setFrameCount();
        this.draw();
    }

    protected void draw() {
    }

    // Dispose \\

    @Override
    protected final void internalDispose() {

        this.setContext(SystemContext.DISPOSE);

        for (int i = 0; i < this.contextArray.length; i++)
            this.contextArray[i].internalDispose();

        super.internalDispose();
    }

    // Utility \\

    private final void setFrameCount() {
        this.frameCount++;
    }

    private final void createGameWindow() {

        WindowInstance mainWindow = create(WindowInstance.class);
        mainWindow.constructor(new WindowData(
                0,
                settings.windowWidth,
                settings.windowHeight,
                EngineSetting.WINDOW_TITLE));

        EngineUtility.windowManager.registerMainWindow(mainWindow);
    }

    // Camera \\

    public final CameraInstance createCamera(float fov, float width, float height) {
        return EngineUtility.createCamera(fov, width, height);
    }

    public final OrthographicCameraInstance createOrthographicCamera(float width, float height) {
        return EngineUtility.createOrthographicCamera(width, height);
    }

    // Accessible \\

    public final EngineState getState() {
        return this.engineState;
    }

    public final long getFrameCount() {
        return this.frameCount;
    }

    public final float getDeltaTime() {
        return this.deltaTime;
    }

    public final long getTime() {
        return this.frameTimeMillis;
    }

    public final void closeGame() {
        this.windowPlatform.exit();
    }

    public final void shutdown() {
        this.engineState = EngineState.EXIT;
        execute(0f);
    }
}