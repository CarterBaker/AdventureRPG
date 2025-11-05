package com.AdventureRPG.Core.Framework;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public abstract class GameManager extends GameSystem {

    // Internal
    private final List<GameSystem> subSystems = new ArrayList<>();
    private final Map<Class<? extends GameSystem>, GameSystem> subsystemsTypeMap = new IdentityHashMap<>();
    private GameSystem[] internalSubsystems = new GameSystem[0];
    private boolean[] managerCache;

    // Constructor \\

    protected final GameSystem register(GameSystem systemFramework) {

        subSystems.add(systemFramework);
        subsystemsTypeMap.put(systemFramework.getClass(), systemFramework);

        return systemFramework;
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

    // Create \\

    public final void internalInit() {

        rootInit(
                settings,
                rootManager);

        cacheSubSystems();

        init();

        for (int i = 0; i < internalSubsystems.length; i++) {

            if (managerCache[i])
                ((GameManager) internalSubsystems[i]).internalInit();

            else
                internalSubsystems[i].init();
        }
    }

    // Awake \\

    public final void internalAwake() {

        awake();

        for (int i = 0; i < internalSubsystems.length; i++) {

            if (managerCache[i])
                ((GameManager) internalSubsystems[i]).internalAwake();

            else
                internalSubsystems[i].awake();
        }
    }

    // Start \\

    public final void internalStart() {

        start();

        for (int i = 0; i < internalSubsystems.length; i++) {

            if (managerCache[i])
                ((GameManager) internalSubsystems[i]).internalStart();

            else
                internalSubsystems[i].start();
        }
    }

    // Menu Exclusive Update \\

    public final void internalMenuExclusiveUpdate() {

        menuExclusiveUpdate();

        for (int i = 0; i < internalSubsystems.length; i++) {

            if (managerCache[i])
                ((GameManager) internalSubsystems[i]).internalMenuExclusiveUpdate();

            else
                internalSubsystems[i].menuExclusiveUpdate();
        }
    }

    // Game Exclusive Update \\

    public final void internalGameExclusiveUpdate() {

        gameExclusiveUpdate();

        for (int i = 0; i < internalSubsystems.length; i++) {

            if (managerCache[i])
                ((GameManager) internalSubsystems[i]).internalGameExclusiveUpdate();

            else
                internalSubsystems[i].gameExclusiveUpdate();
        }
    }

    // Update \\

    public final void internalUpdate() {

        update();

        for (int i = 0; i < internalSubsystems.length; i++) {

            if (managerCache[i])
                ((GameManager) internalSubsystems[i]).internalUpdate();

            else
                internalSubsystems[i].update();
        }
    }

    // Fixed Update \\

    public final void internalFixedUpdate() {

        fixedUpdate();

        for (int i = 0; i < internalSubsystems.length; i++) {

            if (managerCache[i])
                ((GameManager) internalSubsystems[i]).internalFixedUpdate();

            else
                internalSubsystems[i].fixedUpdate();
        }
    }

    // Late Update \\

    public final void internalLateUpdate() {

        lateUpdate();

        for (int i = 0; i < internalSubsystems.length; i++) {

            if (managerCache[i])
                ((GameManager) internalSubsystems[i]).internalLateUpdate();

            else
                internalSubsystems[i].lateUpdate();
        }
    }

    // Render \\

    public final void internalRender() {

        render();

        for (int i = 0; i < internalSubsystems.length; i++) {

            if (managerCache[i])
                ((GameManager) internalSubsystems[i]).internalRender();

            else
                internalSubsystems[i].render();
        }
    }

    // Dispose \\

    public final void internalDispose() {

        dispose();

        for (int i = 0; i < internalSubsystems.length; i++) {

            if (managerCache[i])
                ((GameManager) internalSubsystems[i]).internalDispose();

            else
                internalSubsystems[i].dispose();
        }
    }
}
