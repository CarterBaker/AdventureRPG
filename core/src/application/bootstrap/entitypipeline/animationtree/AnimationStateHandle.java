package application.bootstrap.entitypipeline.animationtree;

import application.bootstrap.animationpipeline.animation.AnimationClipHandle;
import application.bootstrap.animationpipeline.animation.AnimationKeyframeStruct;
import application.bootstrap.entitypipeline.animationtree.AnimationLayerStruct;
import application.bootstrap.entitypipeline.animationtree.AnimationNodeStruct;
import application.bootstrap.entitypipeline.animationtree.AnimationParameter;
import application.bootstrap.entitypipeline.animationtree.AnimationTreeHandle;
import application.bootstrap.entitypipeline.entity.EntityState;
import application.bootstrap.geometrypipeline.rig.RigBoneStruct;
import application.bootstrap.geometrypipeline.rig.RigHandle;
import application.bootstrap.geometrypipeline.rig.RigMathUtility;
import engine.root.EngineSetting;
import engine.root.HandlePackage;
import engine.util.mathematics.matrices.Matrix4;
import engine.util.mathematics.vectors.Vector3;

public class AnimationStateHandle extends HandlePackage {

    /*
     * Per-entity playback of an animation tree. Tracks each layer's node,
     * cross-fade and phase, blends clips with Hermite-sampled tracks as offsets
     * from the bind pose, stacks override and additive layers through bone
     * masks, and writes the per-bone skinning matrices rendering consumes. All
     * arrays are allocated once in constructor(), so updates never allocate.
     */

    // Rig
    private RigHandle rigHandle;
    private AnimationTreeHandle animationTreeHandle;

    // Parameters — indexed by AnimationParameter ordinal
    private float[] parameters;

    // Layers — indexed by layer
    private int[] layerCurrentNodes;
    private int[] layerPreviousNodes;
    private int[] layerPendingNodes;
    private float[] layerPendingElapsed;
    private float[] layerActivations;
    private float[] layerBlendElapsed;
    private float[] layerBlendDurations;
    private boolean[] layerCaptured;
    private float[][] layerNodePhases;

    // Layer Pose — the offsets each layer last produced, per bone
    private Vector3[][] layerPositions;
    private Vector3[][] layerRotations;
    private Vector3[][] layerScales;

    // Capture — the frozen layer pose an interrupted cross-fade fades out of
    private Vector3[][] capturePositions;
    private Vector3[][] captureRotations;
    private Vector3[][] captureScales;

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
    private Vector3 nodePositionScratch;
    private Vector3 nodeRotationScratch;
    private Vector3 nodeScaleScratch;
    private Vector3 clipPositionScratch;
    private Vector3 clipRotationScratch;
    private Vector3 clipScaleScratch;

    // Bracket — the two clips a node blends between this sample
    private int bracketLow;
    private int bracketHigh;
    private float bracketWeight;

    // Constructor \\

    public void constructor(AnimationTreeHandle animationTreeHandle, EntityState initialState) {

        // Rig
        this.animationTreeHandle = animationTreeHandle;
        this.rigHandle = animationTreeHandle.getRigHandle();

        // Parameters
        this.parameters = new float[AnimationParameter.VALUES.length];

        int boneCount = rigHandle.getBoneCount();
        int layerCount = animationTreeHandle.getLayerCount();

        // Layers
        this.layerCurrentNodes = new int[layerCount];
        this.layerPreviousNodes = new int[layerCount];
        this.layerPendingNodes = new int[layerCount];
        this.layerPendingElapsed = new float[layerCount];
        this.layerActivations = new float[layerCount];
        this.layerBlendElapsed = new float[layerCount];
        this.layerBlendDurations = new float[layerCount];
        this.layerCaptured = new boolean[layerCount];
        this.layerNodePhases = new float[layerCount][];

        // Layer Pose and Capture
        this.layerPositions = new Vector3[layerCount][boneCount];
        this.layerRotations = new Vector3[layerCount][boneCount];
        this.layerScales = new Vector3[layerCount][boneCount];
        this.capturePositions = new Vector3[layerCount][boneCount];
        this.captureRotations = new Vector3[layerCount][boneCount];
        this.captureScales = new Vector3[layerCount][boneCount];

        for (int l = 0; l < layerCount; l++) {

            layerNodePhases[l] = new float[animationTreeHandle.getLayer(l).getNodeCount()];
            layerCurrentNodes[l] = EngineSetting.INDEX_NOT_FOUND;
            layerPreviousNodes[l] = EngineSetting.INDEX_NOT_FOUND;
            layerPendingNodes[l] = EngineSetting.INDEX_NOT_FOUND;

            for (int i = 0; i < boneCount; i++) {
                layerPositions[l][i] = new Vector3();
                layerRotations[l][i] = new Vector3();
                layerScales[l][i] = new Vector3(1f, 1f, 1f);
                capturePositions[l][i] = new Vector3();
                captureRotations[l][i] = new Vector3();
                captureScales[l][i] = new Vector3(1f, 1f, 1f);
            }
        }

        // Output
        this.boneProportions = new Vector3[boneCount];
        this.skinningMatrices = new Matrix4[boneCount];
        this.currentWorldMatrices = new Matrix4[boneCount];

        for (int i = 0; i < boneCount; i++) {
            boneProportions[i] = new Vector3(1f, 1f, 1f);
            skinningMatrices[i] = new Matrix4();
            currentWorldMatrices[i] = new Matrix4();
        }

        // Scratch
        this.localScratch = new Matrix4();
        this.proportionScratch = new Matrix4();
        this.matrixScratchA = new Matrix4();
        this.matrixScratchB = new Matrix4();
        this.positionScratch = new Vector3();
        this.rotationScratch = new Vector3();
        this.scaleScratch = new Vector3();
        this.nodePositionScratch = new Vector3();
        this.nodeRotationScratch = new Vector3();
        this.nodeScaleScratch = new Vector3();
        this.clipPositionScratch = new Vector3();
        this.clipRotationScratch = new Vector3();
        this.clipScaleScratch = new Vector3();

        // Playback
        enterInitialNodes(initialState);

        evaluatePose();
    }

    private void enterInitialNodes(EntityState initialState) {

        for (int l = 0; l < animationTreeHandle.getLayerCount(); l++) {

            AnimationLayerStruct layer = animationTreeHandle.getLayer(l);

            if (!layer.hasStateNode(initialState.ordinal()))
                continue;

            enterNode(l, layer.getStateNode(initialState.ordinal()), false);
            layerActivations[l] = 1f;
        }
    }

    // Parameters \\

    public void setParameter(AnimationParameter parameter, float value) {
        parameters[parameter.ordinal()] = value;
    }

    // Proportions \\

    public void setBoneProportion(int boneIndex, float x, float y, float z) {
        boneProportions[boneIndex].set(x, y, z);
    }

    public Vector3 getBoneProportion(int boneIndex) {
        return boneProportions[boneIndex];
    }

    // Update \\

    public void update(EntityState movementState, float deltaTime) {

        for (int l = 0; l < animationTreeHandle.getLayerCount(); l++) {
            resolveLayerNode(l, movementState.ordinal(), deltaTime);
            advanceLayer(l, deltaTime);
        }

        evaluatePose();
    }

    public void refreshPose() {
        evaluatePose();
    }

    // Layer State \\

    private void resolveLayerNode(int layerIndex, int stateOrdinal, float deltaTime) {

        AnimationLayerStruct layer = animationTreeHandle.getLayer(layerIndex);
        float fadeStep = layer.getBlendDuration() > 0f ? deltaTime / layer.getBlendDuration() : 1f;

        if (!layer.hasStateNode(stateOrdinal)) {
            layerPendingNodes[layerIndex] = EngineSetting.INDEX_NOT_FOUND;
            layerActivations[layerIndex] = Math.max(0f, layerActivations[layerIndex] - fadeStep);
            return;
        }

        int targetNode = layer.getStateNode(stateOrdinal);

        if (layerActivations[layerIndex] <= 0f)
            enterNode(layerIndex, targetNode, false);
        else if (targetNode == layerCurrentNodes[layerIndex])
            layerPendingNodes[layerIndex] = EngineSetting.INDEX_NOT_FOUND;
        else if (isPendingReady(layerIndex, targetNode, deltaTime))
            enterNode(layerIndex, targetNode, true);

        layerActivations[layerIndex] = Math.min(1f, layerActivations[layerIndex] + fadeStep);
    }

    private boolean isPendingReady(int layerIndex, int targetNode, float deltaTime) {

        if (layerPendingNodes[layerIndex] != targetNode) {
            layerPendingNodes[layerIndex] = targetNode;
            layerPendingElapsed[layerIndex] = 0f;
        }

        layerPendingElapsed[layerIndex] += deltaTime;

        AnimationNodeStruct node = animationTreeHandle.getLayer(layerIndex).getNode(targetNode);

        return layerPendingElapsed[layerIndex] >= node.getDelay();
    }

    private void enterNode(int layerIndex, int targetNode, boolean crossFade) {

        AnimationLayerStruct layer = animationTreeHandle.getLayer(layerIndex);
        int outgoingNode = layerCurrentNodes[layerIndex];
        float blendDuration = crossFade ? layer.getTransitionBlend(outgoingNode, targetNode) : 0f;

        if (blendDuration > 0f && isLayerBlending(layerIndex))
            captureLayer(layerIndex);
        else {
            layerPreviousNodes[layerIndex] = blendDuration > 0f ? outgoingNode : EngineSetting.INDEX_NOT_FOUND;
            layerCaptured[layerIndex] = false;
        }

        layerNodePhases[layerIndex][targetNode] = resolveEntryPhase(layerIndex, outgoingNode, targetNode);
        layerCurrentNodes[layerIndex] = targetNode;
        layerPendingNodes[layerIndex] = EngineSetting.INDEX_NOT_FOUND;
        layerBlendElapsed[layerIndex] = 0f;
        layerBlendDurations[layerIndex] = blendDuration;
    }

    // Two looping nodes share a phase across the switch, so footfalls carry from one gait into the next
    private float resolveEntryPhase(int layerIndex, int outgoingNode, int targetNode) {

        if (outgoingNode == EngineSetting.INDEX_NOT_FOUND)
            return 0f;

        AnimationLayerStruct layer = animationTreeHandle.getLayer(layerIndex);

        if (!layer.getNode(outgoingNode).isLooping() || !layer.getNode(targetNode).isLooping())
            return 0f;

        return layerNodePhases[layerIndex][outgoingNode];
    }

    private void captureLayer(int layerIndex) {

        for (int i = 0; i < rigHandle.getBoneCount(); i++) {
            capturePositions[layerIndex][i].set(layerPositions[layerIndex][i]);
            captureRotations[layerIndex][i].set(layerRotations[layerIndex][i]);
            captureScales[layerIndex][i].set(layerScales[layerIndex][i]);
        }

        layerPreviousNodes[layerIndex] = EngineSetting.INDEX_NOT_FOUND;
        layerCaptured[layerIndex] = true;
    }

    private boolean isLayerBlending(int layerIndex) {
        return layerBlendElapsed[layerIndex] < layerBlendDurations[layerIndex];
    }

    // Playback \\

    private void advanceLayer(int layerIndex, float deltaTime) {

        if (layerActivations[layerIndex] <= 0f)
            return;

        advanceNode(layerIndex, layerCurrentNodes[layerIndex], deltaTime);

        if (!isLayerBlending(layerIndex))
            return;

        layerBlendElapsed[layerIndex] += deltaTime;

        if (layerPreviousNodes[layerIndex] != EngineSetting.INDEX_NOT_FOUND)
            advanceNode(layerIndex, layerPreviousNodes[layerIndex], deltaTime);

        if (isLayerBlending(layerIndex))
            return;

        layerPreviousNodes[layerIndex] = EngineSetting.INDEX_NOT_FOUND;
        layerCaptured[layerIndex] = false;
    }

    private void advanceNode(int layerIndex, int nodeIndex, float deltaTime) {

        AnimationNodeStruct node = animationTreeHandle.getLayer(layerIndex).getNode(nodeIndex);

        resolveBracket(node);

        float lowFrequency = 1f / node.getClip(bracketLow).getDuration();
        float highFrequency = 1f / node.getClip(bracketHigh).getDuration();
        float frequency = lerp(lowFrequency, highFrequency, bracketWeight);
        float phase = layerNodePhases[layerIndex][nodeIndex]
                + deltaTime * frequency * node.getRate() * resolveRateScale(node);

        layerNodePhases[layerIndex][nodeIndex] = node.isLooping()
                ? phase - (float) Math.floor(phase)
                : Math.min(phase, 1f);
    }

    // A parameter past either end of a rate-scaled blend space plays its edge clip faster or slower in proportion
    private float resolveRateScale(AnimationNodeStruct node) {

        if (!node.isRateScaled())
            return 1f;

        float value = parameters[node.getParameter().ordinal()];
        float first = node.getValue(0);
        float last = node.getValue(node.getClipCount() - 1);
        float edge = value < first ? first : value > last ? last : 0f;

        if (edge <= 0f)
            return 1f;

        return Math.max(
                EngineSetting.ANIMATION_RATE_SCALE_MIN,
                Math.min(EngineSetting.ANIMATION_RATE_SCALE_MAX, value / edge));
    }

    private void resolveBracket(AnimationNodeStruct node) {

        int last = node.getClipCount() - 1;
        float value = node.hasParameter() ? parameters[node.getParameter().ordinal()] : 0f;

        if (last == 0 || value <= node.getValue(0)) {
            setBracket(0, 0, 0f);
            return;
        }

        if (value >= node.getValue(last)) {
            setBracket(last, last, 0f);
            return;
        }

        int low = 0;

        while (value >= node.getValue(low + 1))
            low++;

        float span = node.getValue(low + 1) - node.getValue(low);

        setBracket(low, low + 1, (value - node.getValue(low)) / span);
    }

    private void setBracket(int low, int high, float weight) {
        this.bracketLow = low;
        this.bracketHigh = high;
        this.bracketWeight = weight;
    }

    // Pose Evaluation \\

    private void evaluatePose() {

        int boneCount = rigHandle.getBoneCount();

        for (int i = 0; i < boneCount; i++) {

            RigBoneStruct bone = rigHandle.getBone(i);

            positionScratch.set(0f, 0f, 0f);
            rotationScratch.set(0f, 0f, 0f);
            scaleScratch.set(1f, 1f, 1f);

            for (int l = 0; l < animationTreeHandle.getLayerCount(); l++)
                applyLayer(l, i);

            positionScratch.add(bone.getPosition());
            rotationScratch.add(bone.getRotation());

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

    private void applyLayer(int layerIndex, int boneIndex) {

        AnimationLayerStruct layer = animationTreeHandle.getLayer(layerIndex);
        float weight = layer.getWeight() * smoothStep(layerActivations[layerIndex]) * layer.getBoneMask(boneIndex);

        if (weight <= 0f)
            return;

        evaluateLayer(layerIndex, boneIndex);

        Vector3 position = layerPositions[layerIndex][boneIndex];
        Vector3 rotation = layerRotations[layerIndex][boneIndex];
        Vector3 scale = layerScales[layerIndex][boneIndex];

        if (!layer.isAdditive()) {
            lerpInto(positionScratch, position, weight);
            lerpInto(rotationScratch, rotation, weight);
            lerpInto(scaleScratch, scale, weight);
            return;
        }

        positionScratch.add(position.x * weight, position.y * weight, position.z * weight);
        rotationScratch.add(rotation.x * weight, rotation.y * weight, rotation.z * weight);
        scaleScratch.multiply(
                lerp(1f, scale.x, weight),
                lerp(1f, scale.y, weight),
                lerp(1f, scale.z, weight));
    }

    private void evaluateLayer(int layerIndex, int boneIndex) {

        Vector3 position = layerPositions[layerIndex][boneIndex];
        Vector3 rotation = layerRotations[layerIndex][boneIndex];
        Vector3 scale = layerScales[layerIndex][boneIndex];

        sampleNode(layerIndex, layerCurrentNodes[layerIndex], boneIndex, position, rotation, scale);

        if (!isLayerBlending(layerIndex))
            return;

        float sourceWeight = 1f - smoothStep(layerBlendElapsed[layerIndex] / layerBlendDurations[layerIndex]);

        if (layerCaptured[layerIndex]) {
            lerpInto(position, capturePositions[layerIndex][boneIndex], sourceWeight);
            lerpInto(rotation, captureRotations[layerIndex][boneIndex], sourceWeight);
            lerpInto(scale, captureScales[layerIndex][boneIndex], sourceWeight);
            return;
        }

        sampleNode(
                layerIndex,
                layerPreviousNodes[layerIndex],
                boneIndex,
                nodePositionScratch,
                nodeRotationScratch,
                nodeScaleScratch);

        lerpInto(position, nodePositionScratch, sourceWeight);
        lerpInto(rotation, nodeRotationScratch, sourceWeight);
        lerpInto(scale, nodeScaleScratch, sourceWeight);
    }

    // Sampling \\

    private void sampleNode(
            int layerIndex,
            int nodeIndex,
            int boneIndex,
            Vector3 outPosition,
            Vector3 outRotation,
            Vector3 outScale) {

        AnimationNodeStruct node = animationTreeHandle.getLayer(layerIndex).getNode(nodeIndex);
        float phase = layerNodePhases[layerIndex][nodeIndex];

        resolveBracket(node);
        sampleClip(node.getClip(bracketLow), phase, boneIndex, outPosition, outRotation, outScale);

        if (bracketWeight <= 0f)
            return;

        sampleClip(
                node.getClip(bracketHigh),
                phase,
                boneIndex,
                clipPositionScratch,
                clipRotationScratch,
                clipScaleScratch);

        lerpInto(outPosition, clipPositionScratch, bracketWeight);
        lerpInto(outRotation, clipRotationScratch, bracketWeight);
        lerpInto(outScale, clipScaleScratch, bracketWeight);
    }

    private void sampleClip(
            AnimationClipHandle clip,
            float phase,
            int boneIndex,
            Vector3 outPosition,
            Vector3 outRotation,
            Vector3 outScale) {

        if (!clip.hasBoneTrack(boneIndex)) {
            outPosition.set(0f, 0f, 0f);
            outRotation.set(0f, 0f, 0f);
            outScale.set(1f, 1f, 1f);
            return;
        }

        AnimationKeyframeStruct[] keyframes = clip.getBoneTrack(boneIndex).getKeyframes();
        int count = keyframes.length;
        float time = phase * clip.getDuration();

        if (count == 1 || time <= keyframes[0].getTime()) {
            applyKeyframe(keyframes[0], outPosition, outRotation, outScale);
            return;
        }

        if (time >= keyframes[count - 1].getTime()) {
            applyKeyframe(keyframes[count - 1], outPosition, outRotation, outScale);
            return;
        }

        int segment = findSegment(keyframes, time);
        boolean looping = clip.isLooping();
        float period = keyframes[count - 1].getTime() - keyframes[0].getTime();

        AnimationKeyframeStruct a = keyframes[segment];
        AnimationKeyframeStruct b = keyframes[segment + 1];
        AnimationKeyframeStruct before = resolveBefore(keyframes, segment, looping);
        AnimationKeyframeStruct after = resolveAfter(keyframes, segment + 1, looping);

        float span = b.getTime() - a.getTime();
        float t = (time - a.getTime()) / span;
        float beforeTime = segment > 0 || !looping ? before.getTime() : before.getTime() - period;
        float afterTime = segment + 1 < count - 1 || !looping ? after.getTime() : after.getTime() + period;
        float inTangent = resolveTangentScale(span, beforeTime, b.getTime(), before == b);
        float outTangent = resolveTangentScale(span, a.getTime(), afterTime, after == a);

        sampleChannel(
                before.getPosition(), a.getPosition(), b.getPosition(), after.getPosition(),
                inTangent, outTangent, t, outPosition);
        sampleChannel(
                before.getRotation(), a.getRotation(), b.getRotation(), after.getRotation(),
                inTangent, outTangent, t, outRotation);
        sampleChannel(
                before.getScale(), a.getScale(), b.getScale(), after.getScale(),
                inTangent, outTangent, t, outScale);
    }

    private static void sampleChannel(
            Vector3 before,
            Vector3 a,
            Vector3 b,
            Vector3 after,
            float inTangent,
            float outTangent,
            float t,
            Vector3 out) {

        out.set(
                hermite(before.x, a.x, b.x, after.x, inTangent, outTangent, t),
                hermite(before.y, a.y, b.y, after.y, inTangent, outTangent, t),
                hermite(before.z, a.z, b.z, after.z, inTangent, outTangent, t));
    }

    private static int findSegment(AnimationKeyframeStruct[] keyframes, float time) {

        for (int i = 0; i < keyframes.length - 2; i++)
            if (time < keyframes[i + 1].getTime())
                return i;

        return keyframes.length - 2;
    }

    // A looping track's last key repeats its first, so the neighbour across the seam skips it
    private static AnimationKeyframeStruct resolveBefore(
            AnimationKeyframeStruct[] keyframes,
            int index,
            boolean looping) {

        if (index > 0)
            return keyframes[index - 1];

        return looping ? keyframes[Math.max(0, keyframes.length - 2)] : keyframes[index + 1];
    }

    private static AnimationKeyframeStruct resolveAfter(
            AnimationKeyframeStruct[] keyframes,
            int index,
            boolean looping) {

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

    private static void applyKeyframe(
            AnimationKeyframeStruct keyframe,
            Vector3 outPosition,
            Vector3 outRotation,
            Vector3 outScale) {

        outPosition.set(keyframe.getPosition());
        outRotation.set(keyframe.getRotation());
        outScale.set(keyframe.getScale());
    }

    // Utility \\

    private static void lerpInto(Vector3 target, Vector3 to, float weight) {
        target.set(
                lerp(target.x, to.x, weight),
                lerp(target.y, to.y, weight),
                lerp(target.z, to.z, weight));
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private static float smoothStep(float t) {
        return t * t * (3f - 2f * t);
    }

    // Accessible \\

    public RigHandle getRigHandle() {
        return rigHandle;
    }

    public AnimationTreeHandle getAnimationTreeHandle() {
        return animationTreeHandle;
    }

    public Matrix4[] getSkinningMatrices() {
        return skinningMatrices;
    }

    public Matrix4 getSkinningMatrix(int boneIndex) {
        return skinningMatrices[boneIndex];
    }
}
