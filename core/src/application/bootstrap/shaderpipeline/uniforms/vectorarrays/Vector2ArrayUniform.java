package application.bootstrap.shaderpipeline.uniforms.vectorarrays;

import org.lwjgl.opengl.GL20C;
import org.lwjgl.system.MemoryStack;

import application.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import application.bootstrap.shaderpipeline.uniforms.UniformType;
import engine.util.mathematics.vectors.Vector2;

public final class Vector2ArrayUniform extends UniformAttributeStruct<Object[]> {

    private final int elementCount;

    public Vector2ArrayUniform(int elementCount) {
        super(UniformType.VECTOR2, elementCount, new Vector2[elementCount]);
        this.elementCount = elementCount;
        for (int i = 0; i < elementCount; i++)
            ((Vector2[]) value)[i] = new Vector2();
    }

    @Override
    public UniformAttributeStruct<?> createDefault() {
        return new Vector2ArrayUniform(elementCount);
    }

    @Override
    protected void push(int handle, Object[] value) {
        float[] flat = new float[elementCount * 2];
        for (int i = 0; i < elementCount; i++) {
            Vector2 v = (Vector2) value[i];
            flat[i * 2] = v.x;
            flat[i * 2 + 1] = v.y;
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            java.nio.FloatBuffer buf = stack.mallocFloat(elementCount * 2);
            buf.put(flat).flip();
            GL20C.glUniform2fv(handle, buf);
        }
    }

    @Override
    protected void applyValue(Object[] value) {
        Vector2[] dst = (Vector2[]) this.value;
        for (int i = 0; i < Math.min(value.length, elementCount); i++)
            dst[i].set((Vector2) value[i]);
    }

    @Override
    protected void applyObject(Object value) {
        if (value instanceof Vector2[] v)
            applyValue(v);
        else if (value instanceof engine.util.mathematics.vectors.Vector2[] vectors) {
            Vector2[] dst = (Vector2[]) this.value;
            for (int i = 0; i < Math.min(vectors.length, elementCount); i++) {
                dst[i].x = vectors[i].x;
                dst[i].y = vectors[i].y;
            }
        } else
            throw new IllegalArgumentException("applyObject(Vector2Array): got " + value.getClass());
    }

    public int elementCount() {
        return elementCount;
    }
}
