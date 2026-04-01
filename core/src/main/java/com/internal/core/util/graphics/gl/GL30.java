package com.internal.core.util.graphics.gl;

import java.nio.IntBuffer;

public interface GL30 extends GL20 {
    int GL_UNIFORM_BUFFER=0x8A11, GL_TEXTURE_2D_ARRAY=0x8C1A, GL_COLOR_BUFFER_BIT=0x4000, GL_DEPTH_BUFFER_BIT=0x0100;
    void glGenVertexArrays(int n, IntBuffer arrays); void glBindVertexArray(int array); void glDeleteVertexArrays(int n, IntBuffer arrays);
    void glVertexAttribDivisor(int index, int divisor); void glDrawElementsInstanced(int mode,int count,int type,int indices,int instancecount);
    void glBindBufferBase(int target,int index,int buffer); int glGetUniformBlockIndex(int program,String uniformBlockName); void glUniformBlockBinding(int program,int uniformBlockIndex,int uniformBlockBinding);
    void glDeleteBuffers(int n, IntBuffer buffers);
}
