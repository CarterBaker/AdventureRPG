package editor.bootstrap.shipviewerpipeline.shipviewermanager;

import editor.bootstrap.infopipeline.infomanager.InfoManager;
import editor.runtime.EditorSetting;
import engine.root.ManagerPackage;

public class ShipViewerManager extends ManagerPackage {

    /*
     * Owns the vehicle the Ship Viewer shows: the one last selected under
     * Vehicles in the hierarchy, or opened or saved at the Shipwright,
     * whichever came last. The choice is a name, looked up in the palette
     * by each viewer, and its revision moves only when the name does.
     */

    // Internal
    private InfoManager infoManager;

    // Choice
    private String vehicleName;
    private int revision;

    // Base \\

    @Override
    protected void get() {
        this.infoManager = get(InfoManager.class);
    }

    @Override
    protected void awake() {
        infoManager.addSelectionListener(EditorSetting.INFO_SCHEMA_VEHICLES, entry -> choose(entry.getEntryName()));
    }

    // Management \\

    // The vehicle every Ship Viewer shows from now on
    public void choose(String chosenName) {

        if (chosenName.equals(vehicleName))
            return;

        this.vehicleName = chosenName;
        this.revision++;
    }

    // Accessible \\

    // The chosen vehicle's name, null before any is chosen
    public String getVehicleName() {
        return vehicleName;
    }

    public int getRevision() {
        return revision;
    }
}
