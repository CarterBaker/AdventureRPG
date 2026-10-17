package editor.runtime.menueventsmanager.menus;

import application.kernel.windowpipeline.window.WindowInstance;
import editor.bootstrap.animationtreeeditorpipeline.animationtreeeditormanager.AnimationTreeEditorManager;
import editor.bootstrap.animationtreeeditorpipeline.util.AnimationTreeEditorView;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;

public class AnimationTreeEditorBranch extends BranchPackage {

    /*
     * Menu event handlers for the Animation Tree Editor toolbar and panels.
     * Every action works on the shared active tree. New trees, layers and
     * nodes, and their renames, are named through the name dialog; deleting
     * a tree asks for its name to be typed back as confirmation, and a
     * number's value opens in the text dialog. Panel rows hand back the key
     * of the value they show.
     */

    // Internal
    private AnimationTreeEditorManager animationTreeEditorManager;
    private NameDialogBranch nameDialogBranch;

    // Base \\

    @Override
    protected void get() {
        this.animationTreeEditorManager = get(AnimationTreeEditorManager.class);
        this.nameDialogBranch = get(NameDialogBranch.class);
    }

    // Trees \\

    public void newTree(WindowInstance window) {
        nameDialogBranch.open(
                window,
                EditorSetting.DIALOG_TITLE_NEW_TREE,
                animationTreeEditorManager::isTreeNameAvailable,
                animationTreeEditorManager::createTree);
    }

    public void saveTree() {
        animationTreeEditorManager.saveActiveTree();
    }

    public void revertTree() {
        animationTreeEditorManager.revertActiveTree();
    }

    public void deleteTree(WindowInstance window) {

        if (!animationTreeEditorManager.hasActiveDocument())
            return;

        nameDialogBranch.open(
                window,
                EditorSetting.DIALOG_TITLE_DELETE_PREFIX + animationTreeEditorManager.getActiveLocalName()
                        + EditorSetting.DIALOG_TITLE_DELETE_SUFFIX,
                animationTreeEditorManager::isActiveTreeName,
                confirmedName -> animationTreeEditorManager.deleteActiveTree());
    }

    // Views \\

    public void showLayer() {
        animationTreeEditorManager.setView(AnimationTreeEditorView.LAYER);
    }

    public void showNode() {
        animationTreeEditorManager.setView(AnimationTreeEditorView.NODE);
    }

    public void showStates() {
        animationTreeEditorManager.setView(AnimationTreeEditorView.STATES);
    }

    public void showMask() {
        animationTreeEditorManager.setView(AnimationTreeEditorView.MASK);
    }

    public void showTransitions() {
        animationTreeEditorManager.setView(AnimationTreeEditorView.TRANSITIONS);
    }

    // Rows \\

    public void selectRow(String key) {
        animationTreeEditorManager.select(key);
    }

    public void decreaseRow(String key) {
        animationTreeEditorManager.step(key, -1);
    }

    public void increaseRow(String key) {
        animationTreeEditorManager.step(key, 1);
    }

    public void editRow(String key, WindowInstance window) {
        nameDialogBranch.openText(
                window,
                EditorSetting.DIALOG_TITLE_EDIT_VALUE,
                animationTreeEditorManager.getValueText(key),
                text -> animationTreeEditorManager.isValueValid(key, text),
                text -> animationTreeEditorManager.setValue(key, text));
    }

    public void previousRow(String key) {
        animationTreeEditorManager.cycle(key, -1);
    }

    public void nextRow(String key) {
        animationTreeEditorManager.cycle(key, 1);
    }

    // Layers and nodes are named in the name dialog; every other action applies at once
    public void actRow(String key, WindowInstance window) {

        if (!animationTreeEditorManager.hasActiveDocument())
            return;

        switch (key) {
            case EditorSetting.ANIMATION_TREE_EDITOR_KEY_ADD_LAYER -> nameDialogBranch.open(
                    window,
                    EditorSetting.DIALOG_TITLE_NEW_LAYER,
                    animationTreeEditorManager::isLayerNameValid,
                    animationTreeEditorManager::addLayer);
            case EditorSetting.ANIMATION_TREE_EDITOR_KEY_RENAME_LAYER -> nameDialogBranch.openText(
                    window,
                    EditorSetting.DIALOG_TITLE_RENAME_LAYER,
                    animationTreeEditorManager.getSelectedLayerName(),
                    animationTreeEditorManager::isLayerNameValid,
                    animationTreeEditorManager::renameSelectedLayer);
            case EditorSetting.ANIMATION_TREE_EDITOR_KEY_ADD_NODE -> nameDialogBranch.open(
                    window,
                    EditorSetting.DIALOG_TITLE_NEW_NODE,
                    animationTreeEditorManager::isNodeNameAvailable,
                    animationTreeEditorManager::addNode);
            case EditorSetting.ANIMATION_TREE_EDITOR_KEY_RENAME_NODE -> nameDialogBranch.open(
                    window,
                    EditorSetting.DIALOG_TITLE_RENAME_NODE,
                    animationTreeEditorManager::isNodeNameAvailable,
                    animationTreeEditorManager::renameSelectedNode);
            default -> animationTreeEditorManager.act(key);
        }
    }
}
