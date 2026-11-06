package editor.bootstrap.entityeditorpipeline.entityeditormanager;

import java.util.Objects;

import application.bootstrap.entitypipeline.race.RaceData;
import editor.bootstrap.charactereditorpipeline.characterdocument.CharacterDocumentInstance;
import editor.bootstrap.charactereditorpipeline.charactereditormanager.CharacterEditorManager;
import editor.bootstrap.infopipeline.infoentry.InfoEntryStruct;
import editor.bootstrap.infopipeline.infomanager.InfoManager;
import editor.runtime.characterview.panel.CharacterViewRowStruct;
import editor.runtime.EditorSetting;
import engine.root.ManagerPackage;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class EntityEditorManager extends ManagerPackage {

    /*
     * Owns the entity editor's shared state: the entity picked and the race
     * it is made of. Entities are ARPG entries owned by InfoManager: picking
     * one here selects it there, so the Properties tab shows and edits its
     * fields and the character tabs open its race with every gender's model,
     * and an entity picked in the Hierarchy is picked here. Picking a gender
     * makes it the one every character tab previews, wearing the model that
     * gender requires; picking the race selects it, so its own fields show.
     * A change to the picked entity's race opens the new race in the
     * character tabs. Rows live in their own branch.
     */

    // Internal
    private InfoManager infoManager;
    private CharacterEditorManager characterEditorManager;
    private EntityRowBranch entityRowBranch;

    // Active
    private String activeEntityName;
    private String activeRaceName;

    // Status
    private int revision;

    // Base \\

    @Override
    protected void create() {

        // Internal
        this.entityRowBranch = create(EntityRowBranch.class);
    }

    @Override
    protected void get() {
        this.infoManager = get(InfoManager.class);
        this.characterEditorManager = get(CharacterEditorManager.class);
    }

    @Override
    protected void awake() {
        infoManager.addSelectionListener(EditorSetting.INFO_SCHEMA_ENTITIES, this::openEntry);
        infoManager.addChangeListener(EditorSetting.INFO_SCHEMA_ENTITIES, this::refreshEntry);
        infoManager.addDeleteListener(EditorSetting.INFO_SCHEMA_ENTITIES, this::closeEntry);
    }

    // Entities \\

    private void openEntry(InfoEntryStruct entry) {

        this.activeEntityName = entry.getEntryName();
        this.activeRaceName = readRaceName(entry.getArpg());
        notifyChanged();
    }

    // Any change to an entity may rename what the list shows; a new race for the picked one opens in the character tabs
    private void refreshEntry(InfoEntryStruct entry) {

        if (entry.getEntryName().equals(activeEntityName)) {

            String raceName = readRaceName(entry.getArpg());

            if (raceName != null && !raceName.equals(activeRaceName))
                characterEditorManager.openRace(raceName);

            this.activeRaceName = raceName;
        }

        notifyChanged();
    }

    private void closeEntry(InfoEntryStruct entry) {

        if (entry.getEntryName().equals(activeEntityName)) {
            this.activeEntityName = null;
            this.activeRaceName = null;
        }

        notifyChanged();
    }

    // The race an entity names, null while it names none
    private String readRaceName(ArpgObjectStruct entityArpg) {
        return ArpgUtility.hasString(entityArpg, EditorSetting.CHARACTER_EDITOR_FIELD_RACE)
                ? entityArpg.get(EditorSetting.CHARACTER_EDITOR_FIELD_RACE).getAsString()
                : null;
    }

    // Panel \\

    // A row picked: an entity selected for every window, its race selected, or a gender previewed
    public void select(String key) {

        int separator = key.indexOf(EditorSetting.ENTITY_EDITOR_KEY_SEPARATOR);
        String kind = separator < 0 ? key : key.substring(0, separator);
        String target = separator < 0
                ? ""
                : key.substring(separator + EditorSetting.ENTITY_EDITOR_KEY_SEPARATOR.length());

        switch (kind) {
            case EditorSetting.ENTITY_EDITOR_KEY_ENTITY -> infoManager.selectEntry(
                    EditorSetting.INFO_SCHEMA_ENTITIES, target, target);
            case EditorSetting.ENTITY_EDITOR_KEY_RACE -> selectRace();
            case EditorSetting.ENTITY_EDITOR_KEY_GENDER -> selectGender(Integer.parseInt(target));
            default -> {
            }
        }
    }

    private void selectRace() {

        if (activeRaceName != null)
            infoManager.selectEntry(EditorSetting.INFO_SCHEMA_RACES, activeRaceName, activeRaceName);
    }

    private void selectGender(int genderIndex) {

        RaceData raceData = getActiveRaceData();

        if (raceData != null && genderIndex < raceData.getGenderCount())
            characterEditorManager.selectPreviewGender(raceData.getGenderID(genderIndex));
    }

    public void buildListRows(ObjectArrayList<CharacterViewRowStruct> rows) {
        entityRowBranch.buildRows(rows);
    }

    // Status \\

    private void notifyChanged() {
        revision++;
    }

    // Accessible \\

    public String getActiveEntityName() {
        return activeEntityName;
    }

    public String getActiveRaceName() {
        return activeRaceName;
    }

    // The picked entity's race as the character tabs preview it, edits not yet saved included; null while they show
    // another race or none could be built
    public RaceData getActiveRaceData() {

        CharacterDocumentInstance document = characterEditorManager.getActiveDocument();

        return document != null && document.hasPreview() && Objects.equals(document.getRaceName(), activeRaceName)
                ? document.getPreviewData()
                : null;
    }

    // Moves with the picks here and with every change in the character tabs, whose race and gender the rows show;
    // both only count up, so their sum moves whenever either does
    public int getRevision() {
        return revision + characterEditorManager.getRevision();
    }
}
