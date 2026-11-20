package application.bootstrap.animationpipeline.animationmanager;

import java.io.File;

import application.bootstrap.animationpipeline.animation.AnimationClipData;
import application.bootstrap.animationpipeline.animation.AnimationClipHandle;
import application.bootstrap.animationpipeline.animation.BoneTrackStruct;
import application.bootstrap.animationpipeline.animation.MorphTrackStruct;
import application.bootstrap.animationpipeline.util.AnimationCurveArpgUtility;
import application.bootstrap.geometrypipeline.rig.RigHandle;
import application.bootstrap.geometrypipeline.rigmanager.RigManager;
import engine.root.BuilderPackage;
import engine.root.EngineSetting;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.mathematics.curves.CurveEnds;
import engine.util.mathematics.curves.CurveStruct;

class AnimationBuilder extends BuilderPackage {

    /*
     * Parses one animation clip ARPG file into an AnimationClipData and
     * wraps it in an AnimationClipHandle. Every clip declares the rig its
     * bone names are validated against — track keys are resolved to bone
     * indices at build time, never by name at runtime. A bone's track keys
     * each axis of each channel on a curve of its own
     * (AnimationCurveArpgUtility), keys in strictly increasing time; a
     * looping clip's curves run round their seam and a one-shot's ease into
     * their holds. Duration is derived, never authored — the latest key time
     * across every track, morph curves included. "morphs" keys the weight of morph
     * targets by name, each a curve of keys as a bone axis keys, the
     * blendshape curves a facial clip plays; the mesh that plays the clip
     * resolves the names. "parts" groups the tracks a joined body part
     * brought, each under its own name, so a part's motion is edited apart
     * from the rest and plays as one clip with it; a bone is keyed once,
     * in the clip's own tracks or in one part, and a part naming a bone the
     * rig no longer holds is left out whole, since that part was taken back.
     * Cross-fades are authored on the animation tree nodes that play a clip,
     * never on the clip itself. Bootstrap-only, apart from the unregistered
     * preview builds an editor makes of a clip it is changing and the live
     * rebuilds of a clip a part joined.
     */

    // Internal
    private RigManager rigManager;

    // Base \\

    @Override
    protected void get() {
        this.rigManager = get(RigManager.class);
    }

    // Build \\

    AnimationClipHandle build(File file, String clipName) {
        return build(ArpgUtility.loadObject(file), clipName, file.getName());
    }

    AnimationClipHandle build(ArpgObjectStruct arpg, String clipName, String sourceName) {

        String rigName = ArpgUtility.validateString(arpg, EngineSetting.ANIMATION_FIELD_RIG);
        RigHandle rigHandle = rigManager.getRigHandleFromRigName(rigName);
        boolean looping = ArpgUtility.getBoolean(arpg, EngineSetting.ANIMATION_FIELD_LOOP, false);
        CurveEnds ends = AnimationCurveArpgUtility.resolveEnds(looping);

        BoneTrackStruct[] boneTracks = new BoneTrackStruct[rigHandle.getBoneCount()];
        float duration = 0f;

        if (ArpgUtility.hasObject(arpg, EngineSetting.ANIMATION_FIELD_TRACKS)) {

            ArpgObjectStruct tracksArpg = arpg.getAsObject(EngineSetting.ANIMATION_FIELD_TRACKS);

            for (String boneName : tracksArpg.keySet())
                if (!rigHandle.hasBone(boneName))
                    throwException("Clip \"" + clipName + "\" references unknown bone \"" + boneName
                            + "\" for rig \"" + rigName + "\" in " + sourceName);

            duration = parseTracks(tracksArpg, rigHandle, ends, boneTracks, clipName, sourceName);
        }

        if (ArpgUtility.hasObject(arpg, EngineSetting.ANIMATION_FIELD_PARTS)) {

            ArpgObjectStruct partsArpg = arpg.getAsObject(EngineSetting.ANIMATION_FIELD_PARTS);

            for (String partName : partsArpg.keySet()) {

                ArpgObjectStruct tracksArpg = ArpgUtility.validateObject(
                        partsArpg.getAsObject(partName),
                        EngineSetting.ANIMATION_FIELD_TRACKS);

                if (holdsBones(tracksArpg, rigHandle))
                    duration = Math.max(
                            duration, parseTracks(tracksArpg, rigHandle, ends, boneTracks, clipName, sourceName));
            }
        }

        MorphTrackStruct[] morphTracks = parseMorphTracks(arpg, ends, sourceName);

        for (int i = 0; i < morphTracks.length; i++)
            duration = Math.max(duration, morphTracks[i].getEndTime());

        if (duration <= 0f)
            throwException("Clip \"" + clipName + "\" has no keyframes past time 0 in " + sourceName);

        AnimationClipData clipData = new AnimationClipData(
                clipName,
                rigHandle,
                duration,
                looping,
                boneTracks,
                morphTracks);

        AnimationClipHandle handle = create(AnimationClipHandle.class);
        handle.constructor(clipData);

        return handle;
    }

    // Track Parsing \\

    // Every track of one group laid into the clip's tracks by bone; a bone keyed twice is refused; the latest key
    // time is returned
    private float parseTracks(
            ArpgObjectStruct tracksArpg,
            RigHandle rigHandle,
            CurveEnds ends,
            BoneTrackStruct[] boneTracks,
            String clipName,
            String sourceName) {

        float duration = 0f;

        for (String boneName : tracksArpg.keySet()) {

            int boneIndex = rigHandle.getBoneIndex(boneName);

            if (boneTracks[boneIndex] != null)
                throwException("Clip \"" + clipName + "\" keys bone \"" + boneName
                        + "\" more than once across its tracks and parts in " + sourceName);

            CurveStruct[] curves = AnimationCurveArpgUtility.readTrack(
                    ArpgUtility.validateObject(tracksArpg, boneName), ends, boneName, sourceName);
            BoneTrackStruct track = new BoneTrackStruct(curves, AnimationCurveArpgUtility.resolveEndTime(curves));

            boneTracks[boneIndex] = track;
            duration = Math.max(duration, track.getEndTime());
        }

        return duration;
    }

    // Whether the rig holds every bone a part keys, so the part still stands on it
    private static boolean holdsBones(ArpgObjectStruct tracksArpg, RigHandle rigHandle) {

        for (String boneName : tracksArpg.keySet())
            if (!rigHandle.hasBone(boneName))
                return false;

        return true;
    }

    // Every morph curve the clip keys, in the order the file lists them
    private MorphTrackStruct[] parseMorphTracks(ArpgObjectStruct arpg, CurveEnds ends, String sourceName) {

        if (!ArpgUtility.hasObject(arpg, EngineSetting.ANIMATION_FIELD_MORPHS))
            return new MorphTrackStruct[0];

        ArpgObjectStruct morphsArpg = arpg.getAsObject(EngineSetting.ANIMATION_FIELD_MORPHS);
        MorphTrackStruct[] morphTracks = new MorphTrackStruct[morphsArpg.size()];
        int index = 0;

        for (String morphName : morphsArpg.keySet()) {

            CurveStruct curve = new CurveStruct(ends);

            AnimationCurveArpgUtility.readCurve(
                    ArpgUtility.validateArray(morphsArpg, morphName), curve, morphName, sourceName);
            morphTracks[index++] = new MorphTrackStruct(morphName, curve);
        }

        return morphTracks;
    }
}
