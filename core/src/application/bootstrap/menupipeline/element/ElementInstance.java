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
     * Runtime instance of a UI element. Holds a reference to the shared handle
     * and its ElementData definition, resolved sprite and font instances, optional
     * placement overrides, computed layout values updated each frame, and scroll,
     * content, expansion, hover, and click-state.
     *
     * Hover and click state sprite instances are cloned at open time from the
     * state's sprite override so the render system can swap without re-resolving.
     * Hover state children replace the default child list in-place when hovered.
     * Click state children render as an additional overlay below the element.
     *
     * When the hover state has a master handle, hoverStateRoot holds a full
     * ElementInstance for that master container. The render system renders it as
     * a positioned overlay so its own layout (position offset, size, color, stack)
     * is respected rather than inlining its children into this element's bounds.
     */

    // Internal
    private ElementHandle handle;
    private ElementData data;
    private SpriteInstance spriteInstance;
    private SpriteInstance hoverSpriteInstance;
    private SpriteInstance clickSpriteInstance;
    private FontInstance fontInstance;
    private Runnable resolvedAction;
    private String actionClassOverride;
    private String actionMethodOverride;
    private String actionArgOverride;
    private String actionClass;
    private String actionMethod;
    private String actionArg;
    private LayoutStruct layoutOverride;
    private DimensionVector2 positionOverride;

    // Text
    private String textOverride;

    // Children
    private ObjectArrayList<ElementInstance> children;
    private ObjectArrayList<ElementInstance> hoverStateChildren;
    private ObjectArrayList<ElementInstance> clickStateChildren;

    // Hover State Root — full instance of the master container when hover state has
    // a master
    private ElementInstance hoverStateRoot;

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

    // Click-State Content Size
    private float clickStateContentH;

    // Expansion
    private boolean expanded;

    // Hover / Click-State
    private boolean hovered;
    private boolean clickExpanded;

    // Internal \\

    @Override
    protected void create() {
        this.transform = new Matrix4();
    }

    // Constructor \\

    public void constructor(
            ElementHandle handle,
            SpriteInstance spriteInstance,
            SpriteInstance hoverSpriteInstance,
            SpriteInstance clickSpriteInstance,
            FontInstance fontInstance,
            String textOverride,
            Runnable resolvedAction,
            String actionClassOverride,
            String actionMethodOverride,
            String actionArgOverride,
            String actionClass,
            String actionMethod,
            String actionArg,
            LayoutStruct layoutOverride,
            ObjectArrayList<ElementInstance> children,
            ObjectArrayList<ElementInstance> hoverStateChildren,
            ObjectArrayList<ElementInstance> clickStateChildren) {

        this.handle = handle;
        this.data = handle.getElementData();
        this.spriteInstance = spriteInstance;
        this.hoverSpriteInstance = hoverSpriteInstance;
        this.clickSpriteInstance = clickSpriteInstance;
        this.fontInstance = fontInstance;
        this.textOverride = textOverride;
        this.resolvedAction = resolvedAction;
        this.actionClassOverride = actionClassOverride;
        this.actionMethodOverride = actionMethodOverride;
        this.actionArgOverride = actionArgOverride;
        this.actionClass = actionClass;
        this.actionMethod = actionMethod;
        this.actionArg = actionArg;
        this.layoutOverride = layoutOverride;
        this.children = children;
        this.hoverStateChildren = hoverStateChildren;
        this.clickStateChildren = clickStateChildren;
        this.expanded = data.isStartExpanded();
    }

    // Layout \\

    public void computeTransform(
            float parentLeft, float parentTop,
            float parentW, float parentH,
            LayoutStruct stateLayout) {

        LayoutStruct layout = stateLayout != null ? stateLayout
                : layoutOverride != null ? layoutOverride
                        : data.getLayout();

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
        float ty = anchorY + posY - (1f - layout.getPivot().y) * h;

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

    public void computeTransform(
            float parentLeft, float parentTop,
            float parentW, float parentH) {
        computeTransform(parentLeft, parentTop, parentW, parentH, null);
    }

    /*
     * Used by stacked containers — ignores anchor, pivot, and position. Places
     * the element at the given cursor. Size resolves from the active layout.
     */
    public void computeStackedTransform(
            float left, float top,
            float parentW, float parentH,
            LayoutStruct stateLayout) {

        LayoutStruct layout = stateLayout != null ? stateLayout
                : layoutOverride != null ? layoutOverride
                        : data.getLayout();

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

    public void computeStackedTransform(
            float left, float top,
            float parentW, float parentH) {
        computeStackedTransform(left, top, parentW, parentH, null);
    }

    /*
     * Used by toolbar elements — forces full screen width, top-of-screen
     * placement. Only height resolves from layout.
     */
    public void computeToolbarTransform(float screenW, float screenH) {

        LayoutStruct layout = layoutOverride != null ? layoutOverride : data.getLayout();

        float h = layout.getSize().getY().resolve(screenH);

        if (layout.hasMinSize())
            h = Math.max(h, layout.getMinSize().getY().resolve(screenH));

        if (layout.hasMaxSize())
            h = Math.min(h, layout.getMaxSize().getY().resolve(screenH));

        float top = screenH - h;

        this.computedLeft = 0f;
        this.computedTop = top;
        this.computedW = screenW;
        this.computedH = h;

        transform.set(
                screenW, 0, 0, 0f,
                0, h, 0, top,
                0, 0, 1, 0,
                0, 0, 0, 1);
    }

    // Action \\

    public String getEffectiveActionClass() {
        return actionClassOverride != null ? actionClassOverride : actionClass;
    }

    public String getEffectiveActionMethod() {
        return actionMethodOverride != null ? actionMethodOverride : actionMethod;
    }

    public String getEffectiveActionArg() {
        return actionArgOverride != null ? actionArgOverride : actionArg;
    }

    public boolean hasAction() {
        return getEffectiveActionClass() != null && getEffectiveActionMethod() != null;
    }

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
        this.scrollY = Math.max(0, Math.min(y, getMaxScrollY()));
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

    // Click-State Content Size \\

    public void setClickStateContentH(float h) {
        this.clickStateContentH = h;
    }

    public float getClickStateContentH() {
        return clickStateContentH;
    }

    // Position Override \\

    public void setPositionOverride(DimensionVector2 pos) {
        this.positionOverride = pos;
    }

    public void clearPositionOverride() {
        this.positionOverride = null;
    }

    // Expansion \\

    public void toggleExpanded() {
        this.expanded = !expanded;
    }

    public boolean isExpanded() {
        return expanded;
    }

    // Hover \\

    public void setHovered(boolean hovered) {
        this.hovered = hovered;
    }

    public boolean isHovered() {
        return hovered;
    }

    public boolean hasHoverState() {
        return handle.hasHoverState();
    }

    // Click-State \\

    public void setClickExpanded(boolean clickExpanded) {
        this.clickExpanded = clickExpanded;
    }

    public boolean isClickExpanded() {
        return clickExpanded;
    }

    public boolean hasClickState() {
        return handle.hasClickState();
    }

    public boolean hasClickStateChildren() {
        return clickStateChildren != null && !clickStateChildren.isEmpty();
    }

    public ObjectArrayList<ElementInstance> getClickStateChildren() {
        return clickStateChildren;
    }

    // Hover State Root \\

    public void setHoverStateRoot(ElementInstance root) {
        this.hoverStateRoot = root;
    }

    public ElementInstance getHoverStateRoot() {
        return hoverStateRoot;
    }

    public boolean hasHoverStateRoot() {
        return hoverStateRoot != null;
    }

    // Hover State Children \\

    public boolean hasHoverStateChildren() {
        return hoverStateChildren != null && !hoverStateChildren.isEmpty();
    }

    public ObjectArrayList<ElementInstance> getHoverStateChildren() {
        return hoverStateChildren;
    }

    // Accessible \\

    public ElementHandle getHandle() {
        return handle;
    }

    public ElementData getElementData() {
        return data;
    }

    public SpriteInstance getSpriteInstance() {
        return spriteInstance;
    }

    public SpriteInstance getHoverSpriteInstance() {
        return hoverSpriteInstance;
    }

    public SpriteInstance getClickSpriteInstance() {
        return clickSpriteInstance;
    }

    public FontInstance getFontInstance() {
        return fontInstance;
    }

    public ObjectArrayList<ElementInstance> getChildren() {
        return children;
    }

    public LayoutStruct getLayoutOverride() {
        return layoutOverride;
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