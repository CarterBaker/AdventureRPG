package editor.bootstrap.recorderpipeline.recordermanager;

import editor.bootstrap.recorderpipeline.recording.RecorderClipEdge;
import editor.bootstrap.recorderpipeline.recording.RecordingClipStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingInstance;
import editor.bootstrap.recorderpipeline.recording.RecordingTransitionStruct;
import editor.bootstrap.transitionpipeline.transition.TransitionHandle;
import editor.bootstrap.transitionpipeline.transitionmanager.TransitionManager;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class RecordingTransitionBranch extends BranchPackage {

    /*
     * Reads and edits the transitions laid on a recording's clips. A
     * transition runs inside its clip from the edge it lies on. At a moment
     * the picture passes through the transition of the highest clip showing
     * a picture whose edge's transition runs across it: into a clip, the
     * timeline as it plays without the clip gives way to it — the clip that
     * ends just where it begins on its lane carried on past its own end — and
     * out of a clip it gives way to the timeline without it, the clip that
     * begins just where it ends started early. Each clip's own sounds fade in
     * and out with its transitions. A transition dropped on the timeline
     * lands on the nearer edge of the clip under it, and one dropped on the
     * end of a clip another follows on its lane lands on the start of the one
     * following, so a cut between two clips is one transition. A clip edge is
     * named by one number, the clip's place and its edge together. Nothing
     * here marks the recording changed; RecorderManager does, once, for each
     * edit.
     */

    // Internal
    private TransitionManager transitionManager;

    // Base \\

    @Override
    protected void get() {
        this.transitionManager = get(TransitionManager.class);
    }

    // Resolve \\

    // The transition the picture passes through at the moment, cleared when there is none
    void resolve(RecordingInstance recording, float time, RecordingTransitionStruct transition) {

        transition.clear();

        if (recording == null)
            return;

        ObjectArrayList<RecordingClipStruct> clips = recording.getClips();
        RecordingClipStruct found = null;
        RecorderClipEdge foundEdge = null;

        for (int i = 0; i < clips.size(); i++) {

            RecordingClipStruct clip = clips.get(i);

            if (!clip.showsPicture() || found != null && clip.getLane() < found.getLane())
                continue;

            for (int e = 0; e < RecorderClipEdge.VALUES.length; e++) {

                RecorderClipEdge edge = RecorderClipEdge.VALUES[e];

                if (clip.isInTransition(edge, time)) {
                    found = clip;
                    foundEdge = edge;
                }
            }
        }

        if (found == null)
            return;

        RecordingClipStruct carried = recording.findAdjoiningClip(found, foundEdge == RecorderClipEdge.START);

        if (carried != null && !carried.showsPicture())
            carried = null;

        transition.set(
                found,
                foundEdge,
                carried,
                resolveHandle(found, foundEdge),
                resolveMix(found, foundEdge, time),
                carried != null || recording.showsPictureWithout(time, found));
    }

    // How far the picture has gone over at the moment, eased as the transition paces itself
    float resolveMix(RecordingClipStruct clip, RecorderClipEdge edge, float time) {

        float seconds = clip.resolveTransitionSeconds(edge);
        float progress = seconds > 0f ? Math.clamp((time - clip.resolveTransitionStart(edge)) / seconds, 0f, 1f) : 1f;

        return resolveHandle(clip, edge).getEasing().apply(progress);
    }

    TransitionHandle resolveHandle(RecordingClipStruct clip, RecorderClipEdge edge) {
        return transitionManager.resolveTransition(clip.getTransitionName(edge));
    }

    // How loud the clip's own sounds are at the moment: faded in across the transition into it and out across the
    // one out of it
    float resolveGain(RecordingClipStruct clip, float time) {

        float gain = 1f;

        if (clip.isInTransition(RecorderClipEdge.START, time))
            gain *= resolveMix(clip, RecorderClipEdge.START, time);

        if (clip.isInTransition(RecorderClipEdge.END, time))
            gain *= 1f - resolveMix(clip, RecorderClipEdge.END, time);

        return gain;
    }

    // Edit \\

    // The transition laid on the edge, running as long as asked within what the clip leaves it
    void setTransition(
            RecordingClipStruct clip,
            RecorderClipEdge edge,
            TransitionHandle transitionHandle,
            float seconds) {
        clip.setTransition(edge, transitionHandle.getTransitionName(), clampSeconds(clip, edge, seconds));
    }

    void setSeconds(RecordingClipStruct clip, RecorderClipEdge edge, float seconds) {
        clip.setTransitionSeconds(edge, clampSeconds(clip, edge, seconds));
    }

    // No shorter than a transition may be, and no longer than the clip leaves beside the other edge's transition
    float clampSeconds(RecordingClipStruct clip, RecorderClipEdge edge, float seconds) {

        RecorderClipEdge other = edge == RecorderClipEdge.START ? RecorderClipEdge.END : RecorderClipEdge.START;
        float room = clip.getLength() - (clip.hasTransition(other) ? clip.getTransitionSeconds(other) : 0f);

        return Math.clamp(seconds, EditorSetting.TRANSITION_MIN_SECONDS,
                Math.max(EditorSetting.TRANSITION_MIN_SECONDS, Math.min(room, EditorSetting.TRANSITION_MAX_SECONDS)));
    }

    // How long the edge's transition runs if its far side is dragged to the moment
    float resolveSecondsTo(RecordingClipStruct clip, RecorderClipEdge edge, float time) {
        return clampSeconds(clip, edge, edge == RecorderClipEdge.START ? time - clip.getStart() : clip.getEnd() - time);
    }

    // The moment the far side of the edge's transition stands at, running for the seconds
    float resolveFarSide(RecordingClipStruct clip, RecorderClipEdge edge, float seconds) {
        return edge == RecorderClipEdge.START ? clip.getStart() + seconds : clip.getEnd() - seconds;
    }

    // Find \\

    // The clip edge a transition dropped at the moment on the lane lands on: the nearer edge of the clip under it,
    // or of a clip whose edge lies within the tolerance; the end of a clip another follows on its lane hands over to
    // the following clip's start; -1 for none
    int findDropTarget(RecordingInstance recording, float time, int lane, float tolerance) {

        ObjectArrayList<RecordingClipStruct> clips = recording.getClips();
        int clipIndex = recording.findClipAt(time, lane);

        for (int i = 0; i < clips.size() && clipIndex == EngineSetting.INDEX_NOT_FOUND; i++) {

            RecordingClipStruct clip = clips.get(i);

            if (clip.getLane() == lane && time >= clip.getStart() - tolerance && time <= clip.getEnd() + tolerance)
                clipIndex = i;
        }

        if (clipIndex == EngineSetting.INDEX_NOT_FOUND)
            return EngineSetting.INDEX_NOT_FOUND;

        RecordingClipStruct clip = clips.get(clipIndex);

        return resolveEdgeTarget(recording, clipIndex,
                time < clip.getStart() + clip.getLength() * EditorSetting.RECORDER_TRANSITION_EDGE_SHARE
                        ? RecorderClipEdge.START
                        : RecorderClipEdge.END);
    }

    // The clip edge a transition laid on the clip's edge lands on: the edge itself, or the start of the clip that
    // follows on its lane when the edge is its end
    int resolveEdgeTarget(RecordingInstance recording, int clipIndex, RecorderClipEdge edge) {

        RecordingClipStruct following = edge == RecorderClipEdge.END
                ? recording.findAdjoiningClip(recording.getClips().get(clipIndex), false)
                : null;

        return following != null
                ? toTarget(recording.getClips().indexOf(following), RecorderClipEdge.START)
                : toTarget(clipIndex, edge);
    }

    // The clip edge whose transition runs under the moment on the lane, the highest clip's where two meet; -1 for
    // none
    int findTransitionAt(RecordingInstance recording, float time, int lane, float tolerance) {

        ObjectArrayList<RecordingClipStruct> clips = recording.getClips();

        for (int i = clips.size() - 1; i >= 0; i--) {

            RecordingClipStruct clip = clips.get(i);

            if (clip.getLane() != lane)
                continue;

            for (int e = 0; e < RecorderClipEdge.VALUES.length; e++) {

                RecorderClipEdge edge = RecorderClipEdge.VALUES[e];
                float from = clip.resolveTransitionStart(edge);
                float seconds = clip.resolveTransitionSeconds(edge);

                if (clip.hasTransition(edge) && time >= from - tolerance && time <= from + seconds + tolerance)
                    return toTarget(i, edge);
            }
        }

        return EngineSetting.INDEX_NOT_FOUND;
    }

    // The moment of the timeline a clip edge stands at
    float resolveEdgeTime(RecordingClipStruct clip, RecorderClipEdge edge) {
        return edge == RecorderClipEdge.START ? clip.getStart() : clip.getEnd();
    }

    // Targets \\

    int toTarget(int clipIndex, RecorderClipEdge edge) {
        return clipIndex * RecorderClipEdge.VALUES.length + edge.ordinal();
    }

    int toClipIndex(int target) {
        return target / RecorderClipEdge.VALUES.length;
    }

    RecorderClipEdge toEdge(int target) {
        return RecorderClipEdge.VALUES[target % RecorderClipEdge.VALUES.length];
    }

    // The number typed, NaN when it is not one or lies outside the bounds
    float parseSeconds(String text) {

        try {
            float value = Float.parseFloat(text.trim());
            return value >= EditorSetting.TRANSITION_MIN_SECONDS && value <= EditorSetting.TRANSITION_MAX_SECONDS
                    ? value
                    : Float.NaN;
        } catch (NumberFormatException e) {
            return Float.NaN;
        }
    }
}
