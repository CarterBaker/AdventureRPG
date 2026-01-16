package com.internal.core.engine;

import java.io.File;
import java.util.concurrent.Future;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Screen;
import com.google.gson.Gson;
import com.internal.core.engine.InternalThreadManager.AsyncStructConsumer;
import com.internal.core.engine.InternalThreadManager.AsyncStructConsumerMulti;
import com.internal.core.engine.settings.Settings;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;

public class EnginePackage extends ManagerPackage {

    /*
     * This is the main class that drives the entire game engine.
     * EnginePackage serves as the root of the system hierarchy and
     * manages the core game loop, global state machine, and master
     * system registry. It contains a built-in bootstrap phase to
     * ensure necessary systems are initialized before standard
     * lifecycle operations begin.
     *
     * Key responsibilities:
     * - Global system registry and retrieval
     * - State machine execution (BOOTSTRAP → CREATE → START → UPDATE → EXIT)
     * - Frame timing and fixed timestep updates
     * - Root lifecycle propagation to all created systems
     */

    // Core
    static final ThreadLocal<EngineStruct> ENGINE_STRUCT = new ThreadLocal<>();

    // Root
    public final Game game;
    public final File path;
    public final Gson gson;

    // Internal
    private Main main;
    private Screen screen;
    private WindowInstance windowInstance;
    private EngineState engineState;
    private InternalThreadManager internalThreadManager;

    // System Management
    Object2ObjectLinkedOpenHashMap<Class<?>, SystemPackage> internalRegistry;

    // Timing
    private long frameCount;
    private float deltaTime;
    private float fixedInterval;
    private float elapsedTime;
    private int maxSteps;

    // Internal \\

    EnginePackage() {

        // Internal Manager Package
        super(ENGINE_STRUCT.get() != null ? ENGINE_STRUCT.get().settings : null);

        // Core
        EngineStruct data = ENGINE_STRUCT.get();

        if (data == null)
            throwException("Engine must be created via EnginePackage.setupConstructor()");

        // Root
        this.game = data.game;
        this.path = data.path;
        this.gson = data.gson;

        // Internal
        this.main = data.main;
        this.screen = null;
        this.windowInstance = null;
        this.engineState = EngineState.KERNEL;

        // System Management
        this.internalRegistry = new Object2ObjectLinkedOpenHashMap<>();

        // Timing
        this.frameCount = 0L;
        this.deltaTime = 0f;
        this.fixedInterval = data.settings.FIXED_TIME_STEP;
        this.elapsedTime = 0f;
        this.maxSteps = 5;
    }

    static final class EngineStruct extends StructPackage {

        /*
         * A container used to ensure proper engine creation from `Main`. Used
         * to bypass constructor related timing issues and to ensure that things
         * settings are able to be called easily without using a get method.
         */

        // Internal
        final Settings settings;
        final Main main;
        final Game game;
        final File path;
        final Gson gson;

        // Internal \\

        EngineStruct(
                Settings settings,
                Main main,
                File path,
                Gson gson) {

            // Internal
            this.settings = settings;
            this.main = main;
            this.game = main;
            this.path = path;
            this.gson = gson;
        }
    }

    public static void setupConstructor(
            Settings settings,
            Main main,
            File path,
            Gson gson) {
        ENGINE_STRUCT.set(
                new EngineStruct(
                        settings,
                        main,
                        path,
                        gson));
    }

    // Engine State \\

    final EngineState getEngineState() {
        return this.engineState;
    }

    final void setInternalState(EngineState target) {
        this.engineState = target;
    }

    // System Context \\

    @Override // From `SystemPackage`
    final void setContext(SystemContext targetContext) {
        this.internalContext = targetContext;
    }

    @Override // From `SystemPackage`
    boolean verifyContext(SystemContext targetContext) {
        this.setContext(targetContext);
        return true;
    }

    // System Registry \\

    @SuppressWarnings("unchecked")
    protected <T extends SystemPackage> T registerSystem(T systemPackage) {

        this.internalRegistry.put(systemPackage.getClass(), systemPackage);
        this.systemCollection.add(systemPackage);

        return systemPackage;
    }

    // System Release \\

    @Override // From `ManagerPackage`
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

    // Thread Management \\

    @Override
    protected final ThreadHandle getThreadHandleFromThreadName(String threadName) {
        return internalThreadManager.getThreadHandleFromThreadName(threadName);
    }

    // Submit \\

    @Override
    protected final Future<?> executeAsync(ThreadHandle handle, Runnable task) {
        return internalThreadManager.executeAsync(handle, task);
    }

    @Override
    protected final <T extends AsyncStructPackage> Future<?> executeAsync(
            ThreadHandle handle,
            AsyncStructPackage asyncStruct,
            AsyncStructConsumer<T> consumer) {

        return internalThreadManager.executeAsync(handle, asyncStruct, consumer);
    }

    @Override
    protected final Future<?> executeAsync(
            ThreadHandle handle,
            AsyncStructConsumerMulti consumer,
            AsyncStructPackage... asyncStructs) {

        return internalThreadManager.executeAsync(handle, consumer, asyncStructs);
    }

    // Game State \\

    final void execute() {
        switch (engineState) {
            case KERNEL -> this.kernelCycle();
            case BOOTSTRAP -> this.bootstrapCycle();
            case CREATE -> this.createCycle();
            case START -> this.startCycle();
            case UPDATE -> this.updateCycle();
            case EXIT -> this.exitCycle();
        }
    }

    // Kernel Cycle
    private final void kernelCycle() {

        this.internalKernel();

        preFrameCycle(EngineState.BOOTSTRAP);
    }

    // Bootstrap Cycle
    private final void bootstrapCycle() {

        this.internalBootstrap();

        this.createGameWindow();

        preFrameCycle(EngineState.CREATE);
    }

    private final void preFrameCycle(EngineState nextState) {

        this.internalGet();
        this.internalAwake();
        this.internalRelease();

        this.setInternalState(nextState);
        this.execute();
    }

    // Create Cycle
    private final void createCycle() {

        this.internalCreate();
        this.internalGet();
        this.internalAwake();
        this.internalRelease();

        this.setInternalState(EngineState.START);
    }

    // Start Cycle
    private final void startCycle() {

        this.internalStart();

        this.setInternalState(EngineState.UPDATE);
    }

    // Update Cycle
    private final void updateCycle() {
        this.internalUpdate();
        this.internalFixedUpdate();
        this.internalLateUpdate();
        this.internalRender();
        this.internalDraw();
    }

    // Exit Cycle
    private final void exitCycle() {
        this.internalDispose();
    }

    // Kernel \\

    private void internalKernel() {

        // First make sure to enter the KERNEL state
        this.setContext(SystemContext.KERNEL);

        kernel();

        // Cache any systems created during KERNEL
        this.cacheSubSystems();

        // Now we can enter BOOTSTRAP and walk through create for all systems
        this.setContext(SystemContext.CREATE);

        for (int i = 0; i < this.systemArray.length; i++)
            this.systemArray[i].internalCreate();
    }

    private final void kernel() {

        // Kernel Systems
        this.internalThreadManager = create(InternalThreadManager.class);
    }

    // Bootstrap \\

    private final void internalBootstrap() {

        // First make sure to enter the BOOTSTRAP state
        this.setContext(SystemContext.BOOTSTRAP);

        // Only call OUR bootstrap, not super
        this.bootstrap();

        // Cache any systems created during bootstrap
        this.cacheSubSystems();

        // Now we can enter CREATE and walk through create for all systems
        this.setContext(SystemContext.CREATE);

        for (int i = 0; i < this.systemArray.length; i++)
            this.systemArray[i].internalCreate();
    }

    // Internal engine specific. SystemPackages do not contain `bootstrap`.
    void bootstrap() {
    }

    // Create \\

    @Override // From `ManagerPackage`
    protected final void internalCreate() {

        this.setContext(SystemContext.CREATE);

        super.internalCreate();
    }

    // Get \\

    @Override // From `ManagerPackage`
    protected final void internalGet() {

        this.setContext(SystemContext.GET);

        super.internalGet();
    }

    // Awake \\

    @Override // From `ManagerPackage`
    protected final void internalAwake() {

        this.setContext(SystemContext.AWAKE);

        super.internalAwake();
    }

    // Release \\

    @Override // From `ManagerPackage`
    protected final void internalRelease() {

        this.setContext(SystemContext.RELEASE);

        super.internalRelease();
    }

    // Start \\

    @Override // From `ManagerPackage`
    protected final void internalStart() {

        this.setContext(SystemContext.START);

        super.internalStart();
    }

    // Update \\

    @Override // From `ManagerPackage`
    protected final void internalUpdate() {

        this.setContext(SystemContext.UPDATE);

        super.internalUpdate();
    }

    // Fixed Update \\

    @Override // From `ManagerPackage`
    protected final void internalFixedUpdate() {

        this.elapsedTime += deltaTime;
        int steps = 0;

        while (this.elapsedTime >= this.fixedInterval && steps < this.maxSteps) {

            this.elapsedTime -= fixedInterval;
            steps++;

            this.setContext(SystemContext.FIXED_UPDATE);

            super.internalFixedUpdate();
        }
    }

    // Late Update \\

    @Override // From `ManagerPackage`
    protected final void internalLateUpdate() {

        this.setContext(SystemContext.LATE_UPDATE);

        super.internalLateUpdate();
    }

    // Render \\

    @Override // From `ManagerPackage`
    protected final void internalRender() {

        this.setContext(SystemContext.RENDER);

        super.internalRender();
    }

    // Draw \\

    private final void internalDraw() {

        this.setContext(SystemContext.DRAW);

        this.setFrameCount();

        this.draw();
    }

    // Internal engine specific. SystemPackages do not contain `draw`.
    void draw() {
    }

    // Dispose \\

    @Override // From `ManagerPackage`
    protected final void internalDispose() {

        this.setContext(SystemContext.DISPOSE);

        super.internalDispose();
    }

    // Utility \\

    private final void setFrameCount() {
        this.frameCount++;
    }

    final void setDeltaTime(float delta) {
        this.deltaTime = delta;
    }

    private final void createGameWindow() {

        this.windowInstance = create(WindowInstance.class);

        this.screen = windowInstance;
        this.game.setScreen(screen);
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

    public final Main getMain() {
        return this.main;
    }

    public final WindowInstance getWindowInstance() {
        return this.windowInstance;
    }
}
