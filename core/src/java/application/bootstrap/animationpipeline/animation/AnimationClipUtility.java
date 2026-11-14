package application.bootstrap.animationpipeline.animation;

import engine.root.EngineUtility;
import engine.util.mathematics.vectors.Vector3;

public class AnimationClipUtility extends EngineUtility {

    /*
     * Stateless sampling of a clip's bone tracks, shared by every pose the
     * engine strikes and every editor that reads a clip back. A track is
     * sampled at a phase of the clip with Hermite curves through its
     * keyframes as offsets from the bind pose; a looping track wraps its
     * tangents across the seam, and a one-shot eases into its holds. A bone
     * with no track samples as no offset. Writes into caller-owned vectors,
     * so sampling never allocates.
     */

    // Sampling \\

    public static void sampleClip(
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
}
