package com.AdventureRPG.core.geometrypipeline.vaomanager;

import com.AdventureRPG.core.engine.UtilityPackage;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL30;

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

                vaoHandle.constructor(vao, floatsPerVert);

                return vaoHandle;
        }
}
