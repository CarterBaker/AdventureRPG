package com.internal.bootstrap.geometrypipeline.meshmanager;

import com.internal.core.engine.StructPackage;

class QuadExpansionStruct extends StructPackage {

    /*
     * Scratch result of one quad VBO expansion pass. Holds the assembled
     * vertex and index arrays before they are uploaded to the GPU.
     * Created once per mesh file that contains quad entries.
     */

    // Internal
    final float[] vertices;
    final short[] indices;

    // Constructor \\

    QuadExpansionStruct(float[] vertices, short[] indices) {
        this.vertices = vertices;
        this.indices = indices;
    }
}