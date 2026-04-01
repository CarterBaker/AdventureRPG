package program.bootstrap.menupipeline.fontmanager;

import program.bootstrap.geometrypipeline.dynamicmodel.DynamicModelHandle;
import program.bootstrap.geometrypipeline.vao.VAOHandle;
import program.bootstrap.geometrypipeline.vaomanager.VAOManager;
import program.bootstrap.menupipeline.fonts.FontHandle;
import program.bootstrap.menupipeline.fonts.FontInstance;
import program.core.engine.ManagerPackage;
import program.core.settings.EngineSetting;
import program.core.util.RegistryUtility;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

public class FontManager extends ManagerPackage {

    /*
     * Owns the font palette for the engine lifetime. Drives font rasterization
     * and atlas upload via InternalLoader. Provides FontInstance cloning for
     * per-label use. Releases all GPU texture handles on dispose.
     */

    // Internal
    private VAOManager vaoManager;
    private VAOHandle labelVAOHandle;

    // Palette
    private Object2IntOpenHashMap<String> fontName2FontID;
    private Int2ObjectOpenHashMap<FontHandle> fontID2FontHandle;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.fontName2FontID = new Object2IntOpenHashMap<>();
        this.fontID2FontHandle = new Int2ObjectOpenHashMap<>();
        this.fontName2FontID.defaultReturnValue(-1);
        create(InternalLoader.class);
    }

    @Override
    protected void get() {

        // Internal
        this.vaoManager = get(VAOManager.class);
    }

    @Override
    protected void dispose() {

        for (FontHandle handle : fontID2FontHandle.values())
            GLSLUtility.deleteTexture2D(handle.getGPUHandle());

        fontName2FontID.clear();
        fontID2FontHandle.clear();
    }

    // Management \\

    void addFont(String fontName, FontHandle fontHandle) {

        int id = RegistryUtility.toIntID(fontName);

        fontName2FontID.put(fontName, id);
        fontID2FontHandle.put(id, fontHandle);
    }

    // Accessible \\

    public boolean hasFont(String fontName) {
        return fontName2FontID.containsKey(fontName);
    }

    public int getFontIDFromFontName(String fontName) {

        if (!fontName2FontID.containsKey(fontName))
            request(fontName);

        return fontName2FontID.getInt(fontName);
    }

    public FontHandle getFontHandleFromFontID(int fontID) {

        FontHandle handle = fontID2FontHandle.get(fontID);

        if (handle == null)
            throwException("Font ID not found: " + fontID);

        return handle;
    }

    public FontHandle getFontHandleFromFontName(String fontName) {
        return getFontHandleFromFontID(getFontIDFromFontName(fontName));
    }

    public FontInstance cloneFont(String fontName) {

        FontHandle handle = getFontHandleFromFontName(fontName);

        if (labelVAOHandle == null)
            labelVAOHandle = vaoManager.getVAOHandleFromVAOName(EngineSetting.FONT_DEFAULT_VAO);

        DynamicModelHandle mergedModel = create(DynamicModelHandle.class);
        mergedModel.constructor(handle.getMaterialID(), labelVAOHandle);

        FontInstance instance = create(FontInstance.class);
        instance.constructor(handle, mergedModel);

        return instance;
    }

    public void request(String fontName) {
        ((InternalLoader) internalLoader).request(fontName);
    }
}