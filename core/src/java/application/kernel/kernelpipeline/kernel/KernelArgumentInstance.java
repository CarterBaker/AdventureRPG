package application.kernel.kernelpipeline.kernel;

import java.nio.Buffer;
import java.nio.ByteBuffer;

import org.lwjgl.system.MemoryUtil;

import engine.root.EngineSetting;
import engine.root.InstancePackage;
import engine.util.memory.BufferUtility;

public class KernelArgumentInstance extends InstancePackage {

    /*
     * One kernel call's argument block: KERNEL_ARGUMENT_SLOTS eight-byte slots
     * in a native-order direct buffer, written in order from a cursor. Ints
     * travel as longs, floats as doubles and buffers as the address of their
     * first element, whatever their position; reset() rewinds the cursor for
     * the next call. Each caller owns one per thread and reuses it, so a call
     * allocates nothing.
     */

    // Slots
    private ByteBuffer slots;
    private long address;
    private int cursor;

    // Internal \\

    @Override
    protected void create() {

        // Slots
        this.slots = BufferUtility.newByteBuffer(EngineSetting.KERNEL_ARGUMENT_SLOTS
                * EngineSetting.KERNEL_ARGUMENT_SLOT_BYTES);
        this.address = MemoryUtil.memAddress0(slots);
    }

    // Write \\

    public void reset() {
        cursor = 0;
    }

    public void putInt(int value) {
        putLong(value);
    }

    public void putLong(long value) {
        slots.putLong(nextSlotOffset(), value);
    }

    public void putFloat(float value) {
        putDouble(value);
    }

    public void putDouble(double value) {
        slots.putDouble(nextSlotOffset(), value);
    }

    public void putBuffer(Buffer buffer) {

        if (!buffer.isDirect())
            throwException("A kernel argument buffer must be direct: the kernel reads it by address");

        putLong(MemoryUtil.memAddress0(buffer));
    }

    private int nextSlotOffset() {

        if (cursor >= EngineSetting.KERNEL_ARGUMENT_SLOTS)
            throwException("A kernel call wrote more than its " + EngineSetting.KERNEL_ARGUMENT_SLOTS
                    + " argument slots");

        return cursor++ * EngineSetting.KERNEL_ARGUMENT_SLOT_BYTES;
    }

    // Accessible \\

    public long getAddress() {
        return address;
    }
}
