package editor.bootstrap.recorderpipeline.video;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;

import editor.runtime.EditorSetting;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.LongArrayList;

public class AviVideoEncoderInstance extends VideoEncoderInstance {

    /*
     * Writes a Motion JPEG video in an OpenDML AVI with nothing but the JDK,
     * so an export never needs anything installed. Every frame is one JPEG,
     * every one a keyframe. The file is split into RIFF segments of at most a
     * gigabyte so it may grow without limit: the first carries the header
     * list and an old-style index, and each closes its frame list with a
     * standard index that the header's super index points at. The header list
     * is written up front at its full size, its counts and super index filled
     * in once the last segment closes.
     */

    // File
    private FileChannel channel;
    private OutputStream channelOutput;
    private ByteBuffer headerBuffer;

    // JPEG
    private ImageWriter jpegWriter;
    private ImageWriteParam jpegParam;
    private BufferedImage frameImage;
    private byte[] framePixels;
    private ByteArrayOutputStream jpegBytes;

    // Segment
    private long segmentStart;
    private long frameListStart;
    private boolean firstSegment;
    private LongArrayList segmentFrameOffsets;
    private IntArrayList segmentFrameSizes;

    // Super Index
    private LongArrayList indexOffsets;
    private IntArrayList indexSizes;
    private IntArrayList indexFrameCounts;

    // Totals
    private int totalFrames;
    private int firstSegmentFrames;
    private int largestFrameBytes;

    // Internal \\

    @Override
    protected void create() {

        // Segment
        this.segmentFrameOffsets = new LongArrayList();
        this.segmentFrameSizes = new IntArrayList();

        // Super Index
        this.indexOffsets = new LongArrayList();
        this.indexSizes = new IntArrayList();
        this.indexFrameCounts = new IntArrayList();
    }

    // Management \\

    @Override
    protected boolean openFile() {

        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(EditorSetting.RECORDER_JPEG_FORMAT);

        if (!writers.hasNext()) {
            fail(EditorSetting.RECORDER_MESSAGE_NO_JPEG);
            return false;
        }

        this.jpegWriter = writers.next();
        this.jpegParam = jpegWriter.getDefaultWriteParam();
        jpegParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        jpegParam.setCompressionQuality(EditorSetting.RECORDER_JPEG_QUALITY);

        this.frameImage = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
        this.framePixels = ((DataBufferByte) frameImage.getRaster().getDataBuffer()).getData();
        this.jpegBytes = new ByteArrayOutputStream(width * height);
        this.headerBuffer = allocate(EditorSetting.RECORDER_AVI_HEADER_LIST_BYTES);

        this.totalFrames = 0;
        this.firstSegmentFrames = 0;
        this.largestFrameBytes = 0;
        indexOffsets.clear();
        indexSizes.clear();
        indexFrameCounts.clear();

        try {
            this.channel = FileChannel.open(
                    file.toPath(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE);
            this.channelOutput = Channels.newOutputStream(channel);
            beginSegment(true);
            return true;
        } catch (IOException e) {
            fail(EditorSetting.RECORDER_MESSAGE_WRITE_FAILED + e.getMessage());
            return false;
        }
    }

    @Override
    public void encodeFrame(byte[] frame) {

        if (hasFailed())
            return;

        try {
            compressFrame(frame);
            writeFrame();
        } catch (IOException e) {
            fail(EditorSetting.RECORDER_MESSAGE_WRITE_FAILED + e.getMessage());
        }
    }

    @Override
    public boolean close() {

        if (hasFailed()) {
            abort();
            return false;
        }

        try {
            endSegment();

            if (indexOffsets.size() > EditorSetting.RECORDER_AVI_SUPER_INDEX_ENTRIES)
                fail(EditorSetting.RECORDER_MESSAGE_AVI_TOO_LONG);
            else
                channel.write(buildHeaderList(), EditorSetting.RECORDER_AVI_RIFF_HEADER_BYTES);

            channel.close();
        } catch (IOException e) {
            fail(EditorSetting.RECORDER_MESSAGE_WRITE_FAILED + e.getMessage());
        }

        jpegWriter.dispose();
        return !hasFailed();
    }

    @Override
    public void abort() {

        if (jpegWriter != null)
            jpegWriter.dispose();

        try {
            if (channel != null && channel.isOpen())
                channel.close();
        } catch (IOException e) {
            fail(EditorSetting.RECORDER_MESSAGE_WRITE_FAILED + e.getMessage());
        }
    }

    // Frames \\

    private void compressFrame(byte[] frame) throws IOException {

        System.arraycopy(frame, 0, framePixels, 0, framePixels.length);
        jpegBytes.reset();

        try (ImageOutputStream imageOutput = new MemoryCacheImageOutputStream(jpegBytes)) {
            jpegWriter.setOutput(imageOutput);
            jpegWriter.write(null, new IIOImage(frameImage, null, null), jpegParam);
        }
    }

    // A frame that would carry its segment past the limit starts the next one
    private void writeFrame() throws IOException {

        int frameBytes = jpegBytes.size();
        long chunkBytes = EditorSetting.RECORDER_AVI_CHUNK_HEADER_BYTES + frameBytes + (frameBytes & 1);

        if (!segmentFrameSizes.isEmpty()
                && channel.position() - segmentStart + chunkBytes > EditorSetting.RECORDER_AVI_SEGMENT_LIMIT_BYTES) {
            endSegment();
            beginSegment(false);
        }

        writeChunkHeader(EditorSetting.RECORDER_AVI_FRAME_CHUNK, frameBytes);
        segmentFrameOffsets.add(channel.position());
        segmentFrameSizes.add(frameBytes);
        jpegBytes.writeTo(channelOutput);

        if ((frameBytes & 1) != 0)
            channel.write(ByteBuffer.allocate(1));

        totalFrames++;
        largestFrameBytes = Math.max(largestFrameBytes, frameBytes);
    }

    // Segments \\

    private void beginSegment(boolean first) throws IOException {

        this.firstSegment = first;
        this.segmentStart = channel.position();
        segmentFrameOffsets.clear();
        segmentFrameSizes.clear();

        writeList(EditorSetting.RECORDER_AVI_RIFF, 0, first
                ? EditorSetting.RECORDER_AVI_FORM_FIRST
                : EditorSetting.RECORDER_AVI_FORM_EXTENDED);

        if (first)
            channel.write(buildHeaderList());

        this.frameListStart = channel.position();
        writeList(EditorSetting.RECORDER_AVI_LIST, 0, EditorSetting.RECORDER_AVI_FRAME_LIST);
    }

    // Closes the frame list with its standard index, the first segment adding its old-style index after it
    private void endSegment() throws IOException {

        int frameCount = segmentFrameSizes.size();
        long indexOffset = channel.position();
        int indexBytes = EditorSetting.RECORDER_AVI_STANDARD_INDEX_HEADER_BYTES
                + frameCount * EditorSetting.RECORDER_AVI_STANDARD_INDEX_ENTRY_BYTES;
        ByteBuffer index = allocate(EditorSetting.RECORDER_AVI_CHUNK_HEADER_BYTES + indexBytes);

        putFourCC(index, EditorSetting.RECORDER_AVI_STANDARD_INDEX);
        index.putInt(indexBytes);
        index.putShort(EditorSetting.RECORDER_AVI_STANDARD_INDEX_LONGS_PER_ENTRY);
        index.put((byte) 0);
        index.put(EditorSetting.RECORDER_AVI_INDEX_OF_CHUNKS);
        index.putInt(frameCount);
        putFourCC(index, EditorSetting.RECORDER_AVI_FRAME_CHUNK);
        index.putLong(segmentStart);
        index.putInt(0);

        for (int i = 0; i < frameCount; i++) {
            index.putInt((int) (segmentFrameOffsets.getLong(i) - segmentStart));
            index.putInt(segmentFrameSizes.getInt(i));
        }

        index.flip();
        channel.write(index);

        indexOffsets.add(indexOffset);
        indexSizes.add(EditorSetting.RECORDER_AVI_CHUNK_HEADER_BYTES + indexBytes);
        indexFrameCounts.add(frameCount);

        patchSize(frameListStart, channel.position());

        if (firstSegment) {
            writeOldIndex();
            this.firstSegmentFrames = frameCount;
        }

        patchSize(segmentStart, channel.position());
    }

    // Old-style index — each frame's chunk measured from the frame list's type
    private void writeOldIndex() throws IOException {

        int frameCount = segmentFrameSizes.size();
        int indexBytes = frameCount * EditorSetting.RECORDER_AVI_OLD_INDEX_ENTRY_BYTES;
        long listType = frameListStart + EditorSetting.RECORDER_AVI_CHUNK_HEADER_BYTES;
        ByteBuffer index = allocate(EditorSetting.RECORDER_AVI_CHUNK_HEADER_BYTES + indexBytes);

        putFourCC(index, EditorSetting.RECORDER_AVI_OLD_INDEX);
        index.putInt(indexBytes);

        for (int i = 0; i < frameCount; i++) {
            putFourCC(index, EditorSetting.RECORDER_AVI_FRAME_CHUNK);
            index.putInt(EditorSetting.RECORDER_AVI_KEYFRAME_FLAG);
            index.putInt((int) (segmentFrameOffsets.getLong(i) - EditorSetting.RECORDER_AVI_CHUNK_HEADER_BYTES
                    - listType));
            index.putInt(segmentFrameSizes.getInt(i));
        }

        index.flip();
        channel.write(index);
    }

    // Header List \\

    // The header list at its fixed size, with whatever totals and super index the file has reached
    private ByteBuffer buildHeaderList() {

        ByteBuffer header = headerBuffer;
        int microsPerFrame = EditorSetting.RECORDER_AVI_MICROS_PER_SECOND / frameRate;
        int bufferBytes = largestFrameBytes + EditorSetting.RECORDER_AVI_CHUNK_HEADER_BYTES;

        header.clear();

        putList(header, EditorSetting.RECORDER_AVI_LIST,
                EditorSetting.RECORDER_AVI_HEADER_LIST_BYTES - EditorSetting.RECORDER_AVI_CHUNK_HEADER_BYTES,
                EditorSetting.RECORDER_AVI_HEADER_LIST);

        putChunkHeader(header, EditorSetting.RECORDER_AVI_MAIN_HEADER, EditorSetting.RECORDER_AVI_MAIN_HEADER_BYTES);
        header.putInt(microsPerFrame);
        header.putInt(largestFrameBytes * frameRate);
        header.putInt(0);
        header.putInt(EditorSetting.RECORDER_AVI_HAS_INDEX_FLAG);
        header.putInt(firstSegmentFrames);
        header.putInt(0);
        header.putInt(EditorSetting.RECORDER_AVI_STREAM_COUNT);
        header.putInt(bufferBytes);
        header.putInt(width);
        header.putInt(height);
        putZeros(header, EditorSetting.RECORDER_AVI_MAIN_HEADER_RESERVED_BYTES);

        putList(header, EditorSetting.RECORDER_AVI_LIST, EditorSetting.RECORDER_AVI_STREAM_LIST_BYTES,
                EditorSetting.RECORDER_AVI_STREAM_LIST);

        putChunkHeader(
                header, EditorSetting.RECORDER_AVI_STREAM_HEADER, EditorSetting.RECORDER_AVI_STREAM_HEADER_BYTES);
        putFourCC(header, EditorSetting.RECORDER_AVI_VIDEO_STREAM);
        putFourCC(header, EditorSetting.RECORDER_AVI_CODEC);
        header.putInt(0);
        header.putShort((short) 0);
        header.putShort((short) 0);
        header.putInt(0);
        header.putInt(1);
        header.putInt(frameRate);
        header.putInt(0);
        header.putInt(totalFrames);
        header.putInt(bufferBytes);
        header.putInt(EditorSetting.RECORDER_AVI_DEFAULT_QUALITY);
        header.putInt(0);
        header.putShort((short) 0);
        header.putShort((short) 0);
        header.putShort((short) width);
        header.putShort((short) height);

        putChunkHeader(
                header, EditorSetting.RECORDER_AVI_STREAM_FORMAT, EditorSetting.RECORDER_AVI_BITMAP_HEADER_BYTES);
        header.putInt(EditorSetting.RECORDER_AVI_BITMAP_HEADER_BYTES);
        header.putInt(width);
        header.putInt(height);
        header.putShort(EditorSetting.RECORDER_AVI_PLANES);
        header.putShort(EditorSetting.RECORDER_AVI_BITS_PER_PIXEL);
        putFourCC(header, EditorSetting.RECORDER_AVI_CODEC);
        header.putInt(width * height * EditorSetting.RECORDER_FRAME_BYTES_PER_PIXEL);
        putZeros(header, EditorSetting.RECORDER_AVI_BITMAP_TAIL_BYTES);

        putChunkHeader(header, EditorSetting.RECORDER_AVI_SUPER_INDEX, EditorSetting.RECORDER_AVI_SUPER_INDEX_BYTES);
        header.putShort(EditorSetting.RECORDER_AVI_SUPER_INDEX_LONGS_PER_ENTRY);
        header.put((byte) 0);
        header.put(EditorSetting.RECORDER_AVI_INDEX_OF_INDEXES);
        header.putInt(indexOffsets.size());
        putFourCC(header, EditorSetting.RECORDER_AVI_FRAME_CHUNK);
        putZeros(header, EditorSetting.RECORDER_AVI_SUPER_INDEX_RESERVED_BYTES);

        for (int i = 0; i < EditorSetting.RECORDER_AVI_SUPER_INDEX_ENTRIES; i++) {

            boolean used = i < indexOffsets.size();

            header.putLong(used ? indexOffsets.getLong(i) : 0L);
            header.putInt(used ? indexSizes.getInt(i) : 0);
            header.putInt(used ? indexFrameCounts.getInt(i) : 0);
        }

        putList(header, EditorSetting.RECORDER_AVI_LIST, EditorSetting.RECORDER_AVI_EXTENDED_LIST_BYTES,
                EditorSetting.RECORDER_AVI_EXTENDED_LIST);
        putChunkHeader(
                header, EditorSetting.RECORDER_AVI_EXTENDED_HEADER, EditorSetting.RECORDER_AVI_EXTENDED_HEADER_BYTES);
        header.putInt(totalFrames);
        putZeros(header, EditorSetting.RECORDER_AVI_EXTENDED_HEADER_BYTES - Integer.BYTES);

        header.flip();
        return header;
    }

    // Writing \\

    private void writeList(String fourCC, int size, String type) throws IOException {

        ByteBuffer list = allocate(EditorSetting.RECORDER_AVI_LIST_HEADER_BYTES);

        putList(list, fourCC, size, type);
        list.flip();
        channel.write(list);
    }

    private void writeChunkHeader(String fourCC, int size) throws IOException {

        ByteBuffer chunk = allocate(EditorSetting.RECORDER_AVI_CHUNK_HEADER_BYTES);

        putChunkHeader(chunk, fourCC, size);
        chunk.flip();
        channel.write(chunk);
    }

    // Writes the size of the list or segment starting here, measured to the end given
    private void patchSize(long start, long end) throws IOException {

        ByteBuffer size = allocate(Integer.BYTES);

        size.putInt((int) (end - start - EditorSetting.RECORDER_AVI_CHUNK_HEADER_BYTES));
        size.flip();
        channel.write(size, start + Integer.BYTES);
    }

    private ByteBuffer allocate(int bytes) {
        return ByteBuffer.allocate(bytes).order(ByteOrder.LITTLE_ENDIAN);
    }

    private void putList(ByteBuffer buffer, String fourCC, int size, String type) {
        putChunkHeader(buffer, fourCC, size);
        putFourCC(buffer, type);
    }

    private void putChunkHeader(ByteBuffer buffer, String fourCC, int size) {
        putFourCC(buffer, fourCC);
        buffer.putInt(size);
    }

    private void putFourCC(ByteBuffer buffer, String fourCC) {
        buffer.put(fourCC.getBytes(StandardCharsets.US_ASCII));
    }

    private void putZeros(ByteBuffer buffer, int bytes) {
        for (int i = 0; i < bytes; i++)
            buffer.put((byte) 0);
    }
}
