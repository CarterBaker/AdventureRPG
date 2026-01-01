package com.AdventureRPG.core.engine;

import java.io.File;

import com.AdventureRPG.WorldPipeline.WorldPipeline;
import com.AdventureRPG.core.engine.settings.Settings;
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
import com.google.gson.Gson;

public class GameEngine extends EnginePackage {

    /*
     * This is the game engine itself. This is the less abstract
     * side of the internal engine, though this and EnginePackage
     * are both closely related and work in sync with each other.
     * This class is where all pipelines are declared and all
     * logic is routed through this class from Main to each
     * package in order.
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
    private UISystem UISystem;
    private LightingManager lightingManager;
    private TimeSystem timeSystem;
    private PlayerManager playerManager;
    private WorldPipeline worldPipeline;

    // Internal \\

    public GameEngine(
            Settings settings,
            Main main,
            File path,
            Gson gson) {

        // Internal
        super(
                settings,
                main,
                path,
                gson);
    }

    // BootStrap \\

    @Override
    protected void bootstrap() {

        // BootStrap
        this.threadSystem = (ThreadSystem) register(new ThreadSystem());
        this.scenePipeline = (ScenePipeline) register(new ScenePipeline());
        this.inputSystem = (InputSystem) register(new InputSystem());
        this.movementManager = (MovementManager) register(new MovementManager());
        this.geometryPipeline = (GeometryPipeline) register(new GeometryPipeline());
        this.shaderPipeline = (ShaderPipeline) register(new ShaderPipeline());
        this.renderPipeline = (RenderPipeline) register(new RenderPipeline());
    }

    // Internal \\

    @Override
    protected void create() {

        // Internal
        saveManager = (SaveManager) register(new SaveManager());
        UISystem = (UISystem) register(new UISystem());
        lightingManager = (LightingManager) register(new LightingManager());
        timeSystem = (TimeSystem) register(new TimeSystem());
        playerManager = (PlayerManager) register(new PlayerManager());
        worldPipeline = (WorldPipeline) register(new WorldPipeline());
    }

    @Override
    void draw() {
        this.renderPipeline.draw();
    }
}
