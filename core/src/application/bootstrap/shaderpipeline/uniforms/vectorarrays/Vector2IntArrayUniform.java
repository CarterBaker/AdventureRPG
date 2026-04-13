package application.bootstrap.shaderpipeline.uniforms.vectorarrays;

import org.lwjgl.opengl.GL20C;
import org.lwjgl.system.MemoryStack;

import application.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import application.bootstrap.shaderpipeline.uniforms.UniformType;
import application.core.util.mathematics.vectors.Vector2Int;

public final class Vector2IntArrayUniform extends UniformAttributeStruct<Object[]> {

    private final int elementCount;

    public Vector2IntArrayUniform(int elementCount) {
        super(UniformType.VECTOR2_INT, elementCount, new Vector2Int[elementCount]);
        this.elementCount = elementCount;
        for (int i = 0; i < elementCount; i++)
            ((Vector2Int[]) value)[i] = new Vector2Int();
    }

    @Override
    public UniformAttributeStruct<?> createDefault() {
        return new Vector2IntArrayUniform(elementCount);
    }

    @Override
    protected void push(int handle, Object[] value) {
        int[] flat = new int[elementCount * 2];
        for (int i = 0; i < elementCount; i++) {
            Vector2Int v = (Vector2Int) value[i];
            flat[i * 2] = v.x;
            flat[i * 2 + 1] = v.y;
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            java.nio.IntBuffer buf = stack.mallocInt(elementCount * 2);
            buf.put(flat).flip();
            GL20C.glUniform2iv(handle, buf);
        }
    }

    @Override
    protected void applyValue(Object[] value) {
        Vector2Int[] dst = (Vector2Int[]) this.value;
        for (int i = 0; i < Math.min(value.length, elementCount); i++)
            dst[i].set((Vector2Int) value[i]);
    }

    public int elementCount() {
        return elementCount;
    }
}
