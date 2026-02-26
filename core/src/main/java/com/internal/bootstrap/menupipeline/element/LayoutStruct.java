package com.internal.bootstrap.menupipeline.element;

import com.internal.core.engine.StructPackage;
import com.internal.core.util.mathematics.vectors.Vector2;

public class LayoutStruct extends StructPackage {

    public final Vector2 anchor; // point on parent to measure from
    public final Vector2 pivot; // point on element placed at anchor + offset
    public final DimensionVector2 position; // offset from anchor
    public final DimensionVector2 size;
    public final DimensionVector2 minSize; // null = unconstrained
    public final DimensionVector2 maxSize; // null = unconstrained

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
}