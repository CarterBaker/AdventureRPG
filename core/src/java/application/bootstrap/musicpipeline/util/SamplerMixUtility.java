package application.bootstrap.musicpipeline.util;

import application.bootstrap.musicpipeline.instrument.InstrumentHandle;
import application.bootstrap.musicpipeline.noteset.NoteEnvelopeStruct;
import application.bootstrap.musicpipeline.noteset.NoteLoopMode;
import application.bootstrap.musicpipeline.noteset.NoteRegionStruct;
import application.bootstrap.musicpipeline.noteset.NoteSetHandle;
import application.bootstrap.musicpipeline.sampler.SamplerPcmStruct;
import application.bootstrap.musicpipeline.song.SongClipKind;
import application.bootstrap.musicpipeline.song.SongCursorStruct;
import application.bootstrap.musicpipeline.song.SongTempoStruct;
import application.bootstrap.soundpipeline.sound.SoundPcmStruct;
import engine.root.EngineSetting;
import engine.root.EngineUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class SamplerMixUtility extends EngineUtility {

    /*
     * The one offline render of the sampler, shared by the recorder's video
     * export and the song render: a song laid out flat into cursors is mixed
     * into a stereo buffer one stretch at a time, every note read from the
     * region it plays — the same region SamplerManager would find, its round
     * robin turned by the note's place in its clip — pitched from the
     * sample's root and tuned, at its region's, velocity's, instrument's and
     * clips' gain, shaped by NoteEnvelopeUtility sample by sample: round its
     * sustain loop while held, and once let go — at its end, or when the same
     * pitch is struck again — round the loop still or on through the rest
     * as its loop mode says, fading over its release unless it plays
     * through. A sound clip plays its sound from as far in as it starts,
     * transposed, round and round when the sound loops. Each stretch is
     * worked out from the notes alone, so stretches may be mixed in any order
     * and nothing is carried between them. The caller decodes every sample
     * collectSamples names once, at the mix's rate. Never held — all methods
     * static.
     */

    // Samples \\

    // Every sample the cursor can sound named into the list, each once: the region each of its notes plays, or its
    // sound
    public static void collectSamples(
            SongCursorStruct cursor,
            InstrumentHandle instrument,
            ObjectArrayList<String> soundNames) {

        if (cursor.getKind() == SongClipKind.SOUND) {
            addName(cursor.getClip().getSoundName(), soundNames);
            return;
        }

        NoteSetHandle noteSet = instrument.getNoteSetHandle();

        for (int note = 0; note < cursor.getClip().getNoteCount(); note++) {

            NoteRegionStruct region = findRegion(cursor, instrument, noteSet, note);

            if (region != null)
                addName(region.getSoundName(), soundNames);
        }
    }

    private static void addName(String soundName, ObjectArrayList<String> soundNames) {
        if (!soundNames.contains(soundName))
            soundNames.add(soundName);
    }

    // Mix \\

    // The cursor's notes, or its sound, added over the frames of the stereo mix from the first frame given up to the
    // end frame; the mix's frame 0 falls at the seconds given of the tempo map, the played song's tick 0 at the map's
    // tick offset, and every frame added is scaled by the left and right gains
    public static void mixCursor(
            SongCursorStruct cursor,
            InstrumentHandle instrument,
            SongTempoStruct tempo,
            double tickOffset,
            double startSeconds,
            Object2ObjectOpenHashMap<String, SamplerPcmStruct> samples,
            float leftGain,
            float rightGain,
            float[] mix,
            int firstFrame,
            int endFrame,
            int sampleRate) {

        if (cursor.getKind() == SongClipKind.SOUND) {
            mixSound(cursor, tempo, tickOffset, startSeconds, samples, leftGain, rightGain, mix, firstFrame,
                    endFrame, sampleRate);
            return;
        }

        NoteSetHandle noteSet = instrument.getNoteSetHandle();
        NoteEnvelopeStruct envelope = noteSet.getEnvelope();
        double endSeconds = startSeconds + (double) endFrame / sampleRate;

        for (int note = 0; note < cursor.getClip().getNoteCount(); note++) {

            if (!cursor.playsNote(note))
                continue;

            double onSeconds = tempo.toSeconds(cursor.getNoteTick(note) + tickOffset);

            if (onSeconds >= endSeconds)
                break;

            NoteRegionStruct region = findRegion(cursor, instrument, noteSet, note);
            SamplerPcmStruct sample = region != null ? samples.get(region.getSoundName()) : null;

            if (sample == null)
                continue;

            int velocity = cursor.getClip().getNoteVelocity(note);
            float baseGain = region.getGain()
                    * NoteUtility.toVelocityGain(velocity, noteSet.getVelocityTracking())
                    * instrument.getVolume()
                    * cursor.getGain();

            mixNote(
                    onSeconds,
                    tempo.toSeconds(resolveLetGoTick(cursor, note) + tickOffset),
                    region,
                    envelope,
                    baseGain,
                    NoteUtility.toPitchRatio(cursor.getNotePitch(note), region.getRoot(), region.getTune()),
                    sample,
                    startSeconds,
                    leftGain,
                    rightGain,
                    mix,
                    firstFrame,
                    endFrame,
                    sampleRate);
        }
    }

    // One note added across the frames it is heard in: from its start, through its hold, to the end of its release
    // or of its sample
    private static void mixNote(
            double onSeconds,
            double offSeconds,
            NoteRegionStruct region,
            NoteEnvelopeStruct envelope,
            float baseGain,
            float ratio,
            SamplerPcmStruct sample,
            double startSeconds,
            float leftGain,
            float rightGain,
            float[] mix,
            int firstFrame,
            int endFrame,
            int sampleRate) {

        NoteLoopMode mode = region.getLoopMode();
        float levelAtRelease = NoteEnvelopeUtility.gainAt(envelope, (float) (offSeconds - onSeconds));
        double heardUntil = mode.releases() ? offSeconds + envelope.getRelease() : Double.MAX_VALUE;
        int fromFrame = (int) Math.max(firstFrame, Math.ceil((onSeconds - startSeconds) * sampleRate));
        int toFrame = (int) Math.min(endFrame, Math.ceil((heardUntil - startSeconds) * sampleRate));
        double step = ratio * sample.getPitch();
        double heldTravel = (offSeconds - onSeconds) * sampleRate * step;

        for (int frame = fromFrame; frame < toFrame; frame++) {

            double time = startSeconds + (double) frame / sampleRate;
            double sinceOn = time - onSeconds;
            double position = resolvePosition(sample, mode, sinceOn * sampleRate * step, heldTravel);

            if (position < 0.0)
                break;

            float gain = baseGain * (time < offSeconds || !mode.releases()
                    ? NoteEnvelopeUtility.gainAt(envelope, (float) sinceOn)
                    : NoteEnvelopeUtility.releaseGainAt(envelope, levelAtRelease, (float) (time - offSeconds)));

            addFrame(sample.getPcm(), position, false, gain * leftGain, gain * rightGain, mix, frame);
        }
    }

    // Where in the sample a note reads having travelled so far through it: round its sustain loop while held, and
    // once let go round it still when the mode loops on, else on through the rest from where the loop left it; -1
    // once it has run out
    private static double resolvePosition(
            SamplerPcmStruct sample,
            NoteLoopMode mode,
            double travelled,
            double heldTravel) {

        double position;

        if (!sample.hasLoop() || !mode.loopsHeld())
            position = travelled;
        else if (travelled < heldTravel || mode.loopsReleased())
            position = wrapLoop(sample, travelled);
        else
            position = wrapLoop(sample, heldTravel) + travelled - heldTravel;

        return position < sample.getPcm().getFrameCount() - 1 ? position : EngineSetting.INDEX_NOT_FOUND;
    }

    private static double wrapLoop(SamplerPcmStruct sample, double position) {

        int loopStart = sample.getLoopStartFrame();
        int loopEnd = sample.getLoopEndFrame();

        return position < loopEnd ? position : loopStart + (position - loopStart) % (loopEnd - loopStart);
    }

    // A sound clip's sound added from as far into it as the clip starts, transposed, until the clip's window ends
    private static void mixSound(
            SongCursorStruct cursor,
            SongTempoStruct tempo,
            double tickOffset,
            double startSeconds,
            Object2ObjectOpenHashMap<String, SamplerPcmStruct> samples,
            float leftGain,
            float rightGain,
            float[] mix,
            int firstFrame,
            int endFrame,
            int sampleRate) {

        SamplerPcmStruct sample = samples.get(cursor.getClip().getSoundName());

        if (sample == null)
            return;

        double onSeconds = tempo.toSeconds(cursor.getWindowStart() + tickOffset);
        double offSeconds = tempo.toSeconds(cursor.getWindowEnd() + tickOffset);
        double offsetSeconds = onSeconds - tempo.toSeconds(cursor.toSongTick(0) + tickOffset);
        int fromFrame = (int) Math.max(firstFrame, Math.ceil((onSeconds - startSeconds) * sampleRate));
        int toFrame = (int) Math.min(endFrame, Math.ceil((offSeconds - startSeconds) * sampleRate));
        double step = NoteUtility.toPitchRatio(cursor.getTranspose(), 0, 0f) * sample.getPitch();
        int frames = sample.getPcm().getFrameCount();
        float gain = cursor.getGain();

        for (int frame = fromFrame; frame < toFrame; frame++) {

            double position = (offsetSeconds + (startSeconds + (double) frame / sampleRate - onSeconds))
                    * sampleRate * step;

            if (!sample.isLooping() && position >= frames - 1)
                break;

            addFrame(sample.getPcm(), sample.isLooping() ? position % frames : position, sample.isLooping(),
                    gain * leftGain, gain * rightGain, mix, frame);
        }
    }

    // One frame of the sample, read between its two nearest frames, added to both sides of the mix
    private static void addFrame(
            SoundPcmStruct pcm,
            double position,
            boolean looping,
            float leftGain,
            float rightGain,
            float[] mix,
            int frame) {

        short[] source = pcm.getSamples();
        int channels = pcm.getChannels();
        int first = (int) position;
        int second = looping ? (first + 1) % pcm.getFrameCount() : first + 1;
        float blend = (float) (position - first);
        float left = readBlended(source, channels, first, second, 0, blend);
        float right = channels == EngineSetting.SOUND_MONO_CHANNELS
                ? left
                : readBlended(source, channels, first, second, 1, blend);
        int out = frame * EngineSetting.SAMPLER_MIX_CHANNELS;

        mix[out] += left * leftGain;
        mix[out + 1] += right * rightGain;
    }

    private static float readBlended(short[] source, int channels, int first, int second, int channel, float blend) {

        float a = source[first * channels + channel];
        float b = source[second * channels + channel];

        return a + (b - a) * blend;
    }

    // Utility \\

    // The region the note plays, its round robin turned by its place in its clip; null when none covers it or the
    // instrument does not play its pitch
    private static NoteRegionStruct findRegion(
            SongCursorStruct cursor,
            InstrumentHandle instrument,
            NoteSetHandle noteSet,
            int note) {

        int pitch = cursor.getNotePitch(note);

        if (!instrument.plays(pitch))
            return null;

        return noteSet.findRegion(pitch, cursor.getClip().getNoteVelocity(note), note);
    }

    // The tick the note is let go at: its end, or sooner when the same pitch is struck again before then
    private static int resolveLetGoTick(SongCursorStruct cursor, int note) {

        int endTick = cursor.getNoteEndTick(note);
        int pitch = cursor.getNotePitch(note);

        for (int next = note + 1; next < cursor.getClip().getNoteCount(); next++) {

            int tick = cursor.getNoteTick(next);

            if (tick >= endTick)
                break;

            if (cursor.getNotePitch(next) == pitch && tick > cursor.getNoteTick(note))
                return tick;
        }

        return endTick;
    }
}
