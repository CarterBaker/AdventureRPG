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
     * This is the main class that drives the entire game, the
     * purpose is mainly to serve as a data container for all
     * core pipelines, including a built in pre-operating bootStrap
     * to make sure necessary systems are in place before game
     * operations run. Similar to the ManagerPacket each stage
     * runs for each registered system in order one stage at a
     * time.
     *
     * i.e. All systems run through create()
     * then all systems run through init() > awake() > ect.
     */

    // Root
    public Game game;
    public Screen screen;
    public File path;
    public Gson gson;

    // Internal
    private Main main;
    private WindowInstance windowInstance;

    InternalState internalState;

    private List<SystemPackage> bootStrapTree;
    private SystemPackage[] bootStrapArray;

    private Object2ObjectLinkedOpenHashMap<Class<?>, SystemPackage> systemRegistry;
    private List<SystemPackage> garbageCollection;

    private long frameCount;
    private float deltaTime;

    private float fixedInterval;
    private float elapsedTime;
    private int maxSteps;

    public EnginePackage(
            Settings setting,
            Main main,
            File path,
            Gson gson) {

        // Internal
        this.settings = setting;
        this.internal = this;
        this.local = this;

        this.game = main;
        this.screen = null;
        this.path = path;
        this.gson = gson;

        this.main = main;
        this.windowInstance = null;

        this.internalState = InternalState.CONSTRUCTOR;

        this.bootStrapTree = new ArrayList<>();
        this.bootStrapArray = new SystemPackage[0];

        this.systemRegistry = new Object2ObjectLinkedOpenHashMap<>();
        this.garbageCollection = new ArrayList<>();

        this.frameCount = 0l;
        this.deltaTime = 0f;

        this.fixedInterval = settings.FIXED_TIME_STEP;
        this.elapsedTime = 0f;
        this.maxSteps = 5;
    }

    // Internal State \\

    final InternalState getInternalState() {
        return internalState;
    }

    final void setInternalState(InternalState target) {
        this.internalState = target;
    }

    public final void requestInternalState(InternalState target) {

        if (!target.accessible)
            throwException(
                    "Engine state transition denied.\n" +
                            "Target state: " + target + "\n" +
                            "Reason: target state is not accessible from the current engine engineContext.\n" +
                            "Current state: " + internalState);

        this.setInternalState(target);
    }

    // Internal Context \\

    @Override // From `SystemPackage`
    final void setContext(InternalContext targetContext) {
        this.internalContext = targetContext;
    }

    @Override // From `SystemPackage`
    boolean verifyProcess(InternalContext targetContext) {

        if (!targetContext.canEnterFrom(this.internalContext.order))
            return false;

        this.setContext(targetContext);
        return true;
    }

    // BootStrap Registry \\

    @Override // From `ManagerPackage`
    SystemPackage internalRegister(SystemPackage subSystem) {

        if (this.getContext() != InternalContext.BOOTSTRAP &&
                this.getContext() != InternalContext.CREATE)
            throwException(
                    "Subsystem registration rejected.\n" +
                            "Attempted during process: " + getContext() + "\n" +
                            "Allowed processes: BOOTSTRAP, CREATE\n" +
                            "Rule: subsystems may only be registered during bootStrap boot or creation.");

        if (this.getContext() == InternalContext.CREATE)
            return super.internalRegister(subSystem);

        if (this.bootStrapTree.contains(subSystem))
            throwException(
                    "Duplicate subsystem detected.\n" +
                            "Subsystem type: " + subSystem.getClass().getSimpleName() + "\n" +
                            "Rule: only one instance of a given subsystem may exist within the engine bootStrap.");

        this.bootStrapTree.add(subSystem);
        this.registerToInternalManager(subSystem);

        return subSystem;
    }

    private final void registerToInternalManager(SystemPackage subSystem) {

        this.setContext(InternalContext.CREATE);

        super.internalRegister(subSystem);

        this.setContext(InternalContext.BOOTSTRAP);
    }

    // System Registry \\

    final void registerToSystemRegistry(SystemPackage system) {

        Class<?> type = system.getClass();

        if (systemRegistry.containsKey(type))
            throwException("Subsystem: " + type.getSimpleName()
                    + ", Already exists within the internal engine. Only one instance of any given system can exist at a time");

        systemRegistry.put(type, system);
    }

    // System Release \\

    @Override // From `ManagerPackage`
    void internalRelease(SystemPackage subSystem) {

        if (this.getContext() != InternalContext.FREE_MEMORY)
            throwException(
                    "Release method was called from a process other than free memory. Current process: "
                            + this.getContext());

        if (subSystem instanceof EnginePackage)
            throwException(
                    "Release call was attempted on the game engine itself. This is not allowed under any circumstance");

        if (this.garbageCollection.contains(subSystem))
            return;

        this.garbageCollection.add(subSystem);

        super.internalRelease(subSystem);
    }

    // System Retrieval \\

    @SuppressWarnings("unchecked")
    final <T> T get(boolean bypass, Class<T> type) {

        if (!bypass && getContext() != InternalContext.INIT)
            throwException(
                    "Get called outside INIT phase.\n" +
                            "Requested: " + type.getSimpleName() + "\n" +
                            "Current process: " + getContext());

        T system = (T) systemRegistry.get(type);

        if (system == null)
            throwException(
                    "System not found in registry.\n" +
                            "Requested type: " + type.getSimpleName() + "\n" +
                            "This system either doesn't exist, hasn't been registered yet, or was already released.");

        return system;
    }

    // Game State \\

    void updateInternalState() {
        switch (internalState) {
            case CONSTRUCTOR -> bootState();
            case FIRST_FRAME -> startState();
            case MENU_EXCLUSIVE -> menuState();
            case GAME_EXCLUSIVE -> gameState();
            case EXIT -> exitState();
        }
    }

    // Boot State
    private void bootState() {

        internalBootStrap();
        internalCreate();
        internalInit();
        internalAwake();
        internalFreeMemory();

        setInternalState(InternalState.FIRST_FRAME);
    }

    // Start State
    private void startState() {

        internalStart();

        // TODO
        /*
         * Eventually I want the game to just naturally be in the
         * 'GAME_EXCLUSIVE' state. For now I have bigger concerns so
         * this is being added tot he todo list and I will have to
         * circle back when the time comes.
         */

        setInternalState(InternalState.MENU_EXCLUSIVE); // TODO
    }

    // Menu State
    private void menuState() {
        internalUpdate();
        internalMenuExclusiveUpdate();
        internalFixedUpdate();
        internalLateUpdate();
        internalRender();
        internalDraw();
    }

    // Game State
    private void gameState() {
        internalUpdate();
        internalGameExclusiveUpdate();
        internalFixedUpdate();
        internalLateUpdate();
        internalRender();
        internalDraw();
    }

    // Exit State
    private void exitState() {
        internalDispose();
    }

    // BootStrap \\

    private void internalBootStrap() {
        this.beginBoot();
        this.preCreate();
        this.preInit();
        this.preAwake();
        this.preFreeMemory();
        this.finalizeBoot();
    }

    // Begin
    private final void beginBoot() {

        this.setContext(InternalContext.BOOTSTRAP);

        this.bootStrap();
        this.cacheBootStrap();
    }

    protected void bootStrap() {
    }

    private final void cacheBootStrap() {

        this.bootStrapArray = this.bootStrapTree.toArray(new SystemPackage[0]);
        this.bootStrapTree.clear();
    }

    // Create
    private final void preCreate() {

        this.setContext(InternalContext.CREATE);

        for (int i = 0; i < this.bootStrapArray.length; i++)
            this.bootStrapArray[i].internalCreate();

        this.createGameWindow();
    }

    // Init
    private final void preInit() {

        this.setContext(InternalContext.INIT);

        for (int i = 0; i < this.bootStrapArray.length; i++)
            this.bootStrapArray[i].internalInit();
    }

    // Awake
    private final void preAwake() {

        this.setContext(InternalContext.AWAKE);

        for (int i = 0; i < this.bootStrapArray.length; i++)
            this.bootStrapArray[i].internalAwake();
    }

    // Free Memory
    private final void preFreeMemory() {

        this.setContext(InternalContext.FREE_MEMORY);

        for (int i = 0; i < this.bootStrapArray.length; i++)
            this.bootStrapArray[i].internalFreeMemory();
    }

    // Finalize
    private final void finalizeBoot() {

        this.bootStrapTree.clear();
        this.bootStrapArray = new SystemPackage[0];

        this.setContext(InternalContext.CREATE);
    }

    // Create \\

    @Override // From `SystemPackage`
    protected final void internalCreate() {

        this.setContext(InternalContext.CREATE);

        super.internalCreate();
    }

    // Init \\

    @Override // From `SystemPackage`
    protected final void internalInit() {

        this.setContext(InternalContext.INIT);

        super.internalInit();
    }

    // Awake \\

    @Override // From `SystemPackage`
    protected final void internalAwake() {

        this.setContext(InternalContext.AWAKE);

        super.internalAwake();
    }

    // Free Memory \\

    @Override // From `SystemPackage`
    protected final void internalFreeMemory() {

        this.setContext(InternalContext.FREE_MEMORY);

        super.internalFreeMemory();
    }

    // Start \\

    @Override // From `SystemPackage`
    protected final void internalStart() {

        this.setContext(InternalContext.START);

        super.internalStart();
    }

    // Update \\

    @Override // From `SystemPackage`
    protected final void internalUpdate() {

        this.setContext(InternalContext.UPDATE);

        super.internalUpdate();
    }

    // Menu Exclusive Update \\

    @Override // From `SystemPackage`
    protected final void internalMenuExclusiveUpdate() {

        this.setContext(InternalContext.MENU_EXCLUSIVE);

        super.internalMenuExclusiveUpdate();
    }

    // Game Exclusive Update \\

    @Override // From `SystemPackage`
    protected final void internalGameExclusiveUpdate() {

        this.setContext(InternalContext.GAME_EXCLUSIVE);

        super.internalGameExclusiveUpdate();
    }

    // Fixed Update \\

    @Override // From `SystemPackage`
    protected final void internalFixedUpdate() {

        elapsedTime += deltaTime;
        int steps = 0;

        while (elapsedTime >= fixedInterval && steps < maxSteps) {

            elapsedTime -= fixedInterval;
            steps++;

            this.setContext(InternalContext.FIXED_UPDATE);

            super.internalFixedUpdate();
        }
    }

    // Late Update \\

    @Override // From `SystemPackage`
    protected final void internalLateUpdate() {

        this.setContext(InternalContext.LATE_UPDATE);

        super.internalLateUpdate();
    }

    // Render \\

    @Override // From `SystemPackage`
    protected final void internalRender() {

        this.setContext(InternalContext.RENDER);

        super.internalRender();
    }

    // Draw \\

    protected final void internalDraw() {

        this.setContext(InternalContext.DRAW);

        this.setFrameCount();

        draw();
    }

    // draw() is internal engine specific. SystemPackages do not contain `draw`.
    protected void draw() {
    }

    // Dispose \\

    @Override // From `SystemPackage`
    protected final void internalDispose() {

        this.setContext(InternalContext.DISPOSE);

        super.internalDispose();
    }

    // Utility \\

    private final void setFrameCount() {
        frameCount++;
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

        if (garbageCollection.isEmpty())
            return;

        for (SystemPackage target : garbageCollection)
            systemRegistry.remove(target.getClass());

        garbageCollection.clear();

        super.clearGarbage();
    }

    // Accessible \\

    public final long getFrameCount() {
        return frameCount;
    }

    public final float getDeltaTime() {
        return deltaTime;
    }

    public final WindowInstance getWindowInstance() {
        return windowInstance;
    }
}
