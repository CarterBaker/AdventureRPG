package application.runtime.photo;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javax.imageio.ImageIO;

import application.bootstrap.entitypipeline.playermanager.PlayerManager;
import application.bootstrap.renderpipeline.fbo.FBOInstance;
import application.bootstrap.renderpipeline.pbo.PBOInstance;
import application.bootstrap.renderpipeline.pbomanager.PBOManager;
import application.kernel.inputpipeline.inputmanager.InputManager;
import application.kernel.threadpipeline.thread.ThreadHandle;
import application.kernel.windowpipeline.window.WindowInstance;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import application.runtime.RuntimeSetting;
import application.runtime.postprocessing.PostEffectsSystem;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.settings.KeyBindings;

public class ScreenshotSystem extends SystemPackage {

    /*
     * Takes this window's screenshots: the world as the post effects finish
     * it, photo mode's look included and no menu, HUD or overlay drawn over
     * it, at the window's full size. The Screenshot binding takes one
     * whenever a player is in the world, and photo mode's Take Photo asks
     * for one through request(). The read is queued on the GPU one frame and
     * fetched the next, then written as a PNG to the Screenshots folder of
     * the game's documents on the General thread, so a shot never stalls the
     * frame. Shots asked for while one is still on its way are let go.
     */

    // Internal
    private InputManager inputManager;
    private PlayerManager playerManager;
    private PBOManager pboManager;
    private WindowManager windowManager;
    private PostEffectsSystem postEffectsSystem;
    private ThreadHandle threadHandle;

    // Capture
    private PBOInstance pbo;
    private ByteBuffer readback;
    private boolean requested;
    private boolean queued;
    private int queuedWidth;
    private int queuedHeight;

    // Naming
    private DateTimeFormatter nameFormat;

    // Base \\

    @Override
    protected void create() {

        // Naming
        this.nameFormat = DateTimeFormatter.ofPattern(RuntimeSetting.SCREENSHOT_NAME_PATTERN);
    }

    @Override
    protected void get() {
        this.inputManager = get(InputManager.class);
        this.playerManager = get(PlayerManager.class);
        this.pboManager = get(PBOManager.class);
        this.windowManager = get(WindowManager.class);
        this.postEffectsSystem = get(PostEffectsSystem.class);
        this.threadHandle = getThreadHandleFromThreadName(EngineSetting.GENERAL_THREAD_NAME);
    }

    @Override
    protected void update() {

        WindowInstance window = context.getWindow();

        if (playerManager.hasPlayerForWindow(window.getWindowID())
                && inputManager.bindingClicked(KeyBindings.SCREENSHOT, window))
            request();

        if (queued)
            fetch(window);
        else if (requested)
            queue(window);
    }

    // Capture \\

    // A shot of the next frame the window finishes
    public void request() {
        this.requested = true;
    }

    // The composite as it last drew, its read queued on the GPU
    private void queue(WindowInstance window) {

        FBOInstance composite = postEffectsSystem.getCompositeFbo();
        WindowInstance previous = windowManager.enterGLContext(window);

        if (pbo == null)
            this.pbo = pboManager.createPbo(window.getGLWindow(), composite.getWidth(), composite.getHeight());

        ensureReadback(composite.getWidth(), composite.getHeight());
        pbo.captureFbo(window.getGLWindow(), composite, readback);
        windowManager.exitGLContext(previous);

        this.requested = false;
        this.queued = true;
        this.queuedWidth = composite.getWidth();
        this.queuedHeight = composite.getHeight();
    }

    // The read queued last frame fetched, then written away from the frame
    private void fetch(WindowInstance window) {

        WindowInstance previous = windowManager.enterGLContext(window);
        boolean retrieved = pbo.tryRetrieve(readback);

        windowManager.exitGLContext(previous);
        this.queued = false;

        if (!retrieved)
            return;

        BufferedImage image = toImage(queuedWidth, queuedHeight);
        File file = resolveFile();

        executeAsync(threadHandle, () -> write(image, file));
    }

    private void ensureReadback(int width, int height) {

        int length = width * height * EngineSetting.BYTES_PER_PIXEL_BGRA;

        if (readback == null || readback.capacity() < length)
            this.readback = ByteBuffer.allocateDirect(length).order(ByteOrder.nativeOrder());
    }

    // The fetched BGRA rows, bottom first, laid into an image top first
    private BufferedImage toImage(int width, int height) {

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        int stride = width * EngineSetting.BYTES_PER_PIXEL_BGRA;

        for (int y = 0; y < height; y++) {

            int row = (height - 1 - y) * stride;

            for (int x = 0; x < width; x++) {

                int at = row + x * EngineSetting.BYTES_PER_PIXEL_BGRA;
                int blue = readback.get(at) & 0xFF;
                int green = readback.get(at + 1) & 0xFF;
                int red = readback.get(at + 2) & 0xFF;

                image.setRGB(x, y, (red << 16) | (green << 8) | blue);
            }
        }

        return image;
    }

    // Writing \\

    // Screenshot_<date>_<time>.png in the Screenshots folder, numbered on when two land in one second
    private File resolveFile() {

        File directory = new File(internal.path, EngineSetting.SCREENSHOT_DIRECTORY);
        String stem = RuntimeSetting.SCREENSHOT_NAME_PREFIX + LocalDateTime.now().format(nameFormat);
        File file = new File(directory, stem + RuntimeSetting.SCREENSHOT_EXTENSION);

        for (int copy = 2; file.exists(); copy++)
            file = new File(directory, stem + RuntimeSetting.SCREENSHOT_COPY_SEPARATOR + copy
                    + RuntimeSetting.SCREENSHOT_EXTENSION);

        return file;
    }

    // Worker — the image written as a PNG
    private void write(BufferedImage image, File file) {

        try {

            if (!file.getParentFile().exists() && !file.getParentFile().mkdirs())
                throw new IOException("could not make " + file.getParentFile().getAbsolutePath());

            if (!ImageIO.write(image, RuntimeSetting.SCREENSHOT_FORMAT, file))
                errorLog("No image writer for format '" + RuntimeSetting.SCREENSHOT_FORMAT + "'");
            else
                log("Screenshot saved: " + file.getAbsolutePath());
        } catch (IOException e) {
            errorLog("Failed to save screenshot: " + file.getAbsolutePath(), e);
        }
    }
}
