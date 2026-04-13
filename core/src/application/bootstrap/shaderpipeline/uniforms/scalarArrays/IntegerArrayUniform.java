package application.bootstrap.shaderpipeline.uniforms.scalarArrays;

import org.lwjgl.opengl.GL20C;
import org.lwjgl.system.MemoryStack;

import application.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import application.bootstrap.shaderpipeline.uniforms.UniformType;

public final class IntegerArrayUniform extends UniformAttributeStruct<Object[]> {

    private final int elementCount;

    public IntegerArrayUniform(int elementCount) {
        super(UniformType.INT, elementCount, new Integer[elementCount]);
        this.elementCount = elementCount;
        for (int i = 0; i < elementCount; i++)
            ((Integer[]) value)[i] = 0;
    }

    @Override
    public UniformAttributeStruct<?> createDefault() {
        return new IntegerArrayUniform(elementCount);
    }

    @Override
    protected void push(int handle, Object[] value) {
        int[] flat = new int[elementCount];
        for (int i = 0; i < elementCount; i++)
            flat[i] = (Integer) value[i];
        try (MemoryStack stack = MemoryStack.stackPush()) {
            java.nio.IntBuffer buf = stack.mallocInt(elementCount);
            buf.put(flat).flip();
            GL20C.glUniform1iv(handle, buf);
        }
    }

    @Override
    protected void applyValue(Object[] value) {
        Integer[] dst = (Integer[]) this.value;
        for (int i = 0; i < Math.min(value.length, elementCount); i++)
            dst[i] = (Integer) value[i];
    }

    public int elementCount() {
        return elementCount;
    }
}
