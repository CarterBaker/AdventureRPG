package editor.bootstrap.recorderpipeline.video;

import java.nio.ByteBuffer;

import org.jcodec.codecs.h264.H264Encoder;
import org.jcodec.codecs.h264.encode.RateControl;
import org.jcodec.codecs.h264.io.model.SeqParameterSet;
import org.jcodec.codecs.h264.io.model.SliceType;
import org.jcodec.codecs.h264.io.model.VUIParameters;
import org.jcodec.common.Codec;
import org.jcodec.common.VideoEncoder.EncodedFrame;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.common.model.Size;
import org.jcodec.containers.mp4.Brand;

import editor.bootstrap.recorderpipeline.util.VideoFrameUtility;
import editor.runtime.EditorSetting;

public class H264VideoEncoderInstance extends QuickTimeVideoEncoderInstance implements RateControl {

    /*
     * Writes H.264 in an MP4, the video to watch and share. Every frame is
     * coded at one fixed high quality, keyframes a second apart; this is also
     * the encoder's rate control, which only raises a macroblock's quantiser
     * when it would not fit the packet and eases it back once it fits with
     * room to spare. The stream names its level from the export's frame size
     * and rate, its studio-range BT.709 colour and its frame rate, so players
     * show it as rendered.
     */

    // Encoder
    private H264Encoder encoder;

    // Rate Control
    private int pictureQp;
    private int bitsPerMacroblock;
    private int raisedQp;
    private boolean justChanged;

    // Format \\

    @Override
    protected Brand getBrand() {
        return Brand.MP4;
    }

    @Override
    protected Codec getCodec() {
        return Codec.H264;
    }

    @Override
    protected ColorSpace getColorSpace() {
        return ColorSpace.YUV420J;
    }

    @Override
    protected void openEncoder() {

        this.encoder = new H264Encoder(this) {

            @Override
            public SeqParameterSet initSPS(Size size) {
                return describeStream(super.initSPS(size));
            }
        };

        encoder.setKeyInterval(frameRate * EditorSetting.RECORDER_H264_KEYFRAME_SECONDS);
        encoder.setMotionSearchRange(EditorSetting.RECORDER_H264_MOTION_SEARCH_RANGE);
    }

    @Override
    protected void toPicture(byte[] frame, Picture picture) {
        VideoFrameUtility.toYuv420(frame, width, height, picture);
    }

    @Override
    protected EncodedFrame encodePicture(Picture picture, ByteBuffer packetBuffer) {
        return encoder.encodeFrame(picture, packetBuffer);
    }

    // Stream \\

    private SeqParameterSet describeStream(SeqParameterSet sps) {

        VUIParameters vui = new VUIParameters();

        vui.videoSignalTypePresentFlag = true;
        vui.videoFormat = EditorSetting.RECORDER_H264_VIDEO_FORMAT_UNSPECIFIED;
        vui.videoFullRangeFlag = false;
        vui.colourDescriptionPresentFlag = true;
        vui.colourPrimaries = EditorSetting.RECORDER_COLOR_BT709;
        vui.transferCharacteristics = EditorSetting.RECORDER_COLOR_BT709;
        vui.matrixCoefficients = EditorSetting.RECORDER_COLOR_BT709;
        vui.timingInfoPresentFlag = true;
        vui.numUnitsInTick = 1;
        vui.timeScale = frameRate * EditorSetting.RECORDER_H264_TICKS_PER_FRAME;
        vui.fixedFrameRateFlag = true;

        sps.levelIdc = resolveLevel(sps);
        sps.vuiParams = vui;
        return sps;
    }

    // The lowest level whose frame size and macroblock rate hold the export, the highest when none does
    private int resolveLevel(SeqParameterSet sps) {

        int frameMacroblocks = (sps.picWidthInMbsMinus1 + 1) * (sps.picHeightInMapUnitsMinus1 + 1);
        int macroblockRate = frameMacroblocks * frameRate;

        for (int i = 0; i < EditorSetting.RECORDER_H264_LEVELS.length; i++)
            if (frameMacroblocks <= EditorSetting.RECORDER_H264_LEVEL_FRAME_MACROBLOCKS[i]
                    && macroblockRate <= EditorSetting.RECORDER_H264_LEVEL_MACROBLOCK_RATES[i])
                return EditorSetting.RECORDER_H264_LEVELS[i];

        return EditorSetting.RECORDER_H264_LEVELS[EditorSetting.RECORDER_H264_LEVELS.length - 1];
    }

    // Rate Control \\

    @Override
    public int startPicture(Size size, int maxSize, SliceType sliceType) {

        int macroblocks = ((size.getWidth() + EditorSetting.RECORDER_MACROBLOCK_PIXELS - 1)
                / EditorSetting.RECORDER_MACROBLOCK_PIXELS)
                * ((size.getHeight() + EditorSetting.RECORDER_MACROBLOCK_PIXELS - 1)
                        / EditorSetting.RECORDER_MACROBLOCK_PIXELS);

        this.pictureQp = sliceType == SliceType.I
                ? EditorSetting.RECORDER_H264_INTRA_QP
                : EditorSetting.RECORDER_H264_INTER_QP;
        this.bitsPerMacroblock = (int) ((long) maxSize * Byte.SIZE / macroblocks);
        this.raisedQp = 0;
        this.justChanged = false;

        return pictureQp;
    }

    @Override
    public int initialQpDelta() {
        return 0;
    }

    // Raises the quantiser while a macroblock overruns its share, and eases it back once one fits with room to spare
    @Override
    public int accept(int bits) {

        if (bits >= bitsPerMacroblock && pictureQp + raisedQp < EditorSetting.RECORDER_H264_MAX_QP) {
            this.raisedQp++;
            this.justChanged = true;
            return 1;
        }

        if (raisedQp > 0 && !justChanged
                && bitsPerMacroblock - bits > bitsPerMacroblock >> EditorSetting.RECORDER_H264_EASE_SHIFT) {
            this.raisedQp--;
            this.justChanged = true;
            return -1;
        }

        this.justChanged = false;
        return 0;
    }
}
