package com.AdventureRPG.core.engine;

import com.AdventureRPG.WorldPipeline.WorldPipeline;
import com.AdventureRPG.core.geometrypipeline.GeometryPipeline;
import com.AdventureRPG.core.physicspipeline.input.InputSystem;
import com.AdventureRPG.core.physicspipeline.movement.MovementManager;
import com.AdventureRPG.core.renderpipeline.RenderPipeline;
import com.AdventureRPG.core.scenepipeline.ScenePipeline;
import com.AdventureRPG.core.shaderpipeline.ShaderPipeline;
import com.AdventureRPG.core.threadpipeline.ThreadSystem;
import com.AdventureRPG.lightingsystem.LightingManager;
import com.AdventureRPG.playermanager.PlayerManager;
import com.AdventureRPG.savemanager.SaveManager;
import com.AdventureRPG.timesystem.TimeSystem;
import com.AdventureRPG.uisystem.UISystem;

public class GameEngine extends EnginePackage {

    /*
     * GameEngine defines the concrete engine instance.
     * Registers pipelines and managers, and routes execution
     * from Main to internal systems.
     */

    // BootStrap
    private ThreadSystem threadSystem;
    private ScenePipeline scenePipeline;
    private InputSystem inputSystem;
    private MovementManager movementManager;
    private GeometryPipeline geometryPipeline;
    private ShaderPipeline shaderPipeline;
    private RenderPipeline renderPipeline;

    // Internal
    private SaveManager saveManager;
    private UISystem uiSystem;
    private LightingManager lightingManager;
    private TimeSystem timeSystem;
    private PlayerManager playerManager;
    private WorldPipeline worldPipeline;

    // BootStrap \\

    @Override
    protected void bootstrap() {

        // BootStrap
        this.threadSystem = create(ThreadSystem.class);
        this.scenePipeline = create(ScenePipeline.class);
        this.inputSystem = create(InputSystem.class);
        this.movementManager = create(MovementManager.class);
        this.geometryPipeline = create(GeometryPipeline.class);
        this.shaderPipeline = create(ShaderPipeline.class);
        this.renderPipeline = create(RenderPipeline.class);
    }

    // Internal \\

    @Override
    protected void create() {

        // Internal
        this.saveManager = create(SaveManager.class);
        this.uiSystem = create(UISystem.class);
        this.lightingManager = create(LightingManager.class);
        this.timeSystem = create(TimeSystem.class);
        this.playerManager = create(PlayerManager.class);
        this.worldPipeline = create(WorldPipeline.class);
    }

    @Override
    protected void release() {

        // Internal
        this.geometryPipeline = release(GeometryPipeline.class);
    }

    @Override
    void draw() {
        this.renderPipeline.draw();
    }
}
