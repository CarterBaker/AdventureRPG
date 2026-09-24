package editor.textureviewer.menu;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.renderpipeline.fbomanager.FboManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import editor.bootstrap.itemeditorpipeline.itemeditormanager.ItemEditorManager;
import editor.textureviewer.TextureViewerSetting;
import editor.textureviewer.atlas.TextureViewerAtlasSystem;
import editor.textureviewer.select.TextureViewerSelectSystem;
import engine.editor.EditorSetting;
import engine.root.EngineSetting;
import engine.root.SystemPackage;

public class TextureViewerMenuSystem extends SystemPackage {

    /*
     * Opens the status bar over the atlas. It names the image under the
     * pointer, otherwise the current brush, and rewrites only on change.
     */

    // Internal
    private MenuManager menuManager;
    private FboManager fboManager;
    private ItemEditorManager itemEditorManager;
    private TextureViewerAtlasSystem textureViewerAtlasSystem;
    private TextureViewerSelectSystem textureViewerSelectSystem;

    // Menus
    private MenuInstance statusMenu;

    // Status
    private String shownStatus;

    // Base \\

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.fboManager = get(FboManager.class);
        this.itemEditorManager = get(ItemEditorManager.class);
        this.textureViewerAtlasSystem = get(TextureViewerAtlasSystem.class);
        this.textureViewerSelectSystem = get(TextureViewerSelectSystem.class);
    }

    @Override
    protected void awake() {

        WindowInstance window = context.getWindow();

        menuManager.setMenuTargetFbo(window, fboManager.cloneFbo(RuntimeSetting.FBO_UI, window));
        this.statusMenu = menuManager.openMenu(TextureViewerSetting.MENU_STATUS, window);
    }

    @Override
    protected void dispose() {

        menuManager.closeMenu(statusMenu);
        menuManager.setMenuTargetFbo(context.getWindow(), null);
    }

    // Update \\

    @Override
    protected void update() {

        String status = resolveStatus();

        if (status.equals(shownStatus))
            return;

        shownStatus = status;

        ElementInstance statusLabel = statusMenu.getEntryPoint(TextureViewerSetting.ENTRY_STATUS);

        if (statusLabel != null)
            statusLabel.setFontText(status);
    }

    private String resolveStatus() {

        int hoveredTile = textureViewerSelectSystem.getHoveredTile();

        if (hoveredTile != EngineSetting.INDEX_NOT_FOUND)
            return EditorSetting.TEXTURE_VIEWER_STATUS_HOVERED + textureViewerAtlasSystem.getTileName(hoveredTile);

        String brushTextureName = itemEditorManager.getBrushTextureName();

        return brushTextureName != null
                ? EditorSetting.ITEM_EDITOR_STATUS_BRUSH + brushTextureName
                : EditorSetting.TEXTURE_VIEWER_STATUS_NO_BRUSH;
    }
}
