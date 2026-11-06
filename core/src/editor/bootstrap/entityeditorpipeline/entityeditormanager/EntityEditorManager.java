package editor.bootstrap.entityeditorpipeline.entityeditormanager;

import application.bootstrap.progressionpipeline.gendermanager.GenderManager;
import editor.bootstrap.charactereditorpipeline.characterdocument.CharacterDocumentInstance;
import editor.bootstrap.charactereditorpipeline.charactereditormanager.CharacterEditorManager;
import editor.bootstrap.entityeditorpipeline.util.EntityEditorChoice;
import editor.bootstrap.infopipeline.infoentry.InfoEntryStruct;
import editor.bootstrap.infopipeline.infomanager.InfoManager;
import editor.runtime.characterview.panel.CharacterViewRowStruct;
import editor.runtime.EditorSetting;
import engine.root.ManagerPackage;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.io.FileUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class EntityEditorManager extends ManagerPackage {

    /*
     * Owns the Entity Editor's shared state: the entity picked, the race it
     * is made of and the status line. Entities and races are ARPG entries
     * owned by InfoManager, and every change goes back through it: entities
     * are made, deleted and picked here, an entity's race is chosen, a race
     * is copied under a new name or deleted, its genders are added and
     * removed, and each gender's model — mesh, material and animation tree,
     * on the race's one rig — is chosen. Picking an entity selects it for
     * every window, so the Properties tab shows its other fields and the
     * character tabs open its race; picking a gender makes it the one every
     * character tab previews, and the model rows edit that gender. A changed
     * race on the picked entity opens the new race. Edits, values and rows
     * each live in their own branch.
     */

    // Internal
    private InfoManager infoManager;
    private GenderManager genderManager;
    private CharacterEditorManager characterEditorManager;
    private EntityEditBranch entityEditBranch;
    private EntityPropertyBranch entityPropertyBranch;
    private EntityRowBranch entityRowBranch;

    // Active
    private String activeEntityName;
    private String activeRaceName;

    // Status
    private String statusMessage;
    private int revision;

    // Base \\

    @Override
    protected void create() {

        // Internal
        this.entityEditBranch = create(EntityEditBranch.class);
        this.entityPropertyBranch = create(EntityPropertyBranch.class);
        this.entityRowBranch = create(EntityRowBranch.class);
    }

    @Override
    protected void get() {
        this.infoManager = get(InfoManager.class);
        this.genderManager = get(GenderManager.class);
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
        this.statusMessage = null;
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

    public boolean isEntityNameAvailable(String entityName) {
        return FileUtility.isValidFileName(entityName, EditorSetting.NAME_INPUT_MAX_LENGTH)
                && !infoManager.hasDocument(EditorSetting.INFO_SCHEMA_ENTITIES, entityName);
    }

    public boolean isActiveEntityName(String entityName) {
        return entityName.equals(activeEntityName);
    }

    // A new entity of the picked entity's race, picked at once
    public void createEntity(String entityName) {

        if (!isEntityNameAvailable(entityName))
            return;

        entityEditBranch.createEntity(entityName, activeRaceName);
        setStatusMessage(EditorSetting.ENTITY_EDITOR_MESSAGE_CREATED + entityName);
    }

    public void deleteActiveEntity() {

        if (activeEntityName == null)
            return;

        String entityName = activeEntityName;

        setStatusMessage(entityEditBranch.deleteEntity(entityName)
                ? EditorSetting.ENTITY_EDITOR_MESSAGE_DELETED + entityName
                : infoManager.getRefusal());
    }

    // Races \\

    public boolean isRaceNameAvailable(String raceName) {
        return FileUtility.isValidFileName(raceName, EditorSetting.NAME_INPUT_MAX_LENGTH)
                && !infoManager.hasDocument(EditorSetting.INFO_SCHEMA_RACES, raceName);
    }

    public boolean isActiveRaceName(String raceName) {
        return raceName.equals(activeRaceName);
    }

    // A copy of the picked entity's race under a new name, which the entity is then made of
    public void copyRace(String raceName) {

        if (activeEntityName == null || getRaceArpg() == null || !isRaceNameAvailable(raceName))
            return;

        characterEditorManager.copyRace(activeRaceName, raceName);
        entityEditBranch.setEntityField(activeEntityName, EditorSetting.CHARACTER_EDITOR_FIELD_RACE, raceName);
        setStatusMessage(EditorSetting.ENTITY_EDITOR_MESSAGE_COPIED + raceName);
    }

    public void deleteActiveRace() {

        if (activeRaceName == null)
            return;

        String raceName = activeRaceName;

        setStatusMessage(entityEditBranch.deleteRace(raceName)
                ? EditorSetting.ENTITY_EDITOR_MESSAGE_DELETED + raceName
                : infoManager.getRefusal());
    }

    // Genders \\

    private void selectGender(String genderName) {

        if (genderManager.hasGender(genderName))
            characterEditorManager.selectPreviewGender(genderManager.getGenderIDFromGenderName(genderName));
    }

    // The first gender the race does not have yet, made from the gender previewed and previewed in its place
    private void addGender() {

        ArpgObjectStruct genders = getGendersArpg();
        String sourceGenderName = getPickedGenderName();

        if (genders == null || sourceGenderName == null)
            return;

        ObjectArrayList<InfoEntryStruct> entries = infoManager.getEntries(EditorSetting.INFO_SCHEMA_GENDERS);

        for (int i = 0; i < entries.size(); i++) {

            String genderName = entries.get(i).getEntryName();

            if (genders.has(genderName))
                continue;

            entityEditBranch.addGender(activeRaceName, genderName, sourceGenderName);
            selectGender(genderName);
            setStatusMessage(EditorSetting.ENTITY_EDITOR_MESSAGE_ADDED_GENDER + genderName);
            return;
        }

        setStatusMessage(EditorSetting.ENTITY_EDITOR_MESSAGE_ALL_GENDERS);
    }

    private void removeGender() {

        ArpgObjectStruct genders = getGendersArpg();
        String genderName = getPickedGenderName();

        if (genders == null || genderName == null)
            return;

        if (genders.size() <= 1) {
            setStatusMessage(EditorSetting.ENTITY_EDITOR_MESSAGE_LAST_GENDER);
            return;
        }

        entityEditBranch.removeGender(activeRaceName, genderName);
        setStatusMessage(EditorSetting.ENTITY_EDITOR_MESSAGE_REMOVED_GENDER + genderName);
    }

    // Files \\

    private void save() {
        setStatusMessage(entityEditBranch.save(activeEntityName, getRaceArpg() != null ? activeRaceName : null)
                ? EditorSetting.ENTITY_EDITOR_MESSAGE_SAVED + activeEntityName
                : infoManager.getRefusal());
    }

    private void revert() {
        entityEditBranch.revert(activeEntityName, getRaceArpg() != null ? activeRaceName : null);
        setStatusMessage(EditorSetting.ENTITY_EDITOR_MESSAGE_REVERTED + activeEntityName);
    }

    // Panel \\

    // A row picked: an entity selected for every window, or a gender previewed in every character tab
    public void select(String key) {

        int separator = key.indexOf(EditorSetting.ENTITY_EDITOR_KEY_SEPARATOR);

        if (separator < 0)
            return;

        String kind = key.substring(0, separator);
        String target = key.substring(separator + EditorSetting.ENTITY_EDITOR_KEY_SEPARATOR.length());

        if (kind.equals(EditorSetting.ENTITY_EDITOR_KEY_ENTITY))
            infoManager.selectEntry(EditorSetting.INFO_SCHEMA_ENTITIES, target, target);
        else if (kind.equals(EditorSetting.ENTITY_EDITOR_KEY_GENDER))
            selectGender(target);
    }

    // An action that needs no name: genders added and removed, the entity and its race saved or reverted together
    public void act(String key) {

        if (activeEntityName == null)
            return;

        switch (key) {
            case EditorSetting.ENTITY_EDITOR_KEY_ADD_GENDER -> addGender();
            case EditorSetting.ENTITY_EDITOR_KEY_REMOVE_GENDER -> removeGender();
            case EditorSetting.ENTITY_EDITOR_KEY_SAVE -> save();
            case EditorSetting.ENTITY_EDITOR_KEY_REVERT -> revert();
            default -> {
            }
        }
    }

    public void cycle(String key, int direction) {

        EntityEditorChoice choice = EntityEditorChoice.fromKey(key);

        if (choice != null && activeEntityName != null)
            entityPropertyBranch.cycle(choice, direction);
    }

    public String getValueText(String key) {

        EntityEditorChoice choice = EntityEditorChoice.fromKey(key);
        return choice != null && activeEntityName != null ? entityPropertyBranch.getValue(choice) : "";
    }

    public boolean isValueValid(String key, String text) {

        EntityEditorChoice choice = EntityEditorChoice.fromKey(key);
        return choice != null && activeEntityName != null && entityPropertyBranch.isValueValid(choice, text);
    }

    public void setValue(String key, String text) {

        if (isValueValid(key, text))
            entityPropertyBranch.setValue(EntityEditorChoice.fromKey(key), text);
    }

    public void buildListRows(ObjectArrayList<CharacterViewRowStruct> rows) {
        entityRowBranch.buildRows(rows);
    }

    // Status \\

    private void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
        notifyChanged();
    }

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

    public String getStatusMessage() {
        return statusMessage;
    }

    // The picked entity's live ARPG, as InfoManager holds it; null while none is picked
    ArpgObjectStruct getEntityArpg() {

        InfoEntryStruct entry = activeEntityName != null
                ? infoManager.findEntry(EditorSetting.INFO_SCHEMA_ENTITIES, activeEntityName, activeEntityName)
                : null;

        return entry != null ? entry.getArpg() : null;
    }

    // The picked entity's race's live ARPG; null while it names none or the race has no file
    ArpgObjectStruct getRaceArpg() {

        InfoEntryStruct entry = activeRaceName != null
                ? infoManager.findEntry(EditorSetting.INFO_SCHEMA_RACES, activeRaceName, activeRaceName)
                : null;

        return entry != null ? entry.getArpg() : null;
    }

    ArpgObjectStruct getGendersArpg() {

        ArpgObjectStruct race = getRaceArpg();

        return race != null && ArpgUtility.hasObject(race, EditorSetting.ENTITY_EDITOR_FIELD_GENDERS)
                ? race.getAsObject(EditorSetting.ENTITY_EDITOR_FIELD_GENDERS)
                : null;
    }

    // The gender the model rows edit: the one the character tabs preview while they show this race, else its first
    public String getPickedGenderName() {

        ArpgObjectStruct genders = getGendersArpg();

        if (genders == null || genders.size() == 0)
            return null;

        CharacterDocumentInstance document = characterEditorManager.getActiveDocument();

        if (document != null && document.hasPreview() && document.getRaceName().equals(activeRaceName)) {

            String previewName = genderManager.getGenderHandleFromGenderID(
                    characterEditorManager.getPreviewGenderID()).getGenderName();

            if (genders.has(previewName))
                return previewName;
        }

        return genders.keySet().iterator().next();
    }

    ArpgObjectStruct getPickedGenderArpg() {

        ArpgObjectStruct genders = getGendersArpg();
        String genderName = getPickedGenderName();

        return genderName != null ? genders.getAsObject(genderName) : null;
    }

    boolean isEntityDirty() {
        return infoManager.isDocumentDirty(EditorSetting.INFO_SCHEMA_ENTITIES, activeEntityName);
    }

    boolean isRaceDirty() {
        return infoManager.isDocumentDirty(EditorSetting.INFO_SCHEMA_RACES, activeRaceName);
    }

    // Moves with every pick and status here, every change in the character tabs, whose race and gender the rows show,
    // and every edit InfoManager holds; each only counts up, so their sum moves whenever any does
    public int getRevision() {
        return revision + characterEditorManager.getRevision() + infoManager.getRevision();
    }
}
