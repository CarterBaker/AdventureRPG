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
     * every one a keyframe. A video with sound carries a second stream of
     * 16-bit stereo PCM, each frame's stretch of sound written as its own
     * chunk right after the frame. The file is split into RIFF segments of at
     * most a gigabyte so it may grow without limit: the first carries the
     * header list and an old-style index of every chunk in the order written,
     * and each closes its frame list with a standard index for each stream
     * that the stream's super index in the header points at. The header list
     * is written up front at its full size, its counts and super indexes
     * filled in once the last segment closes.
     */

    // File
    private FileChannel channel;
    private OutputStream channelOutput;
    private ByteBuffer headerBuffer;
    private int headerListBytes;

    // JPEG
    private ImageWriter jpegWriter;
    private ImageWriteParam jpegParam;
    private BufferedImage frameImage;
    private byte[] framePixels;
    private ByteArrayOutputStream jpegBytes;

    // Audio
    private ByteBuffer audioBuffer;
    private int audioChunkBytes;

    // Segment
    private long segmentStart;
    private long frameListStart;
    private boolean firstSegment;
    private LongArrayList segmentFrameOffsets;
    private IntArrayList segmentFrameSizes;
    private LongArrayList segmentAudioOffsets;
    private IntArrayList segmentAudioSizes;

    // Super Index
    private LongArrayList indexOffsets;
    private IntArrayList indexSizes;
    private IntArrayList indexFrameCounts;
    private LongArrayList audioIndexOffsets;
    private IntArrayList audioIndexSizes;
    private IntArrayList audioIndexDurations;

    // Totals
    private int totalFrames;
    private int firstSegmentFrames;
    private int largestFrameBytes;
    private long totalAudioFrames;

    // Internal \\

    @Override
    protected void create() {

        // Segment
        this.segmentFrameOffsets = new LongArrayList();
        this.segmentFrameSizes = new IntArrayList();
        this.segmentAudioOffsets = new LongArrayList();
        this.segmentAudioSizes = new IntArrayList();

        // Super Index
        this.indexOffsets = new LongArrayList();
        this.indexSizes = new IntArrayList();
        this.indexFrameCounts = new IntArrayList();
        this.audioIndexOffsets = new LongArrayList();
        this.audioIndexSizes = new IntArrayList();
        this.audioIndexDurations = new IntArrayList();
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
        this.headerListBytes = audio
                ? EditorSetting.RECORDER_AVI_AUDIO_HEADER_LIST_BYTES
                : EditorSetting.RECORDER_AVI_HEADER_LIST_BYTES;
        this.headerBuffer = allocate(headerListBytes);
        this.audioChunkBytes = audioFramesPerFrame * resolveAudioBlockBytes();
        this.audioBuffer = audio ? allocate(audioChunkBytes) : null;

        this.totalFrames = 0;
        this.firstSegmentFrames = 0;
        this.largestFrameBytes = 0;
        this.totalAudioFrames = 0L;
        indexOffsets.clear();
        indexSizes.clear();
        indexFrameCounts.clear();
        audioIndexOffsets.clear();
        audioIndexSizes.clear();
        audioIndexDurations.clear();

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
    protected void writeAudio(short[] samples) {

        audioBuffer.clear();

        for (int i = 0; i < samples.length; i++)
            audioBuffer.putShort(samples[i]);

        audioBuffer.flip();

        try {
            makeRoom(audioChunkBytes);
            writeChunkHeader(EditorSetting.RECORDER_AVI_AUDIO_CHUNK, audioChunkBytes);
            segmentAudioOffsets.add(channel.position());
            segmentAudioSizes.add(audioChunkBytes);
            channel.write(audioBuffer);
            totalAudioFrames += audioFramesPerFrame;
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

    private void writeFrame() throws IOException {

        int frameBytes = jpegBytes.size();

        makeRoom(frameBytes);
        writeChunkHeader(EditorSetting.RECORDER_AVI_FRAME_CHUNK, frameBytes);
        segmentFrameOffsets.add(channel.position());
        segmentFrameSizes.add(frameBytes);
        jpegBytes.writeTo(channelOutput);

        if ((frameBytes & 1) != 0)
            channel.write(ByteBuffer.allocate(1));

        totalFrames++;
        largestFrameBytes = Math.max(largestFrameBytes, frameBytes);
    }

    // A chunk that would carry its segment past the limit starts the next one
    private void makeRoom(int dataBytes) throws IOException {

        long chunkBytes = EditorSetting.RECORDER_AVI_CHUNK_HEADER_BYTES + dataBytes + (dataBytes & 1);

        if ((!segmentFrameSizes.isEmpty() || !segmentAudioSizes.isEmpty())
                && channel.position() - segmentStart + chunkBytes > EditorSetting.RECORDER_AVI_SEGMENT_LIMIT_BYTES) {
            endSegment();
            beginSegment(false);
        }
    }

    // Segments \\

    private void beginSegment(boolean first) throws IOException {

        this.firstSegment = first;
        this.segmentStart = channel.position();
        segmentFrameOffsets.clear();
        segmentFrameSizes.clear();
        segmentAudioOffsets.clear();
        segmentAudioSizes.clear();

        writeList(EditorSetting.RECORDER_AVI_RIFF, 0, first
                ? EditorSetting.RECORDER_AVI_FORM_FIRST
                : EditorSetting.RECORDER_AVI_FORM_EXTENDED);

        if (first)
            channel.write(buildHeaderList());

        this.frameListStart = channel.position();
        writeList(EditorSetting.RECORDER_AVI_LIST, 0, EditorSetting.RECORDER_AVI_FRAME_LIST);
    }

    // Closes the frame list with a standard index for each stream, the first segment adding its old-style index
    private void endSegment() throws IOException {

        int frameCount = segmentFrameSizes.size();

        indexOffsets.add(channel.position());
        indexSizes.add(writeStandardIndex(
                EditorSetting.RECORDER_AVI_STANDARD_INDEX, EditorSetting.RECORDER_AVI_FRAME_CHUNK,
                segmentFrameOffsets, segmentFrameSizes));
        indexFrameCounts.add(frameCount);

        if (audio) {
            audioIndexOffsets.add(channel.position());
            audioIndexSizes.add(writeStandardIndex(
                    EditorSetting.RECORDER_AVI_AUDIO_STANDARD_INDEX, EditorSetting.RECORDER_AVI_AUDIO_CHUNK,
                    segmentAudioOffsets, segmentAudioSizes));
            audioIndexDurations.add(segmentAudioSizes.size() * audioFramesPerFrame);
        }

        patchSize(frameListStart, channel.position());

        if (firstSegment) {
            writeOldIndex();
            this.firstSegmentFrames = frameCount;
        }

        patchSize(segmentStart, channel.position());
    }

    // One stream's standard index — each chunk's data measured from the segment's start — and its whole size
    private int writeStandardIndex(
            String indexFourCC,
            String chunkFourCC,
            LongArrayList offsets,
            IntArrayList sizes) throws IOException {

        int chunkCount = sizes.size();
        int indexBytes = EditorSetting.RECORDER_AVI_STANDARD_INDEX_HEADER_BYTES
                + chunkCount * EditorSetting.RECORDER_AVI_STANDARD_INDEX_ENTRY_BYTES;
        ByteBuffer index = allocate(EditorSetting.RECORDER_AVI_CHUNK_HEADER_BYTES + indexBytes);

        putFourCC(index, indexFourCC);
        index.putInt(indexBytes);
        index.putShort(EditorSetting.RECORDER_AVI_STANDARD_INDEX_LONGS_PER_ENTRY);
        index.put((byte) 0);
        index.put(EditorSetting.RECORDER_AVI_INDEX_OF_CHUNKS);
        index.putInt(chunkCount);
        putFourCC(index, chunkFourCC);
        index.putLong(segmentStart);
        index.putInt(0);

        for (int i = 0; i < chunkCount; i++) {
            index.putInt((int) (offsets.getLong(i) - segmentStart));
            index.putInt(sizes.getInt(i));
        }

        index.flip();
        channel.write(index);
        return EditorSetting.RECORDER_AVI_CHUNK_HEADER_BYTES + indexBytes;
    }

    // Old-style index — every chunk of both streams in the order written, measured from the frame list's type
    private void writeOldIndex() throws IOException {

        int frameCount = segmentFrameSizes.size();
        int audioCount = segmentAudioSizes.size();
        int indexBytes = (frameCount + audioCount) * EditorSetting.RECORDER_AVI_OLD_INDEX_ENTRY_BYTES;
        long listType = frameListStart + EditorSetting.RECORDER_AVI_CHUNK_HEADER_BYTES;
        ByteBuffer index = allocate(EditorSetting.RECORDER_AVI_CHUNK_HEADER_BYTES + indexBytes);
        int frame = 0;
        int sound = 0;

        putFourCC(index, EditorSetting.RECORDER_AVI_OLD_INDEX);
        index.putInt(indexBytes);

        while (frame < frameCount || sound < audioCount) {

            boolean frameNext = sound == audioCount || frame < frameCount
                    && segmentFrameOffsets.getLong(frame) < segmentAudioOffsets.getLong(sound);

            if (frameNext) {
                putOldIndexEntry(index, EditorSetting.RECORDER_AVI_FRAME_CHUNK,
                        segmentFrameOffsets.getLong(frame), segmentFrameSizes.getInt(frame), listType);
                frame++;
            } else {
                putOldIndexEntry(index, EditorSetting.RECORDER_AVI_AUDIO_CHUNK,
                        segmentAudioOffsets.getLong(sound), segmentAudioSizes.getInt(sound), listType);
                sound++;
            }
        }

        index.flip();
        channel.write(index);
    }

    private void putOldIndexEntry(ByteBuffer index, String fourCC, long dataOffset, int size, long listType) {
        putFourCC(index, fourCC);
        index.putInt(EditorSetting.RECORDER_AVI_KEYFRAME_FLAG);
        index.putInt((int) (dataOffset - EditorSetting.RECORDER_AVI_CHUNK_HEADER_BYTES - listType));
        index.putInt(size);
    }

    // Header List \\

    // The header list at its fixed size, with whatever totals and super indexes the file has reached
    private ByteBuffer buildHeaderList() {

        ByteBuffer header = headerBuffer;
        int microsPerFrame = EditorSetting.RECORDER_AVI_MICROS_PER_SECOND / frameRate;
        int bufferBytes = largestFrameBytes + EditorSetting.RECORDER_AVI_CHUNK_HEADER_BYTES;
        int audioBytesPerSecond = EditorSetting.RECORDER_AUDIO_SAMPLE_RATE * resolveAudioBlockBytes();

        header.clear();

        putList(header, EditorSetting.RECORDER_AVI_LIST,
                headerListBytes - EditorSetting.RECORDER_AVI_CHUNK_HEADER_BYTES,
                EditorSetting.RECORDER_AVI_HEADER_LIST);

        putChunkHeader(header, EditorSetting.RECORDER_AVI_MAIN_HEADER, EditorSetting.RECORDER_AVI_MAIN_HEADER_BYTES);
        header.putInt(microsPerFrame);
        header.putInt(largestFrameBytes * frameRate + (audio ? audioBytesPerSecond : 0));
        header.putInt(0);
        header.putInt(EditorSetting.RECORDER_AVI_HAS_INDEX_FLAG);
        header.putInt(firstSegmentFrames);
        header.putInt(0);
        header.putInt(audio ? EditorSetting.RECORDER_AVI_AUDIO_STREAM_COUNT : EditorSetting.RECORDER_AVI_STREAM_COUNT);
        header.putInt(bufferBytes);
        header.putInt(width);
        header.putInt(height);
        putZeros(header, EditorSetting.RECORDER_AVI_MAIN_HEADER_RESERVED_BYTES);

        putVideoStream(header, bufferBytes);

        if (audio)
            putAudioStream(header, audioBytesPerSecond);

        putList(header, EditorSetting.RECORDER_AVI_LIST, EditorSetting.RECORDER_AVI_EXTENDED_LIST_BYTES,
                EditorSetting.RECORDER_AVI_EXTENDED_LIST);
        putChunkHeader(
                header, EditorSetting.RECORDER_AVI_EXTENDED_HEADER, EditorSetting.RECORDER_AVI_EXTENDED_HEADER_BYTES);
        header.putInt(totalFrames);
        putZeros(header, EditorSetting.RECORDER_AVI_EXTENDED_HEADER_BYTES - Integer.BYTES);

        header.flip();
        return header;
    }

    private void putVideoStream(ByteBuffer header, int bufferBytes) {

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

        putSuperIndex(header, EditorSetting.RECORDER_AVI_FRAME_CHUNK, indexOffsets, indexSizes, indexFrameCounts);
    }

    // The sound stream: PCM counted in blocks of one frame of both sides, so its rate is its sample rate
    private void putAudioStream(ByteBuffer header, int audioBytesPerSecond) {

        short blockBytes = (short) resolveAudioBlockBytes();

        putList(header, EditorSetting.RECORDER_AVI_LIST, EditorSetting.RECORDER_AVI_AUDIO_STREAM_LIST_BYTES,
                EditorSetting.RECORDER_AVI_STREAM_LIST);

        putChunkHeader(
                header, EditorSetting.RECORDER_AVI_STREAM_HEADER, EditorSetting.RECORDER_AVI_STREAM_HEADER_BYTES);
        putFourCC(header, EditorSetting.RECORDER_AVI_AUDIO_STREAM);
        header.putInt(0);
        header.putInt(0);
        header.putShort((short) 0);
        header.putShort((short) 0);
        header.putInt(0);
        header.putInt(blockBytes);
        header.putInt(audioBytesPerSecond);
        header.putInt(0);
        header.putInt((int) totalAudioFrames);
        header.putInt(audioChunkBytes);
        header.putInt(EditorSetting.RECORDER_AVI_DEFAULT_QUALITY);
        header.putInt(blockBytes);
        putZeros(header, EditorSetting.RECORDER_AVI_FRAME_RECT_BYTES);

        putChunkHeader(
                header, EditorSetting.RECORDER_AVI_STREAM_FORMAT, EditorSetting.RECORDER_AVI_WAVE_FORMAT_BYTES);
        header.putShort(EditorSetting.RECORDER_AVI_WAVE_FORMAT_PCM);
        header.putShort((short) EditorSetting.RECORDER_AUDIO_CHANNELS);
        header.putInt(EditorSetting.RECORDER_AUDIO_SAMPLE_RATE);
        header.putInt(audioBytesPerSecond);
        header.putShort(blockBytes);
        header.putShort((short) (EditorSetting.RECORDER_AUDIO_SAMPLE_BYTES * Byte.SIZE));
        header.putShort((short) 0);

        putSuperIndex(header, EditorSetting.RECORDER_AVI_AUDIO_CHUNK,
                audioIndexOffsets, audioIndexSizes, audioIndexDurations);
    }

    // A stream's super index, one entry for each segment's standard index of it and the time it covers
    private void putSuperIndex(
            ByteBuffer header,
            String chunkFourCC,
            LongArrayList offsets,
            IntArrayList sizes,
            IntArrayList durations) {

        putChunkHeader(header, EditorSetting.RECORDER_AVI_SUPER_INDEX, EditorSetting.RECORDER_AVI_SUPER_INDEX_BYTES);
        header.putShort(EditorSetting.RECORDER_AVI_SUPER_INDEX_LONGS_PER_ENTRY);
        header.put((byte) 0);
        header.put(EditorSetting.RECORDER_AVI_INDEX_OF_INDEXES);
        header.putInt(offsets.size());
        putFourCC(header, chunkFourCC);
        putZeros(header, EditorSetting.RECORDER_AVI_SUPER_INDEX_RESERVED_BYTES);

        for (int i = 0; i < EditorSetting.RECORDER_AVI_SUPER_INDEX_ENTRIES; i++) {

            boolean used = i < offsets.size();

            header.putLong(used ? offsets.getLong(i) : 0L);
            header.putInt(used ? sizes.getInt(i) : 0);
            header.putInt(used ? durations.getInt(i) : 0);
        }
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

    // The bytes of one frame of sound, both sides of it
    private int resolveAudioBlockBytes() {
        return EditorSetting.RECORDER_AUDIO_CHANNELS * EditorSetting.RECORDER_AUDIO_SAMPLE_BYTES;
    }
}
