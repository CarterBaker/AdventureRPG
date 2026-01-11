package com.AdventureRPG.core.engine;

import com.AdventureRPG.bootstrap.calendarpipeline.CalendarPipeline;
import com.AdventureRPG.bootstrap.entitypipeline.EntityPipeline;
import com.AdventureRPG.bootstrap.geometrypipeline.GeometryPipeline;
import com.AdventureRPG.bootstrap.inputpipeline.InputPipeline;
import com.AdventureRPG.bootstrap.renderpipeline.RenderPipeline;
import com.AdventureRPG.bootstrap.shaderpipeline.ShaderPipeline;
import com.AdventureRPG.bootstrap.threadpipeline.ThreadSystem;
import com.AdventureRPG.bootstrap.worldpipeline.WorldPipeline;
import com.AdventureRPG.runtime.debug.DebugPipeline;

public class GameEngine extends EnginePackage {

    /*
     * GameEngine defines the concrete engine instance.
     * Registers pipelines and managers, and routes execution
     * from Main to internal systems.
     */

    // BootStrap
    private ThreadSystem threadSystem;
    private GeometryPipeline geometryPipeline;
    private ShaderPipeline shaderPipeline;
    private RenderPipeline renderPipeline;
    private CalendarPipeline calendarPipeline;
    private InputPipeline inputPipeline;
    private WorldPipeline worldPipeline;
    private EntityPipeline entityPipeline;

    // Runtime
    private DebugPipeline debugPipeline;

    // BootStrap \\

    @Override
    protected void bootstrap() {

        // BootStrap
        this.threadSystem = create(ThreadSystem.class);
        this.geometryPipeline = create(GeometryPipeline.class);
        this.shaderPipeline = create(ShaderPipeline.class);
        this.renderPipeline = create(RenderPipeline.class);
        this.calendarPipeline = create(CalendarPipeline.class);
        this.inputPipeline = create(InputPipeline.class);
        this.worldPipeline = create(WorldPipeline.class);
        this.entityPipeline = create(EntityPipeline.class);
    }

    // Runtime \\

    @Override
    protected void create() {

        // Runtime
        this.debugPipeline = create(DebugPipeline.class);
    }

    @Override
    void draw() {
        this.renderPipeline.draw();
    }
}
