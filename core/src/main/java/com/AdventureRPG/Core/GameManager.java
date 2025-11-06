package com.AdventureRPG.Core;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import com.AdventureRPG.SettingsSystem.Settings;

public abstract class GameManager extends GameSystem {

    // Internal
    private final List<GameSystem> subSystems = new ArrayList<>();
    private final Map<Class<? extends GameSystem>, GameSystem> subsystemsTypeMap = new IdentityHashMap<>();
    private GameSystem[] internalSubsystems = new GameSystem[0];

    // Constructor \\

    protected final GameSystem register(GameSystem systemFramework) {

        subSystems.add(systemFramework);
        subsystemsTypeMap.put(systemFramework.getClass(), systemFramework);

        return systemFramework;
    }

    @SuppressWarnings("unchecked")
    <T extends GameSystem> T subsystem(Class<T> type) {
        return (T) subsystemsTypeMap.get(type);
    }

    private final void cacheSubSystems() {
        internalSubsystems = subSystems.toArray(new GameSystem[0]);
    }

    // Init \\

    @Override
    void internalInit(Settings settings, RootManager rootManager) {

        cacheSubSystems();

        super.internalInit(
                settings,
                rootManager);

        for (int i = 0; i < internalSubsystems.length; i++)
            internalSubsystems[i].internalInit(
                    settings,
                    rootManager);
    }

    // Awake \\

    @Override
    void internalAwake() {

        super.internalAwake();

        for (int i = 0; i < internalSubsystems.length; i++)
            internalSubsystems[i].internalAwake();
    }

    // Start \\

    @Override
    void internalStart() {

        super.internalStart();

        for (int i = 0; i < internalSubsystems.length; i++)
            internalSubsystems[i].internalStart();
    }

    // Menu Exclusive Update \\

    @Override
    void internalMenuExclusiveUpdate() {

        super.internalMenuExclusiveUpdate();

        for (int i = 0; i < internalSubsystems.length; i++)
            internalSubsystems[i].internalMenuExclusiveUpdate();
    }

    // Game Exclusive Update \\

    @Override
    void internalGameExclusiveUpdate() {

        super.internalGameExclusiveUpdate();

        for (int i = 0; i < internalSubsystems.length; i++)
            internalSubsystems[i].internalGameExclusiveUpdate();
    }

    // Update \\

    @Override
    void internalUpdate() {

        super.internalUpdate();

        for (int i = 0; i < internalSubsystems.length; i++)
            internalSubsystems[i].internalUpdate();
    }

    // Fixed Update \\

    @Override
    void internalFixedUpdate() {

        super.internalFixedUpdate();

        for (int i = 0; i < internalSubsystems.length; i++)
            internalSubsystems[i].internalFixedUpdate();
    }

    // Late Update \\

    @Override
    void internalLateUpdate() {

        super.internalLateUpdate();

        for (int i = 0; i < internalSubsystems.length; i++)
            internalSubsystems[i].internalLateUpdate();
    }

    // Render \\

    @Override
    void internalRender() {

        super.internalRender();

        for (int i = 0; i < internalSubsystems.length; i++)
            internalSubsystems[i].internalRender();
    }

    // Dispose \\

    @Override
    void internalDispose() {

        super.internalDispose();

        for (int i = 0; i < internalSubsystems.length; i++)
            internalSubsystems[i].internalDispose();
    }
}
