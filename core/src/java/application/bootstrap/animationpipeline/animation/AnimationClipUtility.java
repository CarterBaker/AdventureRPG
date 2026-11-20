package application.bootstrap.animationpipeline.animation;

import engine.root.EngineUtility;
import engine.util.mathematics.vectors.Vector3;

public class AnimationClipUtility extends EngineUtility {

    /*
     * Stateless sampling of a clip's bone tracks, shared by every pose the
     * engine strikes and every editor that reads a clip back. A track is
     * sampled at a phase of the clip from the curve of each axis of each
     * channel, as offsets from the bind pose; a looping clip's curves run
     * round their seam, and a one-shot's ease into its holds. An axis with
     * no curve holds its channel's default, and a bone with no track samples
     * as no offset. A morph curve is sampled linearly
     * between its keys, holding past either end. Writes into caller-owned
     * vectors, so sampling never allocates.
     */

    // Sampling \\

    // A morph curve's weight at a phase of its clip
    public static float sampleMorph(AnimationClipHandle clip, int trackIndex, float phase) {

        MorphTrackStruct track = clip.getMorphTrack(trackIndex);
        float time = phase * clip.getDuration();
        int last = track.getKeyCount() - 1;

        if (time <= track.getTime(0))
            return track.getValue(0);

        if (time >= track.getTime(last))
            return track.getValue(last);

        int key = 0;

        while (track.getTime(key + 1) < time)
            key++;

        float t = (time - track.getTime(key)) / (track.getTime(key + 1) - track.getTime(key));

        return track.getValue(key) + (track.getValue(key + 1) - track.getValue(key)) * t;
    }

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

        BoneTrackStruct track = clip.getBoneTrack(boneIndex);
        float time = phase * clip.getDuration();

        sampleChannel(track, AnimationChannel.POSITION, time, outPosition);
        sampleChannel(track, AnimationChannel.ROTATION, time, outRotation);
        sampleChannel(track, AnimationChannel.SCALE, time, outScale);
    }

    private static void sampleChannel(BoneTrackStruct track, AnimationChannel channel, float time, Vector3 out) {
        out.set(
                track.sample(channel.toSlot(0), time),
                track.sample(channel.toSlot(1), time),
                track.sample(channel.toSlot(2), time));
    }
}
