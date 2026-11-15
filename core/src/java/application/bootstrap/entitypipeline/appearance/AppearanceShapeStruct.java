package application.bootstrap.entitypipeline.appearance;

import engine.root.StructPackage;

public class AppearanceShapeStruct extends StructPackage {

    /*
     * One slider of a character's appearance, of one of three kinds: a bone
     * slider, one channel of a bone the rig offers (both bones of a left and
     * right pair, the right mirrored), its value moving, turning or sizing
     * them in the channel's own unit; a control, one of the rig's controls,
     * driving several bone sliders along their curves; or a shape key, a
     * morph target of the body meshes weighed by its value. Each is known by
     * the name saves and presets know it by, listed under a group, held to
     * its range around its default, and either offered to the player by the
     * creator and NPC variation or kept for the race's author alone, as the
     * bone sliders under a race's controls usually are. The morph is named
     * here and resolved per body mesh by AppearanceData.
     */

    // Identity
    private final String shapeName;
    private final String groupName;
    private final String morphName;

    // Targets — the bones a bone slider moves per unit, and the bone sliders a control drives
    private final AppearanceBoneTargetStruct[] boneTargets;
    private final AppearanceDriveStruct[] drives;

    // Range
    private final float min;
    private final float max;
    private final float defaultValue;

    // Player
    private final boolean shownToPlayer;

    // Constructor \\

    public AppearanceShapeStruct(
            String shapeName,
            String groupName,
            String morphName,
            AppearanceBoneTargetStruct[] boneTargets,
            AppearanceDriveStruct[] drives,
            float min,
            float max,
            float defaultValue,
            boolean shownToPlayer) {

        // Identity
        this.shapeName = shapeName;
        this.groupName = groupName;
        this.morphName = morphName;

        // Targets
        this.boneTargets = boneTargets;
        this.drives = drives;

        // Range
        this.min = min;
        this.max = max;
        this.defaultValue = defaultValue;

        // Player
        this.shownToPlayer = shownToPlayer;
    }

    // Utility \\

    public float clamp(float value) {
        return Math.max(min, Math.min(max, value));
    }

    // Accessible \\

    public String getShapeName() {
        return shapeName;
    }

    public String getGroupName() {
        return groupName;
    }

    // Null for a slider that weighs no morph
    public String getMorphName() {
        return morphName;
    }

    public boolean hasMorph() {
        return morphName != null;
    }

    public int getBoneTargetCount() {
        return boneTargets.length;
    }

    public AppearanceBoneTargetStruct getBoneTarget(int targetIndex) {
        return boneTargets[targetIndex];
    }

    public boolean hasBoneTargets() {
        return boneTargets.length > 0;
    }

    public int getDriveCount() {
        return drives.length;
    }

    public AppearanceDriveStruct getDrive(int driveIndex) {
        return drives[driveIndex];
    }

    public boolean isControl() {
        return drives.length > 0;
    }

    // Whether moving it re-poses the bones, as a bone slider and a control do
    public boolean shapesBones() {
        return hasBoneTargets() || isControl();
    }

    public float getMin() {
        return min;
    }

    public float getMax() {
        return max;
    }

    public float getDefaultValue() {
        return defaultValue;
    }

    // Whether the player's creator and NPC variation offer it
    public boolean isShownToPlayer() {
        return shownToPlayer;
    }
}
