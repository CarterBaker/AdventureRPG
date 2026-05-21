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
     * Runtime instance of a UI element.
     *
     * activeHoverState is set by ElementHitSystem:
     * on enter → hoverEnterState
     * per frame → hoverState (if defined, replaces enter state)
     * on exit → hoverExitState
     * on clear → null
     *
     * The render system reads activeHoverState via resolveActiveState.
     * Sprite instances are cloned for all four states at open time.
     */

    // Internal
    private ElementHandle handle;
    private ElementData data;
    private SpriteInstance spriteInstance;
    private SpriteInstance hoverEnterSpriteInstance;
    private SpriteInstance hoverSpriteInstance;
    private SpriteInstance hoverExitSpriteInstance;
    private SpriteInstance clickSpriteInstance;
    private FontInstance fontInstance;

    // Action overrides — on_click
    private String actionClassOverride;
    private String actionMethodOverride;
    private String actionArgOverride;

    // Action overrides — on_drag
    private String onDragClassOverride;
    private String onDragMethodOverride;
    private String onDragArgOverride;

    // Layout
    private LayoutStruct layoutOverride;
    private DimensionVector2 positionOverride;

    // Text
    private String textOverride;

    // Children
    private ObjectArrayList<ElementInstance> children;
    private ObjectArrayList<ElementInstance> hoverEnterStateChildren;
    private ObjectArrayList<ElementInstance> hoverStateChildren;
    private ObjectArrayList<ElementInstance> hoverExitStateChildren;
    private ObjectArrayList<ElementInstance> clickStateChildren;

    // Hover state root instances — for master-based state overlays
    private ElementInstance hoverEnterStateRoot;
    private ElementInstance hoverStateRoot;
    private ElementInstance hoverExitStateRoot;

    // Active hover state — set by ElementHitSystem
    private ElementStateStruct activeHoverState;

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

    // State flags
    private boolean hovered;
    private boolean clickExpanded;

    @Override
    protected void create() {
        this.transform = new Matrix4();
    }

    // Constructor \\

    public void constructor(
            ElementHandle handle,
            SpriteInstance spriteInstance,
            SpriteInstance hoverEnterSpriteInstance,
            SpriteInstance hoverSpriteInstance,
            SpriteInstance hoverExitSpriteInstance,
            SpriteInstance clickSpriteInstance,
            FontInstance fontInstance,
            String textOverride,
            String actionClassOverride,
            String actionMethodOverride,
            String actionArgOverride,
            String onDragClassOverride,
            String onDragMethodOverride,
            String onDragArgOverride,
            LayoutStruct layoutOverride,
            ObjectArrayList<ElementInstance> children,
            ObjectArrayList<ElementInstance> hoverEnterStateChildren,
            ObjectArrayList<ElementInstance> hoverStateChildren,
            ObjectArrayList<ElementInstance> hoverExitStateChildren,
            ObjectArrayList<ElementInstance> clickStateChildren) {

        this.handle = handle;
        this.data = handle.getElementData();
        this.spriteInstance = spriteInstance;
        this.hoverEnterSpriteInstance = hoverEnterSpriteInstance;
        this.hoverSpriteInstance = hoverSpriteInstance;
        this.hoverExitSpriteInstance = hoverExitSpriteInstance;
        this.clickSpriteInstance = clickSpriteInstance;
        this.fontInstance = fontInstance;
        this.textOverride = textOverride;
        this.actionClassOverride = actionClassOverride;
        this.actionMethodOverride = actionMethodOverride;
        this.actionArgOverride = actionArgOverride;
        this.onDragClassOverride = onDragClassOverride;
        this.onDragMethodOverride = onDragMethodOverride;
        this.onDragArgOverride = onDragArgOverride;
        this.layoutOverride = layoutOverride;
        this.children = children;
        this.hoverEnterStateChildren = hoverEnterStateChildren;
        this.hoverStateChildren = hoverStateChildren;
        this.hoverExitStateChildren = hoverExitStateChildren;
        this.clickStateChildren = clickStateChildren;
        this.expanded = data.isStartExpanded();
    }

    // Active Hover State \\

    public void setActiveHoverState(ElementStateStruct state) {
        this.activeHoverState = state;
    }

    public void clearActiveHoverState() {
        this.activeHoverState = null;
    }

    public ElementStateStruct getActiveHoverState() {
        return activeHoverState;
    }

    public boolean hasActiveHoverState() {
        return activeHoverState != null;
    }

    // Hover State Root \\

    public void setHoverEnterStateRoot(ElementInstance root) {
        this.hoverEnterStateRoot = root;
    }

    public ElementInstance getHoverEnterStateRoot() {
        return hoverEnterStateRoot;
    }

    public boolean hasHoverEnterStateRoot() {
        return hoverEnterStateRoot != null;
    }

    public void setHoverStateRoot(ElementInstance root) {
        this.hoverStateRoot = root;
    }

    public ElementInstance getHoverStateRoot() {
        return hoverStateRoot;
    }

    public boolean hasHoverStateRoot() {
        return hoverStateRoot != null;
    }

    public void setHoverExitStateRoot(ElementInstance root) {
        this.hoverExitStateRoot = root;
    }

    public ElementInstance getHoverExitStateRoot() {
        return hoverExitStateRoot;
    }

    public boolean hasHoverExitStateRoot() {
        return hoverExitStateRoot != null;
    }

    // Effective Action Accessors \\

    public String getEffectiveActionClass() {
        return actionClassOverride != null ? actionClassOverride : data.getActionClass();
    }

    public String getEffectiveActionMethod() {
        return actionMethodOverride != null ? actionMethodOverride : data.getActionMethod();
    }

    public String getEffectiveActionArg() {
        return actionArgOverride != null ? actionArgOverride : data.getActionArg();
    }

    public String getEffectiveOnDragClass() {
        return onDragClassOverride != null ? onDragClassOverride : data.getOnDragClass();
    }

    public String getEffectiveOnDragMethod() {
        return onDragMethodOverride != null ? onDragMethodOverride : data.getOnDragMethod();
    }

    public String getEffectiveOnDragArg() {
        return onDragArgOverride != null ? onDragArgOverride : data.getOnDragArg();
    }

    public boolean hasAction() {
        return getEffectiveActionClass() != null && getEffectiveActionMethod() != null;
    }

    public boolean hasOnDrag() {
        return getEffectiveOnDragClass() != null && getEffectiveOnDragMethod() != null;
    }

    public boolean isHoverable() {
        return handle.isHoverable() || hasAction() || hasOnDrag();
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

        transform.set(w, 0, 0, tx, 0, h, 0, ty, 0, 0, 1, 0, 0, 0, 0, 1);
    }

    public void computeTransform(float parentLeft, float parentTop, float parentW, float parentH) {
        computeTransform(parentLeft, parentTop, parentW, parentH, null);
    }

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

        transform.set(w, 0, 0, left, 0, h, 0, top, 0, 0, 1, 0, 0, 0, 0, 1);
    }

    public void computeStackedTransform(float left, float top, float parentW, float parentH) {
        computeStackedTransform(left, top, parentW, parentH, null);
    }

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

        transform.set(screenW, 0, 0, 0f, 0, h, 0, top, 0, 0, 1, 0, 0, 0, 0, 1);
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

    // Hover State Children \\

    public boolean hasHoverEnterStateChildren() {
        return hoverEnterStateChildren != null && !hoverEnterStateChildren.isEmpty();
    }

    public ObjectArrayList<ElementInstance> getHoverEnterStateChildren() {
        return hoverEnterStateChildren;
    }

    public boolean hasHoverStateChildren() {
        return hoverStateChildren != null && !hoverStateChildren.isEmpty();
    }

    public ObjectArrayList<ElementInstance> getHoverStateChildren() {
        return hoverStateChildren;
    }

    public boolean hasHoverExitStateChildren() {
        return hoverExitStateChildren != null && !hoverExitStateChildren.isEmpty();
    }

    public ObjectArrayList<ElementInstance> getHoverExitStateChildren() {
        return hoverExitStateChildren;
    }

    // Sprite Instances \\

    public SpriteInstance getHoverEnterSpriteInstance() {
        return hoverEnterSpriteInstance;
    }

    public SpriteInstance getHoverSpriteInstance() {
        return hoverSpriteInstance;
    }

    public SpriteInstance getHoverExitSpriteInstance() {
        return hoverExitSpriteInstance;
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