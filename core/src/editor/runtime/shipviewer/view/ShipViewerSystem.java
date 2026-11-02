package editor.runtime.shipviewer.view;

import application.bootstrap.vehiclepipeline.vehicle.VehicleHandle;
import application.bootstrap.vehiclepipeline.vehiclemanager.VehicleManager;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ShipViewerSystem extends SystemPackage {

    /*
     * Chooses the vehicle the Ship Viewer shows. Whenever the palette
     * changes, the type registered last is shown if it is new — so a ship
     * saved at the Shipwright shows the moment it is registered again —
     * and otherwise the one shown is looked up again by name, as a type
     * replaced or removed frees its meshes; with that one gone the first in
     * the palette shows. Nothing is looked up while the palette rests.
     */

    // Internal
    private VehicleManager vehicleManager;

    // Shown
    private VehicleHandle vehicleHandle;
    private VehicleHandle followedHandle;
    private int paletteRevision;
    private int revision;

    // Base \\

    @Override
    protected void create() {
        this.paletteRevision = EngineSetting.INDEX_NOT_FOUND;
    }

    @Override
    protected void get() {
        this.vehicleManager = get(VehicleManager.class);
    }

    // Update \\

    @Override
    protected void update() {

        int current = vehicleManager.getPaletteRevision();

        if (current == paletteRevision)
            return;

        paletteRevision = current;

        VehicleHandle latest = vehicleManager.getLatestVehicleHandle();

        if (latest != null && latest != followedHandle) {
            followedHandle = latest;
            show(latest);
            return;
        }

        show(resolve(vehicleHandle));
    }

    // The type standing under the shown one's name now, or the first in the palette once none does
    private VehicleHandle resolve(VehicleHandle shown) {

        if (shown != null && vehicleManager.hasVehicle(shown.getVehicleName()))
            return vehicleManager.getVehicleHandleFromVehicleName(shown.getVehicleName());

        ObjectArrayList<VehicleHandle> vehicleHandles = vehicleManager.getVehicleHandles();

        return vehicleHandles.isEmpty() ? null : vehicleHandles.get(0);
    }

    // Management \\

    // The vehicle a number of places along the palette from the one shown, wrapping at either end
    public void step(int step) {

        ObjectArrayList<VehicleHandle> vehicleHandles = vehicleManager.getVehicleHandles();

        if (vehicleHandles.isEmpty())
            return;

        int index = vehicleHandle == null ? 0 : vehicleHandles.indexOf(vehicleHandle);

        show(vehicleHandles.get(Math.floorMod(index + step, vehicleHandles.size())));
    }

    private void show(VehicleHandle shown) {
        this.vehicleHandle = shown;
        this.revision++;
    }

    // Accessible \\

    public VehicleHandle getVehicleHandle() {
        return vehicleHandle;
    }

    // Counted up whenever what is shown is chosen again
    public int getRevision() {
        return revision;
    }

    public int getVehicleIndex() {
        return vehicleHandle == null ? EngineSetting.INDEX_NOT_FOUND
                : vehicleManager.getVehicleHandles().indexOf(vehicleHandle);
    }

    public int getVehicleCount() {
        return vehicleManager.getVehicleHandles().size();
    }
}
