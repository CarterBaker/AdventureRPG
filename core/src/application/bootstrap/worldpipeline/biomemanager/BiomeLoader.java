package application.bootstrap.worldpipeline.biomemanager;

import java.io.File;

import com.google.gson.JsonObject;

import application.bootstrap.worldpipeline.biome.BiomeHandle;
import engine.root.EngineSetting;
import engine.root.LoaderPackage;
import engine.util.io.FileUtility;
import engine.util.io.JsonUtility;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class BiomeLoader extends LoaderPackage {

    /*
     * Scans and loads every biome definition into BiomeManager. Map colors are
     * read during scan() and handed to BiomeManager once get() wires it in.
     * On-demand requests register a biome directly without touching the file
     * queue, since they can arrive from world generation workers.
     */

    // Internal
    private File root;
    private BiomeManager biomeManager;
    private BiomeBuilder internalBuilder;

    // File Registry
    private Object2ObjectOpenHashMap<String, File> resourceName2File;

    // Map Color Scan Buffer — filled in scan(), handed off and cleared in get()
    private IntArrayList scannedMapColors;
    private ObjectArrayList<String> scannedMapColorNames;

    // Base \\

    @Override
    protected void create() {
        this.internalBuilder = create(BiomeBuilder.class);
    }

    @Override
    protected void get() {
        this.biomeManager = get(BiomeManager.class);
        handOffScannedMapColors();
    }

    @Override
    protected void scan() {

        this.root = new File(EngineSetting.BIOME_JSON_PATH);
        this.resourceName2File = new Object2ObjectOpenHashMap<>();
        this.scannedMapColors = new IntArrayList();
        this.scannedMapColorNames = new ObjectArrayList<>();

        FileUtility.verifyDirectory(root, "Biome root directory not found: " + root.getAbsolutePath());

        for (File file : FileUtility.collectFiles(root, EngineSetting.JSON_FILE_EXTENSIONS)) {
            String resourceName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
            resourceName2File.put(resourceName, file);
            scanMapColor(file, resourceName);
            queueFile(file);
        }
    }

    // Map Color Scan \\

    private void scanMapColor(File file, String resourceName) {

        try {
            JsonObject json = JsonUtility.loadJsonObject(file);

            if (!json.has("map_color"))
                return;

            scannedMapColors.add(parseMapColorHex(json.get("map_color").getAsString(), resourceName));
            scannedMapColorNames.add(resourceName);
        } catch (Exception e) {
            throwException("Failed to pre-register map color from: " + file.getPath(), e);
        }
    }

    private int parseMapColorHex(String raw, String resourceName) {

        String hex = raw.startsWith("#") ? raw.substring(1) : raw;

        if (hex.length() != 6)
            throwException("Biome \"" + resourceName + "\" has invalid map_color \"" + raw
                    + "\" — expected a 6-digit hex RGB value, e.g. \"#5B8C3A\".");

        try {
            return Integer.parseInt(hex, 16);
        } catch (NumberFormatException e) {
            throwException(
                    "Biome \"" + resourceName + "\" has invalid map_color \"" + raw + "\" — not valid hex.", e);
            return 0;
        }
    }

    private void handOffScannedMapColors() {

        for (int i = 0; i < scannedMapColors.size(); i++)
            biomeManager.registerMapColor(scannedMapColorNames.get(i), scannedMapColors.getInt(i));

        scannedMapColors = null;
        scannedMapColorNames = null;
    }

    // Load \\

    @Override
    protected void load(File file) {

        BiomeHandle biomeHandle = internalBuilder.build(file, root);

        if (biomeHandle != null)
            biomeManager.addBiome(biomeHandle);
    }

    // On-Demand \\

    void request(String biomeName) {

        if (biomeManager.hasBiome(biomeName))
            return;

        File file = resourceName2File.get(biomeName);

        if (file == null)
            throwException("On-demand biome load failed — no file found for: \"" + biomeName + "\"");

        BiomeHandle biomeHandle = internalBuilder.build(file, root);

        if (biomeHandle != null)
            biomeManager.addBiome(biomeHandle);
    }
}