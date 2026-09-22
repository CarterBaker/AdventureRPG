package application.bootstrap.worldpipeline.worldgenerationmanager;

import application.bootstrap.worldpipeline.biome.BiomeBlendStruct;
import engine.root.StructPackage;
import it.unimi.dsi.fastutil.longs.Long2FloatOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

public class TerrainSampleStruct extends StructPackage {

    /*
     * Memo for point queries against the terrain function outside of chunk
     * generation — structure placement and road planning ask for the ground
     * height at thousands of arbitrary positions, and each one lands on the
     * same world-aligned macro and detail grid points chunk generation
     * uses. Memoizing those grid points means a road a thousand blocks long
     * only evaluates the biome field once per macro point it passes, not
     * once per block. Owned by one planning call on one thread; never
     * shared, so it needs no locking.
     */

    // Macro grid point → { shape, detail amplitude, detail wavelength, ocean weight }
    final Long2ObjectOpenHashMap<float[]> macroPoints = new Long2ObjectOpenHashMap<>();

    // Detail grid point → detail offset in blocks
    final Long2FloatOpenHashMap detailPoints = new Long2FloatOpenHashMap();

    final BiomeBlendStruct blend = new BiomeBlendStruct();

    public void clear() {
        macroPoints.clear();
        detailPoints.clear();
    }
}
