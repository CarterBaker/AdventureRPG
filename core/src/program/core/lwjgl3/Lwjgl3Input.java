package program.core.lwjgl3;

import program.core.input.Input;
import program.core.input.InputProcessor;
import org.lwjgl.glfw.GLFW;

class Lwjgl3Input implements Input {

    /*
     * Collects raw GLFW events and forwards them to the active InputProcessor.
     * Delta and scroll values accumulate during the frame and are zeroed in
     * endFrame().
     */

    // Internal
    private final long window;
    private InputProcessor processor;

    // Cursor State
    private double cursorX;
    private double cursorY;
    private float deltaX;
    private float deltaY;
    private boolean firstCursor = true;

    // Scroll State
    private float scrollX;
    private float scrollY;

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

        if (processor != null)
            processor.mouseMoved((int) x, (int) y);
    }

    void onMouseButton(int button, int action) {

        if (processor == null)
            return;

        int mapped = button == GLFW.GLFW_MOUSE_BUTTON_LEFT ? Buttons.LEFT : Buttons.RIGHT;

        if (action == GLFW.GLFW_PRESS)
            processor.touchDown(0, 0, 0, mapped);

        if (action == GLFW.GLFW_RELEASE)
            processor.touchUp(0, 0, 0, mapped);
    }

    void onScroll(double dx, double dy) {

        scrollX += (float) dx;
        scrollY += (float) dy;

        if (processor != null)
            processor.scrolled((float) dx, (float) dy);
    }

    void onKey(int key, int action) {

        if (processor == null)
            return;

        if (action == GLFW.GLFW_PRESS)
            processor.keyDown(key);

        if (action == GLFW.GLFW_RELEASE)
            processor.keyUp(key);
    }

    void onChar(int codepoint) {

        if (processor != null)
            processor.keyTyped((char) codepoint);
    }

    void endFrame() {
        deltaX = 0;
        deltaY = 0;
        scrollX = 0;
        scrollY = 0;
    }

    // Accessible \\

    @Override
    public void setInputProcessor(InputProcessor processor) {
        this.processor = processor;
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

    public float getScrollX() {
        return scrollX;
    }

    public float getScrollY() {
        return scrollY;
    }
}