package engine.graphics.gl;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public interface GL30 extends GL20 {

        /*
         * Engine GL30 interface. Extends GL20 with vertex array objects, UBO
         * binding, instanced drawing, and 3D texture support.
         */

        // Constants
        int GL_UNIFORM_BUFFER = 0x8A11;
        int GL_TEXTURE_2D_ARRAY = 0x8C1A;
        int GL_COLOR_BUFFER_BIT = 0x4000;
        int GL_DEPTH_BUFFER_BIT = 0x0100;
        int GL_FRAMEBUFFER = 0x8D40;
        int GL_RENDERBUFFER = 0x8D41;
        int GL_COLOR_ATTACHMENT0 = 0x8CE0;
        int GL_DEPTH_ATTACHMENT = 0x8D00;
        int GL_DEPTH_COMPONENT24 = 0x81A6;
        int GL_FRAMEBUFFER_COMPLETE = 0x8CD5;
        int GL_RGBA16F = 0x881A;
        int GL_RGB8 = 0x8051;

        // Vertex Arrays \\

        void glGenVertexArrays(int n, IntBuffer arrays);

        void glBindVertexArray(int array);

        void glDeleteVertexArrays(int n, IntBuffer arrays);

        // Framebuffers \\

        int glGenFramebuffer();

        void glBindFramebuffer(int target, int framebuffer);

        void glFramebufferTexture2D(int target, int attachment, int textarget, int texture, int level);

        int glGenRenderbuffer();

        void glBindRenderbuffer(int target, int renderbuffer);

        void glRenderbufferStorage(int target, int internalformat, int width, int height);

        void glFramebufferRenderbuffer(int target, int attachment, int renderbuffertarget, int renderbuffer);

        int glCheckFramebufferStatus(int target);

        // Instancing \\

        void glVertexAttribDivisor(int index, int divisor);

        void glDrawElementsInstanced(int mode, int count, int type, int indices, int instancecount);

        // UBOs \\

        void glBindBufferBase(int target, int index, int buffer);

        int glGetUniformBlockIndex(int program, String uniformBlockName);

        void glUniformBlockBinding(int program, int uniformBlockIndex, int uniformBlockBinding);

        void glDeleteBuffers(int n, IntBuffer buffers);

        // 3D Textures \\

        void glTexImage3D(int target, int level, int internalformat, int width, int height, int depth, int border,
                        int format, int type, ByteBuffer pixels);

        void glTexSubImage3D(int target, int level, int xoffset, int yoffset, int zoffset, int width, int height,
                        int depth,
                        int format, int type, ByteBuffer pixels);
}
