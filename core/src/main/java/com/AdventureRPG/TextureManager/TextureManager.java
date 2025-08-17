package com.AdventureRPG.TextureManager;

import java.io.File;
import java.util.*;
import java.util.function.Function;

import com.AdventureRPG.GameManager;
import com.AdventureRPG.SettingsSystem.Settings;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.PixmapPacker;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;

public class TextureManager {

    // Debug
    private final boolean debug = true; // TODO: Remove debug line

    // Game Manager
    private final Settings settings;

    // Settings
    private final String BLOCK_TEXTURE_PATH;
    private final int BLOCK_TEXTURE_SIZE;
    private final int BLOCK_ATLAS_PADDING;

    private final Color NORMAL_MAP_DEFAULT;
    private final Color HEIGHT_MAP_DEFAULT;
    private final Color METAL_MAP_DEFAULT;
    private final Color ROUGHNESS_MAP_DEFAULT;
    private final Color AO_MAP_DEFAULT;
    private final Color OPACITY_MAP_DEFAULT;
    private final Color CUSTOM_MAP_DEFAULT;

    // ID bookkeeping
    private final Map<Integer, String> idToTexturePath = new HashMap<>();
    private final Map<String, Integer> texturePathToID = new HashMap<>();
    private int nextTextureID = 0;

    // Main: folderName to AtlasGroup
    private final Map<String, AtlasGroup> atlasGroups = new HashMap<>();

    // Flat lookup: "folder_suffix" to TextureAtlas
    private final Map<String, TextureAtlas> atlasNameMap = new HashMap<>();

    // Base \\

    public TextureManager(GameManager gameManager) {

        // Game Manager
        this.settings = gameManager.settings;

        // Settings
        BLOCK_TEXTURE_PATH = settings.BLOCK_TEXTURE_PATH;
        BLOCK_TEXTURE_SIZE = settings.BLOCK_TEXTURE_SIZE;
        BLOCK_ATLAS_PADDING = settings.BLOCK_ATLAS_PADDING;

        this.NORMAL_MAP_DEFAULT = settings.NORMAL_MAP_DEFAULT;
        this.HEIGHT_MAP_DEFAULT = settings.HEIGHT_MAP_DEFAULT;
        this.METAL_MAP_DEFAULT = settings.METAL_MAP_DEFAULT;
        this.ROUGHNESS_MAP_DEFAULT = settings.ROUGHNESS_MAP_DEFAULT;
        this.AO_MAP_DEFAULT = settings.AO_MAP_DEFAULT;
        this.OPACITY_MAP_DEFAULT = settings.OPACITY_MAP_DEFAULT;
        this.CUSTOM_MAP_DEFAULT = settings.CUSTOM_MAP_DEFAULT;

        // Core
        File texturePath = new File(BLOCK_TEXTURE_PATH);
        compileTextures(texturePath);
    }

    // Core \\

    private void compileTextures(File texturePath) {

        if (!texturePath.exists() || !texturePath.isDirectory())
            throw new RuntimeException("Base folder not found: " + texturePath.getAbsolutePath());

        File[] subfolders = texturePath.listFiles(File::isDirectory);

        if (subfolders == null || subfolders.length == 0) {

            if (debug) // TODO: Remove debug line
                System.out.println("No subfolders found in " + texturePath.getAbsolutePath());

            return;
        }

        for (File atlasFolder : subfolders)
            organizeAtlasLibrary(atlasFolder);
    }

    private void organizeAtlasLibrary(File atlasFolder) {

        File[] pngFiles = atlasFolder.listFiles(f -> f.isFile() && f.getName().toLowerCase().endsWith(".png"));

        if (pngFiles == null || pngFiles.length == 0)
            return;

        List<File> albedoMaps = new ArrayList<>();
        List<File> normalMaps = new ArrayList<>();
        List<File> metalMaps = new ArrayList<>();
        List<File> heightMaps = new ArrayList<>();
        List<File> roughnessMaps = new ArrayList<>();
        List<File> aoMaps = new ArrayList<>();
        List<File> opacityMaps = new ArrayList<>();
        Map<String, List<File>> customMaps = new HashMap<>();

        for (File file : pngFiles) {

            String name = file.getName().toLowerCase().replace(".png", "");
            int underscoreIdx = name.lastIndexOf("_");
            String suffix = underscoreIdx > 0 ? name.substring(underscoreIdx + 1) : "";

            switch (suffix) {
                case "n":
                case "normal":
                    normalMaps.add(file);
                    break;
                case "m":
                case "metal":
                case "metallic":
                    metalMaps.add(file);
                    break;
                case "h":
                case "height":
                    heightMaps.add(file);
                    break;
                case "r":
                case "rough":
                case "roughness":
                    roughnessMaps.add(file);
                    break;
                case "ao":
                case "ambientocclusion":
                    aoMaps.add(file);
                    break;
                case "o":
                case "opacity":
                case "alpha":
                    opacityMaps.add(file);
                    break;
                case "":
                    albedoMaps.add(file);
                    break;
                default:
                    customMaps.computeIfAbsent(suffix, k -> new ArrayList<>()).add(file);
                    break;
            }
        }

        // Assign IDs for albedos
        assignUniqueIDs(albedoMaps, atlasFolder);

        // Build grouped sets
        Map<Integer, TextureSet> groupedSets = assembleAtlasMap(albedoMaps, atlasFolder);

        // Create group for this folder
        AtlasGroup group = new AtlasGroup();
        String folder = atlasFolder.getName();

        group.albedo = packType(groupedSets, m -> m.albedo, Color.CLEAR);
        registerAtlas(folder + "_albedo", group.albedo);

        group.normal = packType(groupedSets, m -> m.normal, NORMAL_MAP_DEFAULT);
        registerAtlas(folder + "_normal", group.normal);

        group.height = packType(groupedSets, m -> m.height, HEIGHT_MAP_DEFAULT);
        registerAtlas(folder + "_height", group.height);

        group.metal = packType(groupedSets, m -> m.metal, METAL_MAP_DEFAULT);
        registerAtlas(folder + "_metal", group.metal);

        group.roughness = packType(groupedSets, m -> m.roughness, ROUGHNESS_MAP_DEFAULT);
        registerAtlas(folder + "_roughness", group.roughness);

        group.ao = packType(groupedSets, m -> m.ao, AO_MAP_DEFAULT);
        registerAtlas(folder + "_ao", group.ao);

        group.opacity = packType(groupedSets, m -> m.opacity, OPACITY_MAP_DEFAULT);
        registerAtlas(folder + "_opacity", group.opacity);

        atlasGroups.put(folder, group);

        // Pack custom maps
        for (Map.Entry<String, List<File>> entry : customMaps.entrySet()) {
            String suffix = entry.getKey(); // e.g., "ao", "rough", "emissive"
            List<File> files = entry.getValue();

            Map<Integer, TextureSet> customGroupedSets = new HashMap<>();

            for (File file : files) {
                String textureName = file.getName().replace(".png", "");
                String key = folder + "/" + textureName;

                int id = texturePathToID.getOrDefault(key, nextTextureID++);
                texturePathToID.putIfAbsent(key, id);
                idToTexturePath.putIfAbsent(id, key);

                TextureSet set = new TextureSet(id, key);
                // For now, other maps can remain null; you only care about the custom map
                // itself
                customGroupedSets.put(id, set);
            }

            // Pack the atlas for this suffix
            TextureAtlas customAtlas = packType(customGroupedSets, s -> folder + "/" + s.albedo + "_" + suffix,
                    CUSTOM_MAP_DEFAULT);
            group.custom.put(suffix, customAtlas);
            registerAtlas(folder + "_" + suffix, customAtlas);

            if (debug)
                System.out.println("Packed custom atlas: " + folder + "_" + suffix);
        }

        if (debug) // TODO: Remove debug line
            System.out.println("Built atlas group for: " + atlasFolder.getName());
    }

    private void assignUniqueIDs(List<File> files, File baseFolder) {
        String folderName = baseFolder.getName();

        for (File file : files) {
            String textureName = file.getName().replace(".png", "");
            String key = folderName + "/" + textureName;

            if (!texturePathToID.containsKey(key)) {
                int id = nextTextureID++;
                texturePathToID.put(key, id);
                idToTexturePath.put(id, key);

                if (debug) // TODO: Remove debug line
                    System.out.println("Assigned ID " + id + " → " + key);
            }
        }
    }

    private Map<Integer, TextureSet> assembleAtlasMap(List<File> albedoMaps, File baseFolder) {

        Map<Integer, TextureSet> groupedSets = new HashMap<>();
        String folderName = baseFolder.getName();

        for (File file : albedoMaps) {

            String textureName = file.getName().replace(".png", "");
            String key = folderName + "/" + textureName;
            int id = texturePathToID.get(key);

            TextureSet set = new TextureSet(id, key);
            set.normal = resolveMap(folderName, textureName, "n", NORMAL_MAP_DEFAULT);
            set.height = resolveMap(folderName, textureName, "h", HEIGHT_MAP_DEFAULT);
            set.metal = resolveMap(folderName, textureName, "m", METAL_MAP_DEFAULT);
            set.roughness = resolveMap(folderName, textureName, "r", ROUGHNESS_MAP_DEFAULT);
            set.ao = resolveMap(folderName, textureName, "ao", AO_MAP_DEFAULT);
            set.opacity = resolveMap(folderName, textureName, "o", OPACITY_MAP_DEFAULT);

            groupedSets.put(id, set);
        }

        return groupedSets;
    }

    private String resolveMap(String folder, String textureName, String suffix, Color defaultColor) {

        String candidate = folder + "/" + textureName + "_" + suffix + ".png";
        File f = new File(BLOCK_TEXTURE_PATH, candidate);

        return f.exists() ? candidate : "__DEFAULT__:" + suffix;
    }

    private TextureAtlas packType(
            Map<Integer, TextureSet> groupedSets,
            Function<TextureSet, String> getter,
            Color defaultColor) {

        PixmapPacker packer = new PixmapPacker(
                BLOCK_TEXTURE_SIZE,
                BLOCK_TEXTURE_SIZE,
                Pixmap.Format.RGBA8888,
                BLOCK_ATLAS_PADDING,
                false);

        for (TextureSet set : groupedSets.values()) {

            String path = getter.apply(set);
            Pixmap pixmap;
            String regionName;

            if (path.startsWith("__DEFAULT__")) {
                pixmap = createDefaultPixmap(defaultColor);
                // fallback region name = id_X
                regionName = "id_" + set.id;
            }

            else {
                FileHandle handle = Gdx.files.internal(BLOCK_TEXTURE_PATH + "/" + path);
                pixmap = new Pixmap(handle);
                // region name = file basename
                regionName = new File(path).getName().replace(".png", "");
            }

            packer.pack(regionName, pixmap);
            pixmap.dispose(); // prevent leaks
        }

        return packer.generateTextureAtlas(
                Texture.TextureFilter.Nearest,
                Texture.TextureFilter.Nearest,
                false);
    }

    private Pixmap createDefaultPixmap(Color color) {

        Pixmap pixmap = new Pixmap(BLOCK_TEXTURE_SIZE, BLOCK_TEXTURE_SIZE, Pixmap.Format.RGBA8888);
        pixmap.setColor(color);
        pixmap.fill();

        return pixmap;
    }

    private void registerAtlas(String name, TextureAtlas atlas) {

        atlasNameMap.put(name, atlas);

        if (debug) // TODO: Remove debug line
            System.out.println("Registered atlas: " + name);
    }

    private static class TextureSet {

        int id;
        String albedo;
        String normal;
        String height;
        String roughness;
        String ao;
        String opacity;
        String metal;

        TextureSet(int id, String albedo) {

            this.id = id;
            this.albedo = albedo;
        }
    }

    private static class AtlasGroup {

        TextureAtlas albedo;
        TextureAtlas normal;
        TextureAtlas height;
        TextureAtlas metal;
        TextureAtlas roughness;
        TextureAtlas ao;
        TextureAtlas opacity;

        Map<String, TextureAtlas> custom = new HashMap<>();

        void dispose() {

            if (albedo != null)
                albedo.dispose();
            if (normal != null)
                normal.dispose();
            if (height != null)
                height.dispose();
            if (metal != null)
                metal.dispose();
            if (roughness != null)
                roughness.dispose();
            if (ao != null)
                ao.dispose();
            if (opacity != null)
                opacity.dispose();

            for (TextureAtlas atlas : custom.values())
                atlas.dispose();

            custom.clear();
        }
    }

    // Utility \\

    public TextureAtlas getAtlas(String folder, String type) {
        return atlasGroups.containsKey(folder) ? atlasNameMap.get(folder + "_" + type) : null;
    }

    public TextureAtlas getAtlasByName(String atlasName) {
        return atlasNameMap.get(atlasName);
    }

    public int getIDFromTexture(String texturePath) {
        return texturePathToID.getOrDefault(texturePath, -1);
    }

    public String getTextureFromID(int id) {
        return idToTexturePath.getOrDefault(id, null);
    }

    public void dispose() {

        for (AtlasGroup group : atlasGroups.values())
            group.dispose();
        atlasGroups.clear();

        for (TextureAtlas atlas : atlasNameMap.values())
            atlas.dispose();

        atlasNameMap.clear();
    }
}
