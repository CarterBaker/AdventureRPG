package application.bootstrap.soundpipeline.util;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardOpenOption;

import application.bootstrap.soundpipeline.sound.SoundPcmStruct;
import engine.root.EngineContext;
import engine.root.EngineSetting;
import engine.root.EngineUtility;
import engine.util.io.FileUtility;

public class SoundDecodeUtility extends EngineUtility {

    /*
     * Decodes a sound file into signed 16-bit samples. A WAV file may hold
     * 8, 16, 24 or 32-bit whole samples or 32-bit decimal ones, plainly or in
     * the extensible form; an Ogg Vorbis file is decoded by the audio backend.
     * The samples are then brought to the rate and channels asked for, each
     * frame read between its two nearest source frames, a mono sound spread
     * over both sides and a sound of more channels than wanted averaged down to
     * one or read from its first two. A sound may be held to a length — a
     * long recording kept as a short loop, its tail faded into its head so it
     * loops with no seam — and a WAV file is then read no further than that.
     * A file that cannot be read is fatal, naming the file and why. Never
     * held — all methods static.
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

        String extension = FileUtility.getExtension(file).toLowerCase();
        SoundPcmStruct source = switch (extension) {
            case EngineSetting.SOUND_WAV_EXTENSION -> decodeWav(file, maxSeconds);
            case EngineSetting.SOUND_VORBIS_EXTENSION -> decodeVorbis(file);
            default -> throwException("Sound file '" + file.getAbsolutePath() + "' has an unsupported extension '"
                    + extension + "'. Supported: " + EngineSetting.SOUND_FILE_EXTENSIONS);
        };

        if (source.getFrameCount() == 0)
            throwException("Sound file '" + file.getAbsolutePath() + "' holds no samples.");

        return source;
    }

    // Vorbis \\

    private static SoundPcmStruct decodeVorbis(File file) {

        int[] format = new int[EngineSetting.SOUND_VORBIS_FORMAT_FIELDS];
        short[] samples = EngineContext.audio.decodeVorbis(file.getAbsolutePath(), format);
        int channels = format[EngineSetting.SOUND_VORBIS_FORMAT_CHANNELS];
        int sampleRate = format[EngineSetting.SOUND_VORBIS_FORMAT_SAMPLE_RATE];

        if (samples == null || channels <= 0 || sampleRate <= 0)
            throwException("Sound file '" + file.getAbsolutePath() + "' is not a readable Ogg Vorbis file.");

        return new SoundPcmStruct(samples, channels, sampleRate);
    }

    // WAV \\

    // Only the chunk headers, the format and as much of the data as is kept are read, so a long recording held to a
    // short loop never reads past it
    private static SoundPcmStruct decodeWav(File file, float maxSeconds) {

        try (FileChannel channel = FileChannel.open(file.toPath(), StandardOpenOption.READ)) {

            ByteBuffer header = readBlock(channel, 0L, EngineSetting.SOUND_WAV_HEADER_BYTES);

            if (header.limit() < EngineSetting.SOUND_WAV_HEADER_BYTES
                    || !readFourCC(header, 0).equals(EngineSetting.SOUND_WAV_RIFF)
                    || !readFourCC(header, Integer.BYTES * 2).equals(EngineSetting.SOUND_WAV_WAVE))
                throwException("Sound file '" + file.getAbsolutePath() + "' is not a RIFF WAVE file.");

            int formatTag = 0;
            int channels = 0;
            int sampleRate = 0;
            int blockAlign = 0;
            int bits = 0;
            long dataOffset = EngineSetting.INDEX_NOT_FOUND;
            long dataBytes = 0L;
            long cursor = EngineSetting.SOUND_WAV_HEADER_BYTES;
            long fileBytes = channel.size();

            while (cursor + EngineSetting.SOUND_WAV_CHUNK_HEADER_BYTES <= fileBytes
                    && (dataOffset == EngineSetting.INDEX_NOT_FOUND || channels == 0)) {

                ByteBuffer chunk = readBlock(channel, cursor, EngineSetting.SOUND_WAV_CHUNK_HEADER_BYTES);
                String chunkID = readFourCC(chunk, 0);
                long body = cursor + EngineSetting.SOUND_WAV_CHUNK_HEADER_BYTES;
                long available = Math.min(Integer.toUnsignedLong(chunk.getInt(Integer.BYTES)), fileBytes - body);

                if (chunkID.equals(EngineSetting.SOUND_WAV_FORMAT_CHUNK)) {

                    if (available < EngineSetting.SOUND_WAV_FORMAT_MIN_BYTES)
                        throwException("Sound file '" + file.getAbsolutePath() + "' has a truncated format chunk.");

                    ByteBuffer format = readBlock(channel, body, (int) Math.min(available,
                            EngineSetting.SOUND_WAV_EXTENSIBLE_SUBFORMAT_OFFSET + Short.BYTES));

                    formatTag = Short.toUnsignedInt(format.getShort(0));
                    channels = Short.toUnsignedInt(format.getShort(Short.BYTES));
                    sampleRate = format.getInt(Integer.BYTES);
                    blockAlign = Short.toUnsignedInt(format.getShort(Integer.BYTES * 3));
                    bits = Short.toUnsignedInt(format.getShort(Integer.BYTES * 3 + Short.BYTES));

                    if (formatTag == EngineSetting.SOUND_WAV_FORMAT_EXTENSIBLE
                            && format.limit() >= EngineSetting.SOUND_WAV_EXTENSIBLE_SUBFORMAT_OFFSET + Short.BYTES)
                        formatTag = Short.toUnsignedInt(
                                format.getShort(EngineSetting.SOUND_WAV_EXTENSIBLE_SUBFORMAT_OFFSET));
                } else if (chunkID.equals(EngineSetting.SOUND_WAV_DATA_CHUNK)) {
                    dataOffset = body;
                    dataBytes = available;
                }

                cursor = body + available + (available & 1);
            }

            if (dataOffset == EngineSetting.INDEX_NOT_FOUND || channels <= 0 || sampleRate <= 0 || blockAlign <= 0)
                throwException("Sound file '" + file.getAbsolutePath() + "' has no format or data chunk.");

            if (!isSupportedWav(formatTag, bits) || blockAlign < channels * (bits / Byte.SIZE))
                throwException("Sound file '" + file.getAbsolutePath() + "' holds " + bits
                        + "-bit samples of format " + formatTag
                        + ", which cannot be read. Save it as 8, 16, 24 or 32-bit PCM or 32-bit float.");

            long frameCount = dataBytes / blockAlign;

            if (maxSeconds > 0f)
                frameCount = Math.min(frameCount, (long) Math.ceil(maxSeconds * sampleRate));

            if (frameCount * channels > Integer.MAX_VALUE)
                throwException("Sound file '" + file.getAbsolutePath() + "' is too long to hold whole. Give it a "
                        + "\"length\" in its companion file to keep only part of it.");

            ByteBuffer data = readBlock(channel, dataOffset, Math.toIntExact(frameCount * blockAlign));
            int frames = data.limit() / blockAlign;
            int sampleBytes = bits / Byte.SIZE;
            short[] samples = new short[frames * channels];

            for (int frame = 0; frame < frames; frame++)
                for (int sample = 0; sample < channels; sample++)
                    samples[frame * channels + sample] = readWavSample(
                            data, frame * blockAlign + sample * sampleBytes, formatTag, bits);

            return new SoundPcmStruct(samples, channels, sampleRate);
        } catch (IOException e) {
            return throwException("Sound file '" + file.getAbsolutePath() + "' could not be read.", e);
        }
    }

    private static boolean isSupportedWav(int formatTag, int bits) {

        if (formatTag == EngineSetting.SOUND_WAV_FORMAT_FLOAT)
            return bits == Float.SIZE;

        return formatTag == EngineSetting.SOUND_WAV_FORMAT_PCM
                && (bits == Byte.SIZE || bits == Short.SIZE || bits == Short.SIZE + Byte.SIZE || bits == Integer.SIZE);
    }

    // One sample as a signed 16-bit value, whatever its width and kind in the file
    private static short readWavSample(ByteBuffer bytes, int offset, int formatTag, int bits) {

        if (formatTag == EngineSetting.SOUND_WAV_FORMAT_FLOAT)
            return toSample(bytes.getFloat(offset));

        return switch (bits) {
            case Byte.SIZE -> toSample((Byte.toUnsignedInt(bytes.get(offset))
                    - EngineSetting.SOUND_WAV_SIGNED_8_BIT_OFFSET) / EngineSetting.SOUND_WAV_8_BIT_SCALE);
            case Short.SIZE -> bytes.getShort(offset);
            case Short.SIZE + Byte.SIZE -> toSample(((bytes.get(offset + 2) << Short.SIZE)
                    | (Byte.toUnsignedInt(bytes.get(offset + 1)) << Byte.SIZE)
                    | Byte.toUnsignedInt(bytes.get(offset))) / EngineSetting.SOUND_WAV_24_BIT_SCALE);
            default -> toSample(bytes.getInt(offset) / EngineSetting.SOUND_WAV_32_BIT_SCALE);
        };
    }

    // As many bytes as the file holds from the offset, up to the count, in reading order
    private static ByteBuffer readBlock(FileChannel channel, long offset, int count) throws IOException {

        ByteBuffer block = ByteBuffer.allocate(count).order(ByteOrder.LITTLE_ENDIAN);
        int read = 0;

        while (block.hasRemaining() && read >= 0)
            read = channel.read(block, offset + block.position());

        block.flip();
        return block;
    }

    private static String readFourCC(ByteBuffer bytes, int offset) {
        return new String(bytes.array(), offset, Integer.BYTES, StandardCharsets.US_ASCII);
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
