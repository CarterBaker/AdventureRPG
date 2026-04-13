package application.bootstrap.shaderpipeline.uniforms.vectorarrays;

import org.lwjgl.opengl.GL20C;
import org.lwjgl.system.MemoryStack;

import application.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import application.bootstrap.shaderpipeline.uniforms.UniformType;
import application.core.util.mathematics.vectors.Vector2Double;

public final class Vector2DoubleArrayUniform extends UniformAttributeStruct<Object[]> {

    private final int elementCount;

    public Vector2DoubleArrayUniform(int elementCount) {
        super(UniformType.VECTOR2_DOUBLE, elementCount, new Vector2Double[elementCount]);
        this.elementCount = elementCount;
        for (int i = 0; i < elementCount; i++)
            ((Vector2Double[]) value)[i] = new Vector2Double();
    }

    @Override
    public UniformAttributeStruct<?> createDefault() {
        return new Vector2DoubleArrayUniform(elementCount);
    }

    @Override
    protected void push(int handle, Object[] value) {
        float[] flat = new float[elementCount * 2];
        for (int i = 0; i < elementCount; i++) {
            Vector2Double v = (Vector2Double) value[i];
            flat[i * 2] = (float) v.x;
            flat[i * 2 + 1] = (float) v.y;
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            java.nio.FloatBuffer buf = stack.mallocFloat(elementCount * 2);
            buf.put(flat).flip();
            GL20C.glUniform2fv(handle, buf);
        }
    }

    @Override
    protected void applyValue(Object[] value) {
        Vector2Double[] dst = (Vector2Double[]) this.value;
        for (int i = 0; i < Math.min(value.length, elementCount); i++)
            dst[i].set((Vector2Double) value[i]);
    }

    public int elementCount() {
        return elementCount;
    }
}
