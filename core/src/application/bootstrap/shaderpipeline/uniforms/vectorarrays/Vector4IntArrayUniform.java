package application.bootstrap.shaderpipeline.uniforms.vectorarrays;

import org.lwjgl.opengl.GL20C;
import org.lwjgl.system.MemoryStack;

import application.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import application.bootstrap.shaderpipeline.uniforms.UniformType;
import engine.util.mathematics.vectors.Vector4Int;

public final class Vector4IntArrayUniform extends UniformAttributeStruct<Object[]> {

    private final int elementCount;

    public Vector4IntArrayUniform(int elementCount) {
        super(UniformType.VECTOR4_INT, elementCount, new Vector4Int[elementCount]);
        this.elementCount = elementCount;
        for (int i = 0; i < elementCount; i++)
            ((Vector4Int[]) value)[i] = new Vector4Int();
    }

    @Override
    public UniformAttributeStruct<?> createDefault() {
        return new Vector4IntArrayUniform(elementCount);
    }

    @Override
    protected void push(int handle, Object[] value) {
        int[] flat = new int[elementCount * 4];
        for (int i = 0; i < elementCount; i++) {
            Vector4Int v = (Vector4Int) value[i];
            flat[i * 4] = v.x;
            flat[i * 4 + 1] = v.y;
            flat[i * 4 + 2] = v.z;
            flat[i * 4 + 3] = v.w;
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            java.nio.IntBuffer buf = stack.mallocInt(elementCount * 4);
            buf.put(flat).flip();
            GL20C.glUniform4iv(handle, buf);
        }
    }

    @Override
    protected void applyValue(Object[] value) {
        Vector4Int[] dst = (Vector4Int[]) this.value;
        for (int i = 0; i < Math.min(value.length, elementCount); i++)
            dst[i].set((Vector4Int) value[i]);
    }

    public int elementCount() {
        return elementCount;
    }
}
