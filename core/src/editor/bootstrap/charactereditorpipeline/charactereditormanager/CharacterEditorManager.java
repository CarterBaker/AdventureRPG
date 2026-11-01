package editor.bootstrap.charactereditorpipeline.charactereditormanager;

import java.util.Arrays;

import application.bootstrap.geometrypipeline.mesh.MeshHandle;
import application.bootstrap.geometrypipeline.rig.RigHandle;
import editor.bootstrap.charactereditorpipeline.characterdocument.CharacterDocumentInstance;
import editor.bootstrap.charactereditorpipeline.charactermesh.CharacterMeshDocumentInstance;
import editor.bootstrap.charactereditorpipeline.charactermesh.CharacterStampStruct;
import editor.bootstrap.charactereditorpipeline.util.CharacterEditorColor;
import editor.bootstrap.charactereditorpipeline.util.CharacterEditorMode;
import editor.bootstrap.charactereditorpipeline.util.CharacterEditorPose;
import editor.bootstrap.charactereditorpipeline.util.CharacterEditorSelection;
import editor.bootstrap.charactereditorpipeline.util.CharacterEditorShape;
import editor.bootstrap.charactereditorpipeline.util.CharacterEditorTransform;
import editor.bootstrap.infopipeline.infoentry.InfoEntryStruct;
import editor.bootstrap.infopipeline.infomanager.InfoManager;
import editor.bootstrap.toolpipeline.toolmanager.ToolManager;
import editor.bootstrap.toolpipeline.util.EditorTool;
import editor.bootstrap.toolpipeline.util.ToolCanvas;
import editor.bootstrap.toolpipeline.util.ToolOption;
import editor.runtime.characterview.panel.CharacterViewRowStruct;
import editor.runtime.EditorSetting;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.io.FileUtility;
import engine.util.mathematics.matrices.Matrix4;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class CharacterEditorManager extends ManagerPackage {

    /*
     * Owns the character editor's shared state — the open entity templates, the
     * active one, the panel mode and selection, and how the preview stands, is
     * built and is colored. Templates are ARPG entries owned by InfoManager:
     * selecting one there opens it here, every edit goes back through it, and
     * every change to an open template, made here or in the Info Panel,
     * rebuilds its preview live. The Mesh, Sculpt and Weights modes edit the
     * rigged mesh of the part picked — the body or a default feature — with the
     * tool options and the transform asked for held here, and the Rig mode the
     * rig the character is built on; each is a tool canvas whose tool and
     * options are ToolManager's: on the mesh Move, Rotate and Scale drag the
     * selection, on the sculpt every tool strokes the surface, on the weights
     * the brush strokes with BrushManager's brush in use, Brush drawing toward
     * the target weight, Erase toward none and Smooth blending between
     * neighbours, and on the rig the tools pick, drag, size, add and remove
     * bones. Save and Revert carry every open mesh's changes and the rig's with
     * the template's. Edits, previews, mesh files, selection, geometry,
     * sculpting, weights, the rig, history, panel values and panel rows each
     * live in their own branch.
     */

    // Internal
    private InfoManager infoManager;
    private ToolManager toolManager;
    private CharacterEditBranch characterEditBranch;
    private CharacterPreviewBranch characterPreviewBranch;
    private CharacterPropertyBranch characterPropertyBranch;
    private CharacterRowBranch characterRowBranch;
    private CharacterHistoryBranch characterHistoryBranch;
    private CharacterMeshBranch characterMeshBranch;
    private CharacterSelectionBranch characterSelectionBranch;
    private CharacterWeightBranch characterWeightBranch;
    private CharacterSculptBranch characterSculptBranch;
    private CharacterShapeBranch characterShapeBranch;
    private CharacterRigBranch characterRigBranch;
    private CharacterRigPropertyBranch characterRigPropertyBranch;
    private CharacterRigRowBranch characterRigRowBranch;
    private CharacterMeshPropertyBranch characterMeshPropertyBranch;
    private CharacterMeshRowBranch characterMeshRowBranch;

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

    // Mesh
    private String activePartKey;
    private boolean mirror;
    private boolean xray;
    private boolean proportional;
    private float proportionalRadius;
    private float snapStep;

    // Parts
    private CharacterEditorShape partShape;
    private float partSize;
    private String partTextureName;

    // Transform
    private CharacterEditorTransform requestedTransform;
    private Vector3 requestedAxis;
    private boolean requestedAxisSet;

    // Weights
    private float weightTarget;

    // Scratch
    private ObjectArrayList<String> partKeys;
    private ObjectArrayList<CharacterMeshDocumentInstance> meshDocuments;

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
        this.characterHistoryBranch = create(CharacterHistoryBranch.class);
        this.characterMeshBranch = create(CharacterMeshBranch.class);
        this.characterSelectionBranch = create(CharacterSelectionBranch.class);
        this.characterWeightBranch = create(CharacterWeightBranch.class);
        this.characterSculptBranch = create(CharacterSculptBranch.class);
        this.characterShapeBranch = create(CharacterShapeBranch.class);
        this.characterRigBranch = create(CharacterRigBranch.class);
        this.characterRigPropertyBranch = create(CharacterRigPropertyBranch.class);
        this.characterRigRowBranch = create(CharacterRigRowBranch.class);
        this.characterMeshPropertyBranch = create(CharacterMeshPropertyBranch.class);
        this.characterMeshRowBranch = create(CharacterMeshRowBranch.class);

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

        // Mesh
        this.activePartKey = EditorSetting.CHARACTER_EDITOR_PART_BODY;
        this.mirror = true;
        this.proportionalRadius = EditorSetting.CHARACTER_EDITOR_MESH_RADIUS_DEFAULT;
        this.snapStep = EditorSetting.CHARACTER_EDITOR_MESH_SNAP_DEFAULT;

        // Parts
        this.partShape = CharacterEditorShape.BOX;
        this.partSize = EditorSetting.CHARACTER_EDITOR_PART_SIZE_DEFAULT;

        // Transform
        this.requestedAxis = new Vector3();

        // Weights
        this.weightTarget = 1f;

        // Scratch
        this.partKeys = new ObjectArrayList<>();
        this.meshDocuments = new ObjectArrayList<>();
    }

    @Override
    protected void get() {
        this.infoManager = get(InfoManager.class);
        this.toolManager = get(ToolManager.class);
    }

    @Override
    protected void awake() {
        infoManager.addSelectionListener(EditorSetting.INFO_SCHEMA_ENTITIES, this::openEntry);
        infoManager.addChangeListener(EditorSetting.INFO_SCHEMA_ENTITIES, this::refreshEntry);
        infoManager.addDeleteListener(EditorSetting.INFO_SCHEMA_ENTITIES, this::closeEntry);
        infoManager.addChangeListener(EditorSetting.INFO_SCHEMA_RIGS, characterRigBranch::refreshEntry);
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

        characterSculptBranch.cancelTransform();

        this.activeDocument = document;
        this.selectedBoneIndex = 0;
        this.selectedAnchorIndex = EngineSetting.INDEX_NOT_FOUND;
        this.activePartKey = EditorSetting.CHARACTER_EDITOR_PART_BODY;
        this.requestedTransform = null;
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

        if (!saveDirtyMeshes()) {
            setStatusMessage(EditorSetting.CHARACTER_EDITOR_MESSAGE_MESH_NOT_SAVED);
            return;
        }

        if (!saveRigFile())
            return;

        setStatusMessage(infoManager.saveDocument(EditorSetting.INFO_SCHEMA_ENTITIES, activeDocument.getTemplateName())
                ? EditorSetting.CHARACTER_EDITOR_MESSAGE_SAVED + activeDocument.getTemplateName()
                : infoManager.getRefusal());
    }

    public void revertActiveTemplate() {

        if (activeDocument == null)
            return;

        infoManager.revertDocument(EditorSetting.INFO_SCHEMA_ENTITIES, activeDocument.getTemplateName());
        revertDirtyMeshes();
        revertRigFile();
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

        setStatusMessage(infoManager.deleteDocument(EditorSetting.INFO_SCHEMA_ENTITIES, templateName)
                ? EditorSetting.CHARACTER_EDITOR_MESSAGE_DELETED + templateName
                : infoManager.getRefusal());
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

        characterSculptBranch.cancelTransform();

        this.mode = mode;
        this.requestedTransform = null;
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

    // Parts \\

    void selectPart(String partKey) {

        characterSculptBranch.cancelTransform();

        this.activePartKey = partKey;
        this.requestedTransform = null;
        notifyChanged();
    }

    // The open mesh of the part picked, falling back to the body once the character no longer wears it; null without
    // a character or when the part has no rigged mesh file to edit
    public CharacterMeshDocumentInstance getActiveMeshDocument() {

        if (activeDocument == null || !activeDocument.hasPreview())
            return null;

        MeshHandle meshHandle = characterMeshBranch.resolvePartMesh(activeDocument, activePartKey);

        if (meshHandle == null) {
            this.activePartKey = EditorSetting.CHARACTER_EDITOR_PART_BODY;
            meshHandle = characterMeshBranch.resolvePartMesh(activeDocument, activePartKey);
        }

        return characterMeshBranch.open(meshHandle);
    }

    public boolean hasActiveMeshDocument() {
        return getActiveMeshDocument() != null;
    }

    // The open mesh of every part the character wears that has a rigged mesh file of its own
    public void collectWornMeshDocuments(ObjectArrayList<CharacterMeshDocumentInstance> documents) {

        documents.clear();

        if (activeDocument == null)
            return;

        partKeys.clear();
        characterMeshBranch.collectPartKeys(activeDocument, partKeys);

        for (int i = 0; i < partKeys.size(); i++) {

            CharacterMeshDocumentInstance document = characterMeshBranch.open(
                    characterMeshBranch.resolvePartMesh(activeDocument, partKeys.get(i)));

            if (document != null)
                documents.add(document);
        }
    }

    // Vertices \\

    public void selectVertex(int vertexIndex, CharacterEditorSelection selection) {

        CharacterMeshDocumentInstance document = getActiveMeshDocument();

        if (document != null && !characterSculptBranch.isTransforming())
            characterSelectionBranch.select(document, vertexIndex, selection);
    }

    // Everything when nothing is selected, nothing otherwise
    public void toggleSelection() {

        CharacterMeshDocumentInstance document = getActiveMeshDocument();

        if (document != null && !characterSculptBranch.isTransforming())
            characterSelectionBranch.toggleAll(document);
    }

    public void selectVertices(IntArrayList vertices, CharacterEditorSelection selection) {

        CharacterMeshDocumentInstance document = getActiveMeshDocument();

        if (document != null && !characterSculptBranch.isTransforming())
            characterSelectionBranch.select(document, vertices, selection);
    }

    // Transform \\

    // A transform asked for from the panel or by an edit that leads into one, for the viewport to take up; the axis
    // holds it to a line when given
    void requestTransform(CharacterEditorTransform transform, Vector3 axis) {

        this.requestedTransform = transform;
        this.requestedAxisSet = axis != null;

        if (axis != null)
            requestedAxis.set(axis);
    }

    // The transform asked for, cleared as it is taken
    public CharacterEditorTransform takeTransformRequest() {

        CharacterEditorTransform transform = requestedTransform;
        this.requestedTransform = null;

        return transform;
    }

    public boolean hasRequestedAxis() {
        return requestedAxisSet;
    }

    public Vector3 getRequestedAxis() {
        return requestedAxis;
    }

    public boolean beginTransform() {

        CharacterMeshDocumentInstance document = getActiveMeshDocument();

        if (document == null || mode != CharacterEditorMode.MESH || !characterSculptBranch.beginTransform(document))
            return false;

        notifyChanged();
        return true;
    }

    public void updateTransform(CharacterEditorTransform transform, Vector3 vector, float amount) {
        characterSculptBranch.updateTransform(transform, vector, amount);
    }

    public void confirmTransform() {
        characterSculptBranch.confirmTransform();
    }

    public void cancelTransform() {
        characterSculptBranch.cancelTransform();
    }

    public boolean isTransforming() {
        return characterSculptBranch.isTransforming();
    }

    public Vector3 getTransformPivot() {
        return characterSculptBranch.getTransformPivot();
    }

    // Strokes \\

    // A brush stroke on the active part, its shape in Sculpt mode and its weights otherwise
    public void beginStroke() {

        CharacterMeshDocumentInstance document = getActiveMeshDocument();

        if (document == null)
            return;

        if (mode == CharacterEditorMode.SCULPT)
            characterShapeBranch.beginStroke(document);
        else
            characterWeightBranch.beginStroke(document);
    }

    // One step of the brush over the vertices under it, each at the strength the brush has there
    public void stroke(IntArrayList vertices, FloatArrayList strengths, CharacterStampStruct stamp) {

        CharacterMeshDocumentInstance document = getActiveMeshDocument();

        if (document == null || !document.hasOpenStep())
            return;

        EditorTool tool = toolManager.getTool(mode.getToolCanvas());

        if (mode == CharacterEditorMode.SCULPT) {
            characterShapeBranch.stroke(document, tool, vertices, strengths, stamp);
            return;
        }

        if (tool == EditorTool.SMOOTH) {
            characterWeightBranch.blur(document, vertices, strengths);
            return;
        }

        characterWeightBranch.paint(
                document, selectedBoneIndex, vertices, strengths, tool == EditorTool.ERASE ? 0f : weightTarget);
    }

    public void endStroke() {

        CharacterMeshDocumentInstance document = getActiveMeshDocument();

        if (document == null)
            return;

        if (mode == CharacterEditorMode.SCULPT)
            characterShapeBranch.endStroke(document);
        else
            characterWeightBranch.endStroke(document);
    }

    // History \\

    // A step back in the history of what the mode edits: the rig in Rig mode, the active part's mesh otherwise
    public void undo() {

        if (mode == CharacterEditorMode.RIG) {
            reportHistory(syncRig() && characterRigBranch.undo(), true);
            return;
        }

        CharacterMeshDocumentInstance document = getActiveMeshDocument();

        if (document == null || characterSculptBranch.isTransforming())
            return;

        boolean undone = characterHistoryBranch.undo(document);

        if (undone)
            characterMeshBranch.upload(document);

        reportHistory(undone, true);
    }

    public void redo() {

        if (mode == CharacterEditorMode.RIG) {
            reportHistory(syncRig() && characterRigBranch.redo(), false);
            return;
        }

        CharacterMeshDocumentInstance document = getActiveMeshDocument();

        if (document == null || characterSculptBranch.isTransforming())
            return;

        boolean redone = characterHistoryBranch.redo(document);

        if (redone)
            characterMeshBranch.upload(document);

        reportHistory(redone, false);
    }

    private void reportHistory(boolean stepped, boolean back) {

        if (back)
            setStatusMessage(stepped
                    ? EditorSetting.CHARACTER_EDITOR_MESSAGE_UNDONE
                    : EditorSetting.CHARACTER_EDITOR_MESSAGE_NOTHING_TO_UNDO);
        else
            setStatusMessage(stepped
                    ? EditorSetting.CHARACTER_EDITOR_MESSAGE_REDONE
                    : EditorSetting.CHARACTER_EDITOR_MESSAGE_NOTHING_TO_REDO);
    }

    // Rig \\

    // Follows the rig the active character is built on; false while there is none to edit
    private boolean syncRig() {

        characterRigBranch.sync(activeDocument != null && activeDocument.hasPreview()
                ? activeDocument.getPreviewData().getRigHandle()
                : null);

        return characterRigBranch.hasRig();
    }

    public void selectRigBone(int boneIndex) {

        if (syncRig() && !characterRigBranch.isGesturing())
            characterRigBranch.selectBone(boneIndex);
    }

    // Opens a drag on the selected bone in the viewport; false when there is no bone to drag
    public boolean beginRigGesture() {
        return mode == CharacterEditorMode.RIG && syncRig() && characterRigBranch.beginGesture();
    }

    public void moveRigJoint(Vector3 worldMove) {

        if (characterRigBranch.isGesturing())
            characterRigBranch.moveGesture(worldMove);
    }

    public void scaleRigBone(float factor) {

        if (characterRigBranch.isGesturing())
            characterRigBranch.scaleGesture(factor);
    }

    public void endRigGesture() {
        characterRigBranch.endGesture();
    }

    public void cancelRigGesture() {
        characterRigBranch.cancelGesture();
    }

    public boolean isRigGesturing() {
        return characterRigBranch.isGesturing();
    }

    // A child of the selected bone with its joint at a point in the world
    public void addRigBone(Vector3 worldPoint) {

        if (syncRig() && !characterRigBranch.isGesturing())
            characterRigBranch.addBone(worldPoint);
    }

    public void removeRigBone(int boneIndex) {

        if (syncRig() && !characterRigBranch.isGesturing())
            characterRigBranch.removeBone(boneIndex);
    }

    void saveRig() {

        if (saveRigFile())
            setStatusMessage(EditorSetting.CHARACTER_EDITOR_MESSAGE_SAVED + characterRigBranch.getRigName());
    }

    void revertRig() {

        revertRigFile();
        setStatusMessage(EditorSetting.CHARACTER_EDITOR_MESSAGE_REVERTED + characterRigBranch.getRigName());
    }

    // The rig written when it holds unsaved changes; false, after saying why, when it could not be
    private boolean saveRigFile() {

        characterRigBranch.endGesture();

        return !syncRig() || characterRigBranch.save();
    }

    private void revertRigFile() {

        characterRigBranch.cancelGesture();

        if (syncRig())
            characterRigBranch.revert();
    }

    // Mesh Files \\

    void saveMesh(CharacterMeshDocumentInstance document) {
        setStatusMessage(characterMeshBranch.save(document)
                ? EditorSetting.CHARACTER_EDITOR_MESSAGE_SAVED + document.getMeshName()
                : EditorSetting.CHARACTER_EDITOR_MESSAGE_MESH_NOT_SAVED);
    }

    void revertMesh(CharacterMeshDocumentInstance document) {
        setStatusMessage(characterMeshBranch.revert(document)
                ? EditorSetting.CHARACTER_EDITOR_MESSAGE_REVERTED + document.getMeshName()
                : EditorSetting.CHARACTER_EDITOR_MESSAGE_MESH_NOT_REVERTED);
    }

    // Every open mesh with unsaved changes written; false when any could not be
    private boolean saveDirtyMeshes() {

        boolean saved = true;

        characterSculptBranch.confirmTransform();
        meshDocuments.clear();
        characterMeshBranch.collectDirtyDocuments(meshDocuments);

        for (int i = 0; i < meshDocuments.size(); i++)
            saved &= characterMeshBranch.save(meshDocuments.get(i));

        return saved;
    }

    private void revertDirtyMeshes() {

        characterSculptBranch.cancelTransform();
        meshDocuments.clear();
        characterMeshBranch.collectDirtyDocuments(meshDocuments);

        for (int i = 0; i < meshDocuments.size(); i++)
            characterMeshBranch.revert(meshDocuments.get(i));
    }

    // Options \\

    void setMirror(boolean mirror) {

        this.mirror = mirror;
        notifyChanged();
    }

    void setXray(boolean xray) {

        this.xray = xray;
        notifyChanged();
    }

    void setProportional(boolean proportional) {

        this.proportional = proportional;
        notifyChanged();
    }

    void setProportionalRadius(float proportionalRadius) {

        this.proportionalRadius = proportionalRadius;
        notifyChanged();
    }

    void setSnapStep(float snapStep) {

        this.snapStep = snapStep;
        notifyChanged();
    }

    void setPartShape(CharacterEditorShape partShape) {

        this.partShape = partShape;
        notifyChanged();
    }

    void setPartSize(float partSize) {

        this.partSize = partSize;
        notifyChanged();
    }

    void setPartTextureName(String partTextureName) {

        this.partTextureName = partTextureName;
        notifyChanged();
    }

    void setWeightTarget(float weightTarget) {

        this.weightTarget = weightTarget;
        notifyChanged();
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

    public String getSelectedBoneName() {

        RigHandle rigHandle = activeDocument != null && activeDocument.hasPreview()
                ? activeDocument.getPreviewData().getRigHandle()
                : null;

        return rigHandle != null && selectedBoneIndex < rigHandle.getBoneCount()
                ? rigHandle.getBone(selectedBoneIndex).getName()
                : "";
    }

    public String getActivePartKey() {
        return activePartKey;
    }

    public boolean isMirror() {
        return mirror;
    }

    public boolean isXray() {
        return xray;
    }

    public boolean isProportional() {
        return proportional;
    }

    public float getProportionalRadius() {
        return proportionalRadius;
    }

    public float getSnapStep() {
        return snapStep;
    }

    public CharacterEditorShape getPartShape() {
        return partShape;
    }

    public float getPartSize() {
        return partSize;
    }

    public String getPartTextureName() {
        return partTextureName;
    }

    public float getWeightTarget() {
        return weightTarget;
    }

    // How strongly the brush strokes, the Strength option of the mode's tool in use
    public float getBrushStrength() {
        return toolManager.getOption(mode.getToolCanvas(), ToolOption.STRENGTH);
    }

    // What the viewport's tools work on: the mesh, its sculpt, its weights or the rig, nothing in the other modes
    public ToolCanvas getToolCanvas() {
        return mode.getToolCanvas();
    }

    // True while a mesh mode is open and the tool chosen for it strokes
    public boolean isBrushTool() {
        return mode.isMeshMode() && toolManager.getTool(mode.getToolCanvas()).isStroke();
    }

    // The brush's radius, in window pixels
    public float getBrushRadius() {
        return toolManager.getOption(mode.getToolCanvas(), ToolOption.SIZE);
    }

    // The transform the mesh's tool drags, null while the tool in use is not one
    public CharacterEditorTransform getToolTransform() {
        return mode == CharacterEditorMode.MESH
                ? CharacterEditorTransform.forTool(toolManager.getTool(ToolCanvas.CHARACTER_MESH))
                : null;
    }

    public int getRigBoneCount() {
        return syncRig() ? characterRigBranch.getBoneCount() : 0;
    }

    public int getSelectedRigBoneIndex() {
        return syncRig() ? characterRigBranch.getSelectedBone() : EngineSetting.INDEX_NOT_FOUND;
    }

    // A bone's joint in the world as the rig is being edited
    public Matrix4 getRigBoneWorldMatrix(int boneIndex) {
        return characterRigBranch.getWorldMatrix(boneIndex);
    }

    public Vector3 getRigBoneSize(int boneIndex) {
        return characterRigBranch.getSize(boneIndex);
    }

    public EditorTool getRigTool() {
        return toolManager.getTool(ToolCanvas.CHARACTER_RIG);
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

    private boolean hasDirtyWornMesh() {

        collectWornMeshDocuments(meshDocuments);

        for (int i = 0; i < meshDocuments.size(); i++)
            if (meshDocuments.get(i).isDirty())
                return true;

        return false;
    }

    private void appendMeshStatus(StringBuilder status) {

        CharacterMeshDocumentInstance document = getActiveMeshDocument();

        if (document == null)
            return;

        status.append(EditorSetting.CHARACTER_VIEW_STATUS_SEPARATOR).append(document.getMeshName());

        if (document.isDirty())
            status.append(EditorSetting.CHARACTER_VIEW_DIRTY_MARKER);

        if (characterSculptBranch.isTransforming())
            status.append(EditorSetting.CHARACTER_VIEW_STATUS_SEPARATOR)
                    .append(EditorSetting.CHARACTER_EDITOR_STATUS_TRANSFORM_HINT);
        else
            appendHint(status);
    }

    private void appendHint(StringBuilder status) {

        if (mode.getHint() != null)
            status.append(EditorSetting.CHARACTER_VIEW_STATUS_SEPARATOR).append(mode.getHint());
    }

    private void appendDocumentStatus(StringBuilder status) {

        status.append(activeDocument.getTemplateName());

        if (infoManager.isDocumentDirty(EditorSetting.INFO_SCHEMA_ENTITIES, activeDocument.getTemplateName())
                || hasDirtyWornMesh()
                || syncRig() && characterRigBranch.isDirty())
            status.append(EditorSetting.CHARACTER_VIEW_DIRTY_MARKER);

        status.append(EditorSetting.CHARACTER_VIEW_STATUS_SEPARATOR).append(mode.getLabel());

        if (!mode.holdsBindPose())
            status.append(EditorSetting.CHARACTER_VIEW_STATUS_SEPARATOR).append(pose.getLabel());

        if (mode.isMeshMode())
            appendMeshStatus(status);
        else if (mode == CharacterEditorMode.RIG && syncRig())
            characterRigBranch.appendStatus(status);

        if (!mode.isMeshMode())
            appendHint(status);

        if (activeDocument.hasPreviewError())
            status.append(EditorSetting.CHARACTER_VIEW_STATUS_SEPARATOR)
                    .append(EditorSetting.CHARACTER_VIEW_MESSAGE_PREVIEW_REFUSED)
                    .append(activeDocument.getPreviewError());
    }
}
