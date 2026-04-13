package application.bootstrap.shaderpipeline.sprite;

import application.bootstrap.geometrypipeline.model.ModelInstance;
import application.bootstrap.shaderpipeline.ubo.UBOInstance;
import engine.root.InstancePackage;

public class SpriteInstance extends InstancePackage {

    /*
     * Live sprite handed to external systems via SpriteManager.cloneSprite().
     * Wraps a SpriteData built with its own ModelInstance and UBOInstance.
     * Shares the GPU texture handle with the source SpriteHandle — never
     * owns or disposes it.
     */

    // Internal
    private SpriteData spriteData;

    // Internal \\

    public void constructor(SpriteData spriteData) {
        this.spriteData = spriteData;
    }

    // Accessible \\

    public SpriteData getSpriteData() {
        return spriteData;
    }

    public String getName() {
        return spriteData.getName();
    }

    public int getGpuHandle() {
        return spriteData.getGpuHandle();
    }

    public int getWidth() {
        return spriteData.getWidth();
    }

    public int getHeight() {
        return spriteData.getHeight();
    }

    public ModelInstance getModelInstance() {
        return spriteData.getModelInstance();
    }

    public UBOInstance getSliceData() {
        return spriteData.getSliceData();
    }
}