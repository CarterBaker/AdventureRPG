package application.bootstrap.progressionpipeline.gender;

import engine.root.DataPackage;

public class GenderData extends DataPackage {

    /*
     * Immutable gender definition loaded from ARPG: its name and how it reads
     * on screen. Owned by GenderHandle for the engine lifetime.
     */

    // Identity
    private final String genderName;
    private final short genderID;

    // Presentation
    private final String displayName;

    // Constructor \\

    public GenderData(
            String genderName,
            short genderID,
            String displayName) {

        // Identity
        this.genderName = genderName;
        this.genderID = genderID;

        // Presentation
        this.displayName = displayName;
    }

    // Accessible \\

    public String getGenderName() {
        return genderName;
    }

    public short getGenderID() {
        return genderID;
    }

    public String getDisplayName() {
        return displayName;
    }
}
