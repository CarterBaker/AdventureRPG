package application.bootstrap.renderpipeline.fbomanager;

import java.io.File;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import application.bootstrap.renderpipeline.fbo.FboData;
import application.bootstrap.renderpipeline.fbo.FboInstance;
import application.bootstrap.renderpipeline.fbo.FboSizingStrategy;
import engine.graphics.gl.GL20;
import engine.root.BuilderPackage;
import engine.root.EngineContext;
import engine.root.EngineSetting;
import engine.util.io.JsonUtility;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.lwjgl.opengl.GL30C;

/*
 * Parses FBO JSON descriptors into FboData during bootstrap, and constructs
 * GL-backed FboInstances on demand when getFbo() resolves a name for the
 * first time. GL allocation and framebuffer completeness checks happen here
 * so FboManager stays free of raw GL calls.
 */
class InternalBuilder extends BuilderPackage {

    // Build \\

    ObjectArrayList<FboData> buildData(File file) {
        JsonObject root = JsonUtility.loadJsonObject(file);
        JsonArray list = root.has("fbos") ? JsonUtility.validateArray(root, "fbos") : new JsonArray();

        if (list.size() == 0 && root.has("name"))
            list.add(root);

        ObjectArrayList<FboData> dataList = new ObjectArrayList<>();

        for (int i = 0; i < list.size(); i++) {
            JsonObject json = list.get(i).getAsJsonObject();
            dataList.add(buildDataEntry(json));
        }

        return dataList;
    }

    FboInstance buildInstance(FboData data) {
        int width = data.getSizingStrategy() == FboSizingStrategy.FIXED ? data.getWidth() : settings.windowWidth;
        int height = data.getSizingStrategy() == FboSizingStrategy.FIXED ? data.getHeight() : settings.windowHeight;

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

    // Internal \\

    private FboData buildDataEntry(JsonObject json) {
        String name = JsonUtility.validateString(json, "name");
        String formatName = JsonUtility.getString(json, "format", "RGBA8");
        boolean depth = JsonUtility.getBoolean(json, "depth", true);
        FboSizingStrategy strategy = FboSizingStrategy
                .valueOf(JsonUtility.getString(json, "sizingStrategy", "WINDOW_RELATIVE"));
        int width = JsonUtility.getInt(json, "width", settings.windowWidth);
        int height = JsonUtility.getInt(json, "height", settings.windowHeight);
        int format = resolveInternalFormat(formatName);

        return new FboData(name, format, depth, strategy, width, height);
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