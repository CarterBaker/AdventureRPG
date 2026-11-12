package editor.bootstrap.charactereditorpipeline.charactermesh;

import engine.root.EngineSetting;
import engine.root.StructPackage;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;

public class CharacterPaintStampStruct extends StructPackage {

    /*
     * One stamp of a paint stroke as the viewport sees it: the brush's
     * centre and radius in window pixels, and every face of the part the
     * brush may cover that the camera can see, with its four corners where
     * the window shows them, two floats each. The painter lays the brush
     * over every texel of those faces through them, so a stroke paints
     * exactly what the screen shows under the brush, across UV seams alike.
     * Its lists are cleared and refilled for every stamp, never shrunk.
     */

    // Brush
    private float centerX;
    private float centerY;
    private float radius;

    // Faces — each face's index, and its corners' window pixels, eight floats per face
    private final IntArrayList faces;
    private final FloatArrayList corners;

    // Constructor \\

    public CharacterPaintStampStruct() {
        this.faces = new IntArrayList();
        this.corners = new FloatArrayList();
    }

    // Management \\

    public void begin(float centerX, float centerY, float radius) {

        this.centerX = centerX;
        this.centerY = centerY;
        this.radius = radius;

        faces.clear();
        corners.clear();
    }

    public void addFace(int faceIndex) {
        faces.add(faceIndex);
    }

    public void addCorner(float x, float y) {
        corners.add(x);
        corners.add(y);
    }

    // Accessible \\

    public float getCenterX() {
        return centerX;
    }

    public float getCenterY() {
        return centerY;
    }

    public float getRadius() {
        return radius;
    }

    public int getFaceCount() {
        return faces.size();
    }

    public int getFace(int index) {
        return faces.getInt(index);
    }

    public float getCornerX(int index, int corner) {
        return corners.getFloat((index * EngineSetting.QUAD_VERTEX_COUNT + corner) * 2);
    }

    public float getCornerY(int index, int corner) {
        return corners.getFloat((index * EngineSetting.QUAD_VERTEX_COUNT + corner) * 2 + 1);
    }

    public boolean isEmpty() {
        return faces.isEmpty();
    }
}
