package editor.bootstrap.entityeditorpipeline.entityeditormanager;

import application.bootstrap.entitypipeline.race.RaceData;
import application.bootstrap.entitypipeline.race.RaceGenderStruct;
import application.bootstrap.progressionpipeline.gendermanager.GenderManager;
import editor.bootstrap.charactereditorpipeline.charactereditormanager.CharacterEditorManager;
import editor.bootstrap.infopipeline.infoentry.InfoEntryStruct;
import editor.bootstrap.infopipeline.infomanager.InfoManager;
import editor.runtime.characterview.panel.CharacterViewRowStruct;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class EntityRowBranch extends BranchPackage {

    /*
     * Lists the rows of the Entity Editor: every entity, the picked one
     * highlighted; then the race the picked entity is made of; then every
     * gender that race may be made as, each with the model it requires —
     * its mesh, rig and animation tree — the gender the character tabs
     * preview highlighted.
     */

    // Internal
    private InfoManager infoManager;
    private GenderManager genderManager;
    private CharacterEditorManager characterEditorManager;
    private EntityEditorManager entityEditorManager;

    // Base \\

    @Override
    protected void get() {
        this.infoManager = get(InfoManager.class);
        this.genderManager = get(GenderManager.class);
        this.characterEditorManager = get(CharacterEditorManager.class);
        this.entityEditorManager = get(EntityEditorManager.class);
    }

    // Rows \\

    void buildRows(ObjectArrayList<CharacterViewRowStruct> rows) {

        buildEntityRows(rows);

        if (entityEditorManager.getActiveEntityName() != null)
            buildRaceRows(rows);
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
    }

    private void buildRaceRows(ObjectArrayList<CharacterViewRowStruct> rows) {

        String raceName = entityEditorManager.getActiveRaceName();
        RaceData raceData = entityEditorManager.getActiveRaceData();

        rows.add(CharacterViewRowStruct.header(EditorSetting.ENTITY_EDITOR_HEADER_RACE));

        if (raceName != null)
            rows.add(CharacterViewRowStruct.item(EditorSetting.ENTITY_EDITOR_KEY_RACE, raceName, false));

        if (raceData == null) {
            rows.add(CharacterViewRowStruct.header(EditorSetting.ENTITY_EDITOR_HEADER_NO_RACE));
            return;
        }

        short previewGenderID = characterEditorManager.getPreviewGenderID();

        rows.add(CharacterViewRowStruct.header(EditorSetting.ENTITY_EDITOR_HEADER_GENDERS));

        for (int i = 0; i < raceData.getGenderCount(); i++) {

            RaceGenderStruct gender = raceData.getGenderAt(i);

            rows.add(CharacterViewRowStruct.item(
                    toKey(EditorSetting.ENTITY_EDITOR_KEY_GENDER, Integer.toString(i)),
                    toGenderLabel(gender),
                    gender.getGenderID() == previewGenderID));
        }
    }

    // The gender's name and the model it requires: mesh, rig and animation tree
    private String toGenderLabel(RaceGenderStruct gender) {
        return genderManager.getGenderHandleFromGenderID(gender.getGenderID()).getDisplayName()
                + EditorSetting.ENTITY_EDITOR_LABEL_SEPARATOR + gender.getMeshHandle().getMeshName()
                + EditorSetting.CHARACTER_VIEW_STATUS_SEPARATOR + gender.getRigHandle().getRigName()
                + EditorSetting.CHARACTER_VIEW_STATUS_SEPARATOR + gender.getAnimationTreeHandle().getTreeName();
    }

    private String toKey(String kind, String target) {
        return kind + EditorSetting.ENTITY_EDITOR_KEY_SEPARATOR + target;
    }
}
