package application.bootstrap.worldpipeline.treemanager;

import java.io.File;

import application.bootstrap.worldpipeline.tree.TreeHandle;
import engine.root.EngineSetting;
import engine.root.LoaderPackage;
import engine.util.io.FileUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

class TreeLoader extends LoaderPackage {

    /*
     * Scans the tree ARPG directory and loads every tree species into
     * TreeManager. Supports on-demand loading by tree name, and requestAll()
     * so TreeManager can complete its palette before the first chunk
     * generates. The path below the directory, without its extension, is the
     * species' name.
     */

    // Internal
    private File root;
    private TreeManager treeManager;
    private TreeBuilder internalBuilder;

    // File Registry
    private Object2ObjectOpenHashMap<String, File> treeName2File;

    // Base \\

    @Override
    protected void create() {
        this.internalBuilder = create(TreeBuilder.class);
    }

    @Override
    protected void get() {
        this.treeManager = get(TreeManager.class);
    }

    @Override
    protected void scan() {

        this.root = new File(EngineSetting.TREE_PATH);
        this.treeName2File = new Object2ObjectOpenHashMap<>();

        FileUtility.verifyDirectory(root, "Tree root directory not found: " + root.getAbsolutePath());

        for (File file : FileUtility.collectFiles(root, EngineSetting.ARPG_FILE_EXTENSIONS)) {
            String treeName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
            treeName2File.put(treeName, file);
            queueFile(file);
        }
    }

    // Load \\

    @Override
    protected void load(File file) {

        String treeName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
        TreeHandle treeHandle = internalBuilder.build(file, treeName);

        if (treeHandle == null)
            throwException("Failed to build tree from: " + file.getAbsolutePath());

        treeManager.addTreeHandle(treeHandle);
    }

    // On-Demand \\

    void request(String treeName) {

        if (treeManager.hasTree(treeName))
            return;

        File file = treeName2File.get(treeName);

        if (file == null)
            throwException("On-demand tree load failed — no file found for: \"" + treeName + "\"");

        request(file);
    }
}
