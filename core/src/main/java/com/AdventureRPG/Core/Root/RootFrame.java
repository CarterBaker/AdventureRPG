package com.AdventureRPG.Core.Root;

public class RootFrame extends ManagerFrame {

    // Internal Process \\

    final InternalProcess getInternalRootProcess() {
        return internalProcess;
    }

    final void setInternalRootProcess(InternalProcess internalProcess) {
        this.internalProcess = internalProcess;
    }

    // Internal State \\

    final InternalState getInternalRootState() {
        return internalState;
    }

    final void setInternalRootState(InternalState internalState) {
        this.internalState = internalState;
    }
}
