package editor.bootstrap.animationtreeeditorpipeline.util;

import editor.runtime.EditorSetting;

public enum AnimationTreeEditorView {

    /*
     * What the animation tree editor's right panel shows of the selected
     * layer: the layer's own blend, the selected node, the node each movement
     * state and action enters, the bones the layer touches, or the
     * cross-fades between its nodes.
     */

    LAYER(EditorSetting.ANIMATION_TREE_EDITOR_VIEW_LAYER),
    NODE(EditorSetting.ANIMATION_TREE_EDITOR_VIEW_NODE),
    STATES(EditorSetting.ANIMATION_TREE_EDITOR_VIEW_STATES),
    MASK(EditorSetting.ANIMATION_TREE_EDITOR_VIEW_MASK),
    TRANSITIONS(EditorSetting.ANIMATION_TREE_EDITOR_VIEW_TRANSITIONS);

    // Internal
    private final String label;

    // Constructor \\

    AnimationTreeEditorView(String label) {
        this.label = label;
    }

    // Accessible \\

    public String getLabel() {
        return label;
    }
}
