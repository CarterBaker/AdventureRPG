package com.internal.core.engine;

import com.internal.bootstrap.calendarpipeline.CalendarPipeline;
import com.internal.bootstrap.entitypipeline.EntityPipeline;
import com.internal.bootstrap.geometrypipeline.GeometryPipeline;
import com.internal.bootstrap.inputpipeline.InputPipeline;
import com.internal.bootstrap.physicspipeline.PhysicsPipeline;
import com.internal.bootstrap.renderpipeline.RenderPipeline;
import com.internal.bootstrap.shaderpipeline.ShaderPipeline;
import com.internal.bootstrap.worldpipeline.WorldPipeline;
import com.internal.runtime.debug.DebugPipeline;

public class GameEngine extends EnginePackage {

    /*
     * GameEngine defines the concrete engine instance.
     * Registers pipelines and managers, and routes execution
     * from Main to internal systems.
     */

    // BootStrap
    private GeometryPipeline geometryPipeline;
    private ShaderPipeline shaderPipeline;
    private RenderPipeline renderPipeline;
    private WorldPipeline worldPipeline;
    private PhysicsPipeline physicsPipeline;
    private InputPipeline inputPipeline;
    private EntityPipeline entityPipeline;
    private CalendarPipeline calendarPipeline;

    // Runtime
    private DebugPipeline debugPipeline;

    // BootStrap \\

    @Override
    protected void bootstrap() {

        // BootStrap
        this.geometryPipeline = create(GeometryPipeline.class);
        this.shaderPipeline = create(ShaderPipeline.class);
        this.renderPipeline = create(RenderPipeline.class);
        this.worldPipeline = create(WorldPipeline.class);
        this.physicsPipeline = create(PhysicsPipeline.class);
        this.inputPipeline = create(InputPipeline.class);
        this.entityPipeline = create(EntityPipeline.class);
        this.calendarPipeline = create(CalendarPipeline.class);
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
