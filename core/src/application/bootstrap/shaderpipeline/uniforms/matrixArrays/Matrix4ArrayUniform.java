package application.bootstrap.shaderpipeline.uniforms.matrixArrays;

import java.nio.FloatBuffer;

import application.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import application.bootstrap.shaderpipeline.uniforms.UniformType;
import engine.root.EngineContext;
import engine.util.mathematics.matrices.Matrix4;
import engine.util.memory.BufferUtils;

public final class Matrix4ArrayUniform extends UniformAttributeStruct<Object[]> {

    private final int elementCount;
    private final FloatBuffer uniformBuffer;

    public Matrix4ArrayUniform(int elementCount) {
        super(UniformType.MATRIX4, elementCount, new Matrix4[elementCount]);
        this.elementCount = elementCount;
        this.uniformBuffer = BufferUtils.newFloatBuffer(elementCount * 16);
        for (int i = 0; i < elementCount; i++)
            ((Matrix4[]) value)[i] = new Matrix4();
    }

    @Override
    public UniformAttributeStruct<?> createDefault() {
        return new Matrix4ArrayUniform(elementCount);
    }

    @Override
    protected void push(int handle, Object[] value) {
        uniformBuffer.clear();
        for (int i = 0; i < elementCount; i++)
            uniformBuffer.put(((Matrix4) value[i]).val, 0, 16);
        uniformBuffer.flip();
        EngineContext.gl.glUniformMatrix4fv(handle, elementCount, false, uniformBuffer);
    }

    @Override
    protected void applyValue(Object[] value) {
        Matrix4[] dst = (Matrix4[]) this.value;
        for (int i = 0; i < Math.min(value.length, elementCount); i++)
            dst[i].set((Matrix4) value[i]);
    }

    @Override
    protected void applyObject(Object value) {
        if (value instanceof Matrix4[] m)
            applyValue(m);
        else
            throw new IllegalArgumentException("applyObject(Matrix4Array): got " + value.getClass());
    }

    public int elementCount() {
        return elementCount;
    }
}
