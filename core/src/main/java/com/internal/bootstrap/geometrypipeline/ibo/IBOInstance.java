package com.internal.bootstrap.geometrypipeline.ibo;

import com.internal.core.engine.InstancePackage;

public class IBOInstance extends InstancePackage {

    private IBOStruct iboStruct;

    public void constructor(IBOStruct iboStruct) {
        this.iboStruct = iboStruct;
    }

    public IBOStruct getIBOStruct() {
        return iboStruct;
    }
}