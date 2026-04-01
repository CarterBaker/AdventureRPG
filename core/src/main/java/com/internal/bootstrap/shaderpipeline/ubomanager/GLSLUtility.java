package com.internal.bootstrap.shaderpipeline.ubomanager;

import com.internal.platform.PlatformRuntime;
import com.internal.platform.graphics.GL20;
import com.internal.platform.graphics.GL30;
import com.internal.platform.utils.BufferUtils;
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
        return PlatformRuntime.gl30.glGenBuffer();
    }

    static void allocateUniformBuffer(int buffer, int sizeBytes) {
        PlatformRuntime.gl30.glBindBuffer(GL30.GL_UNIFORM_BUFFER, buffer);
        PlatformRuntime.gl30.glBufferData(GL30.GL_UNIFORM_BUFFER, sizeBytes, null, GL20.GL_DYNAMIC_DRAW);
        PlatformRuntime.gl30.glBindBuffer(GL30.GL_UNIFORM_BUFFER, 0);
    }

    static void bindUniformBufferBase(int buffer, int bindingPoint) {
        PlatformRuntime.gl30.glBindBufferBase(GL30.GL_UNIFORM_BUFFER, bindingPoint, buffer);
    }

    // UBO Upload \\

    static void updateUniformBuffer(int buffer, int offset, ByteBuffer data) {
        PlatformRuntime.gl30.glBindBuffer(GL30.GL_UNIFORM_BUFFER, buffer);
        PlatformRuntime.gl30.glBufferSubData(GL30.GL_UNIFORM_BUFFER, offset, data.remaining(), data);
        PlatformRuntime.gl30.glBindBuffer(GL30.GL_UNIFORM_BUFFER, 0);
    }

    // UBO Deletion \\

    static void deleteUniformBuffer(int buffer) {
        IntBuffer buf = BufferUtils.newIntBuffer(1);
        buf.put(buffer).flip();
        PlatformRuntime.gl30.glDeleteBuffers(1, buf);
    }
}
