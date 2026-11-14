package application.bootstrap.geometrypipeline.meshmanager;

import application.bootstrap.geometrypipeline.mesh.RiggedMeshStruct;
import engine.root.StructPackage;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.floats.FloatArrayList;

class QuadExpansionStruct extends StructPackage {

    /*
     * Scratch result of one VBO expansion pass — quad entries or sub-voxel
     * cubes. Holds the assembled
     * vertex and index arrays before they are uploaded to the GPU, along with
     * the raw min/max vertex position bounds read directly off the assembled
     * position floats — this mesh's own unscaled extent in its authored
     * bind-pose model space. Created once per mesh file that contains quad
     * entries or a sub-voxel block. A rigged expansion also keeps its parsed
     * mesh and morph entries, for the morph texture its handle receives.
     */

    // Internal
    final float[] vertices;
    final short[] indices;
    final Vector3 boundsMin;
    final Vector3 boundsMax;

    // Rigged
    RiggedMeshStruct riggedMesh;
    FloatArrayList morphEntries;

    // Constructor \\

    QuadExpansionStruct(float[] vertices, short[] indices, Vector3 boundsMin, Vector3 boundsMax) {
        this.vertices = vertices;
        this.indices = indices;
        this.boundsMin = boundsMin;
        this.boundsMax = boundsMax;
    }
}