package program.core.util.graphics.gl;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public interface GL30 extends GL20 {
    int GL_UNIFORM_BUFFER=0x8A11, GL_TEXTURE_2D_ARRAY=0x8C1A, GL_COLOR_BUFFER_BIT=0x4000, GL_DEPTH_BUFFER_BIT=0x0100;
    void glGenVertexArrays(int n, IntBuffer arrays); void glBindVertexArray(int array); void glDeleteVertexArrays(int n, IntBuffer arrays);
    void glVertexAttribDivisor(int index, int divisor); void glDrawElementsInstanced(int mode,int count,int type,int indices,int instancecount);
    void glBindBufferBase(int target,int index,int buffer); int glGetUniformBlockIndex(int program,String uniformBlockName); void glUniformBlockBinding(int program,int uniformBlockIndex,int uniformBlockBinding);
    void glDeleteBuffers(int n, IntBuffer buffers);
    void glTexImage3D(int target,int level,int internalformat,int width,int height,int depth,int border,int format,int type,ByteBuffer pixels);
    void glTexSubImage3D(int target,int level,int xoffset,int yoffset,int zoffset,int width,int height,int depth,int format,int type,ByteBuffer pixels);
}