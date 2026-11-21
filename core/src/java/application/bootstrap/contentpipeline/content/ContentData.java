package application.bootstrap.contentpipeline.content;

import application.bootstrap.contentpipeline.util.ContentUtility;
import application.bootstrap.shaderpipeline.texture.TextureRevealStruct;
import engine.root.DataPackage;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.shorts.ShortOpenHashSet;

public class ContentData extends DataPackage {

    /*
     * Immutable content definition: an ore, rock or root held inside the
     * whole blocks it names as hosts, never a block itself. Its overlay tile
     * is laid over every face of a host that holds it, the texels it leaves
     * transparent showing the block beneath, and its reveal is the share of
     * the tile it shows and the color it shows in, which distant faces and
     * maps stand in for it with. A cell holding it needs a tool of at least
     * its break tier and takes its durability in extra strikes. A cell holds
     * from one to the content's most, which its share of the tile shows by,
     * and when the cell's block breaks whole to the right tool a content with
     * an item texture hands out one piece for each it held beside the block's
     * own.
     */

    // Identity
    private final String contentName;
    private final short contentID;
    private final String displayName;

    // Hosts
    private final ShortOpenHashSet hostBlockIDs;

    // Rendering
    private final int tileID;
    private final TextureRevealStruct reveal;

    // Breaking
    private final int breakTier;
    private final int durability;

    // Amount — the most a cell holds
    private final int maxAmount;

    // Pieces — the item texture its pieces are drawn with, CONTENT_ITEM_TEXTURE_NONE for none
    private final String itemTextureName;

    // Constructor \\

    public ContentData(
            String contentName,
            short contentID,
            String displayName,
            ShortOpenHashSet hostBlockIDs,
            int tileID,
            TextureRevealStruct reveal,
            int breakTier,
            int durability,
            int maxAmount,
            String itemTextureName) {

        // Identity
        this.contentName = contentName;
        this.contentID = contentID;
        this.displayName = displayName;

        // Hosts
        this.hostBlockIDs = hostBlockIDs;

        // Rendering
        this.tileID = tileID;
        this.reveal = reveal;

        // Breaking
        this.breakTier = breakTier;
        this.durability = durability;

        // Amount
        this.maxAmount = maxAmount;

        // Pieces
        this.itemTextureName = itemTextureName;
    }

    // Accessible \\

    public String getContentName() {
        return contentName;
    }

    public short getContentID() {
        return contentID;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean canHost(short blockID) {
        return hostBlockIDs.contains(blockID);
    }

    public int getTileID() {
        return tileID;
    }

    public TextureRevealStruct getReveal() {
        return reveal;
    }

    // The share of its tile a host's face holding an amount shows, from none to the whole tile at its most
    public float getRevealShare(int amount) {
        return reveal.getLevelShare(ContentUtility.toRevealLevel(amount, maxAmount));
    }

    public int getBreakTier() {
        return breakTier;
    }

    public int getDurability() {
        return durability;
    }

    public int getMaxAmount() {
        return maxAmount;
    }

    public String getItemTextureName() {
        return itemTextureName;
    }

    public boolean hasPiece() {
        return !itemTextureName.equals(EngineSetting.CONTENT_ITEM_TEXTURE_NONE);
    }
}
