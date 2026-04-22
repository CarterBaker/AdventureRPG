package application.bootstrap.menupipeline.fontmanager;

import application.bootstrap.menupipeline.font.FontHandle;
import application.bootstrap.menupipeline.font.FontInstance;
import application.bootstrap.shaderpipeline.material.MaterialInstance;
import application.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import engine.root.ManagerPackage;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

public class FontManager extends ManagerPackage {

    /*
     * Owns the font palette for the engine lifetime. Drives font rasterization
     * and atlas registration via InternalLoader. GPU resources are owned and
     * disposed by TextureManager — FontManager holds no GPU state directly.
     */

    // Internal
    private MaterialManager materialManager;

    // Palette
    private Object2IntOpenHashMap<String> fontName2FontID;
    private Int2ObjectOpenHashMap<FontHandle> fontID2FontHandle;

    // Base \\

    @Override
    protected void create() {

        this.fontName2FontID = new Object2IntOpenHashMap<>();
        this.fontID2FontHandle = new Int2ObjectOpenHashMap<>();
        this.fontName2FontID.defaultReturnValue(-1);

        create(InternalLoader.class);
    }

    @Override
    protected void get() {
        this.materialManager = get(MaterialManager.class);
    }

    @Override
    protected void dispose() {
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
        return cloneFont(fontName, null);
    }

    public FontInstance cloneFont(String fontName, String materialNameOverride) {

        FontHandle handle = getFontHandleFromFontName(fontName);

        int materialID = materialNameOverride != null
                ? materialManager.getMaterialIDFromMaterialName(materialNameOverride)
                : handle.getMaterialID();

        MaterialInstance material = materialManager.cloneMaterial(materialID);

        if (material.getUniform("u_fontAtlas") != null)
            material.setUniform("u_fontAtlas", handle.getAtlasHandle().getGpuHandle());

        FontInstance instance = create(FontInstance.class);
        instance.constructor(handle, material);

        return instance;
    }

    public void request(String fontName) {
        ((InternalLoader) internalLoader).request(fontName);
    }
}
