package editor.bootstrap.imagepipeline.imagemanager;

import java.nio.ByteBuffer;

import application.bootstrap.shaderpipeline.texturemanager.TextureManager;
import editor.bootstrap.imagepipeline.imagedocument.ImageDocumentInstance;
import editor.bootstrap.imagepipeline.imageregion.ImageRegionStruct;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.memory.BufferUtility;

class ImageTextureBranch extends BranchPackage {

    /*
     * Main thread — keeps each image's GPU texture current. A texture is made
     * the first time a view asks for one, filled whole, and from then on only
     * the rows a frame's changes span are uploaded, once per frame however
     * many views draw it. Texels are nearest-filtered, so pixels stay crisp
     * at any zoom.
     */

    // Internal
    private TextureManager textureManager;

    // Base \\

    @Override
    protected void get() {
        this.textureManager = get(TextureManager.class);
    }

    // Upload \\

    void upload(ImageDocumentInstance document) {

        if (!document.isTextureRequested())
            return;

        ImageRegionStruct region = document.getUploadRegion();

        if (document.getTexture() == 0) {

            document.setTexture(textureManager.createTexture2D(
                    document.getWidth(),
                    document.getHeight(),
                    EngineSetting.GL_CLAMP_TO_EDGE,
                    EngineSetting.GL_NEAREST));

            region.include(0, 0);
            region.include(document.getWidth() - 1, document.getHeight() - 1);
        }

        if (region.isEmpty())
            return;

        // Whole rows, so the region is one contiguous run of the image's pixels
        int rowBytes = document.getWidth() * EngineSetting.COLOR_CHANNEL_COUNT;
        int offset = region.getMinY() * rowBytes;
        int length = region.getHeight() * rowBytes;
        ByteBuffer pixels = document.getPixmap().getPixels().duplicate();
        ByteBuffer rows = BufferUtility.newByteBuffer(length);

        pixels.clear();
        pixels.position(offset).limit(offset + length);
        rows.put(pixels);

        textureManager.updateTexture2D(
                document.getTexture(), 0, region.getMinY(), document.getWidth(), region.getHeight(), rows);
        region.clear();
    }

    // Dispose \\

    void delete(ImageDocumentInstance document) {

        if (document.getTexture() != 0)
            textureManager.deleteTexture2D(document.getTexture());
    }
}
