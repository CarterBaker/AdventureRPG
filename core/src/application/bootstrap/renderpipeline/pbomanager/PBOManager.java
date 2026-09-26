package application.bootstrap.renderpipeline.pbomanager;

import java.nio.ByteBuffer;

import application.bootstrap.renderpipeline.pbo.PBOInstance;
import application.kernel.windowpipeline.window.WindowInstance;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;

public class PBOManager extends ManagerPackage {

    /*
     * Owns pixel-pack buffers and the asynchronous readback behind every
     * PBOInstance. Buffers are regenerated when the serving window changes and
     * reallocated in place when only the size changes. Callers make the right
     * context current first.
     */

    private WindowManager windowManager;

    @Override
    protected void get() {
        this.windowManager = get(WindowManager.class);
    }

    // Creation \\

    public PBOInstance createPbo(WindowInstance window, int width, int height) {

        WindowInstance previous = windowManager.getContextWindow();
        internal.windowPlatform.makeContextCurrent(window);

        int pboA = PBOGLSLUtility.genPackBuffer();
        int pboB = PBOGLSLUtility.genPackBuffer();
        allocateBoth(pboA, pboB, width, height);

        if (previous != null)
            internal.windowPlatform.makeContextCurrent(previous);
        else
            internal.windowPlatform.restoreMainContext();

        PBOInstance instance = create(PBOInstance.class);
        instance.constructor(pboA, pboB, window, width, height);

        return instance;
    }

    // Capture \\

    public boolean capture(PBOInstance pbo, WindowInstance window, int width, int height, ByteBuffer destination) {

        reconcile(pbo, window, width, height);

        int retrieveSlot = 1 - pbo.getWriteSlot();
        boolean hasFrame = pbo.getPending()[retrieveSlot];

        if (hasFrame)
            retrieveSlot(pbo, retrieveSlot, destination);

        queueSlot(pbo, pbo.getWriteSlot());
        pbo.getPending()[pbo.getWriteSlot()] = true;
        pbo.advanceWriteSlot();

        return hasFrame;
    }

    public void queue(PBOInstance pbo, WindowInstance window, int width, int height) {

        reconcile(pbo, window, width, height);

        queueSlot(pbo, pbo.getWriteSlot());
        pbo.getPending()[pbo.getWriteSlot()] = true;
        pbo.advanceWriteSlot();
    }

    public boolean tryRetrieve(PBOInstance pbo, ByteBuffer destination) {

        int retrieveSlot = 1 - pbo.getWriteSlot();

        if (!pbo.getPending()[retrieveSlot])
            return false;

        retrieveSlot(pbo, retrieveSlot, destination);

        return true;
    }

    // Internal \\

    private void reconcile(PBOInstance pbo, WindowInstance window, int width, int height) {

        boolean windowChanged = pbo.getAllocatedWindow() != window;

        if (!windowChanged && pbo.getWidth() == width && pbo.getHeight() == height)
            return;

        if (windowChanged) {
            pbo.setPboHandles(PBOGLSLUtility.genPackBuffer(), PBOGLSLUtility.genPackBuffer());
            pbo.setAllocatedWindow(window);
        }

        allocateBoth(pbo.getPboHandles()[0], pbo.getPboHandles()[1], width, height);
        pbo.setSize(width, height);
        pbo.getPending()[0] = false;
        pbo.getPending()[1] = false;
    }

    private void allocateBoth(int pboA, int pboB, int width, int height) {
        int byteCount = width * height * EngineSetting.BYTES_PER_PIXEL_BGRA;
        PBOGLSLUtility.bindPackBuffer(pboA);
        PBOGLSLUtility.allocatePackBuffer(byteCount);
        PBOGLSLUtility.bindPackBuffer(pboB);
        PBOGLSLUtility.allocatePackBuffer(byteCount);
        PBOGLSLUtility.unbindPackBuffer();
    }

    private void queueSlot(PBOInstance pbo, int slot) {
        PBOGLSLUtility.bindPackBuffer(pbo.getPboHandles()[slot]);
        PBOGLSLUtility.queueRead(pbo.getWidth(), pbo.getHeight());
        PBOGLSLUtility.unbindPackBuffer();
    }

    private void retrieveSlot(PBOInstance pbo, int slot, ByteBuffer destination) {
        int byteCount = pbo.getWidth() * pbo.getHeight() * EngineSetting.BYTES_PER_PIXEL_BGRA;
        PBOGLSLUtility.bindPackBuffer(pbo.getPboHandles()[slot]);
        PBOGLSLUtility.retrieve(byteCount, destination);
        PBOGLSLUtility.unbindPackBuffer();
        pbo.getPending()[slot] = false;
    }
}