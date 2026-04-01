package com.internal.bootstrap.shaderpipeline.uniforms.vectorarrays;

import com.internal.core.app.CoreContext;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformType;
import com.internal.core.util.mathematics.vectors.Vector3Double;

public final class Vector3DoubleArrayUniform extends UniformAttributeStruct<Object[]> {

    private final int elementCount;

    public Vector3DoubleArrayUniform(int elementCount) {
        super(UniformType.VECTOR3_DOUBLE, elementCount, new Vector3Double[elementCount]);
        this.elementCount = elementCount;
        for (int i = 0; i < elementCount; i++)
            ((Vector3Double[]) value)[i] = new Vector3Double();
    }

    @Override
    public UniformAttributeStruct<?> createDefault() {
        return new Vector3DoubleArrayUniform(elementCount);
    }

    @Override
    protected void push(int handle, Object[] value) {
        float[] flat = new float[elementCount * 3];
        for (int i = 0; i < elementCount; i++) {
            Vector3Double v = (Vector3Double) value[i];
            flat[i * 3] = (float) v.x;
            flat[i * 3 + 1] = (float) v.y;
            flat[i * 3 + 2] = (float) v.z;
        }
        CoreContext.gl.glUniform3fv(handle, elementCount, flat, 0);
    }

    @Override
    protected void applyValue(Object[] value) {
        Vector3Double[] dst = (Vector3Double[]) this.value;
        for (int i = 0; i < Math.min(value.length, elementCount); i++)
            dst[i].set((Vector3Double) value[i]);
    }

    public int elementCount() {
        return elementCount;
    }
}
