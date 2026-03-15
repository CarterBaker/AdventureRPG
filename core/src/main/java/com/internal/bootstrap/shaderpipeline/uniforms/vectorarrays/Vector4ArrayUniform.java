package com.internal.bootstrap.shaderpipeline.uniforms.vectorarrays;

import com.badlogic.gdx.Gdx;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformType;
import com.internal.core.util.mathematics.vectors.Vector4;

public final class Vector4ArrayUniform extends UniformAttributeStruct<Object[]> {

    private final int elementCount;

    public Vector4ArrayUniform(int elementCount) {
        super(UniformType.VECTOR4, elementCount, new Vector4[elementCount]);
        this.elementCount = elementCount;
        for (int i = 0; i < elementCount; i++)
            ((Vector4[]) value)[i] = new Vector4();
    }

    @Override
    public UniformAttributeStruct<?> createDefault() {
        return new Vector4ArrayUniform(elementCount);
    }

    @Override
    protected void push(int handle, Object[] value) {
        float[] flat = new float[elementCount * 4];
        for (int i = 0; i < elementCount; i++) {
            Vector4 v = (Vector4) value[i];
            flat[i * 4] = v.x;
            flat[i * 4 + 1] = v.y;
            flat[i * 4 + 2] = v.z;
            flat[i * 4 + 3] = v.w;
        }
        Gdx.gl.glUniform4fv(handle, elementCount, flat, 0);
    }

    @Override
    protected void applyValue(Object[] value) {
        Vector4[] dst = (Vector4[]) this.value;
        for (int i = 0; i < Math.min(value.length, elementCount); i++)
            dst[i].set((Vector4) value[i]);
    }

    @Override
    protected void applyObject(Object value) {
        if (value instanceof Vector4[] v)
            applyValue(v);
        else if (value instanceof com.badlogic.gdx.math.Vector4[] vectors) {
            Vector4[] dst = (Vector4[]) this.value;
            for (int i = 0; i < Math.min(vectors.length, elementCount); i++) {
                dst[i].x = vectors[i].x;
                dst[i].y = vectors[i].y;
                dst[i].z = vectors[i].z;
                dst[i].w = vectors[i].w;
            }
        } else
            throw new IllegalArgumentException("applyObject(Vector4Array): got " + value.getClass());
    }

    public int elementCount() {
        return elementCount;
    }
}