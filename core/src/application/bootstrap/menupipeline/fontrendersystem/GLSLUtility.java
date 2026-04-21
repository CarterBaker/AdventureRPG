package application.bootstrap.menupipeline.fontrendersystem;

import application.bootstrap.shaderpipeline.material.MaterialInstance;
import application.bootstrap.shaderpipeline.ubo.UBOHandle;
import application.bootstrap.shaderpipeline.uniforms.UniformStruct;
import application.kernel.windowpipeline.window.WindowInstance;
import engine.graphics.gl.GL20;
import engine.graphics.gl.GL30;
import engine.root.EngineContext;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

class GLSLUtility {

    /*
     * GL operations for FontCompositeRenderSystem. Package-private.
     * Quad geometry is in local [0,1] space — the font composite shader
     * transforms to screen pixels using per-instance screenPos/screenSize,
     * then to NDC via u_screenSize uniform.
     */

    private static final float[] QUAD_VERTS = { 0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f };
    private static final short[] QUAD_INDICES = { 0, 1, 2, 0, 2, 3 };

    // Quad Geometry \\

    static int createQuadVBO() {
        FloatBuffer buf = ByteBuffer.allocateDirect(QUAD_VERTS.length * Float.BYTES)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        buf.put(QUAD_VERTS).flip();
        GL20 gl = EngineContext.gl20;
        int vbo = gl.glGenBuffer();
        gl.glBindBuffer(GL20.GL_ARRAY_BUFFER, vbo);
        gl.glBufferData(GL20.GL_ARRAY_BUFFER, QUAD_VERTS.length * Float.BYTES, buf, GL20.GL_STATIC_DRAW);
        gl.glBindBuffer(GL20.GL_ARRAY_BUFFER, 0);
        return vbo;
    }

    static int createQuadIBO() {
        ShortBuffer buf = ByteBuffer.allocateDirect(QUAD_INDICES.length * Short.BYTES)
                .order(ByteOrder.nativeOrder()).asShortBuffer();
        buf.put(QUAD_INDICES).flip();
        GL20 gl = EngineContext.gl20;
        int ibo = gl.glGenBuffer();
        gl.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, ibo);
        gl.glBufferData(GL20.GL_ELEMENT_ARRAY_BUFFER, QUAD_INDICES.length * Short.BYTES, buf, GL20.GL_STATIC_DRAW);
        gl.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, 0);
        return ibo;
    }

    // Instance VBO \\

    static int createInstanceVBO(int capacity, int floatsPerInstance) {
        GL20 gl = EngineContext.gl20;
        int vbo = gl.glGenBuffer();
        gl.glBindBuffer(GL20.GL_ARRAY_BUFFER, vbo);
        gl.glBufferData(GL20.GL_ARRAY_BUFFER,
                capacity * floatsPerInstance * Float.BYTES, null, GL20.GL_DYNAMIC_DRAW);
        gl.glBindBuffer(GL20.GL_ARRAY_BUFFER, 0);
        return vbo;
    }

    static void uploadInstanceData(int instanceVBO, float[] data, int floatCount) {
        FloatBuffer buf = ByteBuffer.allocateDirect(floatCount * Float.BYTES)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        buf.put(data, 0, floatCount).flip();
        GL20 gl = EngineContext.gl20;
        gl.glBindBuffer(GL20.GL_ARRAY_BUFFER, instanceVBO);
        gl.glBufferSubData(GL20.GL_ARRAY_BUFFER, 0, floatCount * Float.BYTES, buf);
        gl.glBindBuffer(GL20.GL_ARRAY_BUFFER, 0);
    }

    // VAO \\

    static int createFontVAO(
            int quadVBO, int[] quadAttrSizes,
            int quadIBO,
            int instanceVBO, int[] instanceAttrSizes) {

        GL30 gl30 = EngineContext.gl30;
        GL20 gl20 = EngineContext.gl20;

        IntBuffer idBuf = ByteBuffer.allocateDirect(Integer.BYTES)
                .order(ByteOrder.nativeOrder()).asIntBuffer();
        gl30.glGenVertexArrays(1, idBuf);
        int vao = idBuf.get(0);
        gl30.glBindVertexArray(vao);

        // Quad (mesh) attributes
        gl20.glBindBuffer(GL20.GL_ARRAY_BUFFER, quadVBO);
        int meshStride = 0;
        for (int s : quadAttrSizes)
            meshStride += s;
        int meshStrideBytes = meshStride * Float.BYTES;
        int meshOffsetBytes = 0;
        for (int i = 0; i < quadAttrSizes.length; i++) {
            gl20.glEnableVertexAttribArray(i);
            gl20.glVertexAttribPointer(i, quadAttrSizes[i], GL20.GL_FLOAT, false, meshStrideBytes, meshOffsetBytes);
            meshOffsetBytes += quadAttrSizes[i] * Float.BYTES;
        }

        // Instance attributes — divisor 1
        gl20.glBindBuffer(GL20.GL_ARRAY_BUFFER, instanceVBO);
        int instanceStride = 0;
        for (int s : instanceAttrSizes)
            instanceStride += s;
        int instanceStrideBytes = instanceStride * Float.BYTES;
        int instanceOffsetBytes = 0;
        for (int i = 0; i < instanceAttrSizes.length; i++) {
            int loc = quadAttrSizes.length + i;
            gl20.glEnableVertexAttribArray(loc);
            gl20.glVertexAttribPointer(loc, instanceAttrSizes[i], GL20.GL_FLOAT, false,
                    instanceStrideBytes, instanceOffsetBytes);
            gl30.glVertexAttribDivisor(loc, 1);
            instanceOffsetBytes += instanceAttrSizes[i] * Float.BYTES;
        }

        gl20.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, quadIBO);
        gl30.glBindVertexArray(0);
        gl20.glBindBuffer(GL20.GL_ARRAY_BUFFER, 0);
        return vao;
    }

    // Material Binding \\

    static void bindFontMaterial(MaterialInstance material, WindowInstance window) {

        int shaderHandle = material.getShaderHandle().getGpuHandle();
        GL20 gl20 = EngineContext.gl20;
        GL30 gl30 = EngineContext.gl30;

        gl20.glUseProgram(shaderHandle);

        Object2ObjectOpenHashMap<String, UBOHandle> ubos = material.getSourceUBOs();
        for (UBOHandle ubo : ubos.values()) {
            int blockIdx = gl30.glGetUniformBlockIndex(shaderHandle, ubo.getBlockName());
            if (blockIdx == EngineSetting.GL_INVALID_INDEX)
                continue;
            gl30.glUniformBlockBinding(shaderHandle, blockIdx, ubo.getBindingPoint());
            gl30.glBindBufferBase(GL30.GL_UNIFORM_BUFFER,
                    ubo.getBindingPoint(), ubo.getGpuHandle());
        }

        Object2ObjectOpenHashMap<String, UniformStruct<?>> uniforms = material.getUniforms();
        int textureUnit = 0;
        for (UniformStruct<?> uniform : uniforms.values()) {
            if (uniform.attribute().isSampler())
                uniform.attribute().bindTexture(textureUnit++);
            uniform.push();
        }

        int loc = gl20.glGetUniformLocation(shaderHandle, "u_screenSize");
        if (loc >= 0)
            gl20.glUniform2f(loc, window.getWidth(), window.getHeight());
    }

    // Draw \\

    static void drawInstanced(int vao, int indexCount, int instanceCount) {
        GL30 gl30 = EngineContext.gl30;
        gl30.glBindVertexArray(vao);
        gl30.glDrawElementsInstanced(
                GL20.GL_TRIANGLES, indexCount, GL20.GL_UNSIGNED_SHORT, 0, instanceCount);
        gl30.glBindVertexArray(0);
    }

    // Scissor \\

    static void enableScissor(int x, int y, int w, int h) {
        GL20 gl = EngineContext.gl20;
        gl.glEnable(GL20.GL_SCISSOR_TEST);
        gl.glScissor(x, y, w, h);
    }

    static void disableScissor() {
        EngineContext.gl20.glDisable(GL20.GL_SCISSOR_TEST);
    }

    // Cleanup \\

    static void deleteBuffer(int handle) {
        EngineContext.gl20.glDeleteBuffer(handle);
    }

    static void deleteVAO(int handle) {
        IntBuffer buf = ByteBuffer.allocateDirect(Integer.BYTES)
                .order(ByteOrder.nativeOrder()).asIntBuffer();
        buf.put(handle).flip();
        EngineContext.gl30.glDeleteVertexArrays(1, buf);
    }
}
