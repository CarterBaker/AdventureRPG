package editor.runtime.structureeditor.menu;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import editor.bootstrap.structureeditorpipeline.structureeditormanager.StructureEditorManager;
import editor.runtime.structureeditor.StructureEditorSetting;
import engine.root.EngineSetting;
import engine.root.SystemPackage;

public class StructureEditorMenuSystem extends SystemPackage {

    /*
     * Opens the Structure Editor's menu over the viewport: the status line
     * names the structure painted, how many blocks it lays, the layer picked
     * and the architecture it is shown built in, then the last message, or
     * why nothing is shown, rewritten only when the editor's revision moves.
     * The axis gizmo is every viewport's own (ViewGizmoSystem).
     */

    // Internal
    private MenuManager menuManager;
    private FBOManager fboManager;
    private StructureEditorManager structureEditorManager;

    // Menus
    private MenuInstance viewerMenu;

    // Status
    private int shownRevision;

    // Base \\

    @Override
    protected void create() {

        // Status
        this.shownRevision = EngineSetting.INDEX_NOT_FOUND;
    }

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.fboManager = get(FBOManager.class);
        this.structureEditorManager = get(StructureEditorManager.class);
    }

    @Override
    protected void awake() {

        WindowInstance window = context.getWindow();

        menuManager.setMenuTargetFbo(window, fboManager.cloneFbo(RuntimeSetting.FBO_UI, window));
        this.viewerMenu = menuManager.openMenu(StructureEditorSetting.MENU_VIEWER, window);
    }

    // Update \\

    @Override
    protected void update() {

        int revision = structureEditorManager.getRevision();

        if (revision == shownRevision)
            return;

        this.shownRevision = revision;

        ElementInstance label = viewerMenu.getEntryPoint(StructureEditorSetting.ENTRY_STATUS);

        if (label != null)
            label.setFontText(StructureEditorSetting.STATUS_PREFIX + describe());
    }

    // The structure, its size, the layer and the look it is shown in, then the last message, or why none is shown
    private String describe() {

        String structureName = structureEditorManager.getStructureName();

        if (structureName == null)
            return StructureEditorSetting.STATUS_NONE;

        String architectureName = structureEditorManager.getArchitectureName();
        String message = structureEditorManager.getStatusMessage();

        return structureName + StructureEditorSetting.STATUS_SEPARATOR
                + structureEditorManager.getCells().size() + StructureEditorSetting.STATUS_BLOCKS
                + StructureEditorSetting.STATUS_SEPARATOR + structureEditorManager.getLayer().label
                + StructureEditorSetting.STATUS_SEPARATOR
                + (architectureName.isEmpty()
                        ? StructureEditorSetting.STATUS_AS_AUTHORED
                        : StructureEditorSetting.STATUS_BUILT_IN + architectureName)
                + (message.isEmpty() ? "" : StructureEditorSetting.STATUS_SEPARATOR + message);
    }
}
