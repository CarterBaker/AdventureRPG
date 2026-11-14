package application.bootstrap.musicpipeline.notesetmanager;

import java.io.File;

import application.bootstrap.musicpipeline.noteset.NoteEnvelopeStruct;
import application.bootstrap.musicpipeline.noteset.NoteLoopMode;
import application.bootstrap.musicpipeline.noteset.NoteRegionStruct;
import application.bootstrap.musicpipeline.noteset.NoteSetData;
import application.bootstrap.musicpipeline.noteset.NoteSetHandle;
import application.bootstrap.musicpipeline.util.NoteUtility;
import application.bootstrap.soundpipeline.soundmanager.SoundManager;
import engine.root.BuilderPackage;
import engine.root.EngineSetting;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;

class NoteSetBuilder extends BuilderPackage {

    /*
     * Parses one note set ARPG file into a NoteSetData and wraps it in a
     * NoteSetHandle, under the short note set ID NoteSetManager assigns its
     * name. "regions" lists at least one region, in order of its "low" pitch:
     * the "sound" it plays, which must stand under the sound directory, the
     * "root" pitch it was recorded at, the "low" to "high" pitches it covers,
     * each no more than NOTE_SET_MAX_TRANSPOSE steps from its root, the
     * "velocity_low" to "velocity_high" it answers within 1 to 127, its
     * "round_robin" turn of "round_robin_count", its "tune" in cents, its
     * "volume" in decibels and its "loop_mode". Two regions that sound for
     * one pitch and velocity share a round robin count and take different
     * turns of it, so every note finds one region. "envelope" sets the
     * "attack", "hold", "decay", "sustain" level and "release" every note
     * plays with, and "velocity_tracking" how strongly velocity sets
     * loudness. The per-pitch index findRegion walks is built here, once.
     */

    // Internal
    private NoteSetManager noteSetManager;
    private SoundManager soundManager;

    // Base \\

    @Override
    protected void get() {

        // Internal
        this.noteSetManager = get(NoteSetManager.class);
        this.soundManager = get(SoundManager.class);
    }

    // Build \\

    NoteSetHandle build(File file, String noteSetName) {

        ArpgObjectStruct arpg = ArpgUtility.loadObject(file);
        short noteSetID = noteSetManager.registerNoteSetName(noteSetName);
        String owner = "Note set \"" + noteSetName + "\"";

        NoteRegionStruct[] regions = parseRegions(ArpgUtility.validateArray(arpg, "regions"), owner, file);
        NoteEnvelopeStruct envelope = parseEnvelope(arpg, owner, file);
        float velocityTracking = parseRange(arpg, "velocity_tracking",
                EngineSetting.DEFAULT_NOTE_VELOCITY_TRACKING, 0f, 1f, owner, file);

        validateOverlaps(regions, owner, file);

        int[] pitch2RegionStart = new int[EngineSetting.NOTE_PITCH_COUNT + 1];
        int[] regionIndices = buildIndex(regions, pitch2RegionStart);

        NoteSetData noteSetData = new NoteSetData(
                noteSetName,
                noteSetID,
                regions,
                pitch2RegionStart,
                regionIndices,
                envelope,
                velocityTracking,
                findLow(regions),
                findHigh(regions));

        NoteSetHandle handle = create(NoteSetHandle.class);
        handle.constructor(noteSetData);

        return handle;
    }

    // Region Parsing \\

    private NoteRegionStruct[] parseRegions(ArpgArrayStruct regionsArpg, String owner, File file) {

        if (regionsArpg.size() == 0)
            throwException(owner + " has no regions. File: " + file.getName());

        NoteRegionStruct[] regions = new NoteRegionStruct[regionsArpg.size()];

        for (int i = 0; i < regions.length; i++) {

            regions[i] = parseRegion(regionsArpg.get(i).getAsObject(), owner + " region " + i, file);

            if (i > 0 && regions[i].getLow() < regions[i - 1].getLow())
                throwException(owner + " lists region " + i + " out of order — regions run from the lowest "
                        + "\"low\" up. File: " + file.getName());
        }

        return regions;
    }

    private NoteRegionStruct parseRegion(ArpgObjectStruct regionArpg, String owner, File file) {

        String soundName = ArpgUtility.validateString(regionArpg, "sound");
        int root = parsePitch(regionArpg, "root", ArpgUtility.validateInt(regionArpg, "root"), owner, file);
        int low = parsePitch(regionArpg, "low", root, owner, file);
        int high = parsePitch(regionArpg, "high", root, owner, file);
        int velocityLow = parseVelocity(regionArpg, "velocity_low", EngineSetting.NOTE_VELOCITY_MIN, owner, file);
        int velocityHigh = parseVelocity(regionArpg, "velocity_high", EngineSetting.NOTE_VELOCITY_MAX, owner, file);
        int roundRobin = ArpgUtility.getInt(regionArpg, "round_robin", EngineSetting.DEFAULT_NOTE_ROUND_ROBIN);
        int roundRobinCount = ArpgUtility.getInt(
                regionArpg, "round_robin_count", EngineSetting.DEFAULT_NOTE_ROUND_ROBIN_COUNT);
        float tune = parseRange(regionArpg, "tune", EngineSetting.DEFAULT_NOTE_TUNE,
                -EngineSetting.NOTE_SET_MAX_TUNE, EngineSetting.NOTE_SET_MAX_TUNE, owner, file);
        float volume = parseRange(regionArpg, "volume", EngineSetting.DEFAULT_NOTE_VOLUME,
                EngineSetting.NOTE_SET_MIN_VOLUME, EngineSetting.NOTE_SET_MAX_VOLUME, owner, file);
        NoteLoopMode loopMode = ArpgUtility.getEnum(regionArpg, "loop_mode", NoteLoopMode.class, NoteLoopMode.NONE);

        if (low > high)
            throwException(owner + " covers from \"low\" " + low + " above its \"high\" " + high + ". File: "
                    + file.getName());

        if (root - low > EngineSetting.NOTE_SET_MAX_TRANSPOSE || high - root > EngineSetting.NOTE_SET_MAX_TRANSPOSE)
            throwException(owner + " stretches its sample more than " + EngineSetting.NOTE_SET_MAX_TRANSPOSE
                    + " steps from its \"root\" " + root + " (covers " + low + " to " + high + "). File: "
                    + file.getName());

        if (velocityLow > velocityHigh)
            throwException(owner + " answers from \"velocity_low\" " + velocityLow + " above its \"velocity_high\" "
                    + velocityHigh + ". File: " + file.getName());

        if (roundRobinCount < 1 || roundRobin < 0 || roundRobin >= roundRobinCount)
            throwException(owner + " takes \"round_robin\" " + roundRobin + " of a \"round_robin_count\" of "
                    + roundRobinCount + " — the turn runs from 0 to one less than the count. File: "
                    + file.getName());

        if (!soundManager.hasSoundFile(soundName))
            throwException(owner + " plays sound \"" + soundName + "\", which is not under '"
                    + EngineSetting.SOUND_PATH + "'. File: " + file.getName());

        return new NoteRegionStruct(
                soundName,
                root,
                low,
                high,
                velocityLow,
                velocityHigh,
                roundRobin,
                roundRobinCount,
                tune,
                volume,
                NoteUtility.toGain(volume),
                loopMode);
    }

    private int parsePitch(ArpgObjectStruct arpg, String field, int fallback, String owner, File file) {

        int pitch = ArpgUtility.getInt(arpg, field, fallback);

        if (pitch < EngineSetting.NOTE_PITCH_MIN || pitch > EngineSetting.NOTE_PITCH_MAX)
            throwException(owner + " field \"" + field + "\" must be a pitch from " + EngineSetting.NOTE_PITCH_MIN
                    + " to " + EngineSetting.NOTE_PITCH_MAX + ", got: " + pitch + ". File: " + file.getName());

        return pitch;
    }

    private int parseVelocity(ArpgObjectStruct arpg, String field, int fallback, String owner, File file) {

        int velocity = ArpgUtility.getInt(arpg, field, fallback);

        if (velocity < EngineSetting.NOTE_VELOCITY_MIN || velocity > EngineSetting.NOTE_VELOCITY_MAX)
            throwException(owner + " field \"" + field + "\" must be a velocity from "
                    + EngineSetting.NOTE_VELOCITY_MIN + " to " + EngineSetting.NOTE_VELOCITY_MAX + ", got: "
                    + velocity + ". File: " + file.getName());

        return velocity;
    }

    // Envelope Parsing \\

    private NoteEnvelopeStruct parseEnvelope(ArpgObjectStruct arpg, String owner, File file) {

        ArpgObjectStruct envelopeArpg = ArpgUtility.hasObject(arpg, "envelope")
                ? arpg.getAsObject("envelope")
                : new ArpgObjectStruct();
        String envelopeOwner = owner + " envelope";

        return new NoteEnvelopeStruct(
                parseSeconds(envelopeArpg, "attack", EngineSetting.DEFAULT_NOTE_ATTACK, envelopeOwner, file),
                parseSeconds(envelopeArpg, "hold", EngineSetting.DEFAULT_NOTE_HOLD, envelopeOwner, file),
                parseSeconds(envelopeArpg, "decay", EngineSetting.DEFAULT_NOTE_DECAY, envelopeOwner, file),
                parseRange(envelopeArpg, "sustain", EngineSetting.DEFAULT_NOTE_SUSTAIN, 0f, 1f, envelopeOwner, file),
                parseSeconds(envelopeArpg, "release", EngineSetting.DEFAULT_NOTE_RELEASE, envelopeOwner, file));
    }

    private float parseSeconds(ArpgObjectStruct arpg, String field, float fallback, String owner, File file) {
        return parseRange(arpg, field, fallback, 0f, EngineSetting.NOTE_ENVELOPE_MAX_SECONDS, owner, file);
    }

    // Validation \\

    // Two regions sounding for one pitch and velocity take different turns of one round robin
    private void validateOverlaps(NoteRegionStruct[] regions, String owner, File file) {

        for (int i = 0; i < regions.length; i++) {

            NoteRegionStruct first = regions[i];

            for (int j = i + 1; j < regions.length && regions[j].getLow() <= first.getHigh(); j++) {

                NoteRegionStruct second = regions[j];

                if (second.getVelocityLow() > first.getVelocityHigh()
                        || first.getVelocityLow() > second.getVelocityHigh())
                    continue;

                if (first.getRoundRobinCount() != second.getRoundRobinCount()
                        || first.getRoundRobin() == second.getRoundRobin())
                    throwException(owner + " regions " + i + " and " + j + " both sound for pitch "
                            + second.getLow() + " at one velocity; regions that share a pitch and velocity take "
                            + "different turns of one round robin. File: " + file.getName());
            }
        }
    }

    // Index \\

    // Every pitch's regions in velocity order, laid end to end, with where each pitch's run starts
    private int[] buildIndex(NoteRegionStruct[] regions, int[] pitch2RegionStart) {

        int[] counts = new int[EngineSetting.NOTE_PITCH_COUNT];
        int total = 0;

        for (int i = 0; i < regions.length; i++)
            for (int pitch = regions[i].getLow(); pitch <= regions[i].getHigh(); pitch++) {
                counts[pitch]++;
                total++;
            }

        for (int pitch = 0; pitch < EngineSetting.NOTE_PITCH_COUNT; pitch++)
            pitch2RegionStart[pitch + 1] = pitch2RegionStart[pitch] + counts[pitch];

        int[] regionIndices = new int[total];
        int[] filled = new int[EngineSetting.NOTE_PITCH_COUNT];

        for (int i = 0; i < regions.length; i++)
            for (int pitch = regions[i].getLow(); pitch <= regions[i].getHigh(); pitch++)
                insertByVelocity(regions, regionIndices, pitch2RegionStart[pitch], filled[pitch]++, i);

        return regionIndices;
    }

    // The region placed into its pitch's run so the run stays in order of the velocity each region starts at
    private void insertByVelocity(NoteRegionStruct[] regions, int[] regionIndices, int start, int count, int region) {

        int at = start + count;

        while (at > start && regions[regionIndices[at - 1]].getVelocityLow() > regions[region].getVelocityLow()) {
            regionIndices[at] = regionIndices[at - 1];
            at--;
        }

        regionIndices[at] = region;
    }

    // Utility \\

    private int findLow(NoteRegionStruct[] regions) {

        int low = EngineSetting.NOTE_PITCH_MAX;

        for (int i = 0; i < regions.length; i++)
            low = Math.min(low, regions[i].getLow());

        return low;
    }

    private int findHigh(NoteRegionStruct[] regions) {

        int high = EngineSetting.NOTE_PITCH_MIN;

        for (int i = 0; i < regions.length; i++)
            high = Math.max(high, regions[i].getHigh());

        return high;
    }

    private float parseRange(
            ArpgObjectStruct arpg,
            String field,
            float fallback,
            float minimum,
            float maximum,
            String owner,
            File file) {

        float value = ArpgUtility.getFloat(arpg, field, fallback);

        if (value < minimum || value > maximum)
            throwException(owner + " field \"" + field + "\" must be between " + minimum + " and " + maximum
                    + ", got: " + value + ". File: " + file.getName());

        return value;
    }
}
