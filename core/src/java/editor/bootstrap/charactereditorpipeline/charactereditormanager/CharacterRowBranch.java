package editor.bootstrap.charactereditorpipeline.charactereditormanager;

import application.bootstrap.entitypipeline.appearance.AppearanceData;
import application.bootstrap.entitypipeline.appearance.AppearanceOptionKind;
import application.bootstrap.entitypipeline.appearance.AppearanceShapeStruct;
import application.bootstrap.entitypipeline.feature.FeatureSlot;
import application.bootstrap.geometrypipeline.rig.RigHandle;
import application.bootstrap.entitypipeline.race.RaceGenderStruct;
import application.bootstrap.geometrypipeline.skinnedbuffer.SkinnedDetail;
import application.bootstrap.geometrypipeline.skinnedbuffer.SkinnedOverlay;
import editor.bootstrap.charactereditorpipeline.characterdocument.CharacterDocumentInstance;
import editor.bootstrap.charactereditorpipeline.util.CharacterEditorColor;
import editor.bootstrap.charactereditorpipeline.util.CharacterEditorMode;
import editor.runtime.characterview.panel.CharacterViewRowStruct;
import editor.runtime.characterview.util.CharacterViewUtility;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class CharacterRowBranch extends BranchPackage {

    /*
     * Lists the rows of the character editor's two panels for its mode. The
     * left panel holds how the preview stands and what can be picked — in
     * Appearance everything the player is offered in the game's creator,
     * whether the race is playable, the features and marks worn (each mark
     * with how far it shows), every color with its tones, the details not
     * shown by a mark, each with whether the player is offered it, and
     * every slider under its group, each sliding across the previewed
     * gender's range with that range's two ends beneath it (Blink alone, a
     * preview control, stays on the right), the bones in
     * Build, the anchors in Equipment,
     * the worn parts and the bones in Mesh and Weights, the worn parts in
     * Sculpt and the rig's bones in Rig — and the right panel the values of
     * the mode and of what is picked, the mesh modes' from
     * CharacterMeshRowBranch and the rig's from CharacterRigRowBranch. Every
     * value shown is read through CharacterPropertyBranch, the same path that
     * edits it. Only a mode whose outliner picks the pose lists it — Bind,
     * the rig's own poses, then the movements; a race of more than one
     * gender lists the gender the preview is made as, and a part editor
     * whose part is sided the side it shows, its edits mirrored to the
     * other.
     */

    // Internal
    private CharacterEditorManager characterEditorManager;
    private CharacterPropertyBranch characterPropertyBranch;
    private CharacterMeshRowBranch characterMeshRowBranch;
    private CharacterRigRowBranch characterRigRowBranch;
    private CharacterPaintRowBranch characterPaintRowBranch;
    private CharacterUVBranch characterUVBranch;
    private CharacterSliderBranch characterSliderBranch;

    // Base \\

    @Override
    protected void get() {
        this.characterEditorManager = get(CharacterEditorManager.class);
        this.characterPropertyBranch = get(CharacterPropertyBranch.class);
        this.characterMeshRowBranch = get(CharacterMeshRowBranch.class);
        this.characterRigRowBranch = get(CharacterRigRowBranch.class);
        this.characterPaintRowBranch = get(CharacterPaintRowBranch.class);
        this.characterUVBranch = get(CharacterUVBranch.class);
        this.characterSliderBranch = get(CharacterSliderBranch.class);
    }

    // List \\

    void buildListRows(CharacterDocumentInstance document, ObjectArrayList<CharacterViewRowStruct> rows) {

        CharacterEditorMode mode = characterEditorManager.getMode();
        boolean genders = document.hasPreview() && document.getPreviewData().getGenderCount() > 1;
        boolean sided = mode.isModeling() && characterEditorManager.isSided(characterEditorManager.getRegion());

        if (mode.picksPose() || genders || sided)
            rows.add(CharacterViewRowStruct.header(EditorSetting.CHARACTER_EDITOR_HEADER_PREVIEW));

        if (genders)
            addChoice(document, rows, EditorSetting.CHARACTER_EDITOR_KEY_GENDER,
                    EditorSetting.CHARACTER_EDITOR_LABEL_GENDER);

        if (mode.picksPose())
            addChoice(document, rows, EditorSetting.CHARACTER_EDITOR_KEY_POSE,
                    EditorSetting.CHARACTER_EDITOR_LABEL_POSE);

        if (sided)
            addChoice(document, rows, EditorSetting.CHARACTER_EDITOR_KEY_SIDE,
                    EditorSetting.CHARACTER_EDITOR_LABEL_SIDE);

        RigHandle rigHandle = characterPropertyBranch.resolveRig(document);

        if (rigHandle == null)
            return;

        switch (mode) {
            case APPEARANCE -> {
                addCreatorRows(document, rows);
                addFeatureRows(document, rows);
                addColorRows(document, rows);
                characterSliderBranch.buildRows(document, rows);
                addBuildRows(document, rows);
            }
            case EQUIPMENT -> addAnchorRows(rows);
            case MESH, WEIGHTS -> {
                characterMeshRowBranch.addPartRows(document, rows);
                addBoneRows(rigHandle, rows);
            }
            case SCULPT, PAINT, UV -> characterMeshRowBranch.addPartRows(document, rows);
            case RIG -> characterRigRowBranch.addBoneRows(rows);
        }
    }

    // Whether the game's creator offers the race at all
    private void addCreatorRows(CharacterDocumentInstance document, ObjectArrayList<CharacterViewRowStruct> rows) {
        rows.add(CharacterViewRowStruct.header(EditorSetting.CHARACTER_EDITOR_HEADER_CREATOR));
        addChoice(document, rows, EditorSetting.CHARACTER_EDITOR_KEY_PLAYABLE,
                EditorSetting.CHARACTER_EDITOR_LABEL_PLAYABLE);
    }

    private void addFeatureRows(CharacterDocumentInstance document, ObjectArrayList<CharacterViewRowStruct> rows) {

        if (characterPropertyBranch.getAppearanceArpg() == null) {
            rows.add(CharacterViewRowStruct.header(EditorSetting.CHARACTER_EDITOR_HEADER_NO_APPEARANCE));
            return;
        }

        rows.add(CharacterViewRowStruct.header(EditorSetting.CHARACTER_EDITOR_HEADER_FEATURES));

        for (FeatureSlot featureSlot : FeatureSlot.VALUES) {

            String slotName = ArpgUtility.toEnumName(featureSlot);

            addChoice(document, rows, CharacterViewUtility.toKey(EditorSetting.CHARACTER_EDITOR_KEY_FEATURE, slotName),
                    slotName);

            if (characterPropertyBranch.hasFeatureVariants(document, featureSlot))
                addChoice(document, rows,
                        CharacterViewUtility.toKey(EditorSetting.CHARACTER_EDITOR_KEY_FEATURE_VARIANT, slotName),
                        slotName + EditorSetting.CHARACTER_EDITOR_LABEL_VARIANT_SUFFIX);

            addOffered(document, rows, AppearanceOptionKind.FEATURE, featureSlot.ordinal());
        }

        addOverlayRows(document, rows);
    }

    // The style each overlay the previewed gender's model places starts in and how far it shows, as the creator offers
    // them
    private void addOverlayRows(CharacterDocumentInstance document, ObjectArrayList<CharacterViewRowStruct> rows) {

        RaceGenderStruct raceGender = document.getPreviewData().getGender(characterEditorManager.getPreviewGenderID());
        boolean headed = false;

        for (SkinnedOverlay skinnedOverlay : SkinnedOverlay.VALUES) {

            if (raceGender.getOverlay(skinnedOverlay) == null)
                continue;

            if (!headed) {
                rows.add(CharacterViewRowStruct.header(EditorSetting.CHARACTER_EDITOR_HEADER_OVERLAYS));
                headed = true;
            }

            String overlayName = ArpgUtility.toEnumName(skinnedOverlay);

            addChoice(document, rows,
                    CharacterViewUtility.toKey(EditorSetting.CHARACTER_EDITOR_KEY_OVERLAY_STYLE, overlayName),
                    overlayName);
            addNumber(document, rows,
                    CharacterViewUtility.toKey(
                            EditorSetting.CHARACTER_EDITOR_KEY_DETAIL, skinnedOverlay.getDetail().ordinal()),
                    EditorSetting.CHARACTER_EDITOR_LABEL_OVERLAY_AMOUNT);
            addOffered(document, rows, AppearanceOptionKind.OVERLAY, skinnedOverlay.ordinal());
        }
    }

    private void addBoneRows(RigHandle rigHandle, ObjectArrayList<CharacterViewRowStruct> rows) {

        rows.add(CharacterViewRowStruct.header(EditorSetting.CHARACTER_EDITOR_HEADER_BONES));

        for (int i = 0; i < rigHandle.getBoneCount(); i++)
            rows.add(CharacterViewRowStruct.item(
                    CharacterViewUtility.toKey(EditorSetting.CHARACTER_EDITOR_KEY_BONE, i),
                    rigHandle.getBone(i).getName(),
                    i == characterEditorManager.getSelectedBoneIndex()));
    }

    private void addAnchorRows(ObjectArrayList<CharacterViewRowStruct> rows) {

        rows.add(CharacterViewRowStruct.header(EditorSetting.CHARACTER_EDITOR_HEADER_ANCHORS));

        ArpgArrayStruct anchors = characterPropertyBranch.getAnchorsArpg();

        if (anchors == null)
            return;

        for (int i = 0; i < anchors.size(); i++) {

            ArpgObjectStruct anchor = anchors.get(i).getAsObject();
            String label = ArpgUtility.getString(anchor, EditorSetting.CHARACTER_EDITOR_FIELD_SLOT, "")
                    + EditorSetting.CHARACTER_VIEW_PAIR_SEPARATOR
                    + ArpgUtility.getString(anchor, EditorSetting.CHARACTER_EDITOR_FIELD_BONE, "");

            rows.add(CharacterViewRowStruct.item(
                    CharacterViewUtility.toKey(EditorSetting.CHARACTER_EDITOR_KEY_ANCHOR, i),
                    label,
                    i == characterEditorManager.getSelectedAnchorIndex()));
        }
    }

    // Properties \\

    void buildPropertyRows(CharacterDocumentInstance document, ObjectArrayList<CharacterViewRowStruct> rows) {

        if (!document.hasPreview())
            return;

        switch (characterEditorManager.getMode()) {
            case APPEARANCE -> addPreviewRows(document, rows);
            case EQUIPMENT -> addAnchorPropertyRows(document, rows);
            case MESH -> characterMeshRowBranch.buildMeshRows(rows);
            case SCULPT -> characterMeshRowBranch.buildSculptRows(rows);
            case WEIGHTS -> characterMeshRowBranch.buildWeightRows(rows);
            case PAINT -> characterPaintRowBranch.buildPropertyRows(rows);
            case RIG -> characterRigRowBranch.buildPropertyRows(rows);
            case UV -> characterUVBranch.buildPropertyRows(rows);
        }
    }

    // Every color the player picks, with the race's tones, then every detail amount, as the creator offers them
    private void addColorRows(CharacterDocumentInstance document, ObjectArrayList<CharacterViewRowStruct> rows) {

        if (characterPropertyBranch.getAppearanceArpg() == null)
            return;

        for (CharacterEditorColor color : CharacterEditorColor.VALUES) {

            rows.add(CharacterViewRowStruct.header(color.getLabel()));
            addOffered(document, rows, AppearanceOptionKind.COLOR, color.getSkinnedColor().ordinal());
            addChannels(document, rows, color.getColorKey(), "");
            addChoice(document, rows, color.getToneKey(), EditorSetting.CHARACTER_EDITOR_LABEL_TONE);

            boolean toneWorn = characterEditorManager.getToneIndex(color) != EngineSetting.INDEX_NOT_FOUND;

            if (toneWorn)
                addChannels(document, rows, color.getToneColorKey(), EditorSetting.CHARACTER_EDITOR_LABEL_TONE_PREFIX);

            rows.add(CharacterViewRowStruct.action(
                    color.getAddToneKey(),
                    EditorSetting.CHARACTER_EDITOR_LABEL_ADD_TONE));

            if (toneWorn)
                rows.add(CharacterViewRowStruct.action(
                        color.getRemoveToneKey(),
                        EditorSetting.CHARACTER_EDITOR_LABEL_REMOVE_TONE));
        }

        rows.add(CharacterViewRowStruct.header(EditorSetting.CHARACTER_EDITOR_HEADER_DETAILS));

        for (SkinnedDetail skinnedDetail : SkinnedDetail.VALUES) {

            if (SkinnedOverlay.findByDetail(skinnedDetail) != null)
                continue;

            addNumber(document, rows,
                    CharacterViewUtility.toKey(EditorSetting.CHARACTER_EDITOR_KEY_DETAIL, skinnedDetail.ordinal()),
                    EditorSetting.CHARACTER_EDITOR_DETAIL_LABELS[skinnedDetail.ordinal()]);
            addOffered(document, rows, AppearanceOptionKind.DETAIL, skinnedDetail.ordinal());
        }
    }

    // How the preview plays and stands while the look is set, nothing the player is given
    private void addPreviewRows(CharacterDocumentInstance document, ObjectArrayList<CharacterViewRowStruct> rows) {
        rows.add(CharacterViewRowStruct.header(EditorSetting.CHARACTER_EDITOR_HEADER_PREVIEW));
        addNumber(document, rows, EditorSetting.CHARACTER_EDITOR_KEY_BLINK, EditorSetting.CHARACTER_EDITOR_LABEL_BLINK);
        rows.add(CharacterViewRowStruct.header(EditorSetting.CHARACTER_EDITOR_HEADER_PREVIEW_BODY));
        addNumber(document, rows, EditorSetting.CHARACTER_EDITOR_KEY_WEIGHT,
                EditorSetting.CHARACTER_EDITOR_LABEL_WEIGHT);
        addAxes(document, rows, EditorSetting.CHARACTER_EDITOR_KEY_HEAD, EditorSetting.CHARACTER_EDITOR_LABEL_HEAD);
    }

    private void addChannels(
            CharacterDocumentInstance document,
            ObjectArrayList<CharacterViewRowStruct> rows,
            String keyName,
            String labelPrefix) {

        for (int channel = 0; channel < EditorSetting.CHARACTER_VIEW_CHANNEL_LABELS.length; channel++)
            addNumber(document, rows, CharacterViewUtility.toKey(keyName, channel),
                    labelPrefix + EditorSetting.CHARACTER_VIEW_CHANNEL_LABELS[channel]);
    }

    // The build curve the player's weight slides across, then the girth the selected bone takes from it, the bone
    // picked in the Rig or Weights tab
    private void addBuildRows(CharacterDocumentInstance document, ObjectArrayList<CharacterViewRowStruct> rows) {

        if (characterPropertyBranch.getAppearanceArpg() == null)
            return;

        rows.add(CharacterViewRowStruct.header(EditorSetting.CHARACTER_EDITOR_HEADER_BUILD_CURVE));
        addNumber(document, rows, EditorSetting.CHARACTER_EDITOR_KEY_THIN, EditorSetting.CHARACTER_EDITOR_LABEL_THIN);
        addNumber(document, rows, EditorSetting.CHARACTER_EDITOR_KEY_HEAVY,
                EditorSetting.CHARACTER_EDITOR_LABEL_HEAVY);

        RigHandle rigHandle = characterPropertyBranch.resolveRig(document);
        String boneName = rigHandle.getBone(characterEditorManager.getSelectedBoneIndex()).getName();

        addNumber(document, rows, EditorSetting.CHARACTER_EDITOR_KEY_GIRTH,
                EditorSetting.CHARACTER_EDITOR_LABEL_GIRTH + EditorSetting.CHARACTER_VIEW_STATUS_SEPARATOR + boneName);
    }

    private void addAnchorPropertyRows(
            CharacterDocumentInstance document,
            ObjectArrayList<CharacterViewRowStruct> rows) {

        ArpgObjectStruct anchor = characterPropertyBranch.getSelectedAnchor();

        rows.add(CharacterViewRowStruct.header(EditorSetting.CHARACTER_EDITOR_HEADER_ANCHOR));
        rows.add(CharacterViewRowStruct.action(
                EditorSetting.CHARACTER_EDITOR_KEY_ADD_ANCHOR,
                EditorSetting.CHARACTER_EDITOR_LABEL_ADD_ANCHOR));

        if (anchor == null)
            return;

        rows.add(CharacterViewRowStruct.action(
                EditorSetting.CHARACTER_EDITOR_KEY_DUPLICATE_ANCHOR,
                EditorSetting.CHARACTER_EDITOR_LABEL_DUPLICATE_ANCHOR));
        rows.add(CharacterViewRowStruct.action(
                EditorSetting.CHARACTER_EDITOR_KEY_REMOVE_ANCHOR,
                EditorSetting.CHARACTER_EDITOR_LABEL_REMOVE_ANCHOR));

        addChoice(document, rows, EditorSetting.CHARACTER_EDITOR_KEY_ANCHOR_SLOT,
                EditorSetting.CHARACTER_EDITOR_LABEL_SLOT);
        addChoice(document, rows, EditorSetting.CHARACTER_EDITOR_KEY_ANCHOR_BONE,
                EditorSetting.CHARACTER_EDITOR_LABEL_BONE);
        addChoice(document, rows, EditorSetting.CHARACTER_EDITOR_KEY_ANCHOR_HOLD,
                EditorSetting.CHARACTER_EDITOR_LABEL_HOLD);
        addAxes(document, rows, EditorSetting.CHARACTER_EDITOR_KEY_ANCHOR_POSITION,
                EditorSetting.CHARACTER_EDITOR_LABEL_POSITION);
        addAxes(document, rows, EditorSetting.CHARACTER_EDITOR_KEY_ANCHOR_ROTATION,
                EditorSetting.CHARACTER_EDITOR_LABEL_ROTATION);

        if (!ArpgUtility.getBoolean(anchor, EditorSetting.CHARACTER_EDITOR_FIELD_HOLD, false))
            addAxes(document, rows, EditorSetting.CHARACTER_EDITOR_KEY_ANCHOR_SIZE,
                    EditorSetting.CHARACTER_EDITOR_LABEL_SIZE);
    }

    // Utility \\

    private void addAxes(
            CharacterDocumentInstance document,
            ObjectArrayList<CharacterViewRowStruct> rows,
            String keyName,
            String label) {

        for (int axis = 0; axis < EditorSetting.CHARACTER_VIEW_AXIS_LABELS.length; axis++)
            addNumber(document, rows, CharacterViewUtility.toKey(keyName, axis),
                    label + EditorSetting.CHARACTER_VIEW_LABEL_SPACE + EditorSetting.CHARACTER_VIEW_AXIS_LABELS[axis]);
    }

    private void addNumber(
            CharacterDocumentInstance document,
            ObjectArrayList<CharacterViewRowStruct> rows,
            String key,
            String label) {

        rows.add(CharacterViewRowStruct.number(key, label, characterPropertyBranch.getValueText(document, key)));
    }

    // Whether the game's creator offers the player a color, detail, overlay or feature
    private void addOffered(
            CharacterDocumentInstance document,
            ObjectArrayList<CharacterViewRowStruct> rows,
            AppearanceOptionKind kind,
            int index) {

        addChoice(document, rows, CharacterPropertyBranch.toOfferedKey(kind, index),
                EditorSetting.CHARACTER_EDITOR_LABEL_SHAPE_PLAYER);
    }

    private void addChoice(
            CharacterDocumentInstance document,
            ObjectArrayList<CharacterViewRowStruct> rows,
            String key,
            String label) {

        rows.add(CharacterViewRowStruct.choice(key, label, characterPropertyBranch.getValueText(document, key)));
    }
}
