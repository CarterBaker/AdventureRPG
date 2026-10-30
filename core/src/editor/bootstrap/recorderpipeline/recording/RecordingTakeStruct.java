package editor.bootstrap.recorderpipeline.recording;

import engine.root.StructPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class RecordingTakeStruct extends StructPackage {

    /*
     * One take in a recording's bin: what was played live from Record to
     * Stop, its own clock running from zero. It knows the channels it
     * recorded, how long it ran, and holds a track for each thing it saw —
     * the camera, the player, every resident, thrown item and vehicle about
     * them, the weather, the clock, the interface and the sounds. A take
     * dropped from the sound library holds that one sound alone, from its
     * start, and runs as long as it plays. A take is never played
     * by itself: clips placed on the timeline play spans of it, each with
     * whichever of its channels it is given.
     */

    // Take
    private String name;
    private final int channelMask;
    private float duration;

    // Tracks
    private final RecordingCameraTrackStruct cameraTrack;
    private RecordingBodyTrackStruct playerTrack;
    private final ObjectArrayList<RecordingBodyTrackStruct> residentTracks;
    private final ObjectArrayList<RecordingProjectileTrackStruct> projectileTracks;
    private final ObjectArrayList<RecordingVehicleTrackStruct> vehicleTracks;
    private final RecordingWeatherTrackStruct weatherTrack;
    private final RecordingClockTrackStruct clockTrack;
    private final RecordingOverlayTrackStruct overlayTrack;
    private final RecordingSoundTrackStruct soundTrack;

    // Constructor \\

    public RecordingTakeStruct(String name, int channelMask) {

        // Take
        this.name = name;
        this.channelMask = channelMask;

        // Tracks
        this.cameraTrack = RecorderChannel.CAMERA.isIn(channelMask) ? new RecordingCameraTrackStruct() : null;
        this.residentTracks = new ObjectArrayList<>();
        this.projectileTracks = new ObjectArrayList<>();
        this.vehicleTracks = new ObjectArrayList<>();
        this.weatherTrack = RecorderChannel.WEATHER.isIn(channelMask) ? new RecordingWeatherTrackStruct() : null;
        this.clockTrack = RecorderChannel.TIME.isIn(channelMask) ? new RecordingClockTrackStruct() : null;
        this.overlayTrack = RecorderChannel.INTERFACE.isIn(channelMask) ? new RecordingOverlayTrackStruct() : null;
        this.soundTrack = RecorderChannel.SOUND.isIn(channelMask) ? new RecordingSoundTrackStruct() : null;
    }

    // Management \\

    public boolean holds(RecorderChannel channel) {
        return channel.isIn(channelMask);
    }

    // Moves every track along by the same seconds, as reading an older recording needs
    public void shiftTimes(float seconds) {

        if (cameraTrack != null)
            cameraTrack.shiftTimes(seconds);

        if (playerTrack != null)
            playerTrack.shiftTimes(seconds);

        for (int i = 0; i < residentTracks.size(); i++)
            residentTracks.get(i).shiftTimes(seconds);

        for (int i = 0; i < projectileTracks.size(); i++)
            projectileTracks.get(i).shiftTimes(seconds);

        for (int i = 0; i < vehicleTracks.size(); i++)
            vehicleTracks.get(i).shiftTimes(seconds);

        if (weatherTrack != null)
            weatherTrack.shiftTimes(seconds);

        if (clockTrack != null)
            clockTrack.shiftTimes(seconds);

        if (overlayTrack != null)
            overlayTrack.shiftTimes(seconds);

        if (soundTrack != null)
            soundTrack.shiftTimes(seconds);
    }

    // True for a take dropped from the sound library for the sound: that sound alone, from its start
    public boolean isSoundOf(String soundName) {
        return channelMask == RecorderChannel.SOUND.getBit() && soundTrack.isSingle(soundName);
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

    public float getDuration() {
        return duration;
    }

    public void setDuration(float duration) {
        this.duration = duration;
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

    public ObjectArrayList<RecordingVehicleTrackStruct> getVehicleTracks() {
        return vehicleTracks;
    }

    public RecordingWeatherTrackStruct getWeatherTrack() {
        return weatherTrack;
    }

    public RecordingClockTrackStruct getClockTrack() {
        return clockTrack;
    }

    public RecordingOverlayTrackStruct getOverlayTrack() {
        return overlayTrack;
    }

    public RecordingSoundTrackStruct getSoundTrack() {
        return soundTrack;
    }
}
