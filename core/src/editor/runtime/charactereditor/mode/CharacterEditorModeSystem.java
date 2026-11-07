package editor.runtime.charactereditor.mode;

import editor.bootstrap.charactereditorpipeline.charactereditormanager.CharacterEditorManager;
import editor.bootstrap.charactereditorpipeline.util.CharacterEditorMode;
import editor.bootstrap.charactereditorpipeline.util.CharacterEditorRegion;
import editor.bootstrap.tabpipeline.activewindowmanager.ActiveWindowManager;
import editor.runtime.charactereditor.CharacterEditorWindowProvider;
import engine.root.SystemPackage;

public class CharacterEditorModeSystem extends SystemPackage {

    /*
     * Keeps the mode one character viewport tab works in. Each tab keeps to
     * its own modes — the Character Appearance, Build and Equipment tabs to
     * one each, the Character Mesh Editor to those that take tools — and
     * remembers the last of them it was in. While the tab is the active
     * window, a mode of its own chosen on the editor is
     * taken up here, and any other is handed back for this tab's, so the
     * Character Outliner and the Properties tab always follow the tab
     * worked in last; the tab's region is handed to the editor the same way.
     * The tab draws its preview in its own mode whatever the editor's is.
     */

    // Internal
    private ActiveWindowManager activeWindowManager;
    private CharacterEditorManager characterEditorManager;

    // Window
    private CharacterEditorWindowProvider windowProvider;

    // Mode
    private CharacterEditorMode mode;

    // Base \\

    @Override
    protected void get() {
        this.activeWindowManager = get(ActiveWindowManager.class);
        this.characterEditorManager = get(CharacterEditorManager.class);
    }

    @Override
    protected void awake() {

        if (!(context instanceof CharacterEditorWindowProvider))
            throwException("A character viewport tab's context must name the mode it opens in.");

        this.windowProvider = (CharacterEditorWindowProvider) context;
        this.mode = windowProvider.getStartingMode();
    }

    // Update \\

    @Override
    protected void update() {
        if (activeWindowManager.isActive(context.getWindow()))
            adopt();
    }

    // The editor's mode taken up when it is one of this tab's, and this tab's handed to it otherwise, with its region
    public void adopt() {

        characterEditorManager.setRegion(windowProvider.getRegion());

        CharacterEditorMode shared = characterEditorManager.getMode();

        if (windowProvider.holdsMode(shared))
            this.mode = shared;
        else
            characterEditorManager.setMode(mode);
    }

    // Accessible \\

    public CharacterEditorMode getMode() {
        return mode;
    }

    public CharacterEditorRegion getRegion() {
        return windowProvider.getRegion();
    }
}
