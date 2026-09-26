package application.bootstrap.renderpipeline.fbomanager;

import application.bootstrap.renderpipeline.fbo.AttachmentStruct;
import application.bootstrap.renderpipeline.fbo.FBOData;
import application.bootstrap.renderpipeline.fbo.FBOInstance;
import application.bootstrap.renderpipeline.fbo.FBOSizingStrategy;
import application.bootstrap.renderpipeline.rendermanager.FBORenderSystem;
import application.kernel.windowpipeline.window.WindowInstance;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import engine.root.ManagerPackage;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class FBOManager extends ManagerPackage {

    /*
     * Owns FBO registration, canonical instances and per-window clones, keeping
     * every window-relative instance sized to its window. Only OS windows
     * resize global FBOs. Reparented windows rebuild their framebuffers in the
     * new GL context, and released clones are deleted a frame later inside the
     * context that owns them.
     */

    // Internal
    private FBOBuilder internalBuilder;
    private WindowManager windowManager;
    private FBORenderSystem fboRenderSystem;

    // Data Registry
    private Object2ObjectOpenHashMap<String, FBOData> fboName2Data;
    private ObjectArrayList<FBOData> orderedFboData;

    // Instance Registry
    private Object2ObjectOpenHashMap<String, FBOInstance> fboName2Instance;

    // Resize Tracking
    private Object2ObjectOpenHashMap<WindowInstance, ObjectArrayList<FBOInstance>> window2RelativeInstances;

    // Clone Tracking
    private Object2ObjectOpenHashMap<WindowInstance, ObjectArrayList<FBOInstance>> window2ClonedInstances;

    // Release — keyed by the OS window whose context owns the framebuffers
    private Object2ObjectOpenHashMap<WindowInstance, ObjectArrayList<FBOInstance>> glWindow2PendingReleases;

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
        this.fboRenderSystem = get(FBORenderSystem.class);
    }

    @Override
    protected void update() {
        flushPendingReleases();
    }

    void addFboData(FBOData data) {
        String name = data.getName();

        if (fboName2Data.containsKey(name))
            return;

        fboName2Data.put(name, data);
        orderedFboData.add(data);
    }

    // Bind \\

    public void bind(FBOInstance fbo) {

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

    public void resize(FBOInstance fbo, int width, int height) {

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
            // Logical windows resize only their own tracked FBOs; global ones belong to the OS window
            resizeTracked(window, width, height);
        } else {
            // OS windows resize both global FBOs and their own tracked instances.
            resizeTracked(null, width, height);
            resizeTracked(window, width, height);
        }
    }

    // Accessible \\

    public FBOInstance getFbo(String name) {
        FBOInstance instance = fboName2Instance.get(name);

        if (instance != null)
            return instance;

        FBOData data = resolveFboData(name);
        instance = internalBuilder.buildInstance(data);
        fboName2Instance.put(name, instance);

        if (data.getSizingStrategy() == FBOSizingStrategy.WINDOW_RELATIVE)
            track(window2RelativeInstances, instance, null);

        return instance;
    }

    public FBOInstance cloneFbo(String name, WindowInstance window) {
        FBOData data = resolveFboData(name);

        WindowInstance previous = enterContext(window);
        FBOInstance instance = internalBuilder.buildInstance(data);
        exitContext(previous);

        track(window2ClonedInstances, instance, window);

        if (data.getSizingStrategy() == FBOSizingStrategy.WINDOW_RELATIVE)
            track(window2RelativeInstances, instance, window);

        return instance;
    }

    // Migration \\

    public void migrateWindowFbos(WindowInstance window, WindowInstance previousGLWindow) {

        ObjectArrayList<FBOInstance> instances = window2ClonedInstances.get(window);

        if (instances == null || previousGLWindow == window.getGLWindow())
            return;

        WindowInstance previous = windowManager.getContextWindow();
        deleteFramebuffers(previousGLWindow, instances);
        internal.windowPlatform.makeContextCurrent(window.getGLWindow());

        for (int i = 0; i < instances.size(); i++) {
            FBOInstance instance = instances.get(i);
            instance.getFramebuffers().add(internalBuilder.buildFramebuffer(
                    instance.getFboData(), instance.getTextures(), instance.getDepthTextures()));
        }

        exitContext(previous);
    }

    // Release \\

    public void releaseWindowFbos(WindowInstance window) {

        ObjectArrayList<FBOInstance> instances = window2ClonedInstances.remove(window);
        window2RelativeInstances.remove(window);

        if (instances == null)
            return;

        WindowInstance glWindow = window.getGLWindow();
        ObjectArrayList<FBOInstance> pending = glWindow2PendingReleases.get(glWindow);

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

        for (Object2ObjectOpenHashMap.Entry<WindowInstance, ObjectArrayList<FBOInstance>> entry
                : glWindow2PendingReleases.object2ObjectEntrySet()) {
            deleteFramebuffers(entry.getKey(), entry.getValue());
            deleteTextures(entry.getValue());
        }

        glWindow2PendingReleases.clear();
        exitContext(previous);
    }

    private void deleteFramebuffers(WindowInstance glWindow, ObjectArrayList<FBOInstance> instances) {

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

    private void deleteTextures(ObjectArrayList<FBOInstance> instances) {

        for (int i = 0; i < instances.size(); i++) {

            FBOInstance instance = instances.get(i);

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

    private FBOData resolveFboData(String name) {
        FBOData data = fboName2Data.get(name);

        if (data == null) {
            request(name);
            data = fboName2Data.get(name);
        }

        if (data == null)
            throwException("FBO not found in catalog: " + name);

        return data;
    }

    private void resizeTracked(WindowInstance window, int width, int height) {
        ObjectArrayList<FBOInstance> instances = window2RelativeInstances.get(window);

        if (instances == null)
            return;

        Object[] elements = instances.elements();
        int count = instances.size();

        for (int i = 0; i < count; i++) {
            FBOInstance instance = (FBOInstance) elements[i];
            FBOData data = instance.getFboData();
            resize(instance, data.scaleWindowDimension(width), data.scaleWindowDimension(height));
        }
    }

    private void track(
            Object2ObjectOpenHashMap<WindowInstance, ObjectArrayList<FBOInstance>> window2Instances,
            FBOInstance instance,
            WindowInstance window) {

        ObjectArrayList<FBOInstance> instances = window2Instances.get(window);

        if (instances == null) {
            instances = new ObjectArrayList<>();
            window2Instances.put(window, instances);
        }

        instances.add(instance);
    }

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