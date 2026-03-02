package com.internal.bootstrap.geometrypipeline.vbo;

import com.internal.bootstrap.geometrypipeline.vbomanager.VBOStruct;
import com.internal.core.engine.InstancePackage;

public class VBOInstance extends InstancePackage {

    private VBOStruct vboStruct;

    public void constructor(VBOStruct vboStruct) {
        this.vboStruct = vboStruct;
    }

    public VBOStruct getVBOStruct() {
        return vboStruct;
    }
}