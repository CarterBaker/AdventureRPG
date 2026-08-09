package application.bootstrap.worldpipeline.biomemanager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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
     * Scans the biome JSON directory and loads every biome definition into
     * BiomeManager. Each file's "map_color" is peeked during scan() — before
     * this loader even has a reference to BiomeManager, since scan() runs
     * during CREATE and get() hasn't fired yet — so it's buffered locally
     * and handed off to BiomeManager's own color index the moment get()
     * wires the two together. On-demand requests resolve and register a
     * biome directly, without touching the shared file queue, since that
     * queue can also be drained by the normal per-frame batch on the main
     * thread while an on-demand request arrives from a world-generation
     * worker thread — biomes are the one registry resolved from off the
     * main thread, so this loader is the one place that has to account
     * for it.
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

        try (var stream = Files.walk(root.toPath())) {
            stream
                    .filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .filter(f -> FileUtility.hasExtension(f, EngineSetting.JSON_FILE_EXTENSIONS))
                    .forEach(file -> {
                        String resourceName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
                        resourceName2File.put(resourceName, file);
                        scanMapColor(file, resourceName);
                        fileQueue.offer(file);
                    });
        } catch (IOException e) {
            throwException("Failed to walk biome directory: " + root.getAbsolutePath(), e);
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
            throwException("Biome \"" + resourceName + "\" has invalid map_color \"" + raw + "\" — not valid hex.", e);
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

    /*
     * Resolves and registers a single biome by name directly, bypassing the
     * base loader's file-queue request path. That path removes the file
     * from fileQueue, a plain LinkedList also drained every frame by the
     * main thread's batch loop — safe for every other on-demand registry in
     * this engine, since none of them are ever queried off the main thread,
     * but biomes are resolved from the WorldStreaming thread during chunk
     * generation. Leaving the file in the queue is harmless: addBiome() is
     * idempotent, so if the batch loop reaches the same file later it just
     * re-parses and overwrites the same entry.
     */
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