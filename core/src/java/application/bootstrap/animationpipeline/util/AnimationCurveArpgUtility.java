package application.bootstrap.animationpipeline.util;

import application.bootstrap.animationpipeline.animation.AnimationChannel;
import engine.root.EngineSetting;
import engine.root.EngineUtility;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.mathematics.curves.CurveEnds;
import engine.util.mathematics.curves.CurveKeyMode;
import engine.util.mathematics.curves.CurveStruct;

public class AnimationCurveArpgUtility extends EngineUtility {

    /*
     * The one reading and writing of a bone track in a clip's ARPG, shared by
     * the builder and every editor that keys a clip. A track is an object of
     * channels (rotation, position, scale), each an object of the axes it
     * keys (x, y, z), each a list of keys in strictly increasing time: a
     * "time" and a "value", a "mode" when it is not auto, and for an aligned
     * or free key its "in" and "out" handles, each a moment and a value from
     * the key. A track's curves are held one slot per axis of each channel,
     * null for an axis it does not key. A malformed track is refused with a
     * clear reason, catchably, so an editor can show why.
     */

    // Read \\

    // Every curve of a bone's track, one slot per axis of each channel, null where it keys none
    public static CurveStruct[] readTrack(
            ArpgObjectStruct trackArpg,
            CurveEnds ends,
            String boneName,
            String sourceName) {

        CurveStruct[] curves = new CurveStruct[AnimationChannel.SLOT_COUNT];
        boolean keyed = false;

        for (String field : trackArpg.keySet()) {

            AnimationChannel channel = AnimationChannel.fromField(field);

            if (channel == null || !ArpgUtility.hasObject(trackArpg, field))
                throwException("Bone track \"" + boneName + "\" holds \"" + field
                        + "\", which is no channel, in " + sourceName);

            keyed |= readChannel(trackArpg.getAsObject(field), channel, ends, curves, boneName, sourceName);
        }

        if (!keyed)
            throwException("Bone track \"" + boneName + "\" keys no axis in " + sourceName);

        return curves;
    }

    private static boolean readChannel(
            ArpgObjectStruct channelArpg,
            AnimationChannel channel,
            CurveEnds ends,
            CurveStruct[] curves,
            String boneName,
            String sourceName) {

        boolean keyed = false;

        for (String axisName : channelArpg.keySet()) {

            int axis = findAxis(axisName);

            if (axis == EngineSetting.INDEX_NOT_FOUND || !ArpgUtility.hasArray(channelArpg, axisName))
                throwException("Bone track \"" + boneName + "\" keys \"" + axisName + "\" of its "
                        + channel.getField() + ", which is no axis, in " + sourceName);

            String curveName = boneName + " " + channel.getField() + " " + axisName;
            CurveStruct curve = new CurveStruct(ends);

            readCurve(channelArpg.getAsArray(axisName), curve, curveName, sourceName);
            curves[channel.toSlot(axis)] = curve;
            keyed = true;
        }

        return keyed;
    }

    // A curve's keys laid in order and its handles resolved; refused when empty or out of order
    public static void readCurve(ArpgArrayStruct keysArpg, CurveStruct curve, String curveName, String sourceName) {

        if (keysArpg.isEmpty())
            throwException("Curve \"" + curveName + "\" has no keys in " + sourceName);

        for (int i = 0; i < keysArpg.size(); i++)
            readKey(keysArpg.get(i).getAsObject(), curve);

        if (!curve.isOrdered())
            throwException("Curve \"" + curveName
                    + "\" is out of order — keys must be strictly increasing in time. Source: " + sourceName);

        curve.resolveHandles();
    }

    // A key read whole: its moment and value, its mode, and the handles an aligned or free key carries
    private static void readKey(ArpgObjectStruct keyArpg, CurveStruct curve) {

        float time = ArpgUtility.validateFloat(keyArpg, EngineSetting.ANIMATION_FIELD_TIME);

        ArpgUtility.validateFloat(keyArpg, EngineSetting.ANIMATION_FIELD_VALUE);

        double value = keyArpg.getAsValue(EngineSetting.ANIMATION_FIELD_VALUE).getAsDouble();
        CurveKeyMode mode = ArpgUtility.getEnum(
                keyArpg, EngineSetting.ANIMATION_FIELD_MODE, CurveKeyMode.class, CurveKeyMode.AUTO);

        if (!mode.holdsHandles()) {
            curve.appendKey(time, value, mode, 0f, 0.0, 0f, 0.0);
            return;
        }

        ArpgArrayStruct in = ArpgUtility.validateArray(
                keyArpg, EngineSetting.ANIMATION_FIELD_IN, EngineSetting.ANIMATION_HANDLE_LENGTH);
        ArpgArrayStruct out = ArpgUtility.validateArray(
                keyArpg, EngineSetting.ANIMATION_FIELD_OUT, EngineSetting.ANIMATION_HANDLE_LENGTH);

        curve.appendKey(
                time,
                value,
                mode,
                in.get(0).getAsFloat(),
                in.get(1).getAsDouble(),
                out.get(0).getAsFloat(),
                out.get(1).getAsDouble());
    }

    private static int findAxis(String axisName) {

        for (int i = 0; i < EngineSetting.ANIMATION_AXIS_NAMES.length; i++)
            if (EngineSetting.ANIMATION_AXIS_NAMES[i].equals(axisName))
                return i;

        return EngineSetting.INDEX_NOT_FOUND;
    }

    // Write \\

    // A bone's track as a clip's ARPG holds it: every channel that keys an axis, every axis that holds keys
    public static ArpgObjectStruct writeTrack(CurveStruct[] curves) {

        ArpgObjectStruct trackArpg = new ArpgObjectStruct();

        for (AnimationChannel channel : AnimationChannel.VALUES) {

            ArpgObjectStruct channelArpg = new ArpgObjectStruct();

            for (int axis = 0; axis < AnimationChannel.AXIS_COUNT; axis++) {

                CurveStruct curve = curves[channel.toSlot(axis)];

                if (curve != null && !curve.isEmpty())
                    channelArpg.add(EngineSetting.ANIMATION_AXIS_NAMES[axis], writeCurve(curve));
            }

            if (!channelArpg.isEmpty())
                trackArpg.add(channel.getField(), channelArpg);
        }

        return trackArpg;
    }

    public static ArpgArrayStruct writeCurve(CurveStruct curve) {

        ArpgArrayStruct keysArpg = new ArpgArrayStruct();

        for (int i = 0; i < curve.size(); i++)
            keysArpg.add(writeKey(curve, i));

        return keysArpg;
    }

    private static ArpgObjectStruct writeKey(CurveStruct curve, int index) {

        ArpgObjectStruct keyArpg = new ArpgObjectStruct();
        CurveKeyMode mode = curve.getMode(index);

        keyArpg.addProperty(EngineSetting.ANIMATION_FIELD_TIME, curve.getTime(index));
        keyArpg.addProperty(EngineSetting.ANIMATION_FIELD_VALUE, curve.getValue(index));

        if (mode != CurveKeyMode.AUTO)
            keyArpg.addProperty(EngineSetting.ANIMATION_FIELD_MODE, ArpgUtility.toEnumName(mode));

        if (!mode.holdsHandles())
            return keyArpg;

        keyArpg.add(EngineSetting.ANIMATION_FIELD_IN, writeHandle(curve.getInTime(index), curve.getInValue(index)));
        keyArpg.add(EngineSetting.ANIMATION_FIELD_OUT, writeHandle(curve.getOutTime(index), curve.getOutValue(index)));

        return keyArpg;
    }

    private static ArpgArrayStruct writeHandle(float time, double value) {

        ArpgArrayStruct handleArpg = new ArpgArrayStruct();

        handleArpg.add(time);
        handleArpg.add(value);

        return handleArpg;
    }

    // Utility \\

    // The latest key across every curve of a track, 0 for none
    public static float resolveEndTime(CurveStruct[] curves) {

        float endTime = 0f;

        for (int i = 0; i < curves.length; i++)
            if (curves[i] != null && !curves[i].isEmpty())
                endTime = Math.max(endTime, curves[i].getEndTime());

        return endTime;
    }

    // How a clip's curves run at their ends: round the seam while it loops, easing into its holds otherwise
    public static CurveEnds resolveEnds(boolean looping) {
        return looping ? CurveEnds.CYCLIC : CurveEnds.CLAMPED;
    }
}
