package application.bootstrap.renderpipeline.fbo;

import engine.graphics.color.Color;
import engine.root.DataPackage;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class FBOData extends DataPackage {

    /*
     * Immutable FBO descriptor loaded from JSON. Holds the ordered attachments,
     * sizing strategy, clear color, and the blend and blit flags for
     * premultiplied, jitter-resolved and reduced-resolution targets.
     */

    // Identity
    private final String name;
    private final ObjectArrayList<AttachmentStruct> attachments;
    private final Object2IntOpenHashMap<String> colorName2Index;
    private final FBOSizingStrategy sizingStrategy;
    private final boolean premultipliedBlend;
    private final boolean premultipliedBlit;
    private final boolean resolveBlit;
    private final Color clearColor;
    private final float resolutionScale;

    // Dimensions
    private final int width;
    private final int height;

    // Constructor \\

    public FBOData(
            String name,
            ObjectArrayList<AttachmentStruct> attachments,
            FBOSizingStrategy sizingStrategy,
            int width,
            int height,
            boolean premultipliedBlend,
            boolean premultipliedBlit,
            boolean resolveBlit,
            Color clearColor,
            float resolutionScale) {

        this.name = name;
        this.attachments = attachments;
        this.sizingStrategy = sizingStrategy;
        this.width = width;
        this.height = height;
        this.premultipliedBlend = premultipliedBlend;
        this.premultipliedBlit = premultipliedBlit;
        this.resolveBlit = resolveBlit;
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

    public FBOSizingStrategy getSizingStrategy() {
        return sizingStrategy;
    }

    public boolean isPremultipliedBlend() {
        return premultipliedBlend;
    }

    public boolean isPremultipliedBlit() {
        return premultipliedBlit;
    }

    public boolean isResolveBlit() {
        return resolveBlit;
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