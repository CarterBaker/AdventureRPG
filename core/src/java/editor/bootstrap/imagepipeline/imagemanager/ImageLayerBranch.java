package editor.bootstrap.imagepipeline.imagemanager;

import editor.bootstrap.imagepipeline.imagelayer.ImageLayerStackStruct;
import editor.bootstrap.imagepipeline.util.ImageColorUtility;
import engine.assets.image.Pixmap;
import engine.root.BranchPackage;
import engine.root.EngineSetting;

class ImageLayerBranch extends BranchPackage {

    /*
     * Lays a layered image flat, a rectangle at a time, the one way every
     * editor's layers combine. Where the base keeps its own alpha the base
     * is taken as it is and each layer shown above it colours it by its
     * alpha and opacity; otherwise the image starts clear and every layer
     * shown, the base included, is laid over what is below it by its alpha
     * and opacity.
     */

    // Composite \\

    void composite(ImageLayerStackStruct stack, int minX, int minY, int maxX, int maxY) {

        Pixmap composite = stack.getComposite();
        boolean baseAlphaKept = stack.isBaseAlphaKept();
        int first = baseAlphaKept ? 1 : 0;

        for (int y = minY; y <= maxY; y++)
            for (int x = minX; x <= maxX; x++) {

                int flat = baseAlphaKept ? stack.getLayer(0).getPixmap().getPixel(x, y) : 0;

                for (int l = first; l < stack.getLayerCount(); l++) {

                    if (!stack.isLayerVisible(l))
                        continue;

                    int over = stack.getLayer(l).getPixmap().getPixel(x, y);

                    flat = baseAlphaKept
                            ? ImageColorUtility.mix(
                                    flat,
                                    over | EngineSetting.PACKED_COLOR_CHANNEL_MASK,
                                    (over & EngineSetting.PACKED_COLOR_CHANNEL_MASK)
                                            / EngineSetting.COLOR_CHANNEL_BYTE_MAX * stack.getOpacity(l),
                                    false)
                            : ImageColorUtility.over(flat, over, stack.getOpacity(l));
                }

                composite.drawPixel(x, y, flat);
            }
    }
}
