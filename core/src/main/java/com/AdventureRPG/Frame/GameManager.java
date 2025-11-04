package com.AdventureRPG.Frame;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public abstract class GameManager extends GameSystem {

    // Internal \\
    private final List<GameSystem> subSystems = new ArrayList<>();
    private final Map<Class<? extends GameSystem>, GameSystem> subsystemsTypeMap = new IdentityHashMap<>();
    private GameSystem[] internalSubsystems = new GameSystem[0];
    private boolean[] managerCache;

    // Constructor \\

    protected final void register(GameSystem gameBehavior) {

        subSystems.add(gameBehavior);
        subsystemsTypeMap.put(gameBehavior.getClass(), gameBehavior);
    }

    @SuppressWarnings("unchecked")
    public final <T extends GameSystem> T subsystem(Class<T> type) {
        return (T) subsystemsTypeMap.get(type);
    }

    private final void cacheSubSystems() {

        internalSubsystems = subSystems.toArray(new GameSystem[0]);

        managerCache = new boolean[internalSubsystems.length];

        for (int i = 0; i < internalSubsystems.length; i++)
            managerCache[i] = internalSubsystems[i] instanceof GameManager;
    }

    // Awake \\

    private final void internalAwake() {

        cacheSubSystems();

        awake();

        for (int i = 0; i < internalSubsystems.length; i++) {

            if (managerCache[i])
                ((GameManager) internalSubsystems[i]).internalAwake();

            else
                internalSubsystems[i].awake();
        }
    }

    // Start \\

    private final void internalStart() {

        start();

        for (int i = 0; i < internalSubsystems.length; i++) {

            if (managerCache[i])
                ((GameManager) internalSubsystems[i]).internalStart();

            else
                internalSubsystems[i].start();
        }
    }

    // Update \\

    private final void internalUpdate() {

        update();

        for (int i = 0; i < internalSubsystems.length; i++) {

            if (managerCache[i])
                ((GameManager) internalSubsystems[i]).internalUpdate();

            else
                internalSubsystems[i].update();
        }
    }

    // Fixed Update \\

    private final void internalFixedUpdate() {

        fixedUpdate();

        for (int i = 0; i < internalSubsystems.length; i++) {

            if (managerCache[i])
                ((GameManager) internalSubsystems[i]).internalFixedUpdate();

            else
                internalSubsystems[i].fixedUpdate();
        }
    }

    // Late Update \\

    private final void internalLateUpdate() {

        lateUpdate();

        for (int i = 0; i < internalSubsystems.length; i++) {

            if (managerCache[i])
                ((GameManager) internalSubsystems[i]).internalLateUpdate();

            else
                internalSubsystems[i].lateUpdate();
        }
    }

    // Render \\

    private final void internalRender() {

        render();

        for (int i = 0; i < internalSubsystems.length; i++) {

            if (managerCache[i])
                ((GameManager) internalSubsystems[i]).internalRender();

            else
                internalSubsystems[i].render();
        }
    }

    // Dispose \\

    private final void internalDispose() {

        dispose();

        for (int i = 0; i < internalSubsystems.length; i++) {

            if (managerCache[i])
                ((GameManager) internalSubsystems[i]).internalDispose();

            else
                internalSubsystems[i].dispose();
        }
    }
}
