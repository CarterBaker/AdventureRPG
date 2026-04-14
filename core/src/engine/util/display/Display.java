package engine.util.display;

public interface Display {

    /*
     * Platform graphics state contract. Implemented by the backend graphics
     * class. Provides window dimensions and frame timing.
     */

    int getWidth();

    int getHeight();

    float getDeltaTime();

    boolean isFullscreen();
}