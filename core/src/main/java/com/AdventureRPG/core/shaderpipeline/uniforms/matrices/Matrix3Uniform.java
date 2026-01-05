package com.AdventureRPG.core.shaderpipeline.uniforms.matrices;

import com.AdventureRPG.core.shaderpipeline.uniforms.UniformAttribute;
import com.AdventureRPG.core.util.mathematics.matrices.Matrix3;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;
import java.nio.ByteBuffer;

public class Matrix3Uniform extends UniformAttribute<Object> {

    // Internal
    private final ByteBuffer uboBuffer;

    public Matrix3Uniform() {

        // Internal
        super(new Matrix3());
        this.uboBuffer = BufferUtils.newByteBuffer(48); // (std140): 3 columns * (vector4) 4 floats * 4 bytes = 48 bytes
    }

    @Override
    protected void push(int handle, Object value) {

        // Push libGDX matrix
        if (value instanceof com.badlogic.gdx.math.Matrix3 gdxMatrix)
            Gdx.gl.glUniformMatrix3fv(handle, 1, false, gdxMatrix.val, 0);

        // Push internal matrix
        else if (value instanceof Matrix3 internalMatrix)
            Gdx.gl.glUniformMatrix3fv(handle, 1, false, internalMatrix.val, 0);
    }

    @Override
    public ByteBuffer getByteBuffer() {

        Matrix3 matrix = (Matrix3) value;
        uboBuffer.clear();

        // Column 0
        uboBuffer.putFloat(matrix.val[0]); // m00
        uboBuffer.putFloat(matrix.val[1]); // m10
        uboBuffer.putFloat(matrix.val[2]); // m20
        uboBuffer.putFloat(0f); // padding

        // Column 1
        uboBuffer.putFloat(matrix.val[3]); // m01
        uboBuffer.putFloat(matrix.val[4]); // m11
        uboBuffer.putFloat(matrix.val[5]); // m21
        uboBuffer.putFloat(0f); // padding

        // Column 2
        uboBuffer.putFloat(matrix.val[6]); // m02
        uboBuffer.putFloat(matrix.val[7]); // m12
        uboBuffer.putFloat(matrix.val[8]); // m22
        uboBuffer.putFloat(0f); // padding

        uboBuffer.flip();
        return uboBuffer;
    }

    @Override
    public void set(Object value) {

        Matrix3 target = (Matrix3) this.value;

        // From libGDX matrix
        if (value instanceof com.badlogic.gdx.math.Matrix3 gdxMatrix)
            target.fromGDX(gdxMatrix);

        // From internal matrix
        else if (value instanceof Matrix3 internalMatrix)
            target.set(internalMatrix);

        else // TODO: Add my own error
            throw new IllegalArgumentException(
                    "Expected Matrix3 or com.badlogic.gdx.math.Matrix3, got " + value.getClass());
    }
}