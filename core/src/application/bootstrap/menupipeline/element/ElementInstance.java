package application.bootstrap.menupipeline.element;

import application.bootstrap.menupipeline.font.FontInstance;
import application.bootstrap.menupipeline.util.DimensionVector2;
import application.bootstrap.menupipeline.util.LayoutStruct;
import application.bootstrap.shaderpipeline.sprite.SpriteInstance;
import engine.root.InstancePackage;
import engine.util.mathematics.matrices.Matrix4;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ElementInstance extends InstancePackage {

    /*
     * Runtime instance of a UI element. Holds a reference to the shared
     * ElementData definition, resolved sprite and font instances, a pre-resolved
     * click action, optional placement overrides, computed layout values updated
     * each frame, and scroll and content state for stacked containers.
     */

    // Internal
    private ElementData data;
    private SpriteInstance spriteInstance;
    private FontInstance fontInstance;
    private Runnable resolvedAction;
    private LayoutStruct layoutOverride;
    private DimensionVector2 positionOverride;

    // Text
    private String textOverride;

    // Children
    private ObjectArrayList<ElementInstance> children;

    // Computed
    private Matrix4 transform;
    private float computedLeft;
    private float computedTop;
    private float computedW;
    private float computedH;

    // Scroll
    private float scrollX;
    private float scrollY;

    // Content Size
    private float contentW;
    private float contentH;

    // Internal \\

    @Override
    protected void create() {
        this.transform = new Matrix4();
    }

    // Constructor \\

    public void constructor(
            ElementData data,
            SpriteInstance spriteInstance,
            FontInstance fontInstance,
            String textOverride,
            Runnable resolvedAction,
            LayoutStruct layoutOverride,
            ObjectArrayList<ElementInstance> children) {

        // Internal
        this.data = data;
        this.spriteInstance = spriteInstance;
        this.fontInstance = fontInstance;
        this.textOverride = textOverride;
        this.resolvedAction = resolvedAction;
        this.layoutOverride = layoutOverride;

        // Children
        this.children = children;
    }

    // Layout \\

    public void computeTransform(
            float parentLeft, float parentTop,
            float parentW, float parentH) {

        LayoutStruct layout = layoutOverride != null ? layoutOverride : data.getLayout();
        DimensionVector2 pos = positionOverride != null ? positionOverride : layout.getPosition();

        float posX = pos.getX().resolve(parentW);
        float posY = pos.getY().resolve(parentH);
        float w = layout.getSize().getX().resolve(parentW);
        float h = layout.getSize().getY().resolve(parentH);

        if (layout.hasMinSize()) {
            w = Math.max(w, layout.getMinSize().getX().resolve(parentW));
            h = Math.max(h, layout.getMinSize().getY().resolve(parentH));
        }

        if (layout.hasMaxSize()) {
            w = Math.min(w, layout.getMaxSize().getX().resolve(parentW));
            h = Math.min(h, layout.getMaxSize().getY().resolve(parentH));
        }

        float anchorX = parentLeft + layout.getAnchor().x * parentW;
        float anchorY = parentTop + layout.getAnchor().y * parentH;
        float tx = anchorX + posX - layout.getPivot().x * w;
        float ty = anchorY + posY - layout.getPivot().y * h;

        this.computedLeft = tx;
        this.computedTop = ty;
        this.computedW = w;
        this.computedH = h;

        transform.set(
                w, 0, 0, tx,
                0, h, 0, ty,
                0, 0, 1, 0,
                0, 0, 0, 1);
    }

    /*
     * Used by stacked containers — ignores anchor, pivot, and position. Places
     * the element at the given cursor. Size still resolves from layout.
     */
    public void computeStackedTransform(
            float left, float top,
            float parentW, float parentH) {

        LayoutStruct layout = layoutOverride != null ? layoutOverride : data.getLayout();

        float w = layout.getSize().getX().resolve(parentW);
        float h = layout.getSize().getY().resolve(parentH);

        if (layout.hasMinSize()) {
            w = Math.max(w, layout.getMinSize().getX().resolve(parentW));
            h = Math.max(h, layout.getMinSize().getY().resolve(parentH));
        }

        if (layout.hasMaxSize()) {
            w = Math.min(w, layout.getMaxSize().getX().resolve(parentW));
            h = Math.min(h, layout.getMaxSize().getY().resolve(parentH));
        }

        this.computedLeft = left;
        this.computedTop = top;
        this.computedW = w;
        this.computedH = h;

        transform.set(
                w, 0, 0, left,
                0, h, 0, top,
                0, 0, 1, 0,
                0, 0, 0, 1);
    }

    // Interaction \\

    public void execute() {
        if (resolvedAction != null)
            resolvedAction.run();
    }

    // Child Mutation \\

    public void addChild(ElementInstance child) {
        children.add(child);
    }

    public void removeChild(ElementInstance child) {
        children.remove(child);
    }

    public ElementInstance findChildById(String id) {

        for (int i = 0; i < children.size(); i++) {

            ElementInstance child = children.get(i);

            if (child.getElementData().getId().equals(id))
                return child;

            ElementInstance found = child.findChildById(id);

            if (found != null)
                return found;
        }

        return null;
    }

    // Scroll \\

    public void setScrollX(float x) {
        this.scrollX = x;
    }

    public void setScrollY(float y) {
        this.scrollY = y;
    }

    public float getScrollX() {
        return Math.max(0, Math.min(scrollX, getMaxScrollX()));
    }

    public float getScrollY() {
        return Math.max(0, Math.min(scrollY, getMaxScrollY()));
    }

    public float getMaxScrollX() {
        return Math.max(0f, contentW - computedW);
    }

    public float getMaxScrollY() {
        return Math.max(0f, contentH - computedH);
    }

    // Content Size \\

    public void setContentW(float w) {
        this.contentW = w;
    }

    public void setContentH(float h) {
        this.contentH = h;
    }

    public float getContentW() {
        return contentW;
    }

    public float getContentH() {
        return contentH;
    }

    // Position Override \\

    public void setPositionOverride(DimensionVector2 pos) {
        this.positionOverride = pos;
    }

    public void clearPositionOverride() {
        this.positionOverride = null;
    }

    // Accessible \\

    public ElementData getElementData() {
        return data;
    }

    public SpriteInstance getSpriteInstance() {
        return spriteInstance;
    }

    public FontInstance getFontInstance() {
        return fontInstance;
    }

    public ObjectArrayList<ElementInstance> getChildren() {
        return children;
    }

    public Matrix4 getTransform() {
        return transform;
    }

    public float getComputedLeft() {
        return computedLeft;
    }

    public float getComputedTop() {
        return computedTop;
    }

    public float getComputedW() {
        return computedW;
    }

    public float getComputedH() {
        return computedH;
    }

    public boolean hasSprite() {
        return spriteInstance != null;
    }

    public boolean hasFont() {
        return fontInstance != null;
    }

    public boolean hasChildren() {
        return !children.isEmpty();
    }

    public String getText() {
        return textOverride != null ? textOverride : data.getText();
    }
}