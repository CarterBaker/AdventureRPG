package editor.bootstrap.recorderpipeline.recordermanager;

import editor.bootstrap.layerspipeline.layers.LayerNodeStruct;
import editor.bootstrap.recorderpipeline.recording.RecorderChannel;
import editor.bootstrap.recorderpipeline.recording.RecorderLayerView;
import editor.bootstrap.recorderpipeline.recording.RecordingClipStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingCurveTrackStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingInstance;
import editor.bootstrap.recorderpipeline.recording.RecordingTakeStruct;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class RecordingLayerBranch extends BranchPackage {

    /*
     * The Layers tab's stack for the recorder. In the Layers view, the
     * selected clip's take as layers, in the order the take recorded them:
     * the camera and the player, the characters, items and vehicles each in a
     * group of their own, the weather, the clock and the Sounds layer, the
     * one picked being what the Keys tab shows; an animation clip's bones
     * in a group of their own. In the Takes view, every take
     * in the bin with its channels and its length, the one picked being the
     * one the take actions lay, rename and delete, each dragged onto the
     * Timeline's lanes to lay it there. Every key is a stable name:
     * a track by its place in the take, a take by its place in the bin, a
     * group by what it holds.
     */

    // Internal
    private RecorderManager recorderManager;

    // Scratch
    private StringBuilder labelScratch;

    // Base \\

    @Override
    protected void create() {

        // Scratch
        this.labelScratch = new StringBuilder();
    }

    @Override
    protected void get() {
        this.recorderManager = get(RecorderManager.class);
    }

    // Build \\

    void buildLayers(ObjectArrayList<LayerNodeStruct> roots, RecorderLayerView view) {

        if (view == RecorderLayerView.TAKES)
            buildTakes(roots);
        else
            buildTracks(roots);
    }

    String resolveTitle(RecorderLayerView view) {

        RecordingInstance recording = recorderManager.getRecording();

        if (view == RecorderLayerView.TAKES)
            return recording != null
                    ? String.format(EditorSetting.RECORDER_LAYERS_TITLE_TAKES_FORMAT, recording.getRecordingName())
                    : EditorSetting.RECORDER_LAYERS_TITLE_NO_RECORDING;

        RecordingClipStruct clip = recorderManager.getKeyClip();

        return clip != null && clip.getTake() != null
                ? String.format(EditorSetting.RECORDER_LAYERS_TITLE_TAKE_FORMAT, clip.getTake().getName())
                : EditorSetting.RECORDER_LAYERS_TITLE_NO_CLIP;
    }

    // Every track of the take the Keys tab shows, the characters, items and vehicles grouped, then its sounds
    private void buildTracks(ObjectArrayList<LayerNodeStruct> roots) {

        RecordingClipStruct clip = recorderManager.getKeyClip();
        RecordingTakeStruct take = clip != null ? clip.getTake() : null;

        if (take == null)
            return;

        addTrack(roots, take.getCameraTrack());
        addTrack(roots, take.getPlayerTrack());
        addGroup(roots, EditorSetting.RECORDER_LAYER_KEY_CHARACTERS, EditorSetting.RECORDER_LAYER_LABEL_CHARACTERS,
                take.getResidentTracks());
        addGroup(roots, EditorSetting.RECORDER_LAYER_KEY_ITEMS, EditorSetting.RECORDER_LAYER_LABEL_ITEMS,
                take.getProjectileTracks());
        addGroup(roots, EditorSetting.RECORDER_LAYER_KEY_VEHICLES, EditorSetting.RECORDER_LAYER_LABEL_VEHICLES,
                take.getVehicleTracks());
        addTrack(roots, take.getWeatherTrack());
        addTrack(roots, take.getClockTrack());
        addGroup(roots, EditorSetting.RECORDER_LAYER_KEY_BONES, EditorSetting.RECORDER_LAYER_LABEL_BONES,
                take.getBoneTracks());

        if (take.getSoundTrack() != null)
            roots.add(new LayerNodeStruct(
                    EditorSetting.RECORDER_LAYER_KEY_SOUNDS,
                    String.format(EditorSetting.RECORDER_LAYER_SOUNDS_FORMAT, take.getSoundTrack().size()),
                    recorderManager.getKeyLayer() == EditorSetting.RECORDER_KEY_TRACK_SOUNDS,
                    false));
    }

    // A group holding a layer for each of its tracks that recorded anything, left out when none did
    private void addGroup(
            ObjectArrayList<LayerNodeStruct> roots,
            String groupKey,
            String label,
            ObjectArrayList<? extends RecordingCurveTrackStruct> tracks) {

        ObjectArrayList<LayerNodeStruct> children = new ObjectArrayList<>();

        for (int i = 0; i < tracks.size(); i++)
            addTrack(children, tracks.get(i));

        if (children.isEmpty())
            return;

        boolean holding = false;

        for (int i = 0; i < children.size(); i++)
            holding |= children.get(i).isActive();

        LayerNodeStruct group = new LayerNodeStruct(groupKey, label, holding, true);

        for (int i = 0; i < children.size(); i++)
            group.addChild(children.get(i));

        roots.add(group);
    }

    // A track's layer, keyed by its place among the take's tracks; a track that recorded nothing is no layer
    private void addTrack(ObjectArrayList<LayerNodeStruct> layers, RecordingCurveTrackStruct track) {

        int trackIndex = track != null
                ? recorderManager.getKeyTracks().indexOf(track)
                : EngineSetting.INDEX_NOT_FOUND;

        if (trackIndex == EngineSetting.INDEX_NOT_FOUND)
            return;

        layers.add(new LayerNodeStruct(
                EditorSetting.RECORDER_LAYER_KEY_TRACK_PREFIX + trackIndex,
                track.getTrackLabel(),
                recorderManager.getKeyLayer() == trackIndex,
                false));
    }

    // Every take in the bin with its channels and its length, the one picked active
    private void buildTakes(ObjectArrayList<LayerNodeStruct> roots) {

        RecordingInstance recording = recorderManager.getRecording();

        if (recording == null)
            return;

        ObjectArrayList<RecordingTakeStruct> takes = recording.getTakes();

        for (int i = 0; i < takes.size(); i++)
            roots.add(LayerNodeStruct.draggable(
                    EditorSetting.RECORDER_LAYER_KEY_TAKE_PREFIX + i,
                    describeTake(takes.get(i)),
                    i == recorderManager.getSelectedTake()));
    }

    private String describeTake(RecordingTakeStruct take) {

        labelScratch.setLength(0);

        for (RecorderChannel channel : RecorderChannel.VALUES) {

            if (!channel.isIn(take.getChannelMask()))
                continue;

            if (!labelScratch.isEmpty())
                labelScratch.append(EditorSetting.RECORDER_LAYER_CHANNEL_SEPARATOR);

            labelScratch.append(channel.getLabel());
        }

        return String.format(EditorSetting.RECORDER_LAYER_TAKE_FORMAT,
                take.getName(), labelScratch, take.getDuration());
    }

    // Select \\

    // A track's layer shows its keys, the Sounds layer its sounds, and a take is picked for the take actions; a
    // group only folds
    void selectLayer(String layerKey) {

        if (layerKey.equals(EditorSetting.RECORDER_LAYER_KEY_SOUNDS))
            recorderManager.selectKeyLayer(EditorSetting.RECORDER_KEY_TRACK_SOUNDS);
        else if (layerKey.startsWith(EditorSetting.RECORDER_LAYER_KEY_TRACK_PREFIX))
            recorderManager.selectKeyLayer(Integer.parseInt(
                    layerKey.substring(EditorSetting.RECORDER_LAYER_KEY_TRACK_PREFIX.length())));
        else if (layerKey.startsWith(EditorSetting.RECORDER_LAYER_KEY_TAKE_PREFIX))
            recorderManager.selectTake(findTake(layerKey));
    }

    int findTake(String layerKey) {
        return layerKey.startsWith(EditorSetting.RECORDER_LAYER_KEY_TAKE_PREFIX)
                ? Integer.parseInt(layerKey.substring(EditorSetting.RECORDER_LAYER_KEY_TAKE_PREFIX.length()))
                : EngineSetting.INDEX_NOT_FOUND;
    }
}
