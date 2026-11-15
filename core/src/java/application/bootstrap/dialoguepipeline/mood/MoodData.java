package application.bootstrap.dialoguepipeline.mood;

import engine.root.DataPackage;

public class MoodData extends DataPackage {

    /*
     * Immutable mood definition loaded from ARPG: a feeling a being holds for a
     * while, such as anger or fear, the name it is shown by, and the seconds in
     * which half of it fades back toward what the being's traits lean it to. The
     * seed is the stable number of its name the dialogue models weigh it by. Owned
     * by MoodHandle for the engine lifetime.
     */

    // Identity
    private final String moodName;
    private final short moodID;
    private final int seed;

    // Display
    private final String displayName;
    private final String description;

    // Decay
    private final float halfLifeSeconds;

    // Constructor \\

    public MoodData(
            String moodName,
            short moodID,
            int seed,
            String displayName,
            String description,
            float halfLifeSeconds) {

        // Identity
        this.moodName = moodName;
        this.moodID = moodID;
        this.seed = seed;

        // Display
        this.displayName = displayName;
        this.description = description;

        // Decay
        this.halfLifeSeconds = halfLifeSeconds;
    }

    // Accessible \\

    public String getMoodName() {
        return moodName;
    }

    public short getMoodID() {
        return moodID;
    }

    public int getSeed() {
        return seed;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public float getHalfLifeSeconds() {
        return halfLifeSeconds;
    }
}
