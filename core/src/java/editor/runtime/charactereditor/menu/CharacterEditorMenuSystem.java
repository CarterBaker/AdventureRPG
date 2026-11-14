package editor.runtime.charactereditor.menu;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import editor.bootstrap.charactereditorpipeline.charactereditormanager.CharacterEditorManager;
import editor.bootstrap.faceeditorpipeline.faceeditormanager.FaceEditorManager;
import editor.runtime.charactereditor.CharacterEditorSetting;
import editor.runtime.charactereditor.mode.CharacterEditorModeSystem;
import engine.root.EngineSetting;
import engine.root.SystemPackage;

public class CharacterEditorMenuSystem extends SystemPackage {

    /*
     * Opens the Character Editor window's status bar over the viewport and
     * writes the status of the layer worked on into it: the face editor's on
     * the Face layer, the character editor's on every other, rewritten only
     * when either editor's state moves or the window turns to or from the
     * face. The layers are picked in the Layers tab; the editor's panels are
     * the Character and Face Outliner docker windows and the Properties tab.
     */

    // Internal
    private MenuManager menuManager;
    private FBOManager fboManager;
    private CharacterEditorManager characterEditorManager;
    private FaceEditorManager faceEditorManager;
    private CharacterEditorModeSystem characterEditorModeSystem;

    // Menus
    private MenuInstance toolbarMenu;

    // Status
    private int shownRevision;
    private boolean shownFace;

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
        this.characterEditorManager = get(CharacterEditorManager.class);
        this.faceEditorManager = get(FaceEditorManager.class);
        this.characterEditorModeSystem = get(CharacterEditorModeSystem.class);
    }

    @Override
    protected void awake() {

        WindowInstance window = context.getWindow();

        menuManager.setMenuTargetFbo(window, fboManager.cloneFbo(RuntimeSetting.FBO_UI, window));
        this.toolbarMenu = menuManager.openMenu(CharacterEditorSetting.MENU_TOOLBAR, window);
    }

    // Update \\

    // The face editor's revision moves with every change of either editor, so it alone says when to rewrite
    @Override
    protected void update() {

        boolean face = characterEditorModeSystem.getMode().shapesFace();

        if (faceEditorManager.getRevision() == shownRevision && face == shownFace)
            return;

        this.shownRevision = faceEditorManager.getRevision();
        this.shownFace = face;

        ElementInstance statusLabel = toolbarMenu.getEntryPoint(CharacterEditorSetting.ENTRY_STATUS);

        if (statusLabel != null)
            statusLabel.setFontText(face ? faceEditorManager.getStatusText() : characterEditorManager.getStatusText());
    }
}
