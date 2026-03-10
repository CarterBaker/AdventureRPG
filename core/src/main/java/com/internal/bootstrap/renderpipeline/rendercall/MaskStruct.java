package com.internal.bootstrap.renderpipeline.rendercall;

import com.internal.core.engine.StructPackage;

public class MaskStruct extends StructPackage {

    public final int x;
    public final int y;
    public final int w;
    public final int h;

    public MaskStruct(int x, int y, int w, int h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }
}