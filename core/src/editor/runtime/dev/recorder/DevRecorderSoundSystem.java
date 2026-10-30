package editor.runtime.dev.recorder;

import application.bootstrap.entitypipeline.playermanager.PlayerManager;
import application.bootstrap.soundpipeline.sound.SoundHandle;
import application.bootstrap.soundpipeline.sound.SoundVoiceStruct;
import application.bootstrap.soundpipeline.soundmanager.SoundManager;
import application.bootstrap.worldpipeline.world.WorldHandle;
import editor.bootstrap.recorderpipeline.recordermanager.RecorderManager;
import editor.bootstrap.recorderpipeline.recording.RecorderChannel;
import editor.bootstrap.recorderpipeline.recording.RecorderState;
import editor.bootstrap.recorderpipeline.recording.RecordingClipStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingInstance;
import editor.bootstrap.recorderpipeline.recording.RecordingSoundTrackStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingTakeStruct;
import editor.runtime.EditorSetting;
import editor.runtime.dev.DevContext;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class DevRecorderSoundSystem extends SystemPackage {

    /*
     * Carries the recorder's sound in this Dev window. While the timeline's
     * sounds are heard, every unmuted clip holding the sound channel sounds
     * every sound of its take that spans the playhead, all together whatever
     * their lanes: each is started where the playhead stands within it, at
     * its volume and pitch and, when it was placed, where it was placed, and
     * stopped once the playhead leaves it or its clip. A jump of the playhead
     * — a seek, a pause, the playhead running back — stops every sound so
     * each starts again from where the playhead now stands. While a take
     * records sound live, every sound this window's own systems start, and
     * every sound the shared world starts within this window's world, is
     * captured onto the take from the moment it began — never one this
     * system started, nor one begun before the take or while it was
     * suspended — its length the sound's own unless it is cut short or loops,
     * when it runs until it stops; finishing the take ends every sound still
     * running at the take's end. Nothing here allocates once its lists
     * have grown to the most sounds heard at once.
     */

    // Internal
    private RecorderManager recorderManager;
    private SoundManager soundManager;
    private PlayerManager playerManager;

    // Playback — one entry per sound playing from the timeline, reused as sounds come and go
    private ObjectArrayList<RecordingClipStruct> playingClips;
    private int[] playingEvents;
    private int[] playingVoices;
    private boolean[] playingSeen;
    private int playingCount;
    private float lastPlayhead;
    private boolean sounding;

    // Capture — for each voice slot, the voice captured from it and the sound it became on the take
    private int[] capturedVoices;
    private int[] capturedEvents;
    private boolean[] capturedLoops;
    private long[] capturedStartMillis;
    private boolean capturing;
    private long capturingSinceMillis;

    // Base \\

    @Override
    protected void create() {

        // Playback
        this.playingClips = new ObjectArrayList<>();
        this.playingEvents = new int[EngineSetting.SOUND_VOICE_COUNT];
        this.playingVoices = new int[EngineSetting.SOUND_VOICE_COUNT];
        this.playingSeen = new boolean[EngineSetting.SOUND_VOICE_COUNT];

        // Capture
        this.capturedVoices = new int[EngineSetting.SOUND_VOICE_COUNT];
        this.capturedEvents = new int[EngineSetting.SOUND_VOICE_COUNT];
        this.capturedLoops = new boolean[EngineSetting.SOUND_VOICE_COUNT];
        this.capturedStartMillis = new long[EngineSetting.SOUND_VOICE_COUNT];
        clearCapture();
    }

    @Override
    protected void get() {
        this.recorderManager = get(RecorderManager.class);
        this.soundManager = get(SoundManager.class);
        this.playerManager = get(PlayerManager.class);
    }

    @Override
    protected void dispose() {
        stopAll();
    }

    // Update \\

    @Override
    protected void update() {

        boolean wanted = recorderManager.isSessionFor((DevContext) context) && recorderManager.isSoundPlayed();
        float time = recorderManager.getPlayhead();

        if (!wanted || !sounding || isJump(time))
            stopAll();

        this.sounding = wanted;
        this.lastPlayhead = time;

        if (wanted)
            play(recorderManager.getRecording(), time);
    }

    // True when the playhead did not run on from where it stood a frame ago
    private boolean isJump(float time) {
        return time < lastPlayhead || time - lastPlayhead > EditorSetting.RECORDER_SOUND_JUMP_SECONDS;
    }

    // While the session is suspended the sounds already followed are kept, and none begun meanwhile is taken
    @Override
    protected void lateUpdate() {

        boolean recording = recorderManager.isSessionFor((DevContext) context)
                && recorderManager.getState() == RecorderState.RECORDING
                && RecorderChannel.SOUND.isIn(recorderManager.getLiveChannels());

        if (!recording) {
            clearCapture();
            this.capturing = false;
            return;
        }

        if (recorderManager.isSuspended()) {
            this.capturing = false;
            return;
        }

        if (!capturing) {
            this.capturing = true;
            this.capturingSinceMillis = internal.getTime();
        }

        capture(recorderManager.getTake(), recorderManager.getTakeTime());
    }

    // Playback \\

    // Every sound spanning the playhead kept sounding or started, and every other one stopped
    private void play(RecordingInstance recording, float time) {

        for (int i = 0; i < playingCount; i++)
            playingSeen[i] = false;

        ObjectArrayList<RecordingClipStruct> clips = recording.getClips();

        for (int i = 0; i < clips.size(); i++) {

            RecordingClipStruct clip = clips.get(i);

            if (!clip.covers(RecorderChannel.SOUND, time))
                continue;

            RecordingSoundTrackStruct track = clip.getTake().getSoundTrack();
            float takeTime = clip.toTakeTime(time);

            for (int event = track.findFirstSounding(takeTime);
                    event < track.size() && track.getTime(event) <= takeTime; event++)
                if (takeTime < track.getTime(event) + track.getDuration(event))
                    sound(clip, track, event, takeTime);
        }

        for (int i = playingCount - 1; i >= 0; i--)
            if (!playingSeen[i])
                release(i);
    }

    // The sound kept sounding when it already is, else started where the playhead stands within it
    private void sound(RecordingClipStruct clip, RecordingSoundTrackStruct track, int event, float takeTime) {

        int playing = findPlaying(clip, event);

        if (playing != EngineSetting.INDEX_NOT_FOUND) {
            playingSeen[playing] = true;
            return;
        }

        String soundName = track.getSoundName(event);

        if (!soundManager.hasSound(soundName) || playingCount == playingVoices.length)
            return;

        int soundID = soundManager.getSoundIDFromSoundName(soundName);
        SoundHandle soundHandle = soundManager.getSoundHandleFromSoundID(soundID);
        float pitch = track.getPitch(event);
        float offset = resolveOffset(soundHandle, (takeTime - track.getTime(event)) * pitch * soundHandle.getPitch());
        int voice = track.isSpatial(event)
                ? soundManager.playSoundAt(soundID, track.getVolume(event), pitch, offset, resolveWorld(),
                        track.getChunkCoordinate(event), track.getPositionX(event), track.getPositionY(event),
                        track.getPositionZ(event))
                : soundManager.playSound(soundID, track.getVolume(event), pitch, offset);

        if (voice == EngineSetting.INDEX_NOT_FOUND)
            return;

        if (playingClips.size() == playingCount)
            playingClips.add(clip);
        else
            playingClips.set(playingCount, clip);

        playingEvents[playingCount] = event;
        playingVoices[playingCount] = voice;
        playingSeen[playingCount] = true;
        this.playingCount++;
    }

    // Seconds into the sound's samples, wrapped round when it loops
    private float resolveOffset(SoundHandle soundHandle, float offset) {
        return soundHandle.isLoop() ? offset % soundHandle.getLength() : offset;
    }

    private int findPlaying(RecordingClipStruct clip, int event) {

        for (int i = 0; i < playingCount; i++)
            if (playingClips.get(i) == clip && playingEvents[i] == event)
                return i;

        return EngineSetting.INDEX_NOT_FOUND;
    }

    // The entry's sound stopped, the last entry moved into its place
    private void release(int playing) {

        soundManager.stopVoice(playingVoices[playing]);

        int last = playingCount - 1;

        playingClips.set(playing, playingClips.get(last));
        playingEvents[playing] = playingEvents[last];
        playingVoices[playing] = playingVoices[last];
        playingSeen[playing] = playingSeen[last];
        playingClips.set(last, null);
        this.playingCount--;
    }

    private void stopAll() {
        while (playingCount > 0)
            release(playingCount - 1);
    }

    private boolean isPlayedHere(int voice) {

        for (int i = 0; i < playingCount; i++)
            if (playingVoices[i] == voice)
                return true;

        return false;
    }

    // Take \\

    // Every sound still running ends at the take's end, so it is saved as long as it was heard
    public void finishTake() {

        RecordingTakeStruct take = recorderManager.getTake();

        if (take.holds(RecorderChannel.SOUND))
            for (int slot = 0; slot < capturedVoices.length; slot++)
                if (capturedVoices[slot] != EngineSetting.INDEX_NOT_FOUND)
                    endCapture(take.getSoundTrack(), slot, recorderManager.getTakeTime());

        clearCapture();
    }

    // Capture \\

    // Each voice this window heard begin is laid on the take once, and followed until it stops
    private void capture(RecordingTakeStruct take, float takeTime) {

        RecordingSoundTrackStruct track = take.getSoundTrack();
        int windowID = context.getWindow().getWindowID();
        WorldHandle worldHandle = resolveWorld();

        for (int slot = 0; slot < soundManager.getVoiceCount(); slot++) {

            SoundVoiceStruct voice = soundManager.getVoice(slot);
            boolean live = voice.isActive() && isHeardHere(voice, windowID, worldHandle)
                    && !isPlayedHere(voice.getHandle());

            if (capturedVoices[slot] != EngineSetting.INDEX_NOT_FOUND
                    && (!live || voice.getHandle() != capturedVoices[slot]))
                endCapture(track, slot, takeTime);

            if (!live)
                continue;

            boolean followed = capturedVoices[slot] != EngineSetting.INDEX_NOT_FOUND;

            if (!followed && voice.getStartMillis() >= capturingSinceMillis)
                beginCapture(track, slot, voice, takeTime);
            else if (followed && voice.isLooping())
                track.setDuration(capturedEvents[slot], takeTime - track.getTime(capturedEvents[slot]));
        }
    }

    // The sound laid on the take from the moment it began, as long as it plays unless it loops
    private void beginCapture(RecordingSoundTrackStruct track, int slot, SoundVoiceStruct voice, float takeTime) {

        SoundHandle soundHandle = voice.getSoundHandle();
        float began = Math.max(0f, takeTime - (internal.getTime() - voice.getStartMillis())
                * EngineSetting.SOUND_SECONDS_PER_MILLI);
        float length = voice.isLooping() ? 0f : soundHandle.getPlayedLength() / voice.getPitch();

        capturedVoices[slot] = voice.getHandle();
        capturedLoops[slot] = voice.isLooping();
        capturedStartMillis[slot] = voice.getStartMillis();
        capturedEvents[slot] = track.addEvent(
                began,
                length,
                soundHandle.getSoundName(),
                voice.getVolume(),
                voice.getPitch(),
                voice.isSpatial(),
                voice.getChunkCoordinate(),
                voice.getPosition().x,
                voice.getPosition().y,
                voice.getPosition().z,
                soundManager.resolveHeardGain(voice),
                soundManager.resolveHeardPan(voice));

        for (int other = 0; other < capturedEvents.length; other++)
            if (other != slot && capturedVoices[other] != EngineSetting.INDEX_NOT_FOUND
                    && capturedEvents[other] >= capturedEvents[slot])
                capturedEvents[other]++;
    }

    // A looping sound runs for as long as the take heard it; any other keeps its own length unless it was stopped
    // before it had played through
    private void endCapture(RecordingSoundTrackStruct track, int slot, float takeTime) {

        int event = capturedEvents[slot];
        float ran = Math.max(0f, takeTime - track.getTime(event));
        float played = (internal.getTime() - capturedStartMillis[slot]) * EngineSetting.SOUND_SECONDS_PER_MILLI;

        if (capturedLoops[slot] || played < track.getDuration(event))
            track.setDuration(event, capturedLoops[slot] ? ran : Math.min(track.getDuration(event), ran));

        capturedVoices[slot] = EngineSetting.INDEX_NOT_FOUND;
    }

    private void clearCapture() {
        for (int slot = 0; slot < capturedVoices.length; slot++)
            capturedVoices[slot] = EngineSetting.INDEX_NOT_FOUND;
    }

    // Utility \\

    // Begun by this window's own systems, or by the shared world within this window's world
    private boolean isHeardHere(SoundVoiceStruct voice, int windowID, WorldHandle worldHandle) {
        return voice.getOwnerWindowID() == windowID
                || voice.getOwnerWindowID() == EngineSetting.INDEX_NOT_FOUND
                        && voice.isSpatial() && voice.getWorldHandle() == worldHandle;
    }

    private WorldHandle resolveWorld() {
        return playerManager.getPlayerForWindow(context.getWindow().getWindowID()).getWorldHandle();
    }
}
