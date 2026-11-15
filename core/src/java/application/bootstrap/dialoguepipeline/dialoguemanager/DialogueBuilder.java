package application.bootstrap.dialoguepipeline.dialoguemanager;

import java.io.File;

import application.bootstrap.dialoguepipeline.lesson.LessonStruct;
import application.bootstrap.dialoguepipeline.util.DialogueArpgUtility;
import engine.root.BuilderPackage;
import engine.root.EngineSetting;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class DialogueBuilder extends BuilderPackage {

    /*
     * Parses lesson ARPG files into LessonStructs. Each file lists its
     * lessons under a "lessons" array, each one judged turn exactly as
     * LessonStruct.toArpg() writes it: what was "heard" and the "intent" it
     * meant, the listener's "traits", "moods", "affinity", "morality",
     * "relation" and "profession", the "reply" given, what was "said", the
     * "lines" and "words" it was built from, the "verdict" and what it is
     * worth to "understanding", "response" and "wording", and the "better"
     * reply when one was named. Names are kept as written and resolved only
     * when a lesson is learned, so a lesson naming content since removed
     * loads all the same. Bootstrap-only.
     */

    // Build \\

    ObjectArrayList<LessonStruct> build(File arpgFile, boolean recorded) {

        ArpgObjectStruct rootArpg = ArpgUtility.loadObject(arpgFile);
        ArpgArrayStruct lessonArray = ArpgUtility.validateArray(rootArpg, "lessons");
        ObjectArrayList<LessonStruct> lessons = new ObjectArrayList<>();

        for (int i = 0; i < lessonArray.size(); i++)
            lessons.add(parseLesson(lessonArray.get(i).getAsObject(), recorded));

        return lessons;
    }

    // Parse \\

    private LessonStruct parseLesson(ArpgObjectStruct lessonArpg, boolean recorded) {

        ArpgObjectStruct moodsArpg = ArpgUtility.hasObject(lessonArpg, "moods")
                ? lessonArpg.getAsObject("moods")
                : new ArpgObjectStruct();
        String[] moodNames = moodsArpg.keySet().toArray(new String[0]);
        float[] moodLevels = new float[moodNames.length];

        for (int i = 0; i < moodNames.length; i++)
            moodLevels[i] = moodsArpg.get(moodNames[i]).getAsFloat();

        return new LessonStruct(
                ArpgUtility.validateString(lessonArpg, "heard"),
                ArpgUtility.getString(lessonArpg, "intent", EngineSetting.DIALOGUE_TEXT_NONE),
                DialogueArpgUtility.parseStrings(lessonArpg, "traits"),
                moodNames,
                moodLevels,
                ArpgUtility.getFloat(lessonArpg, "affinity", 0f),
                ArpgUtility.getFloat(lessonArpg, "morality", 0f),
                ArpgUtility.getString(lessonArpg, "relation", EngineSetting.DIALOGUE_TEXT_NONE),
                ArpgUtility.getString(lessonArpg, "profession", EngineSetting.DIALOGUE_TEXT_NONE),
                ArpgUtility.getString(lessonArpg, "reply", EngineSetting.DIALOGUE_TEXT_NONE),
                ArpgUtility.getString(lessonArpg, "said", EngineSetting.DIALOGUE_TEXT_NONE),
                parseKeys(lessonArpg),
                DialogueArpgUtility.parseStrings(lessonArpg, "words"),
                ArpgUtility.getString(lessonArpg, "verdict", EngineSetting.DIALOGUE_TEXT_NONE),
                ArpgUtility.getFloat(lessonArpg, "understanding", 0f),
                ArpgUtility.getFloat(lessonArpg, "response", 0f),
                ArpgUtility.getFloat(lessonArpg, "wording", 0f),
                ArpgUtility.getString(lessonArpg, "better", EngineSetting.DIALOGUE_TEXT_NONE),
                recorded);
    }

    private int[] parseKeys(ArpgObjectStruct lessonArpg) {

        if (!ArpgUtility.hasArray(lessonArpg, "lines"))
            return new int[0];

        ArpgArrayStruct keysArpg = lessonArpg.getAsArray("lines");
        int[] keys = new int[keysArpg.size()];

        for (int i = 0; i < keys.length; i++)
            keys[i] = keysArpg.get(i).getAsInt();

        return keys;
    }
}
