package application.bootstrap.shaderpipeline.uniforms.matrixarrays;

import java.nio.FloatBuffer;

import application.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import application.bootstrap.shaderpipeline.uniforms.UniformType;
import engine.root.EngineContext;
import engine.root.EngineSetting;
import engine.util.mathematics.matrices.Matrix2;
import engine.util.memory.BufferUtility;

public final class Matrix2ArrayUniform extends UniformAttributeStruct<Object[]> {

    /*
     * GLSL mat2 array uniform. Elements are packed into a preallocated
     * direct buffer on push, so uploads never allocate.
     */

    // Internal
    private final int elementCount;
    private final FloatBuffer uniformBuffer;

    // Constructor \\

    public Matrix2ArrayUniform(int elementCount) {

        super(UniformType.MATRIX2, elementCount, new Matrix2[elementCount]);

        this.elementCount = elementCount;
        this.uniformBuffer = BufferUtility.newFloatBuffer(elementCount * EngineSetting.MATRIX2_ELEMENT_COUNT);

        for (int i = 0; i < elementCount; i++)
            ((Matrix2[]) value)[i] = new Matrix2();
    }

    @Override
    public UniformAttributeStruct<?> createDefault() {
        return new Matrix2ArrayUniform(elementCount);
    }

    // Push \\

    @Override
    protected void push(int handle, Object[] value) {

        uniformBuffer.clear();

        for (int i = 0; i < elementCount; i++) {
            Matrix2 matrix = (Matrix2) value[i];
            uniformBuffer.put(matrix.val, 0, EngineSetting.MATRIX2_ELEMENT_COUNT);
        }

        uniformBuffer.flip();
        EngineContext.gl20.glUniformMatrix2fv(handle, elementCount, false, uniformBuffer);
    }

    // Accessible \\

    @Override
    protected void applyValue(Object[] value) {

        Matrix2[] target = (Matrix2[]) this.value;

        for (int i = 0; i < Math.min(value.length, elementCount); i++)
            target[i].set((Matrix2) value[i]);
    }

    @Override
    protected void applyObject(Object value) {

        if (value instanceof Matrix2[] matrices)
            applyValue(matrices);
        else
            throwException("Matrix2ArrayUniform expects Matrix2[], got " + value.getClass().getSimpleName());
    }

    public int elementCount() {
        return elementCount;
    }
}
