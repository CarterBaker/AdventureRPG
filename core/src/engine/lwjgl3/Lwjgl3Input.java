package engine.lwjgl3;

import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryStack;

import engine.input.Input;
import engine.input.InputListener;
import engine.root.EngineSetting;

class Lwjgl3Input implements Input {

    /*
     * Collects raw GLFW events and forwards them to registered InputListeners.
     * Tracks clicked, held, and released state for keys and mouse buttons.
     * Click and release latches are cleared each frame in endFrame().
     *
     * Sprite cursors are created on first use via glGetTexImage readback and
     * cached by GPU handle. Subsequent calls with the same handle skip the
     * readback and set the cursor directly. All cached cursors are destroyed
     * in destroyCursors().
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
    private long cursorDefault;
    private long cursorResizeH;
    private long cursorResizeV;

    // Sprite Cursor Cache — gpuHandle → GLFW cursor handle
    private final Int2LongOpenHashMap spriteCursorCache = new Int2LongOpenHashMap();

    Lwjgl3Input(long window) {
        this.window = window;
        this.spriteCursorCache.defaultReturnValue(0L);
    }

    // Internal \\

    void onCursor(double x, double y) {

        double yUp = toYUp(y);

        if (firstCursor) {
            cursorX = x;
            cursorY = yUp;
            firstCursor = false;
        }

        deltaX = (float) (x - cursorX);
        deltaY = (float) (cursorY - yUp);
        cursorX = x;
        cursorY = yUp;

        for (int i = 0; i < listeners.size(); i++)
            listeners.get(i).onMouseMoved((int) x, (int) yUp);
    }

    private double toYUp(double yDown) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer height = stack.mallocInt(1);
            GLFW.glfwGetWindowSize(window, null, height);
            return height.get(0) - yDown;
        }
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

    public void initCursors() {
        this.cursorDefault = GLFW.glfwCreateStandardCursor(GLFW.GLFW_ARROW_CURSOR);
        this.cursorResizeH = GLFW.glfwCreateStandardCursor(GLFW.GLFW_HRESIZE_CURSOR);
        this.cursorResizeV = GLFW.glfwCreateStandardCursor(GLFW.GLFW_VRESIZE_CURSOR);
    }

    public void setCursorShape(long windowHandle, int shape) {
        long cursor = switch (shape) {
            case EngineSetting.CURSOR_RESIZE_H -> cursorResizeH;
            case EngineSetting.CURSOR_RESIZE_V -> cursorResizeV;
            default -> cursorDefault;
        };
        GLFW.glfwSetCursor(windowHandle, cursor);
    }

    public void destroyCursors() {
        GLFW.glfwDestroyCursor(cursorDefault);
        GLFW.glfwDestroyCursor(cursorResizeH);
        GLFW.glfwDestroyCursor(cursorResizeV);

        for (long cursor : spriteCursorCache.values())
            GLFW.glfwDestroyCursor(cursor);

        spriteCursorCache.clear();
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

    @Override
    public void setCursorFromSprite(int gpuHandle, int width, int height) {

        long cursor = spriteCursorCache.get(gpuHandle);

        if (cursor == 0L) {
            ByteBuffer pixels = BufferUtils.createByteBuffer(width * height * 4);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, gpuHandle);
            GL11.glGetTexImage(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

            GLFWImage image = GLFWImage.malloc();
            image.set(width, height, pixels);
            cursor = GLFW.glfwCreateCursor(image, 0, 0);
            image.free();

            spriteCursorCache.put(gpuHandle, cursor);
        }

        GLFW.glfwSetCursor(window, cursor);
    }

    @Override
    public void clearCursor() {
        GLFW.glfwSetCursor(window, 0L);
    }

    /*
     * Updates cursorX/Y from a direct platform query without computing delta
     * or notifying listeners. Called by the platform after syncInputForWindow
     * so getMouseX/Y() returns current coords even when this window has no OS
     * focus and cursor-move callbacks have not fired. firstCursor is left
     * untouched — the next real onCursor call will handle delta correctly
     * from the refreshed position.
     */
    void refreshCursor(double x, double yDown) {
        cursorX = x;
        cursorY = toYUp(yDown);
    }
}