package editor.bootstrap.recorderpipeline.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Base64;

import editor.runtime.EditorSetting;
import engine.root.EngineUtility;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.LongArrayList;

public class RecordingArpgUtility extends EngineUtility {

    /*
     * Packs a recording's sample columns into one ARPG string value and back.
     * A track's samples are thousands of numbers, so they are written as one
     * little-endian block — a sample count, then each column whole, in the
     * order the caller writes them — and carried as Base64 text rather than
     * as one ARPG element per number. A block that is not valid Base64 or not
     * the size its count calls for reads back as null. Bytes that are already
     * packed, as the interface's frames are, go to text and back whole.
     */

    // Write \\

    public static ByteBuffer allocateBlock(int sampleCount, int bytesPerSample) {

        ByteBuffer block = ByteBuffer.allocate(
                EditorSetting.RECORDER_BLOCK_HEADER_BYTES + sampleCount * bytesPerSample);
        block.order(ByteOrder.LITTLE_ENDIAN);
        block.putInt(sampleCount);

        return block;
    }

    public static void writeFloats(ByteBuffer block, FloatArrayList column) {
        for (int i = 0; i < column.size(); i++)
            block.putFloat(column.getFloat(i));
    }

    public static void writeLongs(ByteBuffer block, LongArrayList column) {
        for (int i = 0; i < column.size(); i++)
            block.putLong(column.getLong(i));
    }

    public static void writeInts(ByteBuffer block, IntArrayList column) {
        for (int i = 0; i < column.size(); i++)
            block.putInt(column.getInt(i));
    }

    public static void writeDoubles(ByteBuffer block, DoubleArrayList column) {
        for (int i = 0; i < column.size(); i++)
            block.putDouble(column.getDouble(i));
    }

    public static String toText(ByteBuffer block) {
        return toText(block.array());
    }

    public static String toText(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    // Read \\

    // The bytes behind the text, or null when it is not valid Base64
    public static byte[] fromText(String text) {

        try {
            return Base64.getDecoder().decode(text);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // The block behind the text, positioned on its first column, or null when it is malformed
    public static ByteBuffer fromText(String text, int bytesPerSample) {

        byte[] bytes = fromText(text);

        if (bytes == null || bytes.length < EditorSetting.RECORDER_BLOCK_HEADER_BYTES)
            return null;

        ByteBuffer block = ByteBuffer.wrap(bytes);
        block.order(ByteOrder.LITTLE_ENDIAN);

        int sampleCount = block.getInt();

        if (sampleCount < 0 || bytes.length != EditorSetting.RECORDER_BLOCK_HEADER_BYTES
                + (long) sampleCount * bytesPerSample)
            return null;

        return block;
    }

    public static int readSampleCount(ByteBuffer block) {
        return block.getInt(0);
    }

    public static void readFloats(ByteBuffer block, int sampleCount, FloatArrayList column) {

        column.ensureCapacity(sampleCount);

        for (int i = 0; i < sampleCount; i++)
            column.add(block.getFloat());
    }

    public static void readLongs(ByteBuffer block, int sampleCount, LongArrayList column) {

        column.ensureCapacity(sampleCount);

        for (int i = 0; i < sampleCount; i++)
            column.add(block.getLong());
    }

    public static void readInts(ByteBuffer block, int sampleCount, IntArrayList column) {

        column.ensureCapacity(sampleCount);

        for (int i = 0; i < sampleCount; i++)
            column.add(block.getInt());
    }

    public static void readDoubles(ByteBuffer block, int sampleCount, DoubleArrayList column) {

        column.ensureCapacity(sampleCount);

        for (int i = 0; i < sampleCount; i++)
            column.add(block.getDouble());
    }
}
