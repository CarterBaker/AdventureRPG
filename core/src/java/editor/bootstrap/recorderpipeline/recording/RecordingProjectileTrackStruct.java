package editor.bootstrap.recorderpipeline.recording;

import editor.bootstrap.recorderpipeline.util.RecordingCurveUtility;
import engine.util.mathematics.quaternions.Quaternion;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.floats.FloatArrayList;

public class RecordingProjectileTrackStruct extends RecordingTrackStruct {

    /*
     * One item thrown or let fall in a take: the item it was, and, sample
     * by sample, where its centre flew and how it was turned as it tumbled,
     * its turn carried on curves as a yaw, a pitch and a roll.
     */

    // Identity
    private final String itemName;

    // Samples
    private final FloatArrayList orientationsW;
    private final FloatArrayList orientationsX;
    private final FloatArrayList orientationsY;
    private final FloatArrayList orientationsZ;

    // Constructor \\

    public RecordingProjectileTrackStruct(String itemName) {

        // Identity
        this.itemName = itemName;

        // Samples
        this.orientationsW = new FloatArrayList();
        this.orientationsX = new FloatArrayList();
        this.orientationsY = new FloatArrayList();
        this.orientationsZ = new FloatArrayList();
    }

    // Management \\

    public void addSample(float time, long chunkCoordinate, Vector3 position, Quaternion orientation) {

        addPlace(time, chunkCoordinate, position);

        orientationsW.add(orientation.getW());
        orientationsX.add(orientation.getX());
        orientationsY.add(orientation.getY());
        orientationsZ.add(orientation.getZ());
    }

    // Curves \\

    @Override
    public String getTrackLabel() {
        return itemName;
    }

    @Override
    protected RecorderCurveProperty[] getCurveProperties() {
        return RecorderCurveProperty.PROJECTILE;
    }

    @Override
    public double readCurveSample(int slot, int sample) {

        RecorderCurveProperty property = getCurveProperty(slot);

        if (property.isPosition())
            return readPlaceSample(property, sample);

        return RecordingCurveUtility.toEulerAngle(
                property,
                orientationsW.getFloat(sample),
                orientationsX.getFloat(sample),
                orientationsY.getFloat(sample),
                orientationsZ.getFloat(sample));
    }

    // Accessible \\

    public String getItemName() {
        return itemName;
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
}
