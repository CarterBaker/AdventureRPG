package editor.bootstrap.charactereditorpipeline.charactermesh;

import engine.root.StructPackage;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.floats.FloatArrayList;

public class CharacterStrandStruct extends StructPackage {

    /*
     * One strand of a groom, a guide curve as Blender's hair curves and
     * Maya's XGen guides are: its points from the root on the surface to the
     * tip, three floats each, the surface's normal at the root its card lies
     * flat against, the bone the root grows from, the texture its card is
     * painted with, its card's width at the root in blocks and the share of
     * that width the tip narrows to. Whether it is selected rides with it.
     */

    // Curve
    private final FloatArrayList points;
    private final Vector3 normal;

    // Card
    private String boneName;
    private String textureName;
    private float width;
    private float taper;

    // Selection
    private boolean selected;

    // Constructor \\

    public CharacterStrandStruct(Vector3 normal, String boneName, String textureName, float width, float taper) {

        // Curve
        this.points = new FloatArrayList();
        this.normal = new Vector3(normal);

        // Card
        this.boneName = boneName;
        this.textureName = textureName;
        this.width = width;
        this.taper = taper;
    }

    public CharacterStrandStruct(CharacterStrandStruct source) {

        this(source.normal, source.boneName, source.textureName, source.width, source.taper);
        points.addAll(source.points);
        this.selected = source.selected;
    }

    // Points \\

    public void addPoint(float x, float y, float z) {
        points.add(x);
        points.add(y);
        points.add(z);
    }

    public void setPoint(int pointIndex, float x, float y, float z) {

        int base = pointIndex * 3;

        points.set(base, x);
        points.set(base + 1, y);
        points.set(base + 2, z);
    }

    // Keeps the first points given, dropping the rest toward the tip
    public void keepPoints(int count) {
        points.size(count * 3);
    }

    public int getPointCount() {
        return points.size() / 3;
    }

    public float getX(int pointIndex) {
        return points.getFloat(pointIndex * 3);
    }

    public float getY(int pointIndex) {
        return points.getFloat(pointIndex * 3 + 1);
    }

    public float getZ(int pointIndex) {
        return points.getFloat(pointIndex * 3 + 2);
    }

    public void getPoint(int pointIndex, Vector3 out) {
        out.set(getX(pointIndex), getY(pointIndex), getZ(pointIndex));
    }

    // The distance from one point to the next
    public float getSegmentLength(int pointIndex) {

        float dx = getX(pointIndex + 1) - getX(pointIndex);
        float dy = getY(pointIndex + 1) - getY(pointIndex);
        float dz = getZ(pointIndex + 1) - getZ(pointIndex);

        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    // The strand's length along its points
    public float getLength() {

        float length = 0f;

        for (int i = 0; i + 1 < getPointCount(); i++)
            length += getSegmentLength(i);

        return length;
    }

    public FloatArrayList getPoints() {
        return points;
    }

    // Accessible \\

    public Vector3 getNormal() {
        return normal;
    }

    public String getBoneName() {
        return boneName;
    }

    public void setBoneName(String boneName) {
        this.boneName = boneName;
    }

    public String getTextureName() {
        return textureName;
    }

    public void setTextureName(String textureName) {
        this.textureName = textureName;
    }

    public float getWidth() {
        return width;
    }

    public void setWidth(float width) {
        this.width = width;
    }

    public float getTaper() {
        return taper;
    }

    public void setTaper(float taper) {
        this.taper = taper;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}
