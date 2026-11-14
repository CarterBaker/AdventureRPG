package editor.runtime.slider;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.util.DimensionValueStruct;
import application.bootstrap.menupipeline.util.DimensionVector2Struct;
import editor.runtime.EditorSetting;
import engine.root.EngineSetting;
import engine.root.EngineUtility;

public class SliderUtility extends EngineUtility {

    /*
     * Stateless arithmetic every editor slider shares: the share of its track
     * a window position lies at, the step of the track nearest a share, the
     * share a step stands for, and the knob moved onto a step. Knob positions
     * are laid out once for every step, so a drag allocates nothing.
     */

    // Knob
    private static final DimensionVector2Struct[] KNOB_POSITIONS = createKnobPositions();

    private static DimensionVector2Struct[] createKnobPositions() {

        DimensionVector2Struct[] positions = new DimensionVector2Struct[EditorSetting.SLIDER_STEPS + 1];

        for (int i = 0; i < positions.length; i++)
            positions[i] = new DimensionVector2Struct(
                    DimensionValueStruct.ofPercent(EngineSetting.PERCENT_MAX * i / EditorSetting.SLIDER_STEPS),
                    DimensionValueStruct.ofAbsolute(0f));

        return positions;
    }

    // Track \\

    // The share of a track a window position lies at, held to the track's ends
    public static float resolveFraction(float pointerX, ElementInstance track) {
        return Math.clamp((pointerX - track.getComputedLeft()) / track.getComputedW(), 0f, 1f);
    }

    public static int resolveStep(float fraction) {
        return Math.round(Math.clamp(fraction, 0f, 1f) * EditorSetting.SLIDER_STEPS);
    }

    public static float toFraction(int step) {
        return step / (float) EditorSetting.SLIDER_STEPS;
    }

    // Knob \\

    public static void placeKnob(ElementInstance track, String knobID, int step) {

        ElementInstance knob = track.findChildById(knobID);

        if (knob != null)
            knob.setPositionOverride(KNOB_POSITIONS[Math.clamp(step, 0, EditorSetting.SLIDER_STEPS)]);
    }
}
