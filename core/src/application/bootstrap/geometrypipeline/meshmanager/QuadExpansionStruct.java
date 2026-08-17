package application.bootstrap.geometrypipeline.meshmanager;

import engine.root.StructPackage;
import engine.util.mathematics.vectors.Vector3;

class QuadExpansionStruct extends StructPackage {

    /*
     * Scratch result of one quad VBO expansion pass. Holds the assembled
     * vertex and index arrays before they are uploaded to the GPU, along with
     * the raw min/max vertex position bounds read directly off the assembled
     * position floats — this mesh's own unscaled extent in its authored
     * bind-pose model space. Created once per mesh file that contains quad
     * entries.
     */

    // Internal
    final float[] vertices;
    final short[] indices;
    final Vector3 boundsMin;
    final Vector3 boundsMax;

    // Constructor \\

    QuadExpansionStruct(float[] vertices, short[] indices, Vector3 boundsMin, Vector3 boundsMax) {
        this.vertices = vertices;
        this.indices = indices;
        this.boundsMin = boundsMin;
        this.boundsMax = boundsMax;
    }
}