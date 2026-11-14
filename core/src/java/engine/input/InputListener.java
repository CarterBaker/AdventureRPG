package engine.input;

public interface InputListener {

    /*
     * Event-driven input callbacks. All methods default to no-op.
     * Implement only the events the receiver cares about.
     */

    default void onKeyDown(int key) {
    }

    default void onKeyUp(int key) {
    }

    default void onChar(char character) {
    }

    default void onMouseDown(int button, int x, int y) {
    }

    default void onMouseUp(int button, int x, int y) {
    }

    default void onMouseMoved(int x, int y) {
    }

    default void onScroll(float dx, float dy) {
    }
}