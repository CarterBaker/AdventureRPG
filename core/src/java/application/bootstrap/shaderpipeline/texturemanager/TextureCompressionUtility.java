package application.bootstrap.shaderpipeline.texturemanager;

import engine.root.EngineSetting;
import engine.root.EngineUtility;

public class TextureCompressionUtility extends EngineUtility {

    /*
     * Block-compresses one mip level for the GPU. Texels arrive as RGBA bytes,
     * rows bottom first as a texture layer holds them, and leave as 4x4 blocks
     * row by row exactly as glCompressedTexSubImage3D takes them; a level
     * smaller than a block repeats its edge texels. BC7 uses mode 6, one
     * subset of full RGBA endpoints with four-bit indices, fitted along the
     * block's principal axis and refined by least squares; BC5 stores the
     * first two channels as two BC4 blocks between their own extremes.
     */

    // Size \\

    public static int getLevelBytes(int width, int height) {
        return getBlocksAcross(width) * getBlocksAcross(height) * EngineSetting.TEXTURE_BLOCK_BYTES;
    }

    // The bytes of a full chain from a square level of the given size down to one texel
    public static int getChainBytes(int size) {

        int bytes = 0;

        for (int level = size; level >= 1; level >>= 1)
            bytes += getLevelBytes(level, level);

        return bytes;
    }

    private static int getBlocksAcross(int texels) {
        return Math.max(1, (texels + EngineSetting.TEXTURE_BLOCK_EDGE - 1) / EngineSetting.TEXTURE_BLOCK_EDGE);
    }

    // BC7 \\

    public static void encodeBC7(
            byte[] rgba,
            int width,
            int height,
            byte[] out,
            int offset,
            TextureCompressionScratchStruct scratch) {

        int across = getBlocksAcross(width);
        int down = getBlocksAcross(height);

        for (int blockY = 0; blockY < down; blockY++) {
            for (int blockX = 0; blockX < across; blockX++) {
                gatherBlock(rgba, width, height, blockX, blockY, scratch.texels);
                encodeBC7Block(scratch, out,
                        offset + (blockY * across + blockX) * EngineSetting.TEXTURE_BLOCK_BYTES);
            }
        }
    }

    private static void encodeBC7Block(TextureCompressionScratchStruct scratch, byte[] out, int offset) {

        resolvePrincipalAxis(scratch);
        resolveAxisEndpoints(scratch);

        long bestError = fitEndpoints(scratch);
        keepCandidate(scratch);

        for (int pass = 0; pass < EngineSetting.TEXTURE_BC7_REFINE_PASSES && bestError > 0; pass++) {

            if (!refineEndpoints(scratch))
                break;

            long error = fitEndpoints(scratch);

            if (error >= bestError)
                break;

            bestError = error;
            keepCandidate(scratch);
        }

        writeBC7Block(scratch, out, offset);
    }

    // The block's mean and the direction its texels spread along most, by power iteration on their covariance
    private static void resolvePrincipalAxis(TextureCompressionScratchStruct scratch) {

        int channels = EngineSetting.COLOR_CHANNEL_COUNT;
        int texelCount = EngineSetting.TEXTURE_BLOCK_TEXELS;
        int[] texels = scratch.texels;
        float[] mean = scratch.mean;
        float[] covariance = scratch.covariance;
        float[] axis = scratch.axis;
        float[] next = scratch.axisNext;

        for (int c = 0; c < channels; c++) {

            float sum = 0f;

            for (int i = 0; i < texelCount; i++)
                sum += texels[i * channels + c];

            mean[c] = sum / texelCount;
        }

        for (int row = 0; row < channels; row++) {
            for (int column = 0; column < channels; column++) {

                float sum = 0f;

                for (int i = 0; i < texelCount; i++)
                    sum += (texels[i * channels + row] - mean[row]) * (texels[i * channels + column] - mean[column]);

                covariance[row * channels + column] = sum / texelCount;
            }
        }

        int widest = 0;

        for (int c = 1; c < channels; c++)
            if (covariance[c * channels + c] > covariance[widest * channels + widest])
                widest = c;

        for (int c = 0; c < channels; c++)
            axis[c] = covariance[widest * channels + c];

        for (int iteration = 0; iteration < EngineSetting.TEXTURE_BC7_PCA_ITERATIONS; iteration++) {

            float largest = 0f;

            for (int row = 0; row < channels; row++) {

                float sum = 0f;

                for (int column = 0; column < channels; column++)
                    sum += covariance[row * channels + column] * axis[column];

                next[row] = sum;
                largest = Math.max(largest, Math.abs(sum));
            }

            if (largest <= EngineSetting.TEXTURE_BC7_FLAT_VARIANCE)
                break;

            for (int c = 0; c < channels; c++)
                axis[c] = next[c] / largest;
        }

        float length = 0f;

        for (int c = 0; c < channels; c++)
            length += axis[c] * axis[c];

        length = (float) Math.sqrt(length);

        for (int c = 0; c < channels; c++)
            axis[c] = length > EngineSetting.TEXTURE_BC7_FLAT_VARIANCE ? axis[c] / length : 0f;
    }

    // The endpoints at the extremes of the texels' projections onto the principal axis
    private static void resolveAxisEndpoints(TextureCompressionScratchStruct scratch) {

        int channels = EngineSetting.COLOR_CHANNEL_COUNT;
        int[] texels = scratch.texels;
        float[] mean = scratch.mean;
        float[] axis = scratch.axis;
        float lowest = 0f;
        float highest = 0f;

        for (int i = 0; i < EngineSetting.TEXTURE_BLOCK_TEXELS; i++) {

            float projection = 0f;

            for (int c = 0; c < channels; c++)
                projection += (texels[i * channels + c] - mean[c]) * axis[c];

            lowest = Math.min(lowest, projection);
            highest = Math.max(highest, projection);
        }

        for (int c = 0; c < channels; c++) {
            scratch.lowEndpoint[c] = clampByte(mean[c] + axis[c] * lowest);
            scratch.highEndpoint[c] = clampByte(mean[c] + axis[c] * highest);
        }
    }

    // Quantizes both endpoints and picks every texel's nearest palette entry, returning the block's squared error
    private static long fitEndpoints(TextureCompressionScratchStruct scratch) {

        int channels = EngineSetting.COLOR_CHANNEL_COUNT;
        scratch.lowParity = quantizeEndpoint(scratch.lowEndpoint, scratch.lowQuantized);
        scratch.highParity = quantizeEndpoint(scratch.highEndpoint, scratch.highQuantized);

        int[] palette = scratch.palette;

        for (int entry = 0; entry < EngineSetting.TEXTURE_BC7_INDEX_COUNT; entry++) {

            int weight = EngineSetting.TEXTURE_BC7_WEIGHTS[entry];

            for (int c = 0; c < channels; c++) {
                int low = expandEndpoint(scratch.lowQuantized[c], scratch.lowParity);
                int high = expandEndpoint(scratch.highQuantized[c], scratch.highParity);
                palette[entry * channels + c] = ((EngineSetting.TEXTURE_BC7_WEIGHT_TOTAL - weight) * low
                        + weight * high + EngineSetting.TEXTURE_BC7_WEIGHT_ROUNDING)
                        >> EngineSetting.TEXTURE_BC7_WEIGHT_SHIFT;
            }
        }

        int[] texels = scratch.texels;
        long error = 0L;

        for (int i = 0; i < EngineSetting.TEXTURE_BLOCK_TEXELS; i++) {

            int bestEntry = 0;
            int bestDistance = Integer.MAX_VALUE;

            for (int entry = 0; entry < EngineSetting.TEXTURE_BC7_INDEX_COUNT; entry++) {

                int distance = 0;

                for (int c = 0; c < channels; c++) {
                    int difference = texels[i * channels + c] - palette[entry * channels + c];
                    distance += difference * difference;
                }

                if (distance < bestDistance) {
                    bestDistance = distance;
                    bestEntry = entry;
                }
            }

            scratch.indices[i] = bestEntry;
            error += bestDistance;
        }

        return error;
    }

    // The seven-bit value per channel and the shared parity bit that land closest to the endpoint
    private static int quantizeEndpoint(float[] endpoint, int[] quantized) {

        int channels = EngineSetting.COLOR_CHANNEL_COUNT;
        int bestParity = 0;
        float bestError = Float.MAX_VALUE;

        for (int parity = 0; parity <= 1; parity++) {

            float error = 0f;

            for (int c = 0; c < channels; c++) {
                int value = quantizeChannel(endpoint[c], parity);
                float difference = expandEndpoint(value, parity) - endpoint[c];
                error += difference * difference;
            }

            if (error < bestError) {
                bestError = error;
                bestParity = parity;
            }
        }

        for (int c = 0; c < channels; c++)
            quantized[c] = quantizeChannel(endpoint[c], bestParity);

        return bestParity;
    }

    private static int quantizeChannel(float value, int parity) {
        return Math.max(0, Math.min(EngineSetting.TEXTURE_BC7_ENDPOINT_MAX, Math.round((value - parity) * 0.5f)));
    }

    private static int expandEndpoint(int quantized, int parity) {
        return (quantized << 1) | parity;
    }

    // Least-squares endpoints for the indices just chosen, false where the indices leave them undetermined
    private static boolean refineEndpoints(TextureCompressionScratchStruct scratch) {

        int channels = EngineSetting.COLOR_CHANNEL_COUNT;
        int[] texels = scratch.texels;
        float lowSquares = 0f;
        float cross = 0f;
        float highSquares = 0f;

        for (int c = 0; c < channels; c++) {
            scratch.lowEndpoint[c] = 0f;
            scratch.highEndpoint[c] = 0f;
        }

        for (int i = 0; i < EngineSetting.TEXTURE_BLOCK_TEXELS; i++) {

            float weight = EngineSetting.TEXTURE_BC7_WEIGHTS[scratch.indices[i]]
                    / (float) EngineSetting.TEXTURE_BC7_WEIGHT_TOTAL;
            float inverse = 1f - weight;

            lowSquares += inverse * inverse;
            cross += inverse * weight;
            highSquares += weight * weight;

            for (int c = 0; c < channels; c++) {
                scratch.lowEndpoint[c] += inverse * texels[i * channels + c];
                scratch.highEndpoint[c] += weight * texels[i * channels + c];
            }
        }

        float determinant = lowSquares * highSquares - cross * cross;

        if (Math.abs(determinant) <= EngineSetting.TEXTURE_BC7_FLAT_VARIANCE)
            return false;

        for (int c = 0; c < channels; c++) {

            float lowSum = scratch.lowEndpoint[c];
            float highSum = scratch.highEndpoint[c];

            scratch.lowEndpoint[c] = clampByte((highSquares * lowSum - cross * highSum) / determinant);
            scratch.highEndpoint[c] = clampByte((lowSquares * highSum - cross * lowSum) / determinant);
        }

        return true;
    }

    private static void keepCandidate(TextureCompressionScratchStruct scratch) {

        System.arraycopy(scratch.lowQuantized, 0, scratch.bestLowQuantized, 0, EngineSetting.COLOR_CHANNEL_COUNT);
        System.arraycopy(scratch.highQuantized, 0, scratch.bestHighQuantized, 0, EngineSetting.COLOR_CHANNEL_COUNT);
        System.arraycopy(scratch.indices, 0, scratch.bestIndices, 0, EngineSetting.TEXTURE_BLOCK_TEXELS);
        scratch.bestLowParity = scratch.lowParity;
        scratch.bestHighParity = scratch.highParity;
    }

    // Mode 6 bits: the header, both endpoints channel by channel, their parity bits, then the indices
    private static void writeBC7Block(TextureCompressionScratchStruct scratch, byte[] out, int offset) {

        int[] low = scratch.bestLowQuantized;
        int[] high = scratch.bestHighQuantized;
        int lowParity = scratch.bestLowParity;
        int highParity = scratch.bestHighParity;
        int[] indices = scratch.bestIndices;
        int highestIndex = EngineSetting.TEXTURE_BC7_INDEX_COUNT - 1;

        if (indices[0] >= EngineSetting.TEXTURE_BC7_ANCHOR_INDEX_LIMIT) {

            int[] swapped = low;
            low = high;
            high = swapped;

            int parity = lowParity;
            lowParity = highParity;
            highParity = parity;

            for (int i = 0; i < EngineSetting.TEXTURE_BLOCK_TEXELS; i++)
                indices[i] = highestIndex - indices[i];
        }

        scratch.bits[0] = 0L;
        scratch.bits[1] = 0L;
        scratch.bitPosition = 0;

        writeBits(scratch, EngineSetting.TEXTURE_BC7_MODE6_HEADER, EngineSetting.TEXTURE_BC7_MODE6_HEADER_BITS);

        for (int c = 0; c < EngineSetting.COLOR_CHANNEL_COUNT; c++) {
            writeBits(scratch, low[c], EngineSetting.TEXTURE_BC7_ENDPOINT_BITS);
            writeBits(scratch, high[c], EngineSetting.TEXTURE_BC7_ENDPOINT_BITS);
        }

        writeBits(scratch, lowParity, 1);
        writeBits(scratch, highParity, 1);
        writeBits(scratch, indices[0], EngineSetting.TEXTURE_BC7_ANCHOR_INDEX_BITS);

        for (int i = 1; i < EngineSetting.TEXTURE_BLOCK_TEXELS; i++)
            writeBits(scratch, indices[i], EngineSetting.TEXTURE_BC7_INDEX_BITS);

        writeLong(scratch.bits[0], out, offset);
        writeLong(scratch.bits[1], out, offset + Long.BYTES);
    }

    // BC5 \\

    public static void encodeBC5(
            byte[] rgba,
            int width,
            int height,
            byte[] out,
            int offset,
            TextureCompressionScratchStruct scratch) {

        int across = getBlocksAcross(width);
        int down = getBlocksAcross(height);

        for (int blockY = 0; blockY < down; blockY++) {
            for (int blockX = 0; blockX < across; blockX++) {

                gatherBlock(rgba, width, height, blockX, blockY, scratch.texels);
                int blockOffset = offset + (blockY * across + blockX) * EngineSetting.TEXTURE_BLOCK_BYTES;

                for (int c = 0; c < EngineSetting.TEXTURE_BC5_CHANNELS; c++)
                    encodeBC4Block(scratch, c, out, blockOffset + c * EngineSetting.TEXTURE_BC4_BYTES);
            }
        }
    }

    // One channel between its own extremes, eight steps apart, the larger endpoint first so all eight are used
    private static void encodeBC4Block(TextureCompressionScratchStruct scratch, int channel, byte[] out, int offset) {

        int channels = EngineSetting.COLOR_CHANNEL_COUNT;
        int steps = EngineSetting.TEXTURE_BC4_INTERPOLATION_STEPS;
        int[] texels = scratch.texels;
        int high = 0;
        int low = EngineSetting.PACKED_COLOR_CHANNEL_MASK;

        for (int i = 0; i < EngineSetting.TEXTURE_BLOCK_TEXELS; i++) {
            high = Math.max(high, texels[i * channels + channel]);
            low = Math.min(low, texels[i * channels + channel]);
        }

        int[] palette = scratch.palette;
        palette[0] = high;
        palette[1] = low;

        for (int entry = 2; entry < EngineSetting.TEXTURE_BC4_PALETTE_SIZE; entry++)
            palette[entry] = Math.round(((steps + 1 - entry) * high + (entry - 1) * low) / (float) steps);

        long bits = (long) high | ((long) low << Byte.SIZE);
        int shift = Byte.SIZE * 2;

        for (int i = 0; i < EngineSetting.TEXTURE_BLOCK_TEXELS; i++) {

            int value = texels[i * channels + channel];
            int bestEntry = 0;
            int bestDistance = Integer.MAX_VALUE;

            for (int entry = 0; high > low && entry < EngineSetting.TEXTURE_BC4_PALETTE_SIZE; entry++) {

                int distance = Math.abs(value - palette[entry]);

                if (distance < bestDistance) {
                    bestDistance = distance;
                    bestEntry = entry;
                }
            }

            bits |= (long) bestEntry << shift;
            shift += EngineSetting.TEXTURE_BC4_INDEX_BITS;
        }

        writeLong(bits, out, offset);
    }

    // Utility \\

    // A block's sixteen texels, repeating the level's edge where the block runs past it
    private static void gatherBlock(byte[] rgba, int width, int height, int blockX, int blockY, int[] texels) {

        int channels = EngineSetting.COLOR_CHANNEL_COUNT;
        int edge = EngineSetting.TEXTURE_BLOCK_EDGE;

        for (int row = 0; row < edge; row++) {

            int y = Math.min(blockY * edge + row, height - 1);

            for (int column = 0; column < edge; column++) {

                int x = Math.min(blockX * edge + column, width - 1);
                int source = (y * width + x) * channels;
                int target = (row * edge + column) * channels;

                for (int c = 0; c < channels; c++)
                    texels[target + c] = rgba[source + c] & EngineSetting.PACKED_COLOR_CHANNEL_MASK;
            }
        }
    }

    private static void writeBits(TextureCompressionScratchStruct scratch, int value, int count) {

        int position = scratch.bitPosition;

        if (position < Long.SIZE) {

            scratch.bits[0] |= (long) value << position;

            if (position + count > Long.SIZE)
                scratch.bits[1] |= (long) value >>> (Long.SIZE - position);
        } else {
            scratch.bits[1] |= (long) value << (position - Long.SIZE);
        }

        scratch.bitPosition = position + count;
    }

    private static void writeLong(long value, byte[] out, int offset) {
        for (int i = 0; i < Long.BYTES; i++)
            out[offset + i] = (byte) (value >>> (i * Byte.SIZE));
    }

    private static float clampByte(float value) {
        return Math.max(0f, Math.min(EngineSetting.PACKED_COLOR_CHANNEL_MASK, value));
    }
}
