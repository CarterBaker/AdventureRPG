package application.bootstrap.renderpipeline.fbo;

import java.io.File;

import engine.graphics.gl.GL20;
import engine.root.EngineContext;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.lwjgl.opengl.GL30C;

public class FboManager extends ManagerPackage {

    private InternalBuilder internalBuilder;

    private Object2ObjectOpenHashMap<String, FboHandle> fboName2Handle;
    private Object2ObjectOpenHashMap<String, FboInstance> fboName2Instance;
    private ObjectArrayList<FboHandle> orderedFbos;

    @Override
    protected void create() {
        this.fboName2Handle = new Object2ObjectOpenHashMap<>();
        this.fboName2Instance = new Object2ObjectOpenHashMap<>();
        this.orderedFbos = new ObjectArrayList<>();

        this.internalBuilder = create(InternalBuilder.class);
        create(InternalLoader.class);
    }

    void addFboHandle(FboHandle handle) {
        String name = handle.getData().getName();

        if (fboName2Handle.containsKey(name))
            return;

        fboName2Handle.put(name, handle);
        orderedFbos.add(handle);
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

        FboHandle handle = fboName2Handle.get(name);

        if (handle == null)
            handle = buildMissingFbo(name);

        if (handle == null)
            throwException("FBO not found in catalog: " + name);

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
        Object[] elements = orderedFbos.elements();
        int count = orderedFbos.size();

        for (int i = 0; i < count; i++) {
            FboHandle handle = (FboHandle) elements[i];
            if (handle.getData().getSizingStrategy() != FboData.SizingStrategy.WINDOW_RELATIVE)
                continue;

            FboInstance instance = getFbo(handle.getData().getName());
            resize(instance, width, height);
        }
    }

    private FboHandle buildMissingFbo(String name) {
        File file = new File(EngineSetting.FBO_CATALOG_JSON_PATH);

        if (!file.exists())
            return null;

        FboHandle handle = internalBuilder.buildByName(file, name);

        if (handle == null)
            return null;

        addFboHandle(handle);
        return handle;
    }
}
