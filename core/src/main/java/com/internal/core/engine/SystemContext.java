package com.internal.core.engine;

import com.internal.core.engine.EngineUtility.InternalException;

enum SystemContext {

        /*
         * SystemContext represent active execution contexts within the
         * internal engine. Each context corresponds to a specific process
         * method currently being executed.
         *
         * They are used to track which process is running at any given time
         * and to enforce execution order for internal engine systems.
         */

        // Empty constructor for initialization
        NULL(),

        // BootStrap
        BOOTSTRAP("NULL"),

        // Constructor chain
        CREATE("NULL",
                        "BOOTSTRAP"),
        GET("CREATE"),
        AWAKE("GET"),
        RELEASE("AWAKE"),
        START("RELEASE"),

        // Runtime (cyclic)
        UPDATE("START",
                        "RENDER",
                        "DRAW"),
        FIXED_UPDATE("UPDATE"),
        LATE_UPDATE("UPDATE",
                        "FIXED_UPDATE"),
        RENDER("LATE_UPDATE"),

        // Internal Render Loop
        DRAW("RENDER"),

        // Game Disposal (can be entered from anywhere)
        DISPOSE("NULL",
                        "BOOTSTRAP",
                        "CREATE",
                        "GET",
                        "AWAKE",
                        "RELEASE",
                        "START",
                        "UPDATE",
                        "FIXED_UPDATE",
                        "LATE_UPDATE",
                        "RENDER",
                        "DRAW");

        // Internal
        final int order;
        private final String[] entryPoints;
        private short entryMask;

        // Internal \\

        SystemContext(String... entryPoints) {

                // Internal
                this.order = this.ordinal();
                this.entryPoints = entryPoints;
        }

        static {
                for (SystemContext context : values()) {

                        short mask = 0;

                        for (String name : context.entryPoints) {

                                try {
                                        SystemContext prev = SystemContext.valueOf(name);
                                        mask |= (short) (1 << prev.order);
                                }

                                catch (IllegalArgumentException e) {
                                        throw new InternalException(
                                                        "Invalid SystemContext predecessor: "
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