package com.internal.bootstrap.geometrypipeline;

import com.internal.bootstrap.geometrypipeline.compositebuffermanager.CompositeBufferManager;
import com.internal.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryManager;
import com.internal.bootstrap.geometrypipeline.ibomanager.IBOManager;
import com.internal.bootstrap.geometrypipeline.meshmanager.MeshManager;
import com.internal.bootstrap.geometrypipeline.modelmanager.ModelManager;
import com.internal.bootstrap.geometrypipeline.vaomanager.VAOManager;
import com.internal.bootstrap.geometrypipeline.vbomanager.VBOManager;
import com.internal.core.engine.PipelinePackage;

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