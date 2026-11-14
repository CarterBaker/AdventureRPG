package application.bootstrap.worldpipeline.macrochunk;

import application.bootstrap.geometrypipeline.mesh.MeshInstance;
import application.bootstrap.geometrypipeline.model.ModelInstance;
import engine.root.EngineSetting;
import engine.root.InstancePackage;
import engine.util.mathematics.vectors.Vector2;

public class MacroWaterInstance extends InstancePackage {

    /*
     * One grid's distant sea: a single flat plane around the active chunk that
     * the water shader lifts to the live tide, and the two textures that say
     * where it may show. The open water mask is a ring of tile patches, one
     * per macro tile, addressed by the tile's position modulo the ring, so a
     * tile keeps its patch however far the grid walks. The ring origin is the
     * active chunk carried forward by every step it takes, never by the
     * world's wrap, and held modulo the ring's span, so the addressing never
     * jumps. Each patch remembers the macro that wrote it, so a tile leaving
     * the ring never clears a patch a newer tile already took. The coverage
     * texture marks every chunk the grid draws itself, together with the
     * anchor and drawn revision it was resolved against, and the plane leaves
     * out the cells the grid always draws for the render distance it was
     * built for.
     */

    // GPU
    private MeshInstance meshInstance;
    private ModelInstance modelInstance;
    private int maskTexture;
    private int coverageTexture;
    private int planeRenderDistance;

    // Ring
    private MacroChunkInstance[] slotOwners;
    private boolean anchored;
    private long lastActiveChunkCoordinate;
    private int originChunkX;
    private int originChunkZ;
    private Vector2 anchorUniform;

    // Coverage
    private boolean coverageCurrent;
    private long coverageAnchorCoordinate;
    private int coverageRevision;

    // Internal \\

    @Override
    protected void create() {
        this.slotOwners = new MacroChunkInstance[EngineSetting.MACRO_WATER_MASK_TILES
                * EngineSetting.MACRO_WATER_MASK_TILES];
        this.anchorUniform = new Vector2();
    }

    // Constructor \\

    public void constructor(
            MeshInstance meshInstance,
            ModelInstance modelInstance,
            int maskTexture,
            int coverageTexture) {

        this.meshInstance = meshInstance;
        this.modelInstance = modelInstance;
        this.maskTexture = maskTexture;
        this.coverageTexture = coverageTexture;
        this.anchored = false;
        this.coverageCurrent = false;
    }

    // Plane \\

    public int getPlaneRenderDistance() {
        return planeRenderDistance;
    }

    public void setPlaneRenderDistance(int planeRenderDistance) {
        this.planeRenderDistance = planeRenderDistance;
    }

    // Ring \\

    public boolean isAnchored() {
        return anchored;
    }

    public void anchor(long activeChunkCoordinate, int originChunkX, int originChunkZ) {
        this.anchored = true;
        this.lastActiveChunkCoordinate = activeChunkCoordinate;
        this.originChunkX = originChunkX;
        this.originChunkZ = originChunkZ;
    }

    public long getLastActiveChunkCoordinate() {
        return lastActiveChunkCoordinate;
    }

    public int getOriginChunkX() {
        return originChunkX;
    }

    public int getOriginChunkZ() {
        return originChunkZ;
    }

    public Vector2 getAnchorUniform() {
        return anchorUniform.set(originChunkX, originChunkZ);
    }

    public MacroChunkInstance getSlotOwner(int slot) {
        return slotOwners[slot];
    }

    public void setSlotOwner(int slot, MacroChunkInstance macro) {
        slotOwners[slot] = macro;
    }

    // Coverage \\

    public boolean isCoverageCurrent(long anchorCoordinate, int drawnRevision) {
        return coverageCurrent && coverageAnchorCoordinate == anchorCoordinate && coverageRevision == drawnRevision;
    }

    public void setCoverageCurrent(long anchorCoordinate, int drawnRevision) {
        this.coverageCurrent = true;
        this.coverageAnchorCoordinate = anchorCoordinate;
        this.coverageRevision = drawnRevision;
    }

    // Accessible \\

    public MeshInstance getMeshInstance() {
        return meshInstance;
    }

    public ModelInstance getModelInstance() {
        return modelInstance;
    }

    public int getMaskTexture() {
        return maskTexture;
    }

    public int getCoverageTexture() {
        return coverageTexture;
    }
}
