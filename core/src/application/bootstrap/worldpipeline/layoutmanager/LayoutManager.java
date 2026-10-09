package application.bootstrap.worldpipeline.layoutmanager;

import application.bootstrap.worldpipeline.layout.LayoutDraftStruct;
import application.bootstrap.worldpipeline.layout.LayoutLotStruct;
import application.bootstrap.worldpipeline.layout.LayoutPlanStruct;
import application.bootstrap.worldpipeline.layout.LayoutSurfaceStruct;
import application.bootstrap.worldpipeline.structure.StructureHandle;
import application.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import application.bootstrap.worldpipeline.world.WorldHandle;
import engine.root.ManagerPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class LayoutManager extends ManagerPackage {

    /*
     * The one home of pathed layouts, the arrangement of roads, walls and
     * structures on lots that settlements, the roads between them, and any
     * later pathed content such as dungeons are planned as. A planner grows
     * a LayoutDraftStruct, fitting each lot through here so lots always face
     * and keep clear of their roads alike, and seals it into an immutable
     * LayoutPlanStruct. From then on the layout is laid into chunks, sampled
     * for distant terrain and the map, and asked what ground it claims only
     * through here, whatever planned it.
     */

    // Internal
    private LayoutLotBranch layoutLotBranch;
    private LayoutStampBranch layoutStampBranch;
    private LayoutSurfaceBranch layoutSurfaceBranch;

    // Base \\

    @Override
    protected void create() {
        this.layoutLotBranch = create(LayoutLotBranch.class);
        this.layoutStampBranch = create(LayoutStampBranch.class);
        this.layoutSurfaceBranch = create(LayoutSurfaceBranch.class);
    }

    // Draft \\

    // Worker — a structure beside a road point on one side of it, its front turned to face the road; null when it
    // cannot stand there
    public LayoutLotStruct placeLotFacing(
            LayoutDraftStruct draft,
            StructureHandle structureHandle,
            double pointX,
            double pointZ,
            float roadY,
            double headingX,
            double headingZ,
            int side,
            double frontDistance) {
        return layoutLotBranch.placeFacing(
                draft, structureHandle, pointX, pointZ, roadY, headingX, headingZ, side, frontDistance);
    }

    // Worker — a structure standing over a road point, running along it; null when it cannot stand there
    public LayoutLotStruct placeLotSpanning(
            LayoutDraftStruct draft,
            StructureHandle structureHandle,
            double pointX,
            double pointZ,
            float roadY,
            double headingX,
            double headingZ) {
        return layoutLotBranch.placeSpanning(draft, structureHandle, pointX, pointZ, roadY, headingX, headingZ);
    }

    // Worker — a structure stood exactly where asked, kept only clear of other lots; null when one is in the way
    public LayoutLotStruct placeLotAt(
            LayoutDraftStruct draft,
            StructureHandle structureHandle,
            int quarterTurns,
            long planX,
            long planZ,
            float floorY) {
        return layoutLotBranch.placeAt(draft, structureHandle, quarterTurns, planX, planZ, floorY);
    }

    // Worker — the ground's top block at a column of a layout, LAYOUT_GROUND_FLOODED where water stands over it
    public int probeGround(WorldHandle worldHandle, double planX, double planZ) {
        return layoutLotBranch.probeGround(worldHandle, planX, planZ);
    }

    public LayoutPlanStruct seal(LayoutDraftStruct draft) {
        return layoutLotBranch.seal(draft);
    }

    // Stamp \\

    // Worker — a layout's share of a chunk
    public void stampLayout(
            WorldHandle worldHandle,
            long chunkCoordinate,
            SubChunkInstance[] subChunks,
            LayoutPlanStruct plan) {
        layoutStampBranch.stamp(worldHandle, chunkCoordinate, subChunks, plan);
    }

    // Surface \\

    // Worker — what the layouts show at a world point seen from afar, everything widened by a tolerance
    public void sampleSurface(
            WorldHandle worldHandle,
            ObjectArrayList<LayoutPlanStruct> plans,
            double worldX,
            double worldZ,
            double toleranceBlocks,
            LayoutSurfaceStruct out) {
        layoutSurfaceBranch.sampleSurface(worldHandle, plans, worldX, worldZ, toleranceBlocks, out);
    }

    // Worker — true when any of the layouts claims ground within a margin of a world point
    public boolean isClaimed(
            WorldHandle worldHandle,
            ObjectArrayList<LayoutPlanStruct> plans,
            double worldX,
            double worldZ,
            double marginBlocks) {
        return layoutSurfaceBranch.isClaimed(worldHandle, plans, worldX, worldZ, marginBlocks);
    }
}
