package application.bootstrap.renderpipeline.pbo;

import java.nio.ByteBuffer;

import application.bootstrap.renderpipeline.pbomanager.PBOManager;
import application.kernel.windowpipeline.window.WindowInstance;
import engine.root.InstancePackage;

public class PBOInstance extends InstancePackage {

    /*
     * Double-buffered pixel-pack buffer. Two slots alternate so capture() reads
     * the slot queued longest ago while queuing a fresh read into the other,
     * never blocking the CPU. All GL work routes through PBOManager.
     */

    private PBOManager pboManager;

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
        this.pboManager = get(PBOManager.class);
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

    // Accessible — read and written by PBOManager \\

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