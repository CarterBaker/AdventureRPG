package application.bootstrap.contentpipeline.contentmanager;

import java.io.File;

import application.bootstrap.contentpipeline.content.ContentData;
import application.bootstrap.contentpipeline.content.ContentHandle;
import application.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryType;
import application.bootstrap.shaderpipeline.texture.TextureHandle;
import application.bootstrap.shaderpipeline.texture.TextureRevealStruct;
import application.bootstrap.shaderpipeline.texturemanager.TextureManager;
import application.bootstrap.worldpipeline.block.BlockHandle;
import application.bootstrap.worldpipeline.blockmanager.BlockManager;
import engine.root.BuilderPackage;
import engine.root.EngineSetting;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.mathematics.extras.Direction3Vector;
import it.unimi.dsi.fastutil.shorts.ShortIterator;
import it.unimi.dsi.fastutil.shorts.ShortOpenHashSet;

class ContentBuilder extends BuilderPackage {

    /*
     * Parses content ARPG into ContentData wrapped in a ContentHandle, under
     * the ID ContentManager assigns its name. Every host must be a whole
     * breakable FULL-geometry block, and the overlay tile must live in the
     * same texture array as every face of each host, since the surface
     * shader reads both from the one array it draws the host with, and must
     * show at least one texel. The break tier, durability and yield are
     * never negative, and a yield is only declared alongside the item
     * texture its pieces are drawn with. Bootstrap only.
     */

    // Internal
    private ContentManager contentManager;
    private BlockManager blockManager;
    private TextureManager textureManager;

    // Base \\

    @Override
    protected void get() {
        this.contentManager = get(ContentManager.class);
        this.blockManager = get(BlockManager.class);
        this.textureManager = get(TextureManager.class);
    }

    // Build \\

    ContentHandle build(File file, String contentName) {

        ArpgObjectStruct arpg = ArpgUtility.loadObject(file);
        short contentID = contentManager.registerContentName(contentName);

        String displayName = ArpgUtility.getString(arpg, "display_name", contentName);

        // Hosts
        ShortOpenHashSet hostBlockIDs = parseHosts(arpg, contentName);

        // Rendering
        TextureHandle texture = textureManager.getTextureHandleFromTextureName(
                ArpgUtility.validateString(arpg, "texture"));
        validateTextureArray(texture, hostBlockIDs, contentName);
        TextureRevealStruct reveal = validateReveal(texture, contentName);

        // Breaking
        int breakTier = parseNonNegative(arpg, "break_tier", EngineSetting.DEFAULT_CONTENT_BREAK_TIER, contentName);
        int durability = parseNonNegative(arpg, "durability", EngineSetting.DEFAULT_CONTENT_DURABILITY, contentName);

        // Pieces
        String itemTextureName = ArpgUtility.getString(
                arpg, "item_texture", EngineSetting.CONTENT_ITEM_TEXTURE_NONE);
        int yield = parseYield(arpg, itemTextureName, contentName);

        ContentData contentData = new ContentData(
                contentName,
                contentID,
                displayName,
                hostBlockIDs,
                texture.getTileID(),
                reveal,
                breakTier,
                durability,
                itemTextureName,
                yield);

        ContentHandle contentHandle = create(ContentHandle.class);
        contentHandle.constructor(contentData);

        return contentHandle;
    }

    // Hosts \\

    private ShortOpenHashSet parseHosts(ArpgObjectStruct arpg, String contentName) {

        ArpgArrayStruct hostArray = ArpgUtility.validateArray(arpg, "hosts");

        if (hostArray.size() == 0)
            throwException("Content \"" + contentName
                    + "\" names no \"hosts\" — it must be held by at least one block.");

        ShortOpenHashSet hostBlockIDs = new ShortOpenHashSet(hostArray.size());

        for (int i = 0; i < hostArray.size(); i++) {

            String blockName = hostArray.get(i).getAsString();
            BlockHandle blockHandle = blockManager.getBlockHandleFromBlockName(blockName);

            if (blockHandle.getGeometry() != DynamicGeometryType.FULL)
                throwException("Content \"" + contentName + "\" names host \"" + blockName
                        + "\", which is not a FULL-geometry block — contents are held only by whole cubes.");

            if (blockHandle.isUnbreakable())
                throwException("Content \"" + contentName + "\" names host \"" + blockName
                        + "\", which cannot be broken — a content must be reachable by mining its host.");

            if (!hostBlockIDs.add(blockHandle.getBlockID()))
                throwException("Content \"" + contentName + "\" names host \"" + blockName + "\" more than once.");
        }

        return hostBlockIDs;
    }

    // Rendering \\

    private void validateTextureArray(
            TextureHandle textureHandle,
            ShortOpenHashSet hostBlockIDs,
            String contentName) {

        ShortIterator iterator = hostBlockIDs.iterator();

        while (iterator.hasNext()) {

            BlockHandle hostHandle = blockManager.getBlockHandleFromBlockID(iterator.nextShort());

            for (Direction3Vector face : Direction3Vector.VALUES) {

                int hostTileID = hostHandle.getTextureForFace(face);

                if (hostTileID == EngineSetting.BLOCK_TEXTURE_UNDEFINED)
                    throwException("Content \"" + contentName + "\" names host \"" + hostHandle.getBlockName()
                            + "\", which draws no texture to lay its overlay over.");

                if (textureManager.getTextureHandleFromTileID(hostTileID).getArrayID() != textureHandle.getArrayID())
                    throwException("Content \"" + contentName + "\" texture \"" + textureHandle.getTileName()
                            + "\" lives in another texture array than host \"" + hostHandle.getBlockName()
                            + "\" — a content's tile must share the array its hosts are drawn from.");
            }
        }
    }

    private TextureRevealStruct validateReveal(TextureHandle textureHandle, String contentName) {

        TextureRevealStruct reveal = textureHandle.getReveal();

        if (reveal == null)
            throwException("Content \"" + contentName + "\" texture \"" + textureHandle.getTileName()
                    + "\" carries no reveal — a content's tile must come from an image texture array.");

        if (reveal.getLevelShare(EngineSetting.COVERAGE_LEVEL_MAX) <= 0f)
            throwException("Content \"" + contentName + "\" texture \"" + textureHandle.getTileName()
                    + "\" is transparent throughout — an overlay must show at least one texel.");

        return reveal;
    }

    // Breaking \\

    private int parseNonNegative(ArpgObjectStruct arpg, String key, int fallback, String contentName) {

        int value = ArpgUtility.getInt(arpg, key, fallback);

        if (value < 0)
            throwException("Content \"" + contentName + "\" has \"" + key + "\" " + value
                    + " — it cannot be negative.");

        return value;
    }

    // Pieces \\

    private int parseYield(ArpgObjectStruct arpg, String itemTextureName, String contentName) {

        if (itemTextureName.equals(EngineSetting.CONTENT_ITEM_TEXTURE_NONE)) {

            if (arpg.has("yield"))
                throwException("Content \"" + contentName
                        + "\" declares a \"yield\" but names no \"item_texture\" its pieces are drawn with.");

            return 0;
        }

        textureManager.getTileIDFromTextureName(itemTextureName);

        int yield = ArpgUtility.getInt(arpg, "yield", EngineSetting.DEFAULT_CONTENT_YIELD);

        if (yield < 1 || yield > EngineSetting.BLOCK_PIECE_STACK_SIZE)
            throwException("Content \"" + contentName + "\" has \"yield\" " + yield
                    + " — it must run from 1 to " + EngineSetting.BLOCK_PIECE_STACK_SIZE + " pieces.");

        return yield;
    }
}
