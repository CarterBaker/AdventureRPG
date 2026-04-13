package application.bootstrap.shaderpipeline.uniforms.scalarArrays;

import org.lwjgl.opengl.GL20C;
import org.lwjgl.system.MemoryStack;

import application.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import application.bootstrap.shaderpipeline.uniforms.UniformType;

public final class FloatArrayUniform extends UniformAttributeStruct<Object[]> {

    private final int elementCount;

    public FloatArrayUniform(int elementCount) {
        super(UniformType.FLOAT, elementCount, new Float[elementCount]);
        this.elementCount = elementCount;
        for (int i = 0; i < elementCount; i++)
            ((Float[]) value)[i] = 0f;
    }

    @Override
    public UniformAttributeStruct<?> createDefault() {
        return new FloatArrayUniform(elementCount);
    }

    @Override
    protected void push(int handle, Object[] value) {
        float[] flat = new float[elementCount];
        for (int i = 0; i < elementCount; i++)
            flat[i] = (Float) value[i];
        try (MemoryStack stack = MemoryStack.stackPush()) {
            java.nio.FloatBuffer buf = stack.mallocFloat(elementCount);
            buf.put(flat).flip();
            GL20C.glUniform1fv(handle, buf);
        }
    }

    @Override
    protected void applyValue(Object[] value) {
        Float[] dst = (Float[]) this.value;
        for (int i = 0; i < Math.min(value.length, elementCount); i++)
            dst[i] = (Float) value[i];
    }

    public int elementCount() {
        return elementCount;
    }
}
