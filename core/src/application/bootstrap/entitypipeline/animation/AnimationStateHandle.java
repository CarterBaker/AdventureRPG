package application.bootstrap.entitypipeline.animation;

import application.bootstrap.animationpipeline.animation.AnimationClipHandle;
import application.bootstrap.animationpipeline.animation.AnimationKeyframeStruct;
import application.bootstrap.animationpipeline.animation.BoneTrackStruct;
import application.bootstrap.geometrypipeline.rig.RigBoneStruct;
import application.bootstrap.geometrypipeline.rig.RigHandle;
import application.bootstrap.geometrypipeline.rig.RigMathUtility;
import engine.root.HandlePackage;
import engine.util.mathematics.matrices.Matrix4;
import engine.util.mathematics.vectors.Vector3;

public class AnimationStateHandle extends HandlePackage {

    /*
     * Per-entity runtime animation playback state. Holds the entity's rig,
     * its currently playing clip, playback time, and the fully evaluated
     * per-bone skinning matrices for the current frame — the single output
     * consumed by rendering. No manager owns this — it lives directly on
     * EntityInstance, same as EntityStateHandle and StatisticsHandle.
     *
     * skinningMatrices[boneIndex] = currentWorldMatrix[boneIndex] *
     * rig.getBindWorldInverseMatrix(boneIndex) — the standard skeletal
     * skinning matrix. Mesh vertices are authored in bind-pose model
     * space, so applying this matrix moves a vertex by exactly how far
     * its bone has moved away from the rest pose.
     *
     * Tracks are sampled with a cubic Hermite curve whose tangents come from
     * the neighbouring keyframes — wrapping around the loop for a looping
     * clip, flat at the ends of a one-shot — so motion eases through every
     * key instead of changing direction on a hard corner. Switching clips
     * never snaps: the pose the entity held at that moment is captured and
     * cross-faded into the new clip over its blend duration with a smooth
     * step. Switching between two looping clips carries the normalized
     * phase across, so a walk that speeds into a run keeps its footfalls.
     *
     * boneProportions is a per-bone, non-inherited geometry scale layered
     * on top of the clip — skinningMatrices[boneIndex] = currentWorld *
     * S(proportion) * bindWorldInverse — so scaling one bone reshapes only
     * the skin it owns, never its children. A child's joint offset IS
     * scaled by its parent's proportion, so a wider torso carries the
     * shoulders and hips out with it. AppearanceHandle is the only writer
     * (body build from weight, head shape); every proportion is 1.0 for an
     * entity with no appearance.
     *
     * Every array here is allocated once in constructor() and mutated in
     * place every update() — zero allocation in the per-frame pose walk.
     */

    // Rig
    private RigHandle rigHandle;

    // Playback
    private AnimationClipHandle currentClip;
    private float playbackTime;

    // Blend — the captured pose the current clip is fading in over
    private float blendElapsed;
    private float blendDuration;
    private Vector3[] blendPositions;
    private Vector3[] blendRotations;
    private Vector3[] blendScales;

    // Pose — the local pose last evaluated, per bone
    private Vector3[] posePositions;
    private Vector3[] poseRotations;
    private Vector3[] poseScales;

    // Proportions — per bone, non-inherited
    private Vector3[] boneProportions;

    // Output — consumed by rendering
    private Matrix4[] skinningMatrices;

    // Scratch — reused every update(), never reallocated
    private Matrix4[] currentWorldMatrices;
    private Matrix4 localScratch;
    private Matrix4 proportionScratch;
    private Matrix4 matrixScratchA;
    private Matrix4 matrixScratchB;
    private Vector3 positionScratch;
    private Vector3 rotationScratch;
    private Vector3 scaleScratch;

    // Constructor \\

    public void constructor(RigHandle rigHandle, AnimationClipHandle initialClip) {

        // Rig
        this.rigHandle = rigHandle;

        // Output
        int boneCount = rigHandle.getBoneCount();
        this.boneProportions = new Vector3[boneCount];
        this.skinningMatrices = new Matrix4[boneCount];
        this.currentWorldMatrices = new Matrix4[boneCount];

        // Blend
        this.blendPositions = new Vector3[boneCount];
        this.blendRotations = new Vector3[boneCount];
        this.blendScales = new Vector3[boneCount];

        // Pose
        this.posePositions = new Vector3[boneCount];
        this.poseRotations = new Vector3[boneCount];
        this.poseScales = new Vector3[boneCount];

        for (int i = 0; i < boneCount; i++) {
            boneProportions[i] = new Vector3(1f, 1f, 1f);
            skinningMatrices[i] = new Matrix4();
            currentWorldMatrices[i] = new Matrix4();
            blendPositions[i] = new Vector3();
            blendRotations[i] = new Vector3();
            blendScales[i] = new Vector3(1f, 1f, 1f);
            posePositions[i] = new Vector3();
            poseRotations[i] = new Vector3();
            poseScales[i] = new Vector3(1f, 1f, 1f);
        }

        // Scratch
        this.localScratch = new Matrix4();
        this.proportionScratch = new Matrix4();
        this.matrixScratchA = new Matrix4();
        this.matrixScratchB = new Matrix4();
        this.positionScratch = new Vector3();
        this.rotationScratch = new Vector3();
        this.scaleScratch = new Vector3();

        // Playback
        setClip(initialClip);

        evaluatePose();
    }

    // Clip Control \\

    public void setClip(AnimationClipHandle clip) {

        if (clip.getRigHandle() != rigHandle)
            throwException("Clip \"" + clip.getClipName() + "\" targets a different rig than this entity uses.");

        if (clip == currentClip)
            return;

        if (currentClip != null)
            beginBlend(clip);

        this.playbackTime = resolveEntryTime(clip);
        this.currentClip = clip;
    }

    public AnimationClipHandle getClip() {
        return currentClip;
    }

    private float resolveEntryTime(AnimationClipHandle clip) {

        if (currentClip == null || !currentClip.isLooping() || !clip.isLooping())
            return 0f;

        return playbackTime / currentClip.getDuration() * clip.getDuration();
    }

    // Blend \\

    private void beginBlend(AnimationClipHandle clip) {

        for (int i = 0; i < rigHandle.getBoneCount(); i++) {
            blendPositions[i].set(posePositions[i]);
            blendRotations[i].set(poseRotations[i]);
            blendScales[i].set(poseScales[i]);
        }

        this.blendElapsed = 0f;
        this.blendDuration = clip.getBlendDuration();
    }

    private boolean isBlending() {
        return blendElapsed < blendDuration;
    }

    private float resolveBlendWeight() {

        float t = blendElapsed / blendDuration;

        return t * t * (3f - 2f * t);
    }

    // Proportions \\

    public void setBoneProportion(int boneIndex, float x, float y, float z) {
        boneProportions[boneIndex].set(x, y, z);
    }

    public Vector3 getBoneProportion(int boneIndex) {
        return boneProportions[boneIndex];
    }

    // Update \\

    public void update(float deltaTime) {
        advanceTime(deltaTime);
        evaluatePose();
    }

    public void refreshPose() {
        evaluatePose();
    }

    private void advanceTime(float deltaTime) {

        this.playbackTime += deltaTime;
        this.blendElapsed += deltaTime;

        float duration = currentClip.getDuration();

        if (currentClip.isLooping())
            this.playbackTime = this.playbackTime % duration;
        else
            this.playbackTime = Math.min(this.playbackTime, duration);
    }

    // Pose Evaluation \\

    private void evaluatePose() {

        int boneCount = rigHandle.getBoneCount();
        boolean blending = isBlending();
        float blendWeight = blending ? resolveBlendWeight() : 1f;

        for (int i = 0; i < boneCount; i++) {

            RigBoneStruct bone = rigHandle.getBone(i);
            BoneTrackStruct track = currentClip.hasBoneTrack(i) ? currentClip.getBoneTrack(i) : null;

            sampleTrack(track, bone);

            if (blending)
                blendFromCapture(i, blendWeight);

            posePositions[i].set(positionScratch);
            poseRotations[i].set(rotationScratch);
            poseScales[i].set(scaleScratch);

            if (!bone.isRoot())
                positionScratch.multiply(boneProportions[bone.getParentIndex()]);

            RigMathUtility.composeLocal(
                    positionScratch, rotationScratch, scaleScratch,
                    localScratch, matrixScratchA, matrixScratchB);

            Matrix4 currentWorld = currentWorldMatrices[i];

            if (bone.isRoot())
                currentWorld.set(localScratch);
            else
                currentWorld.set(currentWorldMatrices[bone.getParentIndex()]).multiply(localScratch);

            RigMathUtility.setScale(proportionScratch, boneProportions[i]);

            skinningMatrices[i]
                    .set(currentWorld)
                    .multiply(proportionScratch)
                    .multiply(rigHandle.getBindWorldInverseMatrix(i));
        }
    }

    private void blendFromCapture(int boneIndex, float weight) {
        mix(positionScratch, blendPositions[boneIndex], weight);
        mix(rotationScratch, blendRotations[boneIndex], weight);
        mix(scaleScratch, blendScales[boneIndex], weight);
    }

    private static void mix(Vector3 target, Vector3 from, float weight) {
        target.set(
                lerp(from.x, target.x, weight),
                lerp(from.y, target.y, weight),
                lerp(from.z, target.z, weight));
    }

    // Sampling \\

    private void sampleTrack(BoneTrackStruct track, RigBoneStruct bone) {

        if (track == null) {
            positionScratch.set(bone.getPosition());
            rotationScratch.set(bone.getRotation());
            scaleScratch.set(1f, 1f, 1f);
            return;
        }

        AnimationKeyframeStruct[] keyframes = track.getKeyframes();
        int count = keyframes.length;

        if (count == 1 || playbackTime <= keyframes[0].getTime()) {
            applyKeyframe(keyframes[0], bone);
            return;
        }

        if (playbackTime >= keyframes[count - 1].getTime()) {
            applyKeyframe(keyframes[count - 1], bone);
            return;
        }

        int segment = findSegment(keyframes);
        boolean looping = currentClip.isLooping();
        float period = keyframes[count - 1].getTime() - keyframes[0].getTime();

        AnimationKeyframeStruct a = keyframes[segment];
        AnimationKeyframeStruct b = keyframes[segment + 1];
        AnimationKeyframeStruct before = resolveBefore(keyframes, segment, looping);
        AnimationKeyframeStruct after = resolveAfter(keyframes, segment + 1, looping);

        float span = b.getTime() - a.getTime();
        float t = (playbackTime - a.getTime()) / span;
        float beforeTime = segment > 0 || !looping ? before.getTime() : before.getTime() - period;
        float afterTime = segment + 1 < count - 1 || !looping ? after.getTime() : after.getTime() + period;
        float inTangent = resolveTangentScale(span, beforeTime, b.getTime(), before == b);
        float outTangent = resolveTangentScale(span, a.getTime(), afterTime, after == a);

        positionScratch.set(
                bone.getPosition().x + hermite(before.getPosition().x, a.getPosition().x, b.getPosition().x,
                        after.getPosition().x, inTangent, outTangent, t),
                bone.getPosition().y + hermite(before.getPosition().y, a.getPosition().y, b.getPosition().y,
                        after.getPosition().y, inTangent, outTangent, t),
                bone.getPosition().z + hermite(before.getPosition().z, a.getPosition().z, b.getPosition().z,
                        after.getPosition().z, inTangent, outTangent, t));

        rotationScratch.set(
                bone.getRotation().x + hermite(before.getRotation().x, a.getRotation().x, b.getRotation().x,
                        after.getRotation().x, inTangent, outTangent, t),
                bone.getRotation().y + hermite(before.getRotation().y, a.getRotation().y, b.getRotation().y,
                        after.getRotation().y, inTangent, outTangent, t),
                bone.getRotation().z + hermite(before.getRotation().z, a.getRotation().z, b.getRotation().z,
                        after.getRotation().z, inTangent, outTangent, t));

        scaleScratch.set(
                hermite(before.getScale().x, a.getScale().x, b.getScale().x,
                        after.getScale().x, inTangent, outTangent, t),
                hermite(before.getScale().y, a.getScale().y, b.getScale().y,
                        after.getScale().y, inTangent, outTangent, t),
                hermite(before.getScale().z, a.getScale().z, b.getScale().z,
                        after.getScale().z, inTangent, outTangent, t));
    }

    private int findSegment(AnimationKeyframeStruct[] keyframes) {

        for (int i = 0; i < keyframes.length - 2; i++)
            if (playbackTime < keyframes[i + 1].getTime())
                return i;

        return keyframes.length - 2;
    }

    // A looping track's last key repeats its first, so the neighbour across the seam skips it
    private AnimationKeyframeStruct resolveBefore(AnimationKeyframeStruct[] keyframes, int index, boolean looping) {

        if (index > 0)
            return keyframes[index - 1];

        return looping ? keyframes[Math.max(0, keyframes.length - 2)] : keyframes[index + 1];
    }

    private AnimationKeyframeStruct resolveAfter(AnimationKeyframeStruct[] keyframes, int index, boolean looping) {

        if (index < keyframes.length - 1)
            return keyframes[index + 1];

        return looping ? keyframes[Math.min(1, keyframes.length - 1)] : keyframes[index - 1];
    }

    // A mirrored neighbour at a one-shot's end gives a flat tangent, easing into and out of the hold
    private static float resolveTangentScale(float span, float fromTime, float toTime, boolean flat) {

        float range = toTime - fromTime;

        return flat || range <= 0f ? 0f : span / range;
    }

    private static float hermite(
            float before,
            float a,
            float b,
            float after,
            float inTangent,
            float outTangent,
            float t) {

        float t2 = t * t;
        float t3 = t2 * t;
        float tangentA = (b - before) * inTangent;
        float tangentB = (after - a) * outTangent;

        return (2f * t3 - 3f * t2 + 1f) * a
                + (t3 - 2f * t2 + t) * tangentA
                + (-2f * t3 + 3f * t2) * b
                + (t3 - t2) * tangentB;
    }

    private void applyKeyframe(AnimationKeyframeStruct keyframe, RigBoneStruct bone) {

        positionScratch.set(
                bone.getPosition().x + keyframe.getPosition().x,
                bone.getPosition().y + keyframe.getPosition().y,
                bone.getPosition().z + keyframe.getPosition().z);

        rotationScratch.set(
                bone.getRotation().x + keyframe.getRotation().x,
                bone.getRotation().y + keyframe.getRotation().y,
                bone.getRotation().z + keyframe.getRotation().z);

        scaleScratch.set(keyframe.getScale());
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    // Accessible \\

    public RigHandle getRigHandle() {
        return rigHandle;
    }

    public Matrix4[] getSkinningMatrices() {
        return skinningMatrices;
    }

    public Matrix4 getSkinningMatrix(int boneIndex) {
        return skinningMatrices[boneIndex];
    }

    public float getPlaybackTime() {
        return playbackTime;
    }
}
