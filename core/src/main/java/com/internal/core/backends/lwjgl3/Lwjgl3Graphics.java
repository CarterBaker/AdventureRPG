package com.internal.core.backends.lwjgl3;

import com.internal.core.graphics.Graphics;

public class Lwjgl3Graphics implements Graphics {
    private int width;
    private int height;
    private float delta;
    private boolean fullscreen;
    private Lwjgl3Window window;

    void setWindow(Lwjgl3Window window){ this.window = window; }
    public Lwjgl3Window getWindow(){ return window; }
    void setSize(int w,int h){ this.width=w; this.height=h; }
    void setDelta(float d){ this.delta=d; }
    void setFullscreen(boolean f){ this.fullscreen=f; }

    @Override public int getWidth(){ return width; }
    @Override public int getHeight(){ return height; }
    @Override public float getDeltaTime(){ return delta; }
    @Override public boolean isFullscreen(){ return fullscreen; }
}
