package com.internal.platform.backends.lwjgl3;

public class Lwjgl3WindowConfiguration {
    String title = "Window";
    int width = 1280;
    int height = 720;

    public void setTitle(String title) { this.title = title; }
    public void setWindowedMode(int width, int height) { this.width = width; this.height = height; }
}
