package program.bootstrap.shaderpipeline.uniforms.vectorarrays;

import program.core.app.CoreContext;
import program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import program.bootstrap.shaderpipeline.uniforms.UniformType;
import program.core.util.mathematics.vectors.Vector2;

public final class Vector2ArrayUniform extends UniformAttributeStruct<Object[]> {

    private final int elementCount;

    public Vector2ArrayUniform(int elementCount) {
        super(UniformType.VECTOR2, elementCount, new Vector2[elementCount]);
        this.elementCount = elementCount;
        for (int i = 0; i < elementCount; i++)
            ((Vector2[]) value)[i] = new Vector2();
    }

    @Override
    public UniformAttributeStruct<?> createDefault() {
        return new Vector2ArrayUniform(elementCount);
    }

    @Override
    protected void push(int handle, Object[] value) {
        float[] flat = new float[elementCount * 2];
        for (int i = 0; i < elementCount; i++) {
            Vector2 v = (Vector2) value[i];
            flat[i * 2] = v.x;
            flat[i * 2 + 1] = v.y;
        }
        CoreContext.gl.glUniform2fv(handle, elementCount, flat, 0);
    }

    @Override
    protected void applyValue(Object[] value) {
        Vector2[] dst = (Vector2[]) this.value;
        for (int i = 0; i < Math.min(value.length, elementCount); i++)
            dst[i].set((Vector2) value[i]);
    }

    @Override
    protected void applyObject(Object value) {
        if (value instanceof Vector2[] v)
            applyValue(v);
        else if (value instanceof program.core.util.mathematics.vectors.Vector2[] vectors) {
            Vector2[] dst = (Vector2[]) this.value;
            for (int i = 0; i < Math.min(vectors.length, elementCount); i++) {
                dst[i].x = vectors[i].x;
                dst[i].y = vectors[i].y;
            }
        } else
            throw new IllegalArgumentException("applyObject(Vector2Array): got " + value.getClass());
    }

    public int elementCount() {
        return elementCount;
    }
}
