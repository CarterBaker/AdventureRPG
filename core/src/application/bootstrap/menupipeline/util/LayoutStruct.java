package application.bootstrap.menupipeline.util;

import engine.root.StructPackage;
import engine.util.mathematics.vectors.Vector2;

public class LayoutStruct extends StructPackage {

    /*
     * Immutable layout descriptor for one UI element. Carries anchor, pivot,
     * position, size, optional min/max constraints, and an optional aspect
     * ratio. Used in full and override forms — null fields in an override mean
     * "keep base value", as does an aspect of zero. Owns size resolution so every
     * layout path clamps and aspect-fits an element identically: with an aspect
     * set, the element is the largest box of that ratio inside its resolved size.
     */

    // Internal
    private final Vector2 anchor;
    private final Vector2 pivot;
    private final DimensionVector2 position;
    private final DimensionVector2 size;
    private final DimensionVector2 minSize;
    private final DimensionVector2 maxSize;
    private final float aspect;

    // Constructor \\

    public LayoutStruct(
            Vector2 anchor,
            Vector2 pivot,
            DimensionVector2 position,
            DimensionVector2 size,
            DimensionVector2 minSize,
            DimensionVector2 maxSize,
            float aspect) {

        this.anchor = anchor;
        this.pivot = pivot;
        this.position = position;
        this.size = size;
        this.minSize = minSize;
        this.maxSize = maxSize;
        this.aspect = aspect;
    }

    // Merge \\

    public static LayoutStruct merge(LayoutStruct base, LayoutStruct override) {
        return new LayoutStruct(
                override.anchor != null ? override.anchor : base.anchor,
                override.pivot != null ? override.pivot : base.pivot,
                override.position != null ? override.position : base.position,
                override.size != null ? override.size : base.size,
                override.minSize != null ? override.minSize : base.minSize,
                override.maxSize != null ? override.maxSize : base.maxSize,
                override.hasAspect() ? override.aspect : base.aspect);
    }

    // Size \\

    public float resolveWidth(float parentW, float parentH) {

        float w = clampWidth(parentW);

        if (!hasAspect())
            return w;

        return Math.min(w, clampHeight(parentH) * aspect);
    }

    public float resolveHeight(float parentW, float parentH) {

        float h = clampHeight(parentH);

        if (!hasAspect())
            return h;

        return Math.min(h, clampWidth(parentW) / aspect);
    }

    private float clampWidth(float parentW) {

        float w = size.getX().resolve(parentW);

        if (hasMinSize())
            w = Math.max(w, minSize.getX().resolve(parentW));
        if (hasMaxSize())
            w = Math.min(w, maxSize.getX().resolve(parentW));

        return w;
    }

    private float clampHeight(float parentH) {

        float h = size.getY().resolve(parentH);

        if (hasMinSize())
            h = Math.max(h, minSize.getY().resolve(parentH));
        if (hasMaxSize())
            h = Math.min(h, maxSize.getY().resolve(parentH));

        return h;
    }

    // Accessible \\

    public Vector2 getAnchor() {
        return anchor;
    }

    public Vector2 getPivot() {
        return pivot;
    }

    public DimensionVector2 getPosition() {
        return position;
    }

    public DimensionVector2 getSize() {
        return size;
    }

    public DimensionVector2 getMinSize() {
        return minSize;
    }

    public DimensionVector2 getMaxSize() {
        return maxSize;
    }

    public float getAspect() {
        return aspect;
    }

    public boolean hasMinSize() {
        return minSize != null;
    }

    public boolean hasMaxSize() {
        return maxSize != null;
    }

    public boolean hasAspect() {
        return aspect > 0f;
    }
}
