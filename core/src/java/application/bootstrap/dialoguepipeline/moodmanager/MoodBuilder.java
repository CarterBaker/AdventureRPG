package application.bootstrap.dialoguepipeline.moodmanager;

import java.io.File;

import application.bootstrap.dialoguepipeline.mood.MoodData;
import application.bootstrap.dialoguepipeline.mood.MoodHandle;
import application.bootstrap.dialoguepipeline.util.DialogueFeatureUtility;
import engine.root.BuilderPackage;
import engine.root.EngineSetting;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class MoodBuilder extends BuilderPackage {

    /*
     * Parses mood ARPG files and builds MoodHandle instances. Each file lists
     * its moods under a "moods" array, each named by its "name" field: the
     * "display_name" and "description" it is shown by and the
     * "half_life_seconds" in which half of it fades. The name calm is
     * reserved for a being no mood has hold of. Bootstrap-only.
     */

    // Internal
    private MoodManager moodManager;

    // Base \\

    @Override
    protected void get() {
        this.moodManager = get(MoodManager.class);
    }

    // Build \\

    ObjectArrayList<MoodHandle> build(File arpgFile) {

        ArpgObjectStruct rootArpg = ArpgUtility.loadObject(arpgFile);
        ArpgArrayStruct moodArray = ArpgUtility.validateArray(rootArpg, "moods");
        ObjectArrayList<MoodHandle> moods = new ObjectArrayList<>();

        for (int i = 0; i < moodArray.size(); i++)
            moods.add(parseMood(moodArray.get(i).getAsObject(), arpgFile));

        return moods;
    }

    // Parse \\

    private MoodHandle parseMood(ArpgObjectStruct moodArpg, File arpgFile) {

        String moodName = ArpgUtility.validateString(moodArpg, "name");
        String owner = "Mood '" + moodName + "' in " + arpgFile.getPath();
        float halfLifeSeconds = ArpgUtility.getFloat(
                moodArpg, "half_life_seconds", EngineSetting.DIALOGUE_MOOD_HALF_LIFE_SECONDS);

        if (moodName.equals(EngineSetting.DIALOGUE_MOOD_CALM))
            throwException(owner + " takes the reserved name \"" + EngineSetting.DIALOGUE_MOOD_CALM
                    + "\", the state of a being no mood has hold of.");

        if (halfLifeSeconds <= 0f)
            throwException(owner + " needs a \"half_life_seconds\" above 0.");

        MoodData moodData = new MoodData(
                moodName,
                moodManager.registerMoodName(moodName),
                DialogueFeatureUtility.hashName(moodName),
                ArpgUtility.getString(moodArpg, "display_name", moodName),
                ArpgUtility.getString(moodArpg, "description", EngineSetting.DIALOGUE_TEXT_NONE),
                halfLifeSeconds);

        MoodHandle moodHandle = create(MoodHandle.class);
        moodHandle.constructor(moodData);

        return moodHandle;
    }
}
