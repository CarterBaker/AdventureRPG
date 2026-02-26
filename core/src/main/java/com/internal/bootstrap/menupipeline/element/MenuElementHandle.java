package com.internal.bootstrap.menupipeline.element;

import com.internal.bootstrap.shaderpipeline.sprite.SpriteHandle;
import com.internal.core.engine.HandlePackage;
import com.internal.core.util.mathematics.matrices.Matrix4;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class MenuElementHandle extends HandlePackage {

    // Identity
    private String id;
    private ElementType type;

    // Visuals
    private SpriteHandle spriteHandle; // null if not visual
    private String text; // null if not text

    // Layout
    private LayoutStruct layout;

    // Interaction
    private Runnable clickAction; // null if not interactive

    // Hierarchy
    private ObjectArrayList<MenuElementHandle> children;

    // Computed each frame — updated before render
    private final Matrix4 transform = new Matrix4();
    private float computedLeft;
    private float computedTop;
    private float computedW;
    private float computedH;

    public void constructor(
            String id,
            ElementType type,
            SpriteHandle spriteHandle,
            String text,
            LayoutStruct layout,
            Runnable clickAction,
            ObjectArrayList<MenuElementHandle> children) {
        this.id = id;
        this.type = type;
        this.spriteHandle = spriteHandle;
        this.text = text;
        this.layout = layout;
        this.clickAction = clickAction;
        this.children = children;
    }

    // Layout \\

    // parentLeft/Top: ortho-space top-left of parent
    // parentW/H: parent pixel dimensions
    public void computeTransform(float parentLeft, float parentTop, float parentW, float parentH) {

        float posX = layout.position.x.resolve(parentW);
        float posY = layout.position.y.resolve(parentH);

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

        // y-up: anchor.y=1.0 → top of parent, anchor.y=0.0 → bottom
        float anchorX = parentLeft + layout.anchor.x * parentW;
        float anchorY = parentTop + layout.anchor.y * parentH;

        // position offset: positive y = up
        float pointX = anchorX + posX;
        float pointY = anchorY + posY;

        // pivot.y=1.0 → top of element sits at point, pivot.y=0.0 → bottom sits at
        // point
        float tx = pointX - layout.pivot.x * w;
        float ty = pointY - layout.pivot.y * h;

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

    // Interaction \\

    public void execute() {
        if (clickAction != null)
            clickAction.run();
    }

    // Accessible \\

    public String getId() {
        return id;
    }

    public ElementType getType() {
        return type;
    }

    public SpriteHandle getSpriteHandle() {
        return spriteHandle;
    }

    public String getText() {
        return text;
    }

    public LayoutStruct getLayout() {
        return layout;
    }

    public Runnable getClickAction() {
        return clickAction;
    }

    public Matrix4 getTransform() {
        return transform;
    }

    public ObjectArrayList<MenuElementHandle> getChildren() {
        return children;
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
        return spriteHandle != null;
    }

    public boolean hasText() {
        return text != null;
    }

    public boolean hasClickAction() {
        return clickAction != null;
    }

    public boolean hasChildren() {
        return !children.isEmpty();
    }
}