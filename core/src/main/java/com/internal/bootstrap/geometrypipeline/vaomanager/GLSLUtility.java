package com.internal.bootstrap.geometrypipeline.vaomanager;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL30;
import com.internal.core.engine.UtilityPackage;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

class GLSLUtility extends UtilityPackage {

        static VAOHandle createVAO(VAOHandle vaoHandle, int[] attrSizes) {

                GL30 gl = Gdx.gl30;
                IntBuffer id = ByteBuffer
                                .allocateDirect(4)
                                .order(ByteOrder.nativeOrder())
                                .asIntBuffer();
                gl.glGenVertexArrays(1, id);
                int vao = id.get(0);

                vaoHandle.constructor(vao, attrSizes);

                // Only enable arrays here - attrib pointers are set in uploadVertexData
                // once a real VBO is bound. Without a VBO bound, glVertexAttribPointer
                // records null and attributes silently read zero.
                gl.glBindVertexArray(vao);
                for (int i = 0; i < attrSizes.length; i++)
                        gl.glEnableVertexAttribArray(i);
                gl.glBindVertexArray(0);

                return vaoHandle;
        }

        static VAOHandle cloneVAO(VAOHandle vaoHandle, VAOHandle templateVAO) {

                GL30 gl = Gdx.gl30;
                IntBuffer id = ByteBuffer
                                .allocateDirect(4)
                                .order(ByteOrder.nativeOrder())
                                .asIntBuffer();
                gl.glGenVertexArrays(1, id);
                int vao = id.get(0);

                vaoHandle.constructor(vao, templateVAO.getAttrSizes());

                gl.glBindVertexArray(vao);
                for (int i = 0; i < templateVAO.getAttrSizes().length; i++)
                        gl.glEnableVertexAttribArray(i);
                gl.glBindVertexArray(0);

                return vaoHandle;
        }

        static void removeVAO(VAOHandle vaoHandle) {

                GL30 gl = Gdx.gl30;
                int vao = vaoHandle.getAttributeHandle();

                if (vao != 0) {
                        IntBuffer buffer = ByteBuffer
                                        .allocateDirect(4)
                                        .order(ByteOrder.nativeOrder())
                                        .asIntBuffer();
                        buffer.put(vao).flip();
                        gl.glDeleteVertexArrays(1, buffer);
                }
        }
}