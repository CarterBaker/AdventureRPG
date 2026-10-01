package application.bootstrap.worldpipeline.macrochunk;

import application.bootstrap.geometrypipeline.mesh.MeshInstance;
import application.bootstrap.geometrypipeline.model.ModelInstance;
import application.bootstrap.shaderpipeline.ubo.UBOInstance;
import application.bootstrap.worldpipeline.world.WorldHandle;
import engine.root.EngineSetting;
import engine.root.InstancePackage;

public class MacroChunkInstance extends InstancePackage {

    /*
     * One distant, world-aligned tile of MACRO_CHUNK_SIZE² chunks drawn as a
     * single coarse heightfield mesh. Pooled by MacroQueueManager. The ring
     * sets the lattice resolution the tile should be built at and the upload
     * records the one it was built at, so a tile whose resolution band moves
     * is rebuilt while its old mesh keeps drawing. Geometry waits in the sync
     * container between build and upload, while the mesh, model, position and
     * coverage UBOs are main-thread only and survive pooling, so a reused
     * macro reuploads into the buffers it already owns. The coverage words mark
     * every chunk of the tile the grid currently draws, together with the
     * anchor and drawn revision they were resolved against.
     */

    // Internal
    private MacroDataSyncContainer macroDataSyncContainer;
    private WorldHandle worldHandle;
    private long coordinate;

    // GPU
    private MeshInstance meshInstance;
    private ModelInstance modelInstance;
    private UBOInstance positionUBO;
    private UBOInstance coverageUBO;
    private boolean rendered;
    private boolean hasGeometry;

    // Target
    private int targetCellsPerSide;

    // Built
    private int builtCellsPerSide;

    // Coverage
    private int[] coverageWords;
    private boolean coverageCurrent;
    private long coverageAnchorCoordinate;
    private int coverageRevision;

    // Placement
    private float angleFromCenter;
    private float angularRadius;

    // Internal \\

    @Override
    protected void create() {

        this.macroDataSyncContainer = create(MacroDataSyncContainer.class);
        this.coverageWords = new int[EngineSetting.MACRO_COVERAGE_WORD_COUNT];
    }

    // Constructor \\

    public void constructor(WorldHandle worldHandle, long coordinate) {

        this.worldHandle = worldHandle;
        this.coordinate = coordinate;

        reset();
    }

    // Reset \\

    public void reset() {
        macroDataSyncContainer.resetData();
        this.rendered = false;
        this.hasGeometry = false;
        this.builtCellsPerSide = 0;
        this.coverageCurrent = false;
    }

    // GPU \\

    public void setModel(MeshInstance meshInstance, ModelInstance modelInstance) {
        this.meshInstance = meshInstance;
        this.modelInstance = modelInstance;
    }

    public void clearModel() {
        this.meshInstance = null;
        this.modelInstance = null;
    }

    public void setPositionUBO(UBOInstance positionUBO) {
        this.positionUBO = positionUBO;
    }

    public void setCoverageUBO(UBOInstance coverageUBO) {
        this.coverageUBO = coverageUBO;
    }

    public void setRendered(boolean rendered) {
        this.rendered = rendered;
    }

    public void setHasGeometry(boolean hasGeometry) {
        this.hasGeometry = hasGeometry;
    }

    // Target \\

    public void setTarget(int cellsPerSide) {
        this.targetCellsPerSide = cellsPerSide;
    }

    public boolean needsRebuild() {
        return builtCellsPerSide != targetCellsPerSide;
    }

    // Built \\

    public void setBuilt(int cellsPerSide) {
        this.builtCellsPerSide = cellsPerSide;
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

    // Placement \\

    public void setPlacement(float angleFromCenter, float angularRadius) {
        this.angleFromCenter = angleFromCenter;
        this.angularRadius = angularRadius;
    }

    // Accessible \\

    public MacroDataSyncContainer getMacroDataSyncContainer() {
        return macroDataSyncContainer;
    }

    public WorldHandle getWorldHandle() {
        return worldHandle;
    }

    public long getCoordinate() {
        return coordinate;
    }

    public MeshInstance getMeshInstance() {
        return meshInstance;
    }

    public ModelInstance getModelInstance() {
        return modelInstance;
    }

    public UBOInstance getPositionUBO() {
        return positionUBO;
    }

    public UBOInstance getCoverageUBO() {
        return coverageUBO;
    }

    public int[] getCoverageWords() {
        return coverageWords;
    }

    public boolean isRendered() {
        return rendered;
    }

    public boolean hasGeometry() {
        return hasGeometry;
    }

    public int getTargetCellsPerSide() {
        return targetCellsPerSide;
    }

    public float getAngleFromCenter() {
        return angleFromCenter;
    }

    public float getAngularRadius() {
        return angularRadius;
    }
}
