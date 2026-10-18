package application.bootstrap.geometrypipeline.meshmanager;

import engine.root.EngineSetting;
import engine.root.StructPackage;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class RiggedMeshScratchStruct extends StructPackage {

    /*
     * Reusable working storage for building a rigged mesh's vertex data: each
     * face's area-weighted normal, the position group every vertex falls in,
     * and the corners of each group laid out group by group, so the normals
     * smoothed across a shared position are summed without allocating. One
     * lives on MeshManager; every array is cleared and refilled per build,
     * and the position keys only ever grow.
     */

    // Normals
    final FloatArrayList faceNormals;

    // Position Groups
    final ObjectArrayList<Vector3> positionKeys;
    final Object2IntOpenHashMap<Vector3> position2Group;
    final IntArrayList vertexGroups;

    // Group Corners
    final IntArrayList groupStarts;
    final IntArrayList groupCursors;
    final IntArrayList groupFaces;

    // Textures
    float[] textureTiles;

    // Constructor \\

    RiggedMeshScratchStruct() {

        // Normals
        this.faceNormals = new FloatArrayList();

        // Position Groups
        this.positionKeys = new ObjectArrayList<>();
        this.position2Group = new Object2IntOpenHashMap<>();
        this.position2Group.defaultReturnValue(EngineSetting.INDEX_NOT_FOUND);
        this.vertexGroups = new IntArrayList();

        // Group Corners
        this.groupStarts = new IntArrayList();
        this.groupCursors = new IntArrayList();
        this.groupFaces = new IntArrayList();

        // Textures
        this.textureTiles = new float[0];
    }
}
