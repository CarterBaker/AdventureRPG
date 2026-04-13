package engine.util.memory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class BufferUtils {

    /*
     * Factory for native-order direct NIO buffers. All allocations are
     * direct and native-order — ready for GL upload without conversion.
     */

    public static IntBuffer newIntBuffer(int capacity) {
        return ByteBuffer.allocateDirect(capacity * Integer.BYTES).order(ByteOrder.nativeOrder()).asIntBuffer();
    }

    public static FloatBuffer newFloatBuffer(int capacity) {
        return ByteBuffer.allocateDirect(capacity * Float.BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
    }

    public static ByteBuffer newByteBuffer(int capacity) {
        return ByteBuffer.allocateDirect(capacity).order(ByteOrder.nativeOrder());
    }
}