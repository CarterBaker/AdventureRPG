package com.AdventureRPG.Core.Bootstrap;

public enum InternalProcess {

    CREATE(0, false),
    INIT(1, false),
    AWAKE(2, false),
    START(3, false),

    MENU_EXCLUSIVE(4, true),
    GAME_EXCLUSIVE(5, true),
    UPDATE(6, true),
    FIXED_UPDATE(7, true),
    LATE_UPDATE(8, true),
    RENDER(9, true),

    DISPOSE(10, false);

    final int order;
    final boolean updateProcess;

    InternalProcess(int order, boolean loopState) {
        this.order = order;
        this.updateProcess = loopState;
    }

    public boolean isUpdateProcess() {
        return updateProcess;
    }
}
