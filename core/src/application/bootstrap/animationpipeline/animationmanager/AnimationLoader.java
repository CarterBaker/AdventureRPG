package application.bootstrap.animationpipeline.animationmanager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import application.bootstrap.animationpipeline.animation.AnimationClipHandle;
import engine.root.EngineSetting;
import engine.root.LoaderPackage;
import engine.util.io.FileUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

class AnimationLoader extends LoaderPackage {

    /*
     * Scans the animation JSON directory and loads every clip into
     * AnimationManager. Supports on-demand loading for clips not yet in
     * the palette at runtime.
     */

    // Internal
    private File root;
    private AnimationManager animationManager;
    private AnimationBuilder internalBuilder;

    // File Registry
    private Object2ObjectOpenHashMap<String, File> clipName2File;

    // Base \\

    @Override
    protected void scan() {

        this.root = new File(EngineSetting.ANIMATION_JSON_PATH);
        this.clipName2File = new Object2ObjectOpenHashMap<>();

        FileUtility.verifyDirectory(root, "Animation JSON directory not found: " + root.getAbsolutePath());

        try (var stream = Files.walk(root.toPath())) {
            stream
                    .filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .filter(f -> EngineSetting.JSON_FILE_EXTENSIONS.contains(FileUtility.getExtension(f)))
                    .forEach(file -> {
                        String clipName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
                        clipName2File.put(clipName, file);
                        fileQueue.offer(file);
                    });
        } catch (IOException e) {
            throwException("Failed to walk animation directory: " + root.getAbsolutePath(), e);
        }
    }

    @Override
    protected void create() {
        this.internalBuilder = create(AnimationBuilder.class);
    }

    @Override
    protected void get() {
        this.animationManager = get(AnimationManager.class);
    }

    // Load \\

    @Override
    protected void load(File file) {

        String clipName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
        AnimationClipHandle handle = internalBuilder.build(file, clipName);

        if (handle == null)
            throwException("Failed to build animation clip from: " + file.getAbsolutePath());

        animationManager.addClip(clipName, handle);
    }

    // On-Demand \\

    void request(String clipName) {

        File file = clipName2File.get(clipName);

        if (file == null)
            throwException("On-demand animation load failed — not found in scan registry: \"" + clipName + "\"");

        request(file);
    }
}