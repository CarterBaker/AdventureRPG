package application.bootstrap.soundpipeline.soundmanager;

import java.io.File;

import engine.root.EngineSetting;
import engine.root.LoaderPackage;
import engine.util.io.FileUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

class SoundLoader extends LoaderPackage {

    /*
     * Scans the sound directory for every WAV and Ogg Vorbis file and loads
     * each into SoundManager; a companion ARPG file beside a sound is read by
     * the builder, never queued itself. Supports on-demand loading for sounds
     * not yet in the palette while the loader lives.
     */

    // Internal
    private File root;
    private SoundManager soundManager;
    private SoundBuilder internalBuilder;

    // File Registry
    private Object2ObjectOpenHashMap<String, File> resourceName2File;

    // Base \\

    @Override
    protected void create() {
        this.internalBuilder = create(SoundBuilder.class);
    }

    @Override
    protected void get() {
        this.soundManager = get(SoundManager.class);
    }

    @Override
    protected void scan() {

        this.root = new File(EngineSetting.SOUND_PATH);
        this.resourceName2File = new Object2ObjectOpenHashMap<>();

        FileUtility.verifyDirectory(root, "Sound root directory not found: " + root.getAbsolutePath());

        for (File file : FileUtility.collectFiles(root, EngineSetting.SOUND_FILE_EXTENSIONS)) {

            String resourceName = FileUtility.getPathWithFileNameWithoutExtension(root, file);

            if (resourceName2File.containsKey(resourceName))
                throwException("Duplicate sound name: '" + resourceName + "' is both '"
                        + resourceName2File.get(resourceName).getName() + "' and '" + file.getName()
                        + "'. Keep one file per sound name.");

            resourceName2File.put(resourceName, file);
            queueFile(file);
        }
    }

    // Load \\

    @Override
    protected void load(File file) {
        soundManager.addSound(internalBuilder.build(file, root));
    }

    // On-Demand \\

    boolean hasFile(String soundName) {
        return resourceName2File.containsKey(soundName);
    }

    void request(String soundName) {

        File file = resourceName2File.get(soundName);

        if (file == null)
            throwException("On-demand sound load failed — no file found for: \"" + soundName + "\"");

        request(file);
    }
}
