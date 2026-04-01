package com.internal.bootstrap.shaderpipeline.uniforms.matrixArrays;

import com.internal.platform.PlatformRuntime;
import com.internal.platform.utils.BufferUtils;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformType;
import com.internal.core.util.mathematics.matrices.Matrix4Double;

import java.nio.FloatBuffer;

public final class Matrix4DoubleArrayUniform extends UniformAttributeStruct<Object[]> {

    private final int elementCount;
    private final FloatBuffer uniformBuffer;

    public Matrix4DoubleArrayUniform(int elementCount) {
        super(UniformType.MATRIX4_DOUBLE, elementCount, new Matrix4Double[elementCount]);
        this.elementCount = elementCount;
        this.uniformBuffer = BufferUtils.newFloatBuffer(elementCount * 16);
        for (int i = 0; i < elementCount; i++)
            ((Matrix4Double[]) value)[i] = new Matrix4Double();
    }

    @Override
    public UniformAttributeStruct<?> createDefault() {
        return new Matrix4DoubleArrayUniform(elementCount);
    }

    @Override
    protected void push(int handle, Object[] value) {
        uniformBuffer.clear();
        for (int i = 0; i < elementCount; i++) {
            Matrix4Double m = (Matrix4Double) value[i];
            for (int j = 0; j < 16; j++)
                uniformBuffer.put((float) m.val[j]);
        }
        uniformBuffer.flip();
        PlatformRuntime.gl.glUniformMatrix4fv(handle, elementCount, false, uniformBuffer);
    }

    @Override
    protected void applyValue(Object[] value) {
        Matrix4Double[] dst = (Matrix4Double[]) this.value;
        for (int i = 0; i < Math.min(value.length, elementCount); i++)
            dst[i].set((Matrix4Double) value[i]);
    }

    public int elementCount() {
        return elementCount;
    }
}
