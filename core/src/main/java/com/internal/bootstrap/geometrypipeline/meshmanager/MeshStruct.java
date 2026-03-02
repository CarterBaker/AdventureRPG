package com.internal.bootstrap.geometrypipeline.meshmanager;

import com.internal.bootstrap.geometrypipeline.ibo.IBOStruct;
import com.internal.bootstrap.geometrypipeline.vao.VAOStruct;
import com.internal.bootstrap.geometrypipeline.vbo.VBOStruct;
import com.internal.core.engine.StructPackage;

public class MeshStruct extends StructPackage {

    public final VAOStruct vaoStruct;
    public final VBOStruct vboStruct;
    public final IBOStruct iboStruct;

    public MeshStruct(VAOStruct vaoStruct, VBOStruct vboStruct, IBOStruct iboStruct) {
        this.vaoStruct = vaoStruct;
        this.vboStruct = vboStruct;
        this.iboStruct = iboStruct;
    }
}