package com.internal.bootstrap.shaderpipeline.uniforms.scalarArrays;

import com.badlogic.gdx.Gdx;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformType;

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
        Gdx.gl.glUniform1fv(handle, elementCount, flat, 0);
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