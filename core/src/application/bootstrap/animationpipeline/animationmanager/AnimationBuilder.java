package application.bootstrap.animationpipeline.animationmanager;

import java.io.File;
import java.util.Set;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import application.bootstrap.animationpipeline.animation.AnimationClipData;
import application.bootstrap.animationpipeline.animation.AnimationClipHandle;
import application.bootstrap.animationpipeline.animation.AnimationKeyframeStruct;
import application.bootstrap.animationpipeline.animation.BoneTrackStruct;
import application.bootstrap.geometrypipeline.rig.RigHandle;
import application.bootstrap.geometrypipeline.rigmanager.RigManager;
import engine.root.BuilderPackage;
import engine.util.io.JsonUtility;
import engine.util.mathematics.vectors.Vector3;

class AnimationBuilder extends BuilderPackage {

    /*
     * Parses one animation clip JSON file into an AnimationClipData and
     * wraps it in an AnimationClipHandle. Every clip declares the rig its
     * bone names are validated against — track keys are resolved to bone
     * indices at build time, never by name at runtime. Keyframes within a
     * track must be supplied in strictly increasing time order. Duration is
     * derived, never authored — the latest keyframe time across every
     * track. Bootstrap-only.
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

        JsonObject json = JsonUtility.loadJsonObject(file);
        String rigName = JsonUtility.validateString(json, "rig");
        RigHandle rigHandle = rigManager.getRigHandleFromRigName(rigName);
        boolean looping = json.has("loop") && json.get("loop").getAsBoolean();

        BoneTrackStruct[] boneTracks = new BoneTrackStruct[rigHandle.getBoneCount()];
        float duration = 0f;

        if (json.has("tracks") && !json.get("tracks").isJsonNull()) {

            JsonObject tracksJson = json.getAsJsonObject("tracks");
            Set<String> boneNames = tracksJson.keySet();

            for (String boneName : boneNames) {

                if (!rigHandle.hasBone(boneName))
                    throwException("Clip \"" + clipName + "\" references unknown bone \"" + boneName
                            + "\" for rig \"" + rigName + "\" in file: " + file.getName());

                int boneIndex = rigHandle.getBoneIndex(boneName);
                JsonArray keyframesJson = tracksJson.getAsJsonArray(boneName);
                BoneTrackStruct track = parseTrack(keyframesJson, boneName, file);

                boneTracks[boneIndex] = track;
                duration = Math.max(duration, track.getKeyframe(track.getKeyframeCount() - 1).getTime());
            }
        }

        if (duration <= 0f)
            throwException("Clip \"" + clipName + "\" has no keyframes past time 0 in file: " + file.getName());

        AnimationClipData clipData = new AnimationClipData(clipName, rigHandle, duration, looping, boneTracks);

        AnimationClipHandle handle = create(AnimationClipHandle.class);
        handle.constructor(clipData);

        return handle;
    }

    // Track Parsing \\

    private BoneTrackStruct parseTrack(JsonArray keyframesJson, String boneName, File file) {

        if (keyframesJson.size() == 0)
            throwException("Bone track \"" + boneName + "\" has no keyframes in file: " + file.getName());

        AnimationKeyframeStruct[] keyframes = new AnimationKeyframeStruct[keyframesJson.size()];
        float previousTime = -1f;

        for (int i = 0; i < keyframesJson.size(); i++) {

            JsonObject keyframeJson = keyframesJson.get(i).getAsJsonObject();

            if (!keyframeJson.has("time"))
                throwException("Keyframe " + i + " on bone \"" + boneName
                        + "\" missing \"time\" in file: " + file.getName());

            float time = keyframeJson.get("time").getAsFloat();

            if (time <= previousTime)
                throwException("Keyframe " + i + " on bone \"" + boneName
                        + "\" is out of order — keyframes must be strictly increasing in time. File: "
                        + file.getName());

            previousTime = time;

            keyframes[i] = new AnimationKeyframeStruct(
                    time,
                    parseVector3(keyframeJson, "rotation", 0f, file),
                    parseVector3(keyframeJson, "position", 0f, file),
                    parseVector3(keyframeJson, "scale", 1f, file));
        }

        return new BoneTrackStruct(keyframes);
    }

    // Vector Parsing \\

    private Vector3 parseVector3(JsonObject json, String key, float defaultValue, File file) {

        if (!json.has(key) || json.get(key).isJsonNull())
            return new Vector3(defaultValue, defaultValue, defaultValue);

        JsonObject vector = json.getAsJsonObject(key);

        return new Vector3(
                vector.has("x") ? vector.get("x").getAsFloat() : defaultValue,
                vector.has("y") ? vector.get("y").getAsFloat() : defaultValue,
                vector.has("z") ? vector.get("z").getAsFloat() : defaultValue);
    }
}