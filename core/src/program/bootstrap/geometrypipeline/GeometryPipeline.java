package program.bootstrap.geometrypipeline;

import program.bootstrap.geometrypipeline.compositebuffermanager.CompositeBufferManager;
import program.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryManager;
import program.bootstrap.geometrypipeline.ibomanager.IBOManager;
import program.bootstrap.geometrypipeline.meshmanager.MeshManager;
import program.bootstrap.geometrypipeline.modelmanager.ModelManager;
import program.bootstrap.geometrypipeline.vaomanager.VAOManager;
import program.bootstrap.geometrypipeline.vbomanager.VBOManager;
import program.core.engine.PipelinePackage;

public class GeometryPipeline extends PipelinePackage {

    /*
     * Registers all geometry pipeline managers in dependency order. VAO,
     * VBO, and IBO managers are registered before MeshManager since mesh
     * assembly depends on all three buffer systems being available.
     */

    @Override
    protected void create() {
        create(VBOManager.class);
        create(IBOManager.class);
        create(VAOManager.class);
        create(MeshManager.class);
        create(ModelManager.class);
        create(DynamicGeometryManager.class);
        create(CompositeBufferManager.class);
    }
}