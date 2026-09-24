package application.bootstrap.menupipeline.menu;

import application.bootstrap.menupipeline.canvas.CanvasInstance;
import application.bootstrap.menupipeline.element.ElementInstance;
import application.kernel.windowpipeline.window.WindowInstance;
import engine.root.InstancePackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class MenuInstance extends InstancePackage {

    /*
     * Runtime menu created by MenuManager.openMenu(). Holds the shared MenuData
     * definition, the live element tree for this session, and the window it was
     * opened in.
     *
     * Canvas: if this menu declared a canvas_area in JSON, a CanvasInstance is
     * created here at construction time. MenuRenderSystem writes computed OpenGL-
     * space bounds into it every frame. Null means no canvas. Callers null-check
     * getCanvas() directly.
     *
     * Entry points resolve anywhere in the live element tree, including the
     * overlay roots an element shows while hovered, so content injected into a
     * hover dropdown lands in the same instance the renderer draws.
     *
     * Visible by default.
     */

    // Internal
    private MenuData data;
    private ObjectArrayList<ElementInstance> elements;

    // Identity
    private WindowInstance window;

    // Canvas
    private CanvasInstance canvas;

    // State
    private boolean visible;

    // Constructor \\

    public void constructor(
            MenuData data,
            ObjectArrayList<ElementInstance> elements,
            WindowInstance window) {
        this.data = data;
        this.elements = elements;
        this.window = window;
        this.canvas = data.hasCanvasArea() ? create(CanvasInstance.class) : null;
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
            ElementInstance found = findById(list.get(i), id);
            if (found != null)
                return found;
        }
        return null;
    }

    private ElementInstance findById(ElementInstance element, String id) {

        if (element == null)
            return null;

        if (element.getElementData().getId().equals(id))
            return element;

        ElementInstance found = findById(element.getChildren(), id);

        if (found == null)
            found = findById(element.getHoverEnterStateRoot(), id);

        if (found == null)
            found = findById(element.getHoverStateRoot(), id);

        if (found == null)
            found = findById(element.getHoverExitStateRoot(), id);

        return found;
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

    public CanvasInstance getCanvas() {
        return canvas;
    }
}