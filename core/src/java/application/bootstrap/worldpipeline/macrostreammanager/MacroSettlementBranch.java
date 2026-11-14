package application.bootstrap.worldpipeline.macrostreammanager;

import application.bootstrap.worldpipeline.layout.LayoutSurfaceKind;
import application.bootstrap.worldpipeline.layout.LayoutSurfaceStruct;
import application.bootstrap.worldpipeline.layoutmanager.LayoutManager;
import application.bootstrap.worldpipeline.settlementmanager.SettlementManager;
import application.bootstrap.worldpipeline.world.WorldHandle;
import engine.root.BranchPackage;
import engine.root.EngineSetting;

public class MacroSettlementBranch extends BranchPackage {

    /*
     * Async — the settlements and roads distant macro terrain shows, judged
     * per lattice point without laying a single block. The layouts reaching
     * a tile are gathered once, and only while its lattice is fine enough to
     * show a building; every point then asks them what stands there, every
     * road, wall and lot widened by half a cell so the lattice still catches
     * them. A road on the land recolors the ground in its surface; a
     * structure, a wall or a bridge rises over it as its own layer in its
     * own colors, and clears any canopy the point carried, so woods never
     * grow through a village seen from afar.
     */

    // Internal
    private SettlementManager settlementManager;
    private LayoutManager layoutManager;

    // Base \\

    @Override
    protected void get() {
        this.settlementManager = get(SettlementManager.class);
        this.layoutManager = get(LayoutManager.class);
    }

    // Gather \\

    // Worker — every layout reaching the tile, when one lattice cell is no wider than a building shows across
    void gatherLayouts(
            MacroBuildAsyncContainer scratch,
            WorldHandle worldHandle,
            double originX,
            double originZ,
            float cellSizeBlocks) {

        scratch.layouts.clear();

        if (cellSizeBlocks > EngineSetting.SETTLEMENT_MACRO_MAX_CELL_BLOCKS)
            return;

        double tolerance = cellSizeBlocks * 0.5;
        double tileSize = EngineSetting.MACRO_TILE_SIZE_BLOCKS;

        settlementManager.collectLayouts(
                worldHandle,
                originX - tolerance, originZ - tolerance,
                originX + tileSize + tolerance, originZ + tileSize + tolerance,
                scratch.layouts);
    }

    // Sample \\

    // Worker — what the gathered layouts show at one lattice point, read after its ground and canopy
    void sampleSettlement(
            MacroBuildAsyncContainer scratch,
            WorldHandle worldHandle,
            int index,
            double x,
            double z,
            float cellSizeBlocks) {

        scratch.structureHeights[index] = 0f;

        if (scratch.layouts.isEmpty())
            return;

        LayoutSurfaceStruct surface = scratch.layoutSurface;

        layoutManager.sampleSurface(worldHandle, scratch.layouts, x, z, cellSizeBlocks * 0.5, surface);

        if (!surface.isFound())
            return;

        scratch.canopyHeights[index] = 0f;

        if (surface.getKind() == LayoutSurfaceKind.GROUND) {

            if (!scratch.openWater[index])
                scratch.topColors[index] = surface.getTopColor();

            return;
        }

        float raised = surface.getHeightBlocks() + EngineSetting.MACRO_SURFACE_OFFSET_BLOCKS
                - scratch.heightBlocks[index];

        if (raised <= 0f)
            return;

        scratch.structureHeights[index] = raised;
        scratch.structureTopColors[index] = surface.getTopColor();
        scratch.structureSideColors[index] = surface.getSideColor();
        scratch.structureCount++;
    }
}
