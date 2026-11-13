package editor.runtime.dev.recorder;

import application.bootstrap.entitypipeline.playermanager.PlayerManager;
import application.bootstrap.musicpipeline.samplermanager.SamplerManager;
import application.bootstrap.musicpipeline.song.SongPlaybackInstance;
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
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class DevRecorderSoundSystem extends SystemPackage {

    /*
     * Carries the recorder's sound in this Dev window. While the timeline's
     * sounds are heard, every unmuted clip holding the sound channel sounds
     * every sound of its take that spans the playhead, all together whatever
     * their lanes, walking down into every nested recording at the moment
     * mapped into it, so a sound laid twice through two nested clips plays
     * twice, each followed along the path of clips it was reached by; a song
     * or a notes clip follows the playhead through the sampler, from wherever
     * the playhead first finds it, each from a fixed pool of song playbacks,
     * and its notes are never captured back into a take. Each sound is started where the playhead stands within it, at
     * its volume and pitch and, when it was placed, where it was placed, and
     * stopped once the playhead leaves it or its clip; across its clip's
     * transitions its volume fades in and out, set again whenever the fade
     * moves it. A jump of the playhead
     * — a seek, a pause, the playhead running back — stops every sound so
     * each starts again from where the playhead now stands, and so does any
     * change made to a sound on the timeline. While a take records sound live,
     * every sound this window's own systems play, every sound the shared world
     * plays within this window's world and every one it plays for no world at
     * all is captured onto the take from the moment it began — never one this
     * system started, nor one begun while the take was suspended. A sound
     * already playing when the take begins is captured too: a loop from the
     * take's start, any other from before it, as far in as it had played. Its
     * length is the sound's own unless it is cut short or loops, when it runs
     * until it stops; finishing the take ends every sound still running at the
     * take's end. Nothing here allocates once its lists have grown to the most
     * sounds heard at once.
     */

    // Internal
    private RecorderManager recorderManager;
    private SoundManager soundManager;
    private SamplerManager samplerManager;
    private PlayerManager playerManager;

    // Playback — one entry per sound playing from the timeline, each the clip, the path of clips it was reached by
    // and its sound on the take, reused as sounds come and go
    private ObjectArrayList<RecordingClipStruct> playingClips;
    private long[] playingPaths;
    private int[] playingEvents;
    private int[] playingVoices;
    private float[] playingFades;
    private boolean[] playingSeen;
    private int playingCount;
    private float lastPlayhead;
    private boolean sounding;
    private int shownSoundRevision;

    // Music — one song playback per song or notes clip following the playhead, by its clip and path, and every voice
    // their notes sound through
    private SongPlaybackInstance[] musicPlaybacks;
    private ObjectArrayList<RecordingClipStruct> musicClips;
    private long[] musicPaths;
    private boolean[] musicSeen;
    private int musicCount;
    private IntArrayList musicVoices;

    // Capture — for each voice slot, the voice captured from it and the sound it became on the take
    private int[] capturedVoices;
    private int[] capturedEvents;
    private boolean[] capturedLoops;
    private long[] capturedStartMillis;
    private boolean capturing;
    private boolean takeBegun;
    private long capturingSinceMillis;

    // Base \\

    @Override
    protected void create() {

        // Playback
        this.playingClips = new ObjectArrayList<>();
        this.playingPaths = new long[EngineSetting.SOUND_VOICE_COUNT];
        this.playingEvents = new int[EngineSetting.SOUND_VOICE_COUNT];
        this.playingVoices = new int[EngineSetting.SOUND_VOICE_COUNT];
        this.playingFades = new float[EngineSetting.SOUND_VOICE_COUNT];
        this.playingSeen = new boolean[EngineSetting.SOUND_VOICE_COUNT];

        // Music
        this.musicPlaybacks = new SongPlaybackInstance[EditorSetting.RECORDER_MUSIC_PLAYBACK_COUNT];
        this.musicClips = new ObjectArrayList<>();
        this.musicPaths = new long[EditorSetting.RECORDER_MUSIC_PLAYBACK_COUNT];
        this.musicSeen = new boolean[EditorSetting.RECORDER_MUSIC_PLAYBACK_COUNT];
        this.musicVoices = new IntArrayList();

        for (int i = 0; i < musicPlaybacks.length; i++) {
            musicPlaybacks[i] = create(SongPlaybackInstance.class);
            musicClips.add(null);
        }

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
        this.samplerManager = get(SamplerManager.class);
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

        if (!wanted || !sounding || isJump(time) || recorderManager.getSoundRevision() != shownSoundRevision)
            stopAll();

        this.sounding = wanted;
        this.lastPlayhead = time;
        this.shownSoundRevision = recorderManager.getSoundRevision();

        if (wanted)
            play(recorderManager.getRecording(), time);
    }

    // True when the playhead did not run on from where it stood a frame ago
    private boolean isJump(float time) {
        return time < lastPlayhead || time - lastPlayhead > EditorSetting.RECORDER_SOUND_JUMP_SECONDS;
    }

    // While the session is suspended the sounds already followed are kept, and none begun meanwhile is taken; the
    // take's first frame takes every sound already playing, a frame after a suspension only those begun since
    @Override
    protected void lateUpdate() {

        boolean recording = recorderManager.isSessionFor((DevContext) context)
                && recorderManager.getState() == RecorderState.RECORDING
                && RecorderChannel.SOUND.isIn(recorderManager.getLiveChannels());

        if (!recording) {
            clearCapture();
            this.capturing = false;
            this.takeBegun = false;
            return;
        }

        if (recorderManager.isSuspended()) {
            this.capturing = false;
            return;
        }

        if (!capturing) {
            this.capturing = true;
            this.capturingSinceMillis = takeBegun ? internal.getTime() : Long.MIN_VALUE;
            this.takeBegun = true;
        }

        capture(recorderManager.getTake(), recorderManager.getTakeTime());
    }

    // Playback \\

    // Every sound spanning the playhead kept sounding or started, every song and notes clip followed, and every other
    // one stopped
    private void play(RecordingInstance recording, float time) {

        for (int i = 0; i < playingCount; i++)
            playingSeen[i] = false;

        for (int i = 0; i < musicCount; i++)
            musicSeen[i] = false;

        playRecording(recording, time, 1f, EditorSetting.RECORDER_PATH_ROOT);

        for (int i = playingCount - 1; i >= 0; i--)
            if (!playingSeen[i])
                release(i);

        for (int i = musicCount - 1; i >= 0; i--)
            if (!musicSeen[i])
                releaseMusic(i);

        collectMusicVoices();
    }

    // Every clip of the recording sounding at the time, each faded by its transitions on top of the fade it was
    // reached with: its take's sounds, the nested recording it lays at the moment mapped into it, or the song it
    // sounds
    private void playRecording(RecordingInstance recording, float time, float fade, long path) {

        ObjectArrayList<RecordingClipStruct> clips = recording.getClips();

        for (int i = 0; i < clips.size(); i++) {

            RecordingClipStruct clip = clips.get(i);

            if (!clip.covers(RecorderChannel.SOUND, time))
                continue;

            long clipPath = path * EditorSetting.RECORDER_PATH_RADIX + i + 1;
            float clipFade = fade * recorderManager.resolveClipGain(clip, time);
            float childTime = clip.toChildTime(time);

            if (clip.getTake() != null)
                soundTake(clip, clipPath, clip.getTake().getSoundTrack(), childTime, clipFade);

            if (clip.getChild() != null)
                playRecording(clip.getChild(), childTime, clipFade, clipPath);

            if (clip.getSong() != null)
                followSong(clip, clipPath, childTime, clipFade);
        }
    }

    private void soundTake(
            RecordingClipStruct clip,
            long path,
            RecordingSoundTrackStruct track,
            float takeTime,
            float fade) {
        for (int event = track.findFirstSounding(takeTime);
                event < track.size() && track.getTime(event) <= takeTime; event++)
            if (takeTime < track.getTime(event) + track.getDuration(event))
                sound(clip, path, track, event, takeTime, fade);
    }

    // The sound kept sounding when it already is, its volume set again when its clip's fade has moved, else started
    // where the playhead stands within it
    private void sound(
            RecordingClipStruct clip,
            long path,
            RecordingSoundTrackStruct track,
            int event,
            float takeTime,
            float fade) {

        int playing = findPlaying(clip, path, event);

        if (playing != EngineSetting.INDEX_NOT_FOUND) {

            playingSeen[playing] = true;

            if (playingFades[playing] != fade) {
                playingFades[playing] = fade;
                soundManager.setVoiceVolume(playingVoices[playing], track.getVolume(event) * fade);
            }

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
                ? soundManager.playSoundAt(soundID, track.getVolume(event) * fade, pitch, offset, resolveWorld(),
                        track.getChunkCoordinate(event), track.getPositionX(event), track.getPositionY(event),
                        track.getPositionZ(event))
                : soundManager.playSound(soundID, track.getVolume(event) * fade, pitch, offset);

        if (voice == EngineSetting.INDEX_NOT_FOUND)
            return;

        if (playingClips.size() == playingCount)
            playingClips.add(clip);
        else
            playingClips.set(playingCount, clip);

        playingPaths[playingCount] = path;
        playingEvents[playingCount] = event;
        playingVoices[playingCount] = voice;
        playingFades[playingCount] = fade;
        playingSeen[playingCount] = true;
        this.playingCount++;
    }

    // Seconds into the sound's samples, wrapped round when it loops
    private float resolveOffset(SoundHandle soundHandle, float offset) {
        return soundHandle.isLoop() ? offset % soundHandle.getLength() : offset;
    }

    private int findPlaying(RecordingClipStruct clip, long path, int event) {

        for (int i = 0; i < playingCount; i++)
            if (playingClips.get(i) == clip && playingPaths[i] == path && playingEvents[i] == event)
                return i;

        return EngineSetting.INDEX_NOT_FOUND;
    }

    // The entry's sound stopped, the last entry moved into its place
    private void release(int playing) {

        soundManager.stopVoice(playingVoices[playing]);

        int last = playingCount - 1;

        playingClips.set(playing, playingClips.get(last));
        playingPaths[playing] = playingPaths[last];
        playingEvents[playing] = playingEvents[last];
        playingVoices[playing] = playingVoices[last];
        playingFades[playing] = playingFades[last];
        playingSeen[playing] = playingSeen[last];
        playingClips.set(last, null);
        this.playingCount--;
    }

    private void stopAll() {

        while (playingCount > 0)
            release(playingCount - 1);

        while (musicCount > 0)
            releaseMusic(musicCount - 1);
    }

    // True for a voice this system sounds: a sound of the timeline's, or a note of a song it follows
    private boolean isPlayedHere(int voice) {

        for (int i = 0; i < playingCount; i++)
            if (playingVoices[i] == voice)
                return true;

        return musicVoices.contains(voice);
    }

    // Music \\

    // The song the clip sounds followed to the playhead from a playback of its own, begun from wherever the playhead
    // first finds it; nothing when every playback is taken
    private void followSong(RecordingClipStruct clip, long path, float childTime, float fade) {

        int slot = findMusic(clip, path);

        if (slot == EngineSetting.INDEX_NOT_FOUND) {

            if (musicCount == musicPlaybacks.length)
                return;

            slot = musicCount++;
            musicClips.set(slot, clip);
            musicPaths[slot] = path;
            musicPlaybacks[slot].follow(clip.getSong(), fade);
        }

        musicSeen[slot] = true;
        musicPlaybacks[slot].setVolume(fade);
        musicPlaybacks[slot].followTo(clip.toSongTick(childTime));
    }

    private int findMusic(RecordingClipStruct clip, long path) {

        for (int i = 0; i < musicCount; i++)
            if (musicClips.get(i) == clip && musicPaths[i] == path)
                return i;

        return EngineSetting.INDEX_NOT_FOUND;
    }

    // The slot's song stopped, every note it held let go, the last slot moved into its place with its playback
    private void releaseMusic(int slot) {

        SongPlaybackInstance stopped = musicPlaybacks[slot];
        int last = musicCount - 1;

        stopped.stop();
        musicPlaybacks[slot] = musicPlaybacks[last];
        musicPlaybacks[last] = stopped;
        musicClips.set(slot, musicClips.get(last));
        musicClips.set(last, null);
        musicPaths[slot] = musicPaths[last];
        musicSeen[slot] = musicSeen[last];
        this.musicCount--;
    }

    // Every voice a followed song's held notes sound through kept, so none of them is captured into a take, and
    // every voice that has stopped let go
    private void collectMusicVoices() {

        for (int i = musicVoices.size() - 1; i >= 0; i--)
            if (!soundManager.isVoicePlaying(musicVoices.getInt(i)))
                musicVoices.removeInt(i);

        for (int i = 0; i < musicCount; i++) {

            SongPlaybackInstance playback = musicPlaybacks[i];

            for (int held = 0; held < playback.getHeldCount(); held++) {

                int voice = samplerManager.getNoteVoice(playback.getHeldNote(held));

                if (voice != EngineSetting.INDEX_NOT_FOUND && !musicVoices.contains(voice))
                    musicVoices.add(voice);
            }
        }
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

    // The sound laid on the take from the moment it began, as long as it plays unless it loops; one begun before the
    // take starts before it, so it is heard from as far in as it had played, but a loop starts with the take, where
    // it plays on all the same
    private void beginCapture(RecordingSoundTrackStruct track, int slot, SoundVoiceStruct voice, float takeTime) {

        SoundHandle soundHandle = voice.getSoundHandle();
        float began = takeTime - (internal.getTime() - voice.getStartMillis()) * EngineSetting.SOUND_SECONDS_PER_MILLI;

        if (voice.isLooping())
            began = Math.max(0f, began);

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

    // Begun by this window's own systems, or by the shared world within this window's world or for no world at all
    private boolean isHeardHere(SoundVoiceStruct voice, int windowID, WorldHandle worldHandle) {
        return voice.getOwnerWindowID() == windowID
                || voice.getOwnerWindowID() == EngineSetting.INDEX_NOT_FOUND
                        && (!voice.isSpatial() || voice.getWorldHandle() == worldHandle);
    }

    private WorldHandle resolveWorld() {
        return playerManager.getPlayerForWindow(context.getWindow().getWindowID()).getWorldHandle();
    }
}
