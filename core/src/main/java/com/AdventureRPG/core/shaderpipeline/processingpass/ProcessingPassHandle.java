package com.AdventureRPG.core.shaderpipeline.processingpass;

import com.AdventureRPG.core.engine.HandlePackage;
import com.AdventureRPG.core.geometrypipeline.Models.ModelHandle;
import com.AdventureRPG.core.geometrypipeline.mesh.MeshHandle;
import com.AdventureRPG.core.shaderpipeline.materials.Material;

public class ProcessingPassHandle extends HandlePackage {

    // Internal
    private String passName;
    private int passID;

    private ModelHandle modelHandle;
    private Material material;

    // Internal \\

    public void constructor(
            String passName,
            int passID,
            Material material,
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

    public Material getMaterial() {
        return material;
    }
}
