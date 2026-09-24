package editor.textureviewer.select;

import application.kernel.inputpipeline.input.RawInputHandle;
import editor.bootstrap.itemeditorpipeline.itemeditormanager.ItemEditorManager;
import editor.textureviewer.TextureViewerSetting;
import editor.textureviewer.atlas.TextureViewerAtlasSystem;
import engine.editor.EditorInputSystem;
import engine.root.EngineSetting;
import engine.root.SystemPackage;

public class TextureViewerSelectSystem extends SystemPackage {

    /*
     * Tracks which atlas image is under the pointer and, on click, makes it
     * the item editor's brush texture.
     */

    // Internal
    private EditorInputSystem editorInputSystem;
    private TextureViewerAtlasSystem textureViewerAtlasSystem;
    private ItemEditorManager itemEditorManager;

    // Hover
    private int hoveredTile;

    // Base \\

    @Override
    protected void create() {
        this.hoveredTile = EngineSetting.INDEX_NOT_FOUND;
    }

    @Override
    protected void get() {
        this.editorInputSystem = get(EditorInputSystem.class);
        this.textureViewerAtlasSystem = get(TextureViewerAtlasSystem.class);
        this.itemEditorManager = get(ItemEditorManager.class);
    }

    // Update \\

    @Override
    protected void update() {

        hoveredTile = EngineSetting.INDEX_NOT_FOUND;

        if (!editorInputSystem.isPointerActive())
            return;

        RawInputHandle rawInput = editorInputSystem.getRawInputHandle();
        hoveredTile = textureViewerAtlasSystem.getTileAt(rawInput.getMouseX(), rawInput.getMouseY());

        if (hoveredTile != EngineSetting.INDEX_NOT_FOUND
                && editorInputSystem.isClicked(TextureViewerSetting.BUTTON_SELECT))
            itemEditorManager.selectBrushTexture(textureViewerAtlasSystem.getTileName(hoveredTile));
    }

    // Accessible \\

    public int getHoveredTile() {
        return hoveredTile;
    }
}
