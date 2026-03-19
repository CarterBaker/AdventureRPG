package com.internal.runtime;

import com.internal.core.engine.ContextPackage;
import com.internal.runtime.input.PlayerInputSystem;
import com.internal.runtime.lighting.SkySystem;
import com.internal.runtime.menu.MenuSystem;
import com.internal.runtime.player.PlayerSystem;
import com.internal.runtime.world.WorldSystem;

public class RuntimeContext extends ContextPackage {

    /*
     * Game runtime entry point. Creates and owns all runtime systems.
     * Paired with a WindowInstance by EnginePackage.createContext() before
     * any lifecycle phase runs — systems call context.getWindow() to reach
     * their render target without knowing which window they are targeting.
     * The editor reuses RuntimeContext unchanged by pairing it with a
     * different window at creation time.
     */

    // Runtime
    private SkySystem skySystem;
    private PlayerSystem playerSystem;
    private MenuSystem menuSystem;
    private WorldSystem worldSystem;
    private PlayerInputSystem playerInputSystem;

    // Internal \\

    @Override
    protected void create() {

        // Runtime
        this.skySystem = create(SkySystem.class);
        this.playerSystem = create(PlayerSystem.class);
        this.menuSystem = create(MenuSystem.class);
        this.worldSystem = create(WorldSystem.class);
        this.playerInputSystem = create(PlayerInputSystem.class);
    }
}