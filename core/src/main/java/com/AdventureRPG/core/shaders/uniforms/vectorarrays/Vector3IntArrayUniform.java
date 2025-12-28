package com.AdventureRPG.core.shaders.uniforms.vectorarrays;

import com.AdventureRPG.core.shaders.uniforms.UniformAttribute;
import com.AdventureRPG.core.util.Mathematics.Vectors.Vector3Int;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public final class Vector3IntArrayUniform extends UniformAttribute<int[]> {

    // Internal
    private final int elementCount;
    private final ByteBuffer uboBuffer;
    private final int[] uniformData;
    private final IntBuffer uniformBuffer;

    public Vector3IntArrayUniform(int elementCount) {

        // Internal
        super(new int[elementCount * 4]);
        this.elementCount = elementCount;
        this.uboBuffer = BufferUtils.newByteBuffer(elementCount * 16);
        this.uniformData = new int[elementCount * 3];
        this.uniformBuffer = uboBuffer.asIntBuffer();
    }

    @Override
    protected void push(int handle, int[] data) {
        Gdx.gl.glUniform3iv(handle, elementCount, uniformData, 0);
    }

    @Override
    public ByteBuffer getByteBuffer() {

        uniformBuffer.clear();

        // Write each vector with padding
        for (int i = 0; i < elementCount; i++) {
            uniformBuffer.put(value[i * 4]); // x
            uniformBuffer.put(value[i * 4 + 1]); // y
            uniformBuffer.put(value[i * 4 + 2]); // z
            uniformBuffer.put(0); // padding
        }

        uniformBuffer.flip();
        return uboBuffer;
    }

    public void set(Vector3Int[] vectors) {

        for (int i = 0; i < vectors.length && i < elementCount; i++) {

            value[i * 4] = vectors[i].x;
            value[i * 4 + 1] = vectors[i].y;
            value[i * 4 + 2] = vectors[i].z;
            value[i * 4 + 3] = 0;

            uniformData[i * 3] = vectors[i].x;
            uniformData[i * 3 + 1] = vectors[i].y;
            uniformData[i * 3 + 2] = vectors[i].z;
        }
    }

    @Override
    public void set(int[] value) {

        System.arraycopy(value, 0, this.value, 0, Math.min(value.length, this.value.length));

        for (int i = 0; i < elementCount; i++) {
            uniformData[i * 3] = this.value[i * 4];
            uniformData[i * 3 + 1] = this.value[i * 4 + 1];
            uniformData[i * 3 + 2] = this.value[i * 4 + 2];
        }
    }

    public int elementCount() {
        return elementCount;
    }
}
