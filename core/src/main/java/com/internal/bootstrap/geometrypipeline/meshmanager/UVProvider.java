package com.internal.bootstrap.geometrypipeline.meshmanager;

@FunctionalInterface
public interface UVProvider {

    // Resolve \\

    // Returns [u0, v0, u1, v1] for the named texture tile.
    // Implementations must throw on unknown names.
    float[] getUVs(String textureName);
}