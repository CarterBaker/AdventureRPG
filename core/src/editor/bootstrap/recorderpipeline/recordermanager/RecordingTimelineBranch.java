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
     * Lays clips on a recording's timeline and edits them. A clip asked onto
     * a lane where it would cover another lands on the nearest clear lane
     * above, then below, and only when every lane is taken on the one asked
     * for. A take just recorded lands above every clip it was recorded over,
     * so it plays over them. Splitting cuts a clip in two at a moment,
     * trimming moves one end of it to a moment, and stamping lays a copy of
     * it at a moment. Nothing here marks the recording changed; RecorderManager
     * does, once, for each edit it makes through here.
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

    // Edit \\

    void moveClip(RecordingInstance recording, RecordingClipStruct clip, float start, int lane) {

        float clampedStart = Math.max(0f, start);

        clip.setLane(resolveLane(recording, clampedStart, clampedStart + clip.getLength(), lane, clip));
        clip.setStart(clampedStart);
    }

    // The clip cut in two at the moment, the second half laid as a clip of its own; -1 when the moment is not
    // inside it
    int splitClip(RecordingInstance recording, RecordingClipStruct clip, float time) {

        if (!isInside(clip, time))
            return EngineSetting.INDEX_NOT_FOUND;

        float cut = clip.toTakeTime(time);
        RecordingClipStruct second = clip.copyTo(clip.getLane(), time);

        second.setSpan(cut, clip.getOutPoint());
        clip.setSpan(clip.getInPoint(), cut);

        return recording.addClip(second);
    }

    // The clip's start brought to the moment, what plays after it left where it was; false when not inside it
    boolean trimStart(RecordingClipStruct clip, float time) {

        if (!isInside(clip, time))
            return false;

        clip.setSpan(clip.toTakeTime(time), clip.getOutPoint());
        clip.setStart(time);
        return true;
    }

    // The clip's end brought to the moment; false when the moment is not inside it
    boolean trimEnd(RecordingClipStruct clip, float time) {

        if (!isInside(clip, time))
            return false;

        clip.setSpan(clip.getInPoint(), clip.toTakeTime(time));
        return true;
    }

    // A copy of the clip laid from the moment, on its lane or the nearest clear one
    int stampClip(RecordingInstance recording, RecordingClipStruct clip, float time) {

        float start = Math.max(0f, time);
        int lane = resolveLane(recording, start, start + clip.getLength(), clip.getLane(), null);

        return recording.addClip(clip.copyTo(lane, start));
    }

    private boolean isInside(RecordingClipStruct clip, float time) {
        return time > clip.getStart() + EditorSetting.RECORDER_MIN_TAKE_SECONDS
                && time < clip.getEnd() - EditorSetting.RECORDER_MIN_TAKE_SECONDS;
    }
}
