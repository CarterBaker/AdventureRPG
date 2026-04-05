package program.core.util.graphics.gl;

import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public interface GL20 {

    /*
     * Engine GL20 interface. Maps the subset of OpenGL 2.0 calls used by
     * the engine. Implemented by the backend GL delegation class.
     */

    // Constants — Buffers
    int GL_ARRAY_BUFFER = 0x8892;
    int GL_ELEMENT_ARRAY_BUFFER = 0x8893;
    int GL_DYNAMIC_DRAW = 0x88E8;
    int GL_STATIC_DRAW = 0x88E4;

    // Constants — Primitives
    int GL_FLOAT = 0x1406;
    int GL_TRIANGLES = 0x0004;
    int GL_UNSIGNED_SHORT = 0x1403;
    int GL_UNSIGNED_BYTE = 0x1401;

    // Constants — Textures
    int GL_TEXTURE_2D = 0x0DE1;
    int GL_TEXTURE0 = 0x84C0;
    int GL_RGBA = 0x1908;
    int GL_RGBA8 = 0x8058;
    int GL_TEXTURE_MIN_FILTER = 0x2801;
    int GL_TEXTURE_MAG_FILTER = 0x2800;
    int GL_TEXTURE_WRAP_S = 0x2802;
    int GL_TEXTURE_WRAP_T = 0x2803;
    int GL_LINEAR = 0x2601;
    int GL_CLAMP_TO_EDGE = 0x812F;
    int GL_REPEAT = 0x2901;

    // Constants — State
    int GL_DEPTH_TEST = 0x0B71;
    int GL_BLEND = 0x0BE2;
    int GL_SRC_ALPHA = 0x0302;
    int GL_ONE_MINUS_SRC_ALPHA = 0x0303;
    int GL_CULL_FACE = 0x0B44;
    int GL_BACK = 0x0405;
    int GL_CCW = 0x0901;
    int GL_SCISSOR_TEST = 0x0C11;
    int GL_LEQUAL = 0x0203;

    // Constants — Shaders
    int GL_VERTEX_SHADER = 0x8B31;
    int GL_FRAGMENT_SHADER = 0x8B30;
    int GL_LINK_STATUS = 0x8B82;
    int GL_COMPILE_STATUS = 0x8B81;

    // Constants — Error
    int GL_NO_ERROR = 0;

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