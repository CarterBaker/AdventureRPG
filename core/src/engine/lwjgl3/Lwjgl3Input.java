package engine.lwjgl3;

import engine.util.input.Input;
import engine.util.input.InputListener;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.lwjgl.glfw.GLFW;

class Lwjgl3Input implements Input {

    /*
     * Collects raw GLFW events and forwards them to registered InputListeners.
     * Tracks clicked, held, and released state for keys and mouse buttons.
     * Click and release latches are cleared each frame in endFrame().
     */

    // Internal
    private final long window;
    private final ObjectArrayList<InputListener> listeners = new ObjectArrayList<>();

    // Key State
    private final IntOpenHashSet heldKeys = new IntOpenHashSet();
    private final IntOpenHashSet clickedKeys = new IntOpenHashSet();
    private final IntOpenHashSet releasedKeys = new IntOpenHashSet();

    // Mouse State
    private final IntOpenHashSet heldButtons = new IntOpenHashSet();
    private final IntOpenHashSet clickedButtons = new IntOpenHashSet();
    private final IntOpenHashSet releasedButtons = new IntOpenHashSet();

    // Cursor State
    private double cursorX;
    private double cursorY;
    private float deltaX;
    private float deltaY;
    private boolean firstCursor = true;

    Lwjgl3Input(long window) {
        this.window = window;
    }

    // Internal \\

    void onCursor(double x, double y) {

        if (firstCursor) {
            cursorX = x;
            cursorY = y;
            firstCursor = false;
        }

        deltaX = (float) (x - cursorX);
        deltaY = (float) (y - cursorY);
        cursorX = x;
        cursorY = y;

        for (int i = 0; i < listeners.size(); i++)
            listeners.get(i).onMouseMoved((int) x, (int) y);
    }

    void onMouseButton(int button, int action) {

        if (action == GLFW.GLFW_PRESS) {
            heldButtons.add(button);
            clickedButtons.add(button);
            for (int i = 0; i < listeners.size(); i++)
                listeners.get(i).onMouseDown(button, (int) cursorX, (int) cursorY);
        }

        if (action == GLFW.GLFW_RELEASE) {
            heldButtons.remove(button);
            releasedButtons.add(button);
            for (int i = 0; i < listeners.size(); i++)
                listeners.get(i).onMouseUp(button, (int) cursorX, (int) cursorY);
        }
    }

    void onScroll(double dx, double dy) {
        for (int i = 0; i < listeners.size(); i++)
            listeners.get(i).onScroll((float) dx, (float) dy);
    }

    void onKey(int key, int action) {

        if (action == GLFW.GLFW_PRESS) {
            heldKeys.add(key);
            clickedKeys.add(key);
            for (int i = 0; i < listeners.size(); i++)
                listeners.get(i).onKeyDown(key);
        }

        if (action == GLFW.GLFW_RELEASE) {
            heldKeys.remove(key);
            releasedKeys.add(key);
            for (int i = 0; i < listeners.size(); i++)
                listeners.get(i).onKeyUp(key);
        }
    }

    void onChar(int codepoint) {
        for (int i = 0; i < listeners.size(); i++)
            listeners.get(i).onChar((char) codepoint);
    }

    void endFrame() {
        deltaX = 0;
        deltaY = 0;
        clickedKeys.clear();
        releasedKeys.clear();
        clickedButtons.clear();
        releasedButtons.clear();
    }

    // Input \\

    @Override
    public void addListener(InputListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(InputListener listener) {
        listeners.remove(listener);
    }

    @Override
    public boolean isKeyClicked(int key) {
        return clickedKeys.contains(key);
    }

    @Override
    public boolean isKeyDown(int key) {
        return heldKeys.contains(key);
    }

    @Override
    public boolean isKeyReleased(int key) {
        return releasedKeys.contains(key);
    }

    @Override
    public boolean isMouseClicked(int button) {
        return clickedButtons.contains(button);
    }

    @Override
    public boolean isMouseDown(int button) {
        return heldButtons.contains(button);
    }

    @Override
    public boolean isMouseReleased(int button) {
        return releasedButtons.contains(button);
    }

    @Override
    public float getMouseX() {
        return (float) cursorX;
    }

    @Override
    public float getMouseY() {
        return (float) cursorY;
    }

    @Override
    public float getDeltaX() {
        return deltaX;
    }

    @Override
    public float getDeltaY() {
        return deltaY;
    }

    @Override
    public void setCursorCatched(boolean captured) {
        GLFW.glfwSetInputMode(
                window,
                GLFW.GLFW_CURSOR,
                captured ? GLFW.GLFW_CURSOR_DISABLED : GLFW.GLFW_CURSOR_NORMAL);
    }
}