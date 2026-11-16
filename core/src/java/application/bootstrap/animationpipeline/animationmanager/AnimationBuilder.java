package application.bootstrap.animationpipeline.animationmanager;

import java.io.File;
import java.util.Set;

import application.bootstrap.animationpipeline.animation.AnimationClipData;
import application.bootstrap.animationpipeline.animation.AnimationClipHandle;
import application.bootstrap.animationpipeline.animation.AnimationKeyframeStruct;
import application.bootstrap.animationpipeline.animation.BoneTrackStruct;
import application.bootstrap.animationpipeline.animation.MorphTrackStruct;
import application.bootstrap.geometrypipeline.rig.RigHandle;
import application.bootstrap.geometrypipeline.rigmanager.RigManager;
import engine.root.BuilderPackage;
import engine.root.EngineSetting;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.mathematics.vectors.Vector3;

class AnimationBuilder extends BuilderPackage {

    /*
     * Parses one animation clip ARPG file into an AnimationClipData and
     * wraps it in an AnimationClipHandle. Every clip declares the rig its
     * bone names are validated against — track keys are resolved to bone
     * indices at build time, never by name at runtime. Keyframes within a
     * track must be supplied in strictly increasing time order. Duration is
     * derived, never authored — the latest keyframe time across every
     * track, morph curves included. "morphs" keys the weight of morph
     * targets by name, each a list of "time" and "value" keys, the
     * blendshape curves a facial clip plays; the mesh that plays the clip
     * resolves the names. Cross-fades are authored on the animation tree nodes that play
     * a clip, never on the clip itself. Bootstrap-only, apart from the
     * unregistered preview builds an editor makes of a clip it is changing.
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

        String rigName = ArpgUtility.validateString(arpg, "rig");
        RigHandle rigHandle = rigManager.getRigHandleFromRigName(rigName);
        boolean looping = arpg.has("loop") && arpg.get("loop").getAsBoolean();

        BoneTrackStruct[] boneTracks = new BoneTrackStruct[rigHandle.getBoneCount()];
        float duration = 0f;

        if (arpg.has("tracks") && !arpg.get("tracks").isNull()) {

            ArpgObjectStruct tracksArpg = arpg.getAsObject("tracks");
            Set<String> boneNames = tracksArpg.keySet();

            for (String boneName : boneNames) {

                if (!rigHandle.hasBone(boneName))
                    throwException("Clip \"" + clipName + "\" references unknown bone \"" + boneName
                            + "\" for rig \"" + rigName + "\" in " + sourceName);

                int boneIndex = rigHandle.getBoneIndex(boneName);
                ArpgArrayStruct keyframesArpg = tracksArpg.getAsArray(boneName);
                BoneTrackStruct track = parseTrack(keyframesArpg, boneName, sourceName);

                boneTracks[boneIndex] = track;
                duration = Math.max(duration, track.getKeyframe(track.getKeyframeCount() - 1).getTime());
            }
        }

        MorphTrackStruct[] morphTracks = parseMorphTracks(arpg, sourceName);

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

    private BoneTrackStruct parseTrack(ArpgArrayStruct keyframesArpg, String boneName, String sourceName) {

        if (keyframesArpg.size() == 0)
            throwException("Bone track \"" + boneName + "\" has no keyframes in " + sourceName);

        AnimationKeyframeStruct[] keyframes = new AnimationKeyframeStruct[keyframesArpg.size()];
        float previousTime = -1f;

        for (int i = 0; i < keyframesArpg.size(); i++) {

            ArpgObjectStruct keyframeArpg = keyframesArpg.get(i).getAsObject();

            if (!keyframeArpg.has("time"))
                throwException("Keyframe " + i + " on bone \"" + boneName
                        + "\" missing \"time\" in " + sourceName);

            float time = keyframeArpg.get("time").getAsFloat();

            if (time <= previousTime)
                throwException("Keyframe " + i + " on bone \"" + boneName
                        + "\" is out of order — keyframes must be strictly increasing in time. Source: "
                        + sourceName);

            previousTime = time;

            keyframes[i] = new AnimationKeyframeStruct(
                    time,
                    parseVector3(keyframeArpg, "rotation", 0f, sourceName),
                    parseVector3(keyframeArpg, "position", 0f, sourceName),
                    parseVector3(keyframeArpg, "scale", 1f, sourceName));
        }

        return new BoneTrackStruct(keyframes);
    }

    // Every morph curve the clip keys, in the order the file lists them
    private MorphTrackStruct[] parseMorphTracks(ArpgObjectStruct arpg, String sourceName) {

        if (!ArpgUtility.hasObject(arpg, EngineSetting.ANIMATION_FIELD_MORPHS))
            return new MorphTrackStruct[0];

        ArpgObjectStruct morphsArpg = arpg.getAsObject(EngineSetting.ANIMATION_FIELD_MORPHS);
        MorphTrackStruct[] morphTracks = new MorphTrackStruct[morphsArpg.size()];
        int index = 0;

        for (String morphName : morphsArpg.keySet())
            morphTracks[index++] = parseMorphTrack(morphsArpg.getAsArray(morphName), morphName, sourceName);

        return morphTracks;
    }

    private MorphTrackStruct parseMorphTrack(ArpgArrayStruct keysArpg, String morphName, String sourceName) {

        if (keysArpg.size() == 0)
            throwException("Morph curve \"" + morphName + "\" has no keys in " + sourceName);

        float[] times = new float[keysArpg.size()];
        float[] values = new float[keysArpg.size()];

        for (int i = 0; i < times.length; i++) {

            ArpgObjectStruct keyArpg = keysArpg.get(i).getAsObject();

            times[i] = ArpgUtility.validateFloat(keyArpg, EngineSetting.ANIMATION_FIELD_TIME);
            values[i] = ArpgUtility.validateFloat(keyArpg, EngineSetting.ANIMATION_FIELD_VALUE);

            if (i > 0 && times[i] <= times[i - 1])
                throwException("Key " + i + " on morph curve \"" + morphName
                        + "\" is out of order — keys must be strictly increasing in time. Source: " + sourceName);
        }

        return new MorphTrackStruct(morphName, times, values);
    }

    // Vector Parsing \\

    private Vector3 parseVector3(ArpgObjectStruct arpg, String key, float defaultValue, String sourceName) {

        if (!arpg.has(key) || arpg.get(key).isNull())
            return new Vector3(defaultValue, defaultValue, defaultValue);

        ArpgObjectStruct vector = arpg.getAsObject(key);

        return new Vector3(
                vector.has("x") ? vector.get("x").getAsFloat() : defaultValue,
                vector.has("y") ? vector.get("y").getAsFloat() : defaultValue,
                vector.has("z") ? vector.get("z").getAsFloat() : defaultValue);
    }
}