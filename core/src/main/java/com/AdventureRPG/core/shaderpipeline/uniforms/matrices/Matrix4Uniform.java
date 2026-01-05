package com.AdventureRPG.core.shaderpipeline.uniforms.matrices;

import com.AdventureRPG.core.shaderpipeline.uniforms.UniformAttribute;
import com.AdventureRPG.core.util.mathematics.matrices.Matrix4;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;
import java.nio.ByteBuffer;

public class Matrix4Uniform extends UniformAttribute<Object> {

    // Internal
    private final ByteBuffer uboBuffer;

    public Matrix4Uniform() {

        // Internal
        super(new Matrix4());
        this.uboBuffer = BufferUtils.newByteBuffer(64); // (std140): 4 columns * (vector4) 4 floats * 4 bytes = 64 bytes
    }

    @Override
    protected void push(int handle, Object value) {

        // Push libGDX matrix
        if (value instanceof com.badlogic.gdx.math.Matrix4 gdxMatrix)
            Gdx.gl.glUniformMatrix4fv(handle, 1, false, gdxMatrix.val, 0);

        // Push internal matrix
        else if (value instanceof Matrix4 internalMatrix)
            Gdx.gl.glUniformMatrix4fv(handle, 1, false, internalMatrix.val, 0);
    }

    @Override
    public ByteBuffer getByteBuffer() {

        Matrix4 matrix = (Matrix4) value;
        uboBuffer.clear();

        // Column 0
        uboBuffer.putFloat((float) matrix.val[0]); // m00
        uboBuffer.putFloat((float) matrix.val[1]); // m10
        uboBuffer.putFloat((float) matrix.val[2]); // m20
        uboBuffer.putFloat((float) matrix.val[3]); // m30

        // Column 1
        uboBuffer.putFloat((float) matrix.val[4]); // m01
        uboBuffer.putFloat((float) matrix.val[5]); // m11
        uboBuffer.putFloat((float) matrix.val[6]); // m21
        uboBuffer.putFloat((float) matrix.val[7]); // m31

        // Column 2
        uboBuffer.putFloat((float) matrix.val[8]); // m02
        uboBuffer.putFloat((float) matrix.val[9]); // m12
        uboBuffer.putFloat((float) matrix.val[10]); // m22
        uboBuffer.putFloat((float) matrix.val[11]); // m32

        // Column 3
        uboBuffer.putFloat((float) matrix.val[12]); // m03
        uboBuffer.putFloat((float) matrix.val[13]); // m13
        uboBuffer.putFloat((float) matrix.val[14]); // m23
        uboBuffer.putFloat((float) matrix.val[15]); // m33

        uboBuffer.flip();
        return uboBuffer;
    }

    @Override
    public void set(Object value) {

        Matrix4 target = (Matrix4) this.value;

        // From libGDX matrix
        if (value instanceof com.badlogic.gdx.math.Matrix4 gdxMatrix)
            target.fromGDX(gdxMatrix);

        // From internal Matrix4
        else if (value instanceof Matrix4 internalMatrix)
            target.set(internalMatrix);

        else // TODO: Add my own error
            throw new IllegalArgumentException(
                    "Expected Matrix4 or com.badlogic.gdx.math.Matrix4, got " + value.getClass());
    }
}