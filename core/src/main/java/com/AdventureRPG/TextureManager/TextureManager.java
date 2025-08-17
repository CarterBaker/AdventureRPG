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

    private int[] idToAtlasIndex;
    private Map<TextureAtlas, Integer> atlasToIndex = new HashMap<>();
    private List<TextureAtlas> indexToAtlas = new ArrayList<>();

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

        Map<String, List<File>> mapsByType = new HashMap<>();

        // Categorize files by suffix
        for (File file : pngFiles) {
            String name = file.getName().replace(".png", "");
            int underscoreIdx = name.lastIndexOf("_");
            String suffix = underscoreIdx > 0 ? name.substring(underscoreIdx + 1).toLowerCase() : "albedo";

            // Normalize common suffixes
            switch (suffix) {
                case "n":
                case "normal":
                    suffix = "normal";
                    break;
                case "m":
                case "metal":
                case "metallic":
                    suffix = "metal";
                    break;
                case "h":
                case "height":
                    suffix = "height";
                    break;
                case "r":
                case "rough":
                case "roughness":
                    suffix = "roughness";
                    break;
                case "ao":
                case "ambientocclusion":
                    suffix = "ao";
                    break;
                case "o":
                case "opacity":
                case "alpha":
                    suffix = "opacity";
                    break;
                case "":
                    suffix = "albedo";
                    break;
            }

            mapsByType.computeIfAbsent(suffix, k -> new ArrayList<>()).add(file);
        }

        String folderName = atlasFolder.getName();
        AtlasGroup group = new AtlasGroup();

        // Assign IDs based on albedo (required for mapping)
        List<File> albedoFiles = mapsByType.getOrDefault("albedo", new ArrayList<>());
        assignUniqueIDs(albedoFiles, atlasFolder);

        // Process each type that exists
        for (Map.Entry<String, List<File>> entry : mapsByType.entrySet()) {
            String type = entry.getKey(); // albedo, normal, metal, height, etc.
            List<File> files = entry.getValue();

            Map<Integer, TextureSet> groupedSets = new HashMap<>();
            for (File f : files) {
                String textureName = f.getName().replace(".png", "");
                String key = folderName + "/" + textureName;
                int id = texturePathToID.getOrDefault(key, nextTextureID++);
                texturePathToID.putIfAbsent(key, id);
                idToTexturePath.putIfAbsent(id, key);

                groupedSets.put(id, new TextureSet(id, key));
            }

            // Decide default color for missing textures (only used if map is missing in a
            // set)
            Color defaultColor = switch (type) {
                case "normal" -> NORMAL_MAP_DEFAULT;
                case "metal" -> METAL_MAP_DEFAULT;
                case "height" -> HEIGHT_MAP_DEFAULT;
                case "roughness" -> ROUGHNESS_MAP_DEFAULT;
                case "ao" -> AO_MAP_DEFAULT;
                case "opacity" -> OPACITY_MAP_DEFAULT;
                default -> Color.CLEAR;
            };

            TextureAtlas atlas = packType(groupedSets, m -> m.albedo, defaultColor, folderName, type);

            // Assign to group
            switch (type) {
                case "albedo":
                    group.albedo = atlas;
                    break;
                case "normal":
                    group.normal = atlas;
                    break;
                case "metal":
                    group.metal = atlas;
                    break;
                case "height":
                    group.height = atlas;
                    break;
                case "roughness":
                    group.roughness = atlas;
                    break;
                case "ao":
                    group.ao = atlas;
                    break;
                case "opacity":
                    group.opacity = atlas;
                    break;
                default:
                    group.custom.put(type, atlas);
                    break;
            }
        }

        atlasGroups.put(folderName, group);

        if (debug)
            System.out.println("Built atlas group for: " + folderName);
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
            Color defaultColor,
            String folder,
            String suffix) {

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
                regionName = "id_" + set.id;
            } else {
                if (!path.endsWith(".png")) {
                    path = path + ".png";
                }

                FileHandle handle = Gdx.files.internal(BLOCK_TEXTURE_PATH + "/" + path);
                if (handle.exists()) {
                    pixmap = new Pixmap(handle);
                } else {
                    System.err.println("Warning: Missing texture: " + handle.path());
                    pixmap = createDefaultPixmap(defaultColor);
                }

                regionName = new File(path).getName().replace(".png", "");
            }

            packer.pack(regionName, pixmap);
            pixmap.dispose();
        }

        // Build the atlas
        TextureAtlas atlas = packer.generateTextureAtlas(
                Texture.TextureFilter.Nearest,
                Texture.TextureFilter.Nearest,
                false);

        // Register atlas (assigns index)
        registerAtlas(folder + "_" + suffix, atlas);

        // --- SAFELY resize idToAtlasIndex ---
        if (idToAtlasIndex == null) {
            idToAtlasIndex = new int[nextTextureID];
            Arrays.fill(idToAtlasIndex, -1); // mark unassigned
        } else if (idToAtlasIndex.length < nextTextureID) {
            int oldLength = idToAtlasIndex.length;
            idToAtlasIndex = Arrays.copyOf(idToAtlasIndex, nextTextureID);
            Arrays.fill(idToAtlasIndex, oldLength, nextTextureID, -1); // fill new slots
        }

        // Fill id→atlasIndex mapping
        int atlasIndex = atlasToIndex.get(atlas);
        for (TextureSet set : groupedSets.values()) {
            idToAtlasIndex[set.id] = atlasIndex;
        }

        return atlas;
    }

    private Pixmap createDefaultPixmap(Color color) {

        Pixmap pixmap = new Pixmap(BLOCK_TEXTURE_SIZE, BLOCK_TEXTURE_SIZE, Pixmap.Format.RGBA8888);
        pixmap.setColor(color);
        pixmap.fill();

        return pixmap;
    }

    private void registerAtlas(String name, TextureAtlas atlas) {

        atlasNameMap.put(name, atlas);

        int index = indexToAtlas.size();
        atlasToIndex.put(atlas, index);
        indexToAtlas.add(atlas);

        if (debug)
            System.out.println("Registered atlas: " + name + " -> index " + index);
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

    public TextureAtlas getAtlasFromID(int id) {
        if (id < 0 || id >= idToAtlasIndex.length)
            return null;
        return indexToAtlas.get(idToAtlasIndex[id]);
    }

    public int getNextTextureID() {
        return nextTextureID;
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
