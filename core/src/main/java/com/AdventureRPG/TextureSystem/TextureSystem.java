package com.AdventureRPG.TextureSystem;

import java.io.File;
import java.util.*;

import com.AdventureRPG.Core.Exceptions.FileException;
import com.AdventureRPG.Core.Root.SystemFrame;
import com.AdventureRPG.Util.GlobalConstant;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.TextureArray;
import com.badlogic.gdx.graphics.Pixmap.Format;

// TODO: Needs to be studied for accuracy and refactored into multiple classes for clarity
public class TextureSystem extends SystemFrame {

    // Settings
    private String BLOCK_TEXTURE_PATH;
    private int BLOCK_TEXTURE_SIZE;
    private int BLOCK_ATLAS_PADDING;

    private Color NORMAL_MAP_DEFAULT;
    private Color HEIGHT_MAP_DEFAULT;
    private Color METAL_MAP_DEFAULT;
    private Color ROUGHNESS_MAP_DEFAULT;
    private Color AO_MAP_DEFAULT;
    private Color CUSTOM_MAP_DEFAULT;

    // ID maps
    private Map<Integer, String> idToTexturePath;
    private Map<String, Integer> texturePathToID;
    private int nextTextureID;

    // Lookup
    private String BASE_ALIAS;
    private String NORMAL_ALIAS;
    private String METAL_ALIAS;
    private String ROUGH_ALIAS;
    private String HEIGHT_ALIAS;
    private String AO_ALIAS;

    private Map<String, String> TYPE_ALIASES;
    private Map<String, Color> ALIAS_COLORS;

    // Folder to ArrayGroup
    private Map<String, ArrayGroup> arrayGroups;

    // UVs per ID (normalized)
    private Map<Integer, UVRect> idToUV;

    // Base \\

    @Override
    protected void create() {

        // Settings
        this.BLOCK_TEXTURE_PATH = GlobalConstant.BLOCK_TEXTURE_PATH;
        this.BLOCK_TEXTURE_SIZE = GlobalConstant.BLOCK_TEXTURE_SIZE;
        this.BLOCK_ATLAS_PADDING = GlobalConstant.BLOCK_ATLAS_PADDING;

        this.NORMAL_MAP_DEFAULT = GlobalConstant.NORMAL_MAP_DEFAULT;
        this.HEIGHT_MAP_DEFAULT = GlobalConstant.HEIGHT_MAP_DEFAULT;
        this.METAL_MAP_DEFAULT = GlobalConstant.METAL_MAP_DEFAULT;
        this.ROUGHNESS_MAP_DEFAULT = GlobalConstant.ROUGHNESS_MAP_DEFAULT;
        this.AO_MAP_DEFAULT = GlobalConstant.AO_MAP_DEFAULT;
        this.CUSTOM_MAP_DEFAULT = GlobalConstant.CUSTOM_MAP_DEFAULT;

        // ID maps
        this.idToTexturePath = new HashMap<>();
        this.texturePathToID = new HashMap<>();

        // Folder to ArrayGroup
        this.arrayGroups = new HashMap<>();

        // UVs per ID (normalized)
        this.idToUV = new HashMap<>();

        // ID maps
        this.nextTextureID = 0;

        // Lookup
        this.BASE_ALIAS = "Albedo";
        this.NORMAL_ALIAS = "Normal";
        this.METAL_ALIAS = "Metal";
        this.ROUGH_ALIAS = "Roughness";
        this.HEIGHT_ALIAS = "Height";
        this.AO_ALIAS = "AO";

        this.TYPE_ALIASES = TYPE_ALIASES();
        this.ALIAS_COLORS = ALIAS_COLORS();
    }

    @Override
    protected void init() {

        // Texture Manager
        compileArrays(new File(BLOCK_TEXTURE_PATH));

        // Clear memory
        TYPE_ALIASES.clear();
        ALIAS_COLORS.clear();
    }

    private Map<String, String> TYPE_ALIASES() {

        Map<String, String> output = new HashMap<>();

        // Normals
        output.put("n", NORMAL_ALIAS);
        output.put("norm", NORMAL_ALIAS);
        output.put("nor", NORMAL_ALIAS);
        output.put("normal", NORMAL_ALIAS);

        // Metal
        output.put("m", METAL_ALIAS);
        output.put("met", METAL_ALIAS);
        output.put("metal", METAL_ALIAS);
        output.put("metallic", METAL_ALIAS);

        // Roughness
        output.put("r", ROUGH_ALIAS);
        output.put("rough", ROUGH_ALIAS);
        output.put("roughness", ROUGH_ALIAS);

        // Height
        output.put("h", HEIGHT_ALIAS);
        output.put("height", HEIGHT_ALIAS);

        // Ambient Occlusion
        output.put("ao", AO_ALIAS);
        output.put("amboc", AO_ALIAS);
        output.put("ambientocclusion", AO_ALIAS);
        output.put("occlusion", AO_ALIAS);

        return output;
    }

    private Map<String, Color> ALIAS_COLORS() {

        Map<String, Color> output = new HashMap<>();

        output.put(BASE_ALIAS, NORMAL_MAP_DEFAULT);
        output.put(NORMAL_ALIAS, NORMAL_MAP_DEFAULT);
        output.put(METAL_ALIAS, METAL_MAP_DEFAULT);
        output.put(ROUGH_ALIAS, ROUGHNESS_MAP_DEFAULT);
        output.put(HEIGHT_ALIAS, HEIGHT_MAP_DEFAULT);
        output.put(AO_ALIAS, AO_MAP_DEFAULT);

        return output;
    }

    @Override
    protected void dispose() {

        for (ArrayGroup group : arrayGroups.values())
            if (group.array != null)
                group.array.dispose();

        arrayGroups.clear();
    }

    // Core Logic \\

    // Assemble each folder from root
    private void compileArrays(File root) {

        if (!root.exists() || !root.isDirectory())
            throw new FileException.FileNotFoundException(root);

        File[] subfolders = root.listFiles(File::isDirectory);

        if (subfolders == null || subfolders.length == 0)
            return;

        for (File folder : subfolders)
            process(folder);
    }

    // Process each folder individually
    private void process(File folder) {

        String folderName = folder.getName();
        Map<String, List<File>> byType = categorizePNG(folder);

        if (byType.isEmpty())
            return;

        // Assign global IDs from albedo files in this folder
        List<File> albedoFiles = byType.getOrDefault(BASE_ALIAS, Collections.emptyList());
        assignIDs(albedoFiles, folderName);

        // Build a layout (grid) for this folder, based on the IDs present from albedo
        FolderLayout layout = computeLayout(folderName, albedoFiles);

        // Build a stitched atlas pixmap per type using the SAME layout (keeps UVs in
        // sync)
        Map<String, FileHandle> typeToTempFile = new LinkedHashMap<>();
        LinkedHashMap<String, Integer> layerMap = new LinkedHashMap<>();

        // Standard order for stable layer indices
        String[] stdTypes = new String[] {
                BASE_ALIAS,
                NORMAL_ALIAS,
                METAL_ALIAS,
                ROUGH_ALIAS,
                HEIGHT_ALIAS,
                AO_ALIAS
        };
        int nextLayer = 0;

        for (String type : stdTypes) {

            FileHandle png = buildLayerPNG(folderName, type, byType.get(type), layout, getDefaultColor(type));
            typeToTempFile.put(type, png);
            layerMap.put(type, nextLayer++);
        }

        // Custom types come after standard ones
        for (Map.Entry<String, List<File>> e : byType.entrySet()) {

            String type = e.getKey();

            if (!layerMap.containsKey(type)) {
                FileHandle png = buildLayerPNG(folderName, type, e.getValue(), layout, CUSTOM_MAP_DEFAULT);
                typeToTempFile.put(type, png);
                layerMap.put(type, nextLayer++);
            }
        }

        // Create TextureArray layer order matches insertion order of typeToTempFile
        TextureArray array = new TextureArray(typeToTempFile.values().toArray(new FileHandle[0]));
        arrayGroups.put(folderName, new ArrayGroup(array, layerMap));

        // We can delete temp PNGs after upload (TextureArray has already read them)
        cleanupTempFiles(typeToTempFile.values());
    }

    // Categorization & Suffix logic \\

    // Categorize all png files in folder
    private Map<String, List<File>> categorizePNG(File folder) {

        File[] files = folder.listFiles(f -> f.isFile() && f.getName().toLowerCase().endsWith(".png"));

        if (files == null)
            return Collections.emptyMap();

        Map<String, List<File>> output = new HashMap<>();

        for (File file : files) {

            String type = suffixOf(file.getName());
            output.computeIfAbsent(type, k -> new ArrayList<>()).add(file);
        }

        return output;
    }

    private String suffixOf(String filename) {

        String base = filename.endsWith(".png") ? filename.substring(0, filename.length() - 4) : filename;
        int i = base.lastIndexOf('_');

        if (i < 0)// no suffix at all
            return BASE_ALIAS;

        String suffix = base.substring(i + 1).toLowerCase();

        return TYPE_ALIASES.getOrDefault(suffix, BASE_ALIAS);
    }

    // ID Handling \\

    private void assignIDs(List<File> albedoFiles, String folderName) {

        for (File f : albedoFiles) {

            String key = folderName + "/" + stripExtension(f.getName());

            if (!texturePathToID.containsKey(key)) {

                int id = nextTextureID++;

                texturePathToID.put(key, id);
                idToTexturePath.put(id, key);
            }
        }
    }

    // Layout \\

    private FolderLayout computeLayout(String folderName, List<File> albedoFiles) {

        // Collect sorted keys for stability
        List<String> keys = new ArrayList<>();

        for (File file : albedoFiles)
            keys.add(folderName + "/" + stripExtension(file.getName()));

        Collections.sort(keys); // deterministic order

        int count = keys.size();

        if (count == 0)
            return new FolderLayout(folderName, new HashMap<>(),
                    BLOCK_TEXTURE_SIZE + 2 * BLOCK_ATLAS_PADDING,
                    BLOCK_TEXTURE_SIZE + 2 * BLOCK_ATLAS_PADDING);

        // Simple square-ish grid
        int columns = (int) Math.ceil(Math.sqrt(count));
        int rows = (int) Math.ceil((double) count / columns);

        int cell = BLOCK_TEXTURE_SIZE + 2 * BLOCK_ATLAS_PADDING;
        int atlasWidth = columns * cell;
        int atlasHeight = rows * cell;

        Map<Integer, Slot> idToSlot = new HashMap<>();

        for (int i = 0; i < count; i++) {

            int row = i / columns;
            int col = i % columns;

            int x = col * cell + BLOCK_ATLAS_PADDING;
            int y = row * cell + BLOCK_ATLAS_PADDING;

            String key = keys.get(i);
            Integer id = texturePathToID.get(key);

            if (id != null)
                idToSlot.put(id, new Slot(x, y, BLOCK_TEXTURE_SIZE, BLOCK_TEXTURE_SIZE, atlasWidth, atlasHeight));
        }

        // Store normalized UVs for every ID in this folder
        for (Map.Entry<Integer, Slot> e : idToSlot.entrySet()) {

            Slot s = e.getValue();

            float u0 = s.x / (float) s.atlasW;
            float v0 = s.y / (float) s.atlasH;
            float u1 = (s.x + s.w) / (float) s.atlasW;
            float v1 = (s.y + s.h) / (float) s.atlasH;

            idToUV.put(e.getKey(), new UVRect(u0, v0, u1, v1));
        }

        return new FolderLayout(folderName, idToSlot, atlasWidth, atlasHeight);
    }

    // Layer construction \\

    private FileHandle buildLayerPNG(String folderName,
            String type,
            List<File> filesOfType,
            FolderLayout layout,
            Color defaultColor) {

        Map<String, File> nameToFile = new HashMap<>();

        if (filesOfType != null) {

            for (File f : filesOfType) {

                // remove explicit suffix if present (_n/_normal etc.)
                String base = stripExtension(f.getName());
                String logicalName = stripSuffix(base);

                nameToFile.put(logicalName, f);
            }
        }

        // Create atlas pixmap for this layer
        Pixmap atlas = new Pixmap(layout.atlasW, layout.atlasH, Format.RGBA8888);
        fillPixmap(atlas, defaultColor);

        // Draw each tile at its assigned slot
        for (Map.Entry<Integer, Slot> e : layout.idToSlot.entrySet()) {

            int id = e.getKey();
            Slot s = e.getValue();

            String key = idToTexturePath.get(id); // folder/nameOriginal
            String nameWithinFolder = key.substring(key.indexOf('/') + 1); // nameOriginal
            String logical = stripSuffix(nameWithinFolder);

            Pixmap tile = loadTilePixmap(folderName, nameToFile.get(logical), type, defaultColor);
            atlas.drawPixmap(tile, s.x, s.y, 0, 0, Math.min(tile.getWidth(), s.w), Math.min(tile.getHeight(), s.h));
            tile.dispose();
        }

        // Save as temp PNG, return handle
        FileHandle out = tempHandle(folderName + "_" + type + ".png");
        PixmapIO.writePNG(out, atlas);
        atlas.dispose();

        return out;
    }

    private Pixmap loadTilePixmap(String folderName, File fileOrNull, String type, Color defaultColor) {

        if (fileOrNull != null) {

            FileHandle fh = Gdx.files.internal(fileOrNull.getPath());

            if (fh.exists()) {

                try {

                    Pixmap p = new Pixmap(fh);

                    if (p.getWidth() == BLOCK_TEXTURE_SIZE && p.getHeight() == BLOCK_TEXTURE_SIZE)
                        return p;

                    // Resize if needed (keeps pipeline robust)
                    Pixmap resized = new Pixmap(BLOCK_TEXTURE_SIZE, BLOCK_TEXTURE_SIZE, Format.RGBA8888);

                    resized.drawPixmap(p, 0, 0, p.getWidth(), p.getHeight(),
                            0, 0,
                            BLOCK_TEXTURE_SIZE, BLOCK_TEXTURE_SIZE);

                    p.dispose();

                    return resized;
                }

                catch (Throwable throwable) {
                    // fall through to default
                }
            }
        }

        // Missing/bad → default tile
        return createDefaultTile(defaultColor);
    }

    // Utility \\

    private String stripExtension(String fileName) {
        return fileName.endsWith(".png") ? fileName.substring(0, fileName.length() - 4) : fileName;
    }

    private void fillPixmap(Pixmap pm, Color color) {

        pm.setColor(color);
        pm.fill();
    }

    private Pixmap createDefaultTile(Color color) {

        Pixmap tile = new Pixmap(BLOCK_TEXTURE_SIZE, BLOCK_TEXTURE_SIZE, Format.RGBA8888);

        tile.setColor(color);
        tile.fill();

        return tile;
    }

    private String stripSuffix(String baseName) {

        int i = baseName.lastIndexOf('_');

        if (i < 0)
            return baseName;

        String suf = baseName.substring(i + 1).toLowerCase();

        if (TYPE_ALIASES.containsKey(suf))
            return baseName.substring(0, i); // remove suffix

        return baseName; // unknown suffix, treat as part of name
    }

    private Color getDefaultColor(String type) {
        return ALIAS_COLORS.getOrDefault(type, CUSTOM_MAP_DEFAULT);
    }

    private FileHandle tempHandle(String fileName) {

        // Put temp atlases under local writable dir
        FileHandle directory = Gdx.files.local("temp"); // TODO: Look into removing temp folder on close

        if (!directory.exists())
            directory.mkdirs();

        return directory.child(fileName);
    }

    private void cleanupTempFiles(Collection<FileHandle> files) {

        for (FileHandle f : files) {

            try {
                if (f.exists())
                    f.delete();
            }

            catch (Exception exception) {

            }
        }
    }

    public static class UVRect {

        public final float u0, v0, u1, v1; // [0..1] in atlas space for that folder

        public UVRect(float u0, float v0, float u1, float v1) {

            this.u0 = u0;
            this.v0 = v0;
            this.u1 = u1;
            this.v1 = v1;
        }
    }

    private static class ArrayGroup {

        final TextureArray array;
        final Map<String, Integer> layerMap; // type to layer index

        ArrayGroup(TextureArray array, Map<String, Integer> layerMap) {

            this.array = array;
            this.layerMap = layerMap;
        }
    }

    private static class Slot {

        final int x, y, w, h;
        final int atlasW, atlasH;

        Slot(int x, int y, int w, int h, int atlasW, int atlasH) {

            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.atlasW = atlasW;
            this.atlasH = atlasH;
        }
    }

    private static class FolderLayout {

        final String folder;
        final Map<Integer, Slot> idToSlot;
        final int atlasW, atlasH;

        FolderLayout(String folder, Map<Integer, Slot> idToSlot, int atlasW, int atlasH) {

            this.folder = folder;
            this.idToSlot = idToSlot;
            this.atlasW = atlasW;
            this.atlasH = atlasH;
        }
    }

    // Accessible \\

    // Returns the TextureArray for a folder, or null if none.
    public TextureArray getArray(String folder) {

        ArrayGroup g = arrayGroups.get(folder);
        return (g != null) ? g.array : null;
    }

    // Returns the layer index within the folder’s TextureArray for a given type
    public int getLayerIndex(String folder, String type) {

        ArrayGroup g = arrayGroups.get(folder);
        return (g != null) ? g.layerMap.getOrDefault(type, -1) : -1;
    }

    // Global: get ID from "folder/name" (without .png).
    public int getIDFromTexture(String texturePath) {
        return texturePathToID.getOrDefault(texturePath, -1);
    }

    // Global: get "folder/name" from ID.
    public String getTextureFromID(int id) {
        return idToTexturePath.getOrDefault(id, null);
    }

    // Global: normalized UVs for this ID within its folder atlas.
    public UVRect getUVRect(int id) {
        return idToUV.get(id);
    }

    // Highest assigned ID + 1.
    public int getNextTextureID() {
        return nextTextureID;
    }

    public TextureArray getArrayFromID(int id) {
        String path = idToTexturePath.get(id);
        if (path == null)
            return null;

        String folder = path.substring(0, path.indexOf('/'));
        ArrayGroup g = arrayGroups.get(folder);
        return (g != null) ? g.array : null;
    }
}
