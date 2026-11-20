package editor.bootstrap.recorderpipeline.recordermanager;

import application.bootstrap.animationpipeline.animation.AnimationChannel;
import application.bootstrap.animationpipeline.util.AnimationCurveArpgUtility;
import application.bootstrap.geometrypipeline.rig.RigHandle;
import application.bootstrap.geometrypipeline.rigmanager.RigManager;
import application.bootstrap.musicpipeline.util.NoteScale;
import application.bootstrap.musicpipeline.util.SongArpgUtility;
import application.bootstrap.worldpipeline.worldmanager.WorldManager;
import editor.bootstrap.infopipeline.infomanager.InfoManager;
import editor.bootstrap.recorderpipeline.recording.RecorderChannel;
import editor.bootstrap.recorderpipeline.recording.RecordingBoneTrackStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingClipStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingExportStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingInstance;
import editor.bootstrap.recorderpipeline.recording.RecordingTakeStruct;
import editor.runtime.characterview.util.CharacterViewUtility;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.root.UtilityPackage.InternalException;
import engine.util.arpg.ArpgElementStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.mathematics.curves.CurveEnds;
import engine.util.mathematics.curves.CurveStruct;

class RecordingAnimationBranch extends BranchPackage {

    /*
     * Opens an animation clip on the timeline and writes it back, the clip's
     * ARPG as InfoManager holds it being what both read. Opened, a clip is a
     * recording of one take laid once from its start, holding a bone track
     * for every bone of its rig a clip may key, each axis of each channel a
     * curve with the clip's own keys, modes and handles, in the joined part
     * that keys it where one does; the take runs as long as its latest key,
     * morph curves included, and never shorter than the floor that leaves
     * room to key an empty clip. Written, every bone track that holds a key
     * is laid back where it came from and every other removed, and every
     * other field of the clip, and every track of a bone the timeline does
     * not show, is kept as it was read. A clip that cannot be read is
     * refused, and why is kept for the status line.
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
        RecordingTakeStruct take = new RecordingTakeStruct(clipName, RecorderChannel.NONE);

        for (int i = 0; i < rigHandle.getBoneCount(); i++)
            if (!rigHandle.isSpringBone(i))
                take.getBoneTracks().add(readBone(clipArpg, rigHandle.getBone(i).getName(), ends, clipName));

        float duration = resolveDuration(take, clipArpg);
        RecordingInstance recording = create(RecordingInstance.class);

        take.setDuration(duration);
        recording.constructor(
                clipName,
                worldManager.getActiveWorld().getWorldName(),
                SongArpgUtility.createTempo(EditorSetting.RECORDER_DEFAULT_BPM),
                EngineSetting.DEFAULT_SONG_BEATS_PER_BAR,
                EngineSetting.DEFAULT_SONG_BEAT_UNIT,
                EngineSetting.DEFAULT_SONG_KEY_ROOT,
                NoteScale.MAJOR,
                new RecordingExportStruct());
        recording.addTake(take);
        recording.addClip(RecordingClipStruct.ofTake(take, 0f, duration, 0, 0f, RecorderChannel.NONE));
        recording.setAnimation(clipName, clipArpg.deepCopy().getAsObject());

        return recording;
    }

    // A bone's track with the clip's keys for it, from the joined part that keys it or the clip's own tracks
    private RecordingBoneTrackStruct readBone(
            ArpgObjectStruct clipArpg,
            String boneName,
            CurveEnds ends,
            String clipName) {

        String partName = findPart(clipArpg, boneName);
        RecordingBoneTrackStruct track = new RecordingBoneTrackStruct(boneName, partName, ends);
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

    // The clip's take laid again to run as long as its keys now reach
    void fitSpan(RecordingInstance recording) {

        RecordingTakeStruct take = recording.getTakes().get(0);
        float duration = resolveDuration(take, recording.getAnimationArpg());

        take.setDuration(duration);
        recording.getClips().get(0).setSpan(0f, duration);
    }

    // The latest key of every bone track and every morph curve, held to the floor an empty clip is keyed in
    private static float resolveDuration(RecordingTakeStruct take, ArpgObjectStruct clipArpg) {

        float duration = EditorSetting.ANIMATION_EDITOR_TIMELINE_MIN_SPAN;

        for (int i = 0; i < take.getBoneTracks().size(); i++) {

            RecordingBoneTrackStruct track = take.getBoneTracks().get(i);

            for (int slot = 0; slot < track.getCurveCount(); slot++)
                if (!track.getCurve(slot).isEmpty())
                    duration = Math.max(duration, track.getCurve(slot).getEndTime());
        }

        ArpgObjectStruct morphs = CharacterViewUtility.findObject(clipArpg, EngineSetting.ANIMATION_FIELD_MORPHS);

        if (morphs == null)
            return duration;

        for (String morphName : morphs.keySet())
            for (ArpgElementStruct key : morphs.getAsArray(morphName))
                duration = Math.max(
                        duration,
                        ArpgUtility.getFloat(key.getAsObject(), EngineSetting.ANIMATION_FIELD_TIME, 0f));

        return duration;
    }

    // Write \\

    // The clip as its ARPG holds it now: every bone track laid back where it came from, the rest as it was read
    ArpgObjectStruct build(RecordingInstance recording) {

        ArpgObjectStruct clipArpg = recording.getAnimationArpg().deepCopy().getAsObject();
        RecordingTakeStruct take = recording.getTakes().get(0);

        for (int i = 0; i < take.getBoneTracks().size(); i++)
            writeBone(clipArpg, take.getBoneTracks().get(i));

        return clipArpg;
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
