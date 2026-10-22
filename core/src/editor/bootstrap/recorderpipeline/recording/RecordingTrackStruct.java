package editor.bootstrap.recorderpipeline.recording;

import editor.bootstrap.recorderpipeline.util.RecordingSampleUtility;
import engine.root.StructPackage;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.longs.LongArrayList;

public abstract class RecordingTrackStruct extends StructPackage {

    /*
     * The samples every moving thing in a take shares: when each was taken,
     * in seconds from the start of the take, and where — a chunk
     * coordinate and a position local to that chunk. Each kind of track adds
     * its own columns beside these, one list per field, so a track of any
     * length stores no object per sample. shiftTimes() moves every sample
     * along by the same seconds, as reading an older recording needs.
     */

    // Samples
    protected final FloatArrayList times;
    protected final LongArrayList chunkCoordinates;
    protected final FloatArrayList positionsX;
    protected final FloatArrayList positionsY;
    protected final FloatArrayList positionsZ;

    // Constructor \\

    protected RecordingTrackStruct() {

        // Samples
        this.times = new FloatArrayList();
        this.chunkCoordinates = new LongArrayList();
        this.positionsX = new FloatArrayList();
        this.positionsY = new FloatArrayList();
        this.positionsZ = new FloatArrayList();
    }

    // Management \\

    protected void addPlace(float time, long chunkCoordinate, Vector3 position) {
        addPlace(time, chunkCoordinate, position.x, position.y, position.z);
    }

    protected void addPlace(float time, long chunkCoordinate, float x, float y, float z) {
        times.add(time);
        chunkCoordinates.add(chunkCoordinate);
        positionsX.add(x);
        positionsY.add(y);
        positionsZ.add(z);
    }

    public void shiftTimes(float seconds) {
        RecordingSampleUtility.shiftTimes(times, seconds);
    }

    // The sample at or before the time, -1 before the first
    public int findSample(float time) {
        return RecordingSampleUtility.findSample(times, time);
    }

    // Accessible \\

    public int size() {
        return times.size();
    }

    public boolean isEmpty() {
        return times.isEmpty();
    }

    public float getStartTime() {
        return times.getFloat(0);
    }

    public float getEndTime() {
        return times.getFloat(times.size() - 1);
    }

    public float getTime(int index) {
        return times.getFloat(index);
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

    public FloatArrayList getTimes() {
        return times;
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
}
