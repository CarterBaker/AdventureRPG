package com.internal.bootstrap.menupipeline.element;

import com.internal.bootstrap.menupipeline.fonts.FontInstance;
import com.internal.bootstrap.shaderpipeline.sprite.SpriteInstance;
import com.internal.core.engine.InstancePackage;
import com.internal.core.util.mathematics.matrices.Matrix4;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ElementInstance extends InstancePackage {

    private ElementHandle handle;
    private SpriteInstance spriteInstance;
    private FontInstance fontInstance;
    private String textOverride;
    private Runnable clickActionOverride;
    private LayoutStruct layoutOverride;
    private DimensionVector2 positionOverride; // null = use layout

    private ObjectArrayList<ElementInstance> children;

    // Computed each frame
    private final Matrix4 transform = new Matrix4();
    private float computedLeft;
    private float computedTop;
    private float computedW;
    private float computedH;

    // Scroll — set externally, clamped on read
    private float scrollX;
    private float scrollY;

    // Content size — written by renderer each frame for stacked containers
    private float contentW;
    private float contentH;

    // Constructor \\

    public void constructor(
            ElementHandle handle,
            SpriteInstance spriteInstance,
            FontInstance fontInstance,
            String textOverride,
            Runnable clickActionOverride,
            LayoutStruct layoutOverride,
            ObjectArrayList<ElementInstance> children) {
        this.handle = handle;
        this.spriteInstance = spriteInstance;
        this.fontInstance = fontInstance;
        this.textOverride = textOverride;
        this.clickActionOverride = clickActionOverride;
        this.layoutOverride = layoutOverride;
        this.children = children;
    }

    // Layout \\

    public void computeTransform(float parentLeft, float parentTop, float parentW, float parentH) {

        LayoutStruct layout = layoutOverride != null ? layoutOverride : handle.getLayout();

        DimensionVector2 pos = positionOverride != null ? positionOverride : layout.position;
        float posX = pos.x.resolve(parentW);
        float posY = pos.y.resolve(parentH);

        float w = layout.size.x.resolve(parentW);
        float h = layout.size.y.resolve(parentH);

        if (layout.minSize != null) {
            w = Math.max(w, layout.minSize.x.resolve(parentW));
            h = Math.max(h, layout.minSize.y.resolve(parentH));
        }
        if (layout.maxSize != null) {
            w = Math.min(w, layout.maxSize.x.resolve(parentW));
            h = Math.min(h, layout.maxSize.y.resolve(parentH));
        }

        float anchorX = parentLeft + layout.anchor.x * parentW;
        float anchorY = parentTop + layout.anchor.y * parentH;
        float tx = anchorX + posX - layout.pivot.x * w;
        float ty = anchorY + posY - layout.pivot.y * h;

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
     * Used by stacked containers — ignores anchor/pivot/position, places element
     * at the given cursor position. Size still resolves from layout.
     */
    public void computeStackedTransform(float left, float top, float parentW, float parentH) {

        LayoutStruct layout = layoutOverride != null ? layoutOverride : handle.getLayout();

        float w = layout.size.x.resolve(parentW);
        float h = layout.size.y.resolve(parentH);

        if (layout.minSize != null) {
            w = Math.max(w, layout.minSize.x.resolve(parentW));
            h = Math.max(h, layout.minSize.y.resolve(parentH));
        }
        if (layout.maxSize != null) {
            w = Math.min(w, layout.maxSize.x.resolve(parentW));
            h = Math.min(h, layout.maxSize.y.resolve(parentH));
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
        Runnable action = clickActionOverride != null
                ? clickActionOverride
                : handle.getClickAction();
        if (action != null)
            action.run();
    }

    // Child Mutation — for runtime injection \\

    public void addChild(ElementInstance child) {
        children.add(child);
    }

    public void removeChild(ElementInstance child) {
        children.remove(child);
    }

    public ElementInstance findChildById(String id) {
        for (int i = 0; i < children.size(); i++) {
            if (children.get(i).getHandle().getId().equals(id))
                return children.get(i);
            ElementInstance found = children.get(i).findChildById(id);
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

    // Content Size — written by renderer each frame for stacked containers \\

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

    // Position Override — for programmatic positioning (e.g. scrollbar handle) \\

    public void setPositionOverride(DimensionVector2 pos) {
        this.positionOverride = pos;
    }

    public void clearPositionOverride() {
        this.positionOverride = null;
    }

    // Accessible \\

    public ElementHandle getHandle() {
        return handle;
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
        return textOverride != null ? textOverride : handle.getText();
    }
}