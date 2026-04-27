package application.bootstrap.renderpipeline.fbomanager;

import engine.graphics.gl.GL20;
import engine.root.EngineContext;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.lwjgl.opengl.GL30C;
import application.bootstrap.renderpipeline.fbo.FboData;
import application.bootstrap.renderpipeline.fbo.FboInstance;
import application.bootstrap.renderpipeline.fbo.FboSizingStrategy;

public class FboManager extends ManagerPackage {

    // Internal
    private InternalBuilder internalBuilder;

    // Data Registry
    private Object2ObjectOpenHashMap<String, FboData> fboName2Data;
    private ObjectArrayList<FboData> orderedFboData;

    // Instance Registry
    private Object2ObjectOpenHashMap<String, FboInstance> fboName2Instance;

    // Base \\

    @Override
    protected void create() {
        this.fboName2Data = new Object2ObjectOpenHashMap<>();
        this.fboName2Instance = new Object2ObjectOpenHashMap<>();
        this.orderedFboData = new ObjectArrayList<>();
        this.internalBuilder = create(InternalBuilder.class);
        create(InternalLoader.class);
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
        GL30C.glBindFramebuffer(GL30C.GL_FRAMEBUFFER, fbo.getFramebuffers().getInt(0));
        EngineContext.gl20.glViewport(0, 0, fbo.getWidth(), fbo.getHeight());
    }

    public void unbind() {
        GL30C.glBindFramebuffer(GL30C.GL_FRAMEBUFFER, 0);
    }

    // Resize \\

    public void resize(FboInstance fbo, int width, int height) {
        if (fbo == null || width <= 0 || height <= 0)
            return;
        int texture = fbo.getTextures().getInt(0);
        EngineContext.gl20.glBindTexture(GL20.GL_TEXTURE_2D, texture);
        EngineContext.gl20.glTexImage2D(GL20.GL_TEXTURE_2D, 0, fbo.getFboData().getFormat(), width, height, 0,
                GL20.GL_RGBA,
                GL20.GL_UNSIGNED_BYTE, null);
        EngineContext.gl20.glBindTexture(GL20.GL_TEXTURE_2D, EngineSetting.GL_HANDLE_NONE);
        if (fbo.getFboData().hasDepth() && !fbo.getDepthRenderbuffers().isEmpty()) {
            int depthBuffer = fbo.getDepthRenderbuffers().getInt(0);
            GL30C.glBindRenderbuffer(GL30C.GL_RENDERBUFFER, depthBuffer);
            GL30C.glRenderbufferStorage(GL30C.GL_RENDERBUFFER, GL30C.GL_DEPTH_COMPONENT24, width, height);
            GL30C.glBindRenderbuffer(GL30C.GL_RENDERBUFFER, 0);
        }
        fbo.setSize(width, height);
    }

    public void resize(String name, int width, int height) {
        FboInstance fbo = getFbo(name);
        resize(fbo, width, height);
    }

    public void resizeWindowRelative(int width, int height) {
        Object[] elements = orderedFboData.elements();
        int count = orderedFboData.size();
        for (int i = 0; i < count; i++) {
            FboData data = (FboData) elements[i];
            if (data.getSizingStrategy() != FboSizingStrategy.WINDOW_RELATIVE)
                continue;
            FboInstance instance = getFbo(data.getName());
            resize(instance, width, height);
        }
    }

    // Get \\

    public void request(String fboName) {
        ((InternalLoader) internalLoader).request(fboName);
    }

    public FboInstance getFbo(String name) {
        FboInstance instance = fboName2Instance.get(name);
        if (instance != null)
            return instance;

        FboData data = fboName2Data.get(name);

        if (data == null) {
            request(name);
            data = fboName2Data.get(name);
        }

        if (data == null)
            throwException("FBO not found in catalog: " + name);

        instance = internalBuilder.buildInstance(data);
        fboName2Instance.put(name, instance);

        return instance;
    }

}