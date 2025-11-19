package com.AdventureRPG.Core.Bootstrap;

import java.util.ArrayList;
import java.util.List;

import com.AdventureRPG.Core.Util.Exceptions.CoreException;
import com.AdventureRPG.Core.Util.Exceptions.CoreException.DuplicateSystemFrameDetected;

public abstract class ManagerFrame extends SystemFrame {

    // Internal
    private List<SystemFrame> systemTree = new ArrayList<>();
    private SystemFrame[] systemArray = new SystemFrame[0];

    // System Registry \\

    protected final SystemFrame register(SystemFrame subSystem) {
        return internalRegister(subSystem);
    }

    SystemFrame internalRegister(SystemFrame subSystem) {

        if (this.getInternalProcess() != InternalProcess.CREATE)
            throw new CoreException.OutOfOrderException(this.getInternalProcess());

        if (subSystem instanceof EngineFrame) // TODO: This will need a specialized error
            throw new CoreException.OutOfOrderException(this.getInternalProcess());

        if (this.systemTree.contains(subSystem))
            throw new DuplicateSystemFrameDetected(subSystem);

        this.systemTree.add(subSystem);

        subSystem.register(
                settings,
                gameEngine,
                this);

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
        this.systemArray = this.systemTree.toArray(new SystemFrame[0]);
    }

    // Create \\

    @Override
    void internalCreate() {

        super.internalCreate();

        this.cacheSubSystems();

        for (int i = 0; i < this.systemArray.length; i++)
            this.systemArray[i].internalCreate();
    }

    // Init \\

    @Override
    void internalInit() {

        super.internalInit();

        for (int i = 0; i < this.systemArray.length; i++)
            this.systemArray[i].internalInit();
    }

    // Awake \\

    @Override
    void internalAwake() {

        super.internalAwake();

        for (int i = 0; i < this.systemArray.length; i++)
            this.systemArray[i].internalAwake();
    }

    // Start \\

    @Override
    void internalStart() {

        super.internalStart();

        for (int i = 0; i < this.systemArray.length; i++)
            this.systemArray[i].internalStart();
    }

    // Menu Exclusive Update \\

    @Override
    void internalMenuExclusiveUpdate() {

        super.internalMenuExclusiveUpdate();

        for (int i = 0; i < this.systemArray.length; i++)
            this.systemArray[i].internalMenuExclusiveUpdate();
    }

    // Game Exclusive Update \\

    @Override
    void internalGameExclusiveUpdate() {

        super.internalGameExclusiveUpdate();

        for (int i = 0; i < this.systemArray.length; i++)
            this.systemArray[i].internalGameExclusiveUpdate();
    }

    // Update \\

    @Override
    void internalUpdate() {

        super.internalUpdate();

        for (int i = 0; i < this.systemArray.length; i++)
            this.systemArray[i].internalUpdate();
    }

    // Fixed Update \\

    @Override
    void internalFixedUpdate() {

        super.internalFixedUpdate();

        for (int i = 0; i < this.systemArray.length; i++)
            this.systemArray[i].internalFixedUpdate();
    }

    // Late Update \\

    @Override
    void internalLateUpdate() {

        super.internalLateUpdate();

        for (int i = 0; i < this.systemArray.length; i++)
            this.systemArray[i].internalLateUpdate();
    }

    // Render \\

    @Override
    void internalRender() {

        super.internalRender();

        for (int i = 0; i < this.systemArray.length; i++)
            this.systemArray[i].internalRender();
    }

    // Dispose \\

    @Override
    void internalDispose() {

        super.internalDispose();

        for (int i = 0; i < this.systemArray.length; i++)
            this.systemArray[i].internalDispose();
    }
}
