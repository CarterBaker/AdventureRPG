package application.bootstrap.dialoguepipeline.util;

import engine.root.EngineSetting;
import engine.root.StructPackage;

public class DialogueConditionStruct extends StructPackage {

    /*
     * Immutable test of who may use a word, a line, a reply or a topic,
     * written under "when": the traits a being must all have and the ones it
     * must not, the moods its dominant mood must be one of, the professions
     * and relations it must be one of, the span its affinity for the speaker
     * must lie in, and where the talk must stand on a topic. An empty list
     * asks nothing. matches() tests a word or a line. permits() tests a
     * reply or a topic, and adds one rule: a wordless being — a beast — is
     * only ever permitted one whose condition names a wordless trait, so
     * nothing written for speech is ever put in its mouth.
     */

    // Values
    public static final DialogueConditionStruct ALWAYS = new DialogueConditionStruct(
            new short[0], new short[0], new short[0], new short[0], new short[0],
            -EngineSetting.AFFINITY_MAX, EngineSetting.AFFINITY_MAX, DialogueTopicState.ANY, false);

    // Traits
    private final short[] traitIDs;
    private final short[] excludedTraitIDs;
    private final boolean wordless;

    // State
    private final short[] moodIDs;
    private final short[] professionIDs;
    private final short[] relationIDs;

    // Regard
    private final float affinityMin;
    private final float affinityMax;

    // Topic
    private final DialogueTopicState topicState;

    // Constructor \\

    public DialogueConditionStruct(
            short[] traitIDs,
            short[] excludedTraitIDs,
            short[] moodIDs,
            short[] professionIDs,
            short[] relationIDs,
            float affinityMin,
            float affinityMax,
            DialogueTopicState topicState,
            boolean wordless) {

        // Traits
        this.traitIDs = traitIDs;
        this.excludedTraitIDs = excludedTraitIDs;
        this.wordless = wordless;

        // State
        this.moodIDs = moodIDs;
        this.professionIDs = professionIDs;
        this.relationIDs = relationIDs;

        // Regard
        this.affinityMin = affinityMin;
        this.affinityMax = affinityMax;

        // Topic
        this.topicState = topicState;
    }

    // Match \\

    public boolean permits(DialogueProfileStruct profile, DialogueTopicState talkState) {
        return (!profile.isWordless() || wordless) && matches(profile, talkState);
    }

    public boolean matches(DialogueProfileStruct profile, DialogueTopicState talkState) {

        if (topicState != DialogueTopicState.ANY && topicState != talkState)
            return false;

        if (profile.getAffinity() < affinityMin || profile.getAffinity() > affinityMax)
            return false;

        for (int i = 0; i < traitIDs.length; i++)
            if (!profile.hasTrait(traitIDs[i]))
                return false;

        for (int i = 0; i < excludedTraitIDs.length; i++)
            if (profile.hasTrait(excludedTraitIDs[i]))
                return false;

        return isOneOf(moodIDs, profile.getDominantMoodID())
                && isOneOf(professionIDs, profile.getProfessionID())
                && isOneOf(relationIDs, profile.getRelationID());
    }

    // True when the list asks nothing or holds the ID
    private boolean isOneOf(short[] ids, short id) {

        if (ids.length == 0)
            return true;

        for (int i = 0; i < ids.length; i++)
            if (ids[i] == id)
                return true;

        return false;
    }
}
