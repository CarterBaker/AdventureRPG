package engine.graphics.gl;

import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public interface GL20 {

    /*
     * Engine GL20 interface. Maps the subset of OpenGL 2.0 calls used by
     * the engine. Implemented by the backend GL delegation class.
     */

    // State \\

    void glEnable(int cap);

    void glDisable(int cap);

    void glDepthFunc(int func);

    void glDepthMask(boolean flag);

    void glBlendFunc(int sfactor, int dfactor);

    void glCullFace(int mode);

    void glFrontFace(int mode);

    void glScissor(int x, int y, int w, int h);

    void glViewport(int x, int y, int w, int h);

    void glClearColor(float r, float g, float b, float a);

    void glClear(int mask);

    // Shaders \\

    int glCreateProgram();

    int glCreateShader(int type);

    void glShaderSource(int shader, String source);

    void glCompileShader(int shader);

    void glGetShaderiv(int shader, int pname, IntBuffer params);

    String glGetShaderInfoLog(int shader);

    void glAttachShader(int program, int shader);

    void glDetachShader(int program, int shader);

    void glDeleteShader(int shader);

    void glLinkProgram(int program);

    void glGetProgramiv(int program, int pname, IntBuffer params);

    String glGetProgramInfoLog(int program);

    void glUseProgram(int program);

    int glGetUniformLocation(int program, String name);

    void glDeleteProgram(int program);

    // Textures \\

    int glGenTexture();

    void glBindTexture(int target, int texture);

    void glActiveTexture(int texture);

    void glTexImage2D(int target, int level, int internalformat, int width, int height, int border, int format,
            int type, Buffer pixels);

    void glTexParameteri(int target, int pname, int param);

    void glDeleteTexture(int texture);

    // Buffers \\

    int glGenBuffer();

    void glBindBuffer(int target, int buffer);

    void glBufferData(int target, int size, Buffer data, int usage);

    void glBufferSubData(int target, int offset, int size, Buffer data);

    void glDeleteBuffer(int buffer);

    // Vertex Attributes \\

    void glEnableVertexAttribArray(int index);

    void glVertexAttribPointer(int index, int size, int type, boolean normalized, int stride, int pointer);

    // Draw \\

    void glDrawElements(int mode, int count, int type, int indices);

    void glDrawArrays(int mode, int first, int count);

    // Uniforms — Scalar \\

    void glUniform1i(int loc, int v0);

    void glUniform1f(int loc, float v0);

    void glUniform2i(int loc, int v0, int v1);

    void glUniform2f(int loc, float v0, float v1);

    void glUniform3i(int loc, int v0, int v1, int v2);

    void glUniform3f(int loc, float v0, float v1, float v2);

    void glUniform4i(int loc, int v0, int v1, int v2, int v3);

    void glUniform4f(int loc, float v0, float v1, float v2, float v3);

    // Uniforms — Arrays \\

    void glUniform1iv(int loc, int count, int[] v, int offset);

    void glUniform1fv(int loc, int count, float[] v, int offset);

    void glUniform2iv(int loc, int count, int[] v, int offset);

    void glUniform2fv(int loc, int count, float[] v, int offset);

    void glUniform3iv(int loc, int count, int[] v, int offset);

    void glUniform3fv(int loc, int count, float[] v, int offset);

    void glUniform4iv(int loc, int count, int[] v, int offset);

    void glUniform4fv(int loc, int count, float[] v, int offset);

    // Uniforms — Matrices \\

    void glUniformMatrix2fv(int loc, int count, boolean transpose, float[] value, int offset);

    void glUniformMatrix3fv(int loc, int count, boolean transpose, float[] value, int offset);

    void glUniformMatrix4fv(int loc, int count, boolean transpose, float[] value, int offset);

    void glUniformMatrix2fv(int loc, int count, boolean transpose, FloatBuffer value);

    void glUniformMatrix3fv(int loc, int count, boolean transpose, FloatBuffer value);

    void glUniformMatrix4fv(int loc, int count, boolean transpose, FloatBuffer value);

    // Error \\

    int glGetError();
}