package com.internal.bootstrap.shaderpipeline.uniforms.vectorarrays;

import com.internal.platform.PlatformRuntime;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformType;
import com.internal.core.util.mathematics.vectors.Vector4Boolean;

public final class Vector4BooleanArrayUniform extends UniformAttributeStruct<Object[]> {

    private final int elementCount;

    public Vector4BooleanArrayUniform(int elementCount) {
        super(UniformType.VECTOR4_BOOLEAN, elementCount, new Vector4Boolean[elementCount]);
        this.elementCount = elementCount;
        for (int i = 0; i < elementCount; i++)
            ((Vector4Boolean[]) value)[i] = new Vector4Boolean();
    }

    @Override
    public UniformAttributeStruct<?> createDefault() {
        return new Vector4BooleanArrayUniform(elementCount);
    }

    @Override
    protected void push(int handle, Object[] value) {
        int[] flat = new int[elementCount * 4];
        for (int i = 0; i < elementCount; i++) {
            Vector4Boolean v = (Vector4Boolean) value[i];
            flat[i * 4] = v.x ? 1 : 0;
            flat[i * 4 + 1] = v.y ? 1 : 0;
            flat[i * 4 + 2] = v.z ? 1 : 0;
            flat[i * 4 + 3] = v.w ? 1 : 0;
        }
        PlatformRuntime.gl.glUniform4iv(handle, elementCount, flat, 0);
    }

    @Override
    protected void applyValue(Object[] value) {
        Vector4Boolean[] dst = (Vector4Boolean[]) this.value;
        for (int i = 0; i < Math.min(value.length, elementCount); i++)
            dst[i].set((Vector4Boolean) value[i]);
    }

    public int elementCount() {
        return elementCount;
    }
}
