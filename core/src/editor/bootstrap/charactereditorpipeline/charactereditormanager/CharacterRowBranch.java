package editor.bootstrap.charactereditorpipeline.charactereditormanager;

import application.bootstrap.entitypipeline.feature.FeatureSlot;
import application.bootstrap.geometrypipeline.rig.RigHandle;
import editor.bootstrap.charactereditorpipeline.characterdocument.CharacterDocumentInstance;
import editor.bootstrap.charactereditorpipeline.util.CharacterEditorColor;
import editor.bootstrap.charactereditorpipeline.util.CharacterEditorMode;
import editor.characterview.panel.CharacterViewRowStruct;
import editor.characterview.util.CharacterViewUtility;
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
     * left panel holds how the preview stands and what can be picked — the
     * features in Appearance, the bones in Build, the anchors in Equipment,
     * the worn parts and the bones in Mesh and Weights — and the right panel
     * the values of the mode and of what is picked, the mesh modes' from
     * CharacterMeshRowBranch. Every value shown is read through
     * CharacterPropertyBranch, the same path that edits it. Mesh mode holds
     * the bind pose, so it lists no pose.
     */

    // Internal
    private CharacterEditorManager characterEditorManager;
    private CharacterPropertyBranch characterPropertyBranch;
    private CharacterMeshRowBranch characterMeshRowBranch;

    // Base \\

    @Override
    protected void get() {
        this.characterEditorManager = get(CharacterEditorManager.class);
        this.characterPropertyBranch = get(CharacterPropertyBranch.class);
        this.characterMeshRowBranch = get(CharacterMeshRowBranch.class);
    }

    // List \\

    void buildListRows(CharacterDocumentInstance document, ObjectArrayList<CharacterViewRowStruct> rows) {

        CharacterEditorMode mode = characterEditorManager.getMode();

        if (mode != CharacterEditorMode.MESH) {
            rows.add(CharacterViewRowStruct.header(EditorSetting.CHARACTER_EDITOR_HEADER_PREVIEW));
            addChoice(document, rows, EditorSetting.CHARACTER_EDITOR_KEY_POSE,
                    EditorSetting.CHARACTER_EDITOR_LABEL_POSE);
        }

        RigHandle rigHandle = characterPropertyBranch.resolveRig(document);

        if (rigHandle == null)
            return;

        switch (mode) {
            case APPEARANCE -> addFeatureRows(document, rows);
            case BUILD -> addBoneRows(rigHandle, rows);
            case EQUIPMENT -> addAnchorRows(rows);
            case MESH, WEIGHTS -> {
                characterMeshRowBranch.addPartRows(document, rows);
                addBoneRows(rigHandle, rows);
            }
        }
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
            case APPEARANCE -> addColorRows(document, rows);
            case BUILD -> addBuildRows(document, rows);
            case EQUIPMENT -> addAnchorPropertyRows(document, rows);
            case MESH -> characterMeshRowBranch.buildMeshRows(rows);
            case WEIGHTS -> characterMeshRowBranch.buildWeightRows(rows);
        }
    }

    private void addColorRows(CharacterDocumentInstance document, ObjectArrayList<CharacterViewRowStruct> rows) {

        if (characterPropertyBranch.getAppearanceArpg() == null)
            return;

        for (CharacterEditorColor color : CharacterEditorColor.VALUES) {

            rows.add(CharacterViewRowStruct.header(color.getLabel()));
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

    private void addBuildRows(CharacterDocumentInstance document, ObjectArrayList<CharacterViewRowStruct> rows) {

        boolean appearance = characterPropertyBranch.getAppearanceArpg() != null;

        if (appearance) {
            rows.add(CharacterViewRowStruct.header(EditorSetting.CHARACTER_EDITOR_HEADER_BUILD_CURVE));
            addNumber(document, rows, EditorSetting.CHARACTER_EDITOR_KEY_THIN,
                    EditorSetting.CHARACTER_EDITOR_LABEL_THIN);
            addNumber(document, rows, EditorSetting.CHARACTER_EDITOR_KEY_HEAVY,
                    EditorSetting.CHARACTER_EDITOR_LABEL_HEAVY);
        }

        rows.add(CharacterViewRowStruct.header(EditorSetting.CHARACTER_EDITOR_HEADER_PREVIEW_BODY));
        addNumber(document, rows, EditorSetting.CHARACTER_EDITOR_KEY_WEIGHT,
                EditorSetting.CHARACTER_EDITOR_LABEL_WEIGHT);
        addAxes(document, rows, EditorSetting.CHARACTER_EDITOR_KEY_HEAD, EditorSetting.CHARACTER_EDITOR_LABEL_HEAD);

        if (!appearance)
            return;

        RigHandle rigHandle = characterPropertyBranch.resolveRig(document);

        String boneName = rigHandle.getBone(characterEditorManager.getSelectedBoneIndex()).getName();

        rows.add(CharacterViewRowStruct.header(boneName));
        addNumber(document, rows, EditorSetting.CHARACTER_EDITOR_KEY_GIRTH, EditorSetting.CHARACTER_EDITOR_LABEL_GIRTH);
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

    private void addChoice(
            CharacterDocumentInstance document,
            ObjectArrayList<CharacterViewRowStruct> rows,
            String key,
            String label) {

        rows.add(CharacterViewRowStruct.choice(key, label, characterPropertyBranch.getValueText(document, key)));
    }
}
