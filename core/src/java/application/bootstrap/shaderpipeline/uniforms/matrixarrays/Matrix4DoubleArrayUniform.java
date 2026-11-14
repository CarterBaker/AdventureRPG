package application.bootstrap.shaderpipeline.uniforms.matrixarrays;

import java.nio.FloatBuffer;

import application.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import application.bootstrap.shaderpipeline.uniforms.UniformType;
import engine.root.EngineContext;
import engine.root.EngineSetting;
import engine.util.mathematics.matrices.Matrix4Double;
import engine.util.memory.BufferUtility;

public final class Matrix4DoubleArrayUniform extends UniformAttributeStruct<Object[]> {

    /*
     * GLSL dmat4 array uniform. Elements are packed into a preallocated
     * direct buffer on push, so uploads never allocate.
     */

    // Internal
    private final int elementCount;
    private final FloatBuffer uniformBuffer;

    // Constructor \\

    public Matrix4DoubleArrayUniform(int elementCount) {

        super(UniformType.MATRIX4_DOUBLE, elementCount, new Matrix4Double[elementCount]);

        this.elementCount = elementCount;
        this.uniformBuffer = BufferUtility.newFloatBuffer(elementCount * EngineSetting.MATRIX4_ELEMENT_COUNT);

        for (int i = 0; i < elementCount; i++)
            ((Matrix4Double[]) value)[i] = new Matrix4Double();
    }

    @Override
    public UniformAttributeStruct<?> createDefault() {
        return new Matrix4DoubleArrayUniform(elementCount);
    }

    // Push \\

    @Override
    protected void push(int handle, Object[] value) {

        uniformBuffer.clear();

        for (int i = 0; i < elementCount; i++) {
            Matrix4Double matrix = (Matrix4Double) value[i];
            for (int j = 0; j < EngineSetting.MATRIX4_ELEMENT_COUNT; j++)
                uniformBuffer.put((float) matrix.val[j]);
        }

        uniformBuffer.flip();
        EngineContext.gl20.glUniformMatrix4fv(handle, elementCount, false, uniformBuffer);
    }

    // Accessible \\

    @Override
    protected void applyValue(Object[] value) {

        Matrix4Double[] target = (Matrix4Double[]) this.value;

        for (int i = 0; i < Math.min(value.length, elementCount); i++)
            target[i].set((Matrix4Double) value[i]);
    }

    @Override
    protected void applyObject(Object value) {

        if (value instanceof Matrix4Double[] matrices)
            applyValue(matrices);
        else
            throwException("Matrix4DoubleArrayUniform expects Matrix4Double[], got "
                    + value.getClass().getSimpleName());
    }

    public int elementCount() {
        return elementCount;
    }
}
