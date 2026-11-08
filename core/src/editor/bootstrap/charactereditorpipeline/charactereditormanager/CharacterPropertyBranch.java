package editor.bootstrap.charactereditorpipeline.charactereditormanager;

import application.bootstrap.entitypipeline.appearance.AppearanceData;
import application.bootstrap.entitypipeline.appearance.AppearanceShapeStruct;
import application.bootstrap.entitypipeline.race.RaceData;
import application.bootstrap.entitypipeline.feature.FeatureHandle;
import application.bootstrap.entitypipeline.feature.FeatureSlot;
import application.bootstrap.entitypipeline.featuremanager.FeatureManager;
import application.bootstrap.entitypipeline.inventory.EquipmentSlot;
import application.bootstrap.geometrypipeline.rig.RigHandle;
import application.bootstrap.geometrypipeline.skinnedbuffer.SkinnedDetail;
import application.bootstrap.progressionpipeline.gendermanager.GenderManager;
import application.runtime.RuntimeSetting;
import editor.bootstrap.charactereditorpipeline.characterdocument.CharacterDocumentInstance;
import editor.bootstrap.charactereditorpipeline.util.CharacterEditorColor;
import editor.bootstrap.charactereditorpipeline.util.CharacterEditorPose;
import editor.bootstrap.faceeditorpipeline.faceeditormanager.FaceEditorManager;
import editor.runtime.characterview.util.CharacterViewUtility;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.mathematics.matrices.Matrix4;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class CharacterPropertyBranch extends BranchPackage {

    /*
     * The one place every value in the character editor's panels is read,
     * stepped, typed, cycled or acted on, by the key its row carries. Template
     * values are read from the live ARPG and written through
     * CharacterEditBranch, clamped to the range the game reads them in;
     * preview values — the pose, the gender, the tone worn, the build and
     * head shape — live on the manager and never touch the race. A
     * slider's range is the Face Editor's to write, through the same path its
     * own range rows take, so both tools keep one rule for it. The
     * keys of the Mesh, Sculpt and Weights panels pass straight to
     * CharacterMeshPropertyBranch, and those of the Rig panels to
     * CharacterRigPropertyBranch.
     */

    // Internal
    private CharacterEditorManager characterEditorManager;
    private CharacterEditBranch characterEditBranch;
    private CharacterMeshPropertyBranch characterMeshPropertyBranch;
    private CharacterRigPropertyBranch characterRigPropertyBranch;
    private FaceEditorManager faceEditorManager;
    private FeatureManager featureManager;
    private GenderManager genderManager;

    // Base \\

    @Override
    protected void get() {
        this.characterEditorManager = get(CharacterEditorManager.class);
        this.characterEditBranch = get(CharacterEditBranch.class);
        this.characterMeshPropertyBranch = get(CharacterMeshPropertyBranch.class);
        this.characterRigPropertyBranch = get(CharacterRigPropertyBranch.class);
        this.faceEditorManager = get(FaceEditorManager.class);
        this.featureManager = get(FeatureManager.class);
        this.genderManager = get(GenderManager.class);
    }

    // Selection \\

    void select(CharacterDocumentInstance document, String key) {

        String keyName = CharacterViewUtility.getKeyName(key);
        int index = CharacterViewUtility.getKeyIndex(key);

        if (characterMeshPropertyBranch.handles(keyName))
            characterMeshPropertyBranch.select(key);
        else if (characterRigPropertyBranch.handles(keyName))
            characterRigPropertyBranch.select(key);
        else if (keyName.equals(EditorSetting.CHARACTER_EDITOR_KEY_BONE))
            characterEditorManager.selectBone(index);
        else if (keyName.equals(EditorSetting.CHARACTER_EDITOR_KEY_ANCHOR))
            characterEditorManager.selectAnchor(index);
    }

    // Values \\

    String getValueText(CharacterDocumentInstance document, String key) {

        if (isMeshKey(key))
            return characterMeshPropertyBranch.getValueText(key);

        if (isRigKey(key))
            return characterRigPropertyBranch.getValueText(key);

        if (isNumberKey(key))
            return CharacterViewUtility.formatNumber(getNumber(document, key));

        return getChoiceText(document, key);
    }

    boolean isValueValid(CharacterDocumentInstance document, String key, String text) {

        if (isMeshKey(key))
            return characterMeshPropertyBranch.isValueValid(key, text);

        if (isRigKey(key))
            return characterRigPropertyBranch.isValueValid(key, text);

        return isNumberKey(key) && hasTarget(document, key) && CharacterViewUtility.isNumber(text);
    }

    void setValue(CharacterDocumentInstance document, String key, String text) {

        if (isMeshKey(key)) {
            characterMeshPropertyBranch.setValue(key, text);
            return;
        }

        if (isRigKey(key)) {
            characterRigPropertyBranch.setValue(key, text);
            return;
        }

        writeNumber(document, key, CharacterViewUtility.parseNumber(text));
    }

    void step(CharacterDocumentInstance document, String key, int direction) {

        if (isMeshKey(key)) {
            characterMeshPropertyBranch.step(key, direction);
            return;
        }

        if (isRigKey(key)) {
            characterRigPropertyBranch.step(key, direction);
            return;
        }

        if (isNumberKey(key) && hasTarget(document, key))
            writeNumber(document, key, getNumber(document, key) + direction * getStep(key));
    }

    private boolean isMeshKey(String key) {
        return characterMeshPropertyBranch.handles(CharacterViewUtility.getKeyName(key));
    }

    private boolean isRigKey(String key) {
        return characterRigPropertyBranch.handles(CharacterViewUtility.getKeyName(key));
    }

    // Numbers \\

    private boolean isNumberKey(String key) {

        String keyName = CharacterViewUtility.getKeyName(key);
        CharacterEditorColor color = CharacterEditorColor.fromKeyName(keyName);

        if (color != null)
            return keyName.equals(color.getColorKey()) || keyName.equals(color.getToneColorKey());

        return keyName.equals(EditorSetting.CHARACTER_EDITOR_KEY_THIN)
                || keyName.equals(EditorSetting.CHARACTER_EDITOR_KEY_HEAVY)
                || keyName.equals(EditorSetting.CHARACTER_EDITOR_KEY_DETAIL)
                || keyName.equals(EditorSetting.CHARACTER_EDITOR_KEY_SHAPE)
                || keyName.equals(EditorSetting.CHARACTER_EDITOR_KEY_SHAPE_MIN)
                || keyName.equals(EditorSetting.CHARACTER_EDITOR_KEY_SHAPE_MAX)
                || keyName.equals(EditorSetting.CHARACTER_EDITOR_KEY_BLINK)
                || keyName.equals(EditorSetting.CHARACTER_EDITOR_KEY_WEIGHT)
                || keyName.equals(EditorSetting.CHARACTER_EDITOR_KEY_HEAD)
                || keyName.equals(EditorSetting.CHARACTER_EDITOR_KEY_GIRTH)
                || keyName.equals(EditorSetting.CHARACTER_EDITOR_KEY_ANCHOR_POSITION)
                || keyName.equals(EditorSetting.CHARACTER_EDITOR_KEY_ANCHOR_ROTATION)
                || keyName.equals(EditorSetting.CHARACTER_EDITOR_KEY_ANCHOR_SIZE);
    }

    // A key names a field of an appearance, a tone, a bone or an anchor that has to exist
    private boolean hasTarget(CharacterDocumentInstance document, String key) {

        String keyName = CharacterViewUtility.getKeyName(key);
        CharacterEditorColor color = CharacterEditorColor.fromKeyName(keyName);
        boolean appearance = getAppearanceArpg() != null;

        if (color != null && keyName.equals(color.getToneColorKey()))
            return appearance && characterEditorManager.getToneIndex(color) != EngineSetting.INDEX_NOT_FOUND;

        if (color != null)
            return appearance;

        return switch (keyName) {
            case EditorSetting.CHARACTER_EDITOR_KEY_THIN,
                    EditorSetting.CHARACTER_EDITOR_KEY_HEAVY,
                    EditorSetting.CHARACTER_EDITOR_KEY_DETAIL -> appearance;
            case EditorSetting.CHARACTER_EDITOR_KEY_SHAPE,
                    EditorSetting.CHARACTER_EDITOR_KEY_SHAPE_MIN,
                    EditorSetting.CHARACTER_EDITOR_KEY_SHAPE_MAX -> resolveShape(key) != null;
            case EditorSetting.CHARACTER_EDITOR_KEY_GIRTH -> appearance && resolveRig(document) != null;
            case EditorSetting.CHARACTER_EDITOR_KEY_WEIGHT,
                    EditorSetting.CHARACTER_EDITOR_KEY_HEAD,
                    EditorSetting.CHARACTER_EDITOR_KEY_BLINK -> true;
            default -> getSelectedAnchor() != null;
        };
    }

    float getNumber(CharacterDocumentInstance document, String key) {

        String keyName = CharacterViewUtility.getKeyName(key);
        int axis = CharacterViewUtility.getKeyIndex(key);
        ArpgObjectStruct appearance = getAppearanceArpg();
        CharacterEditorColor color = CharacterEditorColor.fromKeyName(keyName);

        if (color != null && keyName.equals(color.getColorKey()))
            return CharacterViewUtility.getComponent(
                    CharacterViewUtility.findObject(appearance, EditorSetting.CHARACTER_EDITOR_FIELD_COLORS),
                    color.getField(),
                    axis,
                    EditorSetting.CHARACTER_EDITOR_COLOR_DEFAULT);

        if (color != null)
            return getToneArpg(color).get(axis).getAsFloat();

        return switch (keyName) {
            case EditorSetting.CHARACTER_EDITOR_KEY_THIN -> getBuildFactor(EditorSetting.CHARACTER_EDITOR_FIELD_THIN);
            case EditorSetting.CHARACTER_EDITOR_KEY_HEAVY -> getBuildFactor(EditorSetting.CHARACTER_EDITOR_FIELD_HEAVY);
            case EditorSetting.CHARACTER_EDITOR_KEY_DETAIL -> getDetail(SkinnedDetail.VALUES[axis]);
            case EditorSetting.CHARACTER_EDITOR_KEY_SHAPE -> resolveShape(key).getDefaultValue();
            case EditorSetting.CHARACTER_EDITOR_KEY_SHAPE_MIN -> resolveShape(key).getMin();
            case EditorSetting.CHARACTER_EDITOR_KEY_SHAPE_MAX -> resolveShape(key).getMax();
            case EditorSetting.CHARACTER_EDITOR_KEY_BLINK -> characterEditorManager.getBlink();
            case EditorSetting.CHARACTER_EDITOR_KEY_WEIGHT -> characterEditorManager.getWeightRatio();
            case EditorSetting.CHARACTER_EDITOR_KEY_HEAD -> getAxis(characterEditorManager.getHeadProportion(), axis);
            case EditorSetting.CHARACTER_EDITOR_KEY_GIRTH -> getGirth(document);
            case EditorSetting.CHARACTER_EDITOR_KEY_ANCHOR_POSITION -> CharacterViewUtility.getComponent(
                    getSelectedAnchor(), EditorSetting.CHARACTER_EDITOR_FIELD_POSITION, axis, 0f);
            case EditorSetting.CHARACTER_EDITOR_KEY_ANCHOR_ROTATION -> CharacterViewUtility.getComponent(
                    getSelectedAnchor(), EditorSetting.CHARACTER_EDITOR_FIELD_ROTATION, axis, 0f);
            default -> CharacterViewUtility.getComponent(
                    getSelectedAnchor(), EditorSetting.CHARACTER_EDITOR_FIELD_SIZE, axis,
                    EngineSetting.DEFAULT_ENTITY_SIZE);
        };
    }

    private void writeNumber(CharacterDocumentInstance document, String key, float value) {

        String keyName = CharacterViewUtility.getKeyName(key);
        int axis = CharacterViewUtility.getKeyIndex(key);
        CharacterEditorColor color = CharacterEditorColor.fromKeyName(keyName);
        float channel = CharacterViewUtility.clamp(value, 0f, 1f);

        if (color != null && keyName.equals(color.getColorKey())) {
            characterEditBranch.setColorChannel(document, color, axis, channel);
            return;
        }

        if (color != null) {
            characterEditBranch.setToneChannel(
                    document, color, characterEditorManager.getToneIndex(color), axis, channel);
            return;
        }

        switch (keyName) {
            case EditorSetting.CHARACTER_EDITOR_KEY_THIN -> characterEditBranch.setBuildFactor(
                    document, EditorSetting.CHARACTER_EDITOR_FIELD_THIN, clampBuildFactor(value));
            case EditorSetting.CHARACTER_EDITOR_KEY_HEAVY -> characterEditBranch.setBuildFactor(
                    document, EditorSetting.CHARACTER_EDITOR_FIELD_HEAVY, clampBuildFactor(value));
            case EditorSetting.CHARACTER_EDITOR_KEY_DETAIL -> characterEditBranch.setDetail(
                    document,
                    SkinnedDetail.VALUES[axis],
                    CharacterViewUtility.clamp(
                            value,
                            EditorSetting.CHARACTER_EDITOR_DETAIL_MIN,
                            EditorSetting.CHARACTER_EDITOR_DETAIL_MAX));
            case EditorSetting.CHARACTER_EDITOR_KEY_SHAPE -> setShapeDefault(axis, value);
            case EditorSetting.CHARACTER_EDITOR_KEY_SHAPE_MIN -> faceEditorManager.setRangeEnd(axis, false, value);
            case EditorSetting.CHARACTER_EDITOR_KEY_SHAPE_MAX -> faceEditorManager.setRangeEnd(axis, true, value);
            case EditorSetting.CHARACTER_EDITOR_KEY_BLINK -> characterEditorManager.setBlink(
                    CharacterViewUtility.round(channel));
            case EditorSetting.CHARACTER_EDITOR_KEY_WEIGHT -> characterEditorManager.setWeightRatio(
                    CharacterViewUtility.round(channel));
            case EditorSetting.CHARACTER_EDITOR_KEY_HEAD -> characterEditorManager.setHeadProportion(
                    axis,
                    CharacterViewUtility.round(CharacterViewUtility.clamp(
                            value,
                            RuntimeSetting.CREATOR_HEAD_PROPORTION_MIN,
                            RuntimeSetting.CREATOR_HEAD_PROPORTION_MAX)));
            case EditorSetting.CHARACTER_EDITOR_KEY_GIRTH -> characterEditBranch.setGirth(
                    document,
                    resolveRig(document).getBone(characterEditorManager.getSelectedBoneIndex()).getName(),
                    CharacterViewUtility.clamp(
                            value,
                            EditorSetting.CHARACTER_EDITOR_GIRTH_MIN,
                            EditorSetting.CHARACTER_EDITOR_GIRTH_MAX));
            case EditorSetting.CHARACTER_EDITOR_KEY_ANCHOR_POSITION -> characterEditBranch.setAnchorComponent(
                    document, characterEditorManager.getSelectedAnchorIndex(),
                    EditorSetting.CHARACTER_EDITOR_FIELD_POSITION, axis, value, 0f);
            case EditorSetting.CHARACTER_EDITOR_KEY_ANCHOR_ROTATION -> characterEditBranch.setAnchorComponent(
                    document, characterEditorManager.getSelectedAnchorIndex(),
                    EditorSetting.CHARACTER_EDITOR_FIELD_ROTATION, axis, value, 0f);
            default -> characterEditBranch.setAnchorComponent(
                    document, characterEditorManager.getSelectedAnchorIndex(),
                    EditorSetting.CHARACTER_EDITOR_FIELD_SIZE, axis,
                    Math.max(EditorSetting.CHARACTER_EDITOR_ANCHOR_SIZE_MIN, value),
                    EngineSetting.DEFAULT_ENTITY_SIZE);
        }
    }

    // The default the game starts the slider at moved, the preview following it
    private void setShapeDefault(int shapeIndex, float value) {

        faceEditorManager.setDefault(shapeIndex, CharacterViewUtility.round(value));
        characterEditorManager.setShapeValue(shapeIndex, Float.NaN);
    }

    float getStep(String key) {

        String keyName = CharacterViewUtility.getKeyName(key);

        if (CharacterEditorColor.fromKeyName(keyName) != null)
            return EditorSetting.CHARACTER_EDITOR_COLOR_STEP;

        return switch (keyName) {
            case EditorSetting.CHARACTER_EDITOR_KEY_THIN,
                    EditorSetting.CHARACTER_EDITOR_KEY_HEAVY -> EditorSetting.CHARACTER_EDITOR_BUILD_FACTOR_STEP;
            case EditorSetting.CHARACTER_EDITOR_KEY_WEIGHT -> EditorSetting.CHARACTER_EDITOR_WEIGHT_STEP;
            case EditorSetting.CHARACTER_EDITOR_KEY_DETAIL -> EditorSetting.CHARACTER_EDITOR_DETAIL_STEP;
            case EditorSetting.CHARACTER_EDITOR_KEY_SHAPE,
                    EditorSetting.CHARACTER_EDITOR_KEY_BLINK -> EditorSetting.CHARACTER_EDITOR_SHAPE_STEP;
            case EditorSetting.CHARACTER_EDITOR_KEY_SHAPE_MIN,
                    EditorSetting.CHARACTER_EDITOR_KEY_SHAPE_MAX -> EditorSetting.FACE_EDITOR_RANGE_STEP;
            case EditorSetting.CHARACTER_EDITOR_KEY_HEAD -> EditorSetting.CHARACTER_EDITOR_HEAD_STEP;
            case EditorSetting.CHARACTER_EDITOR_KEY_GIRTH -> EditorSetting.CHARACTER_EDITOR_GIRTH_STEP;
            case EditorSetting.CHARACTER_EDITOR_KEY_ANCHOR_POSITION -> EditorSetting.CHARACTER_EDITOR_POSITION_STEP;
            case EditorSetting.CHARACTER_EDITOR_KEY_ANCHOR_ROTATION -> EditorSetting.CHARACTER_EDITOR_ROTATION_STEP;
            default -> EditorSetting.CHARACTER_EDITOR_SIZE_STEP;
        };
    }

    private float clampBuildFactor(float value) {
        return CharacterViewUtility.clamp(
                value,
                EditorSetting.CHARACTER_EDITOR_BUILD_FACTOR_MIN,
                EditorSetting.CHARACTER_EDITOR_BUILD_FACTOR_MAX);
    }

    private float getBuildFactor(String factorField) {

        ArpgObjectStruct build = CharacterViewUtility.findObject(
                getAppearanceArpg(),
                EditorSetting.CHARACTER_EDITOR_FIELD_BUILD);

        return build != null
                ? ArpgUtility.getFloat(build, factorField, EngineSetting.DEFAULT_BUILD_FACTOR)
                : EngineSetting.DEFAULT_BUILD_FACTOR;
    }

    private float getDetail(SkinnedDetail skinnedDetail) {

        ArpgObjectStruct details = CharacterViewUtility.findObject(
                getAppearanceArpg(),
                EditorSetting.CHARACTER_EDITOR_FIELD_DETAILS);
        float defaultValue = EngineSetting.APPEARANCE_DETAIL_DEFAULTS[skinnedDetail.ordinal()];

        return details != null
                ? ArpgUtility.getFloat(details, ArpgUtility.toEnumName(skinnedDetail), defaultValue)
                : defaultValue;
    }

    // The slider a shape key names, null when the preview's race has none at that index
    private AppearanceShapeStruct resolveShape(String key) {

        AppearanceData appearanceData = resolveAppearanceData();
        int shapeIndex = CharacterViewUtility.getKeyIndex(key);

        return appearanceData != null && shapeIndex >= 0 && shapeIndex < appearanceData.getShapeCount()
                ? appearanceData.getShape(shapeIndex)
                : null;
    }

    private float getGirth(CharacterDocumentInstance document) {

        ArpgObjectStruct bones = CharacterViewUtility.findObject(
                CharacterViewUtility.findObject(getAppearanceArpg(), EditorSetting.CHARACTER_EDITOR_FIELD_BUILD),
                EditorSetting.CHARACTER_EDITOR_FIELD_BONES);
        String boneName = resolveRig(document).getBone(characterEditorManager.getSelectedBoneIndex()).getName();

        return bones != null ? ArpgUtility.getFloat(bones, boneName, 0f) : 0f;
    }

    private float getAxis(Vector3 vector, int axis) {
        return axis == 0 ? vector.x : axis == 1 ? vector.y : vector.z;
    }

    // Choices \\

    void cycle(CharacterDocumentInstance document, String key, int direction) {

        if (isMeshKey(key)) {
            characterMeshPropertyBranch.cycle(key, direction);
            return;
        }

        if (isRigKey(key)) {
            characterRigPropertyBranch.cycle(key, direction);
            return;
        }

        String keyName = CharacterViewUtility.getKeyName(key);
        CharacterEditorColor color = CharacterEditorColor.fromKeyName(keyName);

        if (color != null && keyName.equals(color.getToneKey())) {
            cycleTone(color, direction);
            return;
        }

        switch (keyName) {
            case EditorSetting.CHARACTER_EDITOR_KEY_POSE -> characterEditorManager.setPose(CharacterEditorPose.VALUES[
                    Math.floorMod(characterEditorManager.getPose().ordinal() + direction,
                            CharacterEditorPose.VALUES.length)]);
            case EditorSetting.CHARACTER_EDITOR_KEY_GENDER -> cycleGender(document, direction);
            case EditorSetting.CHARACTER_EDITOR_KEY_FEATURE -> cycleFeature(
                    document, ArpgUtility.toEnum(CharacterViewUtility.getKeyArgument(key), FeatureSlot.class),
                    direction);
            case EditorSetting.CHARACTER_EDITOR_KEY_ANCHOR_SLOT -> cycleAnchorSlot(document, direction);
            case EditorSetting.CHARACTER_EDITOR_KEY_ANCHOR_BONE -> cycleAnchorBone(document, direction);
            case EditorSetting.CHARACTER_EDITOR_KEY_ANCHOR_HOLD -> toggleAnchorHold(document);
            default -> {
            }
        }
    }

    private String getChoiceText(CharacterDocumentInstance document, String key) {

        String keyName = CharacterViewUtility.getKeyName(key);
        CharacterEditorColor color = CharacterEditorColor.fromKeyName(keyName);

        if (color != null && keyName.equals(color.getToneKey()))
            return getToneText(color);

        ArpgObjectStruct anchor = getSelectedAnchor();

        return switch (keyName) {
            case EditorSetting.CHARACTER_EDITOR_KEY_POSE -> characterEditorManager.getPose().getLabel();
            case EditorSetting.CHARACTER_EDITOR_KEY_GENDER -> genderManager
                    .getGenderHandleFromGenderID(characterEditorManager.getPreviewGenderID())
                    .getDisplayName();
            case EditorSetting.CHARACTER_EDITOR_KEY_FEATURE -> getFeatureText(CharacterViewUtility.getKeyArgument(key));
            case EditorSetting.CHARACTER_EDITOR_KEY_ANCHOR_SLOT -> anchor != null
                    ? ArpgUtility.getString(anchor, EditorSetting.CHARACTER_EDITOR_FIELD_SLOT, "")
                    : "";
            case EditorSetting.CHARACTER_EDITOR_KEY_ANCHOR_BONE -> anchor != null
                    ? ArpgUtility.getString(anchor, EditorSetting.CHARACTER_EDITOR_FIELD_BONE, "")
                    : "";
            case EditorSetting.CHARACTER_EDITOR_KEY_ANCHOR_HOLD -> isAnchorHeld(anchor)
                    ? EditorSetting.CHARACTER_VIEW_YES
                    : EditorSetting.CHARACTER_VIEW_NO;
            default -> "";
        };
    }

    // Genders \\

    private void cycleGender(CharacterDocumentInstance document, int direction) {

        int count = document.hasPreview() ? document.getPreviewData().getGenderCount() : 0;

        if (count > 0)
            characterEditorManager.setGenderIndex(Math.floorMod(characterEditorManager.getGenderIndex() + direction,
                    count));
    }

    // Tones \\

    // The tone worn steps from the default color through every palette tone and back
    private void cycleTone(CharacterEditorColor color, int direction) {

        ArpgArrayStruct palette = getPaletteArpg(color);
        int count = palette != null ? palette.size() : 0;
        int next = Math.floorMod(characterEditorManager.getToneIndex(color) + 1 + direction, count + 1) - 1;

        characterEditorManager.setToneIndex(color, next);
    }

    private String getToneText(CharacterEditorColor color) {

        int toneIndex = characterEditorManager.getToneIndex(color);

        if (toneIndex == EngineSetting.INDEX_NOT_FOUND)
            return EditorSetting.CHARACTER_EDITOR_TONE_DEFAULT;

        return (toneIndex + 1) + EditorSetting.CHARACTER_VIEW_COUNT_SEPARATOR + getPaletteArpg(color).size();
    }

    private ArpgArrayStruct getPaletteArpg(CharacterEditorColor color) {

        ArpgObjectStruct palettes = CharacterViewUtility.findObject(
                getAppearanceArpg(),
                EditorSetting.CHARACTER_EDITOR_FIELD_PALETTES);

        return palettes != null && ArpgUtility.hasArray(palettes, color.getField())
                ? palettes.getAsArray(color.getField())
                : null;
    }

    private ArpgArrayStruct getToneArpg(CharacterEditorColor color) {
        return getPaletteArpg(color).get(characterEditorManager.getToneIndex(color)).getAsArray();
    }

    // A new tone copies the color worn, and is worn at once
    private void addTone(CharacterDocumentInstance document, CharacterEditorColor color) {

        if (getAppearanceArpg() == null)
            return;

        String colorKey = characterEditorManager.getToneIndex(color) == EngineSetting.INDEX_NOT_FOUND
                ? color.getColorKey()
                : color.getToneColorKey();

        characterEditBranch.addTone(
                document,
                color,
                getNumber(document, CharacterViewUtility.toKey(colorKey, 0)),
                getNumber(document, CharacterViewUtility.toKey(colorKey, 1)),
                getNumber(document, CharacterViewUtility.toKey(colorKey, 2)));
        characterEditorManager.setToneIndex(color, getPaletteArpg(color).size() - 1);
    }

    private void removeTone(CharacterDocumentInstance document, CharacterEditorColor color) {

        int toneIndex = characterEditorManager.getToneIndex(color);

        if (toneIndex == EngineSetting.INDEX_NOT_FOUND)
            return;

        characterEditBranch.removeTone(document, color, toneIndex);

        ArpgArrayStruct palette = getPaletteArpg(color);
        int remaining = palette != null ? palette.size() : 0;

        characterEditorManager.setToneIndex(color, Math.min(toneIndex, remaining - 1));
    }

    // Features \\

    // The features the preview could wear in a slot, by name: the race's default first, then none where the default
    // wears something, so a gender can go bald, then every feature that fits
    private ObjectArrayList<String> getFeatureOptions(FeatureSlot featureSlot) {

        ObjectArrayList<String> options = new ObjectArrayList<>();
        AppearanceData appearanceData = resolveAppearanceData();
        ObjectArrayList<FeatureHandle> features = featureManager.getFeatureHandlesForSlot(featureSlot);

        for (int i = 0; i < features.size(); i++)
            if (appearanceData == null || appearanceData.isCompatible(features.get(i)))
                options.add(features.get(i).getFeatureName());

        options.sort(String.CASE_INSENSITIVE_ORDER);

        if (isWorn(findFeatureName(getAppearanceArpg(), featureSlot)))
            options.add(0, EngineSetting.APPEARANCE_FEATURE_NONE);

        options.add(0, null);

        return options;
    }

    private void cycleFeature(CharacterDocumentInstance document, FeatureSlot featureSlot, int direction) {

        ObjectArrayList<String> options = getFeatureOptions(featureSlot);

        if (getAppearanceArpg() == null || options.isEmpty())
            return;

        int current = options.indexOf(getGenderFeatureName(featureSlot));
        int next = Math.floorMod(current + direction, options.size());

        characterEditBranch.setFeature(document, getPreviewGenderName(), featureSlot, options.get(next));
    }

    // The feature the previewed gender names for itself in a slot, null when it wears the race's default
    private String getGenderFeatureName(FeatureSlot featureSlot) {
        return findFeatureName(
                CharacterViewUtility.findObject(
                        CharacterViewUtility.findObject(
                                characterEditorManager.getTemplateArpg(),
                                EditorSetting.CHARACTER_EDITOR_FIELD_GENDERS),
                        getPreviewGenderName()),
                featureSlot);
    }

    private String findFeatureName(ArpgObjectStruct owner, FeatureSlot featureSlot) {

        ArpgObjectStruct features = CharacterViewUtility.findObject(
                owner,
                EditorSetting.CHARACTER_EDITOR_FIELD_FEATURES);
        String slotKey = ArpgUtility.toEnumName(featureSlot);

        return features != null && ArpgUtility.hasString(features, slotKey)
                ? features.get(slotKey).getAsString()
                : null;
    }

    private String getPreviewGenderName() {
        return genderManager.getGenderHandleFromGenderID(characterEditorManager.getPreviewGenderID()).getGenderName();
    }

    // The previewed gender's own feature, else the race's default marked as such, else none
    private String getFeatureText(String slotKey) {

        FeatureSlot featureSlot = ArpgUtility.toEnum(slotKey, FeatureSlot.class);
        String featureName = getGenderFeatureName(featureSlot);
        String defaultName = findFeatureName(getAppearanceArpg(), featureSlot);

        if (featureName == null && isWorn(defaultName))
            return defaultName + EditorSetting.CHARACTER_EDITOR_FEATURE_DEFAULT_SUFFIX;

        return isWorn(featureName) ? featureName : EditorSetting.CHARACTER_VIEW_NONE;
    }

    // Whether a slot's feature name wears something, neither left out nor named none
    private boolean isWorn(String featureName) {
        return featureName != null && !featureName.equals(EngineSetting.APPEARANCE_FEATURE_NONE);
    }

    // Anchors \\

    private void cycleAnchorSlot(CharacterDocumentInstance document, int direction) {

        ArpgObjectStruct anchor = getSelectedAnchor();

        if (anchor == null)
            return;

        EquipmentSlot current = CharacterViewUtility.toEnumOrDefault(
                ArpgUtility.getString(anchor, EditorSetting.CHARACTER_EDITOR_FIELD_SLOT, null),
                EquipmentSlot.class,
                EquipmentSlot.VALUES[0]);

        characterEditBranch.setAnchorSlot(
                document,
                characterEditorManager.getSelectedAnchorIndex(),
                EquipmentSlot.VALUES[Math.floorMod(current.ordinal() + direction, EquipmentSlot.VALUES.length)]);
    }

    private void cycleAnchorBone(CharacterDocumentInstance document, int direction) {

        ArpgObjectStruct anchor = getSelectedAnchor();
        RigHandle rigHandle = resolveRig(document);

        if (anchor == null || rigHandle == null)
            return;

        String boneName = ArpgUtility.getString(anchor, EditorSetting.CHARACTER_EDITOR_FIELD_BONE, "");
        int current = rigHandle.hasBone(boneName) ? rigHandle.getBoneIndex(boneName) : 0;
        int next = Math.floorMod(current + direction, rigHandle.getBoneCount());

        characterEditBranch.setAnchorBone(
                document,
                characterEditorManager.getSelectedAnchorIndex(),
                rigHandle.getBone(next).getName());
    }

    private void toggleAnchorHold(CharacterDocumentInstance document) {

        ArpgObjectStruct anchor = getSelectedAnchor();

        if (anchor != null)
            characterEditBranch.setAnchorHold(
                    document,
                    characterEditorManager.getSelectedAnchorIndex(),
                    !isAnchorHeld(anchor));
    }

    private boolean isAnchorHeld(ArpgObjectStruct anchor) {
        return anchor != null && ArpgUtility.getBoolean(anchor, EditorSetting.CHARACTER_EDITOR_FIELD_HOLD, false);
    }

    // A new anchor takes the first slot no anchor fills yet, at the joint of the bone selected or the root
    private void addAnchor(CharacterDocumentInstance document) {

        RigHandle rigHandle = resolveRig(document);

        if (rigHandle == null)
            return;

        EquipmentSlot equipmentSlot = resolveFreeSlot();
        int boneIndex = characterEditorManager.getSelectedBoneIndex();
        Matrix4 joint = rigHandle.getBindWorldMatrix(boneIndex);
        float size = EditorSetting.CHARACTER_EDITOR_ANCHOR_SIZE_DEFAULT;
        ArpgObjectStruct anchor = new ArpgObjectStruct();

        anchor.addProperty(EditorSetting.CHARACTER_EDITOR_FIELD_SLOT, ArpgUtility.toEnumName(equipmentSlot));
        anchor.addProperty(EditorSetting.CHARACTER_EDITOR_FIELD_BONE, rigHandle.getBone(boneIndex).getName());
        anchor.add(EditorSetting.CHARACTER_EDITOR_FIELD_POSITION,
                CharacterViewUtility.toArray(joint.getM03(), joint.getM13(), joint.getM23()));
        anchor.add(EditorSetting.CHARACTER_EDITOR_FIELD_ROTATION, CharacterViewUtility.toArray(0f, 0f, 0f));

        if (equipmentSlot.isHand())
            anchor.addProperty(EditorSetting.CHARACTER_EDITOR_FIELD_HOLD, true);
        else
            anchor.add(EditorSetting.CHARACTER_EDITOR_FIELD_SIZE, CharacterViewUtility.toArray(size, size, size));

        characterEditBranch.addAnchor(document, anchor);
        characterEditorManager.selectAnchor(getAnchorCount() - 1);
    }

    private EquipmentSlot resolveFreeSlot() {

        ArpgArrayStruct anchors = getAnchorsArpg();

        for (EquipmentSlot equipmentSlot : EquipmentSlot.VALUES)
            if (!isSlotAnchored(anchors, equipmentSlot))
                return equipmentSlot;

        return EquipmentSlot.VALUES[0];
    }

    private boolean isSlotAnchored(ArpgArrayStruct anchors, EquipmentSlot equipmentSlot) {

        if (anchors == null)
            return false;

        String slotName = ArpgUtility.toEnumName(equipmentSlot);

        for (int i = 0; i < anchors.size(); i++)
            if (slotName.equals(ArpgUtility.getString(
                    anchors.get(i).getAsObject(), EditorSetting.CHARACTER_EDITOR_FIELD_SLOT, "")))
                return true;

        return false;
    }

    private void duplicateAnchor(CharacterDocumentInstance document) {

        ArpgObjectStruct anchor = getSelectedAnchor();

        if (anchor == null)
            return;

        characterEditBranch.addAnchor(document, anchor.deepCopy());
        characterEditorManager.selectAnchor(getAnchorCount() - 1);
    }

    private void removeAnchor(CharacterDocumentInstance document) {

        int anchorIndex = characterEditorManager.getSelectedAnchorIndex();

        if (getSelectedAnchor() == null)
            return;

        characterEditBranch.removeAnchor(document, anchorIndex);
        characterEditorManager.selectAnchor(Math.min(anchorIndex, getAnchorCount() - 1));
    }

    // Actions \\

    void act(CharacterDocumentInstance document, String key) {

        if (isMeshKey(key)) {
            characterMeshPropertyBranch.act(key);
            return;
        }

        if (isRigKey(key)) {
            characterRigPropertyBranch.act(key);
            return;
        }

        String keyName = CharacterViewUtility.getKeyName(key);
        CharacterEditorColor color = CharacterEditorColor.fromKeyName(keyName);

        if (color != null && keyName.equals(color.getAddToneKey())) {
            addTone(document, color);
            return;
        }

        if (color != null && keyName.equals(color.getRemoveToneKey())) {
            removeTone(document, color);
            return;
        }

        switch (keyName) {
            case EditorSetting.CHARACTER_EDITOR_KEY_ADD_ANCHOR -> addAnchor(document);
            case EditorSetting.CHARACTER_EDITOR_KEY_DUPLICATE_ANCHOR -> duplicateAnchor(document);
            case EditorSetting.CHARACTER_EDITOR_KEY_REMOVE_ANCHOR -> removeAnchor(document);
            default -> {
            }
        }
    }

    // Race \\

    ArpgObjectStruct getAppearanceArpg() {
        return CharacterViewUtility.findObject(
                characterEditorManager.getTemplateArpg(),
                EditorSetting.CHARACTER_EDITOR_FIELD_APPEARANCE);
    }

    ArpgArrayStruct getAnchorsArpg() {

        ArpgObjectStruct race = characterEditorManager.getTemplateArpg();

        return race != null && ArpgUtility.hasArray(race, EditorSetting.CHARACTER_EDITOR_FIELD_EQUIPMENT)
                ? race.getAsArray(EditorSetting.CHARACTER_EDITOR_FIELD_EQUIPMENT)
                : null;
    }

    int getAnchorCount() {

        ArpgArrayStruct anchors = getAnchorsArpg();
        return anchors != null ? anchors.size() : 0;
    }

    ArpgObjectStruct getSelectedAnchor() {

        int anchorIndex = characterEditorManager.getSelectedAnchorIndex();

        return anchorIndex >= 0 && anchorIndex < getAnchorCount()
                ? getAnchorsArpg().get(anchorIndex).getAsObject()
                : null;
    }

    // Preview \\

    RigHandle resolveRig(CharacterDocumentInstance document) {
        return document.hasPreview() ? document.getPreviewData().getRigHandle() : null;
    }

    private AppearanceData resolveAppearanceData() {

        CharacterDocumentInstance document = characterEditorManager.getActiveDocument();

        if (document == null || !document.hasPreview())
            return null;

        RaceData raceData = document.getPreviewData();
        return raceData.hasAppearance() ? raceData.getAppearanceData() : null;
    }
}
