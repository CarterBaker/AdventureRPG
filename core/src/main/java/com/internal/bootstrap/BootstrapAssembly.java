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

    /*
     * Creates and owns all bootstrap pipelines in dependency order.
     * Pure creation — no runtime logic, no draw delegation.
     */

    @Override
    public void create() {
        create(GeometryPipeline.class);
        create(ShaderPipeline.class);
        create(RenderPipeline.class);
        create(ItemPipeline.class);
        create(PhysicsPipeline.class);
        create(InputPipeline.class);
        create(EntityPipeline.class);
        create(WorldPipeline.class);
        create(CalendarPipeline.class);
        create(LightingPipeline.class);
        create(MenuPipeline.class);
    }
}