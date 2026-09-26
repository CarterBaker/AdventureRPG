package application.bootstrap.worldpipeline.blockmanager;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.File;

import application.bootstrap.worldpipeline.block.BlockHandle;
import engine.root.EngineSetting;
import engine.root.LoaderPackage;
import engine.util.io.FileUtility;
import engine.util.io.JsonUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class BlockLoader extends LoaderPackage {

    /*
     * Scans the block JSON directory, pre-registers every block name so blocks
     * can reference each other, and loads each definition into BlockManager, on
     * demand when requested early.
     */

    // Internal
    private File root;
    private BlockManager blockManager;
    private BlockBuilder internalBuilder;

    // File Registry
    private Object2ObjectOpenHashMap<String, File> resourceName2File;
    private Object2ObjectOpenHashMap<String, String> blockName2ResourceName;

    // Base \\

    @Override
    protected void create() {
        this.internalBuilder = create(BlockBuilder.class);
    }

    @Override
    protected void get() {
        this.blockManager = get(BlockManager.class);
    }

    @Override
    protected void scan() {

        this.root = new File(EngineSetting.BLOCK_JSON_PATH);
        this.resourceName2File = new Object2ObjectOpenHashMap<>();
        this.blockName2ResourceName = new Object2ObjectOpenHashMap<>();

        FileUtility.verifyDirectory(root, "Block root directory not found: " + root.getAbsolutePath());

        for (File file : FileUtility.collectFiles(root, EngineSetting.JSON_FILE_EXTENSIONS)) {
            String resourceName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
            resourceName2File.put(resourceName, file);
            preRegisterBlockNames(file, resourceName);
            queueFile(file);
        }
    }

    // Pre-Registration \\

    private void preRegisterBlockNames(File file, String resourceName) {

        try {
            JsonObject json = JsonUtility.loadJsonObject(file);
            JsonArray blockArray = json.getAsJsonArray("blocks");

            if (blockArray == null)
                return;

            for (int i = 0; i < blockArray.size(); i++) {
                JsonObject blockJson = blockArray.get(i).getAsJsonObject();
                if (!blockJson.has("name"))
                    continue;
                String localName = blockJson.get("name").getAsString();
                String blockName = resourceName + "/" + localName;
                blockName2ResourceName.put(blockName, resourceName);
            }
        } catch (Exception e) {
            throwException("Failed to pre-register block names from: " + file.getPath(), e);
        }
    }

    // Load \\

    @Override
    protected void load(File file) {

        ObjectArrayList<BlockHandle> blocks = internalBuilder.build(file, root);

        for (int i = 0; i < blocks.size(); i++)
            blockManager.addBlock(blocks.get(i));
    }

    // On-Demand \\

    void request(String blockName) {

        String resourceName = blockName2ResourceName.get(blockName);

        if (resourceName == null)
            throwException("On-demand block load failed — no file found for: \"" + blockName + "\"");

        request(resourceName2File.get(resourceName));
    }
}