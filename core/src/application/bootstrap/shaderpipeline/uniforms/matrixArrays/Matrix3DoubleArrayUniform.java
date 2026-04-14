package application.bootstrap.shaderpipeline.uniforms.matrixArrays;

import java.nio.FloatBuffer;

import application.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import application.bootstrap.shaderpipeline.uniforms.UniformType;
import engine.root.EngineContext;
import engine.util.mathematics.matrices.Matrix3Double;
import engine.util.memory.BufferUtils;

public final class Matrix3DoubleArrayUniform extends UniformAttributeStruct<Object[]> {

    private final int elementCount;
    private final FloatBuffer uniformBuffer;

    public Matrix3DoubleArrayUniform(int elementCount) {
        super(UniformType.MATRIX3_DOUBLE, elementCount, new Matrix3Double[elementCount]);
        this.elementCount = elementCount;
        this.uniformBuffer = BufferUtils.newFloatBuffer(elementCount * 9);
        for (int i = 0; i < elementCount; i++)
            ((Matrix3Double[]) value)[i] = new Matrix3Double();
    }

    @Override
    public UniformAttributeStruct<?> createDefault() {
        return new Matrix3DoubleArrayUniform(elementCount);
    }

    @Override
    protected void push(int handle, Object[] value) {
        uniformBuffer.clear();
        for (int i = 0; i < elementCount; i++) {
            Matrix3Double m = (Matrix3Double) value[i];
            for (int j = 0; j < 9; j++)
                uniformBuffer.put((float) m.val[j]);
        }
        uniformBuffer.flip();
        EngineContext.gl20.glUniformMatrix3fv(handle, elementCount, false, uniformBuffer);
    }

    @Override
    protected void applyValue(Object[] value) {
        Matrix3Double[] dst = (Matrix3Double[]) this.value;
        for (int i = 0; i < Math.min(value.length, elementCount); i++)
            dst[i].set((Matrix3Double) value[i]);
    }

    public int elementCount() {
        return elementCount;
    }
}
