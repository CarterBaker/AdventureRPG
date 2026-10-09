package application.bootstrap.worldpipeline.settlementmanager;

import application.bootstrap.worldpipeline.biome.BiomeBlendStruct;
import engine.root.AsyncContainerPackage;

public class SettlementSiteAsyncContainer extends AsyncContainerPackage {

    /*
     * Thread-local scratch for settling one settlement cell's site: the blend
     * its candidate centre's biome is judged by.
     */

    // Biome
    BiomeBlendStruct blend;

    @Override
    protected void create() {
        this.blend = new BiomeBlendStruct();
    }

    @Override
    public void reset() {
        blend.reset();
    }
}
