package editor.bootstrap.recorderpipeline.recordermanager;

import application.bootstrap.musicpipeline.instrument.InstrumentHandle;
import application.bootstrap.musicpipeline.instrumentmanager.InstrumentManager;
import application.bootstrap.musicpipeline.sampler.SamplerPcmStruct;
import application.bootstrap.musicpipeline.song.SongClipKind;
import application.bootstrap.musicpipeline.song.SongCursorStruct;
import application.bootstrap.musicpipeline.util.SamplerMixUtility;
import application.bootstrap.musicpipeline.util.SongFlattenUtility;
import application.bootstrap.soundpipeline.sound.SoundPcmStruct;
import application.bootstrap.soundpipeline.soundmanager.SoundManager;
import application.bootstrap.soundpipeline.util.SoundDecodeUtility;
import editor.bootstrap.recorderpipeline.recording.RecorderChannel;
import editor.bootstrap.recorderpipeline.recording.RecordingClipStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingInstance;
import editor.bootstrap.recorderpipeline.recording.RecordingSoundTrackStruct;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class RecordingMixBranch extends BranchPackage {

    /*
     * Mixes a recording's sounds into the stereo sound of its export. Before
     * the export begins every sound a clip plays within its range is decoded
     * once at the export's rate, and every sample the songs and notes clips
     * within it can sound; then each video frame's stretch of sound is mixed
     * from every unmuted clip holding the sound channel — sounds add up,
     * whatever their lanes — walking down into every nested recording at the
     * moment mapped into it, its sounds heard only within the nested clip's
     * span and faded by every transition around it. A take's sound is read
     * from the moment it began, at its pitch, looping when its sound loops,
     * cut where its clip ends, as loud and from the side it reached the
     * camera when it was recorded, and faded in and out across its clip's
     * transitions; a song or a notes clip is laid out flat and rendered
     * through SamplerMixUtility, the one offline sampler, heard from the
     * middle. A mono sound is spread over both sides by equal power, a stereo
     * one balanced. A sound the game no longer has is left silent. The
     * decoded sounds are let go once the export ends.
     */

    // Internal
    private SoundManager soundManager;
    private InstrumentManager instrumentManager;
    private RecordingTransitionBranch recordingTransitionBranch;

    // Samples
    private Object2ObjectOpenHashMap<String, SamplerPcmStruct> soundName2Sample;
    private int sampleRate;

    // Scratch
    private float[] mixScratch;
    private ObjectArrayList<SongCursorStruct> cursors;
    private ObjectArrayList<String> soundNames;

    // Base \\

    @Override
    protected void create() {

        // Samples
        this.soundName2Sample = new Object2ObjectOpenHashMap<>();

        // Scratch
        this.mixScratch = new float[0];
        this.cursors = new ObjectArrayList<>();
        this.soundNames = new ObjectArrayList<>();
    }

    @Override
    protected void get() {
        this.soundManager = get(SoundManager.class);
        this.instrumentManager = get(InstrumentManager.class);
        this.recordingTransitionBranch = get(RecordingTransitionBranch.class);
    }

    // Prepare \\

    // Every sound and sample heard between the moments decoded once at the rate, true when any is
    boolean prepare(RecordingInstance recording, float from, float to, int sampleRate) {

        clear();
        this.sampleRate = sampleRate;
        soundNames.clear();
        collectRecording(recording, from, to);

        for (int i = 0; i < soundNames.size(); i++)
            decode(soundNames.get(i));

        soundNames.clear();
        return !soundName2Sample.isEmpty();
    }

    // Every sound a clip of the recording sounds between the moments named, nested recordings walked at the moments
    // mapped into them
    private void collectRecording(RecordingInstance recording, float from, float to) {

        ObjectArrayList<RecordingClipStruct> clips = recording.getClips();

        for (int i = 0; i < clips.size(); i++) {

            RecordingClipStruct clip = clips.get(i);

            if (!isSounding(clip, from, to))
                continue;

            float clipFrom = Math.max(from, clip.getStart());
            float clipTo = Math.min(to, clip.getEnd());

            if (clip.getTake() != null)
                collectTake(clip, clip.getTake().getSoundTrack(), clipFrom, clipTo);

            if (clip.getChild() != null)
                collectRecording(clip.getChild(), clip.toChildTime(clipFrom), clip.toChildTime(clipTo));

            if (clip.getSong() != null)
                collectSong(clip);
        }
    }

    private void collectTake(RecordingClipStruct clip, RecordingSoundTrackStruct track, float from, float to) {
        for (int event = 0; event < track.size(); event++)
            if (clip.toTimelineTime(track.getTime(event)) < to && resolveEnd(clip, track, event) > from)
                addName(track.getSoundName(event));
    }

    // Every sample the song the clip sounds can play, each of its notes clips on its instrument
    private void collectSong(RecordingClipStruct clip) {

        int count = SongFlattenUtility.flatten(clip.getSong(), EngineSetting.SONG_LANE_ALL, cursors);

        for (int i = 0; i < count; i++) {

            SongCursorStruct cursor = cursors.get(i);

            SamplerMixUtility.collectSamples(cursor, resolveInstrument(cursor), soundNames);
        }
    }

    private void addName(String soundName) {
        if (!soundNames.contains(soundName))
            soundNames.add(soundName);
    }

    // The sound decoded at the mix's rate, loaded first when it has not been yet; left silent when the game has no
    // such sound
    private void decode(String soundName) {

        int soundID = soundManager.findSoundID(soundName);

        if (soundID == EngineSetting.INDEX_NOT_FOUND || soundName2Sample.containsKey(soundName))
            return;

        soundName2Sample.put(soundName, new SamplerPcmStruct(
                soundManager.getSoundHandleFromSoundID(soundID), soundManager.decodeSound(soundID, sampleRate)));
    }

    void clear() {
        soundName2Sample.clear();
        this.mixScratch = new float[0];
    }

    // Mix \\

    // The frames of sound from the moment, every sound playing across them added together and held within range
    void mix(RecordingInstance recording, float time, short[] samples, int frameCount) {

        int sampleCount = frameCount * EditorSetting.RECORDER_AUDIO_CHANNELS;

        if (mixScratch.length < sampleCount)
            this.mixScratch = new float[sampleCount];

        for (int i = 0; i < sampleCount; i++)
            mixScratch[i] = 0f;

        mixRecording(recording, time, 1f, 0, frameCount);

        for (int i = 0; i < sampleCount; i++)
            samples[i] = SoundDecodeUtility.toSample(mixScratch[i] / EngineSetting.SOUND_SAMPLE_SCALE);
    }

    // Every clip of the recording sounding within the frames given of the stretch beginning at the time — the frames
    // every nested clip around it leaves open — faded by every transition around it
    private void mixRecording(RecordingInstance recording, float time, float fade, int firstFrame, int endFrame) {

        ObjectArrayList<RecordingClipStruct> clips = recording.getClips();
        float from = time + (float) firstFrame / sampleRate;
        float to = time + (float) endFrame / sampleRate;

        for (int i = 0; i < clips.size(); i++) {

            RecordingClipStruct clip = clips.get(i);

            if (!isSounding(clip, from, to))
                continue;

            int clipFirst = Math.max(firstFrame, toFrame(clip.getStart() - time));
            int clipEnd = Math.min(endFrame, toFrame(clip.getEnd() - time));

            if (clipEnd <= clipFirst)
                continue;

            float childTime = clip.toChildTime(time);
            float middleFade = fade * recordingTransitionBranch.resolveGain(clip,
                    time + (float) ((clipFirst + clipEnd) >>> 1) / sampleRate);

            if (clip.getTake() != null)
                mixTake(clip, clip.getTake().getSoundTrack(), time, fade, clipFirst, clipEnd);

            if (clip.getChild() != null)
                mixRecording(clip.getChild(), childTime, middleFade, clipFirst, clipEnd);

            if (clip.getSong() != null)
                mixSong(clip, childTime, middleFade, clipFirst, clipEnd);
        }
    }

    private void mixTake(
            RecordingClipStruct clip,
            RecordingSoundTrackStruct track,
            float time,
            float fade,
            int firstFrame,
            int endFrame) {

        float takeTo = clip.toChildTime(time + (float) endFrame / sampleRate);

        for (int event = track.findFirstSounding(clip.toChildTime(time + (float) firstFrame / sampleRate));
                event < track.size() && track.getTime(event) < takeTo; event++)
            mixEvent(clip, track, event, time, fade, firstFrame, endFrame);
    }

    // One sound added across the part of the frames it is heard in, read between its two nearest samples
    private void mixEvent(
            RecordingClipStruct clip,
            RecordingSoundTrackStruct track,
            int event,
            float time,
            float fade,
            int windowFirst,
            int windowEnd) {

        SamplerPcmStruct sample = soundName2Sample.get(track.getSoundName(event));

        if (sample == null)
            return;

        SoundPcmStruct pcm = sample.getPcm();
        float began = clip.toTimelineTime(track.getTime(event));
        float from = Math.max(Math.max(began, clip.getStart()), time);
        float until = resolveEnd(clip, track, event);
        int firstFrame = Math.max(windowFirst, toFrame(from - time));
        int endFrame = Math.min(windowEnd, toFrame(until - time));

        if (endFrame <= firstFrame)
            return;

        short[] source = pcm.getSamples();
        int channels = pcm.getChannels();
        int sourceFrames = pcm.getFrameCount();
        boolean loop = sample.isLooping();
        double step = track.getPitch(event) * sample.getPitch();
        double position = ((time - began) * sampleRate + firstFrame) * step;
        float gain = track.getHeardGain(event) * fade;
        float pan = track.getHeardPan(event);
        float leftGain = resolveLeftGain(channels, gain, pan);
        float rightGain = resolveRightGain(channels, gain, pan);
        boolean faded = clip.hasTransitions();

        for (int frame = firstFrame; frame < endFrame; frame++, position += step) {

            double read = loop ? position % sourceFrames : position;

            if (!loop && read >= sourceFrames - 1)
                break;

            int first = (int) read;
            int second = loop ? (first + 1) % sourceFrames : first + 1;
            float blend = (float) (read - first);
            int out = frame * EditorSetting.RECORDER_AUDIO_CHANNELS;
            float left = readBlended(source, channels, first, second, 0, blend);
            float right = channels == EngineSetting.SOUND_MONO_CHANNELS
                    ? left
                    : readBlended(source, channels, first, second, 1, blend);

            float transitionFade = faded
                    ? recordingTransitionBranch.resolveGain(clip, time + (float) frame / sampleRate)
                    : 1f;

            mixScratch[out] += left * leftGain * transitionFade;
            mixScratch[out + 1] += right * rightGain * transitionFade;
        }
    }

    // The song the clip sounds laid out flat and every part of it rendered through the offline sampler, heard from
    // the middle within the frames the clip leaves open
    private void mixSong(RecordingClipStruct clip, float childTime, float fade, int firstFrame, int endFrame) {

        int count = SongFlattenUtility.flatten(clip.getSong(), EngineSetting.SONG_LANE_ALL, cursors);
        float leftGain = resolveLeftGain(EngineSetting.SOUND_MONO_CHANNELS, fade, 0f);
        float rightGain = resolveRightGain(EngineSetting.SOUND_MONO_CHANNELS, fade, 0f);

        for (int i = 0; i < count; i++) {

            SongCursorStruct cursor = cursors.get(i);

            SamplerMixUtility.mixCursor(
                    cursor,
                    resolveInstrument(cursor),
                    clip.getSong().getTempo(),
                    clip.resolveSongTickOffset(),
                    clip.toSongSeconds(childTime),
                    soundName2Sample,
                    leftGain,
                    rightGain,
                    mixScratch,
                    firstFrame,
                    endFrame,
                    sampleRate);
        }
    }

    private float readBlended(short[] source, int channels, int first, int second, int channel, float blend) {

        float a = source[first * channels + channel];
        float b = source[second * channels + channel];

        return a + (b - a) * blend;
    }

    // A mono sound spread by equal power, a stereo one balanced toward the side it was heard from
    private float resolveLeftGain(int channels, float gain, float pan) {
        return channels == EngineSetting.SOUND_MONO_CHANNELS
                ? gain * (float) Math.cos((pan + 1f) * EditorSetting.RECORDER_AUDIO_PAN_EIGHTH_TURN)
                : gain * Math.min(1f, 1f - pan);
    }

    private float resolveRightGain(int channels, float gain, float pan) {
        return channels == EngineSetting.SOUND_MONO_CHANNELS
                ? gain * (float) Math.sin((pan + 1f) * EditorSetting.RECORDER_AUDIO_PAN_EIGHTH_TURN)
                : gain * Math.min(1f, 1f + pan);
    }

    // Utility \\

    // True when the clip sounds anywhere between the moments
    private boolean isSounding(RecordingClipStruct clip, float from, float to) {
        return !clip.isMuted() && clip.holds(RecorderChannel.SOUND) && clip.overlaps(from, to);
    }

    // The moment of the timeline the sound stops being heard: when it ends, or its clip does first
    private float resolveEnd(RecordingClipStruct clip, RecordingSoundTrackStruct track, int event) {
        return Math.min(clip.toTimelineTime(track.getTime(event) + track.getDuration(event)), clip.getEnd());
    }

    // The frame of the stretch the seconds into it fall on, rounded up
    private int toFrame(float seconds) {
        return (int) Math.ceil(seconds * sampleRate);
    }

    // The instrument a notes cursor plays on, null for a sound cursor
    private InstrumentHandle resolveInstrument(SongCursorStruct cursor) {
        return cursor.getKind() == SongClipKind.NOTES
                ? instrumentManager.getInstrumentHandleFromInstrumentID(cursor.getInstrumentID())
                : null;
    }
}
