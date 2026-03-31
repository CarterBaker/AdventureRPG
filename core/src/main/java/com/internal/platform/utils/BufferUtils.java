package com.internal.platform.utils;

import java.nio.*;

public class BufferUtils {
    public static IntBuffer newIntBuffer(int cap) { return ByteBuffer.allocateDirect(cap * Integer.BYTES).order(ByteOrder.nativeOrder()).asIntBuffer(); }
    public static FloatBuffer newFloatBuffer(int cap) { return ByteBuffer.allocateDirect(cap * Float.BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer(); }
    public static ByteBuffer newByteBuffer(int cap) { return ByteBuffer.allocateDirect(cap).order(ByteOrder.nativeOrder()); }
}
