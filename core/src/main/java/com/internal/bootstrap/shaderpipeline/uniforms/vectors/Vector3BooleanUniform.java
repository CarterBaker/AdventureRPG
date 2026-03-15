package com.internal.bootstrap.shaderpipeline.uniforms.vectors;

import com.badlogic.gdx.Gdx;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformType;
import com.internal.core.util.mathematics.vectors.Vector3Boolean;

public final class Vector3BooleanUniform extends UniformAttributeStruct<Vector3Boolean> {

    public Vector3BooleanUniform() {
        super(UniformType.VECTOR3_BOOLEAN, new Vector3Boolean());
    }

    @Override
    public UniformAttributeStruct<?> createDefault() {
        return new Vector3BooleanUniform();
    }

    @Override
    protected void push(int handle, Vector3Boolean value) {
        Gdx.gl.glUniform3i(handle, value.x ? 1 : 0, value.y ? 1 : 0, value.z ? 1 : 0);
    }

    @Override
    protected void applyValue(Vector3Boolean value) {
        this.value.set(value);
    }
}