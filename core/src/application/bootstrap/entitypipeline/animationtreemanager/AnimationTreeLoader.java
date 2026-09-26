package application.bootstrap.entitypipeline.animationtreemanager;

import java.io.File;

import application.bootstrap.entitypipeline.animationtree.AnimationTreeHandle;
import engine.root.EngineSetting;
import engine.root.LoaderPackage;
import engine.util.io.FileUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

class AnimationTreeLoader extends LoaderPackage {

    /*
     * Scans the animation tree JSON directory and loads every tree into
     * AnimationTreeManager. Supports on-demand loading for trees not yet in
     * the palette — entity templates resolve their tree through that path
     * while they load.
     */

    // Internal
    private File root;
    private AnimationTreeManager animationTreeManager;
    private AnimationTreeBuilder internalBuilder;

    // File Registry
    private Object2ObjectOpenHashMap<String, File> treeName2File;

    // Base \\

    @Override
    protected void scan() {

        this.root = new File(EngineSetting.ANIMATION_TREE_JSON_PATH);
        this.treeName2File = new Object2ObjectOpenHashMap<>();

        FileUtility.verifyDirectory(root, "Animation tree JSON directory not found: " + root.getAbsolutePath());

        for (File file : FileUtility.collectFiles(root, EngineSetting.JSON_FILE_EXTENSIONS)) {
            String treeName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
            treeName2File.put(treeName, file);
            queueFile(file);
        }
    }

    @Override
    protected void create() {
        this.internalBuilder = create(AnimationTreeBuilder.class);
    }

    @Override
    protected void get() {
        this.animationTreeManager = get(AnimationTreeManager.class);
    }

    // Load \\

    @Override
    protected void load(File file) {

        String treeName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
        AnimationTreeHandle handle = internalBuilder.build(file, treeName);

        if (handle == null)
            throwException("Failed to build animation tree from: " + file.getAbsolutePath());

        animationTreeManager.addAnimationTree(handle);
    }

    // On-Demand \\

    void request(String treeName) {

        File file = treeName2File.get(treeName);

        if (file == null)
            throwException("On-demand animation tree load failed — not found in scan registry: \"" + treeName + "\"");

        request(file);
    }
}
