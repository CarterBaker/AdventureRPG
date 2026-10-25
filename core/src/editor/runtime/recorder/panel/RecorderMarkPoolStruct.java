package editor.runtime.recorder.panel;

import application.bootstrap.menupipeline.element.ElementInstance;
import engine.root.StructPackage;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class RecorderMarkPoolStruct extends StructPackage {

    /*
     * The marks of one kind the Keys graph draws — the stretches of its line,
     * its keys, the handles of the key selected — made once from their
     * template into the graph's layer for them and kept, so drawing the graph
     * again only places them. Each remembers where it was last placed, so one
     * that has not moved is left as it stands, and those a drawing no longer
     * needs are set aside.
     */

    // Pool
    private final String template;
    private final ElementInstance container;
    private final ObjectArrayList<ElementInstance> marks;
    private int used;
    private int shown;

    // Placement
    private final FloatArrayList placedX;
    private final FloatArrayList placedY;
    private final FloatArrayList placedLength;
    private final FloatArrayList placedDegrees;

    // Constructor \\

    public RecorderMarkPoolStruct(String template, ElementInstance container) {

        // Pool
        this.template = template;
        this.container = container;
        this.marks = new ObjectArrayList<>();

        // Placement
        this.placedX = new FloatArrayList();
        this.placedY = new FloatArrayList();
        this.placedLength = new FloatArrayList();
        this.placedDegrees = new FloatArrayList();
    }

    // Management \\

    // A drawing begins with no mark used
    public void begin() {
        this.used = 0;
    }

    public void add(ElementInstance mark) {
        marks.add(mark);
        placedX.add(Float.NaN);
        placedY.add(Float.NaN);
        placedLength.add(Float.NaN);
        placedDegrees.add(Float.NaN);
    }

    // The next mark the drawing uses, by its place in the pool
    public int next() {
        return used++;
    }

    // True when the mark already stands there, else it is remembered as standing there now
    public boolean isPlaced(int index, float x, float y, float length, float degrees) {

        if (placedX.getFloat(index) == x && placedY.getFloat(index) == y
                && placedLength.getFloat(index) == length && placedDegrees.getFloat(index) == degrees)
            return true;

        placedX.set(index, x);
        placedY.set(index, y);
        placedLength.set(index, length);
        placedDegrees.set(index, degrees);
        return false;
    }

    // The mark set aside, placed again wherever it is next used
    public void forget(int index) {
        placedX.set(index, Float.NaN);
    }

    // Accessible \\

    public String getTemplate() {
        return template;
    }

    public ElementInstance getContainer() {
        return container;
    }

    public boolean isExhausted() {
        return used == marks.size();
    }

    public ElementInstance get(int index) {
        return marks.get(index);
    }

    public int getUsed() {
        return used;
    }

    public int getShown() {
        return shown;
    }

    public void setShown(int shown) {
        this.shown = shown;
    }
}
