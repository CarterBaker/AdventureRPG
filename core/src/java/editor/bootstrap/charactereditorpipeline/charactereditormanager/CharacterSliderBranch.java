package editor.bootstrap.charactereditorpipeline.charactereditormanager;

import application.bootstrap.entitypipeline.appearance.AppearanceData;
import application.bootstrap.entitypipeline.appearance.AppearanceDriveStruct;
import application.bootstrap.entitypipeline.appearance.AppearanceShapeStruct;
import application.bootstrap.entitypipeline.util.BoneControlCurveUtility;
import editor.bootstrap.charactereditorpipeline.characterdocument.CharacterDocumentInstance;
import editor.runtime.EditorSetting;
import editor.runtime.characterview.panel.CharacterViewRowStruct;
import editor.runtime.characterview.util.CharacterViewUtility;
import editor.runtime.label.LabelUtility;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class CharacterSliderBranch extends BranchPackage {

    /*
     * The one place every slider of the Appearance rows is listed, read,
     * stepped, typed, cycled, picked or acted on, by the key its row carries.
     * Every slider the creator offers — each bone slider the rig's bones
     * offer, each control of the rig and each shape key — is listed under
     * its group in the order the race first lists each, with the value the
     * game starts the previewed gender at, that gender's Min and Max beneath
     * it (widening the slider's own range when they pass it) and whether the
     * player is offered it. A bone slider can be picked for the next
     * control, made with Add Control. Under each control come the bone
     * sliders it drives, each with an adjust slider showing where the bone
     * stands now and, dragged, setting a key of its curve where the control
     * stands, its Increase flag and the bone slider's own Min and Max, the
     * same values as its own rows. A control pushes from zero, so a key
     * cannot be set while the control stands there.
     */

    // Internal
    private CharacterEditorManager characterEditorManager;
    private CharacterEditBranch characterEditBranch;
    private CharacterControlBranch characterControlBranch;
    private CharacterPropertyBranch characterPropertyBranch;

    // Scratch
    private float[] curveScratch;

    // Base \\

    @Override
    protected void create() {
        this.curveScratch = new float[EngineSetting.BONE_CONTROL_CURVE_SCRATCH_FLOATS];
    }

    @Override
    protected void get() {
        this.characterEditorManager = get(CharacterEditorManager.class);
        this.characterEditBranch = get(CharacterEditBranch.class);
        this.characterControlBranch = get(CharacterControlBranch.class);
        this.characterPropertyBranch = get(CharacterPropertyBranch.class);
    }

    // Keys \\

    boolean handles(String keyName) {
        return switch (keyName) {
            case EditorSetting.CHARACTER_EDITOR_KEY_SHAPE,
                    EditorSetting.CHARACTER_EDITOR_KEY_SHAPE_MIN,
                    EditorSetting.CHARACTER_EDITOR_KEY_SHAPE_MAX,
                    EditorSetting.CHARACTER_EDITOR_KEY_SHAPE_PLAYER,
                    EditorSetting.CHARACTER_EDITOR_KEY_SHAPE_PICK,
                    EditorSetting.CHARACTER_EDITOR_KEY_DRIVE_ADJUST,
                    EditorSetting.CHARACTER_EDITOR_KEY_DRIVE_INCREASE,
                    EditorSetting.CHARACTER_EDITOR_KEY_CONTROL_ADD,
                    EditorSetting.CHARACTER_EDITOR_KEY_CONTROL_REMOVE -> true;
            default -> false;
        };
    }

    // Rows \\

    // Every slider under its group in the order the race first lists each, then Add Control
    void buildRows(CharacterDocumentInstance document, ObjectArrayList<CharacterViewRowStruct> rows) {

        AppearanceData appearanceData = resolveAppearanceData();

        if (appearanceData == null)
            return;

        boolean anyBoneSlider = false;

        for (int i = 0; i < appearanceData.getShapeCount(); i++) {

            String groupName = appearanceData.getShape(i).getGroupName();
            anyBoneSlider |= appearanceData.getShape(i).hasBoneTargets();

            if (isGroupListedBefore(appearanceData, groupName, i))
                continue;

            rows.add(CharacterViewRowStruct.header(LabelUtility.toWords(groupName)));

            for (int j = i; j < appearanceData.getShapeCount(); j++)
                if (appearanceData.getShape(j).getGroupName().equals(groupName))
                    addSlider(appearanceData, rows, j);
        }

        if (anyBoneSlider)
            rows.add(CharacterViewRowStruct.action(
                    EditorSetting.CHARACTER_EDITOR_KEY_CONTROL_ADD,
                    EditorSetting.CHARACTER_EDITOR_LABEL_CONTROL_ADD));
    }

    private boolean isGroupListedBefore(AppearanceData appearanceData, String groupName, int shapeIndex) {

        for (int i = 0; i < shapeIndex; i++)
            if (appearanceData.getShape(i).getGroupName().equals(groupName))
                return true;

        return false;
    }

    // A slider: the value the game starts the previewed gender at across their range, the range's two ends, whether
    // the player is offered it, and then by kind, its pick for the next control or the bone sliders it drives
    private void addSlider(
            AppearanceData appearanceData,
            ObjectArrayList<CharacterViewRowStruct> rows,
            int shapeIndex) {

        AppearanceShapeStruct shape = appearanceData.getShape(shapeIndex);
        String key = CharacterViewUtility.toKey(EditorSetting.CHARACTER_EDITOR_KEY_SHAPE, shapeIndex);
        short genderID = characterEditorManager.getPreviewGenderID();

        rows.add(CharacterViewRowStruct.ranged(
                key,
                toLabel(shape),
                getValueText(key),
                getNumber(key),
                appearanceData.getShapeMin(genderID, shapeIndex),
                appearanceData.getShapeMax(genderID, shapeIndex),
                getStep(key)));
        addRange(rows, shapeIndex);
        rows.add(CharacterViewRowStruct.choice(
                CharacterViewUtility.toKey(EditorSetting.CHARACTER_EDITOR_KEY_SHAPE_PLAYER, shapeIndex),
                EditorSetting.CHARACTER_EDITOR_LABEL_SHAPE_PLAYER,
                toSwitchText(shape.isShownToPlayer())));

        if (shape.hasBoneTargets())
            rows.add(CharacterViewRowStruct.item(
                    CharacterViewUtility.toKey(EditorSetting.CHARACTER_EDITOR_KEY_SHAPE_PICK, shapeIndex),
                    EditorSetting.CHARACTER_EDITOR_LABEL_SHAPE_PICK,
                    characterControlBranch.isPicked(shape.getShapeName())));

        if (!shape.isControl())
            return;

        for (int d = 0; d < shape.getDriveCount(); d++)
            addDrive(appearanceData, rows, shapeIndex, d);

        rows.add(CharacterViewRowStruct.action(
                CharacterViewUtility.toKey(EditorSetting.CHARACTER_EDITOR_KEY_CONTROL_REMOVE, shapeIndex),
                EditorSetting.CHARACTER_EDITOR_LABEL_CONTROL_REMOVE));
    }

    private void addRange(ObjectArrayList<CharacterViewRowStruct> rows, int shapeIndex) {

        String minKey = CharacterViewUtility.toKey(EditorSetting.CHARACTER_EDITOR_KEY_SHAPE_MIN, shapeIndex);
        String maxKey = CharacterViewUtility.toKey(EditorSetting.CHARACTER_EDITOR_KEY_SHAPE_MAX, shapeIndex);

        rows.add(CharacterViewRowStruct.pair(
                minKey,
                EditorSetting.CHARACTER_EDITOR_LABEL_SHAPE_MIN,
                getValueText(minKey),
                maxKey,
                EditorSetting.CHARACTER_EDITOR_LABEL_SHAPE_MAX,
                getValueText(maxKey)));
    }

    // A bone slider a control drives: where it stands now, adjusted to set a key, its Increase flag and its own range
    private void addDrive(
            AppearanceData appearanceData,
            ObjectArrayList<CharacterViewRowStruct> rows,
            int controlIndex,
            int driveIndex) {

        int sliderIndex = appearanceData.getShape(controlIndex).getDrive(driveIndex).getSliderIndex();
        AppearanceShapeStruct slider = appearanceData.getShape(sliderIndex);
        String adjustKey = toDriveKey(EditorSetting.CHARACTER_EDITOR_KEY_DRIVE_ADJUST, controlIndex, driveIndex);

        rows.add(CharacterViewRowStruct.ranged(
                adjustKey,
                EditorSetting.CHARACTER_EDITOR_LABEL_DRIVE_PREFIX + toLabel(slider),
                getValueText(adjustKey),
                getNumber(adjustKey),
                slider.getMin(),
                slider.getMax(),
                getStep(adjustKey)));
        rows.add(CharacterViewRowStruct.choice(
                toDriveKey(EditorSetting.CHARACTER_EDITOR_KEY_DRIVE_INCREASE, controlIndex, driveIndex),
                EditorSetting.CHARACTER_EDITOR_LABEL_DRIVE_INCREASE,
                toSwitchText(appearanceData.getShape(controlIndex).getDrive(driveIndex).isIncrease())));
        addRange(rows, sliderIndex);
    }

    // A slider's name as words, a bone slider's bone and channel apart
    private String toLabel(AppearanceShapeStruct shape) {

        String name = shape.getShapeName();
        int separator = name.lastIndexOf(EngineSetting.BONE_SLIDER_NAME_SEPARATOR);

        if (separator == EngineSetting.INDEX_NOT_FOUND)
            return LabelUtility.toWords(name);

        return LabelUtility.toWords(name.substring(0, separator)) + EditorSetting.CHARACTER_EDITOR_LABEL_SLIDER_JOIN
                + LabelUtility.toWords(name.substring(separator + EngineSetting.BONE_SLIDER_NAME_SEPARATOR.length()));
    }

    private String toSwitchText(boolean on) {
        return on ? EditorSetting.CHARACTER_EDITOR_SWITCH_ON : EditorSetting.CHARACTER_EDITOR_SWITCH_OFF;
    }

    private String toDriveKey(String keyName, int controlIndex, int driveIndex) {
        return CharacterViewUtility.toKey(keyName, controlIndex * EditorSetting.CHARACTER_EDITOR_DRIVE_KEY_STRIDE
                + driveIndex);
    }

    // Values \\

    String getValueText(String key) {

        AppearanceData appearanceData = resolveAppearanceData();

        if (appearanceData == null || !hasTarget(appearanceData, key))
            return "";

        return switch (CharacterViewUtility.getKeyName(key)) {
            case EditorSetting.CHARACTER_EDITOR_KEY_SHAPE_PLAYER -> toSwitchText(
                    appearanceData.getShape(CharacterViewUtility.getKeyIndex(key)).isShownToPlayer());
            case EditorSetting.CHARACTER_EDITOR_KEY_DRIVE_INCREASE -> toSwitchText(resolveDrive(appearanceData, key)
                    .isIncrease());
            case EditorSetting.CHARACTER_EDITOR_KEY_SHAPE_PICK,
                    EditorSetting.CHARACTER_EDITOR_KEY_CONTROL_ADD,
                    EditorSetting.CHARACTER_EDITOR_KEY_CONTROL_REMOVE -> "";
            default -> CharacterViewUtility.formatNumber(getNumber(key));
        };
    }

    float getNumber(String key) {

        AppearanceData appearanceData = resolveAppearanceData();

        if (appearanceData == null || !hasTarget(appearanceData, key))
            return 0f;

        int index = CharacterViewUtility.getKeyIndex(key);
        short genderID = characterEditorManager.getPreviewGenderID();

        return switch (CharacterViewUtility.getKeyName(key)) {
            case EditorSetting.CHARACTER_EDITOR_KEY_SHAPE -> getStartingValue(appearanceData, index);
            case EditorSetting.CHARACTER_EDITOR_KEY_SHAPE_MIN -> appearanceData.getShapeMin(genderID, index);
            case EditorSetting.CHARACTER_EDITOR_KEY_SHAPE_MAX -> appearanceData.getShapeMax(genderID, index);
            case EditorSetting.CHARACTER_EDITOR_KEY_DRIVE_ADJUST -> resolveTotal(
                    appearanceData, resolveDrive(appearanceData, key).getSliderIndex());
            default -> 0f;
        };
    }

    // A slider's step: its own range in the editor's count of steps, never finer than the editor's precision
    float getStep(String key) {

        AppearanceData appearanceData = resolveAppearanceData();

        if (appearanceData == null || !hasTarget(appearanceData, key))
            return EditorSetting.CHARACTER_EDITOR_SHAPE_STEP;

        boolean drive = CharacterViewUtility.getKeyName(key).equals(EditorSetting.CHARACTER_EDITOR_KEY_DRIVE_ADJUST);
        AppearanceShapeStruct shape = drive
                ? appearanceData.getShape(resolveDrive(appearanceData, key).getSliderIndex())
                : appearanceData.getShape(CharacterViewUtility.getKeyIndex(key));

        return Math.max(
                1f / EditorSetting.CHARACTER_VIEW_DECIMAL_SCALE,
                CharacterViewUtility.round(
                        (shape.getMax() - shape.getMin()) / EditorSetting.CHARACTER_EDITOR_SLIDER_STEPS));
    }

    boolean isValueValid(String key, String text) {

        AppearanceData appearanceData = resolveAppearanceData();

        return appearanceData != null && hasTarget(appearanceData, key) && isNumberKey(key)
                && CharacterViewUtility.isNumber(text);
    }

    void setValue(String key, String text) {
        if (isValueValid(key, text))
            write(key, CharacterViewUtility.parseNumber(text));
    }

    void step(String key, int direction) {

        AppearanceData appearanceData = resolveAppearanceData();

        if (appearanceData != null && hasTarget(appearanceData, key) && isNumberKey(key))
            write(key, getNumber(key) + direction * getStep(key));
    }

    private boolean isNumberKey(String key) {
        return switch (CharacterViewUtility.getKeyName(key)) {
            case EditorSetting.CHARACTER_EDITOR_KEY_SHAPE,
                    EditorSetting.CHARACTER_EDITOR_KEY_SHAPE_MIN,
                    EditorSetting.CHARACTER_EDITOR_KEY_SHAPE_MAX,
                    EditorSetting.CHARACTER_EDITOR_KEY_DRIVE_ADJUST -> true;
            default -> false;
        };
    }

    private void write(String key, float value) {

        int index = CharacterViewUtility.getKeyIndex(key);

        switch (CharacterViewUtility.getKeyName(key)) {
            case EditorSetting.CHARACTER_EDITOR_KEY_SHAPE -> setStartingValue(index, value);
            case EditorSetting.CHARACTER_EDITOR_KEY_SHAPE_MIN -> setRangeEnd(index, false, value);
            case EditorSetting.CHARACTER_EDITOR_KEY_SHAPE_MAX -> setRangeEnd(index, true, value);
            default -> adjustDrive(key, value);
        }
    }

    // Choices \\

    void cycle(String key) {

        AppearanceData appearanceData = resolveAppearanceData();

        if (appearanceData == null || !hasTarget(appearanceData, key))
            return;

        String keyName = CharacterViewUtility.getKeyName(key);

        if (keyName.equals(EditorSetting.CHARACTER_EDITOR_KEY_SHAPE_PLAYER)) {

            AppearanceShapeStruct shape = appearanceData.getShape(CharacterViewUtility.getKeyIndex(key));
            characterEditBranch.setSliderPlayer(
                    characterEditorManager.getActiveDocument(), shape.getShapeName(), !shape.isShownToPlayer());
        }
        else if (keyName.equals(EditorSetting.CHARACTER_EDITOR_KEY_DRIVE_INCREASE)) {

            AppearanceDriveStruct drive = resolveDrive(appearanceData, key);
            characterControlBranch.setIncrease(
                    resolveControl(appearanceData, key).getShapeName(),
                    appearanceData.getShape(drive.getSliderIndex()).getShapeName(),
                    !drive.isIncrease());
        }
    }

    // A bone slider picked for the next control, or let go
    void select(String key) {

        AppearanceData appearanceData = resolveAppearanceData();

        if (appearanceData != null && hasTarget(appearanceData, key)
                && CharacterViewUtility.getKeyName(key).equals(EditorSetting.CHARACTER_EDITOR_KEY_SHAPE_PICK))
            characterControlBranch.togglePick(
                    appearanceData.getShape(CharacterViewUtility.getKeyIndex(key)).getShapeName());
    }

    // Actions \\

    void act(String key) {

        AppearanceData appearanceData = resolveAppearanceData();

        if (appearanceData == null || !hasTarget(appearanceData, key))
            return;

        if (CharacterViewUtility.getKeyName(key).equals(EditorSetting.CHARACTER_EDITOR_KEY_CONTROL_ADD))
            characterControlBranch.addControl(EngineSetting.APPEARANCE_SHAPE_GROUP_DEFAULT);
        else if (CharacterViewUtility.getKeyName(key).equals(EditorSetting.CHARACTER_EDITOR_KEY_CONTROL_REMOVE))
            characterControlBranch.removeControl(
                    appearanceData.getShape(CharacterViewUtility.getKeyIndex(key)).getShapeName());
    }

    // Edits \\

    // The value the game starts the previewed gender's slider at moved, the preview following it: the gender's own
    // where its preset names the slider, the race's default otherwise
    private void setStartingValue(int shapeIndex, float value) {

        AppearanceData appearanceData = resolveAppearanceData();
        AppearanceShapeStruct shape = appearanceData.getShape(shapeIndex);
        short genderID = characterEditorManager.getPreviewGenderID();
        float rounded = CharacterViewUtility.round(CharacterViewUtility.clamp(
                value,
                appearanceData.getShapeMin(genderID, shapeIndex),
                appearanceData.getShapeMax(genderID, shapeIndex)));
        CharacterDocumentInstance document = characterEditorManager.getActiveDocument();

        if (findGenderShapes(shape) != null)
            characterEditBranch.setGenderShape(document, getPreviewGenderName(), shape.getShapeName(), rounded);
        else
            characterEditBranch.setSliderField(
                    document, shape.getShapeName(), EditorSetting.CHARACTER_EDITOR_FIELD_DEFAULT, rounded);

        characterEditorManager.setShapeValue(shapeIndex, Float.NaN);
    }

    // One end of the range the previewed gender's player may slide a slider across moved, a step from the other end;
    // the slider's own range is widened to take an end past it
    private void setRangeEnd(int shapeIndex, boolean upper, float value) {

        AppearanceData appearanceData = resolveAppearanceData();
        AppearanceShapeStruct shape = appearanceData.getShape(shapeIndex);
        short genderID = characterEditorManager.getPreviewGenderID();
        String key = CharacterViewUtility.toKey(EditorSetting.CHARACTER_EDITOR_KEY_SHAPE, shapeIndex);
        float step = getStep(key);
        float min = appearanceData.getShapeMin(genderID, shapeIndex);
        float max = appearanceData.getShapeMax(genderID, shapeIndex);
        CharacterDocumentInstance document = characterEditorManager.getActiveDocument();

        if (upper)
            max = CharacterViewUtility.round(Math.max(value, min + step));
        else
            min = CharacterViewUtility.round(Math.min(value, max - step));

        if (max > shape.getMax())
            characterEditBranch.setSliderField(document, shape.getShapeName(), EditorSetting.CHARACTER_EDITOR_FIELD_MAX,
                    max);

        if (min < shape.getMin())
            characterEditBranch.setSliderField(document, shape.getShapeName(), EditorSetting.CHARACTER_EDITOR_FIELD_MIN,
                    min);

        characterEditBranch.setGenderRange(document, getPreviewGenderName(), shape.getShapeName(), min, max);
        characterEditorManager.setShapeValue(shapeIndex, Float.NaN);
    }

    // The bone slider a drive moves set to stand at a value where the control now stands: the share its curve must
    // reach there worked out against the push it makes from zero and every other push on the slider, and keyed there
    private void adjustDrive(String key, float value) {

        AppearanceData appearanceData = resolveAppearanceData();
        AppearanceShapeStruct control = resolveControl(appearanceData, key);
        int controlIndex = CharacterViewUtility.getKeyIndex(key) / EditorSetting.CHARACTER_EDITOR_DRIVE_KEY_STRIDE;
        int driveIndex = CharacterViewUtility.getKeyIndex(key) % EditorSetting.CHARACTER_EDITOR_DRIVE_KEY_STRIDE;
        AppearanceDriveStruct drive = control.getDrive(driveIndex);
        AppearanceShapeStruct slider = appearanceData.getShape(drive.getSliderIndex());
        float controlValue = characterEditorManager.getShapeValue(controlIndex);
        float place = appearanceData.toPlace(controlIndex, controlValue);
        float zero = appearanceData.getZeroPlace(controlIndex);

        if (Math.abs(place - zero) <= EditorSetting.CHARACTER_CONTROL_KEY_REACH) {
            characterEditorManager.setStatusMessage(EditorSetting.CHARACTER_CONTROL_MESSAGE_AT_ZERO);
            return;
        }

        float target = slider.clamp(value);
        float others = resolveTotal(appearanceData, drive.getSliderIndex())
                - appearanceData.resolvePush(controlIndex, driveIndex, controlValue, curveScratch);
        float push = (target - others) / (slider.getMax() - slider.getMin()) * (drive.isIncrease() ? 1f : -1f);
        float[] keys = drive.getKeys();
        float share = 0f;

        for (int pass = 0; pass < EditorSetting.CHARACTER_CONTROL_SOLVE_PASSES; pass++) {

            share = CharacterViewUtility.clamp(
                    BoneControlCurveUtility.evaluate(keys, zero, curveScratch) + push, 0f, 1f);
            keys = characterControlBranch.placeKey(drive.getKeys(), place, share);
        }

        characterControlBranch.setKey(control.getShapeName(), slider.getShapeName(), place, share);
    }

    // Reading \\

    // Where a bone slider stands on the preview now: its own value and every control's push on it, held to its range
    private float resolveTotal(AppearanceData appearanceData, int sliderIndex) {

        float total = characterEditorManager.getShapeValue(sliderIndex);

        for (int c = 0; c < appearanceData.getShapeCount(); c++) {

            AppearanceShapeStruct control = appearanceData.getShape(c);

            for (int d = 0; d < control.getDriveCount(); d++)
                if (control.getDrive(d).getSliderIndex() == sliderIndex)
                    total += appearanceData.resolvePush(c, d, characterEditorManager.getShapeValue(c), curveScratch);
        }

        return appearanceData.getShape(sliderIndex).clamp(total);
    }

    // The value the game starts the previewed gender at on a slider: its preset's where it names the slider, else the
    // race's default
    private float getStartingValue(AppearanceData appearanceData, int shapeIndex) {

        AppearanceShapeStruct shape = appearanceData.getShape(shapeIndex);
        ArpgObjectStruct shapes = findGenderShapes(shape);

        return shapes != null
                ? ArpgUtility.getFloat(shapes, shape.getShapeName(), shape.getDefaultValue())
                : shape.getDefaultValue();
    }

    // The previewed gender's preset shapes where they name the slider, null otherwise
    private ArpgObjectStruct findGenderShapes(AppearanceShapeStruct shape) {

        ArpgObjectStruct shapes = CharacterViewUtility.findObject(
                CharacterViewUtility.findObject(
                        CharacterViewUtility.findObject(
                                characterEditorManager.getTemplateArpg(),
                                EditorSetting.CHARACTER_EDITOR_FIELD_GENDERS),
                        getPreviewGenderName()),
                EditorSetting.CHARACTER_EDITOR_FIELD_SHAPES);

        return shapes != null && ArpgUtility.hasNumber(shapes, shape.getShapeName()) ? shapes : null;
    }

    private String getPreviewGenderName() {
        return characterPropertyBranch.getPreviewGenderName();
    }

    // Whether a key names a slider, or a drive of a control, that the preview's race has
    private boolean hasTarget(AppearanceData appearanceData, String key) {

        String keyName = CharacterViewUtility.getKeyName(key);

        if (keyName.equals(EditorSetting.CHARACTER_EDITOR_KEY_CONTROL_ADD))
            return true;

        if (keyName.equals(EditorSetting.CHARACTER_EDITOR_KEY_DRIVE_ADJUST)
                || keyName.equals(EditorSetting.CHARACTER_EDITOR_KEY_DRIVE_INCREASE)) {

            AppearanceShapeStruct control = resolveControl(appearanceData, key);
            int driveIndex = CharacterViewUtility.getKeyIndex(key) % EditorSetting.CHARACTER_EDITOR_DRIVE_KEY_STRIDE;

            return control != null && driveIndex < control.getDriveCount();
        }

        int index = CharacterViewUtility.getKeyIndex(key);

        return index >= 0 && index < appearanceData.getShapeCount();
    }

    private AppearanceShapeStruct resolveControl(AppearanceData appearanceData, String key) {

        int controlIndex = CharacterViewUtility.getKeyIndex(key) / EditorSetting.CHARACTER_EDITOR_DRIVE_KEY_STRIDE;

        return controlIndex >= 0 && controlIndex < appearanceData.getShapeCount()
                && appearanceData.getShape(controlIndex).isControl()
                        ? appearanceData.getShape(controlIndex)
                        : null;
    }

    private AppearanceDriveStruct resolveDrive(AppearanceData appearanceData, String key) {
        return resolveControl(appearanceData, key)
                .getDrive(CharacterViewUtility.getKeyIndex(key) % EditorSetting.CHARACTER_EDITOR_DRIVE_KEY_STRIDE);
    }

    private AppearanceData resolveAppearanceData() {

        CharacterDocumentInstance document = characterEditorManager.getActiveDocument();

        return document != null && document.hasPreview() && document.getPreviewData().hasAppearance()
                ? document.getPreviewData().getAppearanceData()
                : null;
    }
}
