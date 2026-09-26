package application.bootstrap.geometrypipeline;

import application.bootstrap.geometrypipeline.compositebuffermanager.CompositeBufferManager;
import application.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryManager;
import application.bootstrap.geometrypipeline.ibomanager.IBOManager;
import application.bootstrap.geometrypipeline.meshmanager.MeshManager;
import application.bootstrap.geometrypipeline.modelmanager.ModelManager;
import application.bootstrap.geometrypipeline.rigmanager.RigManager;
import application.bootstrap.geometrypipeline.skinnedbuffermanager.SkinnedBufferManager;
import application.bootstrap.geometrypipeline.subvoxelmanager.SubVoxelManager;
import application.bootstrap.geometrypipeline.vaomanager.VAOManager;
import application.bootstrap.geometrypipeline.vbomanager.VBOManager;
import engine.root.PipelinePackage;

public class GeometryPipeline extends PipelinePackage {

    /*
     * Registers the geometry managers in dependency order: VAO, VBO and IBO
     * before MeshManager, RigManager before MeshManager for rigged meshes, then
     * SkinnedBufferManager and SubVoxelManager, which only build on demand.
     */

    @Override
    protected void create() {
        create(VBOManager.class);
        create(IBOManager.class);
        create(VAOManager.class);
        create(RigManager.class);
        create(MeshManager.class);
        create(SubVoxelManager.class);
        create(ModelManager.class);
        create(DynamicGeometryManager.class);
        create(CompositeBufferManager.class);
        create(SkinnedBufferManager.class);
    }
}