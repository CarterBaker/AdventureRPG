package program.bootstrap;

import program.bootstrap.calendarpipeline.CalendarPipeline;
import program.bootstrap.entitypipeline.EntityPipeline;
import program.bootstrap.geometrypipeline.GeometryPipeline;
import program.bootstrap.inputpipeline.InputPipeline;
import program.bootstrap.itempipeline.ItemPipeline;
import program.bootstrap.lightingpipeline.LightingPipeline;
import program.bootstrap.menupipeline.MenuPipeline;
import program.bootstrap.physicspipeline.PhysicsPipeline;
import program.bootstrap.renderpipeline.RenderPipeline;
import program.bootstrap.shaderpipeline.ShaderPipeline;
import program.bootstrap.worldpipeline.WorldPipeline;
import program.core.engine.AssemblyPackage;

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