package editor.bootstrap.treeplannerpipeline.treeplanner;

import application.bootstrap.geometrypipeline.mesh.MeshInstance;
import application.bootstrap.worldpipeline.tree.TreeDetail;
import engine.root.StructPackage;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class TreePreviewTierStruct extends StructPackage {

    /*
     * One tier of the tree preview's row: the detail it draws its tree at,
     * the bark and leaf vertices a worker last built for it in blocks from
     * the centre of the tree's root, and the meshes the main thread uploaded
     * from them, with how many vertices they hold. Only one build runs at a
     * time, and the main thread uploads and empties what it built before the
     * next one starts.
     */

    // Detail
    private final TreeDetail detail;

    // Built
    private final FloatArrayList builtBark;
    private final FloatArrayList builtLeaves;

    // Meshes
    private final ObjectArrayList<MeshInstance> barkMeshes;
    private final ObjectArrayList<MeshInstance> leafMeshes;
    private int vertexCount;

    // Constructor \\

    public TreePreviewTierStruct(TreeDetail detail) {

        // Detail
        this.detail = detail;

        // Built
        this.builtBark = new FloatArrayList();
        this.builtLeaves = new FloatArrayList();

        // Meshes
        this.barkMeshes = new ObjectArrayList<>();
        this.leafMeshes = new ObjectArrayList<>();
    }

    // Accessible \\

    public TreeDetail getDetail() {
        return detail;
    }

    public FloatArrayList getBuiltBark() {
        return builtBark;
    }

    public FloatArrayList getBuiltLeaves() {
        return builtLeaves;
    }

    public ObjectArrayList<MeshInstance> getBarkMeshes() {
        return barkMeshes;
    }

    public ObjectArrayList<MeshInstance> getLeafMeshes() {
        return leafMeshes;
    }

    public int getVertexCount() {
        return vertexCount;
    }

    public void setVertexCount(int vertexCount) {
        this.vertexCount = vertexCount;
    }
}
