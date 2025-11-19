package com.AdventureRPG.Core.Bootstrap;

public enum InternalProcess {

    BOOT_KERNEL(0, false),

    CREATE(1, false),
    INIT(2, false),
    AWAKE(3, false),
    START(4, false),

    MENU_EXCLUSIVE(5, true),
    GAME_EXCLUSIVE(6, true),
    UPDATE(7, true),
    FIXED_UPDATE(8, true),
    LATE_UPDATE(9, true),
    RENDER(10, true),

    DISPOSE(11, false);

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
