package application.bootstrap.menupipeline.menu;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.kernel.windowpipeline.window.WindowInstance;
import engine.root.InstancePackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class MenuInstance extends InstancePackage {

    /*
     * Runtime menu created by MenuManager.openMenu(). Holds the shared MenuData
     * definition, the live element tree for this session, and the window it was
     * opened in. The window is used by MenuManager to route render calls to the
     * correct queue and by hit testing to ignore clicks from other windows.
     *
     * Canvas: if this menu defines a canvas_area element, CanvasAreaSystem
     * writes the computed screen rect directly here each frame. Callers read
     * the typed accessors rather than going back through the manager. hasCanvas()
     * returns false until the first layout pass writes a rect — guards in
     * TabContext.onResize and EditorTabCompositorSystem.update() rely on this.
     *
     * Visible by default.
     */

    // Internal
    private MenuData data;
    private ObjectArrayList<ElementInstance> elements;

    // Identity
    private WindowInstance window;

    // Canvas
    private int canvasX;
    private int canvasY;
    private int canvasW;
    private int canvasH;
    private boolean hasCanvas;

    // State
    private boolean visible;

    // Constructor \\

    public void constructor(
            MenuData data,
            ObjectArrayList<ElementInstance> elements,
            WindowInstance window) {

        // Internal
        this.data = data;
        this.elements = elements;

        // Identity
        this.window = window;

        // Canvas
        this.hasCanvas = false;

        // State
        this.visible = true;
    }

    // Entry Points \\

    public ElementInstance getEntryPoint(int index) {

        ObjectArrayList<String> eps = data.getEntryPoints();

        if (eps == null || index >= eps.size())
            return null;

        return findById(elements, eps.get(index));
    }

    public void addToEntryPoint(int index, ElementInstance element) {

        ElementInstance container = getEntryPoint(index);

        if (container != null)
            container.addChild(element);
    }

    public void removeFromEntryPoint(int index, ElementInstance element) {

        ElementInstance container = getEntryPoint(index);

        if (container != null)
            container.removeChild(element);
    }

    private ElementInstance findById(ObjectArrayList<ElementInstance> list, String id) {

        for (int i = 0; i < list.size(); i++) {

            ElementInstance el = list.get(i);

            if (el.getElementData().getId().equals(id))
                return el;

            if (el.hasChildren()) {
                ElementInstance found = findById(el.getChildren(), id);
                if (found != null)
                    return found;
            }
        }

        return null;
    }

    // Canvas \\

    public void setCanvas(int x, int y, int w, int h) {
        this.canvasX = x;
        this.canvasY = y;
        this.canvasW = w;
        this.canvasH = h;
        this.hasCanvas = true;
    }

    public void clearCanvas() {
        this.canvasX = 0;
        this.canvasY = 0;
        this.canvasW = 0;
        this.canvasH = 0;
        this.hasCanvas = false;
    }

    // Visibility \\

    public void show() {
        this.visible = true;
    }

    public void hide() {
        this.visible = false;
    }

    // Accessible \\

    public MenuData getMenuData() {
        return data;
    }

    public ObjectArrayList<ElementInstance> getElements() {
        return elements;
    }

    public WindowInstance getWindow() {
        return window;
    }

    public int getWindowID() {
        return window.getWindowID();
    }

    public boolean isVisible() {
        return visible;
    }

    public int getCanvasX() {
        return canvasX;
    }

    public int getCanvasY() {
        return canvasY;
    }

    public int getCanvasW() {
        return canvasW;
    }

    public int getCanvasH() {
        return canvasH;
    }

    public boolean hasCanvas() {
        return hasCanvas;
    }
}