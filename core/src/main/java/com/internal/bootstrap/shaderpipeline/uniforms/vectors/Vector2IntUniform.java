package com.internal.bootstrap.shaderpipeline.uniforms.vectors;

import com.internal.platform.Gdx;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformType;
import com.internal.core.util.mathematics.vectors.Vector2Int;

public final class Vector2IntUniform extends UniformAttributeStruct<Vector2Int> {

    public Vector2IntUniform() {
        super(UniformType.VECTOR2_INT, new Vector2Int());
    }

    @Override
    public UniformAttributeStruct<?> createDefault() {
        return new Vector2IntUniform();
    }

    @Override
    protected void push(int handle, Vector2Int value) {
        Gdx.gl.glUniform2i(handle, value.x, value.y);
    }

    @Override
    protected void applyValue(Vector2Int value) {
        this.value.set(value);
    }
}