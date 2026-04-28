package application.bootstrap.renderpipeline.fbomanager;

import java.io.File;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import application.bootstrap.renderpipeline.fbo.FboData;
import application.bootstrap.renderpipeline.fbo.FboInstance;
import application.bootstrap.renderpipeline.fbo.FboSizingStrategy;
import engine.graphics.gl.GL20;
import engine.graphics.gl.GL30;
import engine.root.BuilderPackage;
import engine.root.EngineSetting;
import engine.util.io.JsonUtility;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

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

        for (int i = 0; i < list.size(); i++)
            dataList.add(buildDataEntry(list.get(i).getAsJsonObject()));

        return dataList;
    }

    FboInstance buildInstance(FboData data) {
        int width = data.getSizingStrategy() == FboSizingStrategy.FIXED ? data.getWidth() : settings.windowWidth;
        int height = data.getSizingStrategy() == FboSizingStrategy.FIXED ? data.getHeight() : settings.windowHeight;

        IntArrayList framebuffers = new IntArrayList();
        IntArrayList textures = new IntArrayList();
        IntArrayList depthRenderbuffers = new IntArrayList();

        int fbo = GLSLUtility.genFramebuffer();
        int texture = GLSLUtility.genTexture();

        GLSLUtility.bindFramebuffer(fbo);
        GLSLUtility.bindTexture(texture);
        GLSLUtility.texImage2D(data.getFormat(), width, height);
        GLSLUtility.texParameterLinear();
        GLSLUtility.framebufferTexture2D(texture);

        int depthBuffer = EngineSetting.GL_HANDLE_NONE;

        if (data.hasDepth()) {
            depthBuffer = GLSLUtility.genRenderbuffer();
            GLSLUtility.bindRenderbuffer(depthBuffer);
            GLSLUtility.renderbufferStorage(GL30.GL_DEPTH_COMPONENT24, width, height);
            GLSLUtility.framebufferRenderbuffer(depthBuffer);
            GLSLUtility.unbindRenderbuffer();
        }

        if (GLSLUtility.checkFramebufferStatus() != GL30.GL_FRAMEBUFFER_COMPLETE)
            throwException("Framebuffer is incomplete for: " + data.getName());

        GLSLUtility.unbindFramebuffer();
        GLSLUtility.unbindTexture();

        framebuffers.add(fbo);
        textures.add(texture);

        if (depthBuffer != EngineSetting.GL_HANDLE_NONE)
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
            case "RGBA16F" -> GL30.GL_RGBA16F;
            case "RGB8" -> GL30.GL_RGB8;
            default -> GL20.GL_RGBA8;
        };
    }
}