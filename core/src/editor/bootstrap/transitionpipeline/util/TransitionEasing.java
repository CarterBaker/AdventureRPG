package editor.bootstrap.transitionpipeline.util;

import editor.runtime.EditorSetting;

public enum TransitionEasing {

    /*
     * How a transition paces itself across its length: evenly, starting
     * slowly, finishing slowly, or both. Each turns how far through the
     * transition a moment lies, zero to one, into how far the pictures have
     * gone over, zero to one.
     */

    LINEAR(EditorSetting.TRANSITION_EASING_LINEAR) {
        @Override
        public float apply(float progress) {
            return progress;
        }
    },
    EASE_IN(EditorSetting.TRANSITION_EASING_EASE_IN) {
        @Override
        public float apply(float progress) {
            return progress * progress;
        }
    },
    EASE_OUT(EditorSetting.TRANSITION_EASING_EASE_OUT) {
        @Override
        public float apply(float progress) {
            return 1f - (1f - progress) * (1f - progress);
        }
    },
    EASE_IN_OUT(EditorSetting.TRANSITION_EASING_EASE_IN_OUT) {
        @Override
        public float apply(float progress) {
            return progress * progress * (3f - 2f * progress);
        }
    };

    // Values
    public static final TransitionEasing[] VALUES = values();

    // Internal
    private final String label;

    // Constructor \\

    TransitionEasing(String label) {
        this.label = label;
    }

    // Easing \\

    public abstract float apply(float progress);

    // Accessible \\

    public String getLabel() {
        return label;
    }
}
