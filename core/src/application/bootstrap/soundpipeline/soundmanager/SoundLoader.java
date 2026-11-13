package application.bootstrap.soundpipeline.soundmanager;

import java.io.File;

import application.bootstrap.soundpipeline.sound.SoundBuildStruct;
import application.kernel.threadpipeline.thread.ThreadHandle;
import engine.root.EngineSetting;
import engine.root.LoaderPackage;
import engine.util.arpg.ArpgUtility;
import engine.util.io.FileUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class SoundLoader extends LoaderPackage {

    /*
     * Scans the sound directory for every Ogg Vorbis file and loads each
     * into SoundManager; a companion ARPG file beside a sound is read by the
     * builder, never queued itself, and any other file is refused, so every
     * sound the game holds is in the one format. Instrument samples, under
     * SOUND_NOTE_PATH, are never queued: they load the first time their note
     * set plays. The loader persists for that, loading a sound at once when
     * asked by name, or reading it at once and decoding it on the general
     * thread when asked while the game runs, uploading it the frame the
     * decode lands.
     */

    // Internal
    private File root;
    private SoundManager soundManager;
    private SoundBuilder internalBuilder;
    private ThreadHandle threadHandle;

    // File Registry
    private Object2ObjectOpenHashMap<String, File> resourceName2File;

    // Pending — sounds decoding on a worker, uploaded once their samples land
    private ObjectArrayList<SoundBuildStruct> pendingBuilds;
    private Object2ObjectOpenHashMap<String, SoundBuildStruct> soundName2PendingBuild;

    // Base \\

    @Override
    protected void create() {

        // Pending
        this.pendingBuilds = new ObjectArrayList<>();
        this.soundName2PendingBuild = new Object2ObjectOpenHashMap<>();

        this.internalBuilder = create(SoundBuilder.class);
    }

    @Override
    protected void get() {
        this.soundManager = get(SoundManager.class);
        this.threadHandle = getThreadHandleFromThreadName(EngineSetting.GENERAL_THREAD_NAME);
    }

    @Override
    protected void scan() {

        this.root = new File(EngineSetting.SOUND_PATH);
        this.resourceName2File = new Object2ObjectOpenHashMap<>();

        FileUtility.verifyDirectory(root, "Sound root directory not found: " + root.getAbsolutePath());

        for (File file : FileUtility.collectFiles(root)) {

            if (ArpgUtility.isArpgFile(file))
                continue;

            if (!FileUtility.getExtension(file).equals(EngineSetting.SOUND_FILE_EXTENSION))
                throwException("Sound file '" + file.getAbsolutePath() + "' is not an Ogg Vorbis file. Every sound "
                        + "under '" + EngineSetting.SOUND_PATH + "' is a '." + EngineSetting.SOUND_FILE_EXTENSION
                        + "' file; convert it and remove the original.");

            String soundName = FileUtility.getPathWithFileNameWithoutExtension(root, file);

            resourceName2File.put(soundName, file);

            if (!isNoteSample(soundName))
                queueFile(file);
        }
    }

    // Kept for the engine lifetime, so instrument samples load whenever their note set first plays
    @Override
    protected boolean persists() {
        return true;
    }

    // Load \\

    @Override
    protected void load(File file) {
        soundManager.addSound(internalBuilder.build(file, root));
    }

    // Update \\

    // Main thread — every sound whose samples have landed uploaded and handed to the palette
    @Override
    protected void update() {

        for (int i = pendingBuilds.size() - 1; i >= 0; i--) {

            SoundBuildStruct build = pendingBuilds.get(i);

            if (build.isDecoded())
                finishPending(i);
        }
    }

    private void finishPending(int index) {

        SoundBuildStruct build = pendingBuilds.get(index);

        pendingBuilds.set(index, pendingBuilds.get(pendingBuilds.size() - 1));
        pendingBuilds.remove(pendingBuilds.size() - 1);
        soundName2PendingBuild.remove(build.getSoundName());
        soundManager.addSound(internalBuilder.finish(build));
    }

    // On-Demand \\

    boolean hasFile(String soundName) {
        return resourceName2File.containsKey(soundName);
    }

    // The sound loaded now; one already decoding is waited for rather than read twice
    void request(String soundName) {

        SoundBuildStruct pending = soundName2PendingBuild.get(soundName);

        if (pending != null) {
            pending.awaitDecoded();
            finishPending(pendingBuilds.indexOf(pending));
            return;
        }

        File file = resourceName2File.get(soundName);

        if (file == null)
            throwException("On-demand sound load failed — no file found for: \"" + soundName + "\"");

        request(file);
    }

    // The sound's ID at once, its companion read now and its samples decoded on the general thread
    int requestAsync(String soundName) {

        if (soundManager.hasSound(soundName))
            return soundManager.getSoundIDFromSoundName(soundName);

        SoundBuildStruct pending = soundName2PendingBuild.get(soundName);

        if (pending != null)
            return pending.getSoundID();

        File file = resourceName2File.get(soundName);

        if (file == null)
            return EngineSetting.INDEX_NOT_FOUND;

        SoundBuildStruct build = internalBuilder.read(file, root);

        pendingBuilds.add(build);
        soundName2PendingBuild.put(soundName, build);
        build.setDecodeFuture(executeAsync(threadHandle, () -> internalBuilder.decode(build)));

        return build.getSoundID();
    }

    // Utility \\

    // A sample below the note folder, loaded with its note set rather than at bootstrap
    private boolean isNoteSample(String soundName) {
        return soundName.startsWith(EngineSetting.SOUND_NOTE_PATH + EngineSetting.PATH_SEPARATOR);
    }
}
