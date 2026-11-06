package editor.bootstrap.entityeditorpipeline.util;

import editor.runtime.EditorSetting;
import engine.root.EngineSetting;

public enum EntityEditorChoice {

    /*
     * The values the Entity Editor chooses, as data: the field each writes,
     * which also keys its row, its label, where it is written, the schema
     * whose entries it names (null for a mesh, named by its file), the
     * directory whose files beside the current one are its options (null to
     * offer every entry of the schema), and whether its options are only
     * those on the race's rig. A race's rig is written on every gender at
     * once, since every gender of a race shares one.
     */

    RACE(
            EditorSetting.CHARACTER_EDITOR_FIELD_RACE,
            EditorSetting.ENTITY_EDITOR_LABEL_RACE,
            EntityEditorScope.ENTITY,
            EditorSetting.INFO_SCHEMA_RACES,
            null,
            false),
    RIG(
            EditorSetting.ENTITY_EDITOR_FIELD_RIG,
            EditorSetting.ENTITY_EDITOR_LABEL_RIG,
            EntityEditorScope.RACE,
            EditorSetting.INFO_SCHEMA_RIGS,
            null,
            false),
    MESH(
            EditorSetting.ENTITY_EDITOR_FIELD_MESH,
            EditorSetting.ENTITY_EDITOR_LABEL_MESH,
            EntityEditorScope.GENDER,
            null,
            EngineSetting.MESH_PATH,
            false),
    MATERIAL(
            EditorSetting.ENTITY_EDITOR_FIELD_MATERIAL,
            EditorSetting.ENTITY_EDITOR_LABEL_MATERIAL,
            EntityEditorScope.GENDER,
            EditorSetting.INFO_SCHEMA_MATERIALS,
            EngineSetting.MATERIAL_PATH,
            false),
    ANIMATION_TREE(
            EditorSetting.ENTITY_EDITOR_FIELD_ANIMATION_TREE,
            EditorSetting.ENTITY_EDITOR_LABEL_ANIMATION_TREE,
            EntityEditorScope.GENDER,
            EditorSetting.INFO_SCHEMA_ANIMATION_TREES,
            null,
            true);

    public static final EntityEditorChoice[] VALUES = values();

    // Internal
    private final String field;
    private final String label;
    private final EntityEditorScope scope;
    private final String schemaName;
    private final String directory;
    private final boolean rigBound;

    // Constructor \\

    EntityEditorChoice(
            String field,
            String label,
            EntityEditorScope scope,
            String schemaName,
            String directory,
            boolean rigBound) {
        this.field = field;
        this.label = label;
        this.scope = scope;
        this.schemaName = schemaName;
        this.directory = directory;
        this.rigBound = rigBound;
    }

    // Lookup \\

    // The choice a row's key names, null for a key that names none
    public static EntityEditorChoice fromKey(String key) {

        for (int i = 0; i < VALUES.length; i++)
            if (VALUES[i].field.equals(key))
                return VALUES[i];

        return null;
    }

    // Accessible \\

    public String getField() {
        return field;
    }

    public String getLabel() {
        return label;
    }

    public EntityEditorScope getScope() {
        return scope;
    }

    public String getSchemaName() {
        return schemaName;
    }

    // True for a choice whose options are the files beside the current one, false for one offering a schema's entries
    public boolean listsSiblings() {
        return directory != null;
    }

    public String getDirectory() {
        return directory;
    }

    public boolean isRigBound() {
        return rigBound;
    }
}
