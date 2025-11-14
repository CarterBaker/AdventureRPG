package com.AdventureRPG.Core.Bootstrap;

import java.util.ArrayList;
import java.util.List;

import com.AdventureRPG.Core.Util.Exceptions.CoreException;
import com.AdventureRPG.Core.Util.Exceptions.CoreException.DuplicateSystemFrameDetected;
import com.AdventureRPG.SettingsSystem.Settings;

public abstract class ManagerFrame extends SystemFrame {

    // Internal
    private List<SystemFrame> systemTree = new ArrayList<>();
    private final List<SystemFrame> subSystems = new ArrayList<>();
    private SystemFrame[] systemIterator = new SystemFrame[0];

    // Register \\

    protected final SystemFrame register(SystemFrame subSystem) {

        if (getInternalProcess() != InternalProcess.CREATE)
            throw new CoreException.OutOfOrderException(getInternalProcess());

        if (systemTree.contains(subSystem))
            throw new DuplicateSystemFrameDetected(subSystem);

        systemTree.add(subSystem);
        subSystems.add(subSystem);

        subSystem.registerLocalManager(this);

        return subSystem;
    }

    @SuppressWarnings("unchecked")
    public final <T> T get(Class<T> type) {

        if (getInternalProcess() != InternalProcess.INIT)
            throw new CoreException.OutOfOrderException(getInternalProcess());

        for (SystemFrame frame : systemTree) {

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

        systemIterator = subSystems.toArray(new SystemFrame[0]);
        subSystems.clear();
    }

    // Create \\

    @Override
    void internalCreate(Settings settings, EngineFrame engineManager) {

        super.internalCreate(settings, engineManager);

        cacheSubSystems();

        for (int i = 0; i < systemIterator.length; i++)
            systemIterator[i].internalCreate(settings, engineManager);
    }

    // Init \\

    @Override
    void internalInit() {

        super.internalInit();

        for (int i = 0; i < systemIterator.length; i++)
            systemIterator[i].internalInit();
    }

    // Awake \\

    @Override
    void internalAwake() {

        super.internalAwake();

        for (int i = 0; i < systemIterator.length; i++)
            systemIterator[i].internalAwake();
    }

    // Start \\

    @Override
    void internalStart() {

        super.internalStart();

        for (int i = 0; i < systemIterator.length; i++)
            systemIterator[i].internalStart();
    }

    // Menu Exclusive Update \\

    @Override
    void internalMenuExclusiveUpdate() {

        super.internalMenuExclusiveUpdate();

        for (int i = 0; i < systemIterator.length; i++)
            systemIterator[i].internalMenuExclusiveUpdate();
    }

    // Game Exclusive Update \\

    @Override
    void internalGameExclusiveUpdate() {

        super.internalGameExclusiveUpdate();

        for (int i = 0; i < systemIterator.length; i++)
            systemIterator[i].internalGameExclusiveUpdate();
    }

    // Update \\

    @Override
    void internalUpdate() {

        super.internalUpdate();

        for (int i = 0; i < systemIterator.length; i++)
            systemIterator[i].internalUpdate();
    }

    // Fixed Update \\

    @Override
    void internalFixedUpdate() {

        super.internalFixedUpdate();

        for (int i = 0; i < systemIterator.length; i++)
            systemIterator[i].internalFixedUpdate();
    }

    // Late Update \\

    @Override
    void internalLateUpdate() {

        super.internalLateUpdate();

        for (int i = 0; i < systemIterator.length; i++)
            systemIterator[i].internalLateUpdate();
    }

    // Render \\

    @Override
    void internalRender() {

        super.internalRender();

        for (int i = 0; i < systemIterator.length; i++)
            systemIterator[i].internalRender();
    }

    // Dispose \\

    @Override
    void internalDispose() {

        super.internalDispose();

        for (int i = 0; i < systemIterator.length; i++)
            systemIterator[i].internalDispose();
    }
}
