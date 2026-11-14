package editor.bootstrap.transitionpipeline.util;

import editor.runtime.EditorSetting;

public enum TransitionKind {

    /*
     * How a transition carries one picture into the next: a dissolve blends
     * them, a dip passes through its colour, a wipe sweeps the next across
     * the first, a push slides the next in and the first out, and an iris
     * opens the next from the centre. The ordinal is the kind index the
     * transition shader draws, so the order is fixed.
     */

    DISSOLVE(EditorSetting.TRANSITION_KIND_DISSOLVE),
    DIP(EditorSetting.TRANSITION_KIND_DIP),
    WIPE(EditorSetting.TRANSITION_KIND_WIPE),
    PUSH(EditorSetting.TRANSITION_KIND_PUSH),
    IRIS(EditorSetting.TRANSITION_KIND_IRIS);

    // Values
    public static final TransitionKind[] VALUES = values();

    // Internal
    private final String label;

    // Constructor \\

    TransitionKind(String label) {
        this.label = label;
    }

    // Accessible \\

    public String getLabel() {
        return label;
    }
}
