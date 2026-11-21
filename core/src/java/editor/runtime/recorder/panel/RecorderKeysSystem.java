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
import editor.bootstrap.recorderpipeline.recording.RecordingBoneTrackStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingClipStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingCurveStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingCurveTrackStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingDragStruct;
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
     * marking every key of any of its values, the one picked marked, its values
     * listed in the Properties tab and the one chosen there drawn on the Keys
     * Graph; the Sounds layer is a row for each sound it names with a bar for
     * every time it is heard, the selected one marked and following the cursor
     * while it is dragged. The tab is drawn again when the recorder's revision,
     * its keys or its view move; keys being dragged move only the selected
     * marks on their strip, a sound carried only its bar, and the playhead
     * follows every frame from positions built once.
     */

    // Internal
    private MenuManager menuManager;
    private RecorderManager recorderManager;
    private RecorderRowSystem recorderRowSystem;

    // Menus
    private MenuInstance recorderMenu;
    private ElementInstance keysTitle;
    private RecorderPlayheadStruct playhead;

    // Strips
    private boolean[] stepTaken;
    private DimensionVector2Struct hiddenPosition;
    private ObjectArrayList<ElementInstance> selectedDiamonds;
    private IntArrayList selectedDiamondKeys;
    private ElementInstance selectedSoundBar;

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
        this.hiddenPosition = new DimensionVector2Struct(
                DimensionValueStruct.ofAbsolute(RecorderSetting.HIDDEN_PANEL_PIXELS),
                DimensionValueStruct.ofAbsolute(0f));
        this.selectedDiamonds = new ObjectArrayList<>();
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
        recorderRowSystem.requireEntryPoint(EditorSetting.RECORDER_ENTRY_KEY_STRIPS);
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
    }

    // Keys being dragged or a sound carried: only the selected marks on their strip and the selected sound's bar move
    private void refreshCurve() {

        this.shownCurveRevision = recorderManager.getCurveRevision();

        moveSelectedDiamonds(recorderManager.getKeyClip());
        moveSelectedSoundBar();
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
        else if (layer >= 0 && layer < tracks.size() && tracks.get(layer) instanceof RecordingBoneTrackStruct)
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

        for (int slot = 0; slot < track.getCurveCount(); slot++) {

            boolean selected = track == recorderManager.getSelectedTrack() && slot == recorderManager.getSelectedSlot();
            ElementInstance strip = injectRow(
                    selected ? RecorderSetting.TEMPLATE_KEY_CURVE_ROW_SELECTED : RecorderSetting.TEMPLATE_KEY_CURVE_ROW,
                    track.getCurveLabel(slot),
                    toRowKey(trackIndex, slot));

            if (track.hasCurve(slot))
                injectKeys(strip, clip, track.getCurve(slot), selected);
        }
    }

    // A summary row for every track of the animation layer, the one picked marked; its values are the Properties
    // tab's, and the one chosen there is the Keys Graph's
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
        }
    }

    // One mark for every step of the view where any value of the track holds a key
    private void injectSummaryKeys(ElementInstance strip, RecordingClipStruct clip, RecordingCurveTrackStruct track) {

        clearSteps();

        for (int slot = 0; slot < track.getCurveCount(); slot++) {

            if (!track.hasCurve(slot))
                continue;

            RecordingCurveStruct curve = track.getCurve(slot);

            for (int i = 0; i < curve.size(); i++) {

                int step = resolveKeyStep(clip, curve.getTime(i));

                if (step == EngineSetting.INDEX_NOT_FOUND || stepTaken[step])
                    continue;

                stepTaken[step] = true;
                injectDiamond(strip, RecorderSetting.TEMPLATE_KEY_DIAMOND, step);
            }
        }
    }

    // One row, its name and its strip both handing back the row's key; its strip comes back
    private ElementInstance injectRow(String template, String label, String rowKey) {

        ElementInstance row = menuManager.inject(recorderMenu, EditorSetting.RECORDER_ENTRY_KEY_ROWS, template);
        ElementInstance name = row.findChildById(RecorderSetting.ELEMENT_KEY_NAME);
        ElementInstance strip = row.findChildById(RecorderSetting.ELEMENT_KEY_STRIP);

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
            RecordingCurveStruct curve,
            boolean marked) {

        IntArrayList selectedKeys = recorderManager.getSelectedKeys();
        int selectedSlot = 0;

        clearSteps();

        for (int i = 0; i < curve.size(); i++) {

            boolean selected = marked && selectedSlot < selectedKeys.size() && selectedKeys.getInt(selectedSlot) == i;

            if (selected)
                selectedSlot++;

            int step = resolveKeyStep(clip, curve.getTime(i));

            if (step == EngineSetting.INDEX_NOT_FOUND || !selected && stepTaken[step])
                continue;

            stepTaken[step] = true;

            ElementInstance diamond = injectDiamond(strip, selected
                    ? RecorderSetting.TEMPLATE_KEY_DIAMOND_SELECTED
                    : RecorderSetting.TEMPLATE_KEY_DIAMOND, step);

            if (!selected)
                continue;

            selectedDiamonds.add(diamond);
            selectedDiamondKeys.add(i);
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

        RecordingCurveStruct curve = recorderManager.getSelectedCurve();

        if (clip == null || curve == null)
            return;

        for (int i = 0; i < selectedDiamonds.size(); i++) {

            int key = selectedDiamondKeys.getInt(i);

            if (key >= curve.size())
                continue;

            int step = resolveKeyStep(clip, curve.getTime(key));

            selectedDiamonds.get(i).setPositionOverride(step == EngineSetting.INDEX_NOT_FOUND
                    ? hiddenPosition
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
        for (int i = 0; i < stepTaken.length; i++)
            stepTaken[i] = false;
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
                : hiddenPosition);
    }
}
