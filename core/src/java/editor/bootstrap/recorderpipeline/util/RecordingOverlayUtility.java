package editor.bootstrap.recorderpipeline.util;

import java.util.Arrays;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import engine.root.EngineUtility;

public class RecordingOverlayUtility extends EngineUtility {

    /*
     * Packs the interface's frames and reads them back. A frame is a window
     * overlay's BGRA pixels; a keyframe is compressed whole, any other frame
     * as the bytes it differs by from the frame before, which are zero
     * wherever nothing moved, so a menu sitting still costs almost nothing.
     * Every buffer is the caller's and reused frame to frame; only the packed
     * frame handed back is new, and it is exactly as long as it needs to be.
     */

    // Encode \\

    // The frame packed whole, or as its difference from the previous frame when that is given
    public static byte[] encode(
            byte[] pixels,
            int length,
            byte[] previous,
            byte[] difference,
            byte[] packed,
            Deflater deflater) {

        byte[] source = pixels;

        if (previous != null) {

            for (int i = 0; i < length; i++)
                difference[i] = (byte) (pixels[i] ^ previous[i]);

            source = difference;
        }

        deflater.reset();
        deflater.setInput(source, 0, length);
        deflater.finish();

        int packedLength = 0;

        while (!deflater.finished() && packedLength < packed.length)
            packedLength += deflater.deflate(packed, packedLength, packed.length - packedLength);

        return Arrays.copyOf(packed, packedLength);
    }

    // Room enough for a frame packed even when it does not compress
    public static int resolvePackedCapacity(int length) {
        return length + length / 16 + 64;
    }

    // Decode \\

    // The frame unpacked into the pixels — whole for a keyframe, applied over the frame they already hold for any
    // other — false when the packed bytes are not a frame of this size
    public static boolean decode(
            byte[] packed,
            boolean keyframe,
            byte[] pixels,
            byte[] difference,
            int length,
            Inflater inflater) {

        byte[] target = keyframe ? pixels : difference;

        inflater.reset();
        inflater.setInput(packed);

        try {

            int unpacked = 0;

            while (!inflater.finished() && unpacked < length) {

                int read = inflater.inflate(target, unpacked, length - unpacked);

                if (read == 0 && (inflater.needsInput() || inflater.needsDictionary()))
                    return false;

                unpacked += read;
            }

            if (unpacked != length)
                return false;

        } catch (DataFormatException e) {
            return false;
        }

        if (!keyframe)
            for (int i = 0; i < length; i++)
                pixels[i] ^= difference[i];

        return true;
    }
}
