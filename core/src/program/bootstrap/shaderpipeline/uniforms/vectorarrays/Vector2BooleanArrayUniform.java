package program.bootstrap.shaderpipeline.uniforms.vectorarrays;

import program.core.app.CoreContext;
import program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import program.bootstrap.shaderpipeline.uniforms.UniformType;
import program.core.util.mathematics.vectors.Vector2Boolean;

public final class Vector2BooleanArrayUniform extends UniformAttributeStruct<Object[]> {

    private final int elementCount;

    public Vector2BooleanArrayUniform(int elementCount) {
        super(UniformType.VECTOR2_BOOLEAN, elementCount, new Vector2Boolean[elementCount]);
        this.elementCount = elementCount;
        for (int i = 0; i < elementCount; i++)
            ((Vector2Boolean[]) value)[i] = new Vector2Boolean();
    }

    @Override
    public UniformAttributeStruct<?> createDefault() {
        return new Vector2BooleanArrayUniform(elementCount);
    }

    @Override
    protected void push(int handle, Object[] value) {
        int[] flat = new int[elementCount * 2];
        for (int i = 0; i < elementCount; i++) {
            Vector2Boolean v = (Vector2Boolean) value[i];
            flat[i * 2] = v.x ? 1 : 0;
            flat[i * 2 + 1] = v.y ? 1 : 0;
        }
        CoreContext.gl.glUniform2iv(handle, elementCount, flat, 0);
    }

    @Override
    protected void applyValue(Object[] value) {
        Vector2Boolean[] dst = (Vector2Boolean[]) this.value;
        for (int i = 0; i < Math.min(value.length, elementCount); i++)
            dst[i].set((Vector2Boolean) value[i]);
    }

    public int elementCount() {
        return elementCount;
    }
}
