package application.bootstrap.dialoguepipeline.scenariomanager;

import java.io.File;

import application.bootstrap.dialoguepipeline.moodmanager.MoodManager;
import application.bootstrap.dialoguepipeline.relationmanager.RelationManager;
import application.bootstrap.dialoguepipeline.scenario.ScenarioData;
import application.bootstrap.dialoguepipeline.scenario.ScenarioHandle;
import application.bootstrap.dialoguepipeline.traitmanager.TraitManager;
import application.bootstrap.dialoguepipeline.util.DialogueFeatureUtility;
import application.bootstrap.progressionpipeline.professionmanager.ProfessionManager;
import application.bootstrap.progressionpipeline.util.ProgressionArpgUtility;
import engine.root.BuilderPackage;
import engine.root.EngineSetting;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class ScenarioBuilder extends BuilderPackage {

    /*
     * Parses scenario ARPG files and builds ScenarioHandle instances. Each
     * file lists its scenarios under a "scenarios" array, each named by its
     * "name" field: the "display_name" and "description" it is shown by, the
     * being's "listener" name, "profession" and "traits", the level each mood
     * under "moods" stands at, its "affinity" for the speaker, its "morality"
     * from -1 to 1, the "relation" the speaker is to it, and the "speaker"
     * name. Everything but the name is optional. Bootstrap-only.
     */

    // Internal
    private ScenarioManager scenarioManager;
    private TraitManager traitManager;
    private MoodManager moodManager;
    private RelationManager relationManager;
    private ProfessionManager professionManager;

    // Base \\

    @Override
    protected void get() {
        this.scenarioManager = get(ScenarioManager.class);
        this.traitManager = get(TraitManager.class);
        this.moodManager = get(MoodManager.class);
        this.relationManager = get(RelationManager.class);
        this.professionManager = get(ProfessionManager.class);
    }

    // Build \\

    ObjectArrayList<ScenarioHandle> build(File arpgFile) {

        ArpgObjectStruct rootArpg = ArpgUtility.loadObject(arpgFile);
        ArpgArrayStruct scenarioArray = ArpgUtility.validateArray(rootArpg, "scenarios");
        ObjectArrayList<ScenarioHandle> scenarios = new ObjectArrayList<>();

        for (int i = 0; i < scenarioArray.size(); i++)
            scenarios.add(parseScenario(scenarioArray.get(i).getAsObject(), arpgFile));

        return scenarios;
    }

    // Parse \\

    private ScenarioHandle parseScenario(ArpgObjectStruct scenarioArpg, File arpgFile) {

        String scenarioName = ArpgUtility.validateString(scenarioArpg, "name");
        String owner = "Scenario '" + scenarioName + "' in " + arpgFile.getPath();
        String professionName = ArpgUtility.getString(scenarioArpg, "profession", EngineSetting.DIALOGUE_TEXT_NONE);
        String relationName = ArpgUtility.getString(scenarioArpg, "relation", EngineSetting.DIALOGUE_TEXT_NONE);
        short[] traitIDs = ProgressionArpgUtility.parseNames(
                scenarioArpg, "traits", traitManager::getTraitIDFromTraitName);

        if (traitIDs.length > EngineSetting.DIALOGUE_TRAIT_MAX)
            throwException(owner + " lists " + traitIDs.length + " traits — a being holds no more than "
                    + EngineSetting.DIALOGUE_TRAIT_MAX + ".");

        ScenarioData scenarioData = new ScenarioData(
                scenarioName,
                scenarioManager.registerScenarioName(scenarioName),
                DialogueFeatureUtility.hashName(scenarioName),
                ArpgUtility.getString(scenarioArpg, "display_name", scenarioName),
                ArpgUtility.getString(scenarioArpg, "description", EngineSetting.DIALOGUE_TEXT_NONE),
                ArpgUtility.getString(scenarioArpg, "listener", EngineSetting.DIALOGUE_TEXT_NONE),
                professionName.isEmpty()
                        ? EngineSetting.REGISTRY_RESERVED_ID
                        : professionManager.getProfessionIDFromProfessionName(professionName),
                traitIDs,
                ProgressionArpgUtility.parseValueMap(
                        scenarioArpg, "moods", moodManager::getMoodIDFromMoodName, owner),
                ArpgUtility.getFloat(scenarioArpg, "affinity", 0f),
                ArpgUtility.getFloat(scenarioArpg, "morality", 0f),
                relationName.isEmpty()
                        ? EngineSetting.REGISTRY_RESERVED_ID
                        : relationManager.getRelationIDFromRelationName(relationName),
                ArpgUtility.getString(scenarioArpg, "speaker", EngineSetting.DIALOGUE_SPEAKER_NAME_DEFAULT));

        ScenarioHandle scenarioHandle = create(ScenarioHandle.class);
        scenarioHandle.constructor(scenarioData);

        return scenarioHandle;
    }
}
