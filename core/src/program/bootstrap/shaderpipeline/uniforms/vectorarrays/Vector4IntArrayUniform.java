package program.bootstrap.shaderpipeline.uniforms.vectorarrays;

import program.core.app.CoreContext;
import program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import program.bootstrap.shaderpipeline.uniforms.UniformType;
import program.core.util.mathematics.vectors.Vector4Int;

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
        CoreContext.gl.glUniform4iv(handle, elementCount, flat, 0);
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
