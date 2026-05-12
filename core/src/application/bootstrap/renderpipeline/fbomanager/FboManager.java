package application.bootstrap.renderpipeline.fbomanager;

import application.bootstrap.renderpipeline.fbo.FboData;
import application.bootstrap.renderpipeline.fbo.FboInstance;
import application.bootstrap.renderpipeline.fbo.FboSizingStrategy;
import application.kernel.windowpipeline.window.WindowInstance;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import engine.graphics.gl.GL30;
import engine.root.ManagerPackage;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class FboManager extends ManagerPackage {

    /*
     * Owns FBO data registration, canonical instance creation, and clone
     * distribution. All window-relative instances — canonical and cloned —
     * are tracked so resizeWindowRelative keeps every live FboInstance in sync.
     * Cloned instances are created with the target window's context current so
     * GL resources belong to the correct context.
     */

    // Internal
    private InternalBuilder internalBuilder;
    private WindowManager windowManager;

    // Data Registry
    private Object2ObjectOpenHashMap<String, FboData> fboName2Data;
    private ObjectArrayList<FboData> orderedFboData;

    // Instance Registry
    private Object2ObjectOpenHashMap<String, FboInstance> fboName2Instance;

    // Resize Tracking
    private Object2ObjectOpenHashMap<WindowInstance, ObjectArrayList<FboInstance>> window2RelativeInstances;

    // Internal \\

    @Override
    protected void create() {
        this.fboName2Data = new Object2ObjectOpenHashMap<>();
        this.fboName2Instance = new Object2ObjectOpenHashMap<>();
        this.orderedFboData = new ObjectArrayList<>();
        this.window2RelativeInstances = new Object2ObjectOpenHashMap<>();
        this.internalBuilder = create(InternalBuilder.class);
        create(InternalLoader.class);
    }

    @Override
    protected void get() {
        this.windowManager = get(WindowManager.class);
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

        GLSLUtility.bindFramebuffer(fbo.getFramebuffers().getInt(0));
        GLSLUtility.setViewport(fbo.getWidth(), fbo.getHeight());
    }

    public void unbind() {
        GLSLUtility.unbindFramebuffer();
    }

    // Resize \\

    public void resize(FboInstance fbo, int width, int height) {

        if (fbo == null || width <= 0 || height <= 0)
            return;

        GLSLUtility.resizeTexture(fbo.getTextures().getInt(0), fbo.getFboData().getFormat(), width, height);

        if (fbo.getFboData().hasDepth() && !fbo.getDepthRenderbuffers().isEmpty()) {
            GLSLUtility.bindRenderbuffer(fbo.getDepthRenderbuffers().getInt(0));
            GLSLUtility.renderbufferStorage(GL30.GL_DEPTH_COMPONENT24, width, height);
            GLSLUtility.unbindRenderbuffer();
        }

        fbo.setSize(width, height);
    }

    public void resize(String name, int width, int height) {
        resize(getFbo(name), width, height);
    }

    public void resizeWindowRelative(WindowInstance window, int width, int height) {
        resizeTracked(null, width, height);
        resizeTracked(window, width, height);
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
            registerWindowRelative(instance, null);

        return instance;
    }

    public FboInstance cloneFbo(String name, WindowInstance window) {
        FboData data = resolveFboData(name);

        WindowInstance previous = windowManager.getContextWindow();
        internal.windowPlatform.makeContextCurrent(window);

        FboInstance instance = internalBuilder.buildInstance(data);

        if (previous != null)
            internal.windowPlatform.makeContextCurrent(previous);
        else
            internal.windowPlatform.restoreMainContext();

        if (data.getSizingStrategy() == FboSizingStrategy.WINDOW_RELATIVE)
            registerWindowRelative(instance, window);

        return instance;
    }

    public void request(String fboName) {
        ((InternalLoader) internalLoader).request(fboName);
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

        for (int i = 0; i < count; i++)
            resize((FboInstance) elements[i], width, height);
    }

    private void registerWindowRelative(FboInstance instance, WindowInstance window) {
        ObjectArrayList<FboInstance> instances = window2RelativeInstances.get(window);

        if (instances == null) {
            instances = new ObjectArrayList<>();
            window2RelativeInstances.put(window, instances);
        }

        instances.add(instance);
    }
}