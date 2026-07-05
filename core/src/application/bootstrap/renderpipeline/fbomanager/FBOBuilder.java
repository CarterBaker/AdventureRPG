package application.bootstrap.renderpipeline.fbomanager;

import java.io.File;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import application.bootstrap.renderpipeline.fbo.AttachmentStruct;
import application.bootstrap.renderpipeline.fbo.FboData;
import application.bootstrap.renderpipeline.fbo.FboInstance;
import application.bootstrap.renderpipeline.fbo.FboSizingStrategy;
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
class FBOBuilder extends BuilderPackage {

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
        IntArrayList depthTextures = new IntArrayList();

        int fbo = FBOGLSLUtility.genFramebuffer();
        FBOGLSLUtility.bindFramebuffer(fbo);

        ObjectArrayList<AttachmentStruct> attachments = data.getAttachments();
        int colorIndex = 0;

        for (int i = 0; i < attachments.size(); i++) {
            AttachmentStruct attachment = attachments.get(i);
            int tex = FBOGLSLUtility.genTexture();
            FBOGLSLUtility.bindTexture(tex);

            if (attachment.isDepth()) {
                FBOGLSLUtility.texImage2DDepth(width, height);
                FBOGLSLUtility.texParameterNearest();
                FBOGLSLUtility.framebufferTexture2DDepth(tex);
                depthTextures.add(tex);
            } else {
                FBOGLSLUtility.texImage2DColor(attachment.getInternalFormat(), width, height);
                FBOGLSLUtility.texParameterLinear();
                FBOGLSLUtility.framebufferTexture2DColor(tex, colorIndex);
                textures.add(tex);
                colorIndex++;
            }

            FBOGLSLUtility.unbindTexture();
        }

        if (colorIndex > 0)
            FBOGLSLUtility.drawBuffers(colorIndex);

        if (FBOGLSLUtility.checkFramebufferStatus() != EngineSetting.GL_FRAMEBUFFER_COMPLETE)
            throwException("Framebuffer is incomplete for: " + data.getName());

        FBOGLSLUtility.unbindFramebuffer();

        framebuffers.add(fbo);

        FboInstance instance = create(FboInstance.class);
        instance.constructor(data, framebuffers, textures, depthTextures, width, height);

        return instance;
    }

    // Internal \\

    private FboData buildDataEntry(JsonObject json) {
        String name = JsonUtility.validateString(json, "name");
        FboSizingStrategy strategy = FboSizingStrategy
                .valueOf(JsonUtility.getString(json, "sizingStrategy", "WINDOW_RELATIVE"));
        int width = JsonUtility.getInt(json, "width", settings.windowWidth);
        int height = JsonUtility.getInt(json, "height", settings.windowHeight);

        ObjectArrayList<AttachmentStruct> attachments = new ObjectArrayList<>();
        JsonArray attArray = JsonUtility.validateArray(json, "attachments");

        for (int i = 0; i < attArray.size(); i++) {
            JsonObject att = attArray.get(i).getAsJsonObject();
            boolean isDepth = JsonUtility.getString(att, "type", "color").equals("depth");
            String formatName = JsonUtility.getString(att, "format", isDepth ? "DEPTH24" : "RGBA8");
            String attName = JsonUtility.getString(att, "name", "");
            attachments.add(new AttachmentStruct(attName, isDepth, resolveInternalFormat(formatName)));
        }

        return new FboData(name, attachments, strategy, width, height);
    }

    private int resolveInternalFormat(String formatName) {
        return switch (formatName) {
            case "RGBA16F" -> EngineSetting.GL_RGBA16F;
            case "RGB16F" -> EngineSetting.GL_RGB16F;
            case "RGB8" -> EngineSetting.GL_RGB8;
            case "DEPTH24" -> EngineSetting.GL_DEPTH_COMPONENT32F;
            default -> EngineSetting.GL_RGBA8;
        };
    }
}