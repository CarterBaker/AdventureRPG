package com.AdventureRPG.core.engine;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.AdventureRPG.core.engine.settings.Settings;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Screen;
import com.google.gson.Gson;

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
     * - Root lifecycle propagation to all registered systems
     */

    // Root
    public Game game;
    public Screen screen;
    public File path;
    public Gson gson;

    // Internal
    private Main main;
    private WindowInstance windowInstance;
    private InternalState internalState;

    // System Management
    private Object2ObjectLinkedOpenHashMap<Class<?>, SystemPackage> systemRegistry;
    private List<SystemPackage> garbageCollection;

    // Timing
    private long frameCount;
    private float deltaTime;
    private float fixedInterval;
    private float elapsedTime;
    private int maxSteps;

    // Internal \\

    public EnginePackage(
            Settings setting,
            Main main,
            File path,
            Gson gson) {

        // Core
        this.settings = setting;
        this.internal = this;
        this.local = this;

        // Root
        this.game = main;
        this.screen = null;
        this.path = path;
        this.gson = gson;

        // Internal
        this.main = main;
        this.windowInstance = null;
        this.internalState = InternalState.BOOTSTRAP;

        // System Management
        this.systemRegistry = new Object2ObjectLinkedOpenHashMap<>();
        this.garbageCollection = new ArrayList<>();

        // Timing
        this.frameCount = 0L;
        this.deltaTime = 0f;
        this.fixedInterval = setting.FIXED_TIME_STEP;
        this.elapsedTime = 0f;
        this.maxSteps = 5;
    }

    // Internal State \\

    final InternalState getInternalState() {
        return this.internalState;
    }

    final void setInternalState(InternalState target) {
        this.internalState = target;
    }

    // Internal Context \\

    @Override // From `SystemPackage`
    final void setContext(InternalContext targetContext) {
        this.internalContext = targetContext;
    }

    @Override // From `SystemPackage`
    boolean verifyProcess(InternalContext targetContext) {
        this.setContext(targetContext);
        return true;
    }

    // System Registry \\

    @Override // From `ManagerPackage`
    protected SystemPackage register(SystemPackage subSystem) {

        if (this.getContext() != InternalContext.BOOTSTRAP &&
                this.getContext() != InternalContext.CREATE)
            throwException(
                    "Subsystem registration rejected.\n" +
                            "Attempted during process: " + this.getContext() + "\n" +
                            "Allowed processes: BOOTSTRAP, CREATE");

        return super.internalRegister(subSystem);
    }

    @Override // From `ManagerPackage`
    SystemPackage internalRegister(SystemPackage subSystem) {

        Class<?> type = subSystem.getClass();

        if (this.systemRegistry.containsKey(type))
            throwException("Subsystem: " + type.getSimpleName()
                    + ", Already exists within the internal engine. Only one instance of any given system can exist at a time");

        this.systemRegistry.put(type, subSystem);

        return subSystem;
    }

    // System Release \\

    @Override // From `ManagerPackage`
    void internalRelease(SystemPackage subSystem) {

        if (this.garbageCollection.contains(subSystem))
            return;

        this.garbageCollection.add(subSystem);

        super.internalRelease(subSystem);
    }

    // System Retrieval \\

    @SuppressWarnings("unchecked")
    final <T> T get(boolean bypass, Class<T> type) {

        if (!bypass && this.getContext() != InternalContext.INIT)
            throwException(
                    "Get called outside INIT phase.\n" +
                            "Requested: " + type.getSimpleName() + "\n" +
                            "Current process: " + this.getContext());

        T system = (T) this.systemRegistry.get(type);

        if (system == null)
            throwException(
                    "System not found in registry.\n" +
                            "Requested type: " + type.getSimpleName() + "\n" +
                            "This system either doesn't exist, hasn't been registered yet, or was already released.");

        return system;
    }

    // Game State \\

    final void execute() {
        switch (internalState) {
            case BOOTSTRAP -> this.bootstrapCycle();
            case CREATE -> this.createCycle();
            case START -> this.startCycle();
            case UPDATE -> this.updateCycle();
            case EXIT -> this.exitCycle();
        }
    }

    // Bootstrap Cycle
    private final void bootstrapCycle() {

        this.internalBootstrap();
        this.createGameWindow();

        this.internalInit();
        this.internalAwake();
        this.internalFreeMemory();

        this.setInternalState(InternalState.CREATE);
        this.execute();
    }

    // Create Cycle
    private final void createCycle() {

        this.internalCreate();
        this.internalInit();
        this.internalAwake();
        this.internalFreeMemory();

        this.setInternalState(InternalState.START);
    }

    // Start Cycle
    private final void startCycle() {

        this.internalStart();

        this.setInternalState(InternalState.UPDATE);
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

    // Bootstrap \\

    private final void internalBootstrap() {

        // First make sure to enter the BOOTSTRAP state
        this.setContext(InternalContext.BOOTSTRAP);

        // Only call OUR bootstrap, not super
        this.bootstrap();

        // Cache any systems registered during bootstrap
        this.cacheSubSystems();

        // Now we can enter CREATE and walk through create for all systems
        this.setContext(InternalContext.CREATE);

        for (int i = 0; i < this.systemArray.length; i++)
            this.systemArray[i].internalCreate();
    }

    // Internal engine specific. SystemPackages do not contain `bootstrap`.
    void bootstrap() {
    }

    // Create \\

    @Override // From `ManagerPackage`
    protected final void internalCreate() {

        this.setContext(InternalContext.CREATE);

        super.internalCreate();
    }

    // Init \\

    @Override // From `ManagerPackage`
    protected final void internalInit() {

        this.setContext(InternalContext.INIT);

        super.internalInit();
    }

    // Awake \\

    @Override // From `ManagerPackage`
    protected final void internalAwake() {

        this.setContext(InternalContext.AWAKE);

        super.internalAwake();
    }

    // Free Memory \\

    @Override // From `ManagerPackage`
    protected final void internalFreeMemory() {

        this.setContext(InternalContext.FREE_MEMORY);

        super.internalFreeMemory();
    }

    // Start \\

    @Override // From `ManagerPackage`
    protected final void internalStart() {

        this.setContext(InternalContext.START);

        super.internalStart();
    }

    // Update \\

    @Override // From `ManagerPackage`
    protected final void internalUpdate() {

        this.setContext(InternalContext.UPDATE);

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

            this.setContext(InternalContext.FIXED_UPDATE);

            super.internalFixedUpdate();
        }
    }

    // Late Update \\

    @Override // From `ManagerPackage`
    protected final void internalLateUpdate() {

        this.setContext(InternalContext.LATE_UPDATE);

        super.internalLateUpdate();
    }

    // Render \\

    @Override // From `ManagerPackage`
    protected final void internalRender() {

        this.setContext(InternalContext.RENDER);

        super.internalRender();
    }

    // Draw \\

    private final void internalDraw() {

        this.setContext(InternalContext.DRAW);

        this.setFrameCount();

        this.draw();
    }

    // Internal engine specific. SystemPackages do not contain `draw`.
    void draw() {
    }

    // Dispose \\

    @Override // From `ManagerPackage`
    protected final void internalDispose() {

        this.setContext(InternalContext.DISPOSE);

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

    @Override // From `ManagerPackage`
    final void clearGarbage() {

        if (this.garbageCollection.isEmpty())
            return;

        for (SystemPackage target : this.garbageCollection)
            this.systemRegistry.remove(target.getClass());

        this.garbageCollection.clear();

        super.clearGarbage();
    }

    // Accessible \\

    public final InternalState getState() {
        return this.internalState;
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
