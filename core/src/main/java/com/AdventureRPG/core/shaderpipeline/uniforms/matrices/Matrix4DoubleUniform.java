package com.AdventureRPG.core.shaderpipeline.uniforms.matrices;

import com.AdventureRPG.core.shaderpipeline.uniforms.UniformAttribute;
import com.AdventureRPG.core.util.mathematics.matrices.Matrix4;
import com.AdventureRPG.core.util.mathematics.matrices.Matrix4Double;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;
import java.nio.ByteBuffer;

public class Matrix4DoubleUniform extends UniformAttribute<Matrix4Double> {

    // Internal
    private final ByteBuffer uboBuffer;
    private final Matrix4 uniformBuffer;

    public Matrix4DoubleUniform() {

        // Internal
        super(new Matrix4Double());
        this.uboBuffer = BufferUtils.newByteBuffer(64); // (std140): 4 columns * (vector4) 4 floats * 4 bytes = 64 bytes
        this.uniformBuffer = new Matrix4();
    }

    @Override
    protected void push(int handle, Matrix4Double value) {

        for (int i = 0; i < 16; i++)
            uniformBuffer.val[i] = (float) value.val[i];

        Gdx.gl.glUniformMatrix4fv(handle, 1, false, uniformBuffer.val, 0);
    }

    @Override
    public ByteBuffer getByteBuffer() {

        uboBuffer.clear();

        // Column 0
        uboBuffer.putFloat((float) value.val[0]); // m00
        uboBuffer.putFloat((float) value.val[1]); // m10
        uboBuffer.putFloat((float) value.val[2]); // m20
        uboBuffer.putFloat((float) value.val[3]); // m30

        // Column 1
        uboBuffer.putFloat((float) value.val[4]); // m01
        uboBuffer.putFloat((float) value.val[5]); // m11
        uboBuffer.putFloat((float) value.val[6]); // m21
        uboBuffer.putFloat((float) value.val[7]); // m31

        // Column 2
        uboBuffer.putFloat((float) value.val[8]); // m02
        uboBuffer.putFloat((float) value.val[9]); // m12
        uboBuffer.putFloat((float) value.val[10]); // m22
        uboBuffer.putFloat((float) value.val[11]); // m32

        // Column 3
        uboBuffer.putFloat((float) value.val[12]); // m03
        uboBuffer.putFloat((float) value.val[13]); // m13
        uboBuffer.putFloat((float) value.val[14]); // m23
        uboBuffer.putFloat((float) value.val[15]); // m33

        uboBuffer.flip();
        return uboBuffer;
    }

    @Override
    public void set(Matrix4Double value) {
        this.value.set(value);
    }
}