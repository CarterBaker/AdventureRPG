package editor.bootstrap.faceeditorpipeline.faceeditormanager;

import application.bootstrap.entitypipeline.appearance.AppearanceData;
import application.bootstrap.entitypipeline.race.RaceData;
import application.bootstrap.geometrypipeline.rig.RigHandle;
import editor.bootstrap.charactereditorpipeline.characterdocument.CharacterDocumentInstance;
import editor.bootstrap.charactereditorpipeline.charactereditormanager.CharacterEditorManager;
import editor.bootstrap.charactereditorpipeline.util.CharacterMeshUtility;
import editor.bootstrap.infopipeline.infoentry.InfoEntryStruct;
import editor.bootstrap.infopipeline.infomanager.InfoManager;
import editor.bootstrap.toolpipeline.toolmanager.ToolManager;
import editor.bootstrap.toolpipeline.util.EditorTool;
import editor.bootstrap.toolpipeline.util.ToolCanvas;
import editor.runtime.characterview.history.CharacterViewHistoryStruct;
import editor.runtime.characterview.panel.CharacterViewRowStruct;
import editor.runtime.EditorSetting;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class FaceEditorManager extends ManagerPackage {

    /*
     * Owns the face editor's shared state: the face control picked — one of
     * the race's shape sliders, the way a face rig's controls are its
     * sliders — the face bone picked, whether edits mirror across the face,
     * and each race's undo history. It always works on the race the
     * Character Editor has open, at the gender and slider values the Character
     * Editor previews it with, so both tools show one face. Face bones are the
     * appearance's head bone and every bone below it. Every edit goes through
     * InfoManager on the race, so the Info Panel, the Character Editor and
     * the game read it at once; a drag of a bone in the viewport shapes the
     * control's target on that bone at the control's preview value and undoes
     * at once. Save and Revert are the Character Editor's, so a race's
     * meshes, rig and face are kept together. Edits, panel values and panel
     * rows each live in their own branch.
     */

    // Internal
    private InfoManager infoManager;
    private ToolManager toolManager;
    private CharacterEditorManager characterEditorManager;
    private FaceEditBranch faceEditBranch;
    private FacePropertyBranch facePropertyBranch;
    private FaceRowBranch faceRowBranch;

    // Palette
    private Object2ObjectOpenHashMap<String, CharacterViewHistoryStruct> raceName2History;

    // Selection
    private int selectedShapeIndex;
    private int selectedBoneIndex;
    private boolean mirror;

    // Gesture — the target the drag began from on the bone it shapes
    private Vector3 gestureMove;
    private Vector3 gestureTurn;
    private Vector3 gestureScale;

    // Status
    private String statusMessage;
    private int revision;

    // Base \\

    @Override
    protected void create() {

        // Internal
        this.faceEditBranch = create(FaceEditBranch.class);
        this.facePropertyBranch = create(FacePropertyBranch.class);
        this.faceRowBranch = create(FaceRowBranch.class);

        // Palette
        this.raceName2History = new Object2ObjectOpenHashMap<>();

        // Selection
        this.selectedShapeIndex = EngineSetting.INDEX_NOT_FOUND;
        this.selectedBoneIndex = EngineSetting.INDEX_NOT_FOUND;
        this.mirror = true;

        // Gesture
        this.gestureMove = new Vector3();
        this.gestureTurn = new Vector3();
        this.gestureScale = new Vector3();
    }

    @Override
    protected void get() {
        this.infoManager = get(InfoManager.class);
        this.toolManager = get(ToolManager.class);
        this.characterEditorManager = get(CharacterEditorManager.class);
    }

    @Override
    protected void awake() {
        infoManager.addChangeListener(EditorSetting.INFO_SCHEMA_RACES, this::refreshEntry);
        infoManager.addDeleteListener(EditorSetting.INFO_SCHEMA_RACES, this::closeEntry);
    }

    // Races \\

    // Any change to the open race, here or anywhere else, keeps the picks on what it still holds
    private void refreshEntry(InfoEntryStruct entry) {

        CharacterDocumentInstance document = getActiveDocument();

        if (document == null || !document.getRaceName().equals(entry.getEntryName()))
            return;

        resolveSelection();
        notifyChanged();
    }

    private void closeEntry(InfoEntryStruct entry) {
        raceName2History.remove(entry.getEntryName());
    }

    private void resolveSelection() {

        if (selectedShapeIndex >= faceEditBranch.getShapeCount(getActiveDocument()))
            this.selectedShapeIndex = EngineSetting.INDEX_NOT_FOUND;

        RigHandle rigHandle = getRigHandle();

        if (rigHandle == null || selectedBoneIndex >= rigHandle.getBoneCount())
            this.selectedBoneIndex = EngineSetting.INDEX_NOT_FOUND;
    }

    public void saveActiveTemplate() {
        characterEditorManager.saveActiveTemplate();
        notifyChanged();
    }

    public void revertActiveTemplate() {

        CharacterDocumentInstance document = getActiveDocument();

        if (document == null)
            return;

        characterEditorManager.revertActiveTemplate();
        getHistory(document).clear();
        resolveSelection();
        notifyChanged();
    }

    // Selection \\

    void selectShape(int shapeIndex) {

        this.selectedShapeIndex = shapeIndex;
        notifyChanged();
    }

    public void selectBone(int boneIndex) {

        if (isGesturing())
            return;

        this.selectedBoneIndex = boneIndex;
        notifyChanged();
    }

    void setMirror(boolean mirror) {

        this.mirror = mirror;
        notifyChanged();
    }

    // Panel \\

    public void select(String key) {

        if (hasFace())
            facePropertyBranch.select(key);
    }

    public void step(String key, int direction) {

        if (hasFace())
            facePropertyBranch.step(key, direction);
    }

    public void cycle(String key, int direction) {

        if (hasFace())
            facePropertyBranch.cycle(key, direction);
    }

    public void act(String key) {

        if (hasFace() && !isGesturing())
            facePropertyBranch.act(key);
    }

    public String getValueText(String key) {
        return hasFace() ? facePropertyBranch.getValueText(key) : "";
    }

    public boolean isValueValid(String key, String text) {
        return hasFace() && facePropertyBranch.isValueValid(key, text);
    }

    public void setValue(String key, String text) {

        if (isValueValid(key, text))
            facePropertyBranch.setValue(key, text);
    }

    public void buildListRows(ObjectArrayList<CharacterViewRowStruct> rows) {

        if (hasFace())
            faceRowBranch.buildListRows(rows);
    }

    public void buildPropertyRows(ObjectArrayList<CharacterViewRowStruct> rows) {

        if (hasFace())
            faceRowBranch.buildPropertyRows(rows);
    }

    // Gesture \\

    // Opens one undo step for a drag of the picked face bone into the picked control, first taking a control previewed
    // near nothing to its far end so the drag has a value to shape at; false when there is nothing to shape
    public boolean beginGesture() {

        CharacterDocumentInstance document = getActiveDocument();

        if (!hasFace() || isGesturing())
            return false;

        if (!faceEditBranch.hasShape(document, selectedShapeIndex)) {
            setStatusMessage(EditorSetting.FACE_EDITOR_MESSAGE_NO_CONTROL);
            return false;
        }

        if (!isFaceBone(selectedBoneIndex)) {
            setStatusMessage(EditorSetting.FACE_EDITOR_MESSAGE_NOT_FACE_BONE);
            return false;
        }

        if (Math.abs(getPreviewValue()) < EditorSetting.FACE_EDITOR_DRAG_VALUE_MIN)
            characterEditorManager.setShapeValue(selectedShapeIndex, faceEditBranch.getFarEnd(document,
                    selectedShapeIndex));

        faceEditBranch.readTarget(document, selectedShapeIndex, getBoneName(selectedBoneIndex),
                gestureMove, gestureTurn, gestureScale);
        getHistory(document).begin(getTemplateArpg());
        notifyChanged();

        return true;
    }

    // The picked control's target on the picked bone set whole, its mirror too while mirroring
    public void writeGesture(Vector3 move, Vector3 turn, Vector3 scale) {

        if (!isGesturing())
            return;

        faceEditBranch.writeTarget(getActiveDocument(), selectedShapeIndex, getBoneName(selectedBoneIndex),
                move, turn, scale, mirror ? resolveMirrorBoneName(selectedBoneIndex) : null);
    }

    public void endGesture() {

        if (!isGesturing())
            return;

        getHistory(getActiveDocument()).commit(getTemplateArpg());
        notifyChanged();
    }

    public void cancelGesture() {

        if (!isGesturing())
            return;

        faceEditBranch.restore(getActiveDocument(), getHistory(getActiveDocument()).cancel());
        notifyChanged();
    }

    public boolean isGesturing() {

        CharacterDocumentInstance document = getActiveDocument();
        return document != null && getHistory(document).isOpen();
    }

    public Vector3 getGestureMove() {
        return gestureMove;
    }

    public Vector3 getGestureTurn() {
        return gestureTurn;
    }

    public Vector3 getGestureScale() {
        return gestureScale;
    }

    // History \\

    public void undo() {

        CharacterDocumentInstance document = getActiveDocument();
        ArpgObjectStruct templateArpg = getTemplateArpg();

        if (document == null || templateArpg == null)
            return;

        boolean undone = faceEditBranch.restore(document, getHistory(document).undo(templateArpg));

        setStatusMessage(undone
                ? EditorSetting.CHARACTER_EDITOR_MESSAGE_UNDONE
                : EditorSetting.CHARACTER_EDITOR_MESSAGE_NOTHING_TO_UNDO);
    }

    public void redo() {

        CharacterDocumentInstance document = getActiveDocument();
        ArpgObjectStruct templateArpg = getTemplateArpg();

        if (document == null || templateArpg == null)
            return;

        boolean redone = faceEditBranch.restore(document, getHistory(document).redo(templateArpg));

        setStatusMessage(redone
                ? EditorSetting.CHARACTER_EDITOR_MESSAGE_REDONE
                : EditorSetting.CHARACTER_EDITOR_MESSAGE_NOTHING_TO_REDO);
    }

    // The undo history of a race's face, made the first time it is edited here
    CharacterViewHistoryStruct getHistory(CharacterDocumentInstance document) {

        CharacterViewHistoryStruct history = raceName2History.get(document.getRaceName());

        if (history == null) {
            history = new CharacterViewHistoryStruct();
            raceName2History.put(document.getRaceName(), history);
        }

        return history;
    }

    // Face \\

    // True for the appearance's head bone and every bone below it
    public boolean isFaceBone(int boneIndex) {

        RigHandle rigHandle = getRigHandle();

        if (rigHandle == null || boneIndex < 0 || boneIndex >= rigHandle.getBoneCount())
            return false;

        int headBoneIndex = getAppearanceData().getHeadBoneIndex();

        for (int bone = boneIndex; bone != EngineSetting.INDEX_NOT_FOUND; bone = parentOf(rigHandle, bone))
            if (bone == headBoneIndex)
                return true;

        return false;
    }

    private int parentOf(RigHandle rigHandle, int boneIndex) {
        return rigHandle.getBone(boneIndex).isRoot()
                ? EngineSetting.INDEX_NOT_FOUND
                : rigHandle.getBone(boneIndex).getParentIndex();
    }

    // The bone mirroring one across the face, null for a bone on the middle or one with no partner in the rig
    String resolveMirrorBoneName(int boneIndex) {

        RigHandle rigHandle = getRigHandle();
        String boneName = getBoneName(boneIndex);
        String mirrorName = CharacterMeshUtility.toMirrorBoneName(boneName);

        return !mirrorName.equals(boneName) && rigHandle.hasBone(mirrorName) ? mirrorName : null;
    }

    String getBoneName(int boneIndex) {
        return getRigHandle().getBone(boneIndex).getName();
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
        return characterEditorManager.getActiveDocument();
    }

    // True while the Character Editor has a character open whose preview has an appearance to shape
    public boolean hasFace() {

        CharacterDocumentInstance document = getActiveDocument();

        return document != null && document.hasPreview() && document.getPreviewData().hasAppearance();
    }

    public RaceData getPreviewData() {
        return getActiveDocument().getPreviewData();
    }

    public AppearanceData getAppearanceData() {
        return hasFace() ? getPreviewData().getAppearanceData() : null;
    }

    public RigHandle getRigHandle() {
        return hasFace() ? getPreviewData().getRigHandle() : null;
    }

    // The open race's live ARPG, as InfoManager holds it
    ArpgObjectStruct getTemplateArpg() {

        CharacterDocumentInstance document = getActiveDocument();

        if (document == null)
            return null;

        InfoEntryStruct entry = infoManager.findEntry(
                EditorSetting.INFO_SCHEMA_RACES,
                document.getRaceName(),
                document.getRaceName());

        return entry != null ? entry.getArpg() : null;
    }

    public int getSelectedShapeIndex() {
        return selectedShapeIndex;
    }

    public int getSelectedBoneIndex() {
        return selectedBoneIndex;
    }

    public boolean isMirror() {
        return mirror;
    }

    // The picked control's value in the preview, the Character Editor's own slider
    public float getPreviewValue() {
        return selectedShapeIndex != EngineSetting.INDEX_NOT_FOUND
                ? characterEditorManager.getShapeValue(selectedShapeIndex)
                : 0f;
    }

    // What the viewport's tools work on: the face, while there is one to shape
    public ToolCanvas getToolCanvas() {
        return hasFace() ? ToolCanvas.CHARACTER_FACE : null;
    }

    public EditorTool getFaceTool() {
        return toolManager.getTool(ToolCanvas.CHARACTER_FACE);
    }

    // Moves with every change here and every change the Character Editor shows, both only ever growing
    public int getRevision() {
        return revision + characterEditorManager.getRevision();
    }

    public String getStatusText() {

        CharacterDocumentInstance document = getActiveDocument();

        if (document == null)
            return EditorSetting.FACE_EDITOR_STATUS_NO_TEMPLATE;

        StringBuilder status = new StringBuilder(document.getRaceName());

        if (infoManager.isDocumentDirty(EditorSetting.INFO_SCHEMA_RACES, document.getRaceName()))
            status.append(EditorSetting.CHARACTER_VIEW_DIRTY_MARKER);

        if (faceEditBranch.hasShape(document, selectedShapeIndex))
            status.append(EditorSetting.CHARACTER_VIEW_STATUS_SEPARATOR)
                    .append(faceEditBranch.getShapeName(document, selectedShapeIndex));

        status.append(EditorSetting.CHARACTER_VIEW_STATUS_SEPARATOR).append(EditorSetting.FACE_EDITOR_STATUS_HINT);

        if (document.hasPreviewError())
            status.append(EditorSetting.CHARACTER_VIEW_STATUS_SEPARATOR)
                    .append(EditorSetting.CHARACTER_VIEW_MESSAGE_PREVIEW_REFUSED)
                    .append(document.getPreviewError());

        if (statusMessage != null)
            status.append(EditorSetting.CHARACTER_VIEW_STATUS_SEPARATOR).append(statusMessage);

        return status.toString();
    }
}
