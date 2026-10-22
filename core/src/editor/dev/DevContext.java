package editor.dev;

import application.runtime.RuntimeContext;
import editor.bootstrap.commandpipeline.command.CommandStruct;
import editor.dev.command.DevCommandSystem;
import editor.dev.freecamera.FreeCameraSystem;
import editor.dev.item.DevItemSystem;
import editor.dev.recorder.DevRecorderExportSystem;
import editor.dev.recorder.DevRecorderSystem;
import editor.dev.teleport.DevTeleportSystem;
import editor.dev.time.DevTimeSystem;
import editor.dev.vehicle.DevVehicleSystem;

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
     * capturing and playing it back, and rendering its export — which its
     * record, play, stop and mark commands drive too. The editor pairs it
     * with a Dev Mode tab exactly as it does a preview. Dev-only systems live
     * beside it in the editor, never in runtime.
     */

    // Dev
    private DevCommandSystem devCommandSystem;
    private FreeCameraSystem freeCameraSystem;
    private DevItemSystem devItemSystem;
    private DevVehicleSystem devVehicleSystem;
    private DevTeleportSystem devTeleportSystem;
    private DevTimeSystem devTimeSystem;

    // Recorder — the export sets the playhead before the recorder plays it, so it is created first
    private DevRecorderExportSystem devRecorderExportSystem;
    private DevRecorderSystem devRecorderSystem;

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
    }

    // Management \\

    public void queueCommand(CommandStruct command) {
        devCommandSystem.queueCommand(command);
    }
}
