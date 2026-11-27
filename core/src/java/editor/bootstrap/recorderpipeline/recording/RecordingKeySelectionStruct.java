package editor.bootstrap.recorderpipeline.recording;

import engine.root.EngineSetting;
import engine.root.StructPackage;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class RecordingKeySelectionStruct extends StructPackage {

    /*
     * Keys selected across any number of curves, as a dope sheet holds them:
     * each by the track it belongs to, the slot of its curve and its place on
     * that curve, every curve's keys kept together and in order, so moving
     * them all walks each curve from the far end first and deleting walks it
     * from the last key back. Where every key stood is anchored once as a
     * drag or a typed shift begins, so they all move by the same amount from
     * there however far they travel.
     */

    // Keys
    private final ObjectArrayList<RecordingCurveTrackStruct> tracks;
    private final IntArrayList slots;
    private final IntArrayList keys;

    // Anchors
    private final FloatArrayList anchorTimes;
    private final DoubleArrayList anchorValues;

    // Constructor \\

    public RecordingKeySelectionStruct() {

        // Keys
        this.tracks = new ObjectArrayList<>();
        this.slots = new IntArrayList();
        this.keys = new IntArrayList();

        // Anchors
        this.anchorTimes = new FloatArrayList();
        this.anchorValues = new DoubleArrayList();
    }

    // Management \\

    // The key joins the selection beside the other keys of its curve, in order; false when it was already in it
    public boolean add(RecordingCurveTrackStruct track, int slot, int key) {

        int at = tracks.size();

        for (int i = 0; i < tracks.size(); i++) {

            if (tracks.get(i) != track || slots.getInt(i) != slot)
                continue;

            if (keys.getInt(i) == key)
                return false;

            if (keys.getInt(i) > key) {
                at = i;
                break;
            }

            at = i + 1;
        }

        tracks.add(at, track);
        slots.add(at, slot);
        keys.add(at, key);
        return true;
    }

    // False when the key was not selected
    public boolean remove(RecordingCurveTrackStruct track, int slot, int key) {

        int index = indexOf(track, slot, key);

        if (index == EngineSetting.INDEX_NOT_FOUND)
            return false;

        tracks.remove(index);
        slots.removeInt(index);
        keys.removeInt(index);
        return true;
    }

    public void clear() {
        tracks.clear();
        slots.clear();
        keys.clear();
    }

    public void copyFrom(RecordingKeySelectionStruct source) {

        clear();

        for (int i = 0; i < source.size(); i++) {
            tracks.add(source.getTrack(i));
            slots.add(source.getSlot(i));
            keys.add(source.getKey(i));
        }
    }

    // Every key stood where it stands now, as a drag or a shift begins
    public void anchor() {

        anchorTimes.clear();
        anchorValues.clear();

        for (int i = 0; i < keys.size(); i++) {

            RecordingCurveStruct curve = getCurve(i);

            anchorTimes.add(curve.getTime(keys.getInt(i)));
            anchorValues.add(curve.getValue(keys.getInt(i)));
        }
    }

    // Every key moved by the same time from where it was anchored, the keys of the one curve given rising by the same
    // value too, held as one so that none passes the earliest or the latest moment allowed; keys already lying outside
    // them may come back in, never go further out. Each curve is walked from the end the keys travel toward, so every
    // key still has room beside the next
    public void moveAnchored(
            float shift,
            double rise,
            RecordingCurveTrackStruct riseTrack,
            int riseSlot,
            float earliest,
            float latest) {

        int count = keys.size();

        if (count == 0 || anchorTimes.size() != count)
            return;

        float first = Float.POSITIVE_INFINITY;
        float last = Float.NEGATIVE_INFINITY;

        for (int i = 0; i < count; i++) {
            first = Math.min(first, anchorTimes.getFloat(i));
            last = Math.max(last, anchorTimes.getFloat(i));
        }

        float held = Math.clamp(shift, Math.min(0f, earliest - first), Math.max(0f, latest - last));

        for (int i = 0; i < count; i++) {

            int index = held > 0f ? count - 1 - i : i;
            boolean rising = tracks.get(index) == riseTrack && slots.getInt(index) == riseSlot;

            getCurve(index).moveKey(keys.getInt(index), anchorTimes.getFloat(index) + held,
                    anchorValues.getDouble(index) + (rising ? rise : 0.0));
        }
    }

    // Accessible \\

    public int indexOf(RecordingCurveTrackStruct track, int slot, int key) {

        for (int i = 0; i < tracks.size(); i++)
            if (tracks.get(i) == track && slots.getInt(i) == slot && keys.getInt(i) == key)
                return i;

        return EngineSetting.INDEX_NOT_FOUND;
    }

    public boolean contains(RecordingCurveTrackStruct track, int slot, int key) {
        return indexOf(track, slot, key) != EngineSetting.INDEX_NOT_FOUND;
    }

    // The first of the curve's selected keys, -1 when none of its keys is; the rest of them follow it in order
    public int findFirstOn(RecordingCurveTrackStruct track, int slot) {

        for (int i = 0; i < tracks.size(); i++)
            if (tracks.get(i) == track && slots.getInt(i) == slot)
                return i;

        return EngineSetting.INDEX_NOT_FOUND;
    }

    // True when the entry at the place is the key of the curve given
    public boolean isAt(int index, RecordingCurveTrackStruct track, int slot, int key) {
        return index >= 0 && index < keys.size() && tracks.get(index) == track && slots.getInt(index) == slot
                && keys.getInt(index) == key;
    }

    // True when every key of the other selection is among these
    public boolean containsAll(RecordingKeySelectionStruct other) {

        for (int i = 0; i < other.size(); i++)
            if (!contains(other.getTrack(i), other.getSlot(i), other.getKey(i)))
                return false;

        return true;
    }

    // How many keys of the curve are selected
    public int countOn(RecordingCurveTrackStruct track, int slot) {

        int count = 0;

        for (int i = 0; i < tracks.size(); i++)
            if (tracks.get(i) == track && slots.getInt(i) == slot)
                count++;

        return count;
    }

    // The last selected key of the curve, -1 when none of its keys is
    public int findLastOn(RecordingCurveTrackStruct track, int slot) {

        int found = EngineSetting.INDEX_NOT_FOUND;

        for (int i = 0; i < tracks.size(); i++)
            if (tracks.get(i) == track && slots.getInt(i) == slot)
                found = keys.getInt(i);

        return found;
    }

    public int size() {
        return keys.size();
    }

    public boolean isEmpty() {
        return keys.isEmpty();
    }

    public RecordingCurveTrackStruct getTrack(int index) {
        return tracks.get(index);
    }

    public int getSlot(int index) {
        return slots.getInt(index);
    }

    public int getKey(int index) {
        return keys.getInt(index);
    }

    public RecordingCurveStruct getCurve(int index) {
        return tracks.get(index).getCurve(slots.getInt(index));
    }
}
