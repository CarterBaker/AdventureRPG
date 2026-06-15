package application.runtime;

import application.runtime.input.InputSystem;
import application.runtime.lighting.SkySystem;
import application.runtime.menu.MenuSystem;
import application.runtime.menueventsmanager.MenuEventsManager;
import application.runtime.player.PlayerSystem;
import application.runtime.postprocessing.PostProcessingManager;
import application.runtime.world.WorldSystem;
import engine.root.ContextPackage;

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
    private MenuEventsManager menuEventsManager;
    private WorldSystem worldSystem;
    private InputSystem playerInputSystem;
    private PostProcessingManager postProcessingManager;

    // Internal \\

    @Override
    protected void create() {

        // Runtime
        this.skySystem = create(SkySystem.class);
        this.playerSystem = create(PlayerSystem.class);
        this.menuSystem = create(MenuSystem.class);
        this.menuEventsManager = create(MenuEventsManager.class);
        this.worldSystem = create(WorldSystem.class);
        this.playerInputSystem = create(InputSystem.class);
        this.postProcessingManager = create(PostProcessingManager.class);
    }
}