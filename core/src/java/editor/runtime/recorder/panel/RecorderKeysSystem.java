package editor.runtime.recorder.panel;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.menupipeline.util.DimensionValueStruct;
import application.bootstrap.menupipeline.util.DimensionVector2Struct;
import editor.bootstrap.recorderpipeline.recordermanager.RecorderManager;
import editor.bootstrap.recorderpipeline.recording.RecorderDragKind;
import editor.bootstrap.recorderpipeline.recording.RecorderNodeKind;
import editor.bootstrap.recorderpipeline.recording.RecordingBodyTrackStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingClipStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingCurveStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingCurveTrackStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingDragStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingKeySelectionStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingNodeStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingSoundTrackStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingViewStruct;
import editor.runtime.EditorSetting;
import editor.runtime.recorder.RecorderSetting;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class RecorderKeysSystem extends SystemPackage {

    /*
     * Fills the Keys tab with the layer picked in the Layers tab, of the take
     * the selected clip plays. A track's layer is a row for every value it
     * holds and, for a character, a row of its movement and one of its
     * gestures, every row's strip showing its keys or nodes across the stretch
     * of the timeline in view. An animation clip's layer is a dope sheet, as
     * Blender's lists one: a summary row for every bone and morph it keys,
     * marking every key of any of its values, and under the one picked a row
     * for each of its values, its keys picked one by one; the values are also
     * listed in the Properties tab and the one shown drawn on the Keys Graph.
     * The Sounds layer is a row for each sound it names with a bar for every
     * time it is heard, the selected one marked and following the cursor while
     * it is dragged. Every key selected on any curve is marked, on its value's
     * row and on its summary row, and a box drawn across the rows is drawn
     * over them. The tab is drawn again when the recorder's revision, its keys
     * or its view move; keys being dragged move only the selected marks, a
     * sound carried only its bar, a box only its sides, and the playhead
     * follows every frame from positions built once.
     */

    // Internal
    private MenuManager menuManager;
    private RecorderManager recorderManager;
    private RecorderRowSystem recorderRowSystem;

    // Menus
    private MenuInstance recorderMenu;
    private ElementInstance keysTitle;
    private ElementInstance keyArea;
    private RecorderPlayheadStruct playhead;

    // Strips — the steps a strip has marked, and on a summary row whether a selected key lies there and the value and
    // key it stands for
    private boolean[] stepTaken;
    private boolean[] stepSelected;
    private int[] stepSlot;
    private int[] stepKey;

    // Selected — every selected key's mark, with the track, slot and key it follows while keys are dragged
    private ObjectArrayList<ElementInstance> selectedDiamonds;
    private ObjectArrayList<RecordingCurveTrackStruct> selectedDiamondTracks;
    private IntArrayList selectedDiamondSlots;
    private IntArrayList selectedDiamondKeys;
    private ElementInstance selectedSoundBar;

    // Box
    private RecorderMarkPoolStruct boxPool;

    // State
    private int shownRevision;
    private int shownKeysRevision;
    private int shownViewRevision;
    private int shownCurveRevision;
    private float shownViewStart;
    private float shownViewSpan;

    // Base \\

    @Override
    protected void create() {

        // Strips
        this.stepTaken = new boolean[RecorderSetting.TIMELINE_STEPS + 1];
        this.stepSelected = new boolean[RecorderSetting.TIMELINE_STEPS + 1];
        this.stepSlot = new int[RecorderSetting.TIMELINE_STEPS + 1];
        this.stepKey = new int[RecorderSetting.TIMELINE_STEPS + 1];

        // Selected
        this.selectedDiamonds = new ObjectArrayList<>();
        this.selectedDiamondTracks = new ObjectArrayList<>();
        this.selectedDiamondSlots = new IntArrayList();
        this.selectedDiamondKeys = new IntArrayList();

        // State
        this.shownRevision = EngineSetting.INDEX_NOT_FOUND;
    }

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.recorderManager = get(RecorderManager.class);
        this.recorderRowSystem = get(RecorderRowSystem.class);
    }

    @Override
    protected void awake() {

        this.recorderMenu = recorderRowSystem.getMenu();
        this.keysTitle = recorderRowSystem.requireEntryPoint(RecorderSetting.ENTRY_KEYS_TITLE);
        this.playhead = new RecorderPlayheadStruct(
                recorderRowSystem.requireEntryPoint(RecorderSetting.ENTRY_KEYS_PLAYHEAD));

        recorderRowSystem.requireEntryPoint(EditorSetting.RECORDER_ENTRY_KEY_ROWS);
        this.keyArea = recorderRowSystem.requireEntryPoint(EditorSetting.RECORDER_ENTRY_KEY_STRIPS);

        // Box
        this.boxPool = new RecorderMarkPoolStruct(RecorderSetting.TEMPLATE_GRAPH_BOX, keyArea);
    }

    // Update \\

    @Override
    protected void update() {

        if (isStale())
            refreshKeys();
        else if (recorderManager.getCurveRevision() != shownCurveRevision)
            refreshCurve();

        recorderRowSystem.placePlayhead(playhead);
    }

    private boolean isStale() {

        RecordingViewStruct view = recorderManager.getView();

        return recorderManager.getRevision() != shownRevision
                || recorderManager.getKeysRevision() != shownKeysRevision
                || recorderManager.getViewRevision() != shownViewRevision
                || view.getStart() != shownViewStart
                || view.getSpan() != shownViewSpan;
    }

    // Refresh \\

    private void refreshKeys() {

        RecordingViewStruct view = recorderManager.getView();

        this.shownRevision = recorderManager.getRevision();
        this.shownKeysRevision = recorderManager.getKeysRevision();
        this.shownViewRevision = recorderManager.getViewRevision();
        this.shownCurveRevision = recorderManager.getCurveRevision();
        this.shownViewStart = view.getStart();
        this.shownViewSpan = view.getSpan();
        playhead.forget();

        RecordingClipStruct clip = recorderManager.getKeyClip();

        keysTitle.setFontText(clip != null && clip.getTake() != null
                ? String.format(RecorderSetting.FORMAT_KEYS_TITLE, resolveLayerLabel())
                : RecorderSetting.TEXT_KEYS_TITLE);

        refreshRows(clip);
        placeBox();
    }

    // Keys being dragged, a sound carried or a box drawn: only the selected marks, the selected sound's bar or the
    // box move
    private void refreshCurve() {

        this.shownCurveRevision = recorderManager.getCurveRevision();

        moveSelectedDiamonds(recorderManager.getKeyClip());
        moveSelectedSoundBar();
        placeBox();
    }

    // The box a selection is being drawn with across the rows, from the moment and height it was pressed at to the
    // cursor's
    private void placeBox() {

        RecordingDragStruct drag = recorderManager.getDrag();
        float width = keyArea.getComputedW();
        float height = keyArea.getComputedH();

        boxPool.begin();

        if (drag.getKind() == RecorderDragKind.KEYS_BOX && drag.isMoved() && width > 0f && height > 0f) {

            RecordingViewStruct view = recorderManager.getView();
            float bottom = keyArea.getComputedTop();

            recorderRowSystem.placeBox(boxPool,
                    Math.clamp(view.toFraction(drag.getPressTime()) * width, 0f, width),
                    Math.clamp(drag.getPressY() - bottom, 0f, height),
                    Math.clamp(view.toFraction(drag.getReachTime()) * width, 0f, width),
                    Math.clamp(drag.getReachY() - bottom, 0f, height));
        }

        recorderRowSystem.setAsideUnused(boxPool);
    }

    // The name of the layer shown, or what to pick when none is
    private String resolveLayerLabel() {

        int layer = recorderManager.getKeyLayer();

        if (layer == EditorSetting.RECORDER_KEY_TRACK_SOUNDS)
            return RecorderSetting.TEXT_LAYER_SOUNDS;

        return layer >= 0 && layer < recorderManager.getKeyTracks().size()
                ? recorderManager.getKeyTracks().get(layer).getTrackLabel()
                : RecorderSetting.TEXT_LAYER_NONE;
    }

    // Rows \\

    private void refreshRows(RecordingClipStruct clip) {

        menuManager.ejectAll(recorderMenu, EditorSetting.RECORDER_ENTRY_KEY_ROWS);
        selectedDiamonds.clear();
        selectedDiamondTracks.clear();
        selectedDiamondSlots.clear();
        selectedDiamondKeys.clear();
        this.selectedSoundBar = null;

        if (clip == null) {
            recorderRowSystem.injectText(EditorSetting.RECORDER_ENTRY_KEY_ROWS, RecorderSetting.TEXT_KEYS_NO_CLIP);
            return;
        }

        ObjectArrayList<RecordingCurveTrackStruct> tracks = recorderManager.getKeyTracks();
        RecordingSoundTrackStruct soundTrack = recorderManager.getKeySoundTrack();
        int layer = recorderManager.getKeyLayer();

        if (tracks.isEmpty() && soundTrack == null) {
            recorderRowSystem.injectText(EditorSetting.RECORDER_ENTRY_KEY_ROWS, RecorderSetting.TEXT_KEYS_NO_TRACKS);
            return;
        }

        if (layer == EditorSetting.RECORDER_KEY_TRACK_SOUNDS && soundTrack != null)
            injectSounds(soundTrack, clip);
        else if (recorderManager.isDopeSheet())
            injectDopeSheet(tracks, layer, clip);
        else if (layer >= 0 && layer < tracks.size())
            injectTrack(tracks.get(layer), layer, clip);
        else
            recorderRowSystem.injectText(EditorSetting.RECORDER_ENTRY_KEY_ROWS, RecorderSetting.TEXT_KEYS_PICK_LAYER);
    }

    // The track's node rows and a row for every value it holds
    private void injectTrack(RecordingCurveTrackStruct track, int trackIndex, RecordingClipStruct clip) {

        if (track instanceof RecordingBodyTrackStruct body) {
            injectNodes(injectRow(RecorderSetting.TEMPLATE_KEY_NODE_ROW, RecorderSetting.TEXT_ROW_MOVEMENT,
                    toRowKey(trackIndex, EditorSetting.RECORDER_KEY_ROW_MOVEMENT)), body, RecorderNodeKind.MOVEMENT);
            injectNodes(injectRow(RecorderSetting.TEMPLATE_KEY_NODE_ROW, RecorderSetting.TEXT_ROW_GESTURES,
                    toRowKey(trackIndex, EditorSetting.RECORDER_KEY_ROW_GESTURES)), body, RecorderNodeKind.GESTURE);
        }

        injectCurveRows(track, trackIndex, clip, RecorderSetting.FORMAT_CURVE_ROW);
    }

    // A row for every value of the track, the one shown marked, each with every key it holds
    private void injectCurveRows(
            RecordingCurveTrackStruct track,
            int trackIndex,
            RecordingClipStruct clip,
            String labelFormat) {

        for (int slot = 0; slot < track.getCurveCount(); slot++) {

            boolean shown = track == recorderManager.getSelectedTrack() && slot == recorderManager.getSelectedSlot();
            ElementInstance strip = injectRow(
                    shown ? RecorderSetting.TEMPLATE_KEY_CURVE_ROW_SELECTED : RecorderSetting.TEMPLATE_KEY_CURVE_ROW,
                    String.format(labelFormat, track.getCurveLabel(slot)),
                    toRowKey(trackIndex, slot));

            if (track.hasCurve(slot))
                injectKeys(strip, clip, track, slot);
        }
    }

    // A summary row for every bone and morph of the animation layer, the one picked marked with a row for each of its
    // values under it; its values are also the Properties tab's, and the one shown is the Keys Graph's
    private void injectDopeSheet(
            ObjectArrayList<RecordingCurveTrackStruct> tracks,
            int layer,
            RecordingClipStruct clip) {

        for (int i = 0; i < tracks.size(); i++) {

            RecordingCurveTrackStruct track = tracks.get(i);
            ElementInstance strip = injectRow(
                    i == layer ? RecorderSetting.TEMPLATE_KEY_CURVE_ROW_SELECTED : RecorderSetting.TEMPLATE_KEY_NODE_ROW,
                    track.getTrackLabel(),
                    toRowKey(i, EditorSetting.RECORDER_KEY_ROW_SUMMARY));

            injectSummaryKeys(strip, clip, track);

            if (i == layer)
                injectCurveRows(track, i, clip, RecorderSetting.FORMAT_CHANNEL_ROW);
        }
    }

    // One mark for every step of the view where any value of the track holds a key, marked selected where any key
    // selected lies, and kept to follow that key while keys are dragged
    private void injectSummaryKeys(ElementInstance strip, RecordingClipStruct clip, RecordingCurveTrackStruct track) {

        RecordingKeySelectionStruct selection = recorderManager.getKeySelection();

        clearSteps();

        for (int slot = 0; slot < track.getCurveCount(); slot++) {

            if (!track.hasCurve(slot))
                continue;

            RecordingCurveStruct curve = track.getCurve(slot);
            int selectedEntry = selection.findFirstOn(track, slot);

            for (int i = 0; i < curve.size(); i++) {

                boolean selected = selection.isAt(selectedEntry, track, slot, i);

                if (selected)
                    selectedEntry++;

                int step = resolveKeyStep(clip, curve.getTime(i));

                if (step == EngineSetting.INDEX_NOT_FOUND || stepTaken[step] && (!selected || stepSelected[step]))
                    continue;

                stepTaken[step] = true;
                stepSelected[step] = selected;
                stepSlot[step] = slot;
                stepKey[step] = i;
            }
        }

        for (int step = 0; step < stepTaken.length; step++) {

            if (!stepTaken[step])
                continue;

            ElementInstance diamond = injectDiamond(strip, stepSelected[step]
                    ? RecorderSetting.TEMPLATE_KEY_DIAMOND_SELECTED
                    : RecorderSetting.TEMPLATE_KEY_DIAMOND, step);

            if (stepSelected[step])
                keepSelectedDiamond(diamond, track, stepSlot[step], stepKey[step]);
        }
    }

    private void keepSelectedDiamond(ElementInstance diamond, RecordingCurveTrackStruct track, int slot, int key) {
        selectedDiamonds.add(diamond);
        selectedDiamondTracks.add(track);
        selectedDiamondSlots.add(slot);
        selectedDiamondKeys.add(key);
    }

    // One row, its name and its strip both handing back the row's key; its strip comes back
    private ElementInstance injectRow(String template, String label, String rowKey) {

        ElementInstance row = menuManager.inject(recorderMenu, EditorSetting.RECORDER_ENTRY_KEY_ROWS, template);
        ElementInstance name = row.findChildById(RecorderSetting.ELEMENT_KEY_NAME);
        ElementInstance strip = row.findChildById(EditorSetting.RECORDER_ELEMENT_KEY_STRIP);

        name.setActionArgOverride(rowKey);
        name.findChildById(RecorderSetting.ELEMENT_ROW_LABEL).setFontText(label);
        strip.setOnDragArgOverride(rowKey);
        return strip;
    }

    private String toRowKey(int trackIndex, int row) {
        return trackIndex + EditorSetting.RECORDER_KEY_ROW_SEPARATOR + row;
    }

    // Every key of the curve within the view, inside its clip or not, one mark however many share a step, every
    // selected one always marked and kept so a drag can move them alone
    private void injectKeys(
            ElementInstance strip,
            RecordingClipStruct clip,
            RecordingCurveTrackStruct track,
            int slot) {

        RecordingKeySelectionStruct selection = recorderManager.getKeySelection();
        RecordingCurveStruct curve = track.getCurve(slot);
        int selectedEntry = selection.findFirstOn(track, slot);

        clearSteps();

        for (int i = 0; i < curve.size(); i++) {

            boolean selected = selection.isAt(selectedEntry, track, slot, i);

            if (selected)
                selectedEntry++;

            int step = resolveKeyStep(clip, curve.getTime(i));

            if (step == EngineSetting.INDEX_NOT_FOUND || !selected && stepTaken[step])
                continue;

            stepTaken[step] = true;

            ElementInstance diamond = injectDiamond(strip, selected
                    ? RecorderSetting.TEMPLATE_KEY_DIAMOND_SELECTED
                    : RecorderSetting.TEMPLATE_KEY_DIAMOND, step);

            if (selected)
                keepSelectedDiamond(diamond, track, slot, i);
        }
    }

    private ElementInstance injectDiamond(ElementInstance strip, String template, int step) {

        DimensionVector2Struct position = recorderRowSystem.getStepPosition(step);

        return menuManager.inject(strip, template, diamond -> {
            diamond.setPositionOverride(position);
            diamond.setRotation(RecorderSetting.KEY_DIAMOND_DEGREES);
        });
    }

    // Each selected key's mark follows its key along its strip, set aside while it lies outside the view
    private void moveSelectedDiamonds(RecordingClipStruct clip) {

        if (clip == null)
            return;

        for (int i = 0; i < selectedDiamonds.size(); i++) {

            RecordingCurveTrackStruct track = selectedDiamondTracks.get(i);
            int slot = selectedDiamondSlots.getInt(i);
            int key = selectedDiamondKeys.getInt(i);

            if (!track.hasCurve(slot) || key >= track.getCurve(slot).size())
                continue;

            int step = resolveKeyStep(clip, track.getCurve(slot).getTime(key));

            selectedDiamonds.get(i).setPositionOverride(step == EngineSetting.INDEX_NOT_FOUND
                    ? recorderRowSystem.getHiddenPosition()
                    : recorderRowSystem.getStepPosition(step));
        }
    }

    // The step across the view a key of the take falls on, -1 outside the view
    private int resolveKeyStep(RecordingClipStruct clip, float takeTime) {

        float time = clip.toTimelineTime(takeTime);
        RecordingViewStruct view = recorderManager.getView();

        if (!view.contains(time))
            return EngineSetting.INDEX_NOT_FOUND;

        return recorderRowSystem.toViewStep(time);
    }

    private void clearSteps() {
        for (int i = 0; i < stepTaken.length; i++) {
            stepTaken[i] = false;
            stepSelected[i] = false;
        }
    }

    // The character's movements or gestures on the selected clip across the view, each named
    private void injectNodes(ElementInstance strip, RecordingBodyTrackStruct track, RecorderNodeKind kind) {

        ObjectArrayList<RecordingNodeStruct> nodes = recorderManager.getNodes();
        RecordingViewStruct view = recorderManager.getView();
        int clipIndex = recorderManager.getSelectedClip();

        for (int i = 0; i < nodes.size(); i++) {

            RecordingNodeStruct node = nodes.get(i);

            if (node.getClipIndex() != clipIndex || node.getTrack() != track || node.getKind() != kind
                    || node.getEnd() <= view.getStart() || node.getStart() >= view.getEnd())
                continue;

            int fromStep = recorderRowSystem.toViewStep(node.getStart());
            int steps = recorderRowSystem.toViewStep(node.getEnd()) - fromStep;

            if (steps <= 0)
                continue;

            boolean selected = i == recorderManager.getSelectedNode();
            String template = kind == RecorderNodeKind.MOVEMENT
                    ? selected ? RecorderSetting.TEMPLATE_KEY_MOVEMENT_SELECTED : RecorderSetting.TEMPLATE_KEY_MOVEMENT
                    : selected ? RecorderSetting.TEMPLATE_KEY_GESTURE_SELECTED : RecorderSetting.TEMPLATE_KEY_GESTURE;

            injectBar(strip, template, fromStep, steps, node.getLabel());
        }
    }

    // A named bar across a row's strip from a step for a number of steps; the bar comes back
    private ElementInstance injectBar(ElementInstance strip, String template, int fromStep, int steps, String label) {

        DimensionVector2Struct position = recorderRowSystem.getStepPosition(fromStep);
        DimensionVector2Struct size = new DimensionVector2Struct(
                DimensionValueStruct.ofPercent(recorderRowSystem.toStepPercent(steps)),
                DimensionValueStruct.ofAbsolute(RecorderSetting.KEY_NODE_PIXELS));
        ElementInstance bar = menuManager.inject(strip, template, injected -> {
            injected.setPositionOverride(position);
            injected.setSizeOverride(size);
        });

        bar.findChildById(RecorderSetting.ELEMENT_NODE_LABEL).setFontText(label);
        return bar;
    }

    // Sounds \\

    // A row for each sound the Sounds layer names
    private void injectSounds(RecordingSoundTrackStruct track, RecordingClipStruct clip) {
        for (int nameIndex = 0; nameIndex < track.getSoundNames().size(); nameIndex++)
            injectSoundRow(track, clip, nameIndex);
    }

    // One sound's row with a bar across every time it is heard within the clip and the view, at least a step wide;
    // a bar lying wholly within one drawn before it is left out unless it is the one selected
    private void injectSoundRow(RecordingSoundTrackStruct track, RecordingClipStruct clip, int nameIndex) {

        ElementInstance strip = injectRow(
                RecorderSetting.TEMPLATE_KEY_NODE_ROW,
                String.format(RecorderSetting.FORMAT_SOUND_ROW,
                        track.getSoundNames().get(nameIndex), recorderManager.countSounds(nameIndex)),
                toRowKey(EditorSetting.RECORDER_KEY_TRACK_SOUNDS, nameIndex));
        RecordingViewStruct view = recorderManager.getView();
        int selectedSound = recorderManager.getSelectedSound();
        int coveredStep = EngineSetting.INDEX_NOT_FOUND;

        for (int i = 0; i < track.size(); i++) {

            if (track.getNameIndex(i) != nameIndex || !recorderManager.isSoundHeardIn(clip, i))
                continue;

            float start = Math.max(clip.toTimelineTime(track.getTime(i)), clip.getStart());
            float end = Math.min(clip.toTimelineTime(track.getTime(i) + track.getDuration(i)), clip.getEnd());

            if (end <= view.getStart() || start >= view.getEnd())
                continue;

            int fromStep = Math.min(recorderRowSystem.toViewStep(start), RecorderSetting.TIMELINE_STEPS - 1);
            int toStep = Math.max(fromStep + 1, recorderRowSystem.toViewStep(end));
            boolean selected = i == selectedSound;

            if (!selected && toStep <= coveredStep)
                continue;

            coveredStep = Math.max(coveredStep, toStep);

            ElementInstance bar = injectBar(strip, selected
                    ? RecorderSetting.TEMPLATE_KEY_GESTURE_SELECTED
                    : RecorderSetting.TEMPLATE_KEY_GESTURE, fromStep, toStep - fromStep, RecorderSetting.TEXT_EMPTY);

            if (selected)
                this.selectedSoundBar = bar;
        }
    }

    // The selected sound's bar follows where it would land while it is carried, set aside while that lies out of view
    private void moveSelectedSoundBar() {

        RecordingDragStruct drag = recorderManager.getDrag();

        if (selectedSoundBar == null || drag.getKind() != RecorderDragKind.SOUND_EVENT || !drag.isMoved())
            return;

        float landing = drag.getLandingStart();

        selectedSoundBar.setPositionOverride(recorderManager.getView().contains(landing)
                ? recorderRowSystem.getStepPosition(
                        Math.min(recorderRowSystem.toViewStep(landing), RecorderSetting.TIMELINE_STEPS - 1))
                : recorderRowSystem.getHiddenPosition());
    }
}
