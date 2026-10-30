package application.bootstrap.soundpipeline.soundcuemanager;

import java.io.File;

import engine.root.EngineSetting;
import engine.root.LoaderPackage;
import engine.util.io.FileUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

class SoundCueLoader extends LoaderPackage {

    /*
     * Scans the sound cue directory and loads every cue into SoundCueManager.
     * Supports on-demand loading for cues not yet in the palette while the
     * loader lives.
     */

    // Internal
    private File root;
    private SoundCueManager soundCueManager;
    private SoundCueBuilder internalBuilder;

    // File Registry
    private Object2ObjectOpenHashMap<String, File> resourceName2File;

    // Base \\

    @Override
    protected void create() {
        this.internalBuilder = create(SoundCueBuilder.class);
    }

    @Override
    protected void get() {
        this.soundCueManager = get(SoundCueManager.class);
    }

    @Override
    protected void scan() {

        this.root = new File(EngineSetting.SOUND_CUE_PATH);
        this.resourceName2File = new Object2ObjectOpenHashMap<>();

        FileUtility.verifyDirectory(root, "Sound cue root directory not found: " + root.getAbsolutePath());

        for (File file : FileUtility.collectFiles(root, EngineSetting.ARPG_FILE_EXTENSIONS)) {
            String resourceName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
            resourceName2File.put(resourceName, file);
            queueFile(file);
        }
    }

    // Load \\

    @Override
    protected void load(File file) {
        soundCueManager.addCue(internalBuilder.build(file, root));
    }

    // On-Demand \\

    void request(String cueName) {

        File file = resourceName2File.get(cueName);

        if (file == null)
            throwException("On-demand sound cue load failed — no file found for: \"" + cueName + "\"");

        request(file);
    }
}
