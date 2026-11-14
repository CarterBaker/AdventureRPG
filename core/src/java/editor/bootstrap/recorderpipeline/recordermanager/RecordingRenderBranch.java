package editor.bootstrap.recorderpipeline.recordermanager;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

import application.bootstrap.musicpipeline.instrument.InstrumentHandle;
import application.bootstrap.musicpipeline.instrumentmanager.InstrumentManager;
import application.bootstrap.musicpipeline.sampler.SamplerPcmStruct;
import application.bootstrap.musicpipeline.song.SongClipKind;
import application.bootstrap.musicpipeline.song.SongCursorStruct;
import application.bootstrap.musicpipeline.song.SongData;
import application.bootstrap.musicpipeline.util.SamplerMixUtility;
import application.bootstrap.musicpipeline.util.SongFlattenUtility;
import application.bootstrap.soundpipeline.sound.SoundHandle;
import application.bootstrap.soundpipeline.soundmanager.SoundManager;
import application.bootstrap.soundpipeline.util.SoundDecodeUtility;
import application.kernel.threadpipeline.thread.ThreadHandle;
import editor.bootstrap.recorderpipeline.util.FfmpegUtility;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class RecordingRenderBranch extends BranchPackage {

    /*
     * Renders a Music file to the engine's ordinary sound: the song laid out
     * flat as playback lays it, every part mixed offline through
     * SamplerMixUtility at RECORDER_AUDIO_SAMPLE_RATE in stereo, each placed
     * by its lane's pan with equal power and played past the song's end for
     * its notes' release, then encoded through FfmpegUtility to
     * assets/sounds/music/<Name>.ogg with Delivery.bat's settings, and a
     * companion of the music category written beside it through ArpgUtility,
     * ready for a music cue. The main thread names every sample once and
     * hands the song to the General thread, which decodes, mixes a stretch at
     * a time into a raw file, encodes it and publishes its share done and its
     * last word; the main thread reads them each frame and writes the
     * companion once it succeeds. One render runs at a time, and a failed
     * one leaves no sound behind.
     */

    // Internal
    private SoundManager soundManager;
    private InstrumentManager instrumentManager;
    private ThreadHandle threadHandle;

    // Render — what the worker publishes, read by the main thread once finished is set
    private volatile int percent;
    private volatile boolean finished;
    private volatile boolean succeeded;
    private volatile String message;
    private boolean running;
    private File soundFile;

    // Base \\

    @Override
    protected void get() {
        this.soundManager = get(SoundManager.class);
        this.instrumentManager = get(InstrumentManager.class);
        this.threadHandle = getThreadHandleFromThreadName(EngineSetting.GENERAL_THREAD_NAME);
    }

    // Begin \\

    // The song handed to the worker to be rendered under the name; null once it runs, else why it could not begin
    String begin(String songName, SongData song) {

        File ffmpeg = FfmpegUtility.findFfmpeg();

        if (ffmpeg == null)
            return EditorSetting.RECORDER_STATUS_FFMPEG_MISSING;

        if (song.getLength() <= 0)
            return EditorSetting.RECORDER_STATUS_RENDER_EMPTY;

        ObjectArrayList<SongCursorStruct> cursors = new ObjectArrayList<>();
        int count = SongFlattenUtility.flatten(song, EngineSetting.SONG_LANE_ALL, cursors);
        InstrumentHandle[] instruments = new InstrumentHandle[count];
        ObjectArrayList<String> soundNames = new ObjectArrayList<>();

        for (int i = 0; i < count; i++) {

            SongCursorStruct cursor = cursors.get(i);

            instruments[i] = cursor.getKind() == SongClipKind.NOTES
                    ? instrumentManager.getInstrumentHandleFromInstrumentID(cursor.getInstrumentID())
                    : null;
            SamplerMixUtility.collectSamples(cursor, instruments[i], soundNames);
        }

        Object2ObjectOpenHashMap<String, SoundHandle> soundName2Handle = new Object2ObjectOpenHashMap<>();

        for (int i = 0; i < soundNames.size(); i++) {

            int soundID = soundManager.findSoundID(soundNames.get(i));

            if (soundID != EngineSetting.INDEX_NOT_FOUND)
                soundName2Handle.put(soundNames.get(i), soundManager.getSoundHandleFromSoundID(soundID));
        }

        this.soundFile = resolveSoundFile(songName);
        this.percent = 0;
        this.finished = false;
        this.succeeded = false;
        this.running = true;

        File target = soundFile;
        int frameCount = (int) Math.ceil((song.getTempo().toSeconds(song.getLength())
                + EditorSetting.RECORDER_RENDER_TAIL_SECONDS) * EditorSetting.RECORDER_AUDIO_SAMPLE_RATE);

        executeAsync(threadHandle, () -> render(ffmpeg, song, cursors, count, instruments, soundName2Handle,
                frameCount, target));
        return null;
    }

    // Worker \\

    // Every sample decoded, the song mixed a stretch at a time into a raw file beside the target, encoded, and the raw
    // file let go
    private void render(
            File ffmpeg,
            SongData song,
            ObjectArrayList<SongCursorStruct> cursors,
            int count,
            InstrumentHandle[] instruments,
            Object2ObjectOpenHashMap<String, SoundHandle> soundName2Handle,
            int frameCount,
            File target) {

        File pcmFile = new File(target.getParentFile(), target.getName() + EditorSetting.RECORDER_RENDER_RAW_SUFFIX);

        try {

            Object2ObjectOpenHashMap<String, SamplerPcmStruct> samples = decode(soundName2Handle);

            target.getParentFile().mkdirs();
            mix(song, cursors, count, instruments, samples, frameCount, pcmFile);
            FfmpegUtility.encodeVorbis(ffmpeg, pcmFile, EditorSetting.RECORDER_AUDIO_SAMPLE_RATE,
                    EditorSetting.RECORDER_AUDIO_CHANNELS, target);
            this.message = target.getAbsolutePath();
            this.succeeded = true;
        } catch (IOException | InternalException e) {
            this.message = e.getMessage();
            target.delete();
        } finally {
            pcmFile.delete();
            this.percent = EditorSetting.RECORDER_RENDER_PERCENT_MAX;
            this.finished = true;
        }
    }

    private Object2ObjectOpenHashMap<String, SamplerPcmStruct> decode(
            Object2ObjectOpenHashMap<String, SoundHandle> soundName2Handle) {

        Object2ObjectOpenHashMap<String, SamplerPcmStruct> samples = new Object2ObjectOpenHashMap<>();

        for (Object2ObjectOpenHashMap.Entry<String, SoundHandle> entry : soundName2Handle.object2ObjectEntrySet()) {

            SoundHandle handle = entry.getValue();

            samples.put(entry.getKey(), new SamplerPcmStruct(handle, SoundDecodeUtility.decodeAt(
                    handle.getFile(), EditorSetting.RECORDER_AUDIO_SAMPLE_RATE, handle.getChannels(),
                    handle.getKeepSeconds(), handle.getLoopCrossfadeSeconds())));
        }

        return samples;
    }

    // The whole song mixed a stretch at a time, each stretch written as 16-bit little-endian stereo
    private void mix(
            SongData song,
            ObjectArrayList<SongCursorStruct> cursors,
            int count,
            InstrumentHandle[] instruments,
            Object2ObjectOpenHashMap<String, SamplerPcmStruct> samples,
            int frameCount,
            File pcmFile) throws IOException {

        int channels = EditorSetting.RECORDER_AUDIO_CHANNELS;
        int stretch = EditorSetting.RECORDER_RENDER_STRETCH_FRAMES;
        float[] mixed = new float[stretch * channels];
        byte[] bytes = new byte[stretch * channels * Short.BYTES];

        try (OutputStream out = new BufferedOutputStream(new FileOutputStream(pcmFile))) {

            for (int first = 0; first < frameCount; first += stretch) {

                int frames = Math.min(stretch, frameCount - first);
                double startSeconds = (double) first / EditorSetting.RECORDER_AUDIO_SAMPLE_RATE;

                Arrays.fill(mixed, 0f);

                for (int i = 0; i < count; i++) {

                    SongCursorStruct cursor = cursors.get(i);
                    float pan = song.getLane(cursor.getLane()).getPan();
                    float angle = (pan + 1f) * EditorSetting.RECORDER_AUDIO_PAN_EIGHTH_TURN;

                    SamplerMixUtility.mixCursor(cursor, instruments[i], song.getTempo(), 0.0, startSeconds, samples,
                            (float) Math.cos(angle), (float) Math.sin(angle), mixed, 0, frames,
                            EditorSetting.RECORDER_AUDIO_SAMPLE_RATE);
                }

                for (int at = 0; at < frames * channels; at++) {

                    short sample = SoundDecodeUtility.toSample(mixed[at] / EngineSetting.SOUND_SAMPLE_SCALE);

                    bytes[at * Short.BYTES] = (byte) sample;
                    bytes[at * Short.BYTES + 1] = (byte) (sample >> Byte.SIZE);
                }

                out.write(bytes, 0, frames * channels * Short.BYTES);
                this.percent = (int) ((long) (first + frames) * EditorSetting.RECORDER_RENDER_MIXED_PERCENT
                        / frameCount);
            }
        }
    }

    // Main Thread \\

    boolean isRunning() {
        return running;
    }

    boolean isFinished() {
        return running && finished;
    }

    int getPercent() {
        return percent;
    }

    // The render's last word, its music companion written beside the sound when it succeeded
    String collectResult() {

        this.running = false;
        this.finished = false;

        if (!succeeded)
            return EditorSetting.RECORDER_STATUS_RENDER_FAILED + message;

        ArpgObjectStruct companionArpg = new ArpgObjectStruct();

        companionArpg.addProperty("category", EditorSetting.RECORDER_RENDER_CATEGORY);

        if (!ArpgUtility.tryWriteObject(ArpgUtility.resolveCompanionFile(soundFile), companionArpg))
            return EditorSetting.RECORDER_STATUS_RENDER_FAILED + soundFile.getAbsolutePath();

        return EditorSetting.RECORDER_STATUS_RENDERED + message;
    }

    // The music sound a song of the name renders to
    File resolveSoundFile(String songName) {
        return new File(new File(EngineSetting.SOUND_PATH, EditorSetting.RECORDER_RENDER_DIRECTORY),
                songName + EditorSetting.RECORDER_EXTENSION_SEPARATOR + EngineSetting.SOUND_FILE_EXTENSION);
    }
}
