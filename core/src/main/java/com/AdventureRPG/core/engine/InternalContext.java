package com.AdventureRPG.core.engine;

import com.AdventureRPG.core.engine.EngineUtility.InternalException;

enum InternalContext {

        /*
         * InternalContext represent active execution contexts within the
         * internal engine. Each instance corresponds to a specific process
         * method currently being executed.
         *
         * They are used to track which internalContext is running at any given time
         * and to provide execution order for internal engine systems.
         */

        // Empty constructor for initialization
        NULL(),

        // BootStrap
        BOOTSTRAP("NULL"),

        // Constructor chain
        CREATE("NULL",
                        "BOOTSTRAP"),
        INIT("CREATE"),
        AWAKE("INIT"),
        FREE_MEMORY("AWAKE"),
        START("FREE_MEMORY"),

        // Runtime (cyclic)
        UPDATE("START",
                        "RENDER",
                        "DRAW"),
        MENU_EXCLUSIVE("UPDATE"),
        GAME_EXCLUSIVE("UPDATE"),
        FIXED_UPDATE("MENU_EXCLUSIVE",
                        "GAME_EXCLUSIVE"),
        LATE_UPDATE("MENU_EXCLUSIVE",
                        "GAME_EXCLUSIVE",
                        "FIXED_UPDATE"),
        RENDER("LATE_UPDATE"),

        // Internal Render Loop
        DRAW("RENDER"),

        // Game Disposal (can be entered from anywhere)
        DISPOSE("NULL",
                        "BOOTSTRAP",
                        "CREATE",
                        "INIT",
                        "AWAKE",
                        "FREE_MEMORY",
                        "START",
                        "UPDATE",
                        "MENU_EXCLUSIVE",
                        "GAME_EXCLUSIVE",
                        "FIXED_UPDATE",
                        "LATE_UPDATE",
                        "RENDER",
                        "DRAW");

        // Internal
        final int order;
        private final String[] entryPoints;
        private short entryMask;

        // Internal \\

        InternalContext(String... entryPoints) {

                // Internal
                this.order = this.ordinal();
                this.entryPoints = entryPoints;
        }

        static {
                for (InternalContext context : values()) {

                        short mask = 0;

                        for (String name : context.entryPoints) {

                                try {
                                        InternalContext prev = InternalContext.valueOf(name);
                                        mask |= (short) (1 << prev.order);
                                }

                                catch (IllegalArgumentException e) {
                                        throw new InternalException(
                                                        "Invalid InternalContext predecessor: "
                                                                        + name + " -> " + context.name(),
                                                        e);
                                }
                        }

                        context.entryMask = mask;
                }
        }

        // Accessible \\

        boolean canEnterFrom(int current) {
                return (entryMask & (1 << current)) != 0;
        }
}