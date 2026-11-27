package editor.bootstrap.recorderpipeline.recordermanager;

import editor.bootstrap.layerspipeline.layers.LayerNodeStruct;
import editor.bootstrap.recorderpipeline.recording.RecorderChannel;
import editor.bootstrap.recorderpipeline.recording.RecordingBoneTrackStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingClipStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingCurveTrackStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingInstance;
import editor.bootstrap.recorderpipeline.recording.RecordingTakeStruct;
import editor.runtime.EditorSetting;
import editor.runtime.characterview.panel.CharacterViewRowStruct;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class RecordingLayerBranch extends BranchPackage {

    /*
     * The Layers tab's stack for the recorder: the selected clip's take as
     * layers, in the order the take recorded them: the camera and the player,
     * the characters, items and vehicles each in a group of their own, the
     * weather, the clock and the Sounds layer, the one picked being what the
     * Keys tab shows; an animation clip layer's bones in a group of their
     * own, nested as its rig nests them, then its morphs. The takes
     * themselves are the Palette tab's rows: every take in the bin with its
     * channels and its length, the one picked being the one the take actions
     * lay, rename and delete, each dragged onto the Timeline's lanes to lay
     * it there. Every channel is also a layer whose
     * eye says whether it is in a mask, one stack for the Recorder's channels
     * armed for the next take and one for the Preview's, what it plays from
     * the timeline. Every key is a stable name: a track by its place in the
     * take, a take by its place in the bin, a group by what it holds, a
     * channel by its own key.
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

    // Every channel as a layer whose eye is open while the mask holds it
    void buildChannels(ObjectArrayList<LayerNodeStruct> roots, int channelMask) {

        RecorderChannel[] channels = RecorderChannel.VALUES;

        for (int i = 0; i < channels.length; i++)
            roots.add(new LayerNodeStruct(channels[i].getKey(), channels[i].getLabel(),
                    false, true, channels[i].isIn(channelMask), false));
    }

    String resolveTitle() {

        RecordingClipStruct clip = recorderManager.getKeyClip();

        return clip != null && clip.getTake() != null
                ? String.format(EditorSetting.RECORDER_LAYERS_TITLE_TAKE_FORMAT, clip.getTake().getName())
                : EditorSetting.RECORDER_LAYERS_TITLE_NO_CLIP;
    }

    // Every track of the take the Keys tab shows, the characters, items and vehicles grouped, then its sounds
    void buildLayers(ObjectArrayList<LayerNodeStruct> roots) {

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
        addBones(roots, take.getBoneTracks());
        addTrack(roots, take.getMorphTrack());

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

    // An animation clip layer's bones under one group, each nested under the nearest bone above it the layer holds,
    // a bone holding others still a layer of its own
    private void addBones(ObjectArrayList<LayerNodeStruct> roots, ObjectArrayList<RecordingBoneTrackStruct> bones) {

        if (bones.isEmpty())
            return;

        Object2ObjectOpenHashMap<String, ObjectArrayList<RecordingBoneTrackStruct>> parent2Children =
                new Object2ObjectOpenHashMap<>();
        ObjectArrayList<RecordingBoneTrackStruct> tops = new ObjectArrayList<>();

        for (int i = 0; i < bones.size(); i++) {

            RecordingBoneTrackStruct bone = bones.get(i);

            if (bone.getParentName() == null) {
                tops.add(bone);
                continue;
            }

            ObjectArrayList<RecordingBoneTrackStruct> siblings = parent2Children.get(bone.getParentName());

            if (siblings == null) {
                siblings = new ObjectArrayList<>();
                parent2Children.put(bone.getParentName(), siblings);
            }

            siblings.add(bone);
        }

        ObjectArrayList<LayerNodeStruct> children = new ObjectArrayList<>();

        for (int i = 0; i < tops.size(); i++)
            children.add(buildBone(tops.get(i), parent2Children));

        boolean holding = false;

        for (int i = 0; i < children.size(); i++)
            holding |= children.get(i).isActive();

        LayerNodeStruct group = new LayerNodeStruct(
                EditorSetting.RECORDER_LAYER_KEY_BONES, EditorSetting.RECORDER_LAYER_LABEL_BONES, holding, true);

        for (int i = 0; i < children.size(); i++)
            group.addChild(children.get(i));

        roots.add(group);
    }

    private LayerNodeStruct buildBone(
            RecordingBoneTrackStruct bone,
            Object2ObjectOpenHashMap<String, ObjectArrayList<RecordingBoneTrackStruct>> parent2Children) {

        ObjectArrayList<RecordingBoneTrackStruct> below = parent2Children.get(bone.getBoneName());
        ObjectArrayList<LayerNodeStruct> children = new ObjectArrayList<>();
        int trackIndex = recorderManager.getKeyTracks().indexOf(bone);
        boolean chosen = trackIndex != EngineSetting.INDEX_NOT_FOUND && recorderManager.getKeyLayer() == trackIndex;
        boolean holding = chosen;

        for (int i = 0; below != null && i < below.size(); i++) {

            LayerNodeStruct child = buildBone(below.get(i), parent2Children);

            children.add(child);
            holding |= child.isActive();
        }

        LayerNodeStruct node = new LayerNodeStruct(
                EditorSetting.RECORDER_LAYER_KEY_TRACK_PREFIX + trackIndex, bone.getTrackLabel(), holding, true);

        for (int i = 0; i < children.size(); i++)
            node.addChild(children.get(i));

        if (chosen && !children.isEmpty())
            node.markChosen();

        return node;
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

    // The Palette tab's rows: every take in the bin with its channels and its length under the media file's name, the
    // one picked marked
    void buildTakeRows(ObjectArrayList<CharacterViewRowStruct> rows) {

        RecordingInstance recording = recorderManager.getRecording();

        if (recording == null) {
            rows.add(CharacterViewRowStruct.header(EditorSetting.RECORDER_LAYERS_TITLE_NO_RECORDING));
            return;
        }

        ObjectArrayList<RecordingTakeStruct> takes = recording.getTakes();

        rows.add(CharacterViewRowStruct.header(
                String.format(EditorSetting.RECORDER_PALETTE_TAKES_FORMAT, recording.getRecordingName())));

        for (int i = 0; i < takes.size(); i++)
            rows.add(CharacterViewRowStruct.item(
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

    // A track's layer shows its keys and the Sounds layer its sounds; a group only folds
    void selectLayer(String layerKey) {

        if (layerKey.equals(EditorSetting.RECORDER_LAYER_KEY_SOUNDS))
            recorderManager.selectKeyLayer(EditorSetting.RECORDER_KEY_TRACK_SOUNDS);
        else if (layerKey.startsWith(EditorSetting.RECORDER_LAYER_KEY_TRACK_PREFIX))
            recorderManager.selectKeyLayer(Integer.parseInt(
                    layerKey.substring(EditorSetting.RECORDER_LAYER_KEY_TRACK_PREFIX.length())));
    }

    // The take a Palette row's key names, -1 for any other key
    int findTake(String layerKey) {
        return layerKey.startsWith(EditorSetting.RECORDER_LAYER_KEY_TAKE_PREFIX)
                ? Integer.parseInt(layerKey.substring(EditorSetting.RECORDER_LAYER_KEY_TAKE_PREFIX.length()))
                : EngineSetting.INDEX_NOT_FOUND;
    }
}
