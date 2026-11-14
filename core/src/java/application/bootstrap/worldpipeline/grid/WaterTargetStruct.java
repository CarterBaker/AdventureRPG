package application.bootstrap.worldpipeline.grid;

import application.bootstrap.renderpipeline.fbo.FBOInstance;
import engine.root.StructPackage;

public class WaterTargetStruct extends StructPackage {

    /*
     * Where one grid's water draws and what it reads. Water is drawn forward,
     * after deferred lighting, into its own target; it refracts the lit scene,
     * tests itself against the scene depth by hand, reflects the sky and
     * clouds as the window already drew them, and lays the clouds standing in
     * front of it over itself by the cloud target's distances. Bound onto the
     * grid by the runtime WaterSystem once every target exists.
     */

    // Target
    private final FBOInstance waterFbo;

    // Inputs
    private final FBOInstance sceneFbo;
    private final FBOInstance litFbo;
    private final FBOInstance skyFbo;
    private final FBOInstance cloudFbo;
    private final String colorAttachment;
    private final String distanceAttachment;

    // Constructor \\

    public WaterTargetStruct(
            FBOInstance waterFbo,
            FBOInstance sceneFbo,
            FBOInstance litFbo,
            FBOInstance skyFbo,
            FBOInstance cloudFbo,
            String colorAttachment,
            String distanceAttachment) {

        this.waterFbo = waterFbo;
        this.sceneFbo = sceneFbo;
        this.litFbo = litFbo;
        this.skyFbo = skyFbo;
        this.cloudFbo = cloudFbo;
        this.colorAttachment = colorAttachment;
        this.distanceAttachment = distanceAttachment;
    }

    // Accessible \\

    public FBOInstance getWaterFbo() {
        return waterFbo;
    }

    public int getSceneColorTexture() {
        return litFbo.getColorTexture(colorAttachment);
    }

    public int getSceneDepthTexture() {
        return sceneFbo.getDepthTexture();
    }

    public int getSkyColorTexture() {
        return skyFbo.getColorTexture(colorAttachment);
    }

    public int getCloudColorTexture() {
        return cloudFbo.getColorTexture(colorAttachment);
    }

    public int getCloudDistanceTexture() {
        return cloudFbo.getColorTexture(distanceAttachment);
    }
}
