package editor.runtime.viewcamera;

public interface ViewCameraProvider {

    /*
     * Supplies what a context's view camera frames. Every context that
     * creates a ViewCameraSystem implements it.
     */

    ViewCameraSubject getViewCameraSubject();
}
