package program.core.backends.lwjgl3;

public class Lwjgl3ApplicationConfiguration extends Lwjgl3WindowConfiguration {
    public enum GLEmulation { GL30 }
    private boolean fullscreen;
    private int windowX = -1;
    private int windowY = -1;
    private Lwjgl3WindowAdapter windowListener;

    public static class DisplayMode { public int width,height,refreshRate; public DisplayMode(int w,int h,int rr){width=w;height=h;refreshRate=rr;} }

    public void setOpenGLEmulation(GLEmulation emulation, int major, int minor) {}
    public void useVsync(boolean vsync) {}
    public void setForegroundFPS(int fps) {}
    public void setFullscreenMode(DisplayMode mode) { this.fullscreen = true; this.width = mode.width; this.height = mode.height; }
    public void setWindowPosition(int x, int y) { this.windowX = x; this.windowY = y; }
    public void setWindowListener(Lwjgl3WindowAdapter listener) { this.windowListener = listener; }
    public Lwjgl3WindowAdapter getWindowListener() { return windowListener; }
    public boolean isFullscreen() { return fullscreen; }
    public int getWindowX() { return windowX; }
    public int getWindowY() { return windowY; }

    public static DisplayMode getDisplayMode() { return new DisplayMode(1920,1080,60); }
}
