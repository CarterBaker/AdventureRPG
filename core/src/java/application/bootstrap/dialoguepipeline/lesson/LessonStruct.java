package application.bootstrap.dialoguepipeline.lesson;

import engine.root.StructPackage;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;

public class LessonStruct extends StructPackage {

    /*
     * One judged turn of a talk, kept so the dialogue models can be taught it
     * again from nothing: what was heard and what it truly meant, the being
     * that heard it as it stood when it chose its reply, the reply it gave
     * and how it worded it, the verdict passed on the turn and what that
     * verdict is worth to each model, and the reply that should have been
     * given when the trainer named one. Everything is held by name or by a
     * line's stable key, never by ID, so a lesson outlives any change to the
     * content; a name the content no longer holds is simply not learned from.
     * A lesson the trainer recorded is saved to the trainer's file; one
     * loaded from any other file is only ever read.
     */

    // Heard
    private final String heard;
    private final String intentName;

    // Listener
    private final String[] traitNames;
    private final String[] moodNames;
    private final float[] moodLevels;
    private final float affinity;
    private final float morality;
    private final String relationName;
    private final String professionName;

    // Answer
    private final String replyName;
    private final String said;
    private final int[] lineKeys;
    private final String[] wordNames;

    // Verdict
    private final String verdictName;
    private final float understanding;
    private final float response;
    private final float wording;
    private final String betterReplyName;

    // Source
    private final boolean recorded;

    // Constructor \\

    public LessonStruct(
            String heard,
            String intentName,
            String[] traitNames,
            String[] moodNames,
            float[] moodLevels,
            float affinity,
            float morality,
            String relationName,
            String professionName,
            String replyName,
            String said,
            int[] lineKeys,
            String[] wordNames,
            String verdictName,
            float understanding,
            float response,
            float wording,
            String betterReplyName,
            boolean recorded) {

        // Heard
        this.heard = heard;
        this.intentName = intentName;

        // Listener
        this.traitNames = traitNames;
        this.moodNames = moodNames;
        this.moodLevels = moodLevels;
        this.affinity = affinity;
        this.morality = morality;
        this.relationName = relationName;
        this.professionName = professionName;

        // Answer
        this.replyName = replyName;
        this.said = said;
        this.lineKeys = lineKeys;
        this.wordNames = wordNames;

        // Verdict
        this.verdictName = verdictName;
        this.understanding = understanding;
        this.response = response;
        this.wording = wording;
        this.betterReplyName = betterReplyName;

        // Source
        this.recorded = recorded;
    }

    // Write \\

    // The lesson as it is kept in a lesson file, read back by DialogueBuilder
    public ArpgObjectStruct toArpg() {

        ArpgObjectStruct lessonArpg = new ArpgObjectStruct();
        ArpgObjectStruct moodsArpg = new ArpgObjectStruct();

        for (int i = 0; i < moodNames.length; i++)
            moodsArpg.addProperty(moodNames[i], moodLevels[i]);

        lessonArpg.addProperty("heard", heard);
        lessonArpg.addProperty("intent", intentName);
        lessonArpg.add("traits", toArray(traitNames));
        lessonArpg.add("moods", moodsArpg);
        lessonArpg.addProperty("affinity", affinity);
        lessonArpg.addProperty("morality", morality);
        lessonArpg.addProperty("relation", relationName);
        lessonArpg.addProperty("profession", professionName);
        lessonArpg.addProperty("reply", replyName);
        lessonArpg.addProperty("said", said);
        lessonArpg.add("lines", toArray(lineKeys));
        lessonArpg.add("words", toArray(wordNames));
        lessonArpg.addProperty("verdict", verdictName);
        lessonArpg.addProperty("understanding", understanding);
        lessonArpg.addProperty("response", response);
        lessonArpg.addProperty("wording", wording);
        lessonArpg.addProperty("better", betterReplyName);

        return lessonArpg;
    }

    private ArpgArrayStruct toArray(String[] values) {

        ArpgArrayStruct arrayArpg = new ArpgArrayStruct();

        for (int i = 0; i < values.length; i++)
            arrayArpg.add(values[i]);

        return arrayArpg;
    }

    private ArpgArrayStruct toArray(int[] values) {

        ArpgArrayStruct arrayArpg = new ArpgArrayStruct();

        for (int i = 0; i < values.length; i++)
            arrayArpg.add((long) values[i]);

        return arrayArpg;
    }

    // Accessible \\

    public String getHeard() {
        return heard;
    }

    public String getIntentName() {
        return intentName;
    }

    public String[] getTraitNames() {
        return traitNames;
    }

    public String[] getMoodNames() {
        return moodNames;
    }

    public float[] getMoodLevels() {
        return moodLevels;
    }

    public float getAffinity() {
        return affinity;
    }

    public float getMorality() {
        return morality;
    }

    public String getRelationName() {
        return relationName;
    }

    public String getProfessionName() {
        return professionName;
    }

    public String getReplyName() {
        return replyName;
    }

    public String getSaid() {
        return said;
    }

    public int[] getLineKeys() {
        return lineKeys;
    }

    public String[] getWordNames() {
        return wordNames;
    }

    public String getVerdictName() {
        return verdictName;
    }

    public float getUnderstanding() {
        return understanding;
    }

    public float getResponse() {
        return response;
    }

    public float getWording() {
        return wording;
    }

    public String getBetterReplyName() {
        return betterReplyName;
    }

    public boolean isRecorded() {
        return recorded;
    }
}
