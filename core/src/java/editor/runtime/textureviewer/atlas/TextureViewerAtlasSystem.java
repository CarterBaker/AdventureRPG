package editor.runtime.textureviewer.atlas;

import application.bootstrap.shaderpipeline.texturemanager.TextureManager;
import application.kernel.windowpipeline.window.WindowInstance;
import editor.bootstrap.itemeditorpipeline.itemeditormanager.ItemEditorManager;
import editor.runtime.textureviewer.TextureViewerSetting;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.vectors.Vector4;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class TextureViewerAtlasSystem extends SystemPackage {

    /*
     * Knows the item tiles: every item texture laid out in a square grid, the
     * region each occupies in the grid's own UV space and the tile ID it
     * draws. The tiles are held as one screen texture user while the window
     * is open, so all of them stream in at full detail. Each frame it fits
     * the square grid into this window and answers which texture lies under
     * a window point.
     */

    // Internal
    private ItemEditorManager itemEditorManager;
    private TextureManager textureManager;

    // Tiles
    private ObjectArrayList<String> tileNames;
    private Vector4[] tileRects;
    private Integer[] tileIDs;
    private int textureUser;

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

        int count = tileNames.size();
        int columns = Math.max(1, (int) Math.ceil(Math.sqrt(count)));
        float cell = 1f / columns;
        float gap = cell * TextureViewerSetting.TILE_GAP_SHARE;
        IntArrayList userTiles = new IntArrayList(count);

        this.tileRects = new Vector4[count];
        this.tileIDs = new Integer[TextureViewerSetting.MAX_TILES];

        for (int i = 0; i < TextureViewerSetting.MAX_TILES; i++)
            tileIDs[i] = 0;

        for (int i = 0; i < count; i++) {

            float left = (i % columns) * cell;
            float bottom = 1f - (i / columns + 1) * cell;

            tileRects[i] = new Vector4(left + gap, bottom + gap, left + cell - gap, bottom + cell - gap);
            int tileID = textureManager.getTileIDFromTextureName(tileNames.get(i));

            tileIDs[i] = tileID;
            userTiles.add(tileID);
        }

        this.textureUser = textureManager.registerScreenTextureUser(userTiles);
    }

    @Override
    protected void dispose() {
        textureManager.removeTextureUser(textureUser);
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

    public Integer[] getTileIDs() {
        return tileIDs;
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
