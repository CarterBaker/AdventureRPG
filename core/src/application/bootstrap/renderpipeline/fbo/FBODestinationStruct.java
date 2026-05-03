package application.bootstrap.renderpipeline.fbo;

import engine.root.StructPackage;

public class FBODestinationStruct extends StructPackage {

    /*
     * Destination rectangle for an FBO blit.
     * When present, the blit shader maps the FBO into this pixel rect
     * rather than filling the full screen. Null means fullscreen.
     */

    public float x;
    public float y;
    public float width;
    public float height;

    public FBODestinationStruct(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

}