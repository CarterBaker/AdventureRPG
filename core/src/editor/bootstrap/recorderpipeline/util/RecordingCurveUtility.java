package editor.bootstrap.recorderpipeline.util;

import java.util.Locale;

import application.bootstrap.calendarpipeline.calendar.CalendarHandle;
import application.bootstrap.worldpipeline.util.WorldPositionStruct;
import application.bootstrap.worldpipeline.util.WorldPositionUtility;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import editor.bootstrap.recorderpipeline.recording.RecorderCurveKind;
import editor.bootstrap.recorderpipeline.recording.RecorderCurveProperty;
import editor.bootstrap.recorderpipeline.recording.RecordingCameraTrackStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingCurveTrackStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingTrackStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingVehicleTrackStruct;
import editor.runtime.EditorSetting;
import engine.root.EngineSetting;
import engine.root.EngineUtility;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.mathematics.quaternions.Quaternion;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.floats.FloatArrayList;

public class RecordingCurveUtility extends EngineUtility {

    /*
     * Reads a track's values the way its curves carry them, and plays each
     * value from its curve wherever the track has one, from its samples
     * wherever it does not. Positions are read across chunks from the chunk
     * a track began in, so a curve of one runs on unbroken; the camera's look
     * is a yaw and a pitch, and the turn of items and vehicles a yaw, pitch
     * and roll as Quaternion.setFromEulerAngles() builds them. An angle that
     * turns round is played the short way, whichever way its curve wound.
     * Values are written and read back as the Keys tab shows them, the
     * clock as the day and the time of day it stands on.
     */

    // Angles \\

    public static double toDirectionYaw(float directionX, float directionZ) {
        return Math.toDegrees(Math.atan2(directionX, directionZ));
    }

    public static double toDirectionPitch(float directionX, float directionY, float directionZ) {

        double length = Math.sqrt(directionX * directionX + directionY * directionY + directionZ * directionZ);

        return length > 0.0 ? Math.toDegrees(Math.asin(Math.clamp(directionY / length, -1.0, 1.0))) : 0.0;
    }

    public static Vector3 toDirection(double yaw, double pitch, Vector3 out) {

        double yawRadians = Math.toRadians(yaw);
        double pitchRadians = Math.toRadians(pitch);
        double flat = Math.cos(pitchRadians);

        return out.set(
                (float) (Math.sin(yawRadians) * flat),
                (float) Math.sin(pitchRadians),
                (float) (Math.cos(yawRadians) * flat));
    }

    // The yaw, pitch or roll of a turn, undoing setFromEulerAngles(): a yaw about the up axis, then a pitch, then
    // a roll
    public static double toEulerAngle(RecorderCurveProperty property, float w, float x, float y, float z) {
        return switch (property) {
            case YAW -> Math.toDegrees(Math.atan2(2.0 * (x * z + w * y), 1.0 - 2.0 * (x * x + y * y)));
            case PITCH -> Math.toDegrees(Math.asin(Math.clamp(-2.0 * (y * z - w * x), -1.0, 1.0)));
            case ROLL -> Math.toDegrees(Math.atan2(2.0 * (x * y + w * z), 1.0 - 2.0 * (x * x + z * z)));
            default -> throwException("A turn holds no " + property.getLabel() + ".");
        };
    }

    // An angle brought within half a turn either side of zero
    public static double wrapDegrees(double degrees) {
        return degrees - EditorSetting.RECORDER_FULL_TURN_DEGREES
                * Math.round(degrees / EditorSetting.RECORDER_FULL_TURN_DEGREES);
    }

    // Values \\

    public static String formatValue(RecorderCurveKind kind, double value, CalendarHandle calendar) {
        return switch (kind) {
            case DISTANCE -> String.format(Locale.ROOT, EditorSetting.RECORDER_VALUE_FORMAT_DISTANCE, value);
            case ANGLE, DEGREES -> String.format(Locale.ROOT, EditorSetting.RECORDER_VALUE_FORMAT_DEGREES, value);
            case SCALAR -> String.format(Locale.ROOT, EditorSetting.RECORDER_VALUE_FORMAT_SCALAR, value);
            case CLOCK -> formatClock(EditorSetting.RECORDER_VALUE_FORMAT_CLOCK, value, calendar);
        };
    }

    // A value written so it reads back unchanged, as the value dialog starts from
    public static String formatInput(RecorderCurveKind kind, double value, CalendarHandle calendar) {
        return kind == RecorderCurveKind.CLOCK
                ? formatClock(EditorSetting.RECORDER_VALUE_FORMAT_CLOCK_INPUT, value, calendar)
                : formatValue(kind, value, calendar);
    }

    private static String formatClock(String format, double elapsedMillis, CalendarHandle calendar) {

        long millisPerDay = resolveMillisPerDay(calendar);
        int minutesPerDay = resolveMinutesPerDay(calendar);
        long sinceStart = Math.round(elapsedMillis) + resolveStartMillis(calendar, millisPerDay, minutesPerDay);
        int minute = (int) (Math.floorMod(sinceStart, millisPerDay) * minutesPerDay / millisPerDay);

        return String.format(Locale.ROOT, format, Math.floorDiv(sinceStart, millisPerDay),
                minute / calendar.getMinutesPerHour(), minute % calendar.getMinutesPerHour());
    }

    // The value written back; the clock as a day and a time of day, or a time of day alone on the day it already
    // stands on; NaN when it cannot be read
    public static double parseValue(RecorderCurveKind kind, String text, double current, CalendarHandle calendar) {

        try {
            return kind == RecorderCurveKind.CLOCK
                    ? parseClock(text.trim(), current, calendar)
                    : Double.parseDouble(text.trim());
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }

    private static double parseClock(String text, double current, CalendarHandle calendar) {

        long millisPerDay = resolveMillisPerDay(calendar);
        int minutesPerDay = resolveMinutesPerDay(calendar);
        long startMillis = resolveStartMillis(calendar, millisPerDay, minutesPerDay);
        String[] words = text.split(EditorSetting.RECORDER_VALUE_WORD_PATTERN);
        boolean timeOnly = words.length == 1 && words[0].contains(EditorSetting.RECORDER_VALUE_CLOCK_SEPARATOR);

        if (words.length < 1 || words.length > 2 || words[0].isEmpty())
            return Double.NaN;

        long day = timeOnly
                ? Math.floorDiv(Math.round(current) + startMillis, millisPerDay)
                : Long.parseLong(words[0]);
        String time = timeOnly ? words[0] : words.length == 2 ? words[1] : null;
        int minuteOfDay = 0;

        if (time != null) {

            String[] parts = time.split(EditorSetting.RECORDER_VALUE_CLOCK_SEPARATOR);

            if (parts.length != 2)
                return Double.NaN;

            int hour = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);

            if (hour < 0 || hour >= calendar.getHoursPerDay() || minute < 0 || minute >= calendar.getMinutesPerHour())
                return Double.NaN;

            minuteOfDay = hour * calendar.getMinutesPerHour() + minute;
        }

        if (day < 0)
            return Double.NaN;

        return day * millisPerDay + Math.ceilDiv(minuteOfDay * millisPerDay, minutesPerDay) - startMillis;
    }

    private static long resolveMillisPerDay(CalendarHandle calendar) {
        return EngineSetting.MILLIS_PER_REAL_DAY / calendar.getDaysPerDay();
    }

    private static int resolveMinutesPerDay(CalendarHandle calendar) {
        return calendar.getHoursPerDay() * calendar.getMinutesPerHour();
    }

    // How far into its first day the calendar's clock starts, as the clock itself reckons it
    private static long resolveStartMillis(CalendarHandle calendar, long millisPerDay, int minutesPerDay) {
        return Math.ceilDiv(
                (calendar.getStartHour() * calendar.getMinutesPerHour() + calendar.getStartMinute()) * millisPerDay,
                minutesPerDay);
    }

    // Read \\

    // What the slot held at the sample as a curve carries it, a position read across chunks from the track's first
    // sample; without the world it was taken in, chunks are counted as they lie
    public static double readSample(RecordingCurveTrackStruct track, int slot, int sample, WorldHandle worldHandle) {

        RecorderCurveProperty property = track.getCurveProperty(slot);

        if (!property.isPosition() || !(track instanceof RecordingTrackStruct placed))
            return track.readCurveSample(slot, sample);

        if (property == RecorderCurveProperty.POSITION_Y)
            return placed.getPositionY(sample);

        long delta = resolveChunkDelta(worldHandle, placed.getChunkCoordinate(0), placed.getChunkCoordinate(sample));

        return property == RecorderCurveProperty.POSITION_X
                ? placed.getPositionX(sample) + (double) Coordinate2Long.unpackX(delta) * EngineSetting.CHUNK_SIZE
                : placed.getPositionZ(sample) + (double) Coordinate2Long.unpackY(delta) * EngineSetting.CHUNK_SIZE;
    }

    private static long resolveChunkDelta(WorldHandle worldHandle, long fromChunk, long toChunk) {
        return worldHandle != null
                ? WorldWrapUtility.unwrapToGridCoordinate(worldHandle, fromChunk, toChunk)
                : Coordinate2Long.subtract(toChunk, fromChunk);
    }

    // Play \\

    // The slot's curve at the moment of the take when it has one, else what its samples give
    private static double resolve(RecordingCurveTrackStruct track, int slot, float takeTime, double recorded) {

        if (!track.hasCurve(slot))
            return recorded;

        double value = track.getCurve(slot).evaluate(takeTime);

        return track.getCurveProperty(slot).getKind().isWrapping() ? wrapDegrees(value) : value;
    }

    public static float resolveFloat(
            RecordingCurveTrackStruct track,
            RecorderCurveProperty property,
            float takeTime,
            FloatArrayList column,
            int sample,
            float blend) {

        int slot = track.findCurveSlot(property);

        if (track.hasCurve(slot))
            return (float) resolve(track, slot, takeTime, 0.0);

        return property.getKind().isWrapping()
                ? RecordingSampleUtility.blendDegrees(column, sample, blend)
                : RecordingSampleUtility.blend(column, sample, blend);
    }

    public static double resolveDouble(
            RecordingCurveTrackStruct track,
            RecorderCurveProperty property,
            float takeTime,
            DoubleArrayList column,
            int sample,
            float blend) {

        int slot = track.findCurveSlot(property);

        return track.hasCurve(slot)
                ? resolve(track, slot, takeTime, 0.0)
                : RecordingSampleUtility.blend(column, sample, blend);
    }

    // One mast's brace, sail's hoist or door's opening
    public static float resolvePart(
            RecordingVehicleTrackStruct track,
            RecorderCurveProperty property,
            int part,
            float takeTime,
            FloatArrayList column,
            int partCount,
            int sample,
            float blend) {

        int slot = track.findCurveSlot(property, part);

        return track.hasCurve(slot)
                ? (float) resolve(track, slot, takeTime, 0.0)
                : track.blendPart(column, partCount, part, sample, blend);
    }

    // The track's place at the moment, each axis from its curve where it has one, settled into the chunk it lies in
    public static void resolvePlace(
            WorldHandle worldHandle,
            RecordingTrackStruct track,
            float takeTime,
            int sample,
            float blend,
            WorldPositionStruct out) {

        int slotX = track.findCurveSlot(RecorderCurveProperty.POSITION_X);
        int slotY = track.findCurveSlot(RecorderCurveProperty.POSITION_Y);
        int slotZ = track.findCurveSlot(RecorderCurveProperty.POSITION_Z);

        if (!track.hasCurve(slotX) && !track.hasCurve(slotY) && !track.hasCurve(slotZ)) {
            RecordingSampleUtility.resolvePlace(worldHandle, track, sample, blend, out);
            return;
        }

        out.getPosition().set(
                (float) resolve(track, slotX, takeTime, blendSample(track, slotX, sample, blend, worldHandle)),
                (float) resolve(track, slotY, takeTime, blendSample(track, slotY, sample, blend, worldHandle)),
                (float) resolve(track, slotZ, takeTime, blendSample(track, slotZ, sample, blend, worldHandle)));
        out.setChunkCoordinate(track.getChunkCoordinate(0));
        WorldPositionUtility.settleChunk(worldHandle, out);
    }

    // A slot's value between a sample and the next as a curve carries it
    private static double blendSample(
            RecordingCurveTrackStruct track,
            int slot,
            int sample,
            float blend,
            WorldHandle worldHandle) {

        double from = readSample(track, slot, sample, worldHandle);

        if (blend <= 0f || sample + 1 >= track.size())
            return from;

        return from + (readSample(track, slot, sample + 1, worldHandle) - from) * blend;
    }

    // A turn at the moment: as its samples blend when no part of it has a curve, else rebuilt from its yaw, pitch
    // and roll, each from its curve where it has one
    public static Quaternion resolveOrientation(
            RecordingCurveTrackStruct track,
            float takeTime,
            FloatArrayList columnW,
            FloatArrayList columnX,
            FloatArrayList columnY,
            FloatArrayList columnZ,
            int sample,
            float blend,
            Quaternion out) {

        out.set(
                RecordingSampleUtility.blend(columnW, sample, blend),
                RecordingSampleUtility.blend(columnX, sample, blend),
                RecordingSampleUtility.blend(columnY, sample, blend),
                RecordingSampleUtility.blend(columnZ, sample, blend)).normalize();

        int yawSlot = track.findCurveSlot(RecorderCurveProperty.YAW);
        int pitchSlot = track.findCurveSlot(RecorderCurveProperty.PITCH);
        int rollSlot = track.findCurveSlot(RecorderCurveProperty.ROLL);

        if (!track.hasCurve(yawSlot) && !track.hasCurve(pitchSlot) && !track.hasCurve(rollSlot))
            return out;

        float w = out.getW();
        float x = out.getX();
        float y = out.getY();
        float z = out.getZ();

        return out.setFromEulerAngles(
                (float) resolve(track, yawSlot, takeTime,
                        toEulerAngle(RecorderCurveProperty.YAW, w, x, y, z)),
                (float) resolve(track, pitchSlot, takeTime,
                        toEulerAngle(RecorderCurveProperty.PITCH, w, x, y, z)),
                (float) resolve(track, rollSlot, takeTime,
                        toEulerAngle(RecorderCurveProperty.ROLL, w, x, y, z)));
    }

    // The camera's look at the moment: as its samples blend when neither its yaw nor its pitch has a curve, else
    // rebuilt from both
    public static Vector3 resolveDirection(
            RecordingCameraTrackStruct track,
            float takeTime,
            int sample,
            float blend,
            Vector3 out) {

        out.set(
                RecordingSampleUtility.blend(track.getDirectionsX(), sample, blend),
                RecordingSampleUtility.blend(track.getDirectionsY(), sample, blend),
                RecordingSampleUtility.blend(track.getDirectionsZ(), sample, blend));

        int yawSlot = track.findCurveSlot(RecorderCurveProperty.YAW);
        int pitchSlot = track.findCurveSlot(RecorderCurveProperty.PITCH);

        if (!track.hasCurve(yawSlot) && !track.hasCurve(pitchSlot))
            return out;

        return toDirection(
                resolve(track, yawSlot, takeTime, toDirectionYaw(out.x, out.z)),
                resolve(track, pitchSlot, takeTime, toDirectionPitch(out.x, out.y, out.z)),
                out);
    }
}
