package application.bootstrap.renderpipeline.fbo;

import java.io.File;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import engine.graphics.gl.GL20;
import engine.root.EngineContext;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.io.JsonUtility;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.lwjgl.opengl.GL30C;

public class FboManager extends ManagerPackage {

    private Object2ObjectOpenHashMap<String, FboInstance> fboName2Handle;
    private ObjectArrayList<FboInstance> orderedFbos;

    @Override
    protected void create() {
        this.fboName2Handle = new Object2ObjectOpenHashMap<>();
        this.orderedFbos = new ObjectArrayList<>();
    }

    @Override
    protected void awake() {
        if (settings.fboCatalogJsonPath == null || settings.fboCatalogJsonPath.isBlank())
            return;

        File file = new File(settings.fboCatalogJsonPath);
        if (!file.exists())
            return;

        JsonObject root = JsonUtility.loadJsonObject(file);
        JsonArray list = root.has("fbos") ? JsonUtility.validateArray(root, "fbos") : new JsonArray();

        if (list.size() == 0 && root.has("name"))
            list.add(root);

        for (int i = 0; i < list.size(); i++) {
            JsonObject json = list.get(i).getAsJsonObject();
            FboData data = buildData(json);
            FboInstance instance = createInstance(data);
            fboName2Handle.put(data.getName(), instance);
            orderedFbos.add(instance);
        }

    }

    private FboData buildData(JsonObject json) {
        String name = JsonUtility.validateString(json, "name");
        String formatName = JsonUtility.getString(json, "format", "RGBA8");
        boolean depth = JsonUtility.getBoolean(json, "depth", true);
        FboData.SizingStrategy strategy = FboData.SizingStrategy
                .valueOf(JsonUtility.getString(json, "sizingStrategy", "WINDOW_RELATIVE"));
        int width = JsonUtility.getInt(json, "width", settings.windowWidth);
        int height = JsonUtility.getInt(json, "height", settings.windowHeight);
        int format = resolveInternalFormat(formatName);

        return new FboData(name, format, depth, strategy, width, height);
    }

    private FboInstance createInstance(FboData data) {
        int width = data.getSizingStrategy() == FboData.SizingStrategy.FIXED ? data.getWidth() : settings.windowWidth;
        int height = data.getSizingStrategy() == FboData.SizingStrategy.FIXED ? data.getHeight() : settings.windowHeight;

        IntArrayList framebuffers = new IntArrayList();
        IntArrayList textures = new IntArrayList();
        IntArrayList depthRenderbuffers = new IntArrayList();

        int fbo = GL30C.glGenFramebuffers();
        int texture = EngineContext.gl20.glGenTexture();

        GL30C.glBindFramebuffer(GL30C.GL_FRAMEBUFFER, fbo);
        EngineContext.gl20.glBindTexture(GL20.GL_TEXTURE_2D, texture);
        EngineContext.gl20.glTexImage2D(GL20.GL_TEXTURE_2D, 0, data.getFormat(), width, height, 0, GL20.GL_RGBA,
                GL20.GL_UNSIGNED_BYTE, null);
        EngineContext.gl20.glTexParameteri(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_MIN_FILTER, GL20.GL_LINEAR);
        EngineContext.gl20.glTexParameteri(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_MAG_FILTER, GL20.GL_LINEAR);
        EngineContext.gl20.glTexParameteri(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_WRAP_S, GL20.GL_CLAMP_TO_EDGE);
        EngineContext.gl20.glTexParameteri(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_WRAP_T, GL20.GL_CLAMP_TO_EDGE);

        GL30C.glFramebufferTexture2D(GL30C.GL_FRAMEBUFFER, GL30C.GL_COLOR_ATTACHMENT0, GL20.GL_TEXTURE_2D, texture, 0);

        int depthBuffer = 0;
        if (data.hasDepth()) {
            depthBuffer = GL30C.glGenRenderbuffers();
            GL30C.glBindRenderbuffer(GL30C.GL_RENDERBUFFER, depthBuffer);
            GL30C.glRenderbufferStorage(GL30C.GL_RENDERBUFFER, GL30C.GL_DEPTH_COMPONENT24, width, height);
            GL30C.glFramebufferRenderbuffer(GL30C.GL_FRAMEBUFFER, GL30C.GL_DEPTH_ATTACHMENT, GL30C.GL_RENDERBUFFER,
                    depthBuffer);
            GL30C.glBindRenderbuffer(GL30C.GL_RENDERBUFFER, 0);
        }

        if (GL30C.glCheckFramebufferStatus(GL30C.GL_FRAMEBUFFER) != GL30C.GL_FRAMEBUFFER_COMPLETE)
            throwException("Framebuffer is incomplete for: " + data.getName());

        GL30C.glBindFramebuffer(GL30C.GL_FRAMEBUFFER, 0);
        EngineContext.gl20.glBindTexture(GL20.GL_TEXTURE_2D, EngineSetting.GL_HANDLE_NONE);

        framebuffers.add(fbo);
        textures.add(texture);
        if (depthBuffer != 0)
            depthRenderbuffers.add(depthBuffer);

        FboInstance instance = create(FboInstance.class);
        instance.constructor(data, framebuffers, textures, depthRenderbuffers, width, height);
        return instance;
    }

    public void bind(FboInstance fbo) {
        IntArrayList framebuffers = fbo.getFramebuffers();
        if (framebuffers.isEmpty()) {
            unbind();
            return;
        }

        GL30C.glBindFramebuffer(GL30C.GL_FRAMEBUFFER, framebuffers.getInt(0));
        EngineContext.gl20.glViewport(0, 0, fbo.getWidth(), fbo.getHeight());
    }

    public void unbind() {
        GL30C.glBindFramebuffer(GL30C.GL_FRAMEBUFFER, 0);
    }

    public void resize(FboInstance fbo, int width, int height) {
        if (width <= 0 || height <= 0)
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

    public FboInstance getFbo(String name) {
        return fboName2Handle.get(name);
    }


    public void resize(String name, int width, int height) {
        FboInstance fbo = fboName2Handle.get(name);
        if (fbo != null)
            resize(fbo, width, height);
    }

    public void resizeWindowRelative(int width, int height) {
        Object[] elements = orderedFbos.elements();
        int count = orderedFbos.size();

        for (int i = 0; i < count; i++) {
            FboInstance fbo = (FboInstance) elements[i];
            if (fbo.getFboData().getSizingStrategy() == FboData.SizingStrategy.WINDOW_RELATIVE)
                resize(fbo, width, height);
        }
    }

    private int resolveInternalFormat(String formatName) {
        return switch (formatName) {
            case "RGBA16F" -> GL30C.GL_RGBA16F;
            case "RGB8" -> GL30C.GL_RGB8;
            case "RGBA8" -> GL20.GL_RGBA8;
            default -> GL20.GL_RGBA8;
        };
    }
}
