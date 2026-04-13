package application.bootstrap.shaderpipeline.uniforms.vectorarrays;

import org.lwjgl.opengl.GL20C;
import org.lwjgl.system.MemoryStack;

import application.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import application.bootstrap.shaderpipeline.uniforms.UniformType;
import engine.util.mathematics.vectors.Vector4Double;

public final class Vector4DoubleArrayUniform extends UniformAttributeStruct<Object[]> {

    private final int elementCount;

    public Vector4DoubleArrayUniform(int elementCount) {
        super(UniformType.VECTOR4_DOUBLE, elementCount, new Vector4Double[elementCount]);
        this.elementCount = elementCount;
        for (int i = 0; i < elementCount; i++)
            ((Vector4Double[]) value)[i] = new Vector4Double();
    }

    @Override
    public UniformAttributeStruct<?> createDefault() {
        return new Vector4DoubleArrayUniform(elementCount);
    }

    @Override
    protected void push(int handle, Object[] value) {
        float[] flat = new float[elementCount * 4];
        for (int i = 0; i < elementCount; i++) {
            Vector4Double v = (Vector4Double) value[i];
            flat[i * 4] = (float) v.x;
            flat[i * 4 + 1] = (float) v.y;
            flat[i * 4 + 2] = (float) v.z;
            flat[i * 4 + 3] = (float) v.w;
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            java.nio.FloatBuffer buf = stack.mallocFloat(elementCount * 4);
            buf.put(flat).flip();
            GL20C.glUniform4fv(handle, buf);
        }
    }

    @Override
    protected void applyValue(Object[] value) {
        Vector4Double[] dst = (Vector4Double[]) this.value;
        for (int i = 0; i < Math.min(value.length, elementCount); i++)
            dst[i].set((Vector4Double) value[i]);
    }

    public int elementCount() {
        return elementCount;
    }
}
