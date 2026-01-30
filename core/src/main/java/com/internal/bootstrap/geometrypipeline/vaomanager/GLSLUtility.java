package com.internal.bootstrap.geometrypipeline.vaomanager;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;
import com.internal.core.engine.UtilityPackage;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

class GLSLUtility extends UtilityPackage {

        static VAOHandle createVAO(VAOHandle vaoHandle, int floatsPerVert) {
                GL30 gl = Gdx.gl30;
                IntBuffer id = ByteBuffer
                                .allocateDirect(4)
                                .order(ByteOrder.nativeOrder())
                                .asIntBuffer();

                gl.glGenVertexArrays(1, id);
                int vao = id.get(0);

                // CONFIGURE THE VAO
                gl.glBindVertexArray(vao);

                int strideBytes = floatsPerVert * 4;

                gl.glEnableVertexAttribArray(0);
                gl.glVertexAttribPointer(0, 3, GL20.GL_FLOAT, false, strideBytes, 0);

                gl.glEnableVertexAttribArray(1);
                gl.glVertexAttribPointer(1, 3, GL20.GL_FLOAT, false, strideBytes, 12);

                gl.glEnableVertexAttribArray(2);
                gl.glVertexAttribPointer(2, 1, GL20.GL_FLOAT, false, strideBytes, 24);

                gl.glEnableVertexAttribArray(3);
                gl.glVertexAttribPointer(3, 2, GL20.GL_FLOAT, false, strideBytes, 28);

                gl.glBindVertexArray(0);

                vaoHandle.constructor(vao, floatsPerVert);
                return vaoHandle;
        }
}
