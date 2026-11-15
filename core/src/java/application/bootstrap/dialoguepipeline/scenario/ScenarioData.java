package application.bootstrap.dialoguepipeline.scenario;

import application.bootstrap.progressionpipeline.util.ValueMapStruct;
import engine.root.DataPackage;

public class ScenarioData extends DataPackage {

    /*
     * Immutable trainer scenario loaded from ARPG: a being to hold a practice talk
     * with and how it stands toward the speaker as the talk opens — its name,
     * profession, traits and moods, its regard for the speaker, its morality and
     * what the speaker is to it — and the name the speaker goes by. The seed is
     * the stable number of its name. Owned by ScenarioHandle for the engine
     * lifetime.
     */

    // Identity
    private final String scenarioName;
    private final short scenarioID;
    private final int seed;

    // Display
    private final String displayName;
    private final String description;

    // Being
    private final String listenerName;
    private final short professionID;
    private final short[] traitIDs;
    private final ValueMapStruct moodLevels;
    private final float affinity;
    private final float morality;
    private final short relationID;

    // Speaker
    private final String speakerName;

    // Constructor \\

    public ScenarioData(
            String scenarioName,
            short scenarioID,
            int seed,
            String displayName,
            String description,
            String listenerName,
            short professionID,
            short[] traitIDs,
            ValueMapStruct moodLevels,
            float affinity,
            float morality,
            short relationID,
            String speakerName) {

        // Identity
        this.scenarioName = scenarioName;
        this.scenarioID = scenarioID;
        this.seed = seed;

        // Display
        this.displayName = displayName;
        this.description = description;

        // Being
        this.listenerName = listenerName;
        this.professionID = professionID;
        this.traitIDs = traitIDs;
        this.moodLevels = moodLevels;
        this.affinity = affinity;
        this.morality = morality;
        this.relationID = relationID;

        // Speaker
        this.speakerName = speakerName;
    }

    // Accessible \\

    public String getScenarioName() {
        return scenarioName;
    }

    public short getScenarioID() {
        return scenarioID;
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

    public String getListenerName() {
        return listenerName;
    }

    public short getProfessionID() {
        return professionID;
    }

    public short[] getTraitIDs() {
        return traitIDs;
    }

    public ValueMapStruct getMoodLevels() {
        return moodLevels;
    }

    public float getAffinity() {
        return affinity;
    }

    public float getMorality() {
        return morality;
    }

    public short getRelationID() {
        return relationID;
    }

    public String getSpeakerName() {
        return speakerName;
    }
}
