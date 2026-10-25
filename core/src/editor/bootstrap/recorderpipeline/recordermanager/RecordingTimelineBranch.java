package editor.bootstrap.recorderpipeline.recordermanager;

import editor.bootstrap.recorderpipeline.recording.RecordingClipStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingInstance;
import editor.bootstrap.recorderpipeline.recording.RecordingTakeStruct;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class RecordingTimelineBranch extends BranchPackage {

    /*
     * Lays clips on a recording's timeline and edits them. A clip asked onto a
     * lane where it would cover another lands on the nearest clear lane above,
     * then below, and only when every lane is taken on the one asked for. A
     * take just recorded lands above every clip it was recorded over, so it
     * plays over them. Cutting a clip in two at a moment lays a copy of it
     * where it lies and trims the clip's end and the copy's start to the
     * moment, trimming moves one end of a clip to a moment, and stamping lays
     * a copy of it at a moment; a cut being placed snaps as a drag does and
     * keeps inside its clip. A clip's edges are found near a press, a dragged
     * edge reaches no further than its take or the clips beside it on its
     * lane, and what is dragged snaps to the nearest edge of the timeline —
     * its start, the playhead or another clip's end — within a tolerance.
     * Nothing here marks the recording changed; RecorderManager does, once,
     * for each edit it makes through here.
     */

    // Lanes \\

    // The lane asked for when the span lies clear on it, else the nearest clear lane above, then below
    int resolveLane(
            RecordingInstance recording,
            float start,
            float end,
            int lane,
            RecordingClipStruct ignored) {

        int asked = Math.clamp(lane, 0, EditorSetting.RECORDER_LANE_COUNT - 1);

        for (int distance = 0; distance < EditorSetting.RECORDER_LANE_COUNT; distance++) {

            if (asked + distance < EditorSetting.RECORDER_LANE_COUNT
                    && isClear(recording, start, end, asked + distance, ignored))
                return asked + distance;

            if (asked - distance >= 0 && isClear(recording, start, end, asked - distance, ignored))
                return asked - distance;
        }

        return asked;
    }

    // Just above every clip the span overlaps, so what lands there plays over them
    int resolveTopLane(RecordingInstance recording, float start, float end) {

        ObjectArrayList<RecordingClipStruct> clips = recording.getClips();
        int lane = 0;

        for (int i = 0; i < clips.size(); i++)
            if (clips.get(i).overlaps(start, end))
                lane = Math.max(lane, clips.get(i).getLane() + 1);

        return resolveLane(recording, start, end, lane, null);
    }

    private boolean isClear(
            RecordingInstance recording,
            float start,
            float end,
            int lane,
            RecordingClipStruct ignored) {

        ObjectArrayList<RecordingClipStruct> clips = recording.getClips();

        for (int i = 0; i < clips.size(); i++) {

            RecordingClipStruct clip = clips.get(i);

            if (clip != ignored && clip.getLane() == lane && clip.overlaps(start, end))
                return false;
        }

        return true;
    }

    // Place \\

    // The whole take laid from the moment, on the lane asked for or the nearest clear one
    int placeTake(RecordingInstance recording, RecordingTakeStruct take, float start, int lane) {

        float clampedStart = Math.max(0f, start);
        int landing = resolveLane(recording, clampedStart, clampedStart + take.getDuration(), lane, null);

        return recording.addClip(new RecordingClipStruct(
                take, 0f, take.getDuration(), landing, clampedStart, take.getChannelMask()));
    }

    // The whole take laid where it was recorded, over everything it was recorded against
    int placeRecordedTake(RecordingInstance recording, RecordingTakeStruct take, float start) {

        int lane = resolveTopLane(recording, start, start + take.getDuration());

        return recording.addClip(new RecordingClipStruct(
                take, 0f, take.getDuration(), lane, start, take.getChannelMask()));
    }

    // Edges \\

    // The clip on the lane whose start the press is on — within the tolerance of it outside, and inside within its
    // edge, at most a share of its length — the nearest when two are; -1 for none
    int findStartEdge(RecordingInstance recording, float time, int lane, float tolerance) {

        ObjectArrayList<RecordingClipStruct> clips = recording.getClips();
        int found = EngineSetting.INDEX_NOT_FOUND;
        float nearest = Float.MAX_VALUE;

        for (int i = 0; i < clips.size(); i++) {

            RecordingClipStruct clip = clips.get(i);
            float distance = clip.getStart() - time;

            if (clip.getLane() == lane && distance <= tolerance && -distance <= resolveEdgeZone(clip, tolerance)
                    && Math.abs(distance) < nearest) {
                found = i;
                nearest = Math.abs(distance);
            }
        }

        return found;
    }

    // The clip on the lane whose end the press is on, as its start is found; -1 for none
    int findEndEdge(RecordingInstance recording, float time, int lane, float tolerance) {

        ObjectArrayList<RecordingClipStruct> clips = recording.getClips();
        int found = EngineSetting.INDEX_NOT_FOUND;
        float nearest = Float.MAX_VALUE;

        for (int i = 0; i < clips.size(); i++) {

            RecordingClipStruct clip = clips.get(i);
            float distance = time - clip.getEnd();

            if (clip.getLane() == lane && distance <= tolerance && -distance <= resolveEdgeZone(clip, tolerance)
                    && Math.abs(distance) < nearest) {
                found = i;
                nearest = Math.abs(distance);
            }
        }

        return found;
    }

    // How far inside a clip its edge reaches, so a short clip keeps a middle to take hold of
    private float resolveEdgeZone(RecordingClipStruct clip, float tolerance) {
        return Math.min(tolerance, clip.getLength() * EditorSetting.RECORDER_EDGE_GRAB_SHARE);
    }

    // Snap \\

    // The nearest edge of the timeline within the tolerance — its start, the playhead, or either end of any clip but
    // the one ignored — else the moment itself
    float snapTime(
            RecordingInstance recording,
            float time,
            float playhead,
            float tolerance,
            RecordingClipStruct ignored) {

        float snapped = time;
        float nearest = tolerance;

        if (Math.abs(time) <= nearest) {
            snapped = 0f;
            nearest = Math.abs(time);
        }

        if (Math.abs(playhead - time) <= nearest) {
            snapped = playhead;
            nearest = Math.abs(playhead - time);
        }

        ObjectArrayList<RecordingClipStruct> clips = recording.getClips();

        for (int i = 0; i < clips.size(); i++) {

            RecordingClipStruct clip = clips.get(i);

            if (clip == ignored)
                continue;

            if (Math.abs(clip.getStart() - time) <= nearest) {
                snapped = clip.getStart();
                nearest = Math.abs(clip.getStart() - time);
            }

            if (Math.abs(clip.getEnd() - time) <= nearest) {
                snapped = clip.getEnd();
                nearest = Math.abs(clip.getEnd() - time);
            }
        }

        return snapped;
    }

    // A span's start moved so whichever of its ends lies nearer an edge of the timeline lands on it
    float snapSpan(
            RecordingInstance recording,
            float start,
            float length,
            float playhead,
            float tolerance,
            RecordingClipStruct ignored) {

        float startShift = snapTime(recording, start, playhead, tolerance, ignored) - start;
        float endShift = snapTime(recording, start + length, playhead, tolerance, ignored) - (start + length);

        if (startShift == 0f)
            return start + endShift;

        if (endShift == 0f || Math.abs(startShift) <= Math.abs(endShift))
            return start + startShift;

        return start + endShift;
    }

    // Trim \\

    // The moment a clip's start can be dragged to: no earlier than its take, the timeline or the clip before it on
    // its lane, and no later than leaves it the shortest a take may be
    float resolveTrimStart(RecordingInstance recording, RecordingClipStruct clip, float time) {

        ObjectArrayList<RecordingClipStruct> clips = recording.getClips();
        float earliest = Math.max(0f, clip.toTimelineTime(0f));

        for (int i = 0; i < clips.size(); i++) {

            RecordingClipStruct other = clips.get(i);

            if (other != clip && other.getLane() == clip.getLane() && other.getStart() < clip.getStart())
                earliest = Math.max(earliest, other.getEnd());
        }

        earliest = Math.min(earliest, clip.getStart());

        return Math.clamp(time, earliest, Math.max(earliest, clip.getEnd() - EditorSetting.RECORDER_MIN_TAKE_SECONDS));
    }

    // The moment a clip's end can be dragged to: no later than its take or the clip after it on its lane, and no
    // earlier than leaves it the shortest a take may be
    float resolveTrimEnd(RecordingInstance recording, RecordingClipStruct clip, float time) {

        ObjectArrayList<RecordingClipStruct> clips = recording.getClips();
        float latest = clip.toTimelineTime(clip.getTake().getDuration());

        for (int i = 0; i < clips.size(); i++) {

            RecordingClipStruct other = clips.get(i);

            if (other != clip && other.getLane() == clip.getLane() && other.getStart() > clip.getStart())
                latest = Math.min(latest, other.getStart());
        }

        latest = Math.max(latest, clip.getEnd());

        return Math.clamp(time, Math.min(latest, clip.getStart() + EditorSetting.RECORDER_MIN_TAKE_SECONDS), latest);
    }

    // The clip's start brought to the moment, what plays after it left where it was
    void trimStartTo(RecordingClipStruct clip, float time) {
        clip.setSpan(clip.toTakeTime(time), clip.getOutPoint());
        clip.setStart(time);
    }

    void trimEndTo(RecordingClipStruct clip, float time) {
        clip.setSpan(clip.getInPoint(), clip.toTakeTime(time));
    }

    // Edit \\

    void moveClip(RecordingInstance recording, RecordingClipStruct clip, float start, int lane) {

        float clampedStart = Math.max(0f, start);

        clip.setLane(resolveLane(recording, clampedStart, clampedStart + clip.getLength(), lane, clip));
        clip.setStart(clampedStart);
    }

    // The clip cut in two at the moment: a copy laid where it lies, the clip's end and the copy's start both
    // trimmed to the moment; the copy's index, -1 when the moment is not inside it
    int splitClip(RecordingInstance recording, RecordingClipStruct clip, float time) {

        if (!isInside(clip, time))
            return EngineSetting.INDEX_NOT_FOUND;

        RecordingClipStruct second = clip.copyTo(clip.getLane(), clip.getStart());

        trimEndTo(clip, time);
        trimStartTo(second, time);

        return recording.addClip(second);
    }

    // The moment a cut through the clip would land at: snapped to the timeline's edges, and kept far enough inside
    // the clip that both pieces are at least the shortest a take may be
    float resolveCut(
            RecordingInstance recording,
            RecordingClipStruct clip,
            float time,
            float playhead,
            float tolerance) {

        float snapped = snapTime(recording, time, playhead, tolerance, clip);
        float earliest = clip.getStart() + EditorSetting.RECORDER_MIN_TAKE_SECONDS;
        float latest = clip.getEnd() - EditorSetting.RECORDER_MIN_TAKE_SECONDS;

        return earliest <= latest ? Math.clamp(snapped, earliest, latest) : snapped;
    }

    // The clip's start brought to the moment, what plays after it left where it was; false when not inside it
    boolean trimStart(RecordingClipStruct clip, float time) {

        if (!isInside(clip, time))
            return false;

        trimStartTo(clip, time);
        return true;
    }

    // The clip's end brought to the moment; false when the moment is not inside it
    boolean trimEnd(RecordingClipStruct clip, float time) {

        if (!isInside(clip, time))
            return false;

        trimEndTo(clip, time);
        return true;
    }

    // A copy of the clip laid from the moment, on its lane or the nearest clear one
    int stampClip(RecordingInstance recording, RecordingClipStruct clip, float time) {

        float start = Math.max(0f, time);
        int lane = resolveLane(recording, start, start + clip.getLength(), clip.getLane(), null);

        return recording.addClip(clip.copyTo(lane, start));
    }

    private boolean isInside(RecordingClipStruct clip, float time) {
        return time >= clip.getStart() + EditorSetting.RECORDER_MIN_TAKE_SECONDS
                && time <= clip.getEnd() - EditorSetting.RECORDER_MIN_TAKE_SECONDS;
    }
}
