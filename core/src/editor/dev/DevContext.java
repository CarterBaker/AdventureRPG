package editor.dev;

import application.runtime.RuntimeContext;
import editor.bootstrap.commandpipeline.command.CommandStruct;
import editor.dev.command.DevCommandSystem;
import editor.dev.freecamera.FreeCameraSystem;
import editor.dev.item.DevItemSystem;
import editor.dev.teleport.DevTeleportSystem;
import editor.dev.vehicle.DevVehicleSystem;

public class DevContext extends RuntimeContext {

    /*
     * Dev mode entry point for testing. Runs every system RuntimeContext runs,
     * exactly as the game does, and adds only what the command console
     * drives: a command system that receives what is typed or picked in it,
     * the free camera its fly command switches on, the item giver its give
     * command uses, the vehicle spawner its spawnvehicle and removevehicle
     * commands use, and the teleporter its teleport command uses. The editor
     * pairs it with a Dev Mode tab exactly as it does a preview. Dev-only
     * systems live beside it in the editor, never in runtime.
     */

    // Dev
    private DevCommandSystem devCommandSystem;
    private FreeCameraSystem freeCameraSystem;
    private DevItemSystem devItemSystem;
    private DevVehicleSystem devVehicleSystem;
    private DevTeleportSystem devTeleportSystem;

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
    }

    // Management \\

    public void queueCommand(CommandStruct command) {
        devCommandSystem.queueCommand(command);
    }
}
