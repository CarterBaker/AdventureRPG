package com.internal.bootstrap.geometrypipeline.vao;

import com.internal.core.engine.HandlePackage;

public class VAOHandle extends HandlePackage {

    /*
     * Pure layout template. Carries no GPU object — VAOData.attributeHandle is 0.
     * Used exclusively as the source from which VAOInstances are created.
     * Owned by VAOManager for the engine lifetime. The templateID is assigned
     * during registration and used for window-specific VAO instance tracking.
     */

    // Internal
    private VAOData vaoData;
    private short templateID;

    // Constructor \\

    public void constructor(int[] attrSizes) {

        // Internal
        this.vaoData = new VAOData(0, attrSizes);
    }

    // Accessible \\

    public VAOData getVAOData() {
        return vaoData;
    }

    public short getTemplateID() {
        return templateID;
    }

    public void setTemplateID(short templateID) {
        this.templateID = templateID;
    }
}