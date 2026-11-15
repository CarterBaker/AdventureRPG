package application.bootstrap.dialoguepipeline.trait;

import application.bootstrap.progressionpipeline.util.ValueMapStruct;
import engine.root.DataPackage;

public class TraitData extends DataPackage {

    /*
     * Immutable personality trait loaded from ARPG: a lasting part of a being's
     * nature or of how it speaks. It names the trait it can never be held beside,
     * whether it leaves its holder wordless — a beast, answering only in the lines
     * written for it — the level each mood rests at in its holder, which also
     * quickens that mood, and how it scales what its holder's regard for a speaker
     * gains and loses. The seed is the stable number of its name the dialogue
     * models weigh it by. Owned by TraitHandle for the engine lifetime.
     */

    // Identity
    private final String traitName;
    private final short traitID;
    private final int seed;

    // Display
    private final String displayName;
    private final String description;

    // Nature
    private final short oppositeID;
    private final boolean wordless;

    // Sway
    private final ValueMapStruct moodLeans;
    private final float affinityGain;
    private final float affinityLoss;

    // Constructor \\

    public TraitData(
            String traitName,
            short traitID,
            int seed,
            String displayName,
            String description,
            short oppositeID,
            boolean wordless,
            ValueMapStruct moodLeans,
            float affinityGain,
            float affinityLoss) {

        // Identity
        this.traitName = traitName;
        this.traitID = traitID;
        this.seed = seed;

        // Display
        this.displayName = displayName;
        this.description = description;

        // Nature
        this.oppositeID = oppositeID;
        this.wordless = wordless;

        // Sway
        this.moodLeans = moodLeans;
        this.affinityGain = affinityGain;
        this.affinityLoss = affinityLoss;
    }

    // Accessible \\

    public String getTraitName() {
        return traitName;
    }

    public short getTraitID() {
        return traitID;
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

    public short getOppositeID() {
        return oppositeID;
    }

    public boolean isWordless() {
        return wordless;
    }

    public ValueMapStruct getMoodLeans() {
        return moodLeans;
    }

    public float getAffinityGain() {
        return affinityGain;
    }

    public float getAffinityLoss() {
        return affinityLoss;
    }
}
