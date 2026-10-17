package editor.bootstrap.charactereditorpipeline.charactereditormanager;

import application.bootstrap.entitypipeline.appearance.AppearanceData;
import application.bootstrap.entitypipeline.entity.EntityData;
import application.bootstrap.entitypipeline.feature.FeatureHandle;
import application.bootstrap.entitypipeline.feature.FeatureSlot;
import application.bootstrap.entitypipeline.featuremanager.FeatureManager;
import application.bootstrap.entitypipeline.inventory.EquipmentSlot;
import application.bootstrap.geometrypipeline.rig.RigHandle;
import application.runtime.RuntimeSetting;
import editor.bootstrap.charactereditorpipeline.characterdocument.CharacterDocumentInstance;
import editor.bootstrap.charactereditorpipeline.util.CharacterEditorColor;
import editor.bootstrap.charactereditorpipeline.util.CharacterEditorPose;
import editor.characterview.util.CharacterViewUtility;
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
     * preview values — the pose, the tone worn, the build and head shape —
     * live on the manager and never touch the template.
     */

    // Internal
    private CharacterEditorManager characterEditorManager;
    private CharacterEditBranch characterEditBranch;
    private FeatureManager featureManager;

    // Base \\

    @Override
    protected void get() {
        this.characterEditorManager = get(CharacterEditorManager.class);
        this.characterEditBranch = get(CharacterEditBranch.class);
        this.featureManager = get(FeatureManager.class);
    }

    // Selection \\

    void select(CharacterDocumentInstance document, String key) {

        String keyName = CharacterViewUtility.getKeyName(key);
        int index = CharacterViewUtility.getKeyIndex(key);

        if (keyName.equals(EditorSetting.CHARACTER_EDITOR_KEY_BONE))
            characterEditorManager.selectBone(index);
        else if (keyName.equals(EditorSetting.CHARACTER_EDITOR_KEY_ANCHOR))
            characterEditorManager.selectAnchor(index);
    }

    // Values \\

    String getValueText(CharacterDocumentInstance document, String key) {

        if (isNumberKey(key))
            return CharacterViewUtility.formatNumber(getNumber(document, key));

        return getChoiceText(document, key);
    }

    boolean isValueValid(CharacterDocumentInstance document, String key, String text) {
        return isNumberKey(key) && hasTarget(document, key) && CharacterViewUtility.isNumber(text);
    }

    void setValue(CharacterDocumentInstance document, String key, String text) {
        writeNumber(document, key, CharacterViewUtility.parseNumber(text));
    }

    void step(CharacterDocumentInstance document, String key, int direction) {

        if (isNumberKey(key) && hasTarget(document, key))
            writeNumber(document, key, getNumber(document, key) + direction * getStep(key));
    }

    // Numbers \\

    private boolean isNumberKey(String key) {

        String keyName = CharacterViewUtility.getKeyName(key);
        CharacterEditorColor color = CharacterEditorColor.fromKeyName(keyName);

        if (color != null)
            return keyName.equals(color.getColorKey()) || keyName.equals(color.getToneColorKey());

        return keyName.equals(EditorSetting.CHARACTER_EDITOR_KEY_THIN)
                || keyName.equals(EditorSetting.CHARACTER_EDITOR_KEY_HEAVY)
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
                    EditorSetting.CHARACTER_EDITOR_KEY_HEAVY -> appearance;
            case EditorSetting.CHARACTER_EDITOR_KEY_GIRTH -> appearance && resolveRig(document) != null;
            case EditorSetting.CHARACTER_EDITOR_KEY_WEIGHT,
                    EditorSetting.CHARACTER_EDITOR_KEY_HEAD -> true;
            default -> getSelectedAnchor() != null;
        };
    }

    private float getNumber(CharacterDocumentInstance document, String key) {

        String keyName = CharacterViewUtility.getKeyName(key);
        int axis = CharacterViewUtility.getKeyIndex(key);
        ArpgObjectStruct appearance = getAppearanceArpg();
        CharacterEditorColor color = CharacterEditorColor.fromKeyName(keyName);

        if (color != null && keyName.equals(color.getColorKey()))
            return CharacterViewUtility.getComponent(
                    appearance, color.getColorField(), axis, EditorSetting.CHARACTER_EDITOR_COLOR_DEFAULT);

        if (color != null)
            return getToneArpg(color).get(axis).getAsFloat();

        return switch (keyName) {
            case EditorSetting.CHARACTER_EDITOR_KEY_THIN -> getBuildFactor(EditorSetting.CHARACTER_EDITOR_FIELD_THIN);
            case EditorSetting.CHARACTER_EDITOR_KEY_HEAVY -> getBuildFactor(EditorSetting.CHARACTER_EDITOR_FIELD_HEAVY);
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

    private float getStep(String key) {

        String keyName = CharacterViewUtility.getKeyName(key);

        if (CharacterEditorColor.fromKeyName(keyName) != null)
            return EditorSetting.CHARACTER_EDITOR_COLOR_STEP;

        return switch (keyName) {
            case EditorSetting.CHARACTER_EDITOR_KEY_THIN,
                    EditorSetting.CHARACTER_EDITOR_KEY_HEAVY -> EditorSetting.CHARACTER_EDITOR_BUILD_FACTOR_STEP;
            case EditorSetting.CHARACTER_EDITOR_KEY_WEIGHT -> EditorSetting.CHARACTER_EDITOR_WEIGHT_STEP;
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

        ArpgObjectStruct appearance = getAppearanceArpg();

        return appearance != null && ArpgUtility.hasArray(appearance, color.getPaletteField())
                ? appearance.getAsArray(color.getPaletteField())
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

    // The features the preview could wear in a slot, by name, with none first where the slot may stay empty
    private ObjectArrayList<String> getFeatureOptions(FeatureSlot featureSlot) {

        ObjectArrayList<String> options = new ObjectArrayList<>();
        AppearanceData appearanceData = resolveAppearanceData();
        ObjectArrayList<FeatureHandle> features = featureManager.getFeatureHandlesForSlot(featureSlot);

        for (int i = 0; i < features.size(); i++)
            if (appearanceData == null || appearanceData.isCompatible(features.get(i)))
                options.add(features.get(i).getFeatureName());

        options.sort(String.CASE_INSENSITIVE_ORDER);

        if (!featureSlot.isRequired())
            options.add(0, null);

        return options;
    }

    private void cycleFeature(CharacterDocumentInstance document, FeatureSlot featureSlot, int direction) {

        ObjectArrayList<String> options = getFeatureOptions(featureSlot);

        if (getAppearanceArpg() == null || options.isEmpty())
            return;

        int current = options.indexOf(getFeatureName(featureSlot));
        int next = Math.floorMod(current + direction, options.size());

        characterEditBranch.setFeature(document, featureSlot, options.get(next));
    }

    private String getFeatureName(FeatureSlot featureSlot) {

        ArpgObjectStruct features = CharacterViewUtility.findObject(
                getAppearanceArpg(),
                EditorSetting.CHARACTER_EDITOR_FIELD_FEATURES);
        String slotKey = ArpgUtility.toEnumName(featureSlot);

        return features != null && ArpgUtility.hasString(features, slotKey)
                ? features.get(slotKey).getAsString()
                : null;
    }

    private String getFeatureText(String slotKey) {

        String featureName = getFeatureName(ArpgUtility.toEnum(slotKey, FeatureSlot.class));
        return featureName != null ? featureName : EditorSetting.CHARACTER_VIEW_NONE;
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

    // Template \\

    ArpgObjectStruct getAppearanceArpg() {
        return CharacterViewUtility.findObject(getModelArpg(), EditorSetting.CHARACTER_EDITOR_FIELD_APPEARANCE);
    }

    ArpgArrayStruct getAnchorsArpg() {

        ArpgObjectStruct model = getModelArpg();

        return model != null && ArpgUtility.hasArray(model, EditorSetting.CHARACTER_EDITOR_FIELD_EQUIPMENT)
                ? model.getAsArray(EditorSetting.CHARACTER_EDITOR_FIELD_EQUIPMENT)
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

    private ArpgObjectStruct getModelArpg() {
        return CharacterViewUtility.findObject(
                characterEditorManager.getTemplateArpg(),
                EditorSetting.CHARACTER_EDITOR_FIELD_MODEL);
    }

    // Preview \\

    RigHandle resolveRig(CharacterDocumentInstance document) {
        return document.hasPreview() ? document.getPreviewData().getRigHandle() : null;
    }

    private AppearanceData resolveAppearanceData() {

        CharacterDocumentInstance document = characterEditorManager.getActiveDocument();

        if (document == null || !document.hasPreview())
            return null;

        EntityData entityData = document.getPreviewData();
        return entityData.hasAppearance() ? entityData.getAppearanceData() : null;
    }
}
