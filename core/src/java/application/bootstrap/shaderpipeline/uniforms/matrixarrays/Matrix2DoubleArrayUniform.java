package application.bootstrap.shaderpipeline.uniforms.matrixarrays;

import java.nio.FloatBuffer;

import application.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import application.bootstrap.shaderpipeline.uniforms.UniformType;
import engine.root.EngineContext;
import engine.root.EngineSetting;
import engine.util.mathematics.matrices.Matrix2Double;
import engine.util.memory.BufferUtility;

public final class Matrix2DoubleArrayUniform extends UniformAttributeStruct<Object[]> {

    /*
     * GLSL dmat2 array uniform. Elements are packed into a preallocated
     * direct buffer on push, so uploads never allocate.
     */

    // Internal
    private final int elementCount;
    private final FloatBuffer uniformBuffer;

    // Constructor \\

    public Matrix2DoubleArrayUniform(int elementCount) {

        super(UniformType.MATRIX2_DOUBLE, elementCount, new Matrix2Double[elementCount]);

        this.elementCount = elementCount;
        this.uniformBuffer = BufferUtility.newFloatBuffer(elementCount * EngineSetting.MATRIX2_ELEMENT_COUNT);

        for (int i = 0; i < elementCount; i++)
            ((Matrix2Double[]) value)[i] = new Matrix2Double();
    }

    @Override
    public UniformAttributeStruct<?> createDefault() {
        return new Matrix2DoubleArrayUniform(elementCount);
    }

    // Push \\

    @Override
    protected void push(int handle, Object[] value) {

        uniformBuffer.clear();

        for (int i = 0; i < elementCount; i++) {
            Matrix2Double matrix = (Matrix2Double) value[i];
            for (int j = 0; j < EngineSetting.MATRIX2_ELEMENT_COUNT; j++)
                uniformBuffer.put((float) matrix.val[j]);
        }

        uniformBuffer.flip();
        EngineContext.gl20.glUniformMatrix2fv(handle, elementCount, false, uniformBuffer);
    }

    // Accessible \\

    @Override
    protected void applyValue(Object[] value) {

        Matrix2Double[] target = (Matrix2Double[]) this.value;

        for (int i = 0; i < Math.min(value.length, elementCount); i++)
            target[i].set((Matrix2Double) value[i]);
    }

    @Override
    protected void applyObject(Object value) {

        if (value instanceof Matrix2Double[] matrices)
            applyValue(matrices);
        else
            throwException("Matrix2DoubleArrayUniform expects Matrix2Double[], got "
                    + value.getClass().getSimpleName());
    }

    public int elementCount() {
        return elementCount;
    }
}
