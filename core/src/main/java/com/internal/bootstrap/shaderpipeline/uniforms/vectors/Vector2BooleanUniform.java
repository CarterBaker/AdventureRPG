package com.internal.bootstrap.shaderpipeline.uniforms.vectors;

import com.badlogic.gdx.Gdx;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformType;
import com.internal.core.util.mathematics.vectors.Vector2Boolean;

public final class Vector2BooleanUniform extends UniformAttributeStruct<Vector2Boolean> {

    public Vector2BooleanUniform() {
        super(UniformType.VECTOR2_BOOLEAN, new Vector2Boolean());
    }

    @Override
    public UniformAttributeStruct<?> createDefault() {
        return new Vector2BooleanUniform();
    }

    @Override
    protected void push(int handle, Vector2Boolean value) {
        Gdx.gl.glUniform2i(handle, value.x ? 1 : 0, value.y ? 1 : 0);
    }

    @Override
    protected void applyValue(Vector2Boolean value) {
        this.value.set(value);
    }
}