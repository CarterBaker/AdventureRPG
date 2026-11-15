package application.bootstrap.dialoguepipeline.util;

import application.bootstrap.dialoguepipeline.moodmanager.MoodManager;
import application.bootstrap.dialoguepipeline.relationmanager.RelationManager;
import application.bootstrap.dialoguepipeline.traitmanager.TraitManager;
import application.bootstrap.dialoguepipeline.wordmanager.WordManager;
import application.bootstrap.progressionpipeline.professionmanager.ProfessionManager;
import engine.root.EngineSetting;
import engine.root.StructPackage;

public class DialogueResolverStruct extends StructPackage {

    /*
     * The managers a dialogue file's names are resolved against, gathered once
     * by each builder so DialogueArpgUtility reads every condition and line
     * the same way. A mood named calm and a relation named stranger resolve
     * to their reserved IDs, the state of a being no mood has hold of and of
     * one that is nothing to the other.
     */

    // Internal
    private final WordManager wordManager;
    private final TraitManager traitManager;
    private final MoodManager moodManager;
    private final RelationManager relationManager;
    private final ProfessionManager professionManager;

    // Constructor \\

    public DialogueResolverStruct(
            WordManager wordManager,
            TraitManager traitManager,
            MoodManager moodManager,
            RelationManager relationManager,
            ProfessionManager professionManager) {

        // Internal
        this.wordManager = wordManager;
        this.traitManager = traitManager;
        this.moodManager = moodManager;
        this.relationManager = relationManager;
        this.professionManager = professionManager;
    }

    // Resolve \\

    public int resolveTag(String tagName) {
        return wordManager.getTagIDFromTagName(tagName);
    }

    public int resolveTrait(String traitName) {
        return traitManager.getTraitIDFromTraitName(traitName);
    }

    public boolean isWordlessTrait(short traitID) {
        return traitManager.getTraitHandleFromTraitID(traitID).isWordless();
    }

    public int resolveMood(String moodName) {
        return moodName.equals(EngineSetting.DIALOGUE_MOOD_CALM)
                ? EngineSetting.REGISTRY_RESERVED_ID
                : moodManager.getMoodIDFromMoodName(moodName);
    }

    public int resolveRelation(String relationName) {
        return relationName.equals(EngineSetting.DIALOGUE_RELATION_STRANGER)
                ? EngineSetting.REGISTRY_RESERVED_ID
                : relationManager.getRelationIDFromRelationName(relationName);
    }

    public int resolveProfession(String professionName) {
        return professionManager.getProfessionIDFromProfessionName(professionName);
    }
}
