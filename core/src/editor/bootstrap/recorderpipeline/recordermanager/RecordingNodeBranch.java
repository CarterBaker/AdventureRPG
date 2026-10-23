package editor.bootstrap.recorderpipeline.recordermanager;

import application.bootstrap.entitypipeline.entity.EntityAction;
import application.bootstrap.entitypipeline.entity.EntityState;
import editor.bootstrap.recorderpipeline.recording.RecorderChannel;
import editor.bootstrap.recorderpipeline.recording.RecorderNodeKind;
import editor.bootstrap.recorderpipeline.recording.RecordingBodyTrackStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingClipStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingInstance;
import editor.bootstrap.recorderpipeline.recording.RecordingNodeStruct;
import editor.bootstrap.recorderpipeline.util.RecordingSampleUtility;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class RecordingNodeBranch extends BranchPackage {

    /*
     * Reads what every character is doing in every clip that plays it — the
     * player in a clip playing the player, each resident in a clip playing
     * the characters — and edits it. A movement node opens where a movement
     * state
     * changes and runs to the next change; a gesture node is one action of
     * its hands, from its first sample to its last. Nodes are rebuilt from
     * the takes into structs kept from one rebuild to the next. A movement is
     * retimed by moving the change that opens it, given another state, or
     * removed so what came before carries on; a gesture is moved whole,
     * sample by sample, or removed so the hands are free. Every edit changes
     * the take, so every clip of it shows the change. Nothing here marks the
     * recording changed; RecorderManager does, once, for each edit.
     */

    // Labels
    private String[] stateLabels;
    private String[] actionLabels;

    // Pool
    private ObjectArrayList<RecordingNodeStruct> nodePool;

    // Scratch
    private FloatArrayList gestureTimes;
    private IntArrayList gestureActions;
    private IntArrayList gestureClips;
    private FloatArrayList gesturePhases;
    private IntArrayList gestureSequences;

    // Base \\

    @Override
    protected void create() {

        // Labels
        this.stateLabels = new String[EntityState.VALUES.length];
        this.actionLabels = new String[EntityAction.VALUES.length];

        for (int i = 0; i < stateLabels.length; i++)
            stateLabels[i] = toLabel(EntityState.VALUES[i].name());

        for (int i = 0; i < actionLabels.length; i++)
            actionLabels[i] = toLabel(EntityAction.VALUES[i].name());

        // Pool
        this.nodePool = new ObjectArrayList<>();

        // Scratch
        this.gestureTimes = new FloatArrayList();
        this.gestureActions = new IntArrayList();
        this.gestureClips = new IntArrayList();
        this.gesturePhases = new FloatArrayList();
        this.gestureSequences = new IntArrayList();
    }

    // An enum constant's name as words, its first letter upper case
    private String toLabel(String constantName) {

        String words = constantName.toLowerCase().replace(
                EditorSetting.RECORDER_NODE_NAME_SEPARATOR,
                EditorSetting.RECORDER_NODE_WORD_SEPARATOR);

        return Character.toUpperCase(words.charAt(0)) + words.substring(1);
    }

    // Collect \\

    // Every node of every character every clip plays, the clips in timeline order and each character's nodes in time
    // order
    void collectNodes(RecordingInstance recording, ObjectArrayList<RecordingNodeStruct> nodes) {

        for (int i = 0; i < nodes.size(); i++)
            nodePool.add(nodes.get(i));

        nodes.clear();

        if (recording == null)
            return;

        ObjectArrayList<RecordingClipStruct> clips = recording.getClips();

        for (int i = 0; i < clips.size(); i++) {

            RecordingClipStruct clip = clips.get(i);

            if (clip.getLength() <= 0f)
                continue;

            if (clip.holds(RecorderChannel.PLAYER) && clip.getTake().hasPlayerTrack())
                collectTrack(clip, i, clip.getTake().getPlayerTrack(), nodes);

            if (!clip.holds(RecorderChannel.CHARACTERS))
                continue;

            ObjectArrayList<RecordingBodyTrackStruct> residentTracks = clip.getTake().getResidentTracks();

            for (int j = 0; j < residentTracks.size(); j++)
                collectTrack(clip, i, residentTracks.get(j), nodes);
        }
    }

    private void collectTrack(
            RecordingClipStruct clip,
            int clipIndex,
            RecordingBodyTrackStruct track,
            ObjectArrayList<RecordingNodeStruct> nodes) {

        if (track.isEmpty())
            return;

        collectMovements(clip, clipIndex, track, nodes);
        collectGestures(clip, clipIndex, track, nodes);
    }

    // One node for each run of one movement state the clip plays, the first from wherever its run began
    private void collectMovements(
            RecordingClipStruct clip,
            int clipIndex,
            RecordingBodyTrackStruct track,
            ObjectArrayList<RecordingNodeStruct> nodes) {

        IntArrayList states = track.getStates();
        int run = findRunStart(states, Math.max(0, track.findSample(clip.getInPoint())));
        int previousValue = run > 0 ? states.getInt(run - 1) : EngineSetting.INDEX_NOT_FOUND;
        int previousRun = run > 0 ? findRunStart(states, run - 1) : EngineSetting.INDEX_NOT_FOUND;

        while (run < track.size() && track.getTime(run) < clip.getOutPoint()) {

            int value = states.getInt(run);
            int end = findRunEnd(states, run);
            float takeStart = track.getTime(run);
            RecordingNodeStruct node = nextNode(nodes);

            node.set(clipIndex, track, RecorderNodeKind.MOVEMENT, value, stateLabels[value],
                    run, end, takeStart, resolveRunEnd(clip, track, end), clip);
            node.setEditing(
                    previousValue,
                    previousRun != EngineSetting.INDEX_NOT_FOUND
                            ? Math.max(clip.getInPoint(), track.getTime(previousRun + 1))
                            : takeStart,
                    Math.min(clip.getOutPoint(), track.getTime(end - 1)),
                    previousRun != EngineSetting.INDEX_NOT_FOUND && takeStart > clip.getInPoint());

            previousValue = value;
            previousRun = run;
            run = end;
        }
    }

    // One node for each action of the hands the clip plays, each free to move only as far as its neighbours
    private void collectGestures(
            RecordingClipStruct clip,
            int clipIndex,
            RecordingBodyTrackStruct track,
            ObjectArrayList<RecordingNodeStruct> nodes) {

        IntArrayList actions = track.getActions();
        int sample = findGestureStart(track, Math.max(0, track.findSample(clip.getInPoint())));
        float previousEnd = clip.getInPoint();
        RecordingNodeStruct previous = null;

        while (sample < track.size() && track.getTime(sample) < clip.getOutPoint()) {

            int action = actions.getInt(sample);

            if (action == EntityAction.NONE.ordinal()) {
                sample++;
                continue;
            }

            int end = findGestureEnd(track, sample);
            float takeStart = track.getTime(sample);
            float takeEnd = resolveRunEnd(clip, track, end);
            RecordingNodeStruct node = nextNode(nodes);

            node.set(clipIndex, track, RecorderNodeKind.GESTURE, action, actionLabels[action],
                    sample, end, takeStart, takeEnd, clip);
            node.setEditing(
                    EngineSetting.INDEX_NOT_FOUND,
                    previousEnd,
                    clip.getOutPoint() - node.getTakeLength(),
                    takeStart >= clip.getInPoint() && takeEnd <= clip.getOutPoint());

            if (previous != null)
                previous.setLatestStart(Math.min(previous.getLatestStart(), takeStart - previous.getTakeLength()));

            previousEnd = Math.max(previousEnd, takeEnd);
            previous = node;
            sample = end;
        }
    }

    // A run ends at the next sample, or when it runs to the last, where the take does
    private float resolveRunEnd(RecordingClipStruct clip, RecordingBodyTrackStruct track, int end) {
        return end < track.size()
                ? track.getTime(end)
                : Math.max(track.getEndTime(), clip.getTake().getDuration());
    }

    private int findRunStart(IntArrayList column, int sample) {

        int start = sample;

        while (start > 0 && column.getInt(start - 1) == column.getInt(sample))
            start--;

        return start;
    }

    private int findRunEnd(IntArrayList column, int sample) {

        int end = sample + 1;

        while (end < column.size() && column.getInt(end) == column.getInt(sample))
            end++;

        return end;
    }

    // The first sample of the action the sample is in, the sample itself when its hands are free
    private int findGestureStart(RecordingBodyTrackStruct track, int sample) {

        if (track.getActions().getInt(sample) == EntityAction.NONE.ordinal())
            return sample;

        int start = sample;

        while (start > 0 && isSameGesture(track, start - 1, sample))
            start--;

        return start;
    }

    private int findGestureEnd(RecordingBodyTrackStruct track, int sample) {

        int end = sample + 1;

        while (end < track.size() && isSameGesture(track, end, sample))
            end++;

        return end;
    }

    // One action carries on while both its action and its sequence hold
    private boolean isSameGesture(RecordingBodyTrackStruct track, int sample, int other) {
        return track.getActions().getInt(sample) == track.getActions().getInt(other)
                && track.getSequences().getInt(sample) == track.getSequences().getInt(other);
    }

    private RecordingNodeStruct nextNode(ObjectArrayList<RecordingNodeStruct> nodes) {

        RecordingNodeStruct node = nodePool.isEmpty()
                ? new RecordingNodeStruct()
                : nodePool.remove(nodePool.size() - 1);

        nodes.add(node);
        return node;
    }

    // Find \\

    // The node of the character on the clip a press lies on: in the upper row the gesture under it, below the
    // movement under it, a movement's notch within the tolerance taking precedence; -1 for none
    int findNodeAt(
            ObjectArrayList<RecordingNodeStruct> nodes,
            int clipIndex,
            RecordingBodyTrackStruct track,
            float time,
            boolean nameRow,
            float tolerance) {

        int found = EngineSetting.INDEX_NOT_FOUND;

        for (int i = 0; i < nodes.size(); i++) {

            RecordingNodeStruct node = nodes.get(i);

            if (node.getClipIndex() != clipIndex || node.getTrack() != track)
                continue;

            if (nameRow) {

                if (node.getKind() == RecorderNodeKind.GESTURE
                        && time >= node.getStart() - tolerance && time <= node.getEnd() + tolerance)
                    return i;

                continue;
            }

            if (node.getKind() != RecorderNodeKind.MOVEMENT)
                continue;

            if (isOnNotch(node, time, tolerance))
                return i;

            if (found == EngineSetting.INDEX_NOT_FOUND && node.contains(time))
                found = i;
        }

        return found;
    }

    // True when the moment is on the notch a movement opens with, and that notch can be dragged
    boolean isOnNotch(RecordingNodeStruct node, float time, float tolerance) {
        return node.getKind() == RecorderNodeKind.MOVEMENT
                && node.isMovable()
                && Math.abs(time - node.getStart()) <= tolerance;
    }

    // The node of the kind of the character on the clip whose run holds the take's moment, -1 for none
    int findNode(
            ObjectArrayList<RecordingNodeStruct> nodes,
            int clipIndex,
            RecordingBodyTrackStruct track,
            RecorderNodeKind kind,
            float takeTime) {

        for (int i = 0; i < nodes.size(); i++) {

            RecordingNodeStruct node = nodes.get(i);

            if (node.getClipIndex() == clipIndex && node.getTrack() == track && node.getKind() == kind
                    && takeTime >= node.getTakeStart() && takeTime < node.getTakeEnd())
                return i;
        }

        return EngineSetting.INDEX_NOT_FOUND;
    }

    // The moment of the timeline a movable node's start would be dragged to, kept where its samples allow
    float resolveLanding(RecordingNodeStruct node, RecordingClipStruct clip, float time) {
        return clip.toTimelineTime(Math.clamp(clip.toTakeTime(time), node.getEarliestStart(), node.getLatestStart()));
    }

    // Movement \\

    // The movement's run moves the number of states along, wrapping round
    void stepState(RecordingBodyTrackStruct track, RecordingNodeStruct node, int step) {
        fill(track.getStates(), node.getFirstSample(), node.getEndSample(),
                Math.floorMod(node.getValue() + step, EntityState.VALUES.length));
    }

    // The movement's run carries on what came before it, or what comes after when nothing did; false when the take
    // moves only one way throughout
    boolean removeMovement(RecordingBodyTrackStruct track, RecordingNodeStruct node) {

        IntArrayList states = track.getStates();
        int replacement = node.getPreviousValue() != EngineSetting.INDEX_NOT_FOUND
                ? node.getPreviousValue()
                : node.getEndSample() < track.size()
                        ? states.getInt(node.getEndSample())
                        : EngineSetting.INDEX_NOT_FOUND;

        if (replacement == EngineSetting.INDEX_NOT_FOUND)
            return false;

        fill(states, node.getFirstSample(), node.getEndSample(), replacement);
        return true;
    }

    // The movement opens at the take's moment instead, what came before it ending there
    void retimeMovement(RecordingBodyTrackStruct track, RecordingNodeStruct node, float takeTime) {

        IntArrayList states = track.getStates();
        int boundary = findSampleFrom(track, takeTime);

        if (boundary < node.getFirstSample())
            fill(states, boundary, node.getFirstSample(), node.getValue());
        else
            fill(states, node.getFirstSample(), boundary, node.getPreviousValue());
    }

    // Gesture \\

    void removeGesture(RecordingBodyTrackStruct track, RecordingNodeStruct node) {
        clearGesture(track, node.getFirstSample(), node.getEndSample());
    }

    // The gesture played from the take's moment instead, each sample taking the action as it stood that far into it
    void moveGesture(RecordingBodyTrackStruct track, RecordingNodeStruct node, float takeStart) {

        float shift = takeStart - node.getTakeStart();
        float takeEnd = node.getTakeEnd() + shift;

        copyGesture(track, node.getFirstSample(), node.getEndSample());
        clearGesture(track, node.getFirstSample(), node.getEndSample());

        for (int sample = findSampleFrom(track, takeStart);
                sample < track.size() && track.getTime(sample) < takeEnd;
                sample++) {

            float sourceTime = track.getTime(sample) - shift;
            int source = Math.max(0, RecordingSampleUtility.findSample(gestureTimes, sourceTime));

            track.getActions().set(sample, gestureActions.getInt(source));
            track.getClips().set(sample, gestureClips.getInt(source));
            track.getPhases().set(sample, RecordingSampleUtility.blend(
                    gesturePhases, source, RecordingSampleUtility.resolveBlend(gestureTimes, source, sourceTime)));
            track.getSequences().set(sample, gestureSequences.getInt(source));
        }
    }

    private void copyGesture(RecordingBodyTrackStruct track, int firstSample, int endSample) {

        gestureTimes.clear();
        gestureActions.clear();
        gestureClips.clear();
        gesturePhases.clear();
        gestureSequences.clear();

        for (int sample = firstSample; sample < endSample; sample++) {
            gestureTimes.add(track.getTime(sample));
            gestureActions.add(track.getActions().getInt(sample));
            gestureClips.add(track.getClips().getInt(sample));
            gesturePhases.add(track.getPhases().getFloat(sample));
            gestureSequences.add(track.getSequences().getInt(sample));
        }
    }

    // The hands free over the samples, as though no action had been taken there
    private void clearGesture(RecordingBodyTrackStruct track, int firstSample, int endSample) {

        fill(track.getActions(), firstSample, endSample, EntityAction.NONE.ordinal());
        fill(track.getClips(), firstSample, endSample, EditorSetting.RECORDER_NO_CLIP);

        for (int sample = firstSample; sample < endSample; sample++)
            track.getPhases().set(sample, 0f);
    }

    // Utility \\

    // The first sample at or after the take's moment
    private int findSampleFrom(RecordingBodyTrackStruct track, float takeTime) {

        int sample = track.findSample(takeTime);

        if (sample < 0)
            return 0;

        return track.getTime(sample) < takeTime ? sample + 1 : sample;
    }

    private void fill(IntArrayList column, int firstSample, int endSample, int value) {
        for (int sample = firstSample; sample < endSample; sample++)
            column.set(sample, value);
    }
}
