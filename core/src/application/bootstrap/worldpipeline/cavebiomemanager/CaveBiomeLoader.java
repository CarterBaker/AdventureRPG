package application.bootstrap.worldpipeline.cavebiomemanager;

import java.io.File;

import application.bootstrap.worldpipeline.cavebiome.CaveBiomeHandle;
import engine.root.EngineSetting;
import engine.root.LoaderPackage;
import engine.util.io.FileUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

class CaveBiomeLoader extends LoaderPackage {

    /*
     * Scans the cave biome ARPG directory and loads every cave biome into
     * CaveBiomeManager. Supports on-demand loading by name, so a biome naming
     * one while it builds finds it registered, and requestAll() so the
     * palette is complete before the first chunk generates. The path below
     * the directory, without its extension, is the cave biome's name.
     */

    // Internal
    private File root;
    private CaveBiomeManager caveBiomeManager;
    private CaveBiomeBuilder internalBuilder;

    // File Registry
    private Object2ObjectOpenHashMap<String, File> caveBiomeName2File;

    // Base \\

    @Override
    protected void create() {
        this.internalBuilder = create(CaveBiomeBuilder.class);
    }

    @Override
    protected void get() {
        this.caveBiomeManager = get(CaveBiomeManager.class);
    }

    @Override
    protected void scan() {

        this.root = new File(EngineSetting.CAVE_BIOME_PATH);
        this.caveBiomeName2File = new Object2ObjectOpenHashMap<>();

        FileUtility.verifyDirectory(root, "Cave biome root directory not found: " + root.getAbsolutePath());

        for (File file : FileUtility.collectFiles(root, EngineSetting.ARPG_FILE_EXTENSIONS)) {
            String caveBiomeName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
            caveBiomeName2File.put(caveBiomeName, file);
            queueFile(file);
        }
    }

    // Load \\

    @Override
    protected void load(File file) {

        String caveBiomeName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
        CaveBiomeHandle caveBiomeHandle = internalBuilder.build(file, caveBiomeName);

        if (caveBiomeHandle == null)
            throwException("Failed to build cave biome from: " + file.getAbsolutePath());

        caveBiomeManager.addCaveBiomeHandle(caveBiomeHandle);
    }

    // On-Demand \\

    void request(String caveBiomeName) {

        if (caveBiomeManager.hasCaveBiome(caveBiomeName))
            return;

        File file = caveBiomeName2File.get(caveBiomeName);

        if (file == null)
            throwException("On-demand cave biome load failed — no file found for: \"" + caveBiomeName + "\"");

        request(file);
    }
}
