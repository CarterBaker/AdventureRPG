package program.core.util.graphics.gl;

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

    // Vertex Arrays \\

    void glGenVertexArrays(int n, IntBuffer arrays);

    void glBindVertexArray(int array);

    void glDeleteVertexArrays(int n, IntBuffer arrays);

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

    void glTexSubImage3D(int target, int level, int xoffset, int yoffset, int zoffset, int width, int height, int depth,
            int format, int type, ByteBuffer pixels);
}