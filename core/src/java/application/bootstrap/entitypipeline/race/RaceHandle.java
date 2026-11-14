package application.bootstrap.entitypipeline.race;

import engine.root.HandlePackage;

public class RaceHandle extends HandlePackage {

    /*
     * Persistent reference to a loaded race. Registered and owned by
     * RaceManager and shared by every entity template of the race — which
     * gender a being of it is lives on its EntityInstance, never here.
     */

    // Internal
    private RaceData raceData;

    // Constructor \\

    public void constructor(RaceData raceData) {

        // Internal
        this.raceData = raceData;
    }

    // Accessible \\

    public RaceData getRaceData() {
        return raceData;
    }

    public String getRaceName() {
        return raceData.getRaceName();
    }
}
