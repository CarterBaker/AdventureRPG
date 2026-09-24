package application.bootstrap.shaderpipeline.sprite;

import application.bootstrap.geometrypipeline.model.ModelInstance;
import application.bootstrap.shaderpipeline.ubo.UBOInstance;
import engine.root.EngineSetting;
import engine.root.InstancePackage;
import engine.util.mathematics.vectors.Vector4;

public class SpriteInstance extends InstancePackage {

    /*
     * Live sprite handed to external systems via SpriteManager.cloneSprite().
     * Wraps a SpriteData built with its own ModelInstance and UBOInstance.
     * Shares the GPU texture handle with the source SpriteHandle — never
     * owns or disposes it. setColor() tints the sprite through its own
     * material, so white artwork takes on whatever color it is given.
     */

    // Internal
    private SpriteData spriteData;

    // Internal \\

    public void constructor(SpriteData spriteData) {
        this.spriteData = spriteData;
    }

    // Color \\

    public void setColor(Vector4 color) {
        spriteData.getModelInstance().getMaterial().setUniform(EngineSetting.SPRITE_COLOR_UNIFORM, color);
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