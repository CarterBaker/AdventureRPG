package application.bootstrap.dialoguepipeline.util;

import application.bootstrap.progressionpipeline.util.ValueMapStruct;
import engine.root.EngineSetting;
import engine.root.StructPackage;

public class DialogueLeanStruct extends StructPackage {

    /*
     * Immutable account of who a reply comes naturally to, written under a
     * reply's "leans" and added to what training has taught: a weight for the
     * reply itself, one for each trait a being has, one for each mood scaled
     * by how strongly it is felt, one for what the speaker is to it, and one
     * each scaled by its affinity for the speaker, its own morality and the
     * tone it was spoken to in, each of those running from -1 to 1.
     */

    // Values
    public static final DialogueLeanStruct NONE = new DialogueLeanStruct(
            0f, ValueMapStruct.EMPTY, ValueMapStruct.EMPTY, ValueMapStruct.EMPTY, 0f, 0f, 0f);

    // Weights
    private final float weight;
    private final ValueMapStruct traits;
    private final ValueMapStruct moods;
    private final ValueMapStruct relations;
    private final float affinity;
    private final float morality;
    private final float tone;

    // Constructor \\

    public DialogueLeanStruct(
            float weight,
            ValueMapStruct traits,
            ValueMapStruct moods,
            ValueMapStruct relations,
            float affinity,
            float morality,
            float tone) {

        // Weights
        this.weight = weight;
        this.traits = traits;
        this.moods = moods;
        this.relations = relations;
        this.affinity = affinity;
        this.morality = morality;
        this.tone = tone;
    }

    // Resolve \\

    // How naturally the reply comes to the being, spoken to in the tone given
    public float resolve(DialogueProfileStruct profile, float talkTone) {

        float total = weight
                + affinity * profile.getAffinity() / EngineSetting.AFFINITY_MAX
                + morality * profile.getMorality()
                + tone * talkTone
                + relations.find(profile.getRelationID(), 0f);

        for (int i = 0; i < traits.size(); i++)
            if (profile.hasTrait(traits.getID(i)))
                total += traits.getValue(i);

        for (int i = 0; i < moods.size(); i++)
            total += moods.getValue(i) * profile.getMoodLevel(moods.getID(i));

        return total;
    }
}
