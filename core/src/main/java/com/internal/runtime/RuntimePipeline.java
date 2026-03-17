package com.internal.runtime;

import com.internal.core.engine.PipelinePackage;
import com.internal.runtime.menu.MenuSystem;
import com.internal.runtime.player.PlayerSystem;
import com.internal.runtime.lighting.SkySystem;

public class RuntimePipeline extends PipelinePackage {

    /*
     * Game runtime entry point. Creates and owns all runtime systems.
     * Runs after bootstrap is complete — all managers and handles are
     * available. Each system is responsible for its own startup logic.
     */

    // Runtime
    private SkySystem skySystem;
    private PlayerSystem playerSystem;
    private MenuSystem menuSystem;

    // Runtime \\

    @Override
    protected void create() {

        // Runtime
        this.skySystem = create(SkySystem.class);
        this.playerSystem = create(PlayerSystem.class);
        this.menuSystem = create(MenuSystem.class);
    }
}