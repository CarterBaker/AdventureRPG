package application.bootstrap.geometrypipeline;

import application.bootstrap.geometrypipeline.compositebuffermanager.CompositeBufferManager;
import application.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryManager;
import application.bootstrap.geometrypipeline.ibomanager.IBOManager;
import application.bootstrap.geometrypipeline.meshmanager.MeshManager;
import application.bootstrap.geometrypipeline.modelmanager.ModelManager;
import application.bootstrap.geometrypipeline.rigmanager.RigManager;
import application.bootstrap.geometrypipeline.vaomanager.VAOManager;
import application.bootstrap.geometrypipeline.vbomanager.VBOManager;
import engine.root.PipelinePackage;

public class GeometryPipeline extends PipelinePackage {

    /*
     * Registers all geometry pipeline managers in dependency order. VAO,
     * VBO, and IBO managers are registered before MeshManager since mesh
     * assembly depends on all three buffer systems being available.
     * RigManager is registered before MeshManager since rig-declaring
     * meshes resolve bone names against it during quad expansion.
     */

    @Override
    protected void create() {
        create(VBOManager.class);
        create(IBOManager.class);
        create(VAOManager.class);
        create(RigManager.class);
        create(MeshManager.class);
        create(ModelManager.class);
        create(DynamicGeometryManager.class);
        create(CompositeBufferManager.class);
    }
}