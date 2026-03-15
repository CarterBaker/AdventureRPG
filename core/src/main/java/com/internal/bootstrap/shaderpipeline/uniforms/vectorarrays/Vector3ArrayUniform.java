package com.internal.bootstrap.shaderpipeline.uniforms.vectorarrays;

import com.badlogic.gdx.Gdx;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformType;
import com.internal.core.util.mathematics.vectors.Vector3;

public final class Vector3ArrayUniform extends UniformAttributeStruct<Object[]> {

    private final int elementCount;

    public Vector3ArrayUniform(int elementCount) {
        super(UniformType.VECTOR3, elementCount, new Vector3[elementCount]);
        this.elementCount = elementCount;
        for (int i = 0; i < elementCount; i++)
            ((Vector3[]) value)[i] = new Vector3();
    }

    @Override
    public UniformAttributeStruct<?> createDefault() {
        return new Vector3ArrayUniform(elementCount);
    }

    @Override
    protected void push(int handle, Object[] value) {
        float[] flat = new float[elementCount * 3];
        for (int i = 0; i < elementCount; i++) {
            Vector3 v = (Vector3) value[i];
            flat[i * 3] = v.x;
            flat[i * 3 + 1] = v.y;
            flat[i * 3 + 2] = v.z;
        }
        Gdx.gl.glUniform3fv(handle, elementCount, flat, 0);
    }

    @Override
    protected void applyValue(Object[] value) {
        Vector3[] dst = (Vector3[]) this.value;
        for (int i = 0; i < Math.min(value.length, elementCount); i++)
            dst[i].set((Vector3) value[i]);
    }

    @Override
    protected void applyObject(Object value) {
        if (value instanceof Vector3[] v)
            applyValue(v);
        else if (value instanceof com.badlogic.gdx.math.Vector3[] vectors) {
            Vector3[] dst = (Vector3[]) this.value;
            for (int i = 0; i < Math.min(vectors.length, elementCount); i++) {
                dst[i].x = vectors[i].x;
                dst[i].y = vectors[i].y;
                dst[i].z = vectors[i].z;
            }
        } else
            throw new IllegalArgumentException("applyObject(Vector3Array): got " + value.getClass());
    }

    public int elementCount() {
        return elementCount;
    }
}