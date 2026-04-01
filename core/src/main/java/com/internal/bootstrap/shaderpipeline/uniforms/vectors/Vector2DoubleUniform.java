package com.internal.bootstrap.shaderpipeline.uniforms.vectors;

import com.internal.core.app.CoreContext;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformType;
import com.internal.core.util.mathematics.vectors.Vector2Double;

public final class Vector2DoubleUniform extends UniformAttributeStruct<Vector2Double> {

    public Vector2DoubleUniform() {
        super(UniformType.VECTOR2_DOUBLE, new Vector2Double());
    }

    @Override
    public UniformAttributeStruct<?> createDefault() {
        return new Vector2DoubleUniform();
    }

    @Override
    protected void push(int handle, Vector2Double value) {
        CoreContext.gl.glUniform2f(handle, (float) value.x, (float) value.y);
    }

    @Override
    protected void applyValue(Vector2Double value) {
        this.value.set(value);
    }
}
