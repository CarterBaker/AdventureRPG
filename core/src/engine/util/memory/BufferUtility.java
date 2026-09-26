package engine.util.memory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import engine.root.EngineSetting;
import engine.root.EngineUtility;

public class BufferUtility extends EngineUtility {

    /*
     * Factory for native-order direct NIO buffers, ready for GL upload. The
     * scratch helpers return the given buffer cleared when it is already big
     * enough, and a larger replacement otherwise, so callers reuse one buffer
     * for every upload instead of allocating off-heap memory per call.
     */

    // Allocation \\

    public static IntBuffer newIntBuffer(int capacity) {
        return newByteBuffer(capacity * Integer.BYTES).asIntBuffer();
    }

    public static FloatBuffer newFloatBuffer(int capacity) {
        return newByteBuffer(capacity * Float.BYTES).asFloatBuffer();
    }

    public static ShortBuffer newShortBuffer(int capacity) {
        return newByteBuffer(capacity * Short.BYTES).asShortBuffer();
    }

    public static ByteBuffer newByteBuffer(int capacity) {
        return ByteBuffer.allocateDirect(capacity).order(ByteOrder.nativeOrder());
    }

    // Scratch \\

    public static FloatBuffer ensureCapacity(FloatBuffer scratch, int floatCount) {

        if (scratch != null && scratch.capacity() >= floatCount)
            return scratch.clear();

        return newFloatBuffer(growCapacity(scratch == null ? 0 : scratch.capacity(), floatCount));
    }

    public static ShortBuffer ensureCapacity(ShortBuffer scratch, int shortCount) {

        if (scratch != null && scratch.capacity() >= shortCount)
            return scratch.clear();

        return newShortBuffer(growCapacity(scratch == null ? 0 : scratch.capacity(), shortCount));
    }

    private static int growCapacity(int currentCapacity, int requiredCapacity) {
        return Math.max(requiredCapacity, currentCapacity * EngineSetting.SCRATCH_BUFFER_GROWTH_FACTOR);
    }
}
