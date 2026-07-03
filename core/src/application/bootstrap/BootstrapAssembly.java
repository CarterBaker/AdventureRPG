package application.bootstrap;

import application.bootstrap.animationpipeline.AnimationPipeline;
import application.bootstrap.calendarpipeline.CalendarPipeline;
import application.bootstrap.entitypipeline.EntityPipeline;
import application.bootstrap.geometrypipeline.GeometryPipeline;
import application.bootstrap.itempipeline.ItemPipeline;
import application.bootstrap.lightingpipeline.LightingPipeline;
import application.bootstrap.menupipeline.MenuPipeline;
import application.bootstrap.physicspipeline.PhysicsPipeline;
import application.bootstrap.renderpipeline.RenderPipeline;
import application.bootstrap.settingspipeline.SettingsPipeline;
import application.bootstrap.shaderpipeline.ShaderPipeline;
import application.bootstrap.weatherpipeline.WeatherPipeline;
import application.bootstrap.worldpipeline.WorldPipeline;
import engine.root.AssemblyPackage;

public class BootstrapAssembly extends AssemblyPackage {

    /*
     * Creates and owns all bootstrap pipelines in dependency order.
     * Pure creation — no runtime logic, no draw delegation.
     */

    @Override
    public void create() {
        create(GeometryPipeline.class);
        create(AnimationPipeline.class);
        create(ShaderPipeline.class);
        create(RenderPipeline.class);
        create(ItemPipeline.class);
        create(PhysicsPipeline.class);
        create(EntityPipeline.class);
        create(WorldPipeline.class);
        create(CalendarPipeline.class);
        create(WeatherPipeline.class);
        create(LightingPipeline.class);
        create(MenuPipeline.class);
        create(SettingsPipeline.class);
    }
}