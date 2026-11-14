package editor.bootstrap.recorderpipeline.util;

import application.bootstrap.worldpipeline.util.WorldPositionStruct;
import application.bootstrap.worldpipeline.util.WorldPositionUtility;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import editor.bootstrap.recorderpipeline.recording.RecordingTrackStruct;
import editor.runtime.EditorSetting;
import engine.root.EngineSetting;
import engine.root.EngineUtility;
import engine.util.mathematics.extras.Coordinate2Long;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.floats.FloatArrayList;

public class RecordingSampleUtility extends EngineUtility {

    /*
     * Reads a recording's tracks between their samples. findSample() finds the
     * sample at or before a moment by binary search, resolveBlend() how far
     * the moment stands toward the next, and isPresent() whether the moment
     * lies on the track at all — samples further apart than a gap are never
     * blended, so a thing that left and came back does not slide across the
     * gap. Positions blend in the frame of the earlier sample's chunk, across
     * the world's wrap, and settle into the chunk they land in; angles blend
     * the short way round. shiftTimes() moves a column of times along. None
     * of it allocates.
     */

    // Samples \\

    // The last sample at or before the time, -1 before the first
    public static int findSample(FloatArrayList times, float time) {

        int low = 0;
        int high = times.size() - 1;
        int found = EngineSetting.INDEX_NOT_FOUND;

        while (low <= high) {

            int middle = (low + high) >>> 1;

            if (times.getFloat(middle) <= time) {
                found = middle;
                low = middle + 1;
            }

            else
                high = middle - 1;
        }

        return found;
    }

    // How far the time stands from the sample toward the next, 0 to 1, and 0 past the last
    public static float resolveBlend(FloatArrayList times, int sample, float time) {

        if (sample < 0 || sample >= times.size() - 1)
            return 0f;

        float from = times.getFloat(sample);
        float to = times.getFloat(sample + 1);

        return to > from ? Math.clamp((time - from) / (to - from), 0f, 1f) : 0f;
    }

    // True when the time lies on the track — between two samples no further apart than a gap, or within a gap of one
    public static boolean isPresent(FloatArrayList times, int sample, float time) {

        if (sample < 0)
            return false;

        if (sample < times.size() - 1
                && times.getFloat(sample + 1) - times.getFloat(sample) <= EditorSetting.RECORDER_GAP_SECONDS)
            return true;

        return time - times.getFloat(sample) <= EditorSetting.RECORDER_GAP_SECONDS;
    }

    // Moves every time in the column along by the same seconds
    public static void shiftTimes(FloatArrayList times, float seconds) {
        for (int i = 0; i < times.size(); i++)
            times.set(i, times.getFloat(i) + seconds);
    }

    // Values \\

    public static float blend(FloatArrayList values, int sample, float blend) {

        float from = values.getFloat(sample);

        if (sample + 1 >= values.size())
            return from;

        return from + (values.getFloat(sample + 1) - from) * blend;
    }

    public static double blend(DoubleArrayList values, int sample, float blend) {

        double from = values.getDouble(sample);

        if (sample + 1 >= values.size())
            return from;

        return from + (values.getDouble(sample + 1) - from) * blend;
    }

    // Blends degrees the short way round
    public static float blendDegrees(FloatArrayList values, int sample, float blend) {

        float from = values.getFloat(sample);

        if (sample + 1 >= values.size())
            return from;

        float delta = values.getFloat(sample + 1) - from;
        delta -= EditorSetting.RECORDER_FULL_TURN_DEGREES
                * Math.round(delta / EditorSetting.RECORDER_FULL_TURN_DEGREES);

        return from + delta * blend;
    }

    // Eases 0 to 1 in and out
    public static float smoothStep(float value) {

        float clamped = Math.clamp(value, 0f, 1f);

        return clamped * clamped * (3f - 2f * clamped);
    }

    // Place \\

    // The track's place at the sample blended toward the next, settled into the chunk it lies in
    public static void resolvePlace(
            WorldHandle worldHandle,
            RecordingTrackStruct track,
            int sample,
            float blend,
            WorldPositionStruct out) {

        long chunkCoordinate = track.getChunkCoordinate(sample);
        float x = track.getPositionX(sample);
        float y = track.getPositionY(sample);
        float z = track.getPositionZ(sample);

        if (blend > 0f && sample + 1 < track.size()) {

            long delta = WorldWrapUtility.unwrapToGridCoordinate(
                    worldHandle,
                    chunkCoordinate,
                    track.getChunkCoordinate(sample + 1));
            float toX = track.getPositionX(sample + 1) + Coordinate2Long.unpackX(delta) * EngineSetting.CHUNK_SIZE;
            float toZ = track.getPositionZ(sample + 1) + Coordinate2Long.unpackY(delta) * EngineSetting.CHUNK_SIZE;

            x += (toX - x) * blend;
            y += (track.getPositionY(sample + 1) - y) * blend;
            z += (toZ - z) * blend;
        }

        out.getPosition().set(x, y, z);
        out.setChunkCoordinate(chunkCoordinate);
        WorldPositionUtility.settleChunk(worldHandle, out);
    }
}
