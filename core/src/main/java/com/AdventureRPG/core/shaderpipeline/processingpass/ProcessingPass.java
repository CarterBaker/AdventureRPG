package com.AdventureRPG.core.shaderpipeline.processingpass;

import com.AdventureRPG.core.geometrypipeline.Mesh.MeshHandle;
import com.AdventureRPG.core.geometrypipeline.Models.ModelHandle;
import com.AdventureRPG.core.shaderpipeline.materials.Material;

public class ProcessingPass {

    // Internal
    public final String passName;
    public final int passID;

    public final ModelHandle modelHandle;
    public final Material material;

    public ProcessingPass(
            String passName,
            int passID,
            Material material,
            MeshHandle processingTriangle) {

        // Internal
        this.passName = passName;
        this.passID = passID;

        this.modelHandle = new ModelHandle(
                processingTriangle.vao,
                processingTriangle.vertStride,
                processingTriangle.vbo,
                processingTriangle.vertCount,
                processingTriangle.ibo,
                processingTriangle.indexCount,
                material);
        this.material = material;
    }
}
