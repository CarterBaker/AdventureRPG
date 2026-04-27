package application.bootstrap.renderpipeline.fbo;

import engine.graphics.gl.GL20;
import engine.root.EngineContext;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.lwjgl.opengl.GL30C;

public class FboManager extends ManagerPackage {

    private InternalBuilder internalBuilder;

    private Object2ObjectOpenHashMap<String, FboData> fboName2Data;
    private Object2ObjectOpenHashMap<String, FboInstance> fboName2Instance;
    private ObjectArrayList<FboData> orderedFboData;

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

        fbo.getHandle().setSize(width, height);
    }

    public FboInstance getFbo(String name) {
        FboInstance instance = fboName2Instance.get(name);

        if (instance != null)
            return instance;

        FboData data = fboName2Data.get(name);

        if (data == null)
            throwException("FBO not found in catalog: " + name);

        FboHandle handle = internalBuilder.buildHandle(data);

        instance = create(FboInstance.class);
        instance.constructor(handle);
        fboName2Instance.put(name, instance);

        return instance;
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
            if (data.getSizingStrategy() != FboData.SizingStrategy.WINDOW_RELATIVE)
                continue;

            FboInstance instance = getFbo(data.getName());
            resize(instance, width, height);
        }
    }
}
