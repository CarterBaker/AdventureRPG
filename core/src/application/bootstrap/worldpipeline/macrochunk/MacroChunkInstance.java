package application.bootstrap.worldpipeline.macrochunk;

import application.bootstrap.geometrypipeline.mesh.MeshInstance;
import application.bootstrap.geometrypipeline.model.ModelInstance;
import application.bootstrap.shaderpipeline.ubo.UBOInstance;
import application.bootstrap.worldpipeline.world.WorldHandle;
import engine.root.InstancePackage;

public class MacroChunkInstance extends InstancePackage {

    /*
     * One distant, world-aligned tile of MACRO_CHUNK_SIZE² chunks drawn as a
     * single coarse heightfield mesh. Pooled by MacroQueueManager. Geometry
     * waits in the sync container between build and upload, while the mesh,
     * model and position UBO are main-thread only and survive pooling, so a
     * reused macro reuploads into the buffers it already owns.
     */

    // Internal
    private MacroDataSyncContainer macroDataSyncContainer;
    private WorldHandle worldHandle;
    private long coordinate;

    // GPU
    private MeshInstance meshInstance;
    private ModelInstance modelInstance;
    private UBOInstance positionUBO;
    private boolean rendered;

    // Placement
    private float angleFromCenter;
    private float angularRadius;

    // Internal \\

    @Override
    protected void create() {
        this.macroDataSyncContainer = create(MacroDataSyncContainer.class);
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

    public void setRendered(boolean rendered) {
        this.rendered = rendered;
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

    public boolean isRendered() {
        return rendered;
    }

    public float getAngleFromCenter() {
        return angleFromCenter;
    }

    public float getAngularRadius() {
        return angularRadius;
    }
}
