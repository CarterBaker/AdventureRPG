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
     * export can mix it as it was heard. One list per field, so a track of any
     * length stores no object per sound. The longest any sound runs is kept,
     * so the sounds sounding at a moment are found from a binary search.
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

    public void setDuration(int index, float duration) {
        durations.set(index, duration);
        this.longestDuration = Math.max(longestDuration, duration);
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
