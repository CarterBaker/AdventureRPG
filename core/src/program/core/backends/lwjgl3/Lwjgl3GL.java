package program.core.backends.lwjgl3;

import program.core.engine.UtilityPackage;
import program.core.util.graphics.gl.GL30;
import org.lwjgl.opengl.*;
import java.nio.*;

class Lwjgl3GL implements GL30 {

    /*
     * Thin delegation layer mapping the engine GL30 interface onto LWJGL3 calls.
     * No state. No allocation. Every method is a direct forward to the underlying
     * OpenGL binding.
     */

    // State \\

    public void glEnable(int cap) {
        GL11.glEnable(cap);
    }

    public void glDisable(int cap) {
        GL11.glDisable(cap);
    }

    public void glDepthFunc(int func) {
        GL11.glDepthFunc(func);
    }

    public void glDepthMask(boolean flag) {
        GL11.glDepthMask(flag);
    }

    public void glBlendFunc(int src, int dst) {
        GL11.glBlendFunc(src, dst);
    }

    public void glCullFace(int mode) {
        GL11.glCullFace(mode);
    }

    public void glFrontFace(int mode) {
        GL11.glFrontFace(mode);
    }

    public void glScissor(int x, int y, int w, int h) {
        GL11.glScissor(x, y, w, h);
    }

    public void glViewport(int x, int y, int w, int h) {
        GL11.glViewport(x, y, w, h);
    }

    public void glClearColor(float r, float g, float b, float a) {
        GL11.glClearColor(r, g, b, a);
    }

    public void glClear(int mask) {
        GL11.glClear(mask);
    }

    // Shaders \\

    public int glCreateProgram() {
        return GL20C.glCreateProgram();
    }

    public int glCreateShader(int type) {
        return GL20C.glCreateShader(type);
    }

    public void glShaderSource(int shader, String source) {
        GL20C.glShaderSource(shader, source);
    }

    public void glCompileShader(int shader) {
        GL20C.glCompileShader(shader);
    }

    public void glGetShaderiv(int shader, int pname, IntBuffer params) {
        params.put(0, GL20C.glGetShaderi(shader, pname));
    }

    public String glGetShaderInfoLog(int shader) {
        return GL20C.glGetShaderInfoLog(shader);
    }

    public void glAttachShader(int program, int shader) {
        GL20C.glAttachShader(program, shader);
    }

    public void glDetachShader(int program, int shader) {
        GL20C.glDetachShader(program, shader);
    }

    public void glDeleteShader(int shader) {
        GL20C.glDeleteShader(shader);
    }

    public void glLinkProgram(int program) {
        GL20C.glLinkProgram(program);
    }

    public void glGetProgramiv(int program, int pname, IntBuffer params) {
        params.put(0, GL20C.glGetProgrami(program, pname));
    }

    public String glGetProgramInfoLog(int program) {
        return GL20C.glGetProgramInfoLog(program);
    }

    public void glUseProgram(int program) {
        GL20C.glUseProgram(program);
    }

    public void glDeleteProgram(int program) {
        GL20C.glDeleteProgram(program);
    }

    public int glGetUniformLocation(int program, String name) {
        return GL20C.glGetUniformLocation(program, name);
    }

    public int glGetUniformBlockIndex(int program, String name) {
        return GL31C.glGetUniformBlockIndex(program, name);
    }

    public void glUniformBlockBinding(int program, int index, int binding) {
        GL31C.glUniformBlockBinding(program, index, binding);
    }

    // Uniforms — Scalar \\

    public void glUniform1i(int l, int v0) {
        GL20C.glUniform1i(l, v0);
    }

    public void glUniform1f(int l, float v0) {
        GL20C.glUniform1f(l, v0);
    }

    public void glUniform2i(int l, int v0, int v1) {
        GL20C.glUniform2i(l, v0, v1);
    }

    public void glUniform2f(int l, float v0, float v1) {
        GL20C.glUniform2f(l, v0, v1);
    }

    public void glUniform3i(int l, int v0, int v1, int v2) {
        GL20C.glUniform3i(l, v0, v1, v2);
    }

    public void glUniform3f(int l, float v0, float v1, float v2) {
        GL20C.glUniform3f(l, v0, v1, v2);
    }

    public void glUniform4i(int l, int v0, int v1, int v2, int v3) {
        GL20C.glUniform4i(l, v0, v1, v2, v3);
    }

    public void glUniform4f(int l, float v0, float v1, float v2, float v3) {
        GL20C.glUniform4f(l, v0, v1, v2, v3);
    }

    // Uniforms — Arrays \\

    public void glUniform1iv(int l, int c, int[] v, int o) {
        GL20C.glUniform1iv(l, IntBuffer.wrap(v, o, c));
    }

    public void glUniform1fv(int l, int c, float[] v, int o) {
        GL20C.glUniform1fv(l, FloatBuffer.wrap(v, o, c));
    }

    public void glUniform2iv(int l, int c, int[] v, int o) {
        GL20C.glUniform2iv(l, IntBuffer.wrap(v, o, c * 2));
    }

    public void glUniform2fv(int l, int c, float[] v, int o) {
        GL20C.glUniform2fv(l, FloatBuffer.wrap(v, o, c * 2));
    }

    public void glUniform3iv(int l, int c, int[] v, int o) {
        GL20C.glUniform3iv(l, IntBuffer.wrap(v, o, c * 3));
    }

    public void glUniform3fv(int l, int c, float[] v, int o) {
        GL20C.glUniform3fv(l, FloatBuffer.wrap(v, o, c * 3));
    }

    public void glUniform4iv(int l, int c, int[] v, int o) {
        GL20C.glUniform4iv(l, IntBuffer.wrap(v, o, c * 4));
    }

    public void glUniform4fv(int l, int c, float[] v, int o) {
        GL20C.glUniform4fv(l, FloatBuffer.wrap(v, o, c * 4));
    }

    // Uniforms — Matrices \\

    public void glUniformMatrix2fv(int l, int c, boolean t, float[] v, int o) {
        GL20C.glUniformMatrix2fv(l, t, FloatBuffer.wrap(v, o, c * 4));
    }

    public void glUniformMatrix3fv(int l, int c, boolean t, float[] v, int o) {
        GL20C.glUniformMatrix3fv(l, t, FloatBuffer.wrap(v, o, c * 9));
    }

    public void glUniformMatrix4fv(int l, int c, boolean t, float[] v, int o) {
        GL20C.glUniformMatrix4fv(l, t, FloatBuffer.wrap(v, o, c * 16));
    }

    public void glUniformMatrix2fv(int l, int c, boolean t, FloatBuffer v) {
        GL20C.glUniformMatrix2fv(l, t, v);
    }

    public void glUniformMatrix3fv(int l, int c, boolean t, FloatBuffer v) {
        GL20C.glUniformMatrix3fv(l, t, v);
    }

    public void glUniformMatrix4fv(int l, int c, boolean t, FloatBuffer v) {
        GL20C.glUniformMatrix4fv(l, t, v);
    }

    // Textures \\

    public int glGenTexture() {
        return GL11.glGenTextures();
    }

    public void glBindTexture(int target, int texture) {
        GL11.glBindTexture(target, texture);
    }

    public void glActiveTexture(int texture) {
        GL13.glActiveTexture(texture);
    }

    public void glTexParameteri(int target, int pname, int param) {
        GL11.glTexParameteri(target, pname, param);
    }

    public void glDeleteTexture(int texture) {
        GL11.glDeleteTextures(texture);
    }

    public void glTexImage2D(
            int target, int level, int internalformat,
            int width, int height, int border,
            int format, int type, Buffer pixels) {

        if (pixels instanceof ByteBuffer bb)
            GL11.glTexImage2D(target, level, internalformat, width, height, border, format, type, bb);
        else if (pixels instanceof ShortBuffer sb)
            GL11.glTexImage2D(target, level, internalformat, width, height, border, format, type, sb);
        else if (pixels instanceof IntBuffer ib)
            GL11.glTexImage2D(target, level, internalformat, width, height, border, format, type, ib);
        else if (pixels instanceof FloatBuffer fb)
            GL11.glTexImage2D(target, level, internalformat, width, height, border, format, type, fb);
        else if (pixels == null)
            GL11.glTexImage2D(target, level, internalformat, width, height, border, format, type, (ByteBuffer) null);
        else
            UtilityPackage.throwException("Unsupported pixel buffer type: " + pixels.getClass().getSimpleName());
    }

    public void glTexImage3D(
            int target, int level, int internalformat,
            int width, int height, int depth, int border,
            int format, int type, ByteBuffer pixels) {
        GL12.glTexImage3D(target, level, internalformat, width, height, depth, border, format, type, pixels);
    }

    public void glTexSubImage3D(
            int target, int level,
            int xoffset, int yoffset, int zoffset,
            int width, int height, int depth,
            int format, int type, ByteBuffer pixels) {
        GL12.glTexSubImage3D(target, level, xoffset, yoffset, zoffset, width, height, depth, format, type, pixels);
    }

    // Buffers \\

    public int glGenBuffer() {
        return GL15.glGenBuffers();
    }

    public void glBindBuffer(int target, int buffer) {
        GL15.glBindBuffer(target, buffer);
    }

    public void glBindBufferBase(int target, int index, int buffer) {
        GL30C.glBindBufferBase(target, index, buffer);
    }

    public void glDeleteBuffer(int buffer) {
        GL15.glDeleteBuffers(buffer);
    }

    public void glDeleteBuffers(int n, IntBuffer buffers) {
        GL15.glDeleteBuffers(buffers);
    }

    public void glBufferData(int target, int size, Buffer data, int usage) {
        if (data instanceof FloatBuffer fb)
            GL15.glBufferData(target, fb, usage);
        else if (data instanceof IntBuffer ib)
            GL15.glBufferData(target, ib, usage);
        else if (data instanceof ByteBuffer bb)
            GL15.glBufferData(target, bb, usage);
        else
            GL15.glBufferData(target, size, usage);
    }

    public void glBufferSubData(int target, int offset, int size, Buffer data) {
        if (data instanceof FloatBuffer fb)
            GL15.glBufferSubData(target, offset, fb);
        else if (data instanceof IntBuffer ib)
            GL15.glBufferSubData(target, offset, ib);
        else if (data instanceof ByteBuffer bb)
            GL15.glBufferSubData(target, offset, bb);
    }

    // Vertex Arrays \\

    public void glGenVertexArrays(int n, IntBuffer arrays) {
        GL30C.glGenVertexArrays(arrays);
    }

    public void glBindVertexArray(int array) {
        GL30C.glBindVertexArray(array);
    }

    public void glDeleteVertexArrays(int n, IntBuffer arrays) {
        GL30C.glDeleteVertexArrays(arrays);
    }

    // Vertex Attributes \\

    public void glEnableVertexAttribArray(int index) {
        GL20C.glEnableVertexAttribArray(index);
    }

    public void glVertexAttribDivisor(int index, int divisor) {
        GL33C.glVertexAttribDivisor(index, divisor);
    }

    public void glVertexAttribPointer(
            int index, int size, int type,
            boolean normalized, int stride, int pointer) {
        GL20C.glVertexAttribPointer(index, size, type, normalized, stride, pointer);
    }

    // Draw Calls \\

    public void glDrawElements(int mode, int count, int type, int indices) {
        GL11.glDrawElements(mode, count, type, indices);
    }

    public void glDrawElementsInstanced(int mode, int count, int type, int indices, int instancecount) {
        GL31C.glDrawElementsInstanced(mode, count, type, indices, instancecount);
    }
}