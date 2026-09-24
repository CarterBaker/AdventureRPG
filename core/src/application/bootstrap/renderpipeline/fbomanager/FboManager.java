package application.bootstrap.renderpipeline.fbomanager;

import application.bootstrap.renderpipeline.fbo.AttachmentStruct;
import application.bootstrap.renderpipeline.fbo.FboData;
import application.bootstrap.renderpipeline.fbo.FboInstance;
import application.bootstrap.renderpipeline.fbo.FboSizingStrategy;
import application.bootstrap.renderpipeline.fborendersystem.FboRenderSystem;
import application.kernel.windowpipeline.window.WindowInstance;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import engine.graphics.gl.GL30;
import engine.root.ManagerPackage;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class FboManager extends ManagerPackage {

    /*
     * Owns FBO data registration, canonical instance creation, and clone
     * distribution. All window-relative instances — canonical and cloned —
     * are tracked so resizeWindowRelative keeps every live FboInstance in sync.
     *
     * Only OS windows (hasNativeHandle) are permitted to resize global
     * (null-tracked)
     * FBOs. Logical windows resize only their own tracked instances. Global FBOs
     * are owned by the OS window — a logical window resizing them would clear any
     * rendered content written by a prior logical window draw call in the same
     * frame.
     *
     * Cloned instances are created with the target window's OS window context
     * current. Textures are shared across the context share group, framebuffer
     * objects are not — so when a logical window is reparented onto a
     * different OS window, migrateWindowFbos() rebuilds only the framebuffer
     * objects of its clones inside the new context and reuses the textures.
     *
     * releaseWindowFbos() queues a disposed window's clones for deletion.
     * Deletion happens at the start of the next update, once the frame that
     * may still have queued them for a blit has been drawn. Framebuffer
     * objects are deleted inside the context that owns them; if that OS
     * window is already gone its framebuffers went with it.
     */

    // Internal
    private FBOBuilder internalBuilder;
    private WindowManager windowManager;
    private FboRenderSystem fboRenderSystem;

    // Data Registry
    private Object2ObjectOpenHashMap<String, FboData> fboName2Data;
    private ObjectArrayList<FboData> orderedFboData;

    // Instance Registry
    private Object2ObjectOpenHashMap<String, FboInstance> fboName2Instance;

    // Resize Tracking
    private Object2ObjectOpenHashMap<WindowInstance, ObjectArrayList<FboInstance>> window2RelativeInstances;

    // Clone Tracking
    private Object2ObjectOpenHashMap<WindowInstance, ObjectArrayList<FboInstance>> window2ClonedInstances;

    // Release — keyed by the OS window whose context owns the framebuffers
    private Object2ObjectOpenHashMap<WindowInstance, ObjectArrayList<FboInstance>> glWindow2PendingReleases;

    // Internal \\

    @Override
    protected void create() {
        this.fboName2Data = new Object2ObjectOpenHashMap<>();
        this.fboName2Instance = new Object2ObjectOpenHashMap<>();
        this.orderedFboData = new ObjectArrayList<>();
        this.window2RelativeInstances = new Object2ObjectOpenHashMap<>();
        this.window2ClonedInstances = new Object2ObjectOpenHashMap<>();
        this.glWindow2PendingReleases = new Object2ObjectOpenHashMap<>();
        create(FBOLoader.class);
    }

    @Override
    protected void get() {
        this.internalBuilder = get(FBOBuilder.class);
        this.windowManager = get(WindowManager.class);
        this.fboRenderSystem = get(FboRenderSystem.class);
    }

    @Override
    protected void update() {
        flushPendingReleases();
    }

    void addFboData(FboData data) {
        String name = data.getName();

        if (fboName2Data.containsKey(name))
            return;

        fboName2Data.put(name, data);
        orderedFboData.add(data);
    }

    // Bind \\

    public void bind(FboInstance fbo) {

        if (fbo == null) {
            unbind();
            return;
        }

        if (fbo.getFramebuffers().isEmpty()) {
            unbind();
            return;
        }

        FBOGLSLUtility.bindFramebuffer(fbo.getFramebuffers().getInt(0));
        FBOGLSLUtility.setViewport(fbo.getWidth(), fbo.getHeight());
    }

    public void unbind() {
        FBOGLSLUtility.unbindFramebuffer();
    }

    // Resize \\

    public void resize(FboInstance fbo, int width, int height) {

        if (fbo == null || width <= 0 || height <= 0)
            return;

        if (fbo.getWidth() == width && fbo.getHeight() == height)
            return;

        ObjectArrayList<AttachmentStruct> attachments = fbo.getFboData().getAttachments();
        int colorIndex = 0;

        for (int i = 0; i < attachments.size(); i++) {
            AttachmentStruct attachment = attachments.get(i);

            if (attachment.isDepth()) {
                if (!fbo.getDepthTextures().isEmpty())
                    FBOGLSLUtility.resizeDepthTexture(fbo.getDepthTextures().getInt(0), width, height);
            }

            else {

                if (colorIndex < fbo.getTextures().size())
                    FBOGLSLUtility.resizeColorTexture(
                            fbo.getTextures().getInt(colorIndex),
                            attachment.getInternalFormat(),
                            width, height);
                colorIndex++;
            }
        }

        fbo.setSize(width, height);
    }

    public void resize(String name, int width, int height) {
        resize(getFbo(name), width, height);
    }

    public void resizeWindowRelative(WindowInstance window, int width, int height) {
        if (!window.hasNativeHandle()) {
            // Logical windows (tabs, content) own only their own tracked FBO instances.
            // Global (null-tracked) FBOs belong to the OS window. Resizing them here
            // would clear content rendered by an earlier logical window draw call
            // in the same frame before the OS window gets a chance to blit it.
            resizeTracked(window, width, height);
        } else {
            // OS windows resize both global FBOs and their own tracked instances.
            resizeTracked(null, width, height);
            resizeTracked(window, width, height);
        }
    }

    // Accessible \\

    public FboInstance getFbo(String name) {
        FboInstance instance = fboName2Instance.get(name);

        if (instance != null)
            return instance;

        FboData data = resolveFboData(name);
        instance = internalBuilder.buildInstance(data);
        fboName2Instance.put(name, instance);

        if (data.getSizingStrategy() == FboSizingStrategy.WINDOW_RELATIVE)
            track(window2RelativeInstances, instance, null);

        return instance;
    }

    public FboInstance cloneFbo(String name, WindowInstance window) {
        FboData data = resolveFboData(name);

        WindowInstance previous = enterContext(window);
        FboInstance instance = internalBuilder.buildInstance(data);
        exitContext(previous);

        track(window2ClonedInstances, instance, window);

        if (data.getSizingStrategy() == FboSizingStrategy.WINDOW_RELATIVE)
            track(window2RelativeInstances, instance, window);

        return instance;
    }

    // Migration \\

    /*
     * Rebuilds the framebuffer objects of every clone owned by the given
     * window inside the context of the OS window it now composites onto.
     * Called after a reparent; previousGLWindow is the OS window it was on.
     */
    public void migrateWindowFbos(WindowInstance window, WindowInstance previousGLWindow) {

        ObjectArrayList<FboInstance> instances = window2ClonedInstances.get(window);

        if (instances == null || previousGLWindow == window.getGLWindow())
            return;

        WindowInstance previous = windowManager.getContextWindow();
        deleteFramebuffers(previousGLWindow, instances);
        internal.windowPlatform.makeContextCurrent(window.getGLWindow());

        for (int i = 0; i < instances.size(); i++) {
            FboInstance instance = instances.get(i);
            instance.getFramebuffers().add(internalBuilder.buildFramebuffer(
                    instance.getFboData(), instance.getTextures(), instance.getDepthTextures()));
        }

        exitContext(previous);
    }

    // Release \\

    /*
     * Stops tracking every clone owned by the given window and queues its GL
     * resources for deletion on the next update. Safe to call more than once.
     */
    public void releaseWindowFbos(WindowInstance window) {

        ObjectArrayList<FboInstance> instances = window2ClonedInstances.remove(window);
        window2RelativeInstances.remove(window);

        if (instances == null)
            return;

        WindowInstance glWindow = window.getGLWindow();
        ObjectArrayList<FboInstance> pending = glWindow2PendingReleases.get(glWindow);

        if (pending == null) {
            pending = new ObjectArrayList<>();
            glWindow2PendingReleases.put(glWindow, pending);
        }

        pending.addAll(instances);
    }

    private void flushPendingReleases() {

        if (glWindow2PendingReleases.isEmpty())
            return;

        WindowInstance previous = windowManager.getContextWindow();

        for (Object2ObjectOpenHashMap.Entry<WindowInstance, ObjectArrayList<FboInstance>> entry
                : glWindow2PendingReleases.object2ObjectEntrySet()) {
            deleteFramebuffers(entry.getKey(), entry.getValue());
            deleteTextures(entry.getValue());
        }

        glWindow2PendingReleases.clear();
        exitContext(previous);
    }

    private void deleteFramebuffers(WindowInstance glWindow, ObjectArrayList<FboInstance> instances) {

        boolean contextAlive = glWindow.hasNativeHandle();

        if (contextAlive)
            internal.windowPlatform.makeContextCurrent(glWindow);

        for (int i = 0; i < instances.size(); i++) {

            IntArrayList framebuffers = instances.get(i).getFramebuffers();

            if (contextAlive)
                for (int j = 0; j < framebuffers.size(); j++)
                    FBOGLSLUtility.deleteFramebuffer(framebuffers.getInt(j));

            framebuffers.clear();
        }
    }

    private void deleteTextures(ObjectArrayList<FboInstance> instances) {

        for (int i = 0; i < instances.size(); i++) {

            FboInstance instance = instances.get(i);

            for (int j = 0; j < instance.getTextures().size(); j++)
                FBOGLSLUtility.deleteTexture(instance.getTextures().getInt(j));

            for (int j = 0; j < instance.getDepthTextures().size(); j++)
                FBOGLSLUtility.deleteTexture(instance.getDepthTextures().getInt(j));

            instance.getTextures().clear();
            instance.getDepthTextures().clear();
            fboRenderSystem.removeBlitModel(instance);
        }
    }

    public void request(String fboName) {
        ((FBOLoader) internalLoader).request(fboName);
    }

    // Internal \\

    private FboData resolveFboData(String name) {
        FboData data = fboName2Data.get(name);

        if (data == null) {
            request(name);
            data = fboName2Data.get(name);
        }

        if (data == null)
            throwException("FBO not found in catalog: " + name);

        return data;
    }

    private void resizeTracked(WindowInstance window, int width, int height) {
        ObjectArrayList<FboInstance> instances = window2RelativeInstances.get(window);

        if (instances == null)
            return;

        Object[] elements = instances.elements();
        int count = instances.size();

        for (int i = 0; i < count; i++) {
            FboInstance instance = (FboInstance) elements[i];
            FboData data = instance.getFboData();
            resize(instance, data.scaleWindowDimension(width), data.scaleWindowDimension(height));
        }
    }

    private void track(
            Object2ObjectOpenHashMap<WindowInstance, ObjectArrayList<FboInstance>> window2Instances,
            FboInstance instance,
            WindowInstance window) {

        ObjectArrayList<FboInstance> instances = window2Instances.get(window);

        if (instances == null) {
            instances = new ObjectArrayList<>();
            window2Instances.put(window, instances);
        }

        instances.add(instance);
    }

    /*
     * Makes the GL context of the given window's OS window current and returns
     * the context window that was active before, for exitContext() to restore.
     * Logical windows have no context of their own, so both sides always
     * resolve through getGLWindow().
     */
    private WindowInstance enterContext(WindowInstance window) {
        WindowInstance previous = windowManager.getContextWindow();
        internal.windowPlatform.makeContextCurrent(window.getGLWindow());
        return previous;
    }

    private void exitContext(WindowInstance previous) {
        if (previous != null)
            internal.windowPlatform.makeContextCurrent(previous.getGLWindow());
        else
            internal.windowPlatform.restoreMainContext();
    }
}