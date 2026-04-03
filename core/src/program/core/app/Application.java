package program.core.app;

public interface Application {

    /*
     * Minimal contract for the platform application layer.
     * Implemented by the backend entry point — e.g. Lwjgl3Application.
     */

    void exit();
}