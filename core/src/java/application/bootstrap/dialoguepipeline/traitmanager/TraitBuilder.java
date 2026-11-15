package application.bootstrap.dialoguepipeline.traitmanager;

import java.io.File;

import application.bootstrap.dialoguepipeline.moodmanager.MoodManager;
import application.bootstrap.dialoguepipeline.trait.TraitData;
import application.bootstrap.dialoguepipeline.trait.TraitHandle;
import application.bootstrap.dialoguepipeline.util.DialogueFeatureUtility;
import application.bootstrap.progressionpipeline.util.ProgressionArpgUtility;
import application.bootstrap.progressionpipeline.util.ValueMapStruct;
import engine.root.BuilderPackage;
import engine.root.EngineSetting;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class TraitBuilder extends BuilderPackage {

    /*
     * Parses trait ARPG files and builds TraitHandle instances. Each file
     * lists its traits under a "traits" array, each named by its "name"
     * field: the "display_name" and "description" it is shown by, the
     * "opposite" trait it is never held beside, whether it leaves its holder
     * "wordless", the level each mood under "moods" rests at in its holder,
     * and the "affinity_gain" and "affinity_loss" its holder's regard is
     * scaled by. An opposite is registered by name as it is read, so two
     * traits may name each other. Bootstrap-only.
     */

    // Internal
    private TraitManager traitManager;
    private MoodManager moodManager;

    // Base \\

    @Override
    protected void get() {
        this.traitManager = get(TraitManager.class);
        this.moodManager = get(MoodManager.class);
    }

    // Build \\

    ObjectArrayList<TraitHandle> build(File arpgFile) {

        ArpgObjectStruct rootArpg = ArpgUtility.loadObject(arpgFile);
        ArpgArrayStruct traitArray = ArpgUtility.validateArray(rootArpg, "traits");
        ObjectArrayList<TraitHandle> traits = new ObjectArrayList<>();

        for (int i = 0; i < traitArray.size(); i++)
            traits.add(parseTrait(traitArray.get(i).getAsObject(), arpgFile));

        return traits;
    }

    // Parse \\

    private TraitHandle parseTrait(ArpgObjectStruct traitArpg, File arpgFile) {

        String traitName = ArpgUtility.validateString(traitArpg, "name");
        String owner = "Trait '" + traitName + "' in " + arpgFile.getPath();
        String oppositeName = ArpgUtility.getString(traitArpg, "opposite", EngineSetting.DIALOGUE_TEXT_NONE);
        ValueMapStruct moodLeans = ProgressionArpgUtility.parseValueMap(
                traitArpg, "moods", moodManager::getMoodIDFromMoodName, owner);
        float affinityGain = ArpgUtility.getFloat(
                traitArpg, "affinity_gain", EngineSetting.DIALOGUE_TRAIT_SCALE_DEFAULT);
        float affinityLoss = ArpgUtility.getFloat(
                traitArpg, "affinity_loss", EngineSetting.DIALOGUE_TRAIT_SCALE_DEFAULT);

        if (oppositeName.equals(traitName))
            throwException(owner + " names itself as its \"opposite\".");

        if (affinityGain < 0f || affinityLoss < 0f)
            throwException(owner + " needs an \"affinity_gain\" and an \"affinity_loss\" of at least 0.");

        for (int i = 0; i < moodLeans.size(); i++)
            if (moodLeans.getValue(i) < 0f || moodLeans.getValue(i) > 1f)
                throwException(owner + " must rest every mood under \"moods\" between 0 and 1.");

        TraitData traitData = new TraitData(
                traitName,
                traitManager.registerTraitName(traitName),
                DialogueFeatureUtility.hashName(traitName),
                ArpgUtility.getString(traitArpg, "display_name", traitName),
                ArpgUtility.getString(traitArpg, "description", EngineSetting.DIALOGUE_TEXT_NONE),
                oppositeName.isEmpty()
                        ? EngineSetting.REGISTRY_RESERVED_ID
                        : traitManager.registerTraitName(oppositeName),
                ArpgUtility.getBoolean(traitArpg, "wordless", false),
                moodLeans,
                affinityGain,
                affinityLoss);

        TraitHandle traitHandle = create(TraitHandle.class);
        traitHandle.constructor(traitData);

        return traitHandle;
    }
}
