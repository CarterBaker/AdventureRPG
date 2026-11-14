package editor.bootstrap.recorderpipeline.recording;

import application.bootstrap.vehiclepipeline.vehicle.VehicleInstance;
import editor.bootstrap.recorderpipeline.util.RecordingCurveUtility;
import editor.runtime.EditorSetting;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;

public class RecordingVehicleTrackStruct extends RecordingTrackStruct {

    /*
     * One vehicle in a take: the type it was, how many masts, sails and doors
     * that type had when it was taken, and, sample by sample, how it lay and
     * how its rig stood — its turn, its rudder, whether it rode at anchor,
     * and every mast's brace, sail's hoist and door's opening, each kept in a
     * column of its own that holds one value per mast, sail or door per
     * sample. Its turn is carried on curves as a yaw, a pitch and a roll,
     * and each mast, sail and door has a curve slot of its own after the
     * rudder's.
     */

    // Identity
    private final String vehicleName;
    private final int mastCount;
    private final int sailCount;
    private final int doorCount;

    // Samples
    private final FloatArrayList orientationsW;
    private final FloatArrayList orientationsX;
    private final FloatArrayList orientationsY;
    private final FloatArrayList orientationsZ;
    private final FloatArrayList rudderAngles;
    private final IntArrayList anchored;
    private final FloatArrayList braceAngles;
    private final FloatArrayList sailHoists;
    private final FloatArrayList doorOpenings;

    // Constructor \\

    public RecordingVehicleTrackStruct(String vehicleName, int mastCount, int sailCount, int doorCount) {

        // Identity
        this.vehicleName = vehicleName;
        this.mastCount = mastCount;
        this.sailCount = sailCount;
        this.doorCount = doorCount;

        // Samples
        this.orientationsW = new FloatArrayList();
        this.orientationsX = new FloatArrayList();
        this.orientationsY = new FloatArrayList();
        this.orientationsZ = new FloatArrayList();
        this.rudderAngles = new FloatArrayList();
        this.anchored = new IntArrayList();
        this.braceAngles = new FloatArrayList();
        this.sailHoists = new FloatArrayList();
        this.doorOpenings = new FloatArrayList();
    }

    // Management \\

    public void addSample(float time, VehicleInstance vehicle) {

        addPlace(time, vehicle.getWorldPositionStruct().getChunkCoordinate(),
                vehicle.getWorldPositionStruct().getPosition());

        orientationsW.add(vehicle.getOrientation().getW());
        orientationsX.add(vehicle.getOrientation().getX());
        orientationsY.add(vehicle.getOrientation().getY());
        orientationsZ.add(vehicle.getOrientation().getZ());
        rudderAngles.add(vehicle.getRudderAngle());
        anchored.add(vehicle.isAnchored() ? 1 : 0);

        for (int i = 0; i < mastCount; i++)
            braceAngles.add(vehicle.getBraceAngle(i));

        for (int i = 0; i < sailCount; i++)
            sailHoists.add(vehicle.getSailHoist(i));

        for (int i = 0; i < doorCount; i++)
            doorOpenings.add(vehicle.getDoorOpening(i));
    }

    // The value of one mast, sail or door at the sample blended toward the next
    public float blendPart(FloatArrayList column, int partCount, int partIndex, int sample, float blend) {

        float from = column.getFloat(sample * partCount + partIndex);

        if (sample + 1 >= size())
            return from;

        return from + (column.getFloat((sample + 1) * partCount + partIndex) - from) * blend;
    }

    // Curves \\

    @Override
    public String getTrackLabel() {
        return vehicleName;
    }

    @Override
    protected RecorderCurveProperty[] getCurveProperties() {
        return RecorderCurveProperty.VEHICLE;
    }

    @Override
    public int getCurveCount() {
        return RecorderCurveProperty.VEHICLE.length + mastCount + sailCount + doorCount;
    }

    @Override
    public RecorderCurveProperty getCurveProperty(int slot) {

        int partSlot = slot - RecorderCurveProperty.VEHICLE.length;

        if (partSlot < 0)
            return RecorderCurveProperty.VEHICLE[slot];

        if (partSlot < mastCount)
            return RecorderCurveProperty.BRACE;

        return partSlot < mastCount + sailCount ? RecorderCurveProperty.HOIST : RecorderCurveProperty.DOOR;
    }

    @Override
    public int getCurvePart(int slot) {

        int partSlot = slot - RecorderCurveProperty.VEHICLE.length;

        if (partSlot < 0)
            return super.getCurvePart(slot);

        if (partSlot < mastCount)
            return partSlot;

        return partSlot < mastCount + sailCount ? partSlot - mastCount : partSlot - mastCount - sailCount;
    }

    // A mast, sail or door is named with its number, counted from one
    @Override
    public String getCurveLabel(int slot) {

        int part = getCurvePart(slot);

        return part < 0
                ? super.getCurveLabel(slot)
                : String.format(EditorSetting.RECORDER_CURVE_PART_FORMAT, getCurveProperty(slot).getLabel(), part + 1);
    }

    @Override
    public double readCurveSample(int slot, int sample) {

        RecorderCurveProperty property = getCurveProperty(slot);

        return switch (property) {
            case YAW, PITCH, ROLL -> RecordingCurveUtility.toEulerAngle(
                    property,
                    orientationsW.getFloat(sample),
                    orientationsX.getFloat(sample),
                    orientationsY.getFloat(sample),
                    orientationsZ.getFloat(sample));
            case RUDDER -> rudderAngles.getFloat(sample);
            case BRACE -> braceAngles.getFloat(sample * mastCount + getCurvePart(slot));
            case HOIST -> sailHoists.getFloat(sample * sailCount + getCurvePart(slot));
            case DOOR -> doorOpenings.getFloat(sample * doorCount + getCurvePart(slot));
            default -> readPlaceSample(property, sample);
        };
    }

    // Accessible \\

    public String getVehicleName() {
        return vehicleName;
    }

    public int getMastCount() {
        return mastCount;
    }

    public int getSailCount() {
        return sailCount;
    }

    public int getDoorCount() {
        return doorCount;
    }

    public FloatArrayList getOrientationsW() {
        return orientationsW;
    }

    public FloatArrayList getOrientationsX() {
        return orientationsX;
    }

    public FloatArrayList getOrientationsY() {
        return orientationsY;
    }

    public FloatArrayList getOrientationsZ() {
        return orientationsZ;
    }

    public FloatArrayList getRudderAngles() {
        return rudderAngles;
    }

    public IntArrayList getAnchored() {
        return anchored;
    }

    public FloatArrayList getBraceAngles() {
        return braceAngles;
    }

    public FloatArrayList getSailHoists() {
        return sailHoists;
    }

    public FloatArrayList getDoorOpenings() {
        return doorOpenings;
    }
}
