package editor.runtime.recorder.panel;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.util.DimensionValueStruct;
import application.bootstrap.menupipeline.util.DimensionVector2Struct;
import editor.runtime.recorder.RecorderSetting;
import engine.root.EngineSetting;
import engine.root.StructPackage;

public class RecorderPlayheadStruct extends StructPackage {

    /*
     * The playhead line across one Recorder tab's strips or graph: hidden
     * while the playhead lies out of view, and moved only when it reaches
     * another of the view's steps, so a playhead standing still touches
     * nothing.
     */

    // Internal
    private final ElementInstance element;
    private final DimensionVector2Struct shownSize;
    private final DimensionVector2Struct hiddenSize;

    // State
    private boolean shown;
    private int shownStep;

    // Constructor \\

    public RecorderPlayheadStruct(ElementInstance element) {

        // Internal
        this.element = element;
        this.shownSize = new DimensionVector2Struct(
                DimensionValueStruct.ofAbsolute(RecorderSetting.PLAYHEAD_WIDTH_PIXELS),
                DimensionValueStruct.ofPercent(RecorderSetting.TIMELINE_PERCENT));
        this.hiddenSize = new DimensionVector2Struct(
                DimensionValueStruct.ofAbsolute(0f),
                DimensionValueStruct.ofAbsolute(0f));

        // State
        this.shown = true;
        this.shownStep = EngineSetting.INDEX_NOT_FOUND;
    }

    // Update \\

    // Placed at the step while in view, hidden otherwise
    public void place(boolean inView, int step, DimensionVector2Struct position) {

        if (inView != shown) {
            this.shown = inView;
            this.shownStep = EngineSetting.INDEX_NOT_FOUND;
            element.setSizeOverride(inView ? shownSize : hiddenSize);
        }

        if (!inView || step == shownStep)
            return;

        this.shownStep = step;
        element.setPositionOverride(position);
    }

    // Placed again on the next call, wherever it stood
    public void forget() {
        this.shownStep = EngineSetting.INDEX_NOT_FOUND;
    }
}
