package com.internal.bootstrap;

import com.internal.bootstrap.calendarpipeline.CalendarPipeline;
import com.internal.bootstrap.entitypipeline.EntityPipeline;
import com.internal.bootstrap.geometrypipeline.GeometryPipeline;
import com.internal.bootstrap.inputpipeline.InputPipeline;
import com.internal.bootstrap.itempipeline.ItemPipeline;
import com.internal.bootstrap.lightingpipeline.LightingPipeline;
import com.internal.bootstrap.menupipeline.MenuPipeline;
import com.internal.bootstrap.physicspipeline.PhysicsPipeline;
import com.internal.bootstrap.renderpipeline.RenderPipeline;
import com.internal.bootstrap.shaderpipeline.ShaderPipeline;
import com.internal.bootstrap.worldpipeline.WorldPipeline;
import com.internal.core.engine.AssemblyPackage;

public class BootstrapAssembly extends AssemblyPackage {

    // BootStrap
    private GeometryPipeline geometryPipeline;
    private ShaderPipeline shaderPipeline;
    private RenderPipeline renderPipeline;
    private ItemPipeline itemPipeline;
    private WorldPipeline worldPipeline;
    private PhysicsPipeline physicsPipeline;
    private InputPipeline inputPipeline;
    private EntityPipeline entityPipeline;
    private CalendarPipeline calendarPipeline;
    private LightingPipeline lightingPipeline;
    private MenuPipeline menuPipeline;

    // BootStrap \\

    @Override
    public void create() {

        // BootStrap
        this.geometryPipeline = create(GeometryPipeline.class);
        this.shaderPipeline = create(ShaderPipeline.class);
        this.renderPipeline = create(RenderPipeline.class);
        this.itemPipeline = create(ItemPipeline.class);
        this.worldPipeline = create(WorldPipeline.class);
        this.physicsPipeline = create(PhysicsPipeline.class);
        this.inputPipeline = create(InputPipeline.class);
        this.entityPipeline = create(EntityPipeline.class);
        this.calendarPipeline = create(CalendarPipeline.class);
        this.lightingPipeline = create(LightingPipeline.class);
        this.menuPipeline = create(MenuPipeline.class);
    }

    // Runtime \\

    public void draw() {
        this.renderPipeline.draw();
    }
}
