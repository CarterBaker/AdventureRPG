package editor.bootstrap.charactereditorpipeline.charactereditormanager;

import java.util.function.Consumer;

import application.bootstrap.entitypipeline.feature.FeatureSlot;
import application.bootstrap.entitypipeline.inventory.EquipmentSlot;
import editor.bootstrap.charactereditorpipeline.characterdocument.CharacterDocumentInstance;
import editor.bootstrap.charactereditorpipeline.util.CharacterEditorColor;
import editor.bootstrap.infopipeline.infomanager.InfoManager;
import editor.characterview.util.CharacterViewUtility;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.arpg.ArpgValueStruct;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class CharacterEditBranch extends BranchPackage {

    /*
     * Performs every change the character editor makes to a template, each
     * through InfoManager so the Info Panel, the preview and every tab see
     * it at once. Edits write only the fields they change and keep every
     * other field and its order. A palette emptied of its last tone is
     * removed, so the template falls back to its default color.
     */

    // Internal
    private InfoManager infoManager;

    // Base \\

    @Override
    protected void get() {
        this.infoManager = get(InfoManager.class);
    }

    // Template \\

    private void edit(CharacterDocumentInstance document, Consumer<ArpgObjectStruct> editor) {
        infoManager.editEntry(
                EditorSetting.INFO_SCHEMA_ENTITIES,
                document.getTemplateName(),
                document.getTemplateName(),
                editor);
    }

    private void editModel(CharacterDocumentInstance document, Consumer<ArpgObjectStruct> editor) {
        edit(document, arpg -> editor.accept(
                CharacterViewUtility.requireObject(arpg, EditorSetting.CHARACTER_EDITOR_FIELD_MODEL)));
    }

    private void editAppearance(CharacterDocumentInstance document, Consumer<ArpgObjectStruct> editor) {
        editModel(document, model -> editor.accept(
                CharacterViewUtility.requireObject(model, EditorSetting.CHARACTER_EDITOR_FIELD_APPEARANCE)));
    }

    private void editAnchor(CharacterDocumentInstance document, int anchorIndex, Consumer<ArpgObjectStruct> editor) {
        editModel(document, model -> editor.accept(CharacterViewUtility
                .requireArray(model, EditorSetting.CHARACTER_EDITOR_FIELD_EQUIPMENT)
                .get(anchorIndex)
                .getAsObject()));
    }

    // A copy of every field, in order, as a new template beside the source
    void copyTemplate(ArpgObjectStruct sourceArpg, String templateName) {
        infoManager.createDocument(EditorSetting.INFO_SCHEMA_ENTITIES, templateName, root -> {

            ObjectArrayList<String> defaultKeys = new ObjectArrayList<>(root.keySet());

            for (int i = 0; i < defaultKeys.size(); i++)
                root.remove(defaultKeys.get(i));

            for (String key : sourceArpg.keySet())
                root.add(key, sourceArpg.get(key).deepCopy());
        });
    }

    // Features \\

    void setFeature(CharacterDocumentInstance document, FeatureSlot featureSlot, String featureName) {
        editAppearance(document, appearance -> {

            ArpgObjectStruct features = CharacterViewUtility.requireObject(
                    appearance,
                    EditorSetting.CHARACTER_EDITOR_FIELD_FEATURES);
            String slotKey = ArpgUtility.toEnumName(featureSlot);

            if (featureName == null)
                features.remove(slotKey);
            else
                features.addProperty(slotKey, featureName);
        });
    }

    // Colors \\

    void setColorChannel(CharacterDocumentInstance document, CharacterEditorColor color, int channel, float value) {
        editAppearance(document, appearance -> CharacterViewUtility.setArrayComponent(
                appearance,
                color.getColorField(),
                channel,
                value,
                EditorSetting.CHARACTER_EDITOR_COLOR_DEFAULT));
    }

    void setToneChannel(
            CharacterDocumentInstance document,
            CharacterEditorColor color,
            int toneIndex,
            int channel,
            float value) {

        editAppearance(document, appearance -> appearance.getAsArray(color.getPaletteField())
                .get(toneIndex)
                .getAsArray()
                .set(channel, new ArpgValueStruct(CharacterViewUtility.round(value))));
    }

    void addTone(CharacterDocumentInstance document, CharacterEditorColor color, float r, float g, float b) {
        editAppearance(document, appearance -> CharacterViewUtility
                .requireArray(appearance, color.getPaletteField())
                .add(CharacterViewUtility.toArray(r, g, b)));
    }

    void removeTone(CharacterDocumentInstance document, CharacterEditorColor color, int toneIndex) {
        editAppearance(document, appearance -> {

            ArpgArrayStruct palette = appearance.getAsArray(color.getPaletteField());
            palette.remove(toneIndex);

            if (palette.isEmpty())
                appearance.remove(color.getPaletteField());
        });
    }

    // Build \\

    void setBuildFactor(CharacterDocumentInstance document, String factorField, float value) {
        editAppearance(document, appearance -> CharacterViewUtility
                .requireObject(appearance, EditorSetting.CHARACTER_EDITOR_FIELD_BUILD)
                .addProperty(factorField, CharacterViewUtility.round(value)));
    }

    void setGirth(CharacterDocumentInstance document, String boneName, float value) {
        editAppearance(document, appearance -> CharacterViewUtility
                .requireObject(
                        CharacterViewUtility.requireObject(appearance, EditorSetting.CHARACTER_EDITOR_FIELD_BUILD),
                        EditorSetting.CHARACTER_EDITOR_FIELD_BONES)
                .addProperty(boneName, CharacterViewUtility.round(value)));
    }

    // Equipment \\

    void setAnchorComponent(
            CharacterDocumentInstance document,
            int anchorIndex,
            String vectorField,
            int axis,
            float value,
            float defaultValue) {

        editAnchor(document, anchorIndex, anchor -> CharacterViewUtility.setArrayComponent(
                anchor, vectorField, axis, value, defaultValue));
    }

    void setAnchorSlot(CharacterDocumentInstance document, int anchorIndex, EquipmentSlot equipmentSlot) {
        editAnchor(document, anchorIndex, anchor -> anchor.addProperty(
                EditorSetting.CHARACTER_EDITOR_FIELD_SLOT,
                ArpgUtility.toEnumName(equipmentSlot)));
    }

    void setAnchorBone(CharacterDocumentInstance document, int anchorIndex, String boneName) {
        editAnchor(document, anchorIndex, anchor -> anchor.addProperty(
                EditorSetting.CHARACTER_EDITOR_FIELD_BONE,
                boneName));
    }

    // A held anchor says so; one that is not leaves the field out, as the default
    void setAnchorHold(CharacterDocumentInstance document, int anchorIndex, boolean hold) {
        editAnchor(document, anchorIndex, anchor -> {

            if (hold)
                anchor.addProperty(EditorSetting.CHARACTER_EDITOR_FIELD_HOLD, true);
            else
                anchor.remove(EditorSetting.CHARACTER_EDITOR_FIELD_HOLD);
        });
    }

    void addAnchor(CharacterDocumentInstance document, ArpgObjectStruct anchorArpg) {
        editModel(document, model -> CharacterViewUtility
                .requireArray(model, EditorSetting.CHARACTER_EDITOR_FIELD_EQUIPMENT)
                .add(anchorArpg));
    }

    void removeAnchor(CharacterDocumentInstance document, int anchorIndex) {
        editModel(document, model -> CharacterViewUtility
                .requireArray(model, EditorSetting.CHARACTER_EDITOR_FIELD_EQUIPMENT)
                .remove(anchorIndex));
    }
}
