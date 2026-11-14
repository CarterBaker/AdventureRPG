package application.bootstrap.worldpipeline.biome;

import engine.root.EngineSetting;
import engine.root.StructPackage;

public class BiomeReliefStruct extends StructPackage {

    /*
     * How a biome lays its height over the ground. An absolute biome's curves
     * are heights in their own right. A relative biome's curves are offsets
     * laid over the ground of the absolute biome it is chained into, so a
     * hill or a lake basin rides whatever that ground does, and its crown
     * shapes the offset across each patch: none holds it level to the outline
     * as a plateau, full gathers it toward the patch's heart as a dome or a
     * bowl. Wherever its patch breaks sharp the offset climbs part of its rise
     * at the edge, so a rolling flank turns into a scarp and back. A biome
     * holding still water lays its offset below its own water instead, so its
     * bed lies level under the surface. NONE is absolute.
     */

    public static final BiomeReliefStruct NONE = new BiomeReliefStruct(
            false, EngineSetting.DEFAULT_BIOME_RELIEF_CROWN);

    private final boolean relative;
    private final float crown;

    public BiomeReliefStruct(boolean relative, float crown) {
        this.relative = relative;
        this.crown = crown;
    }

    // Accessible \\

    public boolean isRelative() {
        return relative;
    }

    public float getCrown() {
        return crown;
    }
}
