package editor.bootstrap.entityeditorpipeline.entityeditormanager;

import java.util.function.Consumer;

import editor.bootstrap.infopipeline.infomanager.InfoManager;
import editor.runtime.characterview.util.CharacterViewUtility;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;

class EntityEditBranch extends BranchPackage {

    /*
     * Performs every change the Entity Editor makes to an entity or its race,
     * each through InfoManager so the Info Panel, the character tabs and the
     * game see it at once. Edits write only the fields they change and keep
     * every other field and its order. A gender added to a race wears the
     * model of the gender it is made from, so the race still builds; a rig
     * is written on every gender at once, since a race keeps one.
     */

    // Internal
    private InfoManager infoManager;

    // Base \\

    @Override
    protected void get() {
        this.infoManager = get(InfoManager.class);
    }

    // Entities \\

    // A new entity of the race given, every other field at its default, selected for every window
    void createEntity(String entityName, String raceName) {
        infoManager.createDocument(EditorSetting.INFO_SCHEMA_ENTITIES, entityName, root -> {

            if (raceName != null)
                root.addProperty(EditorSetting.CHARACTER_EDITOR_FIELD_RACE, raceName);
        });
    }

    boolean deleteEntity(String entityName) {
        return infoManager.deleteDocument(EditorSetting.INFO_SCHEMA_ENTITIES, entityName);
    }

    void setEntityField(String entityName, String field, String value) {
        infoManager.editEntry(
                EditorSetting.INFO_SCHEMA_ENTITIES,
                entityName,
                entityName,
                entity -> entity.addProperty(field, value));
    }

    // Races \\

    boolean deleteRace(String raceName) {
        return infoManager.deleteDocument(EditorSetting.INFO_SCHEMA_RACES, raceName);
    }

    private void editGenders(String raceName, Consumer<ArpgObjectStruct> editor) {
        infoManager.editEntry(
                EditorSetting.INFO_SCHEMA_RACES,
                raceName,
                raceName,
                race -> editor.accept(
                        CharacterViewUtility.requireObject(race, EditorSetting.ENTITY_EDITOR_FIELD_GENDERS)));
    }

    // A field written on every gender of the race
    void setRaceField(String raceName, String field, String value) {
        editGenders(raceName, genders -> {

            for (String genderName : genders.keySet())
                genders.getAsObject(genderName).addProperty(field, value);
        });
    }

    void setGenderField(String raceName, String genderName, String field, String value) {
        editGenders(raceName, genders -> genders.getAsObject(genderName).addProperty(field, value));
    }

    // A gender added after the others, wearing the model of the gender it is made from
    void addGender(String raceName, String genderName, String sourceGenderName) {
        editGenders(raceName, genders -> {

            ArpgObjectStruct source = genders.getAsObject(sourceGenderName);
            ArpgObjectStruct gender = new ArpgObjectStruct();

            for (String field : EditorSetting.ENTITY_EDITOR_MODEL_FIELDS)
                if (ArpgUtility.hasString(source, field))
                    gender.addProperty(field, source.get(field).getAsString());

            genders.add(genderName, gender);
        });
    }

    void removeGender(String raceName, String genderName) {
        editGenders(raceName, genders -> genders.remove(genderName));
    }

    // Files \\

    // The race first, then the entity naming it, so a save never writes an entity before the race it needs
    boolean save(String entityName, String raceName) {
        return (raceName == null || infoManager.saveDocument(EditorSetting.INFO_SCHEMA_RACES, raceName))
                && infoManager.saveDocument(EditorSetting.INFO_SCHEMA_ENTITIES, entityName);
    }

    void revert(String entityName, String raceName) {

        infoManager.revertDocument(EditorSetting.INFO_SCHEMA_ENTITIES, entityName);

        if (raceName != null)
            infoManager.revertDocument(EditorSetting.INFO_SCHEMA_RACES, raceName);
    }
}
