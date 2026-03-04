package com.internal.bootstrap.geometrypipeline.ibo;

import com.internal.core.engine.HandlePackage;

public class IBOHandle extends HandlePackage {

    private IBOStruct iboStruct;

    public void constructor(IBOStruct iboStruct) {
        this.iboStruct = iboStruct;
    }

    public IBOStruct getIBOStruct() {
        return iboStruct;
    }
}