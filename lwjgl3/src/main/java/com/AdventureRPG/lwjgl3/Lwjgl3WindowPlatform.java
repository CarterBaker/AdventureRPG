package com.AdventureRPG.lwjgl3;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Window;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3WindowConfiguration;
import com.internal.bootstrap.renderpipeline.window.WindowInstance;
import com.internal.core.engine.WindowPlatform;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;

import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

public class Lwjgl3WindowPlatform implements WindowPlatform {

    private final Map<Integer, Lwjgl3Window> windowID2Native = new HashMap<>();

    @Override
    public void openWindow(WindowInstance window) {

        Lwjgl3WindowConfiguration config = new Lwjgl3WindowConfiguration();
        config.setTitle(window.getTitle());
        config.setWindowedMode(window.getWidth(), window.getHeight());

        Lwjgl3Window nativeWindow = ((Lwjgl3Application) Gdx.app).newWindow(window, config);

        windowID2Native.put(window.getWindowID(), nativeWindow);
        window.setNativeHandle(nativeWindow.getWindowHandle());

        syncWindowSize(window);
    }

    @Override
    public void destroyWindow(WindowInstance window) {
        windowID2Native.remove(window.getWindowID());
        window.setNativeHandle(0L);
    }

    @Override
    public boolean shouldClose(WindowInstance window) {
        if (!window.hasNativeHandle())
            return false;
        return GLFW.glfwWindowShouldClose(window.getNativeHandle());
    }

    @Override
    public void makeContextCurrent(WindowInstance window) {
        if (!window.hasNativeHandle())
            return;
        GLFW.glfwMakeContextCurrent(window.getNativeHandle());
    }

    @Override
    public void swapBuffers(WindowInstance window) {
        if (!window.hasNativeHandle())
            return;
        GLFW.glfwSwapBuffers(window.getNativeHandle());
    }

    @Override
    public void restoreMainContext() {
        if (!(Gdx.graphics instanceof Lwjgl3Graphics))
            return;

        Lwjgl3Window main = ((Lwjgl3Graphics) Gdx.graphics).getWindow();
        GLFW.glfwMakeContextCurrent(main.getWindowHandle());
    }

    @Override
    public void syncWindowSize(WindowInstance window) {
        if (!window.hasNativeHandle())
            return;

        IntBuffer w = BufferUtils.createIntBuffer(1);
        IntBuffer h = BufferUtils.createIntBuffer(1);
        GLFW.glfwGetFramebufferSize(window.getNativeHandle(), w, h);

        int width = w.get(0);
        int height = h.get(0);

        if (width > 0 && height > 0) {
            window.getWindowData().setWidth(width);
            window.getWindowData().setHeight(height);
        }
    }
}