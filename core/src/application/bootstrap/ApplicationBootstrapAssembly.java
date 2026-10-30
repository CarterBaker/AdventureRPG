package application.bootstrap;

import application.bootstrap.animationpipeline.AnimationPipeline;
import application.bootstrap.calendarpipeline.CalendarPipeline;
import application.bootstrap.combatpipeline.CombatPipeline;
import application.bootstrap.contentpipeline.ContentPipeline;
import application.bootstrap.entitypipeline.EntityPipeline;
import application.bootstrap.furnishingpipeline.FurnishingPipeline;
import application.bootstrap.geometrypipeline.GeometryPipeline;
import application.bootstrap.itempipeline.ItemPipeline;
import application.bootstrap.lightingpipeline.LightingPipeline;
import application.bootstrap.lootpipeline.LootPipeline;
import application.bootstrap.mappipeline.MapPipeline;
import application.bootstrap.menupipeline.MenuPipeline;
import application.bootstrap.oceanpipeline.OceanPipeline;
import application.bootstrap.physicspipeline.PhysicsPipeline;
import application.bootstrap.progressionpipeline.ProgressionPipeline;
import application.bootstrap.renderpipeline.RenderPipeline;
import application.bootstrap.savepipeline.SavePipeline;
import application.bootstrap.settingspipeline.SettingsPipeline;
import application.bootstrap.shaderpipeline.ShaderPipeline;
import application.bootstrap.shipwrightpipeline.ShipwrightPipeline;
import application.bootstrap.soundpipeline.SoundPipeline;
import application.bootstrap.vehiclepipeline.VehiclePipeline;
import application.bootstrap.weatherpipeline.WeatherPipeline;
import application.bootstrap.worldpipeline.WorldPipeline;
import engine.root.AssemblyPackage;

public class ApplicationBootstrapAssembly extends AssemblyPackage {

    /*
     * Creates and owns all bootstrap pipelines in dependency order.
     * Pure creation — no runtime logic, no draw delegation.
     */

    @Override
    protected void create() {
        create(GeometryPipeline.class);
        create(AnimationPipeline.class);
        create(ShaderPipeline.class);
        create(RenderPipeline.class);
        create(SoundPipeline.class);
        create(ProgressionPipeline.class);
        create(ItemPipeline.class);
        create(FurnishingPipeline.class);
        create(LootPipeline.class);
        create(PhysicsPipeline.class);
        create(VehiclePipeline.class);
        create(ShipwrightPipeline.class);
        create(EntityPipeline.class);
        create(WorldPipeline.class);
        create(ContentPipeline.class);
        create(CombatPipeline.class);
        create(CalendarPipeline.class);
        create(WeatherPipeline.class);
        create(OceanPipeline.class);
        create(MapPipeline.class);
        create(LightingPipeline.class);
        create(MenuPipeline.class);
        create(SettingsPipeline.class);
        create(SavePipeline.class);
    }
}