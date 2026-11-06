package editor.bootstrap.entityeditorpipeline.entityeditormanager;

import application.bootstrap.progressionpipeline.gendermanager.GenderManager;
import editor.bootstrap.entityeditorpipeline.util.EntityEditorChoice;
import editor.bootstrap.infopipeline.infoentry.InfoEntryStruct;
import editor.bootstrap.infopipeline.infomanager.InfoManager;
import editor.runtime.characterview.panel.CharacterViewRowStruct;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import engine.util.arpg.ArpgObjectStruct;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class EntityRowBranch extends BranchPackage {

    /*
     * Lists the rows of the Entity Editor: every entity, the picked one
     * highlighted, with New and Delete; the picked entity's race to choose,
     * copy or delete; the race's rig and its genders, the previewed one
     * highlighted, with Add and Remove; the previewed gender's model — mesh,
     * material and animation tree; Save and Revert for the entity and its
     * race together; and last the status line, so a message never moves the
     * rows above it. An entity or race with changes not yet saved is marked. Every value shown is read through
     * EntityPropertyBranch, the same path that writes it.
     */

    // Internal
    private InfoManager infoManager;
    private GenderManager genderManager;
    private EntityEditorManager entityEditorManager;
    private EntityPropertyBranch entityPropertyBranch;

    // Base \\

    @Override
    protected void get() {
        this.infoManager = get(InfoManager.class);
        this.genderManager = get(GenderManager.class);
        this.entityEditorManager = get(EntityEditorManager.class);
        this.entityPropertyBranch = get(EntityPropertyBranch.class);
    }

    // Rows \\

    void buildRows(ObjectArrayList<CharacterViewRowStruct> rows) {

        buildEntityRows(rows);

        if (entityEditorManager.getActiveEntityName() != null)
            buildActiveRows(rows);

        if (entityEditorManager.getStatusMessage() != null)
            rows.add(CharacterViewRowStruct.header(entityEditorManager.getStatusMessage()));
    }

    private void buildActiveRows(ObjectArrayList<CharacterViewRowStruct> rows) {

        buildRaceRows(rows);

        if (entityEditorManager.getGendersArpg() != null)
            buildGenderRows(rows);

        rows.add(CharacterViewRowStruct.action(
                EditorSetting.ENTITY_EDITOR_KEY_SAVE,
                EditorSetting.ENTITY_EDITOR_LABEL_SAVE));
        rows.add(CharacterViewRowStruct.action(
                EditorSetting.ENTITY_EDITOR_KEY_REVERT,
                EditorSetting.ENTITY_EDITOR_LABEL_REVERT));
    }

    private void buildEntityRows(ObjectArrayList<CharacterViewRowStruct> rows) {

        ObjectArrayList<InfoEntryStruct> entries = infoManager.getEntries(EditorSetting.INFO_SCHEMA_ENTITIES);
        String activeEntityName = entityEditorManager.getActiveEntityName();

        rows.add(CharacterViewRowStruct.header(EditorSetting.ENTITY_EDITOR_HEADER_ENTITIES));

        if (entries.isEmpty())
            rows.add(CharacterViewRowStruct.header(EditorSetting.ENTITY_EDITOR_HEADER_NO_ENTITIES));

        for (int i = 0; i < entries.size(); i++) {

            String entityName = entries.get(i).getEntryName();

            rows.add(CharacterViewRowStruct.item(
                    toKey(EditorSetting.ENTITY_EDITOR_KEY_ENTITY, entityName),
                    entityName,
                    entityName.equals(activeEntityName)));
        }

        rows.add(CharacterViewRowStruct.action(
                EditorSetting.ENTITY_EDITOR_KEY_NEW_ENTITY,
                EditorSetting.ENTITY_EDITOR_LABEL_NEW_ENTITY));

        if (activeEntityName != null)
            rows.add(CharacterViewRowStruct.action(
                    EditorSetting.ENTITY_EDITOR_KEY_DELETE_ENTITY,
                    EditorSetting.ENTITY_EDITOR_LABEL_DELETE_ENTITY));
    }

    private void buildRaceRows(ObjectArrayList<CharacterViewRowStruct> rows) {

        rows.add(CharacterViewRowStruct.header(toHeader(
                EditorSetting.ENTITY_EDITOR_HEADER_ENTITY,
                entityEditorManager.getActiveEntityName(),
                entityEditorManager.isEntityDirty())));
        addChoice(rows, EntityEditorChoice.RACE);

        if (entityEditorManager.getRaceArpg() == null) {
            rows.add(CharacterViewRowStruct.header(EditorSetting.ENTITY_EDITOR_HEADER_NO_RACE));
            return;
        }

        rows.add(CharacterViewRowStruct.action(
                EditorSetting.ENTITY_EDITOR_KEY_COPY_RACE,
                EditorSetting.ENTITY_EDITOR_LABEL_COPY_RACE));
        rows.add(CharacterViewRowStruct.action(
                EditorSetting.ENTITY_EDITOR_KEY_DELETE_RACE,
                EditorSetting.ENTITY_EDITOR_LABEL_DELETE_RACE));
    }

    private void buildGenderRows(ObjectArrayList<CharacterViewRowStruct> rows) {

        ArpgObjectStruct genders = entityEditorManager.getGendersArpg();
        String pickedGenderName = entityEditorManager.getPickedGenderName();

        rows.add(CharacterViewRowStruct.header(toHeader(
                EditorSetting.ENTITY_EDITOR_HEADER_RACE,
                entityEditorManager.getActiveRaceName(),
                entityEditorManager.isRaceDirty())));
        addChoice(rows, EntityEditorChoice.RIG);
        rows.add(CharacterViewRowStruct.header(EditorSetting.ENTITY_EDITOR_HEADER_GENDERS));

        for (String genderName : genders.keySet())
            rows.add(CharacterViewRowStruct.item(
                    toKey(EditorSetting.ENTITY_EDITOR_KEY_GENDER, genderName),
                    toGenderLabel(genderName),
                    genderName.equals(pickedGenderName)));

        rows.add(CharacterViewRowStruct.action(
                EditorSetting.ENTITY_EDITOR_KEY_ADD_GENDER,
                EditorSetting.ENTITY_EDITOR_LABEL_ADD_GENDER));
        rows.add(CharacterViewRowStruct.action(
                EditorSetting.ENTITY_EDITOR_KEY_REMOVE_GENDER,
                EditorSetting.ENTITY_EDITOR_LABEL_REMOVE_GENDER));

        if (pickedGenderName == null)
            return;

        rows.add(CharacterViewRowStruct.header(toHeader(
                EditorSetting.ENTITY_EDITOR_HEADER_MODEL,
                toGenderLabel(pickedGenderName),
                false)));
        addChoice(rows, EntityEditorChoice.MESH);
        addChoice(rows, EntityEditorChoice.MATERIAL);
        addChoice(rows, EntityEditorChoice.ANIMATION_TREE);
    }

    private void addChoice(ObjectArrayList<CharacterViewRowStruct> rows, EntityEditorChoice choice) {

        String value = entityPropertyBranch.getValue(choice);

        rows.add(CharacterViewRowStruct.choice(
                choice.getField(),
                choice.getLabel(),
                value.isEmpty() ? EditorSetting.ENTITY_EDITOR_VALUE_NONE : value));
    }

    // Text \\

    // A section's title and what it holds, marked while it has changes not yet saved
    private String toHeader(String title, String name, boolean dirty) {
        return title + EditorSetting.ENTITY_EDITOR_LABEL_SEPARATOR + name
                + (dirty ? EditorSetting.CHARACTER_VIEW_DIRTY_MARKER : "");
    }

    // A gender by its display name, or by its own name while the gender list does not know it
    private String toGenderLabel(String genderName) {
        return genderManager.hasGender(genderName)
                ? genderManager.getGenderHandleFromGenderName(genderName).getDisplayName()
                : genderName;
    }

    private String toKey(String kind, String target) {
        return kind + EditorSetting.ENTITY_EDITOR_KEY_SEPARATOR + target;
    }
}
