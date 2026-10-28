package editor.bootstrap.imagepipeline.util;

public enum ImageStroke {

    /*
     * What a stroke does to the pixels under its brush. Paint lays its color
     * down, fading it in where the image blends and dithering it in where it
     * cannot. Smooth softens toward each pixel's square, averaging where the
     * image blends and taking the commonest color around where it does not.
     * Swirl turns the pixels under the brush about its centre and Scatter
     * shuffles them, both moving only what is already there, so neither ever
     * writes a color the image could not hold; only these two read the tip's
     * pixels as they stood before the stamp.
     */

    PAINT(false),
    SMOOTH(false),
    SWIRL(true),
    SCATTER(true);

    // Internal
    private final boolean moving;

    // Constructor \\

    ImageStroke(boolean moving) {
        this.moving = moving;
    }

    // Accessible \\

    // True for a stroke that moves the tip's own pixels about
    public boolean isMoving() {
        return moving;
    }
}
