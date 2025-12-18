package com.AdventureRPG.core.engine;

public enum InternalProcess {

    BOOT_KERNEL(false),

    CREATE(false),
    INIT(false),
    AWAKE(false),
    FREE_MEMORY(false),
    START(false),

    MENU_EXCLUSIVE(true),
    GAME_EXCLUSIVE(true),
    UPDATE(true),
    FIXED_UPDATE(true),
    LATE_UPDATE(true),
    RENDER(true),

    DRAW(true),

    DISPOSE(false);

    private final int order;
    private final boolean updateProcess;

    InternalProcess(boolean loopState) {
        this.order = this.ordinal();
        this.updateProcess = loopState;
    }

    public int getOrder() {
        return order;
    }

    public boolean isUpdateProcess() {
        return updateProcess;
    }
}
