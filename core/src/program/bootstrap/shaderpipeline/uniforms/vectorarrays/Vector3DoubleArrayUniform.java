package program.bootstrap.shaderpipeline.uniforms.vectorarrays;

import org.lwjgl.opengl.GL20C;
import org.lwjgl.system.MemoryStack;

import program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import program.bootstrap.shaderpipeline.uniforms.UniformType;
import program.core.util.mathematics.vectors.Vector3Double;

public final class Vector3DoubleArrayUniform extends UniformAttributeStruct<Object[]> {

    private final int elementCount;

    public Vector3DoubleArrayUniform(int elementCount) {
        super(UniformType.VECTOR3_DOUBLE, elementCount, new Vector3Double[elementCount]);
        this.elementCount = elementCount;
        for (int i = 0; i < elementCount; i++)
            ((Vector3Double[]) value)[i] = new Vector3Double();
    }

    @Override
    public UniformAttributeStruct<?> createDefault() {
        return new Vector3DoubleArrayUniform(elementCount);
    }

    @Override
    protected void push(int handle, Object[] value) {
        float[] flat = new float[elementCount * 3];
        for (int i = 0; i < elementCount; i++) {
            Vector3Double v = (Vector3Double) value[i];
            flat[i * 3] = (float) v.x;
            flat[i * 3 + 1] = (float) v.y;
            flat[i * 3 + 2] = (float) v.z;
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            java.nio.FloatBuffer buf = stack.mallocFloat(elementCount * 3);
            buf.put(flat).flip();
            GL20C.glUniform3fv(handle, buf);
        }
    }

    @Override
    protected void applyValue(Object[] value) {
        Vector3Double[] dst = (Vector3Double[]) this.value;
        for (int i = 0; i < Math.min(value.length, elementCount); i++)
            dst[i].set((Vector3Double) value[i]);
    }

    public int elementCount() {
        return elementCount;
    }
}
