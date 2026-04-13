package engine.input;

public interface Input {

    /*
     * Platform input contract. Implemented by the backend input class.
     */

    void addListener(InputListener listener);

    void removeListener(InputListener listener);

    boolean isKeyDown(int key);

    boolean isMouseDown(int button);

    float getDeltaX();

    float getDeltaY();

    void setCursorCatched(boolean captured);
}