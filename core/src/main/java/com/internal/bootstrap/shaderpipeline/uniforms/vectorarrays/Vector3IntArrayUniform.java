package com.internal.bootstrap.shaderpipeline.uniforms.vectorarrays;

import com.badlogic.gdx.Gdx;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformType;
import com.internal.core.util.mathematics.vectors.Vector3Int;

public final class Vector3IntArrayUniform extends UniformAttributeStruct<Object[]> {

    private final int elementCount;

    public Vector3IntArrayUniform(int elementCount) {
        super(UniformType.VECTOR3_INT, elementCount, new Vector3Int[elementCount]);
        this.elementCount = elementCount;
        for (int i = 0; i < elementCount; i++)
            ((Vector3Int[]) value)[i] = new Vector3Int();
    }

    @Override
    public UniformAttributeStruct<?> createDefault() {
        return new Vector3IntArrayUniform(elementCount);
    }

    @Override
    protected void push(int handle, Object[] value) {
        int[] flat = new int[elementCount * 3];
        for (int i = 0; i < elementCount; i++) {
            Vector3Int v = (Vector3Int) value[i];
            flat[i * 3] = v.x;
            flat[i * 3 + 1] = v.y;
            flat[i * 3 + 2] = v.z;
        }
        Gdx.gl.glUniform3iv(handle, elementCount, flat, 0);
    }

    @Override
    protected void applyValue(Object[] value) {
        Vector3Int[] dst = (Vector3Int[]) this.value;
        for (int i = 0; i < Math.min(value.length, elementCount); i++)
            dst[i].set((Vector3Int) value[i]);
    }

    public int elementCount() {
        return elementCount;
    }
}