package application.bootstrap.soundpipeline.soundmanager;

import java.io.File;

import application.bootstrap.soundpipeline.sound.SoundCategory;
import application.bootstrap.soundpipeline.sound.SoundData;
import application.bootstrap.soundpipeline.sound.SoundHandle;
import application.bootstrap.soundpipeline.sound.SoundPcmStruct;
import application.bootstrap.soundpipeline.util.SoundDecodeUtility;
import engine.root.BuilderPackage;
import engine.root.EngineSetting;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.io.FileUtility;

class SoundBuilder extends BuilderPackage {

    /*
     * Decodes one sound file and wraps it in a SoundHandle. How it plays is
     * read from the companion ARPG file beside it, when it has one, every
     * field falling back to its EngineSetting default: "category" names what
     * it is, "volume" and "pitch" scale how loud and how high it plays, "loop"
     * plays it again and again until stopped, and "spatial" places it in the
     * world, downmixed to mono so the device can place it, heard at full
     * volume within "referenceDistance" blocks, fading at "rolloff" to no
     * quieter than it is at "maxDistance". "mono" downmixes a sound that is
     * not placed, as a surround or ambisonic recording needs, and "length"
     * keeps only its first seconds — a long recording looped from a short
     * stretch of it, its tail faded into its head across "loopCrossfade"
     * seconds so it loops with no seam. The samples go to the device and are
     * let go at once.
     */

    // Internal
    private SoundManager soundManager;

    // Base \\

    @Override
    protected void get() {
        this.soundManager = get(SoundManager.class);
    }

    // Build \\

    SoundHandle build(File file, File root) {

        String soundName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
        int soundID = soundManager.registerSoundName(soundName);
        File companion = ArpgUtility.resolveCompanionFile(file);
        ArpgObjectStruct arpg = companion.isFile() ? ArpgUtility.loadObject(companion) : new ArpgObjectStruct();

        SoundCategory category = ArpgUtility.toEnum(
                ArpgUtility.getString(arpg, "category", EngineSetting.DEFAULT_SOUND_CATEGORY), SoundCategory.class);
        float volume = parseRange(arpg, soundName, "volume", EngineSetting.DEFAULT_SOUND_VOLUME, 0f, 1f);
        float pitch = parsePositive(arpg, soundName, "pitch", EngineSetting.DEFAULT_SOUND_PITCH);
        boolean loop = ArpgUtility.getBoolean(arpg, "loop", EngineSetting.DEFAULT_SOUND_LOOP);
        boolean spatial = ArpgUtility.getBoolean(arpg, "spatial", EngineSetting.DEFAULT_SOUND_SPATIAL);
        float referenceDistance = parsePositive(
                arpg, soundName, "referenceDistance", EngineSetting.DEFAULT_SOUND_REFERENCE_DISTANCE);
        float maxDistance = parsePositive(arpg, soundName, "maxDistance", EngineSetting.DEFAULT_SOUND_MAX_DISTANCE);
        float rolloff = parseRange(
                arpg, soundName, "rolloff", EngineSetting.DEFAULT_SOUND_ROLLOFF, 0f, Float.MAX_VALUE);

        boolean mono = ArpgUtility.getBoolean(arpg, "mono", EngineSetting.DEFAULT_SOUND_MONO);
        float keepSeconds = parseRange(
                arpg, soundName, "length", EngineSetting.DEFAULT_SOUND_LENGTH, 0f, Float.MAX_VALUE);
        float loopCrossfadeSeconds = loop ? parseRange(arpg, soundName, "loopCrossfade",
                EngineSetting.DEFAULT_SOUND_LOOP_CROSSFADE, 0f, Float.MAX_VALUE) : 0f;

        if (maxDistance < referenceDistance)
            throwException("Sound \"" + soundName + "\" field \"maxDistance\" (" + maxDistance
                    + ") must be at least its \"referenceDistance\" (" + referenceDistance + ").");

        SoundPcmStruct pcm = SoundDecodeUtility.decode(file, EngineSetting.SOUND_MAX_SAMPLE_RATE, spatial || mono,
                keepSeconds, loopCrossfadeSeconds);

        SoundData soundData = new SoundData(
                soundName,
                soundID,
                file,
                soundManager.createBuffer(pcm),
                pcm.getChannels(),
                pcm.getSampleRate(),
                pcm.getFrameCount(),
                keepSeconds,
                loopCrossfadeSeconds,
                category,
                volume,
                pitch,
                loop,
                spatial,
                referenceDistance,
                maxDistance,
                rolloff);

        SoundHandle soundHandle = create(SoundHandle.class);
        soundHandle.constructor(soundData);

        return soundHandle;
    }

    // Parsing \\

    private float parseRange(
            ArpgObjectStruct arpg,
            String soundName,
            String field,
            float fallback,
            float minimum,
            float maximum) {

        float value = ArpgUtility.getFloat(arpg, field, fallback);

        if (value < minimum || value > maximum)
            throwException("Sound \"" + soundName + "\" field \"" + field + "\" must be between " + minimum
                    + " and " + maximum + ", got: " + value);

        return value;
    }

    private float parsePositive(ArpgObjectStruct arpg, String soundName, String field, float fallback) {

        float value = ArpgUtility.getFloat(arpg, field, fallback);

        if (value <= 0f)
            throwException("Sound \"" + soundName + "\" field \"" + field + "\" must be greater than 0, got: "
                    + value);

        return value;
    }
}
