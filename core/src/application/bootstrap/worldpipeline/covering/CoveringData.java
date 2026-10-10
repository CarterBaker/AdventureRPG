package application.bootstrap.worldpipeline.covering;

import application.bootstrap.shaderpipeline.texture.TextureRevealStruct;
import engine.root.DataPackage;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.shorts.ShortOpenHashSet;

public class CoveringData extends DataPackage {

    /*
     * Immutable covering definition: the grass, moss or litter that grows
     * over the faces of the blocks it names as hosts, never a block itself.
     * Its top tiles cover a host's upper face and its optional side tiles
     * its upright faces, each revealed by its growth map as the cell's level
     * rises. It grows a level by its growth chance each tick, spreads to a
     * bare host beside it by its spread chance once it reaches its spread
     * level, recedes under a solid block when it needs an open top, and
     * grows or spreads only near liquid when it names a moisture radius. Its
     * tint strength is how far the biome's tint colors it, its reveals how
     * its top and side tiles show at every level, which distant faces and
     * maps stand in for it with, and a fully grown cell broken whole hands
     * out its drop item, if it names one.
     */

    // Identity
    private final String coveringName;
    private final short coveringID;
    private final int nameSeed;

    // Hosts
    private final ShortOpenHashSet hostBlockIDs;

    // Rendering
    private final int topTileID;
    private final int sideTileID;
    private final TextureRevealStruct topReveal;
    private final TextureRevealStruct sideReveal;
    private final float tintStrength;

    // Growth
    private final float growthChance;
    private final float spreadChance;
    private final int spreadLevel;
    private final boolean requiresOpenTop;
    private final int moistureRadius;

    // Drop — the item a fully grown cell hands out, COVERING_DROP_NONE for none
    private final String dropItemName;

    // Constructor \\

    public CoveringData(
            String coveringName,
            short coveringID,
            int nameSeed,
            ShortOpenHashSet hostBlockIDs,
            int topTileID,
            int sideTileID,
            TextureRevealStruct topReveal,
            TextureRevealStruct sideReveal,
            float tintStrength,
            float growthChance,
            float spreadChance,
            int spreadLevel,
            boolean requiresOpenTop,
            int moistureRadius,
            String dropItemName) {

        // Identity
        this.coveringName = coveringName;
        this.coveringID = coveringID;
        this.nameSeed = nameSeed;

        // Hosts
        this.hostBlockIDs = hostBlockIDs;

        // Rendering
        this.topTileID = topTileID;
        this.sideTileID = sideTileID;
        this.topReveal = topReveal;
        this.sideReveal = sideReveal;
        this.tintStrength = tintStrength;

        // Growth
        this.growthChance = growthChance;
        this.spreadChance = spreadChance;
        this.spreadLevel = spreadLevel;
        this.requiresOpenTop = requiresOpenTop;
        this.moistureRadius = moistureRadius;

        // Drop
        this.dropItemName = dropItemName;
    }

    // Accessible \\

    public String getCoveringName() {
        return coveringName;
    }

    public short getCoveringID() {
        return coveringID;
    }

    public int getNameSeed() {
        return nameSeed;
    }

    public boolean canHost(short blockID) {
        return hostBlockIDs.contains(blockID);
    }

    public int getTopTileID() {
        return topTileID;
    }

    public int getSideTileID() {
        return sideTileID;
    }

    public boolean hasSide() {
        return sideTileID != EngineSetting.BLOCK_TEXTURE_UNDEFINED;
    }

    public TextureRevealStruct getTopReveal() {
        return topReveal;
    }

    // The side tiles' reveal, null where the covering has no side tiles
    public TextureRevealStruct getSideReveal() {
        return sideReveal;
    }

    public float getTintStrength() {
        return tintStrength;
    }

    public float getGrowthChance() {
        return growthChance;
    }

    public float getSpreadChance() {
        return spreadChance;
    }

    public int getSpreadLevel() {
        return spreadLevel;
    }

    public boolean requiresOpenTop() {
        return requiresOpenTop;
    }

    public int getMoistureRadius() {
        return moistureRadius;
    }

    public boolean requiresMoisture() {
        return moistureRadius > 0;
    }

    public String getDropItemName() {
        return dropItemName;
    }

    public boolean hasDrop() {
        return !dropItemName.equals(EngineSetting.COVERING_DROP_NONE);
    }
}
