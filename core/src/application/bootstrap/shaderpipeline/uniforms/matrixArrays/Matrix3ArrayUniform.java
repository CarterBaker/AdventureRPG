package application.bootstrap.shaderpipeline.uniforms.matrixArrays;

import java.nio.FloatBuffer;

import application.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import application.bootstrap.shaderpipeline.uniforms.UniformType;
import engine.root.EngineContext;
import engine.util.mathematics.matrices.Matrix3;
import engine.util.memory.BufferUtils;

public final class Matrix3ArrayUniform extends UniformAttributeStruct<Object[]> {

    private final int elementCount;
    private final FloatBuffer uniformBuffer;

    public Matrix3ArrayUniform(int elementCount) {
        super(UniformType.MATRIX3, elementCount, new Matrix3[elementCount]);
        this.elementCount = elementCount;
        this.uniformBuffer = BufferUtils.newFloatBuffer(elementCount * 9);
        for (int i = 0; i < elementCount; i++)
            ((Matrix3[]) value)[i] = new Matrix3();
    }

    @Override
    public UniformAttributeStruct<?> createDefault() {
        return new Matrix3ArrayUniform(elementCount);
    }

    @Override
    protected void push(int handle, Object[] value) {
        uniformBuffer.clear();
        for (int i = 0; i < elementCount; i++)
            uniformBuffer.put(((Matrix3) value[i]).val, 0, 9);
        uniformBuffer.flip();
        EngineContext.gl20.glUniformMatrix3fv(handle, elementCount, false, uniformBuffer);
    }

    @Override
    protected void applyValue(Object[] value) {
        Matrix3[] dst = (Matrix3[]) this.value;
        for (int i = 0; i < Math.min(value.length, elementCount); i++)
            dst[i].set((Matrix3) value[i]);
    }

    @Override
    protected void applyObject(Object value) {
        if (value instanceof Matrix3[] m)
            applyValue(m);
        else
            throw new IllegalArgumentException("applyObject(Matrix3Array): got " + value.getClass());
    }

    public int elementCount() {
        return elementCount;
    }
}
