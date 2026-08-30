package application.bootstrap.renderpipeline.pbo;

import java.nio.ByteBuffer;

import application.bootstrap.renderpipeline.pbomanager.PboManager;
import application.kernel.windowpipeline.window.WindowInstance;
import engine.root.InstancePackage;

public class PboInstance extends InstancePackage {

    /*
     * Runtime double-buffered pixel-pack-buffer wrapper. Holds the raw GL
     * buffer names and per-slot pending state directly — no indirection
     * at call sites. Every operation delegates back to PboManager so GL
     * binding, allocation, and buffer regeneration stay in one place, the
     * same split FboInstance follows for framebuffers. Two fixed slots
     * take turns being written by the GPU and read by the CPU: capture()
     * retrieves whichever slot was queued longest ago — by construction
     * already finished on the GPU — while queuing a fresh read into the
     * other, so the CPU is never blocked waiting on the front-buffer
     * transfer to complete.
     */

    private PboManager pboManager;

    private final int[] pboHandles = new int[2];
    private final boolean[] pending = new boolean[2];
    private int writeSlot;

    private WindowInstance allocatedWindow;
    private int width;
    private int height;

    // Constructor \\

    public void constructor(int pboA, int pboB, WindowInstance window, int width, int height) {
        this.pboHandles[0] = pboA;
        this.pboHandles[1] = pboB;
        this.allocatedWindow = window;
        this.width = width;
        this.height = height;
    }

    @Override
    protected void get() {
        this.pboManager = get(PboManager.class);
    }

    // Capture \\

    public boolean capture(WindowInstance window, int width, int height, ByteBuffer destination) {
        return pboManager.capture(this, window, width, height, destination);
    }

    public void queue(WindowInstance window, int width, int height) {
        pboManager.queue(this, window, width, height);
    }

    public boolean tryRetrieve(ByteBuffer destination) {
        return pboManager.tryRetrieve(this, destination);
    }

    // Accessible — read and written by PboManager \\

    public int[] getPboHandles() {
        return pboHandles;
    }

    public void setPboHandles(int pboA, int pboB) {
        this.pboHandles[0] = pboA;
        this.pboHandles[1] = pboB;
    }

    public boolean[] getPending() {
        return pending;
    }

    public int getWriteSlot() {
        return writeSlot;
    }

    public void advanceWriteSlot() {
        writeSlot = 1 - writeSlot;
    }

    public WindowInstance getAllocatedWindow() {
        return allocatedWindow;
    }

    public void setAllocatedWindow(WindowInstance window) {
        this.allocatedWindow = window;
    }

    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}