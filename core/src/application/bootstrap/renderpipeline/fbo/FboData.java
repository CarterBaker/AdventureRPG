package application.bootstrap.renderpipeline.fbo;

import engine.graphics.color.Color;
import engine.root.DataPackage;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class FboData extends DataPackage {

    /*
     * Immutable FBO descriptor loaded from JSON during bootstrap. Holds an
     * ordered list of attachment descriptors used by InternalBuilder to allocate
     * GL textures and configure draw buffer targets. colorName2Index maps
     * attachment names to their color texture list index for semantic lookup.
     * Width and height are only meaningful when strategy is FIXED.
     *
     * premultipliedBlend marks targets whose content is written in
     * premultiplied-alpha form (volumetric/raymarched passes). RenderSystem
     * uses GL_ONE instead of GL_SRC_ALPHA as the source blend factor for
     * these, so a transparent-cleared target receives an unmodified copy of
     * what the shader wrote rather than an extra, incorrect alpha multiply.
     *
     * clearColor is what RenderSystem clears the target to each frame —
     * transparent unless the JSON declares one, so a target that should
     * never show what lies beneath it (an editor viewport) can be opaque.
     *
     * premultipliedBlit marks targets whose premultiplied content must be
     * restored to straight alpha when FboRenderSystem blits them to their
     * window, so translucent pixels keep their full coverage on screen.
     *
     * resolutionScale sizes a WINDOW_RELATIVE target as a fraction of its
     * window, for passes soft enough to render below screen resolution — the
     * blit samples it linearly back up to the window.
     */

    // Identity
    private final String name;
    private final ObjectArrayList<AttachmentStruct> attachments;
    private final Object2IntOpenHashMap<String> colorName2Index;
    private final FboSizingStrategy sizingStrategy;
    private final boolean premultipliedBlend;
    private final boolean premultipliedBlit;
    private final Color clearColor;
    private final float resolutionScale;

    // Dimensions
    private final int width;
    private final int height;

    // Constructor \\

    public FboData(
            String name,
            ObjectArrayList<AttachmentStruct> attachments,
            FboSizingStrategy sizingStrategy,
            int width,
            int height,
            boolean premultipliedBlend,
            boolean premultipliedBlit,
            Color clearColor,
            float resolutionScale) {

        this.name = name;
        this.attachments = attachments;
        this.sizingStrategy = sizingStrategy;
        this.width = width;
        this.height = height;
        this.premultipliedBlend = premultipliedBlend;
        this.premultipliedBlit = premultipliedBlit;
        this.clearColor = clearColor;
        this.resolutionScale = resolutionScale;
        this.colorName2Index = new Object2IntOpenHashMap<>();
        this.colorName2Index.defaultReturnValue(-1);

        int colorIndex = 0;

        for (int i = 0; i < attachments.size(); i++) {
            AttachmentStruct attachment = attachments.get(i);

            if (!attachment.isDepth() && !attachment.getName().isEmpty()) {
                colorName2Index.put(attachment.getName(), colorIndex);
                colorIndex++;
            } else if (!attachment.isDepth()) {
                colorIndex++;
            }
        }
    }

    // Accessible \\

    public String getName() {
        return name;
    }

    public ObjectArrayList<AttachmentStruct> getAttachments() {
        return attachments;
    }

    public int getColorIndex(String attachmentName) {
        return colorName2Index.getInt(attachmentName);
    }

    public FboSizingStrategy getSizingStrategy() {
        return sizingStrategy;
    }

    public boolean isPremultipliedBlend() {
        return premultipliedBlend;
    }

    public boolean isPremultipliedBlit() {
        return premultipliedBlit;
    }

    public Color getClearColor() {
        return clearColor;
    }

    public float getResolutionScale() {
        return resolutionScale;
    }

    public int scaleWindowDimension(int windowDimension) {
        return Math.max(1, Math.round(windowDimension * resolutionScale));
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}