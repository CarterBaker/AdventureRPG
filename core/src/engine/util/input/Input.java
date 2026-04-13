package engine.util.input;

public interface Input {

    /*
     * Platform input contract. Exposes click, held, and release state for both
     * keys and mouse buttons, plus cursor position and deltas.
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

    void setCursorCatched(boolean captured);
}