package editor.textureviewer.atlas;

import application.bootstrap.shaderpipeline.texture.TextureHandle;
import application.bootstrap.shaderpipeline.texturemanager.TextureManager;
import application.kernel.windowpipeline.window.WindowInstance;
import editor.bootstrap.itemeditorpipeline.itemeditormanager.ItemEditorManager;
import editor.textureviewer.TextureViewerSetting;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.vectors.Vector4;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class TextureViewerAtlasSystem extends SystemPackage {

    /*
     * Knows the item atlas: every source image packed into it and the region
     * each occupies, in atlas UV space. Each frame it fits the square atlas
     * into this window and answers which image lies under a window point.
     */

    // Internal
    private ItemEditorManager itemEditorManager;
    private TextureManager textureManager;

    // Tiles
    private ObjectArrayList<String> tileNames;
    private Vector4[] tileRects;

    // Layout
    private float atlasX;
    private float atlasY;
    private float atlasSize;

    // Base \\

    @Override
    protected void get() {
        this.itemEditorManager = get(ItemEditorManager.class);
        this.textureManager = get(TextureManager.class);
    }

    @Override
    protected void awake() {

        this.tileNames = new ObjectArrayList<>(itemEditorManager.getTextureNames());

        if (tileNames.size() > TextureViewerSetting.MAX_TILES)
            throwException("Item atlas holds " + tileNames.size() + " textures; the texture viewer shows at most "
                    + TextureViewerSetting.MAX_TILES + ".");

        this.tileRects = new Vector4[tileNames.size()];

        for (int i = 0; i < tileNames.size(); i++) {
            TextureHandle handle = textureManager.getTextureHandleFromTextureName(tileNames.get(i));
            tileRects[i] = new Vector4(handle.getU0(), handle.getV0(), handle.getU1(), handle.getV1());
        }
    }

    // Update \\

    @Override
    protected void update() {

        WindowInstance window = context.getWindow();
        float width = window.getWidth();
        float height = window.getHeight();

        atlasSize = Math.max(0f, Math.min(width, height) - TextureViewerSetting.VIEW_MARGIN_PIXELS * 2f);
        atlasX = (width - atlasSize) * 0.5f;
        atlasY = (height - atlasSize) * 0.5f;
    }

    // Accessible \\

    public int getTileAt(float x, float y) {

        if (atlasSize <= 0f)
            return EngineSetting.INDEX_NOT_FOUND;

        float u = (x - atlasX) / atlasSize;
        float v = (y - atlasY) / atlasSize;

        for (int i = 0; i < tileRects.length; i++) {
            Vector4 rect = tileRects[i];
            if (u >= rect.x && v >= rect.y && u <= rect.z && v <= rect.w)
                return i;
        }

        return EngineSetting.INDEX_NOT_FOUND;
    }

    public int indexOfTile(String textureName) {
        return textureName != null ? tileNames.indexOf(textureName) : EngineSetting.INDEX_NOT_FOUND;
    }

    public String getTileName(int tileIndex) {
        return tileNames.get(tileIndex);
    }

    public Vector4[] getTileRects() {
        return tileRects;
    }

    public float getAtlasX() {
        return atlasX;
    }

    public float getAtlasY() {
        return atlasY;
    }

    public float getAtlasSize() {
        return atlasSize;
    }
}
