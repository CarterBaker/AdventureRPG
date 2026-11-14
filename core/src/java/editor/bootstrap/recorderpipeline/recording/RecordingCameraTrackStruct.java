package editor.bootstrap.recorderpipeline.recording;

import editor.bootstrap.recorderpipeline.util.RecordingCurveUtility;
import editor.runtime.EditorSetting;
import engine.assets.camera.CameraInstance;
import it.unimi.dsi.fastutil.floats.FloatArrayList;

public class RecordingCameraTrackStruct extends RecordingTrackStruct {

    /*
     * The view of a take, sample by sample: the camera's position in the
     * frame of the chunk the player stood in, the direction it looked along,
     * the shake it turned by in degrees, and its field of view. Its look is
     * carried on curves as a yaw and a pitch.
     */

    // Samples
    private final FloatArrayList directionsX;
    private final FloatArrayList directionsY;
    private final FloatArrayList directionsZ;
    private final FloatArrayList shakeYaws;
    private final FloatArrayList shakePitches;
    private final FloatArrayList shakeRolls;
    private final FloatArrayList fovs;

    // Constructor \\

    public RecordingCameraTrackStruct() {

        // Samples
        this.directionsX = new FloatArrayList();
        this.directionsY = new FloatArrayList();
        this.directionsZ = new FloatArrayList();
        this.shakeYaws = new FloatArrayList();
        this.shakePitches = new FloatArrayList();
        this.shakeRolls = new FloatArrayList();
        this.fovs = new FloatArrayList();
    }

    // Management \\

    public void addSample(float time, long chunkCoordinate, CameraInstance camera) {

        addPlace(time, chunkCoordinate, camera.getPosition());

        directionsX.add(camera.getDirection().x);
        directionsY.add(camera.getDirection().y);
        directionsZ.add(camera.getDirection().z);
        shakeYaws.add(camera.getShakeYawDegrees());
        shakePitches.add(camera.getShakePitchDegrees());
        shakeRolls.add(camera.getShakeRollDegrees());
        fovs.add(camera.getFOV());
    }

    // Curves \\

    @Override
    public String getTrackLabel() {
        return EditorSetting.RECORDER_TRACK_LABEL_CAMERA;
    }

    @Override
    protected RecorderCurveProperty[] getCurveProperties() {
        return RecorderCurveProperty.CAMERA;
    }

    @Override
    public double readCurveSample(int slot, int sample) {
        return switch (getCurveProperty(slot)) {
            case YAW -> RecordingCurveUtility.toDirectionYaw(
                    directionsX.getFloat(sample), directionsZ.getFloat(sample));
            case PITCH -> RecordingCurveUtility.toDirectionPitch(
                    directionsX.getFloat(sample), directionsY.getFloat(sample), directionsZ.getFloat(sample));
            case FIELD_OF_VIEW -> fovs.getFloat(sample);
            case SHAKE_YAW -> shakeYaws.getFloat(sample);
            case SHAKE_PITCH -> shakePitches.getFloat(sample);
            case SHAKE_ROLL -> shakeRolls.getFloat(sample);
            default -> readPlaceSample(getCurveProperty(slot), sample);
        };
    }

    // Accessible \\

    public FloatArrayList getDirectionsX() {
        return directionsX;
    }

    public FloatArrayList getDirectionsY() {
        return directionsY;
    }

    public FloatArrayList getDirectionsZ() {
        return directionsZ;
    }

    public FloatArrayList getShakeYaws() {
        return shakeYaws;
    }

    public FloatArrayList getShakePitches() {
        return shakePitches;
    }

    public FloatArrayList getShakeRolls() {
        return shakeRolls;
    }

    public FloatArrayList getFovs() {
        return fovs;
    }
}
