package editor.bootstrap.faceeditorpipeline.faceeditormanager;

import java.util.function.Consumer;

import editor.bootstrap.charactereditorpipeline.characterdocument.CharacterDocumentInstance;
import editor.bootstrap.infopipeline.infomanager.InfoManager;
import editor.runtime.characterview.util.CharacterViewUtility;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.mathematics.vectors.Vector3;

class FaceEditBranch extends BranchPackage {

    /*
     * Reads and performs every change the face editor makes to a race's
     * face controls — its appearance's shape sliders and the bones each
     * drives — each through InfoManager so the Info Panel, both character
     * tools and the game see it at once, and each recorded in the race's
     * face history first unless a drag already holds it. A control is found
     * by its place in the shapes list, the order the builder reads it in; a
     * bone target by its bone. A target keeps only the channels that move
     * anything, and stays on its bone until it is removed, so a control
     * never ends up driving nothing. Renaming or
     * removing a control renames or drops the start every gender of the race
     * gives it.
     * Mirroring writes the partner bone's target across the face: its move
     * flipped on X, its turn on Y and Z, its scale as it is.
     */

    // Internal
    private InfoManager infoManager;
    private FaceEditorManager faceEditorManager;

    // Scratch
    private Vector3 mirrorMove;
    private Vector3 mirrorTurn;
    private Vector3 readMove;
    private Vector3 readTurn;
    private Vector3 readScale;

    // Base \\

    @Override
    protected void create() {

        // Scratch
        this.mirrorMove = new Vector3();
        this.mirrorTurn = new Vector3();
        this.readMove = new Vector3();
        this.readTurn = new Vector3();
        this.readScale = new Vector3();
    }

    @Override
    protected void get() {
        this.infoManager = get(InfoManager.class);
        this.faceEditorManager = get(FaceEditorManager.class);
    }

    // Race \\

    // One change to the race, recorded for undo first unless a gesture already holds the race
    private void editRace(CharacterDocumentInstance document, Consumer<ArpgObjectStruct> editor) {

        ArpgObjectStruct raceArpg = faceEditorManager.getTemplateArpg();

        if (raceArpg == null)
            return;

        faceEditorManager.getHistory(document).record(raceArpg);
        apply(document, editor);
    }

    // One change to the race's appearance
    private void edit(CharacterDocumentInstance document, Consumer<ArpgObjectStruct> editor) {
        editRace(document, race -> editor.accept(
                CharacterViewUtility.requireObject(race, EditorSetting.CHARACTER_EDITOR_FIELD_APPEARANCE)));
    }

    private void editShape(CharacterDocumentInstance document, int shapeIndex, Consumer<ArpgObjectStruct> editor) {
        edit(document, appearance -> editor.accept(CharacterViewUtility
                .requireArray(appearance, EditorSetting.FACE_EDITOR_FIELD_SHAPES)
                .get(shapeIndex)
                .getAsObject()));
    }

    private void apply(CharacterDocumentInstance document, Consumer<ArpgObjectStruct> editor) {
        infoManager.editEntry(
                EditorSetting.INFO_SCHEMA_RACES,
                document.getRaceName(),
                document.getRaceName(),
                editor);
    }

    // Puts a recorded race back whole; false when there is none to put back
    boolean restore(CharacterDocumentInstance document, ArpgObjectStruct snapshot) {

        if (snapshot == null)
            return false;

        apply(document, race -> CharacterViewUtility.replaceObject(race, snapshot));
        return true;
    }

    // Reading \\

    ArpgObjectStruct getAppearanceArpg() {
        return CharacterViewUtility.findObject(
                faceEditorManager.getTemplateArpg(),
                EditorSetting.CHARACTER_EDITOR_FIELD_APPEARANCE);
    }

    private ArpgArrayStruct getShapesArpg() {

        ArpgObjectStruct appearance = getAppearanceArpg();

        return appearance != null && ArpgUtility.hasArray(appearance, EditorSetting.FACE_EDITOR_FIELD_SHAPES)
                ? appearance.getAsArray(EditorSetting.FACE_EDITOR_FIELD_SHAPES)
                : null;
    }

    int getShapeCount(CharacterDocumentInstance document) {

        ArpgArrayStruct shapes = document != null ? getShapesArpg() : null;
        return shapes != null ? shapes.size() : 0;
    }

    boolean hasShape(CharacterDocumentInstance document, int shapeIndex) {
        return shapeIndex >= 0 && shapeIndex < getShapeCount(document);
    }

    // A control's ARPG, null when the race has none at the index
    ArpgObjectStruct getShapeArpg(CharacterDocumentInstance document, int shapeIndex) {
        return hasShape(document, shapeIndex) ? getShapesArpg().get(shapeIndex).getAsObject() : null;
    }

    // A control's name: its own, else its morph's, as the builder knows it
    String getShapeName(CharacterDocumentInstance document, int shapeIndex) {

        ArpgObjectStruct shape = getShapeArpg(document, shapeIndex);

        if (shape == null)
            return "";

        return ArpgUtility.getString(shape, EditorSetting.FACE_EDITOR_FIELD_NAME,
                ArpgUtility.getString(shape, EditorSetting.FACE_EDITOR_FIELD_MORPH, ""));
    }

    // The index of the control of a name, INDEX_NOT_FOUND when none has it
    int findShape(CharacterDocumentInstance document, String shapeName) {

        for (int i = 0; i < getShapeCount(document); i++)
            if (getShapeName(document, i).equals(shapeName))
                return i;

        return EngineSetting.INDEX_NOT_FOUND;
    }

    float getNumber(CharacterDocumentInstance document, int shapeIndex, String field, float defaultValue) {

        ArpgObjectStruct shape = getShapeArpg(document, shapeIndex);
        return shape != null ? ArpgUtility.getFloat(shape, field, defaultValue) : defaultValue;
    }

    float getMin(CharacterDocumentInstance document, int shapeIndex) {
        return getNumber(document, shapeIndex, EditorSetting.FACE_EDITOR_FIELD_MIN, EngineSetting.APPEARANCE_SHAPE_MIN);
    }

    float getMax(CharacterDocumentInstance document, int shapeIndex) {
        return getNumber(document, shapeIndex, EditorSetting.FACE_EDITOR_FIELD_MAX, EngineSetting.APPEARANCE_SHAPE_MAX);
    }

    float getDefault(CharacterDocumentInstance document, int shapeIndex) {
        return getNumber(document, shapeIndex, EditorSetting.FACE_EDITOR_FIELD_DEFAULT,
                EngineSetting.APPEARANCE_SHAPE_DEFAULT);
    }

    boolean hasMorph(CharacterDocumentInstance document, int shapeIndex) {

        ArpgObjectStruct shape = getShapeArpg(document, shapeIndex);
        return shape != null && ArpgUtility.hasString(shape, EditorSetting.FACE_EDITOR_FIELD_MORPH);
    }

    // The value a gender starts a control at as the race writes it, NaN when the gender gives it none
    float getStart(String genderName, String shapeName) {

        ArpgObjectStruct shapes = CharacterViewUtility.findObject(
                CharacterViewUtility.findObject(
                        CharacterViewUtility.findObject(
                                faceEditorManager.getTemplateArpg(),
                                EditorSetting.FACE_EDITOR_FIELD_GENDERS),
                        genderName),
                EditorSetting.FACE_EDITOR_FIELD_SHAPES);

        return shapes != null && ArpgUtility.hasNumber(shapes, shapeName)
                ? shapes.get(shapeName).getAsFloat()
                : Float.NaN;
    }

    // The end of a control's range farthest from nothing, the value a drag shapes it at when it is previewed near none
    float getFarEnd(CharacterDocumentInstance document, int shapeIndex) {

        float min = getMin(document, shapeIndex);
        float max = getMax(document, shapeIndex);

        return Math.abs(max) >= Math.abs(min) ? max : min;
    }

    ArpgArrayStruct getTargetsArpg(CharacterDocumentInstance document, int shapeIndex) {

        ArpgObjectStruct shape = getShapeArpg(document, shapeIndex);

        return shape != null && ArpgUtility.hasArray(shape, EditorSetting.FACE_EDITOR_FIELD_BONES)
                ? shape.getAsArray(EditorSetting.FACE_EDITOR_FIELD_BONES)
                : null;
    }

    int getTargetCount(CharacterDocumentInstance document, int shapeIndex) {

        ArpgArrayStruct targets = getTargetsArpg(document, shapeIndex);
        return targets != null ? targets.size() : 0;
    }

    String getTargetBoneName(CharacterDocumentInstance document, int shapeIndex, int targetIndex) {
        return ArpgUtility.getString(
                getTargetsArpg(document, shapeIndex).get(targetIndex).getAsObject(),
                EditorSetting.FACE_EDITOR_FIELD_BONE,
                "");
    }

    // The target on a bone among a control's targets, INDEX_NOT_FOUND when the control does not drive it
    int findTarget(CharacterDocumentInstance document, int shapeIndex, String boneName) {

        for (int i = 0; i < getTargetCount(document, shapeIndex); i++)
            if (getTargetBoneName(document, shapeIndex, i).equals(boneName))
                return i;

        return EngineSetting.INDEX_NOT_FOUND;
    }

    float getTargetComponent(
            CharacterDocumentInstance document,
            int shapeIndex,
            int targetIndex,
            int channel,
            int axis) {

        return CharacterViewUtility.getComponent(
                getTargetsArpg(document, shapeIndex).get(targetIndex).getAsObject(),
                EditorSetting.FACE_EDITOR_CHANNEL_FIELDS[channel],
                axis,
                0f);
    }

    // A control's whole target on a bone, every channel nothing when it does not drive the bone
    void readTarget(
            CharacterDocumentInstance document,
            int shapeIndex,
            String boneName,
            Vector3 move,
            Vector3 turn,
            Vector3 scale) {

        int targetIndex = findTarget(document, shapeIndex, boneName);

        readChannel(document, shapeIndex, targetIndex, EditorSetting.FACE_EDITOR_CHANNEL_MOVE, move);
        readChannel(document, shapeIndex, targetIndex, EditorSetting.FACE_EDITOR_CHANNEL_TURN, turn);
        readChannel(document, shapeIndex, targetIndex, EditorSetting.FACE_EDITOR_CHANNEL_SCALE, scale);
    }

    private void readChannel(
            CharacterDocumentInstance document,
            int shapeIndex,
            int targetIndex,
            int channel,
            Vector3 out) {

        if (targetIndex == EngineSetting.INDEX_NOT_FOUND) {
            out.set(0f, 0f, 0f);
            return;
        }

        out.set(
                getTargetComponent(document, shapeIndex, targetIndex, channel, 0),
                getTargetComponent(document, shapeIndex, targetIndex, channel, 1),
                getTargetComponent(document, shapeIndex, targetIndex, channel, 2));
    }

    // Controls \\

    // A new control at the end of the list, returning its index
    int addShape(CharacterDocumentInstance document, ArpgObjectStruct shapeArpg) {

        edit(document, appearance -> CharacterViewUtility
                .requireArray(appearance, EditorSetting.FACE_EDITOR_FIELD_SHAPES)
                .add(shapeArpg));

        return getShapeCount(document) - 1;
    }

    // Removes a control and the start every gender gives it
    void removeShape(CharacterDocumentInstance document, int shapeIndex) {

        String shapeName = getShapeName(document, shapeIndex);

        editRace(document, race -> {
            race.getAsObject(EditorSetting.CHARACTER_EDITOR_FIELD_APPEARANCE)
                    .getAsArray(EditorSetting.FACE_EDITOR_FIELD_SHAPES)
                    .remove(shapeIndex);
            renameStarts(race, shapeName, null);
        });
    }

    // A control's name, every gender's start for it following
    void renameShape(CharacterDocumentInstance document, int shapeIndex, String shapeName) {

        String oldName = getShapeName(document, shapeIndex);

        editRace(document, race -> {
            race.getAsObject(EditorSetting.CHARACTER_EDITOR_FIELD_APPEARANCE)
                    .getAsArray(EditorSetting.FACE_EDITOR_FIELD_SHAPES)
                    .get(shapeIndex)
                    .getAsObject()
                    .addProperty(EditorSetting.FACE_EDITOR_FIELD_NAME, shapeName);
            renameStarts(race, oldName, shapeName);
        });
    }

    // Every gender's start for a control moved to a new name, or dropped for none
    private void renameStarts(ArpgObjectStruct race, String oldName, String newName) {

        ArpgObjectStruct genders = CharacterViewUtility.findObject(race, EditorSetting.FACE_EDITOR_FIELD_GENDERS);

        if (genders == null)
            return;

        for (String genderName : genders.keySet()) {

            ArpgObjectStruct shapes = CharacterViewUtility.findObject(
                    genders.getAsObject(genderName),
                    EditorSetting.FACE_EDITOR_FIELD_SHAPES);

            if (shapes == null || !shapes.has(oldName))
                continue;

            float value = shapes.get(oldName).getAsFloat();
            shapes.remove(oldName);

            if (newName != null)
                shapes.addProperty(newName, value);
        }
    }

    // A text field of a control set, or removed for null
    void setField(CharacterDocumentInstance document, int shapeIndex, String field, String value) {
        editShape(document, shapeIndex, shape -> {

            if (value == null)
                shape.remove(field);
            else
                shape.addProperty(field, value);
        });
    }

    // A control's range and default written as one edit
    void setRange(CharacterDocumentInstance document, int shapeIndex, float min, float max, float defaultValue) {
        editShape(document, shapeIndex, shape -> {
            shape.addProperty(EditorSetting.FACE_EDITOR_FIELD_MIN, CharacterViewUtility.round(min));
            shape.addProperty(EditorSetting.FACE_EDITOR_FIELD_MAX, CharacterViewUtility.round(max));
            shape.addProperty(EditorSetting.FACE_EDITOR_FIELD_DEFAULT, CharacterViewUtility.round(defaultValue));
        });
    }

    // The value a gender starts a control at, or its start removed for NaN so the control's default holds
    void setStart(CharacterDocumentInstance document, String genderName, String shapeName, float value) {
        editRace(document, race -> {

            ArpgObjectStruct shapes = CharacterViewUtility.requireObject(
                    CharacterViewUtility.requireObject(
                            CharacterViewUtility.requireObject(race, EditorSetting.FACE_EDITOR_FIELD_GENDERS),
                            genderName),
                    EditorSetting.FACE_EDITOR_FIELD_SHAPES);

            if (Float.isNaN(value))
                shapes.remove(shapeName);
            else
                shapes.addProperty(shapeName, CharacterViewUtility.round(value));
        });
    }

    // Targets \\

    // A bone the control does not yet drive, added moving nothing
    void addTarget(CharacterDocumentInstance document, int shapeIndex, String boneName) {

        if (findTarget(document, shapeIndex, boneName) != EngineSetting.INDEX_NOT_FOUND)
            return;

        editShape(document, shapeIndex, shape -> CharacterViewUtility
                .requireArray(shape, EditorSetting.FACE_EDITOR_FIELD_BONES)
                .add(createTarget(boneName)));
    }

    void removeTarget(CharacterDocumentInstance document, int shapeIndex, int targetIndex) {
        editShape(document, shapeIndex, shape -> {

            ArpgArrayStruct targets = shape.getAsArray(EditorSetting.FACE_EDITOR_FIELD_BONES);
            targets.remove(targetIndex);

            if (targets.isEmpty())
                shape.remove(EditorSetting.FACE_EDITOR_FIELD_BONES);
        });
    }

    void setTargetComponent(
            CharacterDocumentInstance document,
            int shapeIndex,
            int targetIndex,
            int channel,
            int axis,
            float value) {

        editShape(document, shapeIndex, shape -> CharacterViewUtility.setArrayComponent(
                shape.getAsArray(EditorSetting.FACE_EDITOR_FIELD_BONES).get(targetIndex).getAsObject(),
                EditorSetting.FACE_EDITOR_CHANNEL_FIELDS[channel],
                axis,
                value,
                0f));
    }

    // A control's whole target on a bone, and its mirror on the partner bone when one is named
    void writeTarget(
            CharacterDocumentInstance document,
            int shapeIndex,
            String boneName,
            Vector3 move,
            Vector3 turn,
            Vector3 scale,
            String mirrorBoneName) {

        if (mirrorBoneName != null) {
            mirrorMove.set(-move.x, move.y, move.z);
            mirrorTurn.set(turn.x, -turn.y, -turn.z);
        }

        editShape(document, shapeIndex, shape -> {

            ArpgArrayStruct targets = CharacterViewUtility.requireArray(shape, EditorSetting.FACE_EDITOR_FIELD_BONES);

            putTarget(targets, boneName, move, turn, scale);

            if (mirrorBoneName != null)
                putTarget(targets, mirrorBoneName, mirrorMove, mirrorTurn, scale);
        });
    }

    // Every target on one side of the face written across onto its partner bone, replacing what the partner held
    void mirrorTargets(CharacterDocumentInstance document, int shapeIndex, String fromSuffix, String toSuffix) {

        int count = getTargetCount(document, shapeIndex);

        faceEditorManager.getHistory(document).begin(faceEditorManager.getTemplateArpg());

        for (int i = 0; i < count; i++) {

            String boneName = getTargetBoneName(document, shapeIndex, i);

            if (!boneName.endsWith(fromSuffix))
                continue;

            String partnerName = boneName.substring(0, boneName.length() - fromSuffix.length()) + toSuffix;

            if (!faceEditorManager.getRigHandle().hasBone(partnerName))
                continue;

            readTarget(document, shapeIndex, boneName, readMove, readTurn, readScale);
            writeTarget(document, shapeIndex, partnerName, mirrorMove.set(-readMove.x, readMove.y, readMove.z),
                    mirrorTurn.set(readTurn.x, -readTurn.y, -readTurn.z), readScale, null);
        }

        faceEditorManager.getHistory(document).commit(faceEditorManager.getTemplateArpg());
    }

    private void putTarget(ArpgArrayStruct targets, String boneName, Vector3 move, Vector3 turn, Vector3 scale) {

        int targetIndex = EngineSetting.INDEX_NOT_FOUND;

        for (int i = 0; i < targets.size(); i++)
            if (boneName.equals(ArpgUtility.getString(
                    targets.get(i).getAsObject(), EditorSetting.FACE_EDITOR_FIELD_BONE, "")))
                targetIndex = i;

        ArpgObjectStruct target = targetIndex != EngineSetting.INDEX_NOT_FOUND
                ? targets.get(targetIndex).getAsObject()
                : createTarget(boneName);

        putChannel(target, EditorSetting.FACE_EDITOR_CHANNEL_MOVE, move);
        putChannel(target, EditorSetting.FACE_EDITOR_CHANNEL_TURN, turn);
        putChannel(target, EditorSetting.FACE_EDITOR_CHANNEL_SCALE, scale);

        if (targetIndex == EngineSetting.INDEX_NOT_FOUND)
            targets.add(target);
    }

    private void putChannel(ArpgObjectStruct target, int channel, Vector3 value) {

        String field = EditorSetting.FACE_EDITOR_CHANNEL_FIELDS[channel];

        if (isMoving(value))
            target.add(field, CharacterViewUtility.toArray(value.x, value.y, value.z));
        else
            target.remove(field);
    }

    private ArpgObjectStruct createTarget(String boneName) {

        ArpgObjectStruct target = new ArpgObjectStruct();
        target.addProperty(EditorSetting.FACE_EDITOR_FIELD_BONE, boneName);

        return target;
    }

    // True for an amount that survives rounding to the precision files are written at
    private boolean isMoving(Vector3 value) {
        return CharacterViewUtility.round(value.x) != 0f
                || CharacterViewUtility.round(value.y) != 0f
                || CharacterViewUtility.round(value.z) != 0f;
    }
}
