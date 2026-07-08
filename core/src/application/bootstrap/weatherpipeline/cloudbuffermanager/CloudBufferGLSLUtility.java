package application.bootstrap.weatherpipeline.cloudbuffermanager;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import engine.graphics.gl.GL20;
import engine.root.EngineContext;
import engine.root.EngineSetting;

class CloudBufferGLSLUtility {

    /*
     * GL creation, upload, and disposal operations for CloudBufferManager.
     * Package-private — only CloudBufferManager may call these. Mirrors
     * SkinnedBufferGLSLUtility/CompositeBufferGLSLUtility exactly, just for
     * a generic per-instance float layout rather than a fixed model-matrix
     * width.
     */

    // Instance VBO \\

    static int createDynamicInstanceVBO(int maxInstances, int floatsPerInstance) {

        GL20 gl20 = EngineContext.gl20;
        int size = maxInstances * floatsPerInstance * Float.BYTES;

        int vbo = gl20.glGenBuffer();
        gl20.glBindBuffer(EngineSetting.GL_ARRAY_BUFFER, vbo);
        gl20.glBufferData(EngineSetting.GL_ARRAY_BUFFER, size, null, EngineSetting.GL_DYNAMIC_DRAW);
        gl20.glBindBuffer(EngineSetting.GL_ARRAY_BUFFER, 0);

        return vbo;
    }

    static void uploadInstanceVBO(int vbo, float[] instanceData, int floatCount) {

        GL20 gl20 = EngineContext.gl20;

        FloatBuffer buffer = ByteBuffer
                .allocateDirect(floatCount * Float.BYTES)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        buffer.put(instanceData, 0, floatCount).flip();

        gl20.glBindBuffer(EngineSetting.GL_ARRAY_BUFFER, vbo);
        gl20.glBufferSubData(EngineSetting.GL_ARRAY_BUFFER, 0, floatCount * Float.BYTES, buffer);
        gl20.glBindBuffer(EngineSetting.GL_ARRAY_BUFFER, 0);
    }

    // Disposal \\

    static void deleteBuffer(int handle) {
        if (handle != 0)
            EngineContext.gl20.glDeleteBuffer(handle);
    }
}