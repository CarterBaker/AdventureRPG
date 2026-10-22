package editor.runtime.dev;

import application.runtime.RuntimeContext;
import editor.bootstrap.commandpipeline.command.CommandStruct;
import editor.runtime.dev.command.DevCommandSystem;
import editor.runtime.dev.freecamera.FreeCameraSystem;
import editor.runtime.dev.item.DevItemSystem;
import editor.runtime.dev.recorder.DevRecorderExportSystem;
import editor.runtime.dev.recorder.DevRecorderOverlaySystem;
import editor.runtime.dev.recorder.DevRecorderSystem;
import editor.runtime.dev.teleport.DevTeleportSystem;
import editor.runtime.dev.time.DevTimeSystem;
import editor.runtime.dev.vehicle.DevVehicleSystem;

public class DevContext extends RuntimeContext {

    /*
     * Dev mode entry point for testing. Runs every system RuntimeContext runs,
     * exactly as the game does, and adds only what the command console
     * drives: a command system that receives what is typed or picked in it,
     * the free camera its fly command switches on, the item giver its give
     * command uses, the vehicle spawner its spawnvehicle and removevehicle
     * commands use, the teleporter its teleport command uses, and the clock
     * mover its time, dawn, noon, dusk and midnight commands use. The
     * recorder's systems carry out a recording session in this window —
     * capturing and playing it back, its interface included, and rendering
     * its export — which its record, play, stop and mark commands drive too.
     * The editor pairs it with a Dev Mode tab exactly as it does a preview.
     * Dev-only systems live beside it in the editor's runtime, never in the
     * game's.
     */

    // Dev
    private DevCommandSystem devCommandSystem;
    private FreeCameraSystem freeCameraSystem;
    private DevItemSystem devItemSystem;
    private DevVehicleSystem devVehicleSystem;
    private DevTeleportSystem devTeleportSystem;
    private DevTimeSystem devTimeSystem;

    // Recorder — the export sets the playhead before the recorder plays it, so it is created first, and the
    // interface renders after every other system so it sees every overlay the window pushes
    private DevRecorderExportSystem devRecorderExportSystem;
    private DevRecorderSystem devRecorderSystem;
    private DevRecorderOverlaySystem devRecorderOverlaySystem;

    // Internal \\

    @Override
    protected void create() {

        super.create();

        // Dev
        this.devCommandSystem = create(DevCommandSystem.class);
        this.freeCameraSystem = create(FreeCameraSystem.class);
        this.devItemSystem = create(DevItemSystem.class);
        this.devVehicleSystem = create(DevVehicleSystem.class);
        this.devTeleportSystem = create(DevTeleportSystem.class);
        this.devTimeSystem = create(DevTimeSystem.class);

        // Recorder
        this.devRecorderExportSystem = create(DevRecorderExportSystem.class);
        this.devRecorderSystem = create(DevRecorderSystem.class);
        this.devRecorderOverlaySystem = create(DevRecorderOverlaySystem.class);
    }

    // Management \\

    public void queueCommand(CommandStruct command) {
        devCommandSystem.queueCommand(command);
    }

    // Hands the recorder everything a take still holds here before it is saved
    public void finishTake() {
        devRecorderOverlaySystem.finishTake();
    }
}
