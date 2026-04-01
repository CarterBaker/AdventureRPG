package com.internal.bootstrap.shaderpipeline.ubomanager;

import com.internal.core.app.CoreContext;
import com.internal.core.util.graphics.gl.GL20;
import com.internal.core.util.graphics.gl.GL30;
import com.internal.core.util.memory.BufferUtils;
import com.internal.core.engine.UtilityPackage;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

/*
 * GL30 wrapper for all UBO operations — creation, allocation, binding,
 * upload, and deletion. Single GL entry point for the UBO system.
 * Package-private and stateless.
 */
class GLSLUtility extends UtilityPackage {

    // UBO Creation \\

    static int createUniformBuffer() {
        return CoreContext.gl30.glGenBuffer();
    }

    static void allocateUniformBuffer(int buffer, int sizeBytes) {
        CoreContext.gl30.glBindBuffer(GL30.GL_UNIFORM_BUFFER, buffer);
        CoreContext.gl30.glBufferData(GL30.GL_UNIFORM_BUFFER, sizeBytes, null, GL20.GL_DYNAMIC_DRAW);
        CoreContext.gl30.glBindBuffer(GL30.GL_UNIFORM_BUFFER, 0);
    }

    static void bindUniformBufferBase(int buffer, int bindingPoint) {
        CoreContext.gl30.glBindBufferBase(GL30.GL_UNIFORM_BUFFER, bindingPoint, buffer);
    }

    // UBO Upload \\

    static void updateUniformBuffer(int buffer, int offset, ByteBuffer data) {
        CoreContext.gl30.glBindBuffer(GL30.GL_UNIFORM_BUFFER, buffer);
        CoreContext.gl30.glBufferSubData(GL30.GL_UNIFORM_BUFFER, offset, data.remaining(), data);
        CoreContext.gl30.glBindBuffer(GL30.GL_UNIFORM_BUFFER, 0);
    }

    // UBO Deletion \\

    static void deleteUniformBuffer(int buffer) {
        IntBuffer buf = BufferUtils.newIntBuffer(1);
        buf.put(buffer).flip();
        CoreContext.gl30.glDeleteBuffers(1, buf);
    }
}
