package com.internal.bootstrap.geometrypipeline.vaomanager;

import com.internal.core.engine.HandlePackage;

/*
 * Pure layout template. Carries no GPU object — VAOStruct.attributeHandle is 0.
 * Used exclusively as the source from which VAOInstances are created.
 */
public class VAOHandle extends HandlePackage {

    private VAOStruct vaoStruct;

    public void constructor(int[] attrSizes) {
        this.vaoStruct = new VAOStruct(0, attrSizes);
    }

    public VAOStruct getVAOStruct() {
        return vaoStruct;
    }
}