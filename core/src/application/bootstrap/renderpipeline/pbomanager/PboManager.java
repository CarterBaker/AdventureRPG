package application.bootstrap.renderpipeline.pbomanager;

import java.nio.ByteBuffer;

import application.bootstrap.renderpipeline.pbo.PboInstance;
import application.kernel.windowpipeline.window.WindowInstance;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;

public class PboManager extends ManagerPackage {

    /*
     * Owns pixel-pack-buffer allocation and the asynchronous readback
     * sequencing behind every PboInstance — the same split FboManager
     * follows for framebuffers, so GL binding and buffer lifetime stay
     * centralized in one place rather than scattered across callers.
     * Buffer objects belong to a single GL context, so a reused instance
     * has its GL objects regenerated whenever the window it's asked to
     * serve changes, and is reallocated in place — same object names,
     * fresh storage — whenever only the requested size changes. Callers
     * are responsible for making the correct window's context current
     * before calling queue(), tryRetrieve(), or capture(); this manager
     * only ever switches context itself while first generating a fresh
     * PboInstance's GL objects, matching how FboManager's own bind() and
     * resize() assume the caller's context is already correct.
     */

    private WindowManager windowManager;

    @Override
    protected void get() {
        this.windowManager = get(WindowManager.class);
    }

    // Creation \\

    public PboInstance createPbo(WindowInstance window, int width, int height) {

        WindowInstance previous = windowManager.getContextWindow();
        internal.windowPlatform.makeContextCurrent(window);

        int pboA = PBOGLUtility.genPackBuffer();
        int pboB = PBOGLUtility.genPackBuffer();
        allocateBoth(pboA, pboB, width, height);

        if (previous != null)
            internal.windowPlatform.makeContextCurrent(previous);
        else
            internal.windowPlatform.restoreMainContext();

        PboInstance instance = create(PboInstance.class);
        instance.constructor(pboA, pboB, window, width, height);

        return instance;
    }

    // Capture \\

    public boolean capture(PboInstance pbo, WindowInstance window, int width, int height, ByteBuffer destination) {

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

    public void queue(PboInstance pbo, WindowInstance window, int width, int height) {

        reconcile(pbo, window, width, height);

        queueSlot(pbo, pbo.getWriteSlot());
        pbo.getPending()[pbo.getWriteSlot()] = true;
        pbo.advanceWriteSlot();
    }

    public boolean tryRetrieve(PboInstance pbo, ByteBuffer destination) {

        int retrieveSlot = 1 - pbo.getWriteSlot();

        if (!pbo.getPending()[retrieveSlot])
            return false;

        retrieveSlot(pbo, retrieveSlot, destination);

        return true;
    }

    // Internal \\

    private void reconcile(PboInstance pbo, WindowInstance window, int width, int height) {

        boolean windowChanged = pbo.getAllocatedWindow() != window;

        if (!windowChanged && pbo.getWidth() == width && pbo.getHeight() == height)
            return;

        if (windowChanged) {
            pbo.setPboHandles(PBOGLUtility.genPackBuffer(), PBOGLUtility.genPackBuffer());
            pbo.setAllocatedWindow(window);
        }

        allocateBoth(pbo.getPboHandles()[0], pbo.getPboHandles()[1], width, height);
        pbo.setSize(width, height);
        pbo.getPending()[0] = false;
        pbo.getPending()[1] = false;
    }

    private void allocateBoth(int pboA, int pboB, int width, int height) {
        int byteCount = width * height * EngineSetting.BYTES_PER_PIXEL_BGRA;
        PBOGLUtility.bindPackBuffer(pboA);
        PBOGLUtility.allocatePackBuffer(byteCount);
        PBOGLUtility.bindPackBuffer(pboB);
        PBOGLUtility.allocatePackBuffer(byteCount);
        PBOGLUtility.unbindPackBuffer();
    }

    private void queueSlot(PboInstance pbo, int slot) {
        PBOGLUtility.bindPackBuffer(pbo.getPboHandles()[slot]);
        PBOGLUtility.queueRead(pbo.getWidth(), pbo.getHeight());
        PBOGLUtility.unbindPackBuffer();
    }

    private void retrieveSlot(PboInstance pbo, int slot, ByteBuffer destination) {
        int byteCount = pbo.getWidth() * pbo.getHeight() * EngineSetting.BYTES_PER_PIXEL_BGRA;
        PBOGLUtility.bindPackBuffer(pbo.getPboHandles()[slot]);
        PBOGLUtility.retrieve(byteCount, destination);
        PBOGLUtility.unbindPackBuffer();
        pbo.getPending()[slot] = false;
    }
}