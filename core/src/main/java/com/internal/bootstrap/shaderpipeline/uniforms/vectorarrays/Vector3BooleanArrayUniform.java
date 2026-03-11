package com.internal.bootstrap.shaderpipeline.uniforms.vectorarrays;

import com.badlogic.gdx.Gdx;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformAttribute;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformType;
import com.internal.core.util.mathematics.vectors.Vector3Boolean;

public final class Vector3BooleanArrayUniform extends UniformAttribute<Object[]> {

    private final int elementCount;

    public Vector3BooleanArrayUniform(int elementCount) {
        super(UniformType.VECTOR3_BOOLEAN, elementCount, new Vector3Boolean[elementCount]);
        this.elementCount = elementCount;
        for (int i = 0; i < elementCount; i++)
            ((Vector3Boolean[]) value)[i] = new Vector3Boolean();
    }

    @Override
    public UniformAttribute<?> createDefault() {
        return new Vector3BooleanArrayUniform(elementCount);
    }

    @Override
    protected void push(int handle, Object[] value) {
        int[] flat = new int[elementCount * 3];
        for (int i = 0; i < elementCount; i++) {
            Vector3Boolean v = (Vector3Boolean) value[i];
            flat[i * 3] = v.x ? 1 : 0;
            flat[i * 3 + 1] = v.y ? 1 : 0;
            flat[i * 3 + 2] = v.z ? 1 : 0;
        }
        Gdx.gl.glUniform3iv(handle, elementCount, flat, 0);
    }

    @Override
    protected void applyValue(Object[] value) {
        Vector3Boolean[] dst = (Vector3Boolean[]) this.value;
        for (int i = 0; i < Math.min(value.length, elementCount); i++)
            dst[i].set((Vector3Boolean) value[i]);
    }

    public int elementCount() {
        return elementCount;
    }
}