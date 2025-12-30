package com.AdventureRPG.core.engine;

public enum InternalProcess {

    // Kernel
    BOOT_KERNEL(false),

    // Constructor
    CREATE(false),
    INIT(false),
    AWAKE(false),
    FREE_MEMORY(false),
    START(false),

    // Runtime
    UPDATE(true),
    MENU_EXCLUSIVE(true),
    GAME_EXCLUSIVE(true),
    FIXED_UPDATE(true),
    LATE_UPDATE(true),
    RENDER(true),

    // Internal Render Loop
    DRAW(true),

    // Game Disposal
    DISPOSE(false);

    // Internal
    public final int order;
    public final boolean isUpdateProcess;

    // Internal \\

    InternalProcess(boolean loopState) {

        // Internal
        this.order = this.ordinal();
        this.isUpdateProcess = loopState;
    }
}
