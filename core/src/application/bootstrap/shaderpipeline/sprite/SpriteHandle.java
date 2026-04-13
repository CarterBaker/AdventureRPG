package application.bootstrap.shaderpipeline.sprite;

import application.bootstrap.geometrypipeline.model.ModelInstance;
import application.core.engine.HandlePackage;

public class SpriteHandle extends HandlePackage {

    /*
     * Persistent GPU sprite resource owned exclusively by SpriteManager.
     * Wraps SpriteData holding the canonical template. External systems
     * receive a SpriteInstance via SpriteManager.cloneSprite(). GPU texture
     * handle is shared and never disposed by the instance.
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

    public float getBorderLeft() {
        return spriteData.getBorderLeft();
    }

    public float getBorderBottom() {
        return spriteData.getBorderBottom();
    }

    public float getBorderRight() {
        return spriteData.getBorderRight();
    }

    public float getBorderTop() {
        return spriteData.getBorderTop();
    }

    public boolean hasSlice() {
        return spriteData.hasSlice();
    }

    public ModelInstance getModelInstance() {
        return spriteData.getModelInstance();
    }
}