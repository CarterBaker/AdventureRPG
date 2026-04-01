package com.internal.platform.backends.lwjgl3;

import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;

import java.nio.IntBuffer;

public class Lwjgl3Window {
    private final long handle;
    Lwjgl3Window(long handle){this.handle=handle;}
    public long getWindowHandle(){return handle;}
    public int getPositionX(){ IntBuffer x=BufferUtils.createIntBuffer(1); IntBuffer y=BufferUtils.createIntBuffer(1); GLFW.glfwGetWindowPos(handle,x,y); return x.get(0); }
    public int getPositionY(){ IntBuffer x=BufferUtils.createIntBuffer(1); IntBuffer y=BufferUtils.createIntBuffer(1); GLFW.glfwGetWindowPos(handle,x,y); return y.get(0); }
}
