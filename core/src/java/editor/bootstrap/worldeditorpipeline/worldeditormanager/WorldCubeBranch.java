package editor.bootstrap.worldeditorpipeline.worldeditormanager;

import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worldmanager.WorldManager;
import editor.bootstrap.infopipeline.infoentry.InfoEntryStruct;
import editor.bootstrap.infopipeline.infomanager.InfoManager;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.mathematics.matrices.Matrix3Double;
import engine.util.mathematics.vectors.Vector3;
import engine.util.mathematics.vectors.Vector3Double;
import it.unimi.dsi.fastutil.floats.FloatArrayList;

class WorldCubeBranch extends BranchPackage {

    /*
     * A globe's block cube as the World Editor's Cube layer works it. The
     * cube's turn lives in the world's entry as cube_rotation, so the Rotate
     * tool and the Properties tab edit one value: whenever the entry changes,
     * the world's cube is turned to match and the world restreamed. Rotate
     * turns the cube as a trackball turns, about the globe's middle, carrying
     * the point first pressed to the point under the pointer; the cube is
     * shown turning while the drag lasts and written to the entry once let
     * go, as one edit, which Undo and Redo step back and forth through on the
     * layer. Select picks the world's entry, so its fields are the Properties
     * tab's.
     */

    // Internal
    private InfoManager infoManager;
    private WorldManager worldManager;
    private WorldEditorManager worldEditorManager;

    // Turn
    private final Matrix3Double shownFromCube = new Matrix3Double();
    private final Matrix3Double pressFromCube = new Matrix3Double();
    private final Vector3Double pressDirection = new Vector3Double();
    private boolean turning;

    // History — the degrees about X, Y and Z of every turn stepped from, three to a turn
    private final FloatArrayList undoTurns = new FloatArrayList();
    private final FloatArrayList redoTurns = new FloatArrayList();

    // Base \\

    @Override
    protected void get() {
        this.infoManager = get(InfoManager.class);
        this.worldManager = get(WorldManager.class);
        this.worldEditorManager = get(WorldEditorManager.class);
    }

    @Override
    protected void awake() {
        infoManager.addChangeListener(EditorSetting.INFO_SCHEMA_WORLDS, this::adoptEntry);
    }

    // Entry \\

    // The world's cube turned to its entry's cube_rotation whenever the two differ and the entry holds a turn the
    // world can take
    private void adoptEntry(InfoEntryStruct entry) {

        WorldHandle worldHandle = worldEditorManager.getWorldHandle();

        if (worldHandle == null || !worldHandle.isGlobe() || !entry.getEntryName().equals(worldHandle.getWorldName()))
            return;

        ArpgObjectStruct worldArpg = entry.getArpg();

        if (!ArpgUtility.hasArray(worldArpg, EditorSetting.WORLD_EDITOR_KEY_CUBE_ROTATION))
            return;

        ArpgArrayStruct degrees = worldArpg.getAsArray(EditorSetting.WORLD_EDITOR_KEY_CUBE_ROTATION);

        if (degrees.size() != EngineSetting.AXIS_COUNT)
            return;

        float degreesX = degrees.get(EngineSetting.AXIS_X).getAsFloat();
        float degreesY = degrees.get(EngineSetting.AXIS_Y).getAsFloat();
        float degreesZ = degrees.get(EngineSetting.AXIS_Z).getAsFloat();
        Vector3 current = worldHandle.getCubeRotation();

        if (current.x == degreesX && current.y == degreesY && current.z == degreesZ)
            return;

        if (!isTurnAllowed(degreesX) || !isTurnAllowed(degreesY) || !isTurnAllowed(degreesZ))
            return;

        worldManager.setCubeRotation(worldHandle, degreesX, degreesY, degreesZ);
        worldEditorManager.restreamWorld(worldHandle);
    }

    private boolean isTurnAllowed(float degrees) {
        return Math.abs(degrees) <= EngineSetting.GLOBE_CUBE_ROTATION_MAX_DEGREES;
    }

    // Select \\

    void select(WorldHandle worldHandle) {
        infoManager.selectEntry(EditorSetting.INFO_SCHEMA_WORLDS, worldHandle.getWorldName(), worldHandle.getWorldName());
    }

    // Turn \\

    // A turn begun from the globe-frame direction pressed, the cube as it stands
    void beginTurn(WorldHandle worldHandle, Vector3Double direction) {

        pressFromCube.set(worldHandle.getGlobeFromCube());
        shownFromCube.set(pressFromCube);
        pressDirection.set(direction);
        this.turning = true;
    }

    // The cube turned about the axis carrying the pressed direction to this one, through the angle between them
    void dragTurn(Vector3Double direction) {

        if (!turning)
            return;

        double axisX = pressDirection.y * direction.z - pressDirection.z * direction.y;
        double axisY = pressDirection.z * direction.x - pressDirection.x * direction.z;
        double axisZ = pressDirection.x * direction.y - pressDirection.y * direction.x;
        double sine = Math.sqrt(axisX * axisX + axisY * axisY + axisZ * axisZ);
        double cosine = pressDirection.x * direction.x + pressDirection.y * direction.y
                + pressDirection.z * direction.z;

        if (sine < EngineSetting.DIVISION_EPSILON) {
            shownFromCube.set(pressFromCube);
            return;
        }

        axisX /= sine;
        axisY /= sine;
        axisZ /= sine;

        double angle = Math.atan2(sine, cosine);
        double c = Math.cos(angle);
        double s = Math.sin(angle);
        double t = 1.0 - c;

        shownFromCube.set(
                t * axisX * axisX + c, t * axisX * axisY - s * axisZ, t * axisX * axisZ + s * axisY,
                t * axisX * axisY + s * axisZ, t * axisY * axisY + c, t * axisY * axisZ - s * axisX,
                t * axisX * axisZ - s * axisY, t * axisY * axisZ + s * axisX, t * axisZ * axisZ + c)
                .multiply(pressFromCube);
    }

    // The turn shown written to the world's entry as degrees about X, Y and Z, one step of the layer's history
    void endTurn(WorldHandle worldHandle) {

        if (!turning)
            return;

        this.turning = false;

        double pitch = Math.asin(Math.max(-1.0, Math.min(1.0, -shownFromCube.getM12())));
        double yaw = Math.atan2(shownFromCube.getM02(), shownFromCube.getM22());
        double roll = Math.atan2(shownFromCube.getM10(), shownFromCube.getM11());
        pushTurn(undoTurns, worldHandle.getCubeRotation());
        redoTurns.clear();
        writeTurn(worldHandle, toStep(pitch), toStep(yaw), toStep(roll));
    }

    // History \\

    // The turn before the last one written back, false when there is none
    boolean undo(WorldHandle worldHandle) {
        return stepTurn(worldHandle, undoTurns, redoTurns);
    }

    boolean redo(WorldHandle worldHandle) {
        return stepTurn(worldHandle, redoTurns, undoTurns);
    }

    private boolean stepTurn(WorldHandle worldHandle, FloatArrayList from, FloatArrayList to) {

        if (from.isEmpty())
            return false;

        int last = from.size() - EngineSetting.AXIS_COUNT;

        pushTurn(to, worldHandle.getCubeRotation());
        writeTurn(worldHandle, from.getFloat(last), from.getFloat(last + 1), from.getFloat(last + 2));
        from.size(last);

        return true;
    }

    private void pushTurn(FloatArrayList turns, Vector3 degrees) {
        turns.add(degrees.x);
        turns.add(degrees.y);
        turns.add(degrees.z);
    }

    // Degrees about X, Y and Z written to the world's entry as its cube_rotation, which turns the world's cube
    private void writeTurn(WorldHandle worldHandle, float degreesX, float degreesY, float degreesZ) {

        ArpgArrayStruct degrees = new ArpgArrayStruct();

        degrees.add(degreesX);
        degrees.add(degreesY);
        degrees.add(degreesZ);

        infoManager.editEntry(
                EditorSetting.INFO_SCHEMA_WORLDS,
                worldHandle.getWorldName(),
                worldHandle.getWorldName(),
                worldArpg -> {
                    worldArpg.remove(EditorSetting.WORLD_EDITOR_KEY_CUBE_ROTATION);
                    worldArpg.add(EditorSetting.WORLD_EDITOR_KEY_CUBE_ROTATION, degrees);
                });
    }

    // An angle in radians as degrees held to the turn's step, so the entry keeps tidy values
    private float toStep(double radians) {

        double step = EditorSetting.WORLD_EDITOR_CUBE_DEGREE_STEP;

        return (float) (Math.round(Math.toDegrees(radians) / step) * step);
    }

    // Accessible \\

    // The cube as it is shown: turning under the drag while one lasts, else as the world holds it
    Matrix3Double getShownFromCube(WorldHandle worldHandle) {
        return turning ? shownFromCube : worldHandle.getGlobeFromCube();
    }

    boolean isTurning() {
        return turning;
    }
}
