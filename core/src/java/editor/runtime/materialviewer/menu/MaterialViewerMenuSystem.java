package editor.runtime.materialviewer.menu;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import editor.bootstrap.materialspipeline.materialsmanager.MaterialsManager;
import editor.bootstrap.toolpipeline.toolmanager.ToolManager;
import editor.bootstrap.toolpipeline.util.ToolCanvas;
import editor.bootstrap.toolpipeline.util.ToolOption;
import editor.runtime.EditorSetting;
import editor.runtime.characterview.util.CharacterViewUtility;
import editor.runtime.materialviewer.MaterialViewerSetting;
import engine.root.EngineSetting;
import engine.root.SystemPackage;

public class MaterialViewerMenuSystem extends SystemPackage {

    /*
     * Opens the Material Viewer's status line over the viewport. It names the
     * material shown and the shape it is shown on, then what the last file
     * action did, rewritten only when the materials or the tool options
     * change.
     */

    // Internal
    private MenuManager menuManager;
    private FBOManager fboManager;
    private MaterialsManager materialsManager;
    private ToolManager toolManager;

    // Menus
    private MenuInstance viewerMenu;
    private ElementInstance statusLabel;

    // Status
    private int shownRevision;
    private int shownOptionRevision;

    // Base \\

    @Override
    protected void create() {

        // Status
        this.shownRevision = EngineSetting.INDEX_NOT_FOUND;
        this.shownOptionRevision = EngineSetting.INDEX_NOT_FOUND;
    }

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.fboManager = get(FBOManager.class);
        this.materialsManager = get(MaterialsManager.class);
        this.toolManager = get(ToolManager.class);
    }

    @Override
    protected void awake() {

        WindowInstance window = context.getWindow();

        menuManager.setMenuTargetFbo(window, fboManager.cloneFbo(RuntimeSetting.FBO_UI, window));
        this.viewerMenu = menuManager.openMenu(MaterialViewerSetting.MENU_VIEWER, window);
        this.statusLabel = viewerMenu.getEntryPoint(MaterialViewerSetting.ENTRY_STATUS);

        if (statusLabel == null)
            throwException("Material Viewer menu '" + MaterialViewerSetting.MENU_VIEWER + "' has no entry point "
                    + MaterialViewerSetting.ENTRY_STATUS + ".");
    }

    // Update \\

    @Override
    protected void update() {

        if (materialsManager.getRevision() == shownRevision && toolManager.getOptionRevision() == shownOptionRevision)
            return;

        this.shownRevision = materialsManager.getRevision();
        this.shownOptionRevision = toolManager.getOptionRevision();

        statusLabel.setFontText(describe());
    }

    // The material shown, the shape it is on, and what the last file action did
    private String describe() {

        int shape = Math.clamp(
                (int) toolManager.getOption(ToolCanvas.MATERIAL_VIEWER, ToolOption.SHAPE),
                0,
                EditorSetting.TOOL_SHAPE_NAMES.length - 1);
        StringBuilder text = new StringBuilder(MaterialViewerSetting.STATUS_PREFIX)
                .append(CharacterViewUtility.toResourceLabel(materialsManager.getChosenName()))
                .append(MaterialViewerSetting.STATUS_SEPARATOR)
                .append(EditorSetting.TOOL_SHAPE_NAMES[shape]);

        if (!materialsManager.getStatusMessage().isEmpty())
            text.append(MaterialViewerSetting.STATUS_SEPARATOR).append(materialsManager.getStatusMessage());

        return text.toString();
    }
}
