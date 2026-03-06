package com.internal.bootstrap.shaderpipeline.Texture;

import com.internal.core.engine.HandlePackage;

/*
 * Normalised UV region within a single atlas layer. Owned by TextureManager
 * and retrieved at runtime by geometry systems for UV coordinate lookup.
 * (u0, v0) is bottom-left; (u1, v1) is top-right.
 */
public class UVHandle extends HandlePackage {

    public float u0, v0;
    public float u1, v1;

    public void constructor(float u0, float v0, float u1, float v1) {
        this.u0 = u0;
        this.v0 = v0;
        this.u1 = u1;
        this.v1 = v1;
    }
}