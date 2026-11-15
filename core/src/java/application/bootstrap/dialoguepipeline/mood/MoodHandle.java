package application.bootstrap.dialoguepipeline.mood;

import engine.root.HandlePackage;

public class MoodHandle extends HandlePackage {

    /*
     * Persistent reference to a loaded mood definition. Registered and owned by
     * MoodManager. Delegates all accessors through MoodData.
     */

    // Internal
    private MoodData moodData;

    // Constructor \\

    public void constructor(MoodData moodData) {

        // Internal
        this.moodData = moodData;
    }

    // Accessible \\

    public MoodData getMoodData() {
        return moodData;
    }

    public String getMoodName() {
        return moodData.getMoodName();
    }

    public short getMoodID() {
        return moodData.getMoodID();
    }

    public int getSeed() {
        return moodData.getSeed();
    }

    public String getDisplayName() {
        return moodData.getDisplayName();
    }

    public String getDescription() {
        return moodData.getDescription();
    }

    public float getHalfLifeSeconds() {
        return moodData.getHalfLifeSeconds();
    }
}
