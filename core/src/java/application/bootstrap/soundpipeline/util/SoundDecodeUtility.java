package application.bootstrap.soundpipeline.util;

import java.io.File;

import application.bootstrap.soundpipeline.sound.SoundPcmStruct;
import engine.root.EngineContext;
import engine.root.EngineSetting;
import engine.root.EngineUtility;

public class SoundDecodeUtility extends EngineUtility {

    /*
     * Decodes an Ogg Vorbis sound file, through the audio backend, into
     * signed 16-bit samples. The samples are then brought to the rate and
     * channels asked for, each frame read between its two nearest source
     * frames, a mono sound spread over both sides and a sound of more channels
     * than wanted averaged down to one or read from its first two. A sound may
     * be held to a length — a long recording kept as a short loop, its tail
     * faded into its head so it loops with no seam — and its file is then
     * decoded no further than that. A file that cannot be read is fatal,
     * naming the file. Never held — all methods static.
     */

    // Decode \\

    // The file's samples at their own rate, never above the cap, downmixed to mono when asked, and held to the
    // length given when it is above nothing, looping seamlessly across the crossfade when that is too
    public static SoundPcmStruct decode(
            File file,
            int sampleRateCap,
            boolean mono,
            float keepSeconds,
            float loopCrossfadeSeconds) {

        SoundPcmStruct source = decodeSource(file, keepSeconds + loopCrossfadeSeconds);
        int channels = mono ? EngineSetting.SOUND_MONO_CHANNELS
                : Math.min(source.getChannels(), EngineSetting.SOUND_STEREO_CHANNELS);

        return trim(convert(source, Math.min(source.getSampleRate(), sampleRateCap), channels),
                keepSeconds, loopCrossfadeSeconds);
    }

    // The file's samples at exactly the rate and over exactly the channels given, held and looped as decode() holds
    // and loops them
    public static SoundPcmStruct decodeAt(
            File file,
            int sampleRate,
            int channels,
            float keepSeconds,
            float loopCrossfadeSeconds) {

        return trim(convert(decodeSource(file, keepSeconds + loopCrossfadeSeconds), sampleRate, channels),
                keepSeconds, loopCrossfadeSeconds);
    }

    // The file's samples as they are, no more than the seconds given of them when that is above nothing
    private static SoundPcmStruct decodeSource(File file, float maxSeconds) {

        int[] format = new int[EngineSetting.SOUND_VORBIS_FORMAT_FIELDS];
        short[] samples = EngineContext.audio.decodeVorbis(file.getAbsolutePath(), maxSeconds, format);
        int channels = format[EngineSetting.SOUND_VORBIS_FORMAT_CHANNELS];
        int sampleRate = format[EngineSetting.SOUND_VORBIS_FORMAT_SAMPLE_RATE];

        if (samples == null || channels <= 0 || sampleRate <= 0)
            throwException("Sound file '" + file.getAbsolutePath() + "' is not a readable Ogg Vorbis file.");

        if (samples.length == 0)
            throwException("Sound file '" + file.getAbsolutePath() + "' holds no samples.");

        return new SoundPcmStruct(samples, channels, sampleRate);
    }

    // Convert \\

    // The samples brought to the rate and channels given, each frame read between its two nearest source frames
    private static SoundPcmStruct convert(SoundPcmStruct source, int sampleRate, int channels) {

        if (source.getSampleRate() == sampleRate && source.getChannels() == channels)
            return source;

        short[] input = source.getSamples();
        int inputChannels = source.getChannels();
        int inputFrames = source.getFrameCount();
        int outputFrames = (int) ((long) inputFrames * sampleRate / source.getSampleRate());
        double step = (double) source.getSampleRate() / sampleRate;
        short[] output = new short[outputFrames * channels];

        for (int frame = 0; frame < outputFrames; frame++) {

            double position = frame * step;
            int first = Math.min((int) position, inputFrames - 1);
            int second = Math.min(first + 1, inputFrames - 1);
            float blend = (float) (position - first);

            for (int channel = 0; channel < channels; channel++) {

                float a = readChannel(input, inputChannels, first, channel, channels);
                float b = readChannel(input, inputChannels, second, channel, channels);

                output[frame * channels + channel] = (short) Math.round(a + (b - a) * blend);
            }
        }

        return new SoundPcmStruct(output, channels, sampleRate);
    }

    // One output channel of a source frame: every source channel averaged for mono, else the source channel of the
    // same side, a mono source feeding both
    private static float readChannel(short[] input, int inputChannels, int frame, int channel, int channels) {

        int base = frame * inputChannels;

        if (channels == EngineSetting.SOUND_MONO_CHANNELS && inputChannels > 1) {

            float sum = 0f;

            for (int i = 0; i < inputChannels; i++)
                sum += input[base + i];

            return sum / inputChannels;
        }

        return input[base + Math.min(channel, inputChannels - 1)];
    }

    // Trim \\

    // The samples held to the seconds given when that is above nothing; the part past the end is faded into the start
    // across the crossfade, so the samples loop with no seam
    private static SoundPcmStruct trim(SoundPcmStruct pcm, float keepSeconds, float loopCrossfadeSeconds) {

        int frames = pcm.getFrameCount();
        int keptFrames = (int) Math.ceil(keepSeconds * pcm.getSampleRate());

        if (keepSeconds <= 0f || keptFrames >= frames)
            return pcm;

        int channels = pcm.getChannels();
        int fadeFrames = Math.min((int) (loopCrossfadeSeconds * pcm.getSampleRate()), frames - keptFrames);
        short[] source = pcm.getSamples();
        short[] kept = new short[keptFrames * channels];

        System.arraycopy(source, 0, kept, 0, kept.length);

        for (int frame = 0; frame < fadeFrames; frame++) {

            double turn = (double) frame / fadeFrames * EngineSetting.SOUND_QUARTER_TURN;
            float rise = (float) Math.sin(turn);
            float fall = (float) Math.cos(turn);

            for (int channel = 0; channel < channels; channel++) {

                int at = frame * channels + channel;

                kept[at] = (short) Math.clamp(Math.round(source[at] * rise
                        + source[(keptFrames + frame) * channels + channel] * fall),
                        Short.MIN_VALUE, Short.MAX_VALUE);
            }
        }

        return new SoundPcmStruct(kept, channels, pcm.getSampleRate());
    }

    // Utility \\

    // A sample from -1 to 1 as a signed 16-bit value, held within range
    public static short toSample(float value) {
        return (short) Math.round(Math.clamp(value, -1f, 1f) * EngineSetting.SOUND_SAMPLE_SCALE);
    }
}
