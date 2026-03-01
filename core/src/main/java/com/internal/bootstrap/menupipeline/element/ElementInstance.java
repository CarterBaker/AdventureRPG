package com.internal.bootstrap.menupipeline.element;

import com.internal.bootstrap.shaderpipeline.sprite.SpriteInstance;
import com.internal.core.engine.InstancePackage;
import com.internal.core.util.mathematics.matrices.Matrix4;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

/*
 * A live UI element instance spawned from an ElementHandle. Owns its own
 * SpriteInstance with independent material state. Overrides are applied at
 * spawn time; null overrides fall back to the handle at read time.
 */
public class ElementInstance extends InstancePackage {

    // Source handle — pure immutable data, never mutated
    private ElementHandle handle;

    // Per-instance overrides — null means use handle's value
    private SpriteInstance spriteInstance;
    private String textOverride;
    private Runnable clickActionOverride;
    private LayoutStruct layoutOverride;

    // Hierarchy
    private ObjectArrayList<ElementInstance> children;

    // Computed each frame
    private final Matrix4 transform = new Matrix4();
    private float computedLeft;
    private float computedTop;
    private float computedW;
    private float computedH;

    // Internal \\

    public void constructor(
            ElementHandle handle,
            SpriteInstance spriteInstance,
            String textOverride,
            Runnable clickActionOverride,
            LayoutStruct layoutOverride,
            ObjectArrayList<ElementInstance> children) {
        this.handle = handle;
        this.spriteInstance = spriteInstance;
        this.textOverride = textOverride;
        this.clickActionOverride = clickActionOverride;
        this.layoutOverride = layoutOverride;
        this.children = children;
    }

    // Layout \\

    public void computeTransform(float parentLeft, float parentTop, float parentW, float parentH) {

        LayoutStruct layout = layoutOverride != null ? layoutOverride : handle.getLayout();

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

    // Interaction \\

    public void execute() {
        Runnable action = clickActionOverride != null
                ? clickActionOverride
                : handle.getClickAction();
        if (action != null)
            action.run();
    }

    // Accessible \\

    public ElementHandle getHandle() {
        return handle;
    }

    public SpriteInstance getSpriteInstance() {
        return spriteInstance;
    }

    public String getText() {
        return textOverride != null ? textOverride : handle.getText();
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

    public boolean hasChildren() {
        return !children.isEmpty();
    }
}