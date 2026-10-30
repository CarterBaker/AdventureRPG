package editor.bootstrap.recorderpipeline.video;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.jcodec.common.AudioFormat;
import org.jcodec.common.Codec;
import org.jcodec.common.VideoCodecMeta;
import org.jcodec.common.VideoEncoder.EncodedFrame;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Packet.FrameType;
import org.jcodec.common.model.Picture;
import org.jcodec.common.model.Rational;
import org.jcodec.common.model.Rect;
import org.jcodec.common.model.Size;
import org.jcodec.common.model.Unit;
import org.jcodec.containers.mp4.Brand;
import org.jcodec.containers.mp4.boxes.ColorExtension;
import org.jcodec.containers.mp4.muxer.CodecMP4MuxerTrack;
import org.jcodec.containers.mp4.muxer.MP4Muxer;
import org.jcodec.containers.mp4.muxer.PCMMP4MuxerTrack;

import editor.runtime.EditorSetting;

public abstract class QuickTimeVideoEncoderInstance extends VideoEncoderInstance {

    /*
     * Writes a video in the MP4 and MOV family with nothing but the engine's
     * own libraries. Each frame is turned into the picture the format's
     * encoder takes — whole macroblocks, the frame cropped from them — and
     * the encoded frame is written to the file at once as its own chunk, so
     * one picture, one packet and one packet buffer serve the whole export.
     * Frames are stamped one tick of the export's rate apart, and the track
     * names its colour as BT.709. A video with sound carries it as a track
     * of little-endian 16-bit stereo samples, each frame's stretch written as
     * its own chunk beside the frame through one buffer. Formats supply their
     * brand, codec, colour space, how a frame becomes the picture, and the
     * encoding itself.
     */

    // File
    private SeekableByteChannel channel;
    private MP4Muxer muxer;
    private CodecMP4MuxerTrack track;
    private PCMMP4MuxerTrack audioTrack;

    // Frame
    private Picture picture;
    private ByteBuffer packetBuffer;
    private Packet packet;
    private long frameNumber;

    // Audio
    private ByteBuffer audioBuffer;

    // Management \\

    @Override
    protected boolean openFile() {

        int codedWidth = toMacroblocks(width);
        int codedHeight = toMacroblocks(height);

        this.picture = Picture.createCropped(codedWidth, codedHeight, getColorSpace(), new Rect(0, 0, width, height));
        this.packetBuffer = ByteBuffer.allocate(
                codedWidth * codedHeight * EditorSetting.RECORDER_PACKET_BYTES_PER_PIXEL);
        this.packet = Packet.createPacket(null, 0L, frameRate, 1L, 0L, FrameType.KEY, null);
        this.frameNumber = 0L;

        try {
            this.channel = NIOUtils.writableChannel(file);
            this.muxer = MP4Muxer.createMP4Muxer(channel, getBrand());
            this.track = (CodecMP4MuxerTrack) muxer.addVideoTrack(
                    getCodec(),
                    VideoCodecMeta.createSimpleVideoCodecMeta(new Size(width, height), getColorSpace()));
            track.setTgtChunkDuration(new Rational(EditorSetting.RECORDER_FRAMES_PER_CHUNK, 1), Unit.FRAME);
            describeColor();
            openAudio();
            openEncoder();
            return true;
        } catch (IOException e) {
            fail(EditorSetting.RECORDER_MESSAGE_WRITE_FAILED + e.getMessage());
            abort();
            return false;
        }
    }

    @Override
    public void encodeFrame(byte[] frame) {

        if (hasFailed())
            return;

        toPicture(frame, picture);
        packetBuffer.clear();

        EncodedFrame encoded = encodePicture(picture, packetBuffer);

        packet.setData(encoded.getData());
        packet.setPts(frameNumber);
        packet.frameNo = frameNumber;
        packet.setFrameType(encoded.isKeyFrame() ? FrameType.KEY : FrameType.INTER);

        try {
            track.addFrame(packet);
            frameNumber++;
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
            audioTrack.addSamples(audioBuffer);
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
            muxer.finish();
            channel.close();
        } catch (IOException e) {
            fail(EditorSetting.RECORDER_MESSAGE_WRITE_FAILED + e.getMessage());
            abort();
        }

        return !hasFailed();
    }

    @Override
    public void abort() {

        try {
            if (channel != null && channel.isOpen())
                channel.close();
        } catch (IOException e) {
            fail(EditorSetting.RECORDER_MESSAGE_WRITE_FAILED + e.getMessage());
        }
    }

    // Format \\

    protected abstract Brand getBrand();

    protected abstract Codec getCodec();

    protected abstract ColorSpace getColorSpace();

    protected abstract void openEncoder();

    protected abstract void toPicture(byte[] frame, Picture picture);

    protected abstract EncodedFrame encodePicture(Picture picture, ByteBuffer packetBuffer);

    // Utility \\

    // The sound track, its chunks one video frame's stretch long so each is written as soon as it is given
    private void openAudio() {

        if (!audio)
            return;

        this.audioTrack = muxer.addPCMAudioTrack(AudioFormat.STEREO_S16_LE(EditorSetting.RECORDER_AUDIO_SAMPLE_RATE));
        audioTrack.setTgtChunkDuration(new Rational(audioFramesPerFrame, 1), Unit.FRAME);
        this.audioBuffer = ByteBuffer.allocate(audioFramesPerFrame * EditorSetting.RECORDER_AUDIO_CHANNELS
                * EditorSetting.RECORDER_AUDIO_SAMPLE_BYTES).order(ByteOrder.LITTLE_ENDIAN);
    }

    private void describeColor() {
        track.getEntries().get(0).add(ColorExtension.createColorExtension(
                (short) EditorSetting.RECORDER_COLOR_BT709,
                (short) EditorSetting.RECORDER_COLOR_BT709,
                (short) EditorSetting.RECORDER_COLOR_BT709));
    }

    private int toMacroblocks(int pixels) {
        return (pixels + EditorSetting.RECORDER_MACROBLOCK_PIXELS - 1)
                / EditorSetting.RECORDER_MACROBLOCK_PIXELS * EditorSetting.RECORDER_MACROBLOCK_PIXELS;
    }
}
