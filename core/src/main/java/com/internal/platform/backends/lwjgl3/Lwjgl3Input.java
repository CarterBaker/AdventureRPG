package com.internal.platform.backends.lwjgl3;

import com.internal.platform.Input;
import com.internal.platform.InputProcessor;
import org.lwjgl.glfw.GLFW;

class Lwjgl3Input implements Input {
    private InputProcessor processor;
    private int deltaX;
    private int deltaY;
    private double lastX;
    private double lastY;
    private boolean first = true;

    @Override public void setInputProcessor(InputProcessor inputProcessor) { this.processor = inputProcessor; }
    @Override public int getDeltaX() { return deltaX; }
    @Override public int getDeltaY() { return deltaY; }
    @Override public void setCursorCatched(boolean captured) { GLFW.glfwSetInputMode(Lwjgl3Application.mainWindowHandle(), GLFW.GLFW_CURSOR, captured ? GLFW.GLFW_CURSOR_DISABLED : GLFW.GLFW_CURSOR_NORMAL); }

    void onCursor(double x, double y) {
        if (first) { lastX = x; lastY = y; first = false; }
        deltaX = (int)(x - lastX); deltaY = (int)(y - lastY);
        lastX = x; lastY = y;
        if (processor != null) processor.mouseMoved((int)x,(int)y);
    }

    void onMouseButton(int button, int action) {
        if (processor == null) return;
        int mapped = (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) ? Buttons.LEFT : Buttons.RIGHT;
        if (action == GLFW.GLFW_PRESS) processor.touchDown(0,0,0,mapped);
        if (action == GLFW.GLFW_RELEASE) processor.touchUp(0,0,0,mapped);
    }

    void onKey(int key, int action) {
        if (processor == null) return;
        if (action == GLFW.GLFW_PRESS) processor.keyDown(key);
        if (action == GLFW.GLFW_RELEASE) processor.keyUp(key);
    }

    void endFrame() { deltaX = 0; deltaY = 0; }
}
