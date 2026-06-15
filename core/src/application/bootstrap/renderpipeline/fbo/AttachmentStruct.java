package application.bootstrap.renderpipeline.fbo;

import engine.root.StructPackage;

public class AttachmentStruct extends StructPackage {

    /*
     * Describes a single FBO attachment — color or depth — parsed from JSON
     * during bootstrap. Passed to InternalBuilder to drive GL texture allocation
     * and draw buffer list construction for multi-render-target framebuffers.
     */

    // Identity
    private final String name;
    private final boolean depth;
    private final int internalFormat;

    // Constructor \\

    public AttachmentStruct(String name, boolean depth, int internalFormat) {
        this.name = name;
        this.depth = depth;
        this.internalFormat = internalFormat;
    }

    // Accessible \\

    public String getName() {
        return name;
    }

    public boolean isDepth() {
        return depth;
    }

    public int getInternalFormat() {
        return internalFormat;
    }
}