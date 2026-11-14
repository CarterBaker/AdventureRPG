package launcher.bootstrap;

import application.bootstrap.animationpipeline.AnimationPipeline;
import application.bootstrap.geometrypipeline.GeometryPipeline;
import application.bootstrap.menupipeline.MenuPipeline;
import application.bootstrap.musicpipeline.MusicPipeline;
import application.bootstrap.renderpipeline.RenderPipeline;
import application.bootstrap.settingspipeline.SettingsPipeline;
import application.bootstrap.shaderpipeline.ShaderPipeline;
import application.bootstrap.soundpipeline.SoundPipeline;
import engine.root.AssemblyPackage;
import launcher.bootstrap.launcherpipeline.LauncherPipeline;

public class LauncherBootstrapAssembly extends AssemblyPackage {

    /*
     * Launcher bootstrap root. Creates only what the launcher's menus draw
     * and sound with — geometry, animation, shaders, rendering, sound,
     * music, menus and settings, in the order the application bootstrap
     * creates them — and the launcher's own pipeline. Animation stands only
     * because an instrument names the clips its rigs play it with; the
     * launcher's asset manifest queues none of them. No world, entity, item
     * or weather pipeline exists in the launcher, so nothing of the game is
     * loaded until the game starts.
     */

    // Internal \\

    @Override
    protected void create() {
        create(GeometryPipeline.class);
        create(AnimationPipeline.class);
        create(ShaderPipeline.class);
        create(RenderPipeline.class);
        create(SoundPipeline.class);
        create(MusicPipeline.class);
        create(MenuPipeline.class);
        create(SettingsPipeline.class);
        create(LauncherPipeline.class);
    }
}
