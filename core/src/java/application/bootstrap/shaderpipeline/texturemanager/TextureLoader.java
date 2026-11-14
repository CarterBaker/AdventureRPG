package application.bootstrap.shaderpipeline.texturemanager;

import java.io.File;

import engine.root.EngineSetting;
import engine.root.LoaderPackage;
import engine.util.io.FileUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class TextureLoader extends LoaderPackage {

    /*
     * Discovers the baked texture packs under bakedtextures/tiles and loads
     * one pack per load() call: every tile definition in name order, each
     * registered with its fallback and extra maps staged into the pools,
     * then uploaded together. The texture manager requests every pack at
     * bootstrap, so the game never reads a source image or packs an atlas;
     * the tiers themselves stream later. The alias library loads first, for
     * the extra set's defaults and for the editor that paints tiles.
     */

    // Internal
    private File root;
    private TextureManager textureManager;
    private TextureBuilder internalBuilder;

    // File Registry
    private Object2ObjectOpenHashMap<String, File> arrayName2File;

    // Base \\

    @Override
    protected void scan() {

        this.root = new File(new File(EngineSetting.BAKED_TEXTURE_PATH), EngineSetting.BAKED_TEXTURE_TILE_DIRECTORY);
        this.arrayName2File = new Object2ObjectOpenHashMap<>();

        FileUtility.verifyDirectory(root, "Baked texture directory not found: " + root.getAbsolutePath()
                + ". Run gradlew lwjgl3:bake.");

        ObjectArrayList<File> directories = FileUtility.collectAllSubdirectories(root);
        directories.sort(null);

        for (int i = 0; i < directories.size(); i++) {
            File directory = directories.get(i);
            arrayName2File.put(FileUtility.getPathWithFileNameWithoutExtension(root, directory), directory);
            queueFile(directory);
        }
    }

    @Override
    protected void create() {
        this.internalBuilder = create(TextureBuilder.class);
    }

    @Override
    protected void get() {
        this.textureManager = get(TextureManager.class);
    }

    @Override
    protected void awake() {
        textureManager.setAliasLibrary(
                TextureBakeUtility.loadAliasLibrary(new File(EngineSetting.BLOCK_TEXTURE_ALIAS_PATH)));
    }

    // Load \\

    @Override
    protected void load(File directory) {

        ObjectArrayList<File> definitionFiles = FileUtility.collectFilesShallow(
                directory, EngineSetting.ARPG_FILE_EXTENSIONS);

        if (definitionFiles.isEmpty())
            return;

        String arrayName = FileUtility.getPathWithFileNameWithoutExtension(root, directory);
        definitionFiles.sort(null);

        for (int i = 0; i < definitionFiles.size(); i++)
            internalBuilder.build(definitionFiles.get(i), arrayName);

        textureManager.flushUploads();
    }

    // On-Demand \\

    void request(String arrayName) {

        File directory = arrayName2File.get(arrayName);

        if (directory == null)
            throwException("On-demand texture load failed — array not found: \"" + arrayName + "\"");

        request(directory);
    }
}
