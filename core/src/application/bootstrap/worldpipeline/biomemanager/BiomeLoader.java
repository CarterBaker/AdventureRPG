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
     * BiomeManager. Also peeks each file's "map_color" during scan — before
     * any biome is fully built — so world generation can match a world-map
     * pixel to the nearest registered biome and trigger that biome's
     * on-demand load without waiting on load order.
     */

    // Internal
    private File root;
    private BiomeManager biomeManager;
    private BiomeBuilder internalBuilder;

    // File Registry
    private Object2ObjectOpenHashMap<String, File> resourceName2File;

    // Map Color Registry
    private IntArrayList registeredMapColors;
    private ObjectArrayList<String> registeredMapColorNames;

    // Base \\

    @Override
    protected void create() {
        this.internalBuilder = create(BiomeBuilder.class);
    }

    @Override
    protected void get() {
        this.biomeManager = get(BiomeManager.class);
    }

    @Override
    protected void scan() {

        this.root = new File(EngineSetting.BIOME_JSON_PATH);
        this.resourceName2File = new Object2ObjectOpenHashMap<>();
        this.registeredMapColors = new IntArrayList();
        this.registeredMapColorNames = new ObjectArrayList<>();

        FileUtility.verifyDirectory(root, "Biome root directory not found: " + root.getAbsolutePath());

        try (var stream = Files.walk(root.toPath())) {
            stream
                    .filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .filter(f -> FileUtility.hasExtension(f, EngineSetting.JSON_FILE_EXTENSIONS))
                    .forEach(file -> {
                        String resourceName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
                        resourceName2File.put(resourceName, file);
                        preRegisterMapColor(file, resourceName);
                        fileQueue.offer(file);
                    });
        } catch (IOException e) {
            throwException("Failed to walk biome directory: " + root.getAbsolutePath(), e);
        }
    }

    // Pre-Registration \\

    private void preRegisterMapColor(File file, String resourceName) {

        try {
            JsonObject json = JsonUtility.loadJsonObject(file);

            if (!json.has("map_color"))
                return;

            registeredMapColors.add(parseMapColorHex(json.get("map_color").getAsString(), resourceName));
            registeredMapColorNames.add(resourceName);

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

    // Load \\

    @Override
    protected void load(File file) {

        BiomeHandle biomeHandle = internalBuilder.build(file, root);

        if (biomeHandle != null)
            biomeManager.addBiome(biomeHandle);
    }

    // On-Demand \\

    void request(String biomeName) {

        File file = resourceName2File.get(biomeName);

        if (file == null)
            throwException("On-demand biome load failed — no file found for: \"" + biomeName + "\"");

        request(file);
    }

    // Map Color Lookup \\

    String getNearestBiomeNameForColor(int color) {

        if (registeredMapColors.isEmpty())
            throwException(
                    "No biomes define a \"map_color\" — world generation cannot resolve a biome from the world map.");

        int targetR = (color >> 16) & 0xFF;
        int targetG = (color >> 8) & 0xFF;
        int targetB = color & 0xFF;

        String nearestName = null;
        int nearestDistanceSq = Integer.MAX_VALUE;

        for (int i = 0; i < registeredMapColors.size(); i++) {

            int candidate = registeredMapColors.getInt(i);
            int dr = ((candidate >> 16) & 0xFF) - targetR;
            int dg = ((candidate >> 8) & 0xFF) - targetG;
            int db = (candidate & 0xFF) - targetB;
            int distanceSq = dr * dr + dg * dg + db * db;

            if (distanceSq < nearestDistanceSq) {
                nearestDistanceSq = distanceSq;
                nearestName = registeredMapColorNames.get(i);
            }
        }

        return nearestName;
    }
}