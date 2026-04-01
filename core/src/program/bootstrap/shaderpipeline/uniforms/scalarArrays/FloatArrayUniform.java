package program.bootstrap.shaderpipeline.uniforms.scalarArrays;

import program.core.app.CoreContext;
import program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import program.bootstrap.shaderpipeline.uniforms.UniformType;

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
        CoreContext.gl.glUniform1fv(handle, elementCount, flat, 0);
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
