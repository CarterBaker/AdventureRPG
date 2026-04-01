package program.bootstrap.shaderpipeline.uniforms.scalarArrays;

import program.core.app.CoreContext;
import program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import program.bootstrap.shaderpipeline.uniforms.UniformType;

public final class BooleanArrayUniform extends UniformAttributeStruct<Object[]> {

    private final int elementCount;

    public BooleanArrayUniform(int elementCount) {
        super(UniformType.BOOL, elementCount, new Boolean[elementCount]);
        this.elementCount = elementCount;
        for (int i = 0; i < elementCount; i++)
            ((Boolean[]) value)[i] = false;
    }

    @Override
    public UniformAttributeStruct<?> createDefault() {
        return new BooleanArrayUniform(elementCount);
    }

    @Override
    protected void push(int handle, Object[] value) {
        int[] flat = new int[elementCount];
        for (int i = 0; i < elementCount; i++)
            flat[i] = (Boolean) value[i] ? 1 : 0;
        CoreContext.gl.glUniform1iv(handle, elementCount, flat, 0);
    }

    @Override
    protected void applyValue(Object[] value) {
        Boolean[] dst = (Boolean[]) this.value;
        for (int i = 0; i < Math.min(value.length, elementCount); i++)
            dst[i] = (Boolean) value[i];
    }

    public int elementCount() {
        return elementCount;
    }
}
