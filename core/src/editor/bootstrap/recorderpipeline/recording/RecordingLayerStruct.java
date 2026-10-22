package editor.bootstrap.recorderpipeline.recording;

import engine.root.StructPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class RecordingLayerStruct extends StructPackage {

    /*
     * One take of a recording: the channels it recorded, the span of the
     * timeline it covers, and a track for each thing it saw — the camera, the
     * player, every resident and thrown item about them, and the clock.
     * Within its span a layer stands over every layer beneath it for each
     * channel it holds, unless it is muted. Its time ramp is how long each
     * jump of its clock is swept through on playback, a cut at zero.
     */

    // Layer
    private String name;
    private final int channelMask;
    private float startTime;
    private float endTime;
    private boolean muted;
    private float timeRampSeconds;

    // Tracks
    private final RecordingCameraTrackStruct cameraTrack;
    private RecordingBodyTrackStruct playerTrack;
    private final ObjectArrayList<RecordingBodyTrackStruct> residentTracks;
    private final ObjectArrayList<RecordingProjectileTrackStruct> projectileTracks;
    private final RecordingClockTrackStruct clockTrack;

    // Constructor \\

    public RecordingLayerStruct(String name, int channelMask, float startTime) {

        // Layer
        this.name = name;
        this.channelMask = channelMask;
        this.startTime = startTime;
        this.endTime = startTime;

        // Tracks
        this.cameraTrack = RecorderChannel.CAMERA.isIn(channelMask) ? new RecordingCameraTrackStruct() : null;
        this.residentTracks = new ObjectArrayList<>();
        this.projectileTracks = new ObjectArrayList<>();
        this.clockTrack = RecorderChannel.TIME.isIn(channelMask) ? new RecordingClockTrackStruct() : null;
    }

    // Management \\

    // True when the layer speaks for the channel at the time
    public boolean covers(RecorderChannel channel, float time) {
        return !muted && channel.isIn(channelMask) && time >= startTime && time <= endTime;
    }

    public boolean holds(RecorderChannel channel) {
        return channel.isIn(channelMask);
    }

    // Accessible \\

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getChannelMask() {
        return channelMask;
    }

    public float getStartTime() {
        return startTime;
    }

    public float getEndTime() {
        return endTime;
    }

    public void setEndTime(float endTime) {
        this.endTime = endTime;
    }

    public float getDuration() {
        return endTime - startTime;
    }

    public boolean isMuted() {
        return muted;
    }

    public void setMuted(boolean muted) {
        this.muted = muted;
    }

    public float getTimeRampSeconds() {
        return timeRampSeconds;
    }

    public void setTimeRampSeconds(float timeRampSeconds) {
        this.timeRampSeconds = timeRampSeconds;
    }

    public RecordingCameraTrackStruct getCameraTrack() {
        return cameraTrack;
    }

    public boolean hasPlayerTrack() {
        return playerTrack != null;
    }

    public RecordingBodyTrackStruct getPlayerTrack() {
        return playerTrack;
    }

    public void setPlayerTrack(RecordingBodyTrackStruct playerTrack) {
        this.playerTrack = playerTrack;
    }

    public ObjectArrayList<RecordingBodyTrackStruct> getResidentTracks() {
        return residentTracks;
    }

    public ObjectArrayList<RecordingProjectileTrackStruct> getProjectileTracks() {
        return projectileTracks;
    }

    public RecordingClockTrackStruct getClockTrack() {
        return clockTrack;
    }
}
