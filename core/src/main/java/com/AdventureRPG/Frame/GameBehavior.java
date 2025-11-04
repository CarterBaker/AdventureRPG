package com.AdventureRPG.Frame;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public abstract class GameBehavior implements GameSystem {

    // Internal \\

    private String systemName = getClass().getSimpleName();
    private final Map<Class<? extends GameBehavior>, GameBehavior> typeMap = new IdentityHashMap<>();
    private final List<GameBehavior> systemArray = new ArrayList<>();
    private GameBehavior[] subsystemsFlattened = new GameBehavior[0];

    // Constructor \\

    protected final void register(GameBehavior gameBehavior) {

        systemArray.add(gameBehavior);
        typeMap.put(gameBehavior.getClass(), gameBehavior);
    }

    protected final void constructor() {

        List<GameBehavior> flatList = new ArrayList<>();
        subsystemsFlattened = flatList.toArray(new GameBehavior[flatList.size()]);
    }

    @SuppressWarnings("unchecked")
    public final <T extends GameBehavior> T subsystem(Class<T> type) {
        return (T) typeMap.get(type);
    }

    // Awake \\

    public final void internalAwake() {

        constructor();

        awake();

        for (int i = 0; i < subsystemsFlattened.length; i++)
            subsystemsFlattened[i].awake();
    }

    @Override
    public void awake() {
    }

    // Start \\

    public final void internalStart() {

        start();

        for (int i = 0; i < subsystemsFlattened.length; i++)
            subsystemsFlattened[i].start();
    }

    @Override
    public void start() {
    }

    // Update \\

    public final void internalUpdate() {

        update();

        for (int i = 0; i < subsystemsFlattened.length; i++)
            subsystemsFlattened[i].update();
    }

    @Override
    public void update() {
    }

    // Fixed Update \\

    public final void internalFixedUpdate() {

        fixedUpdate();

        for (int i = 0; i < subsystemsFlattened.length; i++)
            subsystemsFlattened[i].fixedUpdate();
    }

    @Override
    public void fixedUpdate() {
    }

    // Late Update \\

    public final void internalLateUpdate() {

        lateUpdate();

        for (int i = 0; i < subsystemsFlattened.length; i++)
            subsystemsFlattened[i].lateUpdate();
    }

    @Override
    public void lateUpdate() {
    }

    // Render \\

    public final void internalRender() {

        render();

        for (int i = 0; i < subsystemsFlattened.length; i++)
            subsystemsFlattened[i].dispose();
    }

    @Override
    public void render() {
    }

    // Dispose \\

    public final void internalDispose() {

        dispose();

        for (GameBehavior subSystem : typeMap.values())
            subSystem.internalDispose();

        dispose();
    }

    @Override
    public void dispose() {
    }

    // Debug \\

    public void debug(String msg) {
        System.out.println("[" + systemName + "] " + msg);
    }
}
