package application.bootstrap.shaderpipeline.uniforms.matrixarrays;

import java.nio.FloatBuffer;

import application.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import application.bootstrap.shaderpipeline.uniforms.UniformType;
import engine.root.EngineContext;
import engine.root.EngineSetting;
import engine.util.mathematics.matrices.Matrix4;
import engine.util.memory.BufferUtility;

public final class Matrix4ArrayUniform extends UniformAttributeStruct<Object[]> {

    /*
     * GLSL mat4 array uniform. Elements are packed into a preallocated
     * direct buffer on push, so uploads never allocate.
     */

    // Internal
    private final int elementCount;
    private final FloatBuffer uniformBuffer;

    // Constructor \\

    public Matrix4ArrayUniform(int elementCount) {

        super(UniformType.MATRIX4, elementCount, new Matrix4[elementCount]);

        this.elementCount = elementCount;
        this.uniformBuffer = BufferUtility.newFloatBuffer(elementCount * EngineSetting.MATRIX4_ELEMENT_COUNT);

        for (int i = 0; i < elementCount; i++)
            ((Matrix4[]) value)[i] = new Matrix4();
    }

    @Override
    public UniformAttributeStruct<?> createDefault() {
        return new Matrix4ArrayUniform(elementCount);
    }

    // Push \\

    @Override
    protected void push(int handle, Object[] value) {

        uniformBuffer.clear();

        for (int i = 0; i < elementCount; i++) {
            Matrix4 matrix = (Matrix4) value[i];
            uniformBuffer.put(matrix.val, 0, EngineSetting.MATRIX4_ELEMENT_COUNT);
        }

        uniformBuffer.flip();
        EngineContext.gl20.glUniformMatrix4fv(handle, elementCount, false, uniformBuffer);
    }

    // Accessible \\

    @Override
    protected void applyValue(Object[] value) {

        Matrix4[] target = (Matrix4[]) this.value;

        for (int i = 0; i < Math.min(value.length, elementCount); i++)
            target[i].set((Matrix4) value[i]);
    }

    @Override
    protected void applyObject(Object value) {

        if (value instanceof Matrix4[] matrices)
            applyValue(matrices);
        else
            throwException("Matrix4ArrayUniform expects Matrix4[], got " + value.getClass().getSimpleName());
    }

    public int elementCount() {
        return elementCount;
    }
}
