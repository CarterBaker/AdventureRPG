package com.internal.bootstrap.menupipeline.element;

import com.internal.core.engine.StructPackage;
import com.internal.core.util.mathematics.vectors.Vector2;

public class LayoutStruct extends StructPackage {

    public final Vector2 anchor;
    public final Vector2 pivot;
    public final DimensionVector2 position;
    public final DimensionVector2 size;
    public final DimensionVector2 minSize; // null = no minimum
    public final DimensionVector2 maxSize; // null = no maximum

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

    public static LayoutStruct merge(LayoutStruct base, LayoutStruct override) {
        return new LayoutStruct(
                override.anchor != null ? override.anchor : base.anchor,
                override.pivot != null ? override.pivot : base.pivot,
                override.position != null ? override.position : base.position,
                override.size != null ? override.size : base.size,
                override.minSize != null ? override.minSize : base.minSize,
                override.maxSize != null ? override.maxSize : base.maxSize);
    }
}