package com.internal.bootstrap.menupipeline.fontmanager;

import com.internal.bootstrap.geometrypipeline.dynamicmodel.DynamicModelHandle;
import com.internal.bootstrap.geometrypipeline.vao.VAOHandle;
import com.internal.bootstrap.geometrypipeline.vaomanager.VAOManager;
import com.internal.bootstrap.menupipeline.fonts.FontHandle;
import com.internal.bootstrap.menupipeline.fonts.FontInstance;
import com.internal.core.engine.ManagerPackage;
import com.internal.core.engine.settings.EngineSetting;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

public class FontManager extends ManagerPackage {

    // Registry
    private Object2IntOpenHashMap<String> fontName2FontID;
    private Int2ObjectOpenHashMap<FontHandle> fontID2FontHandle;
    private int nextFontID;

    // Internal
    private VAOManager vaoManager;
    private VAOHandle labelVAOHandle;

    // Base \\

    @Override
    protected void create() {
        create(InternalLoader.class);
        this.fontName2FontID = new Object2IntOpenHashMap<>();
        this.fontID2FontHandle = new Int2ObjectOpenHashMap<>();
        this.fontName2FontID.defaultReturnValue(-1);
        this.nextFontID = 0;
    }

    @Override
    protected void get() {
        this.vaoManager = get(VAOManager.class);
    }

    @Override
    protected void dispose() {
        for (FontHandle handle : fontID2FontHandle.values())
            GLSLUtility.deleteTexture2D(handle.getGPUHandle());
        fontName2FontID.clear();
        fontID2FontHandle.clear();
    }

    // Bootstrap Registration \\

    void addFont(String fontName, FontHandle fontHandle) {
        fontName2FontID.put(fontName, nextFontID);
        fontID2FontHandle.put(nextFontID, fontHandle);
        nextFontID++;
    }

    // On-Demand Loading \\

    public void request(String fontName) {
        ((InternalLoader) internalLoader).request(fontName);
    }

    // Accessors \\

    public FontHandle getFontHandleFromFontName(String fontName) {
        if (!fontName2FontID.containsKey(fontName))
            request(fontName);
        return fontID2FontHandle.get(fontName2FontID.getInt(fontName));
    }

    public FontInstance cloneFont(String fontName) {
        FontHandle handle = getFontHandleFromFontName(fontName);

        if (labelVAOHandle == null)
            labelVAOHandle = vaoManager.getVAOHandleFromName(EngineSetting.FONT_DEFAULT_VAO);

        DynamicModelHandle mergedModel = create(DynamicModelHandle.class);
        mergedModel.constructor(handle.getMaterialID(), labelVAOHandle);

        FontInstance instance = create(FontInstance.class);
        instance.constructor(handle, mergedModel);
        return instance;
    }
}