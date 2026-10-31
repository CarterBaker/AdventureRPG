package editor.bootstrap.recorderpipeline.recording;

import editor.bootstrap.recorderpipeline.util.RecordingSampleUtility;
import engine.root.EngineSetting;
import engine.root.StructPackage;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class RecordingSoundTrackStruct extends StructPackage {

    /*
     * The sounds through a take, in the order they began: when each began, in
     * seconds from the start of the take, how long it was heard, which sound
     * it was — named once in the track's own list of names — how loud and at
     * what pitch it was asked for, whether it was placed in the world and
     * where, and how loud and from which side it reached the camera, so an
     * export can mix it as it was heard. A sound already playing when the take
     * began starts before it, so it is heard from as far into it as it had
     * reached. Sounds are moved, copied, changed and removed in place, and a
     * name no sound uses any longer leaves the list. One list per field, so a
     * track of any length stores no object per sound. The longest any sound
     * runs is kept, so the sounds sounding at a moment are found from a binary
     * search.
     */

    // Names
    private final ObjectArrayList<String> soundNames;
    private final Object2IntOpenHashMap<String> soundName2Index;

    // Events
    private final FloatArrayList times;
    private final FloatArrayList durations;
    private final IntArrayList nameIndices;
    private final FloatArrayList volumes;
    private final FloatArrayList pitches;
    private final IntArrayList spatials;
    private final LongArrayList chunkCoordinates;
    private final FloatArrayList positionsX;
    private final FloatArrayList positionsY;
    private final FloatArrayList positionsZ;
    private final FloatArrayList heardGains;
    private final FloatArrayList heardPans;

    // Reach
    private float longestDuration;

    // Constructor \\

    public RecordingSoundTrackStruct() {

        // Names
        this.soundNames = new ObjectArrayList<>();
        this.soundName2Index = new Object2IntOpenHashMap<>();
        this.soundName2Index.defaultReturnValue(EngineSetting.INDEX_NOT_FOUND);

        // Events
        this.times = new FloatArrayList();
        this.durations = new FloatArrayList();
        this.nameIndices = new IntArrayList();
        this.volumes = new FloatArrayList();
        this.pitches = new FloatArrayList();
        this.spatials = new IntArrayList();
        this.chunkCoordinates = new LongArrayList();
        this.positionsX = new FloatArrayList();
        this.positionsY = new FloatArrayList();
        this.positionsZ = new FloatArrayList();
        this.heardGains = new FloatArrayList();
        this.heardPans = new FloatArrayList();
    }

    // Management \\

    // A sound heard from the moment, kept in the order sounds began; its place comes back
    public int addEvent(
            float time,
            float duration,
            String soundName,
            float volume,
            float pitch,
            boolean spatial,
            long chunkCoordinate,
            float x,
            float y,
            float z,
            float heardGain,
            float heardPan) {

        int index = times.size();

        while (index > 0 && times.getFloat(index - 1) > time)
            index--;

        times.add(index, time);
        durations.add(index, duration);
        nameIndices.add(index, resolveNameIndex(soundName));
        volumes.add(index, volume);
        pitches.add(index, pitch);
        spatials.add(index, spatial ? 1 : 0);
        chunkCoordinates.add(index, chunkCoordinate);
        positionsX.add(index, x);
        positionsY.add(index, y);
        positionsZ.add(index, z);
        heardGains.add(index, heardGain);
        heardPans.add(index, heardPan);
        this.longestDuration = Math.max(longestDuration, duration);

        return index;
    }

    // The sound heard again from another moment, every other field kept; its new place comes back
    public int moveEvent(int index, float time) {

        int moved = copyEvent(index, time);

        removeEvent(moved > index ? index : index + 1);
        return moved > index ? moved - 1 : moved;
    }

    // A second hearing of the sound from another moment, every other field kept; its place comes back
    public int copyEvent(int index, float time) {
        return addEvent(
                time,
                getDuration(index),
                getSoundName(index),
                getVolume(index),
                getPitch(index),
                isSpatial(index),
                getChunkCoordinate(index),
                getPositionX(index),
                getPositionY(index),
                getPositionZ(index),
                getHeardGain(index),
                getHeardPan(index));
    }

    // The sound gone from the track, and its name with it once no other sound uses it
    public void removeEvent(int index) {

        times.removeFloat(index);
        durations.removeFloat(index);
        nameIndices.removeInt(index);
        volumes.removeFloat(index);
        pitches.removeFloat(index);
        spatials.removeInt(index);
        chunkCoordinates.removeLong(index);
        positionsX.removeFloat(index);
        positionsY.removeFloat(index);
        positionsZ.removeFloat(index);
        heardGains.removeFloat(index);
        heardPans.removeFloat(index);
        pruneNames();
        resolveLongestDuration();
    }

    public void setDuration(int index, float duration) {
        durations.set(index, duration);
        this.longestDuration = Math.max(longestDuration, duration);
    }

    // As loud as asked, and reaching the camera louder or quieter by just as much
    public void setVolume(int index, float volume) {

        float previous = getVolume(index);

        heardGains.set(index, previous > 0f ? getHeardGain(index) * volume / previous : getHeardGain(index));
        volumes.set(index, volume);
    }

    public void setPitch(int index, float pitch) {
        pitches.set(index, pitch);
    }

    // Another sound heard in its place, reaching the camera as loud as the gain given
    public void setSound(int index, String soundName, float heardGain) {
        nameIndices.set(index, resolveNameIndex(soundName));
        heardGains.set(index, heardGain);
        pruneNames();
    }

    // The name's place in the track's list, added the first time it is named
    public int resolveNameIndex(String soundName) {

        int index = soundName2Index.getInt(soundName);

        if (index != EngineSetting.INDEX_NOT_FOUND)
            return index;

        soundNames.add(soundName);
        soundName2Index.put(soundName, soundNames.size() - 1);
        return soundNames.size() - 1;
    }

    // Every name no sound uses dropped from the list, the rest keeping their order
    private void pruneNames() {

        int kept = 0;

        for (int name = 0; name < soundNames.size(); name++) {

            if (!nameIndices.contains(name))
                continue;

            for (int i = 0; i < nameIndices.size(); i++)
                if (nameIndices.getInt(i) == name)
                    nameIndices.set(i, kept);

            soundNames.set(kept++, soundNames.get(name));
        }

        soundNames.size(kept);
        soundName2Index.clear();

        for (int name = 0; name < kept; name++)
            soundName2Index.put(soundNames.get(name), name);
    }

    // The longest duration measured again, as reading a track back needs
    public void resolveLongestDuration() {

        this.longestDuration = 0f;

        for (int i = 0; i < durations.size(); i++)
            this.longestDuration = Math.max(longestDuration, durations.getFloat(i));
    }

    public void shiftTimes(float seconds) {
        RecordingSampleUtility.shiftTimes(times, seconds);
    }

    // The first sound that may still be heard at the moment — every one before it ended earlier
    public int findFirstSounding(float time) {
        return RecordingSampleUtility.findSample(times, time - longestDuration) + 1;
    }

    // True when every sound the track holds is a single one of the sound, begun at the take's start
    public boolean isSingle(String soundName) {
        return times.size() == 1 && times.getFloat(0) == 0f && getSoundName(0).equals(soundName);
    }

    // Accessible \\

    public int size() {
        return times.size();
    }

    public boolean isEmpty() {
        return times.isEmpty();
    }

    public float getTime(int index) {
        return times.getFloat(index);
    }

    public float getDuration(int index) {
        return durations.getFloat(index);
    }

    public String getSoundName(int index) {
        return soundNames.get(nameIndices.getInt(index));
    }

    public int getNameIndex(int index) {
        return nameIndices.getInt(index);
    }

    public float getVolume(int index) {
        return volumes.getFloat(index);
    }

    public float getPitch(int index) {
        return pitches.getFloat(index);
    }

    public boolean isSpatial(int index) {
        return spatials.getInt(index) != 0;
    }

    public long getChunkCoordinate(int index) {
        return chunkCoordinates.getLong(index);
    }

    public float getPositionX(int index) {
        return positionsX.getFloat(index);
    }

    public float getPositionY(int index) {
        return positionsY.getFloat(index);
    }

    public float getPositionZ(int index) {
        return positionsZ.getFloat(index);
    }

    public float getHeardGain(int index) {
        return heardGains.getFloat(index);
    }

    public float getHeardPan(int index) {
        return heardPans.getFloat(index);
    }

    public ObjectArrayList<String> getSoundNames() {
        return soundNames;
    }

    public FloatArrayList getTimes() {
        return times;
    }

    public FloatArrayList getDurations() {
        return durations;
    }

    public IntArrayList getNameIndices() {
        return nameIndices;
    }

    public FloatArrayList getVolumes() {
        return volumes;
    }

    public FloatArrayList getPitches() {
        return pitches;
    }

    public IntArrayList getSpatials() {
        return spatials;
    }

    public LongArrayList getChunkCoordinates() {
        return chunkCoordinates;
    }

    public FloatArrayList getPositionsX() {
        return positionsX;
    }

    public FloatArrayList getPositionsY() {
        return positionsY;
    }

    public FloatArrayList getPositionsZ() {
        return positionsZ;
    }

    public FloatArrayList getHeardGains() {
        return heardGains;
    }

    public FloatArrayList getHeardPans() {
        return heardPans;
    }
}
