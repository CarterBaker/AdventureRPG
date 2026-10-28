package editor.runtime.biomeviewer;

import application.runtime.input.InputSystem;
import application.runtime.menu.MenuSystem;
import application.runtime.player.PlayerSystem;
import application.runtime.postprocessing.PostProcessingManager;
import application.runtime.sky.SkySystem;
import application.runtime.sky.SquintSystem;
import application.runtime.tree.TreeSystem;
import application.runtime.water.WaterSystem;
import application.runtime.weather.PrecipitationSystem;
import application.runtime.weather.WeatherSystem;
import application.runtime.world.WorldSystem;
import editor.runtime.biomeviewer.menu.BiomeViewerMenuSystem;
import editor.runtime.biomeviewer.minimap.BiomeViewerMinimapSystem;
import editor.runtime.biomeviewer.pause.BiomeViewerPauseSystem;
import editor.runtime.biomeviewer.view.BiomeViewerSystem;
import engine.root.ContextPackage;

public class BiomeViewerContext extends ContextPackage {

    /*
     * Editor tab flying a free camera over the biome planner's world, so the
     * biomes laid in its grid are seen exactly as the game generates them —
     * their relief, cliffs, rivers, coverings, trees and the biomes they
     * spawn within themselves, and how they blend into each other. It runs
     * the systems the game draws a world with and nothing that plays it: no
     * main menu, character, fighting or creatures. Biomes are made in the
     * Hierarchy, Info Panel and Biome Planner; every change there regenerates
     * what this tab shows. It opens paused with the cursor free; Pause or
     * Fly hands it the camera and Pause again lets go. A minimap in the
     * corner shows where the camera stands over the planner's grid.
     */

    // Viewer — created first so the player is sent to the planner's world before it spawns
    private BiomeViewerSystem biomeViewerSystem;

    // Runtime
    private SkySystem skySystem;
    private PlayerSystem playerSystem;
    private MenuSystem menuSystem;
    private WorldSystem worldSystem;
    private InputSystem inputSystem;
    private PostProcessingManager postProcessingManager;
    private WeatherSystem weatherSystem;
    private WaterSystem waterSystem;
    private PrecipitationSystem precipitationSystem;
    private SquintSystem squintSystem;
    private TreeSystem treeSystem;

    // Menu
    private BiomeViewerMenuSystem biomeViewerMenuSystem;
    private BiomeViewerMinimapSystem biomeViewerMinimapSystem;
    private BiomeViewerPauseSystem biomeViewerPauseSystem;

    // Internal \\

    @Override
    protected void create() {

        // Viewer
        this.biomeViewerSystem = create(BiomeViewerSystem.class);

        // Runtime
        this.skySystem = create(SkySystem.class);
        this.playerSystem = create(PlayerSystem.class);
        this.menuSystem = create(MenuSystem.class);
        this.worldSystem = create(WorldSystem.class);
        this.inputSystem = create(InputSystem.class);
        this.postProcessingManager = create(PostProcessingManager.class);
        this.weatherSystem = create(WeatherSystem.class);
        this.waterSystem = create(WaterSystem.class);
        this.precipitationSystem = create(PrecipitationSystem.class);
        this.squintSystem = create(SquintSystem.class);
        this.treeSystem = create(TreeSystem.class);

        // Menu
        this.biomeViewerMenuSystem = create(BiomeViewerMenuSystem.class);
        this.biomeViewerMinimapSystem = create(BiomeViewerMinimapSystem.class);
        this.biomeViewerPauseSystem = create(BiomeViewerPauseSystem.class);
    }

    // Management \

    public void resume() {
        biomeViewerPauseSystem.resume();
    }
}
