package editor.bootstrap.recorderpipeline.util;

import java.nio.ByteBuffer;

import editor.runtime.EditorSetting;
import engine.root.EngineSetting;
import engine.root.EngineUtility;

public class VideoFrameUtility extends EngineUtility {

    /*
     * Turns frames read back from the GPU into frames a video takes. A read
     * back is four bytes a pixel, blue first and rows bottom up, at the render
     * size; accumulate() adds it into a running sum at the output size, every
     * output pixel taking the square of render pixels it covers, and resolve()
     * divides the sum by how many pixels went into each and writes three bytes
     * a pixel, blue first and rows top down, clearing the sum as it goes. So
     * supersampling and motion blur are one average, and neither allocates.
     */

    public static void accumulate(
            ByteBuffer source,
            int renderWidth,
            int renderHeight,
            int scale,
            int[] sum,
            int outputWidth,
            int outputHeight) {

        int sourceStride = renderWidth * EngineSetting.BYTES_PER_PIXEL_BGRA;
        int sampleStride = scale * EngineSetting.BYTES_PER_PIXEL_BGRA;

        for (int y = 0; y < outputHeight; y++) {

            int outputRow = y * outputWidth * EditorSetting.RECORDER_FRAME_BYTES_PER_PIXEL;

            for (int sampleY = 0; sampleY < scale; sampleY++) {

                int sourceRow = (renderHeight - 1 - (y * scale + sampleY)) * sourceStride;

                for (int x = 0; x < outputWidth; x++) {

                    int target = outputRow + x * EditorSetting.RECORDER_FRAME_BYTES_PER_PIXEL;
                    int sourcePixel = sourceRow + x * sampleStride;

                    for (int sampleX = 0; sampleX < scale; sampleX++) {

                        int pixel = sourcePixel + sampleX * EngineSetting.BYTES_PER_PIXEL_BGRA;

                        sum[target] += source.get(pixel) & EditorSetting.RECORDER_BYTE_MASK;
                        sum[target + 1] += source.get(pixel + 1) & EditorSetting.RECORDER_BYTE_MASK;
                        sum[target + 2] += source.get(pixel + 2) & EditorSetting.RECORDER_BYTE_MASK;
                    }
                }
            }
        }
    }

    public static void resolve(int[] sum, int divisor, byte[] frame) {

        int rounding = divisor / 2;

        for (int i = 0; i < frame.length; i++) {
            frame[i] = (byte) ((sum[i] + rounding) / divisor);
            sum[i] = 0;
        }
    }
}
