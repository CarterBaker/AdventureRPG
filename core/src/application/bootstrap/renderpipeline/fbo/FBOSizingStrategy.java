package application.bootstrap.renderpipeline.fbo;

public enum FBOSizingStrategy {

    /*
     * How an FBO is sized: relative to the window it renders for, or at the
     * fixed size its JSON declares.
     */

    WINDOW_RELATIVE,
    FIXED
}