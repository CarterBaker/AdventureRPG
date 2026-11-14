package application.bootstrap.shaderpipeline.texturemanager;

import java.io.File;

import application.bootstrap.shaderpipeline.texture.TextureData;
import application.bootstrap.shaderpipeline.texture.TextureHandle;
import application.bootstrap.shaderpipeline.texture.TextureMapType;
import engine.root.BuilderPackage;
import engine.root.EngineSetting;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.io.FileUtility;

class TextureBuilder extends BuilderPackage {

    /*
     * Builds a TextureHandle from one baked tile definition: its size, the
     * average colour and reveal the far tier stands in with, the tier files
     * streaming reads, and the fallback and extra maps the pools take at
     * once. A tile spans its whole layer, so its UV region is the unit square
     * and its pixel size is the size it was baked at.
     */

    // Internal
    private TextureManager textureManager;
    private File bakeRoot;

    // Base \\

    @Override
    protected void create() {
        this.bakeRoot = new File(EngineSetting.BAKED_TEXTURE_PATH);
    }

    @Override
    protected void get() {
        this.textureManager = get(TextureManager.class);
    }

    // Build \\

    void build(File file, String arrayName) {

        String tileName = TextureBakeUtility.toTileName(arrayName, FileUtility.getFileName(file));
        ArpgObjectStruct arpg = ArpgUtility.loadObject(file);
        int size = ArpgUtility.validateInt(arpg, EngineSetting.TEXTURE_BAKE_KEY_SIZE);
        File[] tierFiles = new File[EngineSetting.TEXTURE_TIER_COUNT];
        int tierCount = 0;

        while (tierCount < EngineSetting.TEXTURE_TIER_COUNT && EngineSetting.TEXTURE_TIER_SIZES[tierCount] <= size) {

            tierFiles[tierCount] = TextureBakeUtility.resolveTierFile(bakeRoot, tileName, tierCount);

            if (!tierFiles[tierCount].isFile())
                throwException("Baked tier missing for texture '" + tileName + "': " + tierFiles[tierCount]
                        + ". Run gradlew lwjgl3:bake.");

            tierCount++;
        }

        if (tierCount == 0)
            throwException("Baked texture '" + tileName + "' is " + size + " pixels, below the smallest tier.");

        ArpgObjectStruct fallbackArpg = ArpgUtility.validateObject(arpg, EngineSetting.TEXTURE_BAKE_KEY_FALLBACK);
        byte[][] fallbackMaps = new byte[TextureMapType.TIERED_COUNT][];

        for (int m = 0; m < TextureMapType.TIERED_COUNT; m++)
            fallbackMaps[m] = TextureBakeUtility.readMap(fallbackArpg, TextureMapType.VALUES[m]);

        byte[] extraMap = ArpgUtility.hasBytes(arpg, EngineSetting.TEXTURE_BAKE_KEY_EXTRA)
                ? ArpgUtility.validateBytes(arpg, EngineSetting.TEXTURE_BAKE_KEY_EXTRA)
                : null;

        TextureData data = new TextureData(
                tileName, textureManager.registerTileName(tileName),
                textureManager.registerArrayName(arrayName), arrayName,
                EngineSetting.GL_HANDLE_NONE,
                size, size,
                0f, 0f, 1f, 1f,
                ArpgUtility.validateInt(arpg, EngineSetting.TEXTURE_BAKE_KEY_AVERAGE),
                TextureBakeUtility.readReveal(ArpgUtility.validateObject(arpg, EngineSetting.TEXTURE_BAKE_KEY_REVEAL)));

        TextureHandle handle = create(TextureHandle.class);
        handle.constructor(data);
        textureManager.addStreamedTile(handle, tierFiles, tierCount, fallbackMaps, extraMap);
    }
}
