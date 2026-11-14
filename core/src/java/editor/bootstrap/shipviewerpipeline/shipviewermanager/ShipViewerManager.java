package editor.bootstrap.shipviewerpipeline.shipviewermanager;

import editor.bootstrap.infopipeline.infomanager.InfoManager;
import editor.runtime.EditorSetting;
import application.bootstrap.shipwrightpipeline.shipwright.ShipwrightSessionInstance;
import engine.root.ManagerPackage;

public class ShipViewerManager extends ManagerPackage {

    /*
     * Owns the ship the Ship Viewer shows: the vehicle last selected under
     * Vehicles in the hierarchy, or the ship on the Shipwright's table as it
     * is being drawn, whichever was chosen last. A vehicle is chosen by its
     * name, looked up in the palette by each viewer; the table by its
     * session, built afresh by each viewer whenever its sheet rests. A table
     * closing lets its session go, and the viewers fall back to the vehicle.
     * The revision moves only when the choice does.
     */

    // Internal
    private InfoManager infoManager;

    // Choice
    private String vehicleName;
    private ShipwrightSessionInstance session;
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

        if (session == null && chosenName.equals(vehicleName))
            return;

        this.vehicleName = chosenName;
        this.session = null;
        this.revision++;
    }

    // The ship on a drafting table every Ship Viewer shows from now on, as it is drawn
    public void chooseSession(ShipwrightSessionInstance chosenSession) {

        if (chosenSession == session)
            return;

        this.session = chosenSession;
        this.revision++;
    }

    // A drafting table closing takes its ship off every Ship Viewer that shows it
    public void releaseSession(ShipwrightSessionInstance releasedSession) {

        if (releasedSession == null || releasedSession != session)
            return;

        this.session = null;
        this.revision++;
    }

    // Accessible \\

    // The chosen vehicle's name, null before any is chosen
    public String getVehicleName() {
        return vehicleName;
    }

    // The drafting table chosen, null while a vehicle is
    public ShipwrightSessionInstance getSession() {
        return session;
    }

    public int getRevision() {
        return revision;
    }
}
