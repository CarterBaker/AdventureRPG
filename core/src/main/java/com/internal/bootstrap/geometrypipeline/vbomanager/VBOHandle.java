package com.internal.bootstrap.geometrypipeline.vbomanager;

import com.internal.core.engine.HandlePackage;

public class VBOHandle extends HandlePackage {

    private VBOStruct vboStruct;

    public void constructor(VBOStruct vboStruct) {
        this.vboStruct = vboStruct;
    }

    public VBOStruct getVBOStruct() {
        return vboStruct;
    }
}