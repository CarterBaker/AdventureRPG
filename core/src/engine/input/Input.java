package engine.input;

public interface Input {

    /*
     * Platform input contract. Exposes click, held, and release state for both
     * keys and mouse buttons, plus cursor position, deltas, cursor shape, and
     * accumulated scroll delta for this frame.
     */

    void addListener(InputListener listener);

    void removeListener(InputListener listener);

    boolean isKeyClicked(int key);

    boolean isKeyDown(int key);

    boolean isKeyReleased(int key);

    boolean isMouseClicked(int button);

    boolean isMouseDown(int button);

    boolean isMouseReleased(int button);

    float getMouseX();

    float getMouseY();

    float getDeltaX();

    float getDeltaY();

    float getScrollX();

    float getScrollY();

    void setCursorCatched(boolean captured);

    void setCursorFromSprite(int gpuHandle, int width, int height);

    void clearCursor();
}