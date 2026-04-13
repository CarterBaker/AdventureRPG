package program.bootstrap.shaderpipeline.ubomanager;

import program.core.engine.EngineContext;
import program.core.util.graphics.gl.GL20;
import program.core.util.graphics.gl.GL30;
import program.core.util.memory.BufferUtils;
import program.core.engine.EngineUtility;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

/*
 * GL30 wrapper for all UBO operations — creation, allocation, binding,
 * upload, and deletion. Single GL entry point for the UBO system.
 * Package-private and stateless.
 */
class GLSLUtility extends EngineUtility {

    // UBO Creation \\

    static int createUniformBuffer() {
        return EngineContext.gl30.glGenBuffer();
    }

    static void allocateUniformBuffer(int buffer, int sizeBytes) {
        EngineContext.gl30.glBindBuffer(GL30.GL_UNIFORM_BUFFER, buffer);
        EngineContext.gl30.glBufferData(GL30.GL_UNIFORM_BUFFER, sizeBytes, null, GL20.GL_DYNAMIC_DRAW);
        EngineContext.gl30.glBindBuffer(GL30.GL_UNIFORM_BUFFER, 0);
    }

    static void bindUniformBufferBase(int buffer, int bindingPoint) {
        EngineContext.gl30.glBindBufferBase(GL30.GL_UNIFORM_BUFFER, bindingPoint, buffer);
    }

    // UBO Upload \\

    static void updateUniformBuffer(int buffer, int offset, ByteBuffer data) {
        EngineContext.gl30.glBindBuffer(GL30.GL_UNIFORM_BUFFER, buffer);
        EngineContext.gl30.glBufferSubData(GL30.GL_UNIFORM_BUFFER, offset, data.remaining(), data);
        EngineContext.gl30.glBindBuffer(GL30.GL_UNIFORM_BUFFER, 0);
    }

    // UBO Deletion \\

    static void deleteUniformBuffer(int buffer) {
        IntBuffer buf = BufferUtils.newIntBuffer(1);
        buf.put(buffer).flip();
        EngineContext.gl30.glDeleteBuffers(1, buf);
    }
}
