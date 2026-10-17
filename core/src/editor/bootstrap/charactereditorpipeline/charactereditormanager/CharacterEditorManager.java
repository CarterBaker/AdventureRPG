package editor.bootstrap.charactereditorpipeline.charactereditormanager;

import java.util.Arrays;

import editor.bootstrap.charactereditorpipeline.characterdocument.CharacterDocumentInstance;
import editor.bootstrap.charactereditorpipeline.util.CharacterEditorColor;
import editor.bootstrap.charactereditorpipeline.util.CharacterEditorMode;
import editor.bootstrap.charactereditorpipeline.util.CharacterEditorPose;
import editor.bootstrap.infopipeline.infoentry.InfoEntryStruct;
import editor.bootstrap.infopipeline.infomanager.InfoManager;
import editor.characterview.panel.CharacterViewRowStruct;
import editor.runtime.EditorSetting;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.io.FileUtility;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class CharacterEditorManager extends ManagerPackage {

    /*
     * Owns the character editor's shared state — the open entity templates,
     * the active one, the panel mode and selection, and how the preview
     * stands, is built and is colored. Templates are ARPG entries owned by
     * InfoManager: selecting one there opens it here, every edit goes back
     * through it, and every change to an open template, made here or in the
     * Info Panel, rebuilds its preview live. Edits, previews, panel values
     * and panel rows each live in their own branch.
     */

    // Internal
    private InfoManager infoManager;
    private CharacterEditBranch characterEditBranch;
    private CharacterPreviewBranch characterPreviewBranch;
    private CharacterPropertyBranch characterPropertyBranch;
    private CharacterRowBranch characterRowBranch;

    // Palette
    private Object2ObjectOpenHashMap<String, CharacterDocumentInstance> templateName2CharacterDocument;

    // Active
    private CharacterDocumentInstance activeDocument;
    private CharacterEditorMode mode;
    private int selectedBoneIndex;
    private int selectedAnchorIndex;

    // Preview
    private CharacterEditorPose pose;
    private float weightRatio;
    private Vector3 headProportion;
    private int[] toneIndices;

    // Status
    private String statusMessage;
    private int revision;

    // Base \\

    @Override
    protected void create() {

        // Internal
        this.characterEditBranch = create(CharacterEditBranch.class);
        this.characterPreviewBranch = create(CharacterPreviewBranch.class);
        this.characterPropertyBranch = create(CharacterPropertyBranch.class);
        this.characterRowBranch = create(CharacterRowBranch.class);

        // Palette
        this.templateName2CharacterDocument = new Object2ObjectOpenHashMap<>();

        // Active
        this.mode = CharacterEditorMode.APPEARANCE;
        this.selectedAnchorIndex = EngineSetting.INDEX_NOT_FOUND;

        // Preview
        this.pose = CharacterEditorPose.IDLE;
        this.weightRatio = EditorSetting.CHARACTER_VIEW_WEIGHT_RATIO;
        this.headProportion = new Vector3(1f, 1f, 1f);
        this.toneIndices = new int[CharacterEditorColor.VALUES.length];
        Arrays.fill(toneIndices, EngineSetting.INDEX_NOT_FOUND);
    }

    @Override
    protected void get() {
        this.infoManager = get(InfoManager.class);
    }

    @Override
    protected void awake() {
        infoManager.addSelectionListener(EditorSetting.INFO_SCHEMA_ENTITIES, this::openEntry);
        infoManager.addChangeListener(EditorSetting.INFO_SCHEMA_ENTITIES, this::refreshEntry);
        infoManager.addDeleteListener(EditorSetting.INFO_SCHEMA_ENTITIES, this::closeEntry);
    }

    // Templates \\

    private void openEntry(InfoEntryStruct entry) {

        CharacterDocumentInstance document = templateName2CharacterDocument.get(entry.getEntryName());

        if (document == null) {
            document = create(CharacterDocumentInstance.class);
            document.constructor(entry.getEntryName());
            templateName2CharacterDocument.put(entry.getEntryName(), document);
            characterPreviewBranch.rebuild(document, entry.getArpg());
        }

        if (document != activeDocument)
            activate(document);
    }

    private void refreshEntry(InfoEntryStruct entry) {

        CharacterDocumentInstance document = templateName2CharacterDocument.get(entry.getEntryName());

        if (document == null)
            return;

        characterPreviewBranch.rebuild(document, entry.getArpg());

        if (document == activeDocument)
            resolveSelection();

        notifyChanged();
    }

    private void closeEntry(InfoEntryStruct entry) {

        CharacterDocumentInstance document = templateName2CharacterDocument.remove(entry.getEntryName());

        if (document != null && document == activeDocument)
            activate(null);
    }

    private void activate(CharacterDocumentInstance document) {

        this.activeDocument = document;
        this.selectedBoneIndex = 0;
        this.selectedAnchorIndex = EngineSetting.INDEX_NOT_FOUND;
        this.statusMessage = null;
        Arrays.fill(toneIndices, EngineSetting.INDEX_NOT_FOUND);
        notifyChanged();
    }

    // Keeps the selected bone on the rig the preview is built on now
    private void resolveSelection() {

        if (!activeDocument.hasPreview())
            return;

        if (selectedBoneIndex >= activeDocument.getPreviewData().getRigHandle().getBoneCount())
            this.selectedBoneIndex = 0;
    }

    public void saveActiveTemplate() {

        if (activeDocument == null)
            return;

        infoManager.saveDocument(EditorSetting.INFO_SCHEMA_ENTITIES, activeDocument.getTemplateName());
        setStatusMessage(EditorSetting.CHARACTER_EDITOR_MESSAGE_SAVED + activeDocument.getTemplateName());
    }

    public void revertActiveTemplate() {

        if (activeDocument == null)
            return;

        infoManager.revertDocument(EditorSetting.INFO_SCHEMA_ENTITIES, activeDocument.getTemplateName());
        setStatusMessage(EditorSetting.CHARACTER_EDITOR_MESSAGE_REVERTED + activeDocument.getTemplateName());
    }

    public void copyActiveTemplate(String localName) {

        if (activeDocument == null || !isTemplateNameAvailable(localName))
            return;

        characterEditBranch.copyTemplate(getTemplateArpg(), toSiblingName(localName));
        setStatusMessage(EditorSetting.CHARACTER_EDITOR_MESSAGE_COPIED + localName);
    }

    public void deleteActiveTemplate() {

        if (activeDocument == null)
            return;

        String templateName = activeDocument.getTemplateName();

        infoManager.deleteDocument(EditorSetting.INFO_SCHEMA_ENTITIES, templateName);
        setStatusMessage(EditorSetting.CHARACTER_EDITOR_MESSAGE_DELETED + templateName);
    }

    public boolean isTemplateNameAvailable(String localName) {
        return activeDocument != null
                && FileUtility.isValidFileName(localName, EditorSetting.NAME_INPUT_MAX_LENGTH)
                && !infoManager.hasDocument(EditorSetting.INFO_SCHEMA_ENTITIES, toSiblingName(localName));
    }

    public boolean isActiveTemplateName(String localName) {
        return activeDocument != null && toLocalName(activeDocument.getTemplateName()).equals(localName);
    }

    public String getActiveLocalName() {
        return activeDocument != null ? toLocalName(activeDocument.getTemplateName()) : "";
    }

    // A new template beside the active one, in the same folder
    private String toSiblingName(String localName) {

        String templateName = activeDocument.getTemplateName();
        int separator = templateName.lastIndexOf(EditorSetting.INFO_FOLDER_SEPARATOR);

        return separator == EngineSetting.INDEX_NOT_FOUND
                ? localName
                : templateName.substring(0, separator + EditorSetting.INFO_FOLDER_SEPARATOR.length()) + localName;
    }

    private String toLocalName(String templateName) {
        return templateName.substring(templateName.lastIndexOf(EditorSetting.INFO_FOLDER_SEPARATOR) + 1);
    }

    // Modes \\

    public void setMode(CharacterEditorMode mode) {

        this.mode = mode;
        notifyChanged();
    }

    // Panel \\

    public void select(String key) {

        if (activeDocument != null)
            characterPropertyBranch.select(activeDocument, key);
    }

    public void step(String key, int direction) {

        if (activeDocument != null)
            characterPropertyBranch.step(activeDocument, key, direction);
    }

    public void cycle(String key, int direction) {

        if (activeDocument != null)
            characterPropertyBranch.cycle(activeDocument, key, direction);
    }

    public void act(String key) {

        if (activeDocument != null)
            characterPropertyBranch.act(activeDocument, key);
    }

    public String getValueText(String key) {
        return activeDocument != null ? characterPropertyBranch.getValueText(activeDocument, key) : "";
    }

    public boolean isValueValid(String key, String text) {
        return activeDocument != null && characterPropertyBranch.isValueValid(activeDocument, key, text);
    }

    public void setValue(String key, String text) {

        if (isValueValid(key, text))
            characterPropertyBranch.setValue(activeDocument, key, text);
    }

    public void buildListRows(ObjectArrayList<CharacterViewRowStruct> rows) {

        if (activeDocument != null)
            characterRowBranch.buildListRows(activeDocument, rows);
    }

    public void buildPropertyRows(ObjectArrayList<CharacterViewRowStruct> rows) {

        if (activeDocument != null)
            characterRowBranch.buildPropertyRows(activeDocument, rows);
    }

    // Selection \\

    void selectBone(int boneIndex) {

        this.selectedBoneIndex = boneIndex;
        notifyChanged();
    }

    void selectAnchor(int anchorIndex) {

        this.selectedAnchorIndex = anchorIndex;
        notifyChanged();
    }

    // Preview \\

    void setPose(CharacterEditorPose pose) {

        this.pose = pose;
        notifyChanged();
    }

    void setWeightRatio(float weightRatio) {

        this.weightRatio = weightRatio;
        notifyChanged();
    }

    void setHeadProportion(int axis, float proportion) {

        headProportion.set(
                axis == 0 ? proportion : headProportion.x,
                axis == 1 ? proportion : headProportion.y,
                axis == 2 ? proportion : headProportion.z);
        notifyChanged();
    }

    void setToneIndex(CharacterEditorColor color, int toneIndex) {

        toneIndices[color.ordinal()] = toneIndex;
        notifyChanged();
    }

    // Status \\

    void notifyChanged() {
        revision++;
    }

    void setStatusMessage(String statusMessage) {

        this.statusMessage = statusMessage;
        notifyChanged();
    }

    // Accessible \\

    public CharacterDocumentInstance getActiveDocument() {
        return activeDocument;
    }

    public boolean hasActiveDocument() {
        return activeDocument != null;
    }

    // The active template's live ARPG, as InfoManager holds it
    ArpgObjectStruct getTemplateArpg() {

        InfoEntryStruct entry = infoManager.findEntry(
                EditorSetting.INFO_SCHEMA_ENTITIES,
                activeDocument.getTemplateName(),
                activeDocument.getTemplateName());

        return entry != null ? entry.getArpg() : null;
    }

    public CharacterEditorMode getMode() {
        return mode;
    }

    public int getSelectedBoneIndex() {
        return selectedBoneIndex;
    }

    public int getSelectedAnchorIndex() {
        return selectedAnchorIndex;
    }

    public CharacterEditorPose getPose() {
        return pose;
    }

    public float getWeightRatio() {
        return weightRatio;
    }

    public Vector3 getHeadProportion() {
        return headProportion;
    }

    public int getToneIndex(CharacterEditorColor color) {
        return toneIndices[color.ordinal()];
    }

    public int getRevision() {
        return revision;
    }

    public String getStatusText() {

        StringBuilder status = new StringBuilder();

        if (activeDocument == null)
            status.append(EditorSetting.CHARACTER_EDITOR_STATUS_NO_TEMPLATE);
        else
            appendDocumentStatus(status);

        if (statusMessage != null)
            status.append(EditorSetting.CHARACTER_VIEW_STATUS_SEPARATOR).append(statusMessage);

        return status.toString();
    }

    private void appendDocumentStatus(StringBuilder status) {

        status.append(activeDocument.getTemplateName());

        if (infoManager.isDocumentDirty(EditorSetting.INFO_SCHEMA_ENTITIES, activeDocument.getTemplateName()))
            status.append(EditorSetting.CHARACTER_VIEW_DIRTY_MARKER);

        status.append(EditorSetting.CHARACTER_VIEW_STATUS_SEPARATOR).append(mode.getLabel());
        status.append(EditorSetting.CHARACTER_VIEW_STATUS_SEPARATOR).append(pose.getLabel());

        if (activeDocument.hasPreviewError())
            status.append(EditorSetting.CHARACTER_VIEW_STATUS_SEPARATOR)
                    .append(EditorSetting.CHARACTER_VIEW_MESSAGE_PREVIEW_REFUSED)
                    .append(activeDocument.getPreviewError());
    }
}
