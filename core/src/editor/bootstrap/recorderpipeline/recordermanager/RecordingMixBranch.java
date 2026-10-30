package editor.bootstrap.recorderpipeline.recordermanager;

import application.bootstrap.soundpipeline.sound.SoundHandle;
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
     * once at the export's rate; then each video frame's stretch of sound is
     * mixed from every unmuted clip holding the sound channel — sounds add up,
     * whatever their lanes — each sound read from the moment it began, at its
     * pitch, looping when its sound loops, cut where its clip ends, as loud
     * and from the side it reached the camera when it was recorded. A mono
     * sound is spread over both sides by equal power, a stereo one balanced.
     * A sound the game no longer has is left silent. The decoded sounds are
     * let go once the export ends.
     */

    // Internal
    private SoundManager soundManager;

    // Samples
    private Object2ObjectOpenHashMap<String, SoundPcmStruct> soundName2Pcm;
    private int sampleRate;

    // Scratch
    private float[] mixScratch;

    // Base \\

    @Override
    protected void create() {

        // Samples
        this.soundName2Pcm = new Object2ObjectOpenHashMap<>();

        // Scratch
        this.mixScratch = new float[0];
    }

    @Override
    protected void get() {
        this.soundManager = get(SoundManager.class);
    }

    // Prepare \\

    // Every sound heard between the moments decoded once at the rate, true when any is
    boolean prepare(RecordingInstance recording, float from, float to, int sampleRate) {

        clear();
        this.sampleRate = sampleRate;

        ObjectArrayList<RecordingClipStruct> clips = recording.getClips();

        for (int i = 0; i < clips.size(); i++) {

            RecordingClipStruct clip = clips.get(i);

            if (!isSounding(clip, from, to))
                continue;

            RecordingSoundTrackStruct track = clip.getTake().getSoundTrack();

            for (int event = 0; event < track.size(); event++)
                if (clip.toTimelineTime(track.getTime(event)) < to
                        && resolveEnd(clip, track, event) > Math.max(from, clip.getStart()))
                    decode(track.getSoundName(event));
        }

        return !soundName2Pcm.isEmpty();
    }

    private void decode(String soundName) {
        if (!soundName2Pcm.containsKey(soundName) && soundManager.hasSound(soundName))
            soundName2Pcm.put(soundName, soundManager.decodeSound(
                    soundManager.getSoundIDFromSoundName(soundName), sampleRate));
    }

    void clear() {
        soundName2Pcm.clear();
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

        float to = time + (float) frameCount / sampleRate;
        ObjectArrayList<RecordingClipStruct> clips = recording.getClips();

        for (int i = 0; i < clips.size(); i++) {

            RecordingClipStruct clip = clips.get(i);

            if (!isSounding(clip, time, to))
                continue;

            RecordingSoundTrackStruct track = clip.getTake().getSoundTrack();
            float takeTo = clip.toTakeTime(to);

            for (int event = track.findFirstSounding(clip.toTakeTime(time));
                    event < track.size() && track.getTime(event) < takeTo; event++)
                mixEvent(clip, track, event, time, frameCount);
        }

        for (int i = 0; i < sampleCount; i++)
            samples[i] = SoundDecodeUtility.toSample(mixScratch[i] / EngineSetting.SOUND_SAMPLE_SCALE);
    }

    // One sound added across the part of the frames it is heard in, read between its two nearest samples
    private void mixEvent(
            RecordingClipStruct clip,
            RecordingSoundTrackStruct track,
            int event,
            float time,
            int frameCount) {

        SoundPcmStruct pcm = soundName2Pcm.get(track.getSoundName(event));

        if (pcm == null)
            return;

        SoundHandle soundHandle = soundManager.getSoundHandleFromSoundName(track.getSoundName(event));
        float began = clip.toTimelineTime(track.getTime(event));
        float from = Math.max(Math.max(began, clip.getStart()), time);
        float until = resolveEnd(clip, track, event);
        int firstFrame = Math.max(0, (int) Math.ceil((from - time) * sampleRate));
        int endFrame = Math.min(frameCount, (int) Math.ceil((until - time) * sampleRate));

        if (endFrame <= firstFrame)
            return;

        short[] source = pcm.getSamples();
        int channels = pcm.getChannels();
        int sourceFrames = pcm.getFrameCount();
        boolean loop = soundHandle.isLoop();
        double step = track.getPitch(event) * soundHandle.getPitch();
        double position = ((time - began) * sampleRate + firstFrame) * step;
        float gain = track.getHeardGain(event);
        float pan = track.getHeardPan(event);
        float leftGain = resolveLeftGain(channels, gain, pan);
        float rightGain = resolveRightGain(channels, gain, pan);

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

            mixScratch[out] += left * leftGain;
            mixScratch[out + 1] += right * rightGain;
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
}
