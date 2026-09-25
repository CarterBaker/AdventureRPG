package editor.dev;

import application.runtime.input.InputSystem;
import application.runtime.lighting.SkySystem;
import application.runtime.menu.MenuSystem;
import application.runtime.menueventsmanager.MenuEventsManager;
import application.runtime.postprocessing.PostProcessingManager;
import application.runtime.recording.RecordingInputSystem;
import application.runtime.weather.PrecipitationSystem;
import application.runtime.weather.WeatherSystem;
import application.runtime.world.WorldSystem;
import editor.dev.freecamera.FreeCameraSystem;
import engine.root.ContextPackage;

public class DevContext extends ContextPackage {

    /*
     * Dev mode entry point for testing. Runs the same world, sky, lighting,
     * weather, menu, and capture systems as RuntimeContext, but spawns a free
     * camera in place of the player and opens no main menu — the window drops
     * straight into the world, no character is drawn or controlled, and the
     * camera flies through it with no physics. The editor pairs it with a Dev
     * Mode tab exactly as it does a preview. Dev-only systems live beside it
     * in the editor, never in runtime.
     */

    // Runtime
    private SkySystem skySystem;
    private FreeCameraSystem freeCameraSystem;
    private MenuSystem menuSystem;
    private MenuEventsManager menuEventsManager;
    private WorldSystem worldSystem;
    private InputSystem playerInputSystem;
    private RecordingInputSystem recordingInputSystem;
    private PostProcessingManager postProcessingManager;
    private WeatherSystem weatherSystem;
    private PrecipitationSystem precipitationSystem;

    // Internal \\

    @Override
    protected void create() {

        // Runtime
        this.skySystem = create(SkySystem.class);
        this.freeCameraSystem = create(FreeCameraSystem.class);
        this.menuSystem = create(MenuSystem.class);
        this.menuEventsManager = create(MenuEventsManager.class);
        this.worldSystem = create(WorldSystem.class);
        this.playerInputSystem = create(InputSystem.class);
        this.recordingInputSystem = create(RecordingInputSystem.class);
        this.postProcessingManager = create(PostProcessingManager.class);
        this.weatherSystem = create(WeatherSystem.class);
        this.precipitationSystem = create(PrecipitationSystem.class);
    }
}
