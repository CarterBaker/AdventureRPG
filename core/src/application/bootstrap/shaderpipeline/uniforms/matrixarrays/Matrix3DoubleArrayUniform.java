package application.bootstrap.shaderpipeline.uniforms.matrixarrays;

import java.nio.FloatBuffer;

import application.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import application.bootstrap.shaderpipeline.uniforms.UniformType;
import engine.root.EngineContext;
import engine.root.EngineSetting;
import engine.util.mathematics.matrices.Matrix3Double;
import engine.util.memory.BufferUtility;

public final class Matrix3DoubleArrayUniform extends UniformAttributeStruct<Object[]> {

    /*
     * GLSL dmat3 array uniform. Elements are packed into a preallocated
     * direct buffer on push, so uploads never allocate.
     */

    // Internal
    private final int elementCount;
    private final FloatBuffer uniformBuffer;

    // Constructor \\

    public Matrix3DoubleArrayUniform(int elementCount) {

        super(UniformType.MATRIX3_DOUBLE, elementCount, new Matrix3Double[elementCount]);

        this.elementCount = elementCount;
        this.uniformBuffer = BufferUtility.newFloatBuffer(elementCount * EngineSetting.MATRIX3_ELEMENT_COUNT);

        for (int i = 0; i < elementCount; i++)
            ((Matrix3Double[]) value)[i] = new Matrix3Double();
    }

    @Override
    public UniformAttributeStruct<?> createDefault() {
        return new Matrix3DoubleArrayUniform(elementCount);
    }

    // Push \\

    @Override
    protected void push(int handle, Object[] value) {

        uniformBuffer.clear();

        for (int i = 0; i < elementCount; i++) {
            Matrix3Double matrix = (Matrix3Double) value[i];
            for (int j = 0; j < EngineSetting.MATRIX3_ELEMENT_COUNT; j++)
                uniformBuffer.put((float) matrix.val[j]);
        }

        uniformBuffer.flip();
        EngineContext.gl20.glUniformMatrix3fv(handle, elementCount, false, uniformBuffer);
    }

    // Accessible \\

    @Override
    protected void applyValue(Object[] value) {

        Matrix3Double[] target = (Matrix3Double[]) this.value;

        for (int i = 0; i < Math.min(value.length, elementCount); i++)
            target[i].set((Matrix3Double) value[i]);
    }

    @Override
    protected void applyObject(Object value) {

        if (value instanceof Matrix3Double[] matrices)
            applyValue(matrices);
        else
            throwException("Matrix3DoubleArrayUniform expects Matrix3Double[], got "
                    + value.getClass().getSimpleName());
    }

    public int elementCount() {
        return elementCount;
    }
}
