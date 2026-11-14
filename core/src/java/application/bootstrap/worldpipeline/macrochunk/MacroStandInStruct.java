package application.bootstrap.worldpipeline.macrochunk;

import engine.root.EngineSetting;
import engine.root.StructPackage;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;

public class MacroStandInStruct extends StructPackage {

    /*
     * Tree stand-ins of one macro tile waiting to be laid into its mesh: the
     * vertices in the macro layout, the triangles indexing them from the
     * first vertex of the shape they belong to, and each shape's vertex and
     * index count, so the mesh can stop between whole shapes once it can
     * index no more. Kept warm between builds.
     */

    // Vertices — MACRO_VERTEX_FLOAT_COUNT apiece
    private final FloatArrayList vertices;

    // Triangles — indices from their shape's first vertex
    private final ShortArrayList indices;

    // Shapes — vertex count then index count apiece
    private final IntArrayList shapes;
    private int shapeVertexStart;
    private int shapeIndexStart;

    // Constructor \\

    public MacroStandInStruct() {
        this.vertices = new FloatArrayList();
        this.indices = new ShortArrayList();
        this.shapes = new IntArrayList();
    }

    // Management \\

    public void beginShape() {
        this.shapeVertexStart = getVertexCount();
        this.shapeIndexStart = indices.size();
    }

    // A vertex of the open shape — its index within the shape
    public int addVertex(float x, float y, float z, int topColor, int sideColor, float cover) {

        vertices.add(x);
        vertices.add(y);
        vertices.add(z);
        vertices.add(topColor);
        vertices.add(sideColor);
        vertices.add(cover);

        return getVertexCount() - 1 - shapeVertexStart;
    }

    public void addTriangle(int first, int second, int third) {
        indices.add((short) first);
        indices.add((short) second);
        indices.add((short) third);
    }

    public void endShape() {
        shapes.add(getVertexCount() - shapeVertexStart);
        shapes.add(indices.size() - shapeIndexStart);
    }

    public void clear() {
        vertices.clear();
        indices.clear();
        shapes.clear();
        this.shapeVertexStart = 0;
        this.shapeIndexStart = 0;
    }

    // Accessible \\

    public FloatArrayList getVertices() {
        return vertices;
    }

    public ShortArrayList getIndices() {
        return indices;
    }

    public int getVertexCount() {
        return vertices.size() / EngineSetting.MACRO_VERTEX_FLOAT_COUNT;
    }

    public int getShapeCount() {
        return shapes.size() / 2;
    }

    public int getShapeVertexCount(int shape) {
        return shapes.getInt(shape * 2);
    }

    public int getShapeIndexCount(int shape) {
        return shapes.getInt(shape * 2 + 1);
    }
}
