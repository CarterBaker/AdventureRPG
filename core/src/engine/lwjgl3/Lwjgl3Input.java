package engine.lwjgl3;

import org.lwjgl.glfw.GLFW;

import engine.input.Buttons;
import engine.input.Input;
import engine.input.InputListener;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class Lwjgl3Input implements Input {

    /*
     * Collects raw GLFW events and forwards them to registered InputListeners.
     * Tracks held keys and buttons for polling via isKeyDown / isMouseDown.
     * Delta values accumulate during the frame and are zeroed in endFrame().
     */

    // Internal
    private final long window;
    private final ObjectArrayList<InputListener> listeners = new ObjectArrayList<>();

    // Key State
    private final IntOpenHashSet heldKeys = new IntOpenHashSet();
    private final IntOpenHashSet heldButtons = new IntOpenHashSet();

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

        int mapped = button == GLFW.GLFW_MOUSE_BUTTON_LEFT ? Buttons.LEFT
                : button == GLFW.GLFW_MOUSE_BUTTON_RIGHT ? Buttons.RIGHT
                        : button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE ? Buttons.MIDDLE : button;

        if (action == GLFW.GLFW_PRESS) {
            heldButtons.add(mapped);
            for (int i = 0; i < listeners.size(); i++)
                listeners.get(i).onMouseDown(mapped, (int) cursorX, (int) cursorY);
        }

        if (action == GLFW.GLFW_RELEASE) {
            heldButtons.remove(mapped);
            for (int i = 0; i < listeners.size(); i++)
                listeners.get(i).onMouseUp(mapped, (int) cursorX, (int) cursorY);
        }
    }

    void onScroll(double dx, double dy) {
        for (int i = 0; i < listeners.size(); i++)
            listeners.get(i).onScroll((float) dx, (float) dy);
    }

    void onKey(int key, int action) {

        if (action == GLFW.GLFW_PRESS) {
            heldKeys.add(key);
            for (int i = 0; i < listeners.size(); i++)
                listeners.get(i).onKeyDown(key);
        }

        if (action == GLFW.GLFW_RELEASE) {
            heldKeys.remove(key);
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
    public boolean isKeyDown(int key) {
        return heldKeys.contains(key);
    }

    @Override
    public boolean isMouseDown(int button) {
        return heldButtons.contains(button);
    }

    @Override
    public void setCursorCatched(boolean captured) {
        GLFW.glfwSetInputMode(
                window,
                GLFW.GLFW_CURSOR,
                captured ? GLFW.GLFW_CURSOR_DISABLED : GLFW.GLFW_CURSOR_NORMAL);
    }

    @Override
    public int getDeltaX() {
        return (int) deltaX;
    }

    @Override
    public int getDeltaY() {
        return (int) deltaY;
    }

    public int getX() {
        return (int) cursorX;
    }

    public int getY() {
        return (int) cursorY;
    }
}