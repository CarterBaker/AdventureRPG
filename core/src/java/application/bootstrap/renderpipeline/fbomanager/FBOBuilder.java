package application.bootstrap.renderpipeline.fbomanager;

import java.io.File;

import application.bootstrap.renderpipeline.fbo.AttachmentStruct;
import application.bootstrap.renderpipeline.fbo.FBOData;
import application.bootstrap.renderpipeline.fbo.FBOInstance;
import application.bootstrap.renderpipeline.fbo.FBOSizingStrategy;
import engine.graphics.color.Color;
import engine.root.BuilderPackage;
import engine.root.EngineSetting;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class FBOBuilder extends BuilderPackage {

    /*
     * Parses FBO ARPG descriptors into FBOData during bootstrap, and constructs
     * GL-backed FBOInstances on demand when getFbo() resolves a name for the
     * first time. GL allocation and framebuffer completeness checks happen here
     * so FBOManager stays free of raw GL calls.
     */

    // Build \\

    ObjectArrayList<FBOData> buildData(File file) {
        ArpgObjectStruct root = ArpgUtility.loadObject(file);
        ArpgArrayStruct list = root.has("fbos") ? ArpgUtility.validateArray(root, "fbos") : new ArpgArrayStruct();

        if (list.size() == 0 && root.has("name"))
            list.add(root);

        ObjectArrayList<FBOData> dataList = new ObjectArrayList<>();

        for (int i = 0; i < list.size(); i++)
            dataList.add(buildDataEntry(list.get(i).getAsObject()));

        return dataList;
    }

    FBOInstance buildInstance(FBOData data) {
        int width = data.getSizingStrategy() == FBOSizingStrategy.FIXED
                ? data.getWidth()
                : data.scaleWindowDimension(settings.windowWidth);
        int height = data.getSizingStrategy() == FBOSizingStrategy.FIXED
                ? data.getHeight()
                : data.scaleWindowDimension(settings.windowHeight);

        IntArrayList framebuffers = new IntArrayList();
        IntArrayList textures = new IntArrayList();
        IntArrayList depthTextures = new IntArrayList();

        ObjectArrayList<AttachmentStruct> attachments = data.getAttachments();

        for (int i = 0; i < attachments.size(); i++) {
            AttachmentStruct attachment = attachments.get(i);
            int tex = FBOGLSLUtility.genTexture();
            FBOGLSLUtility.bindTexture(tex);

            if (attachment.isDepth()) {
                FBOGLSLUtility.texImage2DDepth(width, height);
                FBOGLSLUtility.texParameterNearest();
                depthTextures.add(tex);
            } else {
                FBOGLSLUtility.texImage2DColor(attachment.getInternalFormat(), width, height);
                FBOGLSLUtility.texParameterLinear();
                textures.add(tex);
            }

            FBOGLSLUtility.unbindTexture();
        }

        framebuffers.add(buildFramebuffer(data, textures, depthTextures));

        FBOInstance instance = create(FBOInstance.class);
        instance.constructor(data, framebuffers, textures, depthTextures, width, height);

        return instance;
    }

    int buildFramebuffer(FBOData data, IntArrayList textures, IntArrayList depthTextures) {

        int fbo = FBOGLSLUtility.genFramebuffer();
        FBOGLSLUtility.bindFramebuffer(fbo);

        ObjectArrayList<AttachmentStruct> attachments = data.getAttachments();
        int colorIndex = 0;
        int depthIndex = 0;

        for (int i = 0; i < attachments.size(); i++) {

            if (attachments.get(i).isDepth()) {
                FBOGLSLUtility.framebufferTexture2DDepth(depthTextures.getInt(depthIndex));
                depthIndex++;
            } else {
                FBOGLSLUtility.framebufferTexture2DColor(textures.getInt(colorIndex), colorIndex);
                colorIndex++;
            }
        }

        if (colorIndex > 0)
            FBOGLSLUtility.drawBuffers(colorIndex);

        if (FBOGLSLUtility.checkFramebufferStatus() != EngineSetting.GL_FRAMEBUFFER_COMPLETE)
            throwException("Framebuffer is incomplete for: " + data.getName());

        FBOGLSLUtility.unbindFramebuffer();

        return fbo;
    }

    // Internal \\

    private FBOData buildDataEntry(ArpgObjectStruct arpg) {
        String name = ArpgUtility.validateString(arpg, "name");
        FBOSizingStrategy strategy = FBOSizingStrategy
                .valueOf(ArpgUtility.getString(arpg, "sizingStrategy", "WINDOW_RELATIVE"));
        int width = ArpgUtility.getInt(arpg, "width", settings.windowWidth);
        int height = ArpgUtility.getInt(arpg, "height", settings.windowHeight);
        boolean premultipliedBlend = arpg.has("premultipliedBlend") && arpg.get("premultipliedBlend").getAsBoolean();
        boolean premultipliedBlit = ArpgUtility.getBoolean(arpg, "premultipliedBlit", false);
        boolean resolveBlit = ArpgUtility.getBoolean(arpg, "resolveBlit", false);
        Color clearColor = parseClearColor(arpg);
        float resolutionScale = ArpgUtility.getFloat(
                arpg, "resolutionScale", EngineSetting.DEFAULT_FBO_RESOLUTION_SCALE);

        if (resolutionScale <= 0f || resolutionScale > 1f)
            throwException("FBO \"" + name + "\" resolutionScale must be greater than 0.0 and at most 1.0, got: "
                    + resolutionScale);

        ObjectArrayList<AttachmentStruct> attachments = new ObjectArrayList<>();
        ArpgArrayStruct attArray = ArpgUtility.validateArray(arpg, "attachments");

        for (int i = 0; i < attArray.size(); i++) {
            ArpgObjectStruct att = attArray.get(i).getAsObject();
            boolean isDepth = ArpgUtility.getString(att, "type", "color").equals("depth");
            String formatName = ArpgUtility.getString(att, "format", isDepth ? "DEPTH24" : "RGBA8");
            String attName = ArpgUtility.getString(att, "name", "");
            attachments.add(new AttachmentStruct(attName, isDepth, resolveInternalFormat(formatName)));
        }

        return new FBOData(name, attachments, strategy, width, height, premultipliedBlend, premultipliedBlit,
                resolveBlit, clearColor, resolutionScale);
    }

    private Color parseClearColor(ArpgObjectStruct arpg) {

        if (!arpg.has("clearColor"))
            return new Color(Color.CLEAR);

        ArpgArrayStruct clearColor = ArpgUtility.validateArray(arpg, "clearColor", 4);

        return new Color(
                clearColor.get(0).getAsFloat(),
                clearColor.get(1).getAsFloat(),
                clearColor.get(2).getAsFloat(),
                clearColor.get(3).getAsFloat());
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