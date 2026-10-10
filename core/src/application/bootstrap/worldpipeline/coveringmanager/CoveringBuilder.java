package application.bootstrap.worldpipeline.coveringmanager;

import java.io.File;

import application.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryType;
import application.bootstrap.shaderpipeline.texture.TextureHandle;
import application.bootstrap.shaderpipeline.texture.TextureRevealStruct;
import application.bootstrap.shaderpipeline.texturemanager.TextureManager;
import application.bootstrap.worldpipeline.block.BlockHandle;
import application.bootstrap.worldpipeline.blockmanager.BlockManager;
import application.bootstrap.worldpipeline.covering.CoveringData;
import application.bootstrap.worldpipeline.covering.CoveringHandle;
import application.bootstrap.worldpipeline.util.CoverageUtility;
import engine.root.BuilderPackage;
import engine.root.EngineSetting;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.mathematics.extras.Direction3Vector;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.shorts.ShortIterator;
import it.unimi.dsi.fastutil.shorts.ShortOpenHashSet;

class CoveringBuilder extends BuilderPackage {

    /*
     * Parses covering ARPG into CoveringData wrapped in a CoveringHandle,
     * under the ID CoveringManager assigns its name. Every host must be a
     * whole FULL-geometry block, and the top and optional side tiles must
     * live in the same texture array as each host's own faces, since the
     * surface shader reads both from the one array it draws the host with.
     * Chances run from 0 to 1, the spread level from 1 to the full level,
     * and the tint strength from 0 to 1. Bootstrap only.
     */

    // Internal
    private CoveringManager coveringManager;
    private BlockManager blockManager;
    private TextureManager textureManager;

    // Base \\

    @Override
    protected void get() {
        this.coveringManager = get(CoveringManager.class);
        this.blockManager = get(BlockManager.class);
        this.textureManager = get(TextureManager.class);
    }

    // Build \\

    CoveringHandle build(File file, String coveringName) {

        ArpgObjectStruct arpg = ArpgUtility.loadObject(file);
        short coveringID = coveringManager.registerCoveringName(coveringName);

        // Hosts
        ShortOpenHashSet hostBlockIDs = parseHosts(arpg, coveringName);

        // Rendering
        TextureHandle topTexture = textureManager.getTextureHandleFromTextureName(
                ArpgUtility.validateString(arpg, "top_texture"));
        validateTextureArray(topTexture, hostBlockIDs, coveringName);

        int sideTileID = EngineSetting.BLOCK_TEXTURE_UNDEFINED;
        TextureRevealStruct sideReveal = null;
        String sideTextureName = ArpgUtility.getString(arpg, "side_texture", EngineSetting.COVERING_TEXTURE_NONE);

        if (!sideTextureName.equals(EngineSetting.COVERING_TEXTURE_NONE)) {
            TextureHandle sideTexture = textureManager.getTextureHandleFromTextureName(sideTextureName);
            validateTextureArray(sideTexture, hostBlockIDs, coveringName);
            sideTileID = sideTexture.getTileID();
            sideReveal = validateReveal(sideTexture, coveringName);
        }

        float tintStrength = parseShare(
                arpg, "tint_strength", EngineSetting.DEFAULT_COVERING_TINT_STRENGTH, coveringName);

        // Growth
        float growthChance = parseShare(
                arpg, "growth_chance", EngineSetting.DEFAULT_COVERING_GROWTH_CHANCE, coveringName);
        float spreadChance = parseShare(
                arpg, "spread_chance", EngineSetting.DEFAULT_COVERING_SPREAD_CHANCE, coveringName);
        int spreadLevel = parseSpreadLevel(arpg, coveringName);
        boolean requiresOpenTop = ArpgUtility.getBoolean(
                arpg, "requires_open_top", EngineSetting.DEFAULT_COVERING_REQUIRES_OPEN_TOP);
        int moistureRadius = parseMoistureRadius(arpg, coveringName);

        // Drop
        String dropItemName = ArpgUtility.getString(arpg, "drop", EngineSetting.COVERING_DROP_NONE);

        CoveringData coveringData = new CoveringData(
                coveringName,
                coveringID,
                RegistryUtility.toNameSeed(coveringName),
                hostBlockIDs,
                topTexture.getTileID(),
                sideTileID,
                validateReveal(topTexture, coveringName),
                sideReveal,
                tintStrength,
                growthChance,
                spreadChance,
                spreadLevel,
                requiresOpenTop,
                moistureRadius,
                dropItemName);

        CoveringHandle coveringHandle = create(CoveringHandle.class);
        coveringHandle.constructor(coveringData);

        return coveringHandle;
    }

    // Hosts \\

    private ShortOpenHashSet parseHosts(ArpgObjectStruct arpg, String coveringName) {

        ArpgArrayStruct hostArray = ArpgUtility.validateArray(arpg, "hosts");

        if (hostArray.size() == 0)
            throwException("Covering \"" + coveringName
                    + "\" names no \"hosts\" — it must grow on at least one block.");

        ShortOpenHashSet hostBlockIDs = new ShortOpenHashSet(hostArray.size());

        for (int i = 0; i < hostArray.size(); i++) {

            String blockName = hostArray.get(i).getAsString();
            BlockHandle blockHandle = blockManager.getBlockHandleFromBlockName(blockName);

            if (blockHandle.getGeometry() != DynamicGeometryType.FULL)
                throwException("Covering \"" + coveringName + "\" names host \"" + blockName
                        + "\", which is not a FULL-geometry block — coverings grow only over whole cube faces.");

            if (!hostBlockIDs.add(blockHandle.getBlockID()))
                throwException("Covering \"" + coveringName + "\" names host \"" + blockName + "\" more than once.");
        }

        return hostBlockIDs;
    }

    // Rendering \\

    private void validateTextureArray(
            TextureHandle textureHandle,
            ShortOpenHashSet hostBlockIDs,
            String coveringName) {

        ShortIterator iterator = hostBlockIDs.iterator();

        while (iterator.hasNext()) {

            BlockHandle hostHandle = blockManager.getBlockHandleFromBlockID(iterator.nextShort());
            int hostTileID = hostHandle.getTextureForFace(Direction3Vector.UP);

            if (hostTileID == EngineSetting.BLOCK_TEXTURE_UNDEFINED)
                throwException("Covering \"" + coveringName + "\" names host \"" + hostHandle.getBlockName()
                        + "\", which draws no texture to grow over.");

            if (textureManager.getTextureHandleFromTileID(hostTileID).getArrayID() != textureHandle.getArrayID())
                throwException("Covering \"" + coveringName + "\" texture \"" + textureHandle.getTileName()
                        + "\" lives in another texture array than host \"" + hostHandle.getBlockName()
                        + "\" — a covering's tiles must share the array its hosts are drawn from.");
        }
    }

    private TextureRevealStruct validateReveal(TextureHandle textureHandle, String coveringName) {

        TextureRevealStruct reveal = textureHandle.getReveal();

        if (reveal == null)
            throwException("Covering \"" + coveringName + "\" texture \"" + textureHandle.getTileName()
                    + "\" carries no reveal — a covering's tiles must come from an image texture array.");

        return reveal;
    }

    // Growth \\

    private float parseShare(ArpgObjectStruct arpg, String key, float fallback, String coveringName) {

        float share = ArpgUtility.getFloat(arpg, key, fallback);

        if (share < 0f || share > 1f)
            throwException("Covering \"" + coveringName + "\" has \"" + key + "\" " + share
                    + " — it must lie between 0 and 1.");

        return share;
    }

    private int parseSpreadLevel(ArpgObjectStruct arpg, String coveringName) {

        int spreadLevel = ArpgUtility.getInt(arpg, "spread_level", EngineSetting.DEFAULT_COVERING_SPREAD_LEVEL);

        if (spreadLevel < 1 || spreadLevel > CoverageUtility.LEVEL_MAX)
            throwException("Covering \"" + coveringName + "\" has \"spread_level\" " + spreadLevel
                    + " — it must lie between 1 and " + CoverageUtility.LEVEL_MAX + ".");

        return spreadLevel;
    }

    private int parseMoistureRadius(ArpgObjectStruct arpg, String coveringName) {

        int moistureRadius = ArpgUtility.getInt(
                arpg, "moisture_radius", EngineSetting.DEFAULT_COVERING_MOISTURE_RADIUS);

        if (moistureRadius < 0 || moistureRadius >= EngineSetting.CHUNK_SIZE)
            throwException("Covering \"" + coveringName + "\" has \"moisture_radius\" " + moistureRadius
                    + " — it must lie between 0 and " + (EngineSetting.CHUNK_SIZE - 1) + " blocks.");

        return moistureRadius;
    }
}
