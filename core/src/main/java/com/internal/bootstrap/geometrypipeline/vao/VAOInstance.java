package com.internal.bootstrap.geometrypipeline.vao;

import com.internal.bootstrap.geometrypipeline.vaomanager.VAOStruct;
import com.internal.core.engine.InstancePackage;

public class VAOInstance extends InstancePackage {

    private VAOStruct vaoStruct;

    public void constructor(VAOStruct vaoStruct) {
        this.vaoStruct = vaoStruct;
    }

    public VAOStruct getVAOStruct() {
        return vaoStruct;
    }
}