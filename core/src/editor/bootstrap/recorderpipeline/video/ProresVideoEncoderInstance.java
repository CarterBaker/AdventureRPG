package editor.bootstrap.recorderpipeline.video;

import java.nio.ByteBuffer;

import org.jcodec.codecs.prores.ProresEncoder;
import org.jcodec.common.Codec;
import org.jcodec.common.VideoEncoder.EncodedFrame;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.containers.mp4.Brand;

import editor.bootstrap.recorderpipeline.util.VideoFrameUtility;
import editor.runtime.EditorSetting;

public class ProresVideoEncoderInstance extends QuickTimeVideoEncoderInstance {

    /*
     * Writes ProRes 422 HQ in a MOV, the video to edit. Every frame stands
     * alone at full chroma across each row, so an editor cuts it anywhere
     * without loss. The encoder's frame header names no colour of its own, so
     * each frame's header is set to name the studio-range BT.709 it carries.
     */

    // Encoder
    private ProresEncoder encoder;

    // Format \\

    @Override
    protected Brand getBrand() {
        return Brand.MOV;
    }

    @Override
    protected Codec getCodec() {
        return Codec.PRORES;
    }

    @Override
    protected ColorSpace getColorSpace() {
        return ColorSpace.YUV422;
    }

    @Override
    protected void openEncoder() {
        this.encoder = new ProresEncoder(ProresEncoder.Profile.HQ, false);
    }

    @Override
    protected void toPicture(byte[] frame, Picture picture) {
        VideoFrameUtility.toYuv422(frame, width, height, picture);
    }

    @Override
    protected EncodedFrame encodePicture(Picture picture, ByteBuffer packetBuffer) {

        EncodedFrame encoded = encoder.encodeFrame(picture, packetBuffer);
        ByteBuffer data = encoded.getData();
        int start = data.position();

        data.put(start + EditorSetting.RECORDER_PRORES_PRIMARIES_BYTE, (byte) EditorSetting.RECORDER_COLOR_BT709);
        data.put(start + EditorSetting.RECORDER_PRORES_TRANSFER_BYTE, (byte) EditorSetting.RECORDER_COLOR_BT709);
        data.put(start + EditorSetting.RECORDER_PRORES_MATRIX_BYTE, (byte) EditorSetting.RECORDER_COLOR_BT709);

        return encoded;
    }
}
