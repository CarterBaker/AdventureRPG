package com.AdventureRPG.bootstrap.shaderpipeline.passmanager;

import com.AdventureRPG.bootstrap.geometrypipeline.modelmanager.MeshHandle;
import com.AdventureRPG.bootstrap.geometrypipeline.modelmanager.ModelHandle;
import com.AdventureRPG.bootstrap.shaderpipeline.materialmanager.MaterialHandle;
import com.AdventureRPG.core.engine.HandlePackage;

public class PassHandle extends HandlePackage {

    // Internal
    private String passName;
    private int passID;

    private ModelHandle modelHandle;
    private MaterialHandle material;

    // Internal \\

    public void constructor(
            String passName,
            int passID,
            MaterialHandle material,
            MeshHandle processingTriangle) {

        // Internal
        this.passName = passName;
        this.passID = passID;

        this.modelHandle = create(ModelHandle.class);
        this.modelHandle.constructor(
                processingTriangle.getVaoHandle(),
                processingTriangle.getVertStride(),
                processingTriangle.getVboHandle(),
                processingTriangle.getVertCount(),
                processingTriangle.getIboHandle(),
                processingTriangle.getIndexCount(),
                material);
        this.material = material;
    }

    // Accessible \\

    public String getPassName() {
        return passName;
    }

    public int getPassID() {
        return passID;
    }

    public ModelHandle getModelHandle() {
        return modelHandle;
    }

    public MaterialHandle getMaterial() {
        return material;
    }
}
