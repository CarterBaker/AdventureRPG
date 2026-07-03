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
     * Every array here is allocated once in constructor() and mutated in
     * place every update() — zero allocation in the per-frame pose walk.
     */

    // Rig
    private RigHandle rigHandle;

    // Playback
    private AnimationClipHandle currentClip;
    private float playbackTime;

    // Output — consumed by rendering
    private Matrix4[] skinningMatrices;

    // Scratch — reused every update(), never reallocated
    private Matrix4[] currentWorldMatrices;
    private Matrix4 localScratch;
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
        this.skinningMatrices = new Matrix4[boneCount];
        this.currentWorldMatrices = new Matrix4[boneCount];

        for (int i = 0; i < boneCount; i++) {
            skinningMatrices[i] = new Matrix4();
            currentWorldMatrices[i] = new Matrix4();
        }

        // Scratch
        this.localScratch = new Matrix4();
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

        this.currentClip = clip;
        this.playbackTime = 0f;
    }

    public AnimationClipHandle getClip() {
        return currentClip;
    }

    // Update \\

    public void update(float deltaTime) {
        advanceTime(deltaTime);
        evaluatePose();
    }

    private void advanceTime(float deltaTime) {

        this.playbackTime += deltaTime;

        float duration = currentClip.getDuration();

        if (currentClip.isLooping())
            this.playbackTime = this.playbackTime % duration;
        else
            this.playbackTime = Math.min(this.playbackTime, duration);
    }

    // Pose Evaluation \\

    private void evaluatePose() {

        int boneCount = rigHandle.getBoneCount();

        for (int i = 0; i < boneCount; i++) {

            RigBoneStruct bone = rigHandle.getBone(i);
            BoneTrackStruct track = currentClip.hasBoneTrack(i) ? currentClip.getBoneTrack(i) : null;

            sampleTrack(track, bone);

            RigMathUtility.composeLocal(
                    positionScratch, rotationScratch, scaleScratch,
                    localScratch, matrixScratchA, matrixScratchB);

            Matrix4 currentWorld = currentWorldMatrices[i];

            if (bone.isRoot())
                currentWorld.set(localScratch);
            else
                currentWorld.set(currentWorldMatrices[bone.getParentIndex()]).multiply(localScratch);

            skinningMatrices[i].set(currentWorld).multiply(rigHandle.getBindWorldInverseMatrix(i));
        }
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

        for (int i = 0; i < count - 1; i++) {

            AnimationKeyframeStruct a = keyframes[i];
            AnimationKeyframeStruct b = keyframes[i + 1];

            if (playbackTime < a.getTime() || playbackTime > b.getTime())
                continue;

            float span = b.getTime() - a.getTime();
            float t = span > 0f ? (playbackTime - a.getTime()) / span : 0f;

            positionScratch.set(
                    bone.getPosition().x + lerp(a.getPosition().x, b.getPosition().x, t),
                    bone.getPosition().y + lerp(a.getPosition().y, b.getPosition().y, t),
                    bone.getPosition().z + lerp(a.getPosition().z, b.getPosition().z, t));

            rotationScratch.set(
                    bone.getRotation().x + lerp(a.getRotation().x, b.getRotation().x, t),
                    bone.getRotation().y + lerp(a.getRotation().y, b.getRotation().y, t),
                    bone.getRotation().z + lerp(a.getRotation().z, b.getRotation().z, t));

            scaleScratch.set(
                    lerp(a.getScale().x, b.getScale().x, t),
                    lerp(a.getScale().y, b.getScale().y, t),
                    lerp(a.getScale().z, b.getScale().z, t));

            return;
        }
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