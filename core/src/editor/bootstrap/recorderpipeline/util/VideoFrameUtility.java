package editor.bootstrap.recorderpipeline.util;

import java.nio.ByteBuffer;

import org.jcodec.common.model.Picture;

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
     * toYuv420() and toYuv422() turn such a frame into the studio-range
     * BT.709 planes of a picture an encoder takes, each chroma sample from the
     * average of the pixels it covers, and fill the picture's macroblock
     * padding past the frame's edges by repeating the last row and column.
     * Export sizes are even, so every chroma sample covers whole pixels.
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

    // YUV \\

    // Luma for every pixel, chroma for every square of two by two
    public static void toYuv420(byte[] frame, int width, int height, Picture picture) {

        byte[] luma = picture.getPlaneData(0);
        byte[] blue = picture.getPlaneData(1);
        byte[] red = picture.getPlaneData(2);
        int lumaStride = picture.getPlaneWidth(0);
        int chromaStride = picture.getPlaneWidth(1);
        int rowBytes = width * EditorSetting.RECORDER_FRAME_BYTES_PER_PIXEL;

        for (int y = 0; y < height; y += 2) {

            int topRow = y * rowBytes;
            int bottomRow = topRow + rowBytes;
            int topLuma = y * lumaStride;
            int bottomLuma = topLuma + lumaStride;
            int chromaRow = (y >> 1) * chromaStride;

            for (int x = 0; x < width; x += 2) {

                int topLeft = topRow + x * EditorSetting.RECORDER_FRAME_BYTES_PER_PIXEL;
                int topRight = topLeft + EditorSetting.RECORDER_FRAME_BYTES_PER_PIXEL;
                int bottomLeft = bottomRow + x * EditorSetting.RECORDER_FRAME_BYTES_PER_PIXEL;
                int bottomRight = bottomLeft + EditorSetting.RECORDER_FRAME_BYTES_PER_PIXEL;

                luma[topLuma + x] = toLuma(frame, topLeft);
                luma[topLuma + x + 1] = toLuma(frame, topRight);
                luma[bottomLuma + x] = toLuma(frame, bottomLeft);
                luma[bottomLuma + x + 1] = toLuma(frame, bottomRight);

                int b = toByte(frame[topLeft]) + toByte(frame[topRight])
                        + toByte(frame[bottomLeft]) + toByte(frame[bottomRight]);
                int g = toByte(frame[topLeft + 1]) + toByte(frame[topRight + 1])
                        + toByte(frame[bottomLeft + 1]) + toByte(frame[bottomRight + 1]);
                int r = toByte(frame[topLeft + 2]) + toByte(frame[topRight + 2])
                        + toByte(frame[bottomLeft + 2]) + toByte(frame[bottomRight + 2]);
                int chroma = chromaRow + (x >> 1);

                blue[chroma] = toChroma(EditorSetting.RECORDER_YUV_BLUE_RED * r
                        + EditorSetting.RECORDER_YUV_BLUE_GREEN * g + EditorSetting.RECORDER_YUV_BLUE_BLUE * b, 2);
                red[chroma] = toChroma(EditorSetting.RECORDER_YUV_RED_RED * r
                        + EditorSetting.RECORDER_YUV_RED_GREEN * g + EditorSetting.RECORDER_YUV_RED_BLUE * b, 2);
            }
        }

        padPlane(luma, width, height, lumaStride, picture.getPlaneHeight(0));
        padPlane(blue, width >> 1, height >> 1, chromaStride, picture.getPlaneHeight(1));
        padPlane(red, width >> 1, height >> 1, chromaStride, picture.getPlaneHeight(2));
    }

    // Luma for every pixel, chroma for every pair side by side
    public static void toYuv422(byte[] frame, int width, int height, Picture picture) {

        byte[] luma = picture.getPlaneData(0);
        byte[] blue = picture.getPlaneData(1);
        byte[] red = picture.getPlaneData(2);
        int lumaStride = picture.getPlaneWidth(0);
        int chromaStride = picture.getPlaneWidth(1);
        int rowBytes = width * EditorSetting.RECORDER_FRAME_BYTES_PER_PIXEL;

        for (int y = 0; y < height; y++) {

            int row = y * rowBytes;
            int lumaRow = y * lumaStride;
            int chromaRow = y * chromaStride;

            for (int x = 0; x < width; x += 2) {

                int left = row + x * EditorSetting.RECORDER_FRAME_BYTES_PER_PIXEL;
                int right = left + EditorSetting.RECORDER_FRAME_BYTES_PER_PIXEL;

                luma[lumaRow + x] = toLuma(frame, left);
                luma[lumaRow + x + 1] = toLuma(frame, right);

                int b = toByte(frame[left]) + toByte(frame[right]);
                int g = toByte(frame[left + 1]) + toByte(frame[right + 1]);
                int r = toByte(frame[left + 2]) + toByte(frame[right + 2]);
                int chroma = chromaRow + (x >> 1);

                blue[chroma] = toChroma(EditorSetting.RECORDER_YUV_BLUE_RED * r
                        + EditorSetting.RECORDER_YUV_BLUE_GREEN * g + EditorSetting.RECORDER_YUV_BLUE_BLUE * b, 1);
                red[chroma] = toChroma(EditorSetting.RECORDER_YUV_RED_RED * r
                        + EditorSetting.RECORDER_YUV_RED_GREEN * g + EditorSetting.RECORDER_YUV_RED_BLUE * b, 1);
            }
        }

        padPlane(luma, width, height, lumaStride, picture.getPlaneHeight(0));
        padPlane(blue, width >> 1, height, chromaStride, picture.getPlaneHeight(1));
        padPlane(red, width >> 1, height, chromaStride, picture.getPlaneHeight(2));
    }

    private static byte toLuma(byte[] frame, int pixel) {

        int weighted = EditorSetting.RECORDER_YUV_LUMA_BLUE * toByte(frame[pixel])
                + EditorSetting.RECORDER_YUV_LUMA_GREEN * toByte(frame[pixel + 1])
                + EditorSetting.RECORDER_YUV_LUMA_RED * toByte(frame[pixel + 2]);

        return (byte) (((weighted + EditorSetting.RECORDER_YUV_ROUNDING) >> EditorSetting.RECORDER_YUV_SHIFT)
                + EditorSetting.RECORDER_YUV_LUMA_BLACK - EditorSetting.RECORDER_YUV_PLANE_OFFSET);
    }

    // A chroma sample from weights summed over the 2 to the power given of pixels it covers, centred on zero
    private static byte toChroma(int weighted, int samplesShift) {

        int shift = EditorSetting.RECORDER_YUV_SHIFT + samplesShift;

        return (byte) ((weighted + (1 << (shift - 1))) >> shift);
    }

    private static int toByte(byte value) {
        return value & EditorSetting.RECORDER_BYTE_MASK;
    }

    // Repeats the last column across the padding of each row, then the last row down the padding below it
    private static void padPlane(byte[] plane, int width, int height, int stride, int planeHeight) {

        if (width < stride)
            for (int y = 0; y < height; y++) {

                int row = y * stride;
                byte edge = plane[row + width - 1];

                for (int x = width; x < stride; x++)
                    plane[row + x] = edge;
            }

        int lastRow = (height - 1) * stride;

        for (int y = height; y < planeHeight; y++)
            System.arraycopy(plane, lastRow, plane, y * stride, stride);
    }
}
