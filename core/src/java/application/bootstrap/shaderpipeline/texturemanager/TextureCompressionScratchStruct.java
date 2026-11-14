package application.bootstrap.shaderpipeline.texturemanager;

import engine.root.EngineSetting;
import engine.root.StructPackage;

public class TextureCompressionScratchStruct extends StructPackage {

    /*
     * The working storage one caller compresses blocks with: the block's
     * texels, its statistics, both candidate endpoint pairs and their indices,
     * and the bits a block is packed into. Each thread that compresses keeps
     * its own, so encoding allocates nothing per block.
     */

    // Block
    final int[] texels;
    final float[] mean;
    final float[] covariance;
    final float[] axis;
    final float[] axisNext;

    // Endpoints
    final float[] lowEndpoint;
    final float[] highEndpoint;
    final int[] lowQuantized;
    final int[] highQuantized;
    final int[] bestLowQuantized;
    final int[] bestHighQuantized;
    final int[] palette;
    int lowParity;
    int highParity;
    int bestLowParity;
    int bestHighParity;

    // Indices
    final int[] indices;
    final int[] bestIndices;

    // Bits
    final long[] bits;
    int bitPosition;

    // Constructor \\

    public TextureCompressionScratchStruct() {

        int channels = EngineSetting.COLOR_CHANNEL_COUNT;

        // Block
        this.texels = new int[EngineSetting.TEXTURE_BLOCK_TEXELS * channels];
        this.mean = new float[channels];
        this.covariance = new float[channels * channels];
        this.axis = new float[channels];
        this.axisNext = new float[channels];

        // Endpoints
        this.lowEndpoint = new float[channels];
        this.highEndpoint = new float[channels];
        this.lowQuantized = new int[channels];
        this.highQuantized = new int[channels];
        this.bestLowQuantized = new int[channels];
        this.bestHighQuantized = new int[channels];
        this.palette = new int[EngineSetting.TEXTURE_BC7_INDEX_COUNT * channels];

        // Indices
        this.indices = new int[EngineSetting.TEXTURE_BLOCK_TEXELS];
        this.bestIndices = new int[EngineSetting.TEXTURE_BLOCK_TEXELS];

        // Bits
        this.bits = new long[2];
    }
}
