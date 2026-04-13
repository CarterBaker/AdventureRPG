package engine.util.graphics;

public interface Graphics {

    /*
     * Platform graphics state contract. Implemented by the backend graphics
     * class. Provides window dimensions and frame timing.
     */

    int getWidth();

    int getHeight();

    float getDeltaTime();

    boolean isFullscreen();
}