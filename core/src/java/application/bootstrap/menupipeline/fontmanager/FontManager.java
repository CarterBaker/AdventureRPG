package application.bootstrap.menupipeline.fontmanager;

import application.bootstrap.menupipeline.font.FontHandle;
import application.bootstrap.menupipeline.font.FontInstance;
import application.bootstrap.shaderpipeline.material.MaterialInstance;
import application.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class FontManager extends ManagerPackage {

    /*
     * Owns the font palette for the engine lifetime. Drives font rasterization
     * and atlas registration via FontLoader, with font IDs assigned in
     * registration order. GPU resources are owned and disposed by
     * TextureManager — FontManager holds no GPU state directly.
     */

    // Internal
    private MaterialManager materialManager;

    // Palette
    private Object2IntOpenHashMap<String> fontName2FontID;
    private ObjectArrayList<FontHandle> fontID2FontHandle;

    // Base \\

    @Override
    protected void create() {

        this.fontName2FontID = RegistryUtility.createNameIndex();
        this.fontID2FontHandle = RegistryUtility.createPalette();

        create(FontLoader.class);
    }

    @Override
    protected void get() {
        this.materialManager = get(MaterialManager.class);
    }

    @Override
    protected void dispose() {
        RegistryUtility.clearPalette(fontName2FontID, fontID2FontHandle);
    }

    // Management \\

    void addFont(String fontName, FontHandle fontHandle) {
        RegistryUtility.registerHandle(
                fontName2FontID, fontID2FontHandle, fontName, fontHandle, EngineSetting.REGISTRY_INT_ID_COUNT);
    }

    // Accessible \\

    public boolean hasFont(String fontName) {
        return RegistryUtility.getHandle(fontName2FontID, fontID2FontHandle, fontName) != null;
    }

    public int getFontIDFromFontName(String fontName) {

        if (!hasFont(fontName))
            request(fontName);

        return fontName2FontID.getInt(fontName);
    }

    public FontHandle getFontHandleFromFontID(int fontID) {

        FontHandle handle = RegistryUtility.getHandle(fontID2FontHandle, fontID);

        if (handle == null)
            throwException("Font ID not found: " + fontID);

        return handle;
    }

    public FontHandle getFontHandleFromFontName(String fontName) {
        return getFontHandleFromFontID(getFontIDFromFontName(fontName));
    }

    public FontInstance cloneFont(String fontName) {
        return cloneFont(fontName, null);
    }

    public FontInstance cloneFont(String fontName, String materialNameOverride) {

        FontHandle handle = getFontHandleFromFontName(fontName);

        int materialID = materialNameOverride != null
                ? materialManager.getMaterialIDFromMaterialName(materialNameOverride)
                : handle.getMaterialID();

        MaterialInstance material = materialManager.cloneMaterial(materialID);

        if (material.getUniform(EngineSetting.UNIFORM_FONT_ATLAS) != null)
            material.setUniform(EngineSetting.UNIFORM_FONT_ATLAS, handle.getAtlasHandle().getGpuHandle());

        FontInstance instance = create(FontInstance.class);
        instance.constructor(handle, material);

        return instance;
    }

    public void request(String fontName) {
        ((FontLoader) internalLoader).request(fontName);
    }
}
