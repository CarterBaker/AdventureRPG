package application.bootstrap.progressionpipeline.gender;

import engine.root.HandlePackage;

public class GenderHandle extends HandlePackage {

    /*
     * Persistent reference to a loaded gender definition. Registered and owned
     * by GenderManager. Delegates all accessors through GenderData.
     */

    // Internal
    private GenderData genderData;

    // Constructor \\

    public void constructor(GenderData genderData) {

        // Internal
        this.genderData = genderData;
    }

    // Accessible \\

    public GenderData getGenderData() {
        return genderData;
    }

    public String getGenderName() {
        return genderData.getGenderName();
    }

    public short getGenderID() {
        return genderData.getGenderID();
    }

    public String getDisplayName() {
        return genderData.getDisplayName();
    }
}
