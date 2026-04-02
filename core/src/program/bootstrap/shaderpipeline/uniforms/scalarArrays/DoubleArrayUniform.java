package program.bootstrap.shaderpipeline.uniforms.scalarArrays;

import org.lwjgl.opengl.GL20C;
import org.lwjgl.system.MemoryStack;

import program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import program.bootstrap.shaderpipeline.uniforms.UniformType;

public final class DoubleArrayUniform extends UniformAttributeStruct<Object[]> {

    private final int elementCount;

    public DoubleArrayUniform(int elementCount) {
        super(UniformType.DOUBLE, elementCount, new Double[elementCount]);
        this.elementCount = elementCount;
        for (int i = 0; i < elementCount; i++)
            ((Double[]) value)[i] = 0.0;
    }

    @Override
    public UniformAttributeStruct<?> createDefault() {
        return new DoubleArrayUniform(elementCount);
    }

    @Override
    protected void push(int handle, Object[] value) {
        float[] flat = new float[elementCount];
        for (int i = 0; i < elementCount; i++)
            flat[i] = ((Double) value[i]).floatValue();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            java.nio.FloatBuffer buf = stack.mallocFloat(elementCount);
            buf.put(flat).flip();
            GL20C.glUniform1fv(handle, buf);
        }
    }

    @Override
    protected void applyValue(Object[] value) {
        Double[] dst = (Double[]) this.value;
        for (int i = 0; i < Math.min(value.length, elementCount); i++)
            dst[i] = (Double) value[i];
    }

    public int elementCount() {
        return elementCount;
    }
}
