package com.internal.runtime.debug;

import com.internal.bootstrap.geometrypipeline.mesh.MeshHandle;
import com.internal.bootstrap.geometrypipeline.meshmanager.MeshManager;
import com.internal.bootstrap.geometrypipeline.model.ModelInstance;
import com.internal.bootstrap.renderpipeline.rendersystem.RenderSystem;
import com.internal.bootstrap.shaderpipeline.material.MaterialInstance;
import com.internal.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import com.internal.core.engine.PipelinePackage;

public class DebugItemPipeline extends PipelinePackage {

    private static final String MESH_PATH = "items/Apple";
    private static final String MATERIAL_PATH = "debug/DebugItem";
    private static final float ROTATION_SPEED = 1.0f;

    private MeshManager meshManager;
    private MaterialManager materialManager;
    private RenderSystem renderSystem;

    private ModelInstance debugModel;
    private MaterialInstance debugMaterial;
    private float rotation = 0f;

    @Override
    protected void get() {
        this.meshManager = get(MeshManager.class);
        this.materialManager = get(MaterialManager.class);
        this.renderSystem = get(RenderSystem.class);
    }

    @Override
    protected void awake() {

        int meshID = meshManager.getMeshHandleIDFromMeshName(MESH_PATH);
        MeshHandle meshHandle = meshManager.getMeshHandleFromMeshHandleID(meshID);

        int matID = materialManager.getMaterialIDFromMaterialName(MATERIAL_PATH);
        this.debugMaterial = materialManager.cloneMaterial(matID);

        this.debugModel = create(ModelInstance.class);
        this.debugModel.constructor(meshHandle.getMeshStruct(), debugMaterial);
    }

    @Override
    protected void update() {
        rotation += ROTATION_SPEED * internal.getDeltaTime();
        if (rotation > (float) (Math.PI * 2))
            rotation -= (float) (Math.PI * 2);

        debugMaterial.setUniform("u_rotation", rotation);
        // renderSystem.pushRenderCall(debugModel, 0);
    }
}