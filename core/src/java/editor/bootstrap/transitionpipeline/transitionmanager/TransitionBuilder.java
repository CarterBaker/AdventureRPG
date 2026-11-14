package editor.bootstrap.transitionpipeline.transitionmanager;

import editor.bootstrap.transitionpipeline.transition.TransitionData;
import editor.bootstrap.transitionpipeline.util.TransitionDirection;
import editor.bootstrap.transitionpipeline.util.TransitionEasing;
import editor.bootstrap.transitionpipeline.util.TransitionKind;
import editor.runtime.EditorSetting;
import engine.root.BuilderPackage;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.mathematics.vectors.Vector3;

class TransitionBuilder extends BuilderPackage {

    /*
     * Parses one transition's ARPG object into TransitionData, from its file
     * at boot or from the Info Panel's live copy while it is edited. Every
     * field is optional: an empty or missing display name falls back to the
     * transition's name, the kind to a dissolve, the direction to the left,
     * the colour to black, the easing to even, and each number to its default,
     * held to its range.
     */

    // Internal
    private TransitionManager transitionManager;

    // Base \\

    @Override
    protected void get() {
        this.transitionManager = get(TransitionManager.class);
    }

    // Build \\

    TransitionData build(String transitionName, ArpgObjectStruct transitionArpg) {

        String displayName = ArpgUtility.getString(transitionArpg, "display_name", "");

        return new TransitionData(
                transitionName,
                transitionManager.registerTransitionName(transitionName),
                displayName.isEmpty() ? transitionName : displayName,
                ArpgUtility.getEnum(transitionArpg, "kind", TransitionKind.class, TransitionKind.DISSOLVE),
                ArpgUtility.getEnum(transitionArpg, "direction", TransitionDirection.class, TransitionDirection.LEFT),
                parseColor(transitionArpg),
                Math.clamp(ArpgUtility.getFloat(transitionArpg, "softness", EditorSetting.TRANSITION_DEFAULT_SOFTNESS),
                        0f, 1f),
                ArpgUtility.getEnum(transitionArpg, "easing", TransitionEasing.class, TransitionEasing.LINEAR),
                Math.clamp(ArpgUtility.getFloat(transitionArpg, "seconds", EditorSetting.TRANSITION_DEFAULT_SECONDS),
                        EditorSetting.TRANSITION_MIN_SECONDS, EditorSetting.TRANSITION_MAX_SECONDS));
    }

    // Each channel of the colour held from black to full, black when none is given
    private Vector3 parseColor(ArpgObjectStruct transitionArpg) {

        if (!ArpgUtility.hasArray(transitionArpg, "color"))
            return new Vector3(0f, 0f, 0f);

        ArpgArrayStruct colorArpg = ArpgUtility.validateArray(transitionArpg, "color", 3);

        return new Vector3(
                Math.clamp(colorArpg.get(0).getAsFloat(), 0f, 1f),
                Math.clamp(colorArpg.get(1).getAsFloat(), 0f, 1f),
                Math.clamp(colorArpg.get(2).getAsFloat(), 0f, 1f));
    }
}
