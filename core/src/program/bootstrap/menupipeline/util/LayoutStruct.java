package program.bootstrap.menupipeline.util;

import program.core.engine.StructPackage;
import program.core.util.mathematics.vectors.Vector2;

public class LayoutStruct extends StructPackage {

    /*
     * Immutable layout descriptor for one UI element. Carries anchor, pivot,
     * position, size, and optional min/max constraints. Used in full and override
     * forms — null fields in an override mean "keep base value".
     */

    // Internal
    private final Vector2 anchor;
    private final Vector2 pivot;
    private final DimensionVector2 position;
    private final DimensionVector2 size;
    private final DimensionVector2 minSize;
    private final DimensionVector2 maxSize;

    // Constructor \\

    public LayoutStruct(
            Vector2 anchor,
            Vector2 pivot,
            DimensionVector2 position,
            DimensionVector2 size,
            DimensionVector2 minSize,
            DimensionVector2 maxSize) {

        this.anchor = anchor;
        this.pivot = pivot;
        this.position = position;
        this.size = size;
        this.minSize = minSize;
        this.maxSize = maxSize;
    }

    // Merge \\

    public static LayoutStruct merge(LayoutStruct base, LayoutStruct override) {
        return new LayoutStruct(
                override.anchor != null ? override.anchor : base.anchor,
                override.pivot != null ? override.pivot : base.pivot,
                override.position != null ? override.position : base.position,
                override.size != null ? override.size : base.size,
                override.minSize != null ? override.minSize : base.minSize,
                override.maxSize != null ? override.maxSize : base.maxSize);
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

    public boolean hasMinSize() {
        return minSize != null;
    }

    public boolean hasMaxSize() {
        return maxSize != null;
    }
}