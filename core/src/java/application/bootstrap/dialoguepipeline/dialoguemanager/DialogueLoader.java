package application.bootstrap.dialoguepipeline.dialoguemanager;

import java.io.File;

import application.bootstrap.dialoguepipeline.lesson.LessonStruct;
import engine.root.EngineSetting;
import engine.root.LoaderPackage;
import engine.util.io.FileUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class DialogueLoader extends LoaderPackage {

    /*
     * Scans the lesson ARPG directory and loads every lesson into
     * DialogueManager, in the order its file keeps them, since the models are
     * taught them in that order. The file named by DIALOGUE_LESSON_FILE is the
     * one the trainer writes: its lessons are marked as recorded, so they are
     * the ones saved back to it. Every lesson is in before the models are
     * trained, through requestAll().
     */

    // Internal
    private File root;
    private DialogueManager dialogueManager;
    private DialogueBuilder internalBuilder;

    // Base \\

    @Override
    protected void scan() {

        this.root = new File(EngineSetting.LESSON_PATH);

        FileUtility.verifyDirectory(root, "Lesson directory not found: " + root.getAbsolutePath());

        for (File file : FileUtility.collectFiles(root, EngineSetting.ARPG_FILE_EXTENSIONS))
            queueFile(file);
    }

    @Override
    protected void create() {
        this.internalBuilder = create(DialogueBuilder.class);
    }

    @Override
    protected void get() {
        this.dialogueManager = get(DialogueManager.class);
    }

    // Load \\

    @Override
    protected void load(File file) {

        boolean recorded = FileUtility.getPathWithFileNameWithoutExtension(root, file)
                .equals(EngineSetting.DIALOGUE_LESSON_FILE);
        ObjectArrayList<LessonStruct> lessons = internalBuilder.build(file, recorded);

        for (int i = 0; i < lessons.size(); i++)
            dialogueManager.addLesson(lessons.get(i));
    }
}
