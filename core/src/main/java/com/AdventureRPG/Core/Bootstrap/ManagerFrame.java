package com.AdventureRPG.Core.Bootstrap;

import java.util.ArrayList;
import java.util.List;

import com.AdventureRPG.Core.Util.Exceptions.CoreException;
import com.AdventureRPG.Core.Util.Exceptions.CoreException.DuplicateSystemFrameDetected;
import com.AdventureRPG.SettingsSystem.Settings;

public abstract class ManagerFrame extends SystemFrame {

    // Internal
    private List<SystemFrame> systemTree = new ArrayList<>();
    private List<SystemFrame> subSystems = new ArrayList<>();
    private SystemFrame[] gameSystems = new SystemFrame[0];

    // Register \\

    protected final SystemFrame register(SystemFrame subSystem) {

        if (this.getInternalProcess() != InternalProcess.CREATE)
            throw new CoreException.OutOfOrderException(this.getInternalProcess());

        if (this.systemTree.contains(subSystem))
            throw new DuplicateSystemFrameDetected(subSystem);

        this.systemTree.add(subSystem);
        this.subSystems.add(subSystem);

        subSystem.registerLocalManager(this);

        return subSystem;
    }

    @SuppressWarnings("unchecked")
    public final <T> T get(Class<T> type) {

        if (this.gameEngine.getInternalProcess() != InternalProcess.INIT)
            throw new CoreException.OutOfOrderException(this.getInternalProcess());

        for (SystemFrame frame : this.systemTree) {

            if (type.isAssignableFrom(frame.getClass()))
                return (T) frame;

            if (frame instanceof ManagerFrame manager) {

                T nested = manager.get(type);

                if (nested != null)
                    return nested;
            }
        }

        return null;
    }

    private final void cacheSubSystems() {

        this.gameSystems = this.subSystems.toArray(new SystemFrame[0]);
        this.subSystems.clear();
    }

    // Create \\

    @Override
    void internalCreate(Settings settings, EngineFrame gameEngine) {

        super.internalCreate(settings, gameEngine);

        this.cacheSubSystems();

        for (int i = 0; i < this.gameSystems.length; i++)
            this.gameSystems[i].internalCreate(this.settings, this.gameEngine);
    }

    // Init \\

    @Override
    void internalInit() {

        super.internalInit();

        for (int i = 0; i < this.gameSystems.length; i++)
            this.gameSystems[i].internalInit();
    }

    // Awake \\

    @Override
    void internalAwake() {

        super.internalAwake();

        for (int i = 0; i < this.gameSystems.length; i++)
            this.gameSystems[i].internalAwake();
    }

    // Start \\

    @Override
    void internalStart() {

        super.internalStart();

        for (int i = 0; i < this.gameSystems.length; i++)
            this.gameSystems[i].internalStart();
    }

    // Menu Exclusive Update \\

    @Override
    void internalMenuExclusiveUpdate() {

        super.internalMenuExclusiveUpdate();

        for (int i = 0; i < this.gameSystems.length; i++)
            this.gameSystems[i].internalMenuExclusiveUpdate();
    }

    // Game Exclusive Update \\

    @Override
    void internalGameExclusiveUpdate() {

        super.internalGameExclusiveUpdate();

        for (int i = 0; i < this.gameSystems.length; i++)
            this.gameSystems[i].internalGameExclusiveUpdate();
    }

    // Update \\

    @Override
    void internalUpdate() {

        super.internalUpdate();

        for (int i = 0; i < this.gameSystems.length; i++)
            this.gameSystems[i].internalUpdate();
    }

    // Fixed Update \\

    @Override
    void internalFixedUpdate() {

        super.internalFixedUpdate();

        for (int i = 0; i < this.gameSystems.length; i++)
            this.gameSystems[i].internalFixedUpdate();
    }

    // Late Update \\

    @Override
    void internalLateUpdate() {

        super.internalLateUpdate();

        for (int i = 0; i < this.gameSystems.length; i++)
            this.gameSystems[i].internalLateUpdate();
    }

    // Render \\

    @Override
    void internalRender() {

        super.internalRender();

        for (int i = 0; i < this.gameSystems.length; i++)
            this.gameSystems[i].internalRender();
    }

    // Dispose \\

    @Override
    void internalDispose() {

        super.internalDispose();

        for (int i = 0; i < this.gameSystems.length; i++)
            this.gameSystems[i].internalDispose();
    }
}
