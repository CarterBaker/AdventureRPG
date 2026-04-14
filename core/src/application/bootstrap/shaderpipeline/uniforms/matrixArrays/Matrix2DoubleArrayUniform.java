package application.bootstrap.shaderpipeline.uniforms.matrixArrays;

import java.nio.FloatBuffer;

import application.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import application.bootstrap.shaderpipeline.uniforms.UniformType;
import engine.root.EngineContext;
import engine.util.mathematics.matrices.Matrix2Double;
import engine.util.memory.BufferUtils;

public final class Matrix2DoubleArrayUniform extends UniformAttributeStruct<Object[]> {

    private final int elementCount;
    private final FloatBuffer uniformBuffer;

    public Matrix2DoubleArrayUniform(int elementCount) {
        super(UniformType.MATRIX2_DOUBLE, elementCount, new Matrix2Double[elementCount]);
        this.elementCount = elementCount;
        this.uniformBuffer = BufferUtils.newFloatBuffer(elementCount * 4);
        for (int i = 0; i < elementCount; i++)
            ((Matrix2Double[]) value)[i] = new Matrix2Double();
    }

    @Override
    public UniformAttributeStruct<?> createDefault() {
        return new Matrix2DoubleArrayUniform(elementCount);
    }

    @Override
    protected void push(int handle, Object[] value) {
        uniformBuffer.clear();
        for (int i = 0; i < elementCount; i++) {
            Matrix2Double m = (Matrix2Double) value[i];
            uniformBuffer.put((float) m.val[0]);
            uniformBuffer.put((float) m.val[1]);
            uniformBuffer.put((float) m.val[2]);
            uniformBuffer.put((float) m.val[3]);
        }
        uniformBuffer.flip();
        EngineContext.gl20.glUniformMatrix2fv(handle, elementCount, false, uniformBuffer);
    }

    @Override
    protected void applyValue(Object[] value) {
        Matrix2Double[] dst = (Matrix2Double[]) this.value;
        for (int i = 0; i < Math.min(value.length, elementCount); i++)
            dst[i].set((Matrix2Double) value[i]);
    }

    public int elementCount() {
        return elementCount;
    }
}
