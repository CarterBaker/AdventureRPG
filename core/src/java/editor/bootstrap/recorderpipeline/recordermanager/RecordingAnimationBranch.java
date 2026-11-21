package editor.bootstrap.recorderpipeline.recordermanager;

import application.bootstrap.animationpipeline.animation.AnimationChannel;
import application.bootstrap.animationpipeline.util.AnimationCurveArpgUtility;
import application.bootstrap.geometrypipeline.rig.RigHandle;
import application.bootstrap.geometrypipeline.rigmanager.RigManager;
import application.bootstrap.musicpipeline.util.NoteScale;
import application.bootstrap.musicpipeline.util.SongArpgUtility;
import application.bootstrap.worldpipeline.worldmanager.WorldManager;
import editor.bootstrap.infopipeline.infoentry.InfoEntryStruct;
import editor.bootstrap.infopipeline.infomanager.InfoManager;
import editor.bootstrap.recorderpipeline.recording.RecorderChannel;
import editor.bootstrap.recorderpipeline.recording.RecordingBoneTrackStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingClipStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingExportStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingCurveTrackStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingInstance;
import editor.bootstrap.recorderpipeline.recording.RecordingMorphTrackStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingTakeStruct;
import editor.runtime.characterview.util.CharacterViewUtility;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.root.UtilityPackage.InternalException;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.mathematics.curves.CurveEnds;
import engine.util.mathematics.curves.CurveStruct;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class RecordingAnimationBranch extends BranchPackage {

    /*
     * Opens an animation clip on the timeline and writes it back, the clip's
     * ARPG as InfoManager holds it being what both read. Opened, a clip is a
     * recording of one take for each of its layers, each laid once from its
     * start on a lane of its own named after it, as a sequencer stacks the
     * layers of one character's motion: the clip's own tracks on the first,
     * its morph curves with them, then the clip's own tracks of each body
     * part a stamp joined, one lane to each part the rig records, as long as
     * lanes remain, then every part a stamp joined with motion of its own on
     * the next, in the order the clip lists them. A layer holds a bone track
     * for every bone of the rig it keys — the clip's own layer every bone no
     * other layer holds — each axis of each channel a curve with the clip's own keys, modes and
     * handles, nested under the nearest bone above it the layer holds; every
     * layer runs as long as the clip's latest key, so they read as the one
     * clip they play as, and never shorter than the floor that leaves room
     * to key an empty clip. Written, every bone track that holds a key is
     * laid back in its layer and every other removed, the morph curves with
     * it, and every other field of the clip, and every track of a bone the
     * timeline does not show, is kept as it was read. A clip that cannot be
     * read, or holds more layers than the timeline has lanes, is refused,
     * and why is kept for the status line.
     */

    // Internal
    private InfoManager infoManager;
    private RigManager rigManager;
    private WorldManager worldManager;

    // Write
    private CurveStruct[] curveScratch;
    private boolean writing;

    // Status
    private String refusal;

    // Base \\

    @Override
    protected void create() {

        // Write
        this.curveScratch = new CurveStruct[AnimationChannel.SLOT_COUNT];
    }

    @Override
    protected void get() {
        this.infoManager = get(InfoManager.class);
        this.rigManager = get(RigManager.class);
        this.worldManager = get(WorldManager.class);
    }

    // Read \\

    // The clip open on the timeline as a recording, or null when it cannot be read
    RecordingInstance parse(ArpgObjectStruct clipArpg, String clipName) {

        try {
            return parseClip(clipArpg, clipName);
        } catch (InternalException e) {
            this.refusal = e.getMessage();
            return null;
        }
    }

    private RecordingInstance parseClip(ArpgObjectStruct clipArpg, String clipName) {

        RigHandle rigHandle = rigManager.getRigHandleFromRigName(
                ArpgUtility.validateString(clipArpg, EngineSetting.ANIMATION_FIELD_RIG));
        CurveEnds ends = AnimationCurveArpgUtility.resolveEnds(
                ArpgUtility.getBoolean(clipArpg, EngineSetting.ANIMATION_FIELD_LOOP, false));
        ObjectArrayList<String> partNames = readPartNames(clipArpg);

        if (partNames.size() + 1 > EngineSetting.SONG_LANE_COUNT)
            throwException("Clip \"" + clipName + "\" has more layers than the timeline has lanes");

        ArpgArrayStruct stamps = readStamps(rigHandle, EngineSetting.SONG_LANE_COUNT - 1 - partNames.size());
        ObjectArrayList<RecordingTakeStruct> layers = new ObjectArrayList<>(partNames.size() + stamps.size() + 1);

        layers.add(new RecordingTakeStruct(EditorSetting.RECORDER_ANIMATION_BASE_LAYER, RecorderChannel.NONE));

        for (int i = 0; i < stamps.size(); i++)
            layers.add(new RecordingTakeStruct(readStampName(stamps, i), RecorderChannel.NONE));

        for (int i = 0; i < partNames.size(); i++)
            layers.add(new RecordingTakeStruct(partNames.get(i), RecorderChannel.NONE));

        readBones(clipArpg, rigHandle, partNames, stamps, ends, layers, clipName);
        layers.get(0).setMorphTrack(readMorphs(clipArpg, ends, clipName));

        RecordingInstance recording = create(RecordingInstance.class);

        recording.constructor(
                clipName,
                worldManager.getActiveWorld().getWorldName(),
                SongArpgUtility.createTempo(EditorSetting.RECORDER_DEFAULT_BPM),
                EngineSetting.DEFAULT_SONG_BEATS_PER_BAR,
                EngineSetting.DEFAULT_SONG_BEAT_UNIT,
                EngineSetting.DEFAULT_SONG_KEY_ROOT,
                NoteScale.MAJOR,
                new RecordingExportStruct());

        for (int lane = 0; lane < layers.size(); lane++) {
            recording.addTake(layers.get(lane));
            recording.addClip(RecordingClipStruct.ofTake(layers.get(lane), 0f, 0f, lane, 0f, RecorderChannel.NONE));
            recording.getLane(lane).setName(layers.get(lane).getName());
        }

        recording.setAnimation(clipName, clipArpg.deepCopy().getAsObject());
        fitSpan(recording);

        return recording;
    }

    // The parts the clip joined, in the order it lists them
    private static ObjectArrayList<String> readPartNames(ArpgObjectStruct clipArpg) {

        ObjectArrayList<String> partNames = new ObjectArrayList<>();
        ArpgObjectStruct parts = CharacterViewUtility.findObject(clipArpg, EngineSetting.ANIMATION_FIELD_PARTS);

        if (parts != null)
            partNames.addAll(parts.keySet());

        return partNames;
    }

    // The rig's records of the bones each joined body part brought, as many as the lanes left take, empty when it
    // records none
    private ArpgArrayStruct readStamps(RigHandle rigHandle, int laneCount) {

        ArpgArrayStruct stamps = new ArpgArrayStruct();
        InfoEntryStruct entry = infoManager.findEntry(
                EditorSetting.INFO_SCHEMA_RIGS,
                rigHandle.getRigName(),
                rigHandle.getRigName());
        ArpgArrayStruct recorded = entry != null
                && ArpgUtility.hasArray(entry.getArpg(), EditorSetting.CHARACTER_RIG_FIELD_STAMPS)
                        ? entry.getArpg().getAsArray(EditorSetting.CHARACTER_RIG_FIELD_STAMPS)
                        : null;

        for (int i = 0; recorded != null && i < recorded.size() && stamps.size() < laneCount; i++)
            stamps.add(recorded.get(i));

        return stamps;
    }

    private static String readStampName(ArpgArrayStruct stamps, int stampIndex) {
        return ArpgUtility.validateString(stamps.get(stampIndex).getAsObject(), EditorSetting.CHARACTER_RIG_FIELD_NAME);
    }

    // The record holding a bone by its name, or INDEX_NOT_FOUND when none does
    private static int findStamp(ArpgArrayStruct stamps, String boneName) {

        for (int s = 0; s < stamps.size(); s++) {

            ArpgArrayStruct stampBones = ArpgUtility.validateArray(
                    stamps.get(s).getAsObject(), EditorSetting.CHARACTER_RIG_FIELD_BONES);

            for (int b = 0; b < stampBones.size(); b++)
                if (boneName.equals(stampBones.get(b).getAsString()))
                    return s;
        }

        return EngineSetting.INDEX_NOT_FOUND;
    }

    // Every bone of the rig but its spring bones laid in the layer that keys it — a joined part's own motion, else
    // the lane of the body part that brought it, else the clip's own — under the nearest bone above it that layer
    // holds
    private void readBones(
            ArpgObjectStruct clipArpg,
            RigHandle rigHandle,
            ObjectArrayList<String> partNames,
            ArpgArrayStruct stamps,
            CurveEnds ends,
            ObjectArrayList<RecordingTakeStruct> layers,
            String clipName) {

        int boneCount = rigHandle.getBoneCount();
        int[] boneLayers = new int[boneCount];
        String[] boneParts = new String[boneCount];

        for (int i = 0; i < boneCount; i++) {

            String boneName = rigHandle.getBone(i).getName();
            String partName = findPart(clipArpg, boneName);
            int stamp = findStamp(stamps, boneName);

            boneParts[i] = partName;
            boneLayers[i] = rigHandle.isSpringBone(i)
                    ? EngineSetting.INDEX_NOT_FOUND
                    : partName != null ? stamps.size() + partNames.indexOf(partName) + 1 : stamp + 1;
        }

        for (int i = 0; i < boneCount; i++) {

            if (boneLayers[i] == EngineSetting.INDEX_NOT_FOUND)
                continue;

            int parent = rigHandle.getBone(i).getParentIndex();

            while (parent >= 0 && boneLayers[parent] != boneLayers[i])
                parent = rigHandle.getBone(parent).getParentIndex();

            String parentName = parent >= 0 ? rigHandle.getBone(parent).getName() : null;
            int layer = boneLayers[i];

            layers.get(layer).getBoneTracks().add(readBone(
                    clipArpg,
                    rigHandle.getBone(i).getName(),
                    boneParts[i],
                    parentName,
                    ends,
                    clipName));
        }
    }

    // A bone's track with the clip's keys for it, from the joined part that keys it or the clip's own tracks
    private RecordingBoneTrackStruct readBone(
            ArpgObjectStruct clipArpg,
            String boneName,
            String partName,
            String parentName,
            CurveEnds ends,
            String clipName) {

        RecordingBoneTrackStruct track = new RecordingBoneTrackStruct(boneName, partName, parentName, ends);
        ArpgObjectStruct tracks = findTracks(clipArpg, partName);

        if (tracks == null || !ArpgUtility.hasObject(tracks, boneName))
            return track;

        CurveStruct[] curves = AnimationCurveArpgUtility.readTrack(
                tracks.getAsObject(boneName), ends, boneName, clipName);

        for (int slot = 0; slot < curves.length; slot++)
            if (curves[slot] != null)
                track.getCurve(slot).copyKeys(curves[slot]);

        return track;
    }

    // The clip's morph curves, one slot each in the order it lists them, null when it keys none
    private static RecordingMorphTrackStruct readMorphs(ArpgObjectStruct clipArpg, CurveEnds ends, String clipName) {

        ArpgObjectStruct morphs = CharacterViewUtility.findObject(clipArpg, EngineSetting.ANIMATION_FIELD_MORPHS);

        if (morphs == null || morphs.isEmpty())
            return null;

        ObjectArrayList<String> morphNames = new ObjectArrayList<>(morphs.keySet());
        RecordingMorphTrackStruct track = new RecordingMorphTrackStruct(morphNames, ends);

        for (int slot = 0; slot < morphNames.size(); slot++)
            AnimationCurveArpgUtility.readCurve(
                    ArpgUtility.validateArray(morphs, morphNames.get(slot)),
                    track.getCurve(slot),
                    morphNames.get(slot),
                    clipName);

        return track;
    }

    // The joined part whose tracks key the bone, null when the clip's own do or nothing does
    private static String findPart(ArpgObjectStruct clipArpg, String boneName) {

        ArpgObjectStruct parts = CharacterViewUtility.findObject(clipArpg, EngineSetting.ANIMATION_FIELD_PARTS);

        if (parts == null)
            return null;

        for (String partName : parts.keySet()) {

            ArpgObjectStruct tracks = findTracks(clipArpg, partName);

            if (tracks != null && ArpgUtility.hasObject(tracks, boneName))
                return partName;
        }

        return null;
    }

    private static ArpgObjectStruct findTracks(ArpgObjectStruct clipArpg, String partName) {

        if (partName == null)
            return CharacterViewUtility.findObject(clipArpg, EngineSetting.ANIMATION_FIELD_TRACKS);

        ArpgObjectStruct parts = CharacterViewUtility.findObject(clipArpg, EngineSetting.ANIMATION_FIELD_PARTS);
        ArpgObjectStruct part = parts != null ? CharacterViewUtility.findObject(parts, partName) : null;

        return part != null ? CharacterViewUtility.findObject(part, EngineSetting.ANIMATION_FIELD_TRACKS) : null;
    }

    // Length \\

    // Every layer laid again to run as long as the clip's keys now reach
    void fitSpan(RecordingInstance recording) {

        float duration = resolveDuration(recording);

        for (int i = 0; i < recording.getTakes().size(); i++)
            recording.getTakes().get(i).setDuration(duration);

        for (int i = 0; i < recording.getClips().size(); i++)
            recording.getClips().get(i).setSpan(0f, duration);
    }

    // The latest key of every curve of every layer, held to the floor an empty clip is keyed in
    private static float resolveDuration(RecordingInstance recording) {

        float duration = EditorSetting.ANIMATION_EDITOR_TIMELINE_MIN_SPAN;

        for (int i = 0; i < recording.getTakes().size(); i++) {

            RecordingTakeStruct layer = recording.getTakes().get(i);

            for (int b = 0; b < layer.getBoneTracks().size(); b++)
                duration = Math.max(duration, resolveEndTime(layer.getBoneTracks().get(b)));

            if (layer.getMorphTrack() != null)
                duration = Math.max(duration, resolveEndTime(layer.getMorphTrack()));
        }

        return duration;
    }

    private static float resolveEndTime(RecordingCurveTrackStruct track) {

        float endTime = 0f;

        for (int slot = 0; slot < track.getCurveCount(); slot++)
            if (!track.getCurve(slot).isEmpty())
                endTime = Math.max(endTime, track.getCurve(slot).getEndTime());

        return endTime;
    }

    // Write \\

    // The clip as its ARPG holds it now: every bone track laid back where it came from, the rest as it was read
    ArpgObjectStruct build(RecordingInstance recording) {

        ArpgObjectStruct clipArpg = recording.getAnimationArpg().deepCopy().getAsObject();

        for (int i = 0; i < recording.getTakes().size(); i++) {

            RecordingTakeStruct layer = recording.getTakes().get(i);

            for (int b = 0; b < layer.getBoneTracks().size(); b++)
                writeBone(clipArpg, layer.getBoneTracks().get(b));

            if (layer.getMorphTrack() != null)
                writeMorphs(clipArpg, layer.getMorphTrack());
        }

        return clipArpg;
    }

    private static void writeMorphs(ArpgObjectStruct clipArpg, RecordingMorphTrackStruct track) {

        ArpgObjectStruct morphs = CharacterViewUtility.requireObject(clipArpg, EngineSetting.ANIMATION_FIELD_MORPHS);

        for (int slot = 0; slot < track.getCurveCount(); slot++)
            if (!track.getCurve(slot).isEmpty())
                morphs.add(track.getMorphName(slot), AnimationCurveArpgUtility.writeCurve(track.getCurve(slot)));
    }

    private void writeBone(ArpgObjectStruct clipArpg, RecordingBoneTrackStruct track) {

        ArpgObjectStruct tracks = track.getPartName() != null
                ? findTracks(clipArpg, track.getPartName())
                : CharacterViewUtility.requireObject(clipArpg, EngineSetting.ANIMATION_FIELD_TRACKS);

        if (tracks == null)
            return;

        if (!track.isKeyed()) {
            tracks.remove(track.getBoneName());
            return;
        }

        for (int slot = 0; slot < curveScratch.length; slot++)
            curveScratch[slot] = track.getCurve(slot);

        tracks.add(track.getBoneName(), AnimationCurveArpgUtility.writeTrack(curveScratch));
    }

    // The clip written into InfoManager as it now stands, so the preview, the Info Panel and every tab see it
    void write(RecordingInstance recording) {

        ArpgObjectStruct built = build(recording);

        this.writing = true;

        try {
            infoManager.editEntry(
                    EditorSetting.INFO_SCHEMA_ANIMATIONS,
                    recording.getAnimationName(),
                    recording.getAnimationName(),
                    clip -> CharacterViewUtility.replaceObject(clip, built));
        } finally {
            this.writing = false;
        }

        recording.setAnimation(recording.getAnimationName(), built);
    }

    // Accessible \\

    // True while the clip is being written, so the change it makes is not read back as one made elsewhere
    boolean isWriting() {
        return writing;
    }

    String getRefusal() {
        return refusal;
    }
}
