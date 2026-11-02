package editor.runtime.shipviewer.view;

import application.bootstrap.vehiclepipeline.vehicle.VehicleHandle;
import application.bootstrap.vehiclepipeline.vehiclemanager.VehicleManager;
import editor.bootstrap.shipviewerpipeline.shipviewermanager.ShipViewerManager;
import engine.root.EngineSetting;
import engine.root.SystemPackage;

public class ShipViewerSystem extends SystemPackage {

    /*
     * Finds the vehicle the Ship Viewer shows: the one ShipViewerManager
     * names, looked up in the palette again whenever the choice or the
     * palette changes, as a ship saved again replaces its type and frees the
     * old one's meshes. Nothing is looked up while both rest.
     */

    // Internal
    private VehicleManager vehicleManager;
    private ShipViewerManager shipViewerManager;

    // Shown
    private VehicleHandle vehicleHandle;
    private int chosenRevision;
    private int paletteRevision;
    private int revision;

    // Base \\

    @Override
    protected void create() {
        this.chosenRevision = EngineSetting.INDEX_NOT_FOUND;
        this.paletteRevision = EngineSetting.INDEX_NOT_FOUND;
    }

    @Override
    protected void get() {
        this.vehicleManager = get(VehicleManager.class);
        this.shipViewerManager = get(ShipViewerManager.class);
    }

    // Update \\

    @Override
    protected void update() {

        int chosen = shipViewerManager.getRevision();
        int palette = vehicleManager.getPaletteRevision();

        if (chosen == chosenRevision && palette == paletteRevision)
            return;

        chosenRevision = chosen;
        paletteRevision = palette;

        String vehicleName = shipViewerManager.getVehicleName();

        this.vehicleHandle = vehicleName != null && vehicleManager.hasVehicle(vehicleName)
                ? vehicleManager.getVehicleHandleFromVehicleName(vehicleName)
                : null;
        this.revision++;
    }

    // Accessible \\

    // The vehicle shown, null while none is chosen or the one chosen is not in the palette
    public VehicleHandle getVehicleHandle() {
        return vehicleHandle;
    }

    public String getVehicleName() {
        return shipViewerManager.getVehicleName();
    }

    // Counted up whenever what is shown is looked up again
    public int getRevision() {
        return revision;
    }
}
