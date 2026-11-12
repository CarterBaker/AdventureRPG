package editor.bootstrap.charactereditorpipeline.charactereditormanager;

import java.util.Arrays;

import application.bootstrap.entitypipeline.appearance.AppearanceData;
import application.bootstrap.entitypipeline.appearance.AppearanceHandle;
import application.bootstrap.entitypipeline.race.RaceData;
import application.bootstrap.geometrypipeline.mesh.MeshHandle;
import application.bootstrap.geometrypipeline.rig.RigHandle;
import editor.bootstrap.charactereditorpipeline.characterdocument.CharacterDocumentInstance;
import editor.bootstrap.charactereditorpipeline.charactermesh.CharacterMeshDocumentInstance;
import editor.bootstrap.charactereditorpipeline.charactermesh.CharacterStampStruct;
import editor.bootstrap.charactereditorpipeline.util.CharacterEditorColor;
import editor.bootstrap.charactereditorpipeline.util.CharacterEditorMode;
import editor.bootstrap.charactereditorpipeline.util.CharacterEditorPose;
import editor.bootstrap.charactereditorpipeline.util.CharacterEditorRegion;
import editor.bootstrap.charactereditorpipeline.util.CharacterEditorSelection;
import editor.bootstrap.charactereditorpipeline.util.CharacterEditorShape;
import editor.bootstrap.charactereditorpipeline.util.CharacterEditorSide;
import editor.bootstrap.charactereditorpipeline.util.CharacterEditorTransform;
import editor.bootstrap.infopipeline.infoentry.InfoEntryStruct;
import editor.bootstrap.infopipeline.infomanager.InfoManager;
import editor.bootstrap.toolpipeline.toolmanager.ToolManager;
import editor.bootstrap.toolpipeline.util.EditorTool;
import editor.bootstrap.toolpipeline.util.ToolCanvas;
import editor.bootstrap.toolpipeline.util.ToolOption;
import editor.runtime.characterview.panel.CharacterViewRowStruct;
import editor.runtime.characterview.util.CharacterViewUtility;
import editor.runtime.EditorSetting;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.io.FileUtility;
import engine.util.mathematics.matrices.Matrix4;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class CharacterEditorManager extends ManagerPackage {

    /*
     * Owns the character editor's shared state — the open races, the active
     * one, the panel mode and selection, and how the preview stands, is
     * built and is colored, made as one of the race's genders at a time in
     * the model that gender requires. Races are ARPG entries owned by
     * InfoManager: selecting one there opens it here, as does selecting an
     * entity, which opens the race it is made of; every edit goes back
     * through it, and every change to an open race, made here or in the
     * Info Panel, rebuilds its preview live. The Mesh, Sculpt and Weights
     * modes edit the rigged mesh of the part picked — the body or a default
     * feature — with the tool options and the transform asked for held
     * here, and the Rig mode the rig the character is built on; each is a
     * tool canvas whose tool and options are ToolManager's: on the mesh and
     * the weights Select sweeps the brush over the vertices it selects,
     * each as much as the tip covers it, and on the mesh Move, Rotate and
     * Scale drag the selection, each vertex as far as it is selected; on
     * the sculpt every tool strokes the surface, on the weights the brush
     * strokes with BrushManager's brush in use, Brush drawing toward the
     * target weight, Erase toward none and Smooth blending between
     * neighbours, and on the rig the tools pick, drag, size, add and remove
     * bones. Save and Revert carry every open mesh's changes and the rig's
     * with the race's. The rig's spring chains — the hair's laid on the locks
     * its cards are found in — are previewed live in the modelling tabs while
     * Preview Springs is on, the preview then playing the pose chosen and
     * travelling at its pace. Edits, previews, mesh files, selection,
     * geometry, sculpting, weights, the rig, hair locks, history, panel
     * values and panel rows each live in their own branch.
     */

    // Internal
    private InfoManager infoManager;
    private ToolManager toolManager;
    private CharacterEditBranch characterEditBranch;
    private CharacterPreviewBranch characterPreviewBranch;
    private CharacterLookBranch characterLookBranch;
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
    private CharacterRegionBranch characterRegionBranch;
    private CharacterDigitBranch characterDigitBranch;
    private CharacterHairBranch characterHairBranch;

    // Palette
    private Object2ObjectOpenHashMap<String, CharacterDocumentInstance> raceName2CharacterDocument;

    // Active
    private CharacterDocumentInstance activeDocument;
    private CharacterEditorMode mode;
    private int selectedBoneIndex;
    private int selectedAnchorIndex;

    // Preview — the pose struck: Bind, one of the rig's own poses, or a movement played
    private CharacterEditorPose pose;
    private int poseClipIndex;
    private boolean springPreview;
    private int genderIndex;
    private float weightRatio;
    private Vector3 headProportion;
    private int[] toneIndices;
    private float[] shapeValues;
    private float blink;

    // Mesh
    private String activePartKey;
    private CharacterEditorRegion region;
    private CharacterEditorSide[] region2Side;
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

    // Strokes
    private boolean strokeMoved;

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
        this.characterLookBranch = create(CharacterLookBranch.class);
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
        this.characterRegionBranch = create(CharacterRegionBranch.class);
        this.characterDigitBranch = create(CharacterDigitBranch.class);
        this.characterHairBranch = create(CharacterHairBranch.class);

        // Palette
        this.raceName2CharacterDocument = new Object2ObjectOpenHashMap<>();

        // Active
        this.mode = CharacterEditorMode.APPEARANCE;
        this.selectedAnchorIndex = EngineSetting.INDEX_NOT_FOUND;

        // Preview
        this.pose = CharacterEditorPose.REST;
        this.poseClipIndex = EngineSetting.INDEX_NOT_FOUND;
        this.weightRatio = EditorSetting.CHARACTER_VIEW_WEIGHT_RATIO;
        this.headProportion = new Vector3(1f, 1f, 1f);
        this.toneIndices = new int[CharacterEditorColor.VALUES.length];
        Arrays.fill(toneIndices, EngineSetting.INDEX_NOT_FOUND);
        this.shapeValues = new float[0];

        // Mesh
        this.activePartKey = EditorSetting.CHARACTER_EDITOR_PART_BODY;
        this.region = CharacterEditorRegion.WHOLE;
        this.region2Side = new CharacterEditorSide[CharacterEditorRegion.VALUES.length];
        Arrays.fill(region2Side, CharacterEditorSide.LEFT);
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
        infoManager.addSelectionListener(EditorSetting.INFO_SCHEMA_RACES, this::openEntry);
        infoManager.addChangeListener(EditorSetting.INFO_SCHEMA_RACES, this::refreshEntry);
        infoManager.addDeleteListener(EditorSetting.INFO_SCHEMA_RACES, this::closeEntry);
        infoManager.addSelectionListener(EditorSetting.INFO_SCHEMA_ENTITIES, this::openEntityRace);
        infoManager.addChangeListener(EditorSetting.INFO_SCHEMA_ENTITIES, this::openEntityRace);
        infoManager.addChangeListener(EditorSetting.INFO_SCHEMA_RIGS, characterRigBranch::refreshEntry);
    }

    // Races \\

    // An entity picked or changed anywhere opens the race it is made of, with every gender's model
    private void openEntityRace(InfoEntryStruct entityEntry) {

        ArpgObjectStruct entityArpg = entityEntry.getArpg();

        if (ArpgUtility.hasString(entityArpg, EditorSetting.CHARACTER_EDITOR_FIELD_RACE))
            openRace(entityArpg.get(EditorSetting.CHARACTER_EDITOR_FIELD_RACE).getAsString());
    }

    // Opens a race by name in every character tab, without changing what the Hierarchy has selected
    public void openRace(String raceName) {

        InfoEntryStruct raceEntry = infoManager.findEntry(EditorSetting.INFO_SCHEMA_RACES, raceName, raceName);

        if (raceEntry != null)
            openEntry(raceEntry);
    }

    private void openEntry(InfoEntryStruct entry) {

        CharacterDocumentInstance document = raceName2CharacterDocument.get(entry.getEntryName());

        if (document == null) {
            document = create(CharacterDocumentInstance.class);
            document.constructor(entry.getEntryName());
            raceName2CharacterDocument.put(entry.getEntryName(), document);
            characterPreviewBranch.rebuild(document, entry.getArpg());
        }

        if (document != activeDocument)
            activate(document);
    }

    private void refreshEntry(InfoEntryStruct entry) {

        CharacterDocumentInstance document = raceName2CharacterDocument.get(entry.getEntryName());

        if (document == null)
            return;

        characterPreviewBranch.rebuild(document, entry.getArpg());

        if (document == activeDocument)
            resolveSelection();

        notifyChanged();
    }

    private void closeEntry(InfoEntryStruct entry) {

        CharacterDocumentInstance document = raceName2CharacterDocument.remove(entry.getEntryName());

        if (document != null && document == activeDocument)
            activate(null);
    }

    private void activate(CharacterDocumentInstance document) {

        characterSculptBranch.cancelTransform();

        this.activeDocument = document;
        this.selectedBoneIndex = 0;
        this.selectedAnchorIndex = EngineSetting.INDEX_NOT_FOUND;
        this.activePartKey = EditorSetting.CHARACTER_EDITOR_PART_BODY;
        this.genderIndex = 0;
        this.requestedTransform = null;
        this.statusMessage = null;
        this.blink = 0f;
        Arrays.fill(toneIndices, EngineSetting.INDEX_NOT_FOUND);
        Arrays.fill(shapeValues, Float.NaN);
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

        setStatusMessage(infoManager.saveDocument(EditorSetting.INFO_SCHEMA_RACES, activeDocument.getRaceName())
                ? EditorSetting.CHARACTER_EDITOR_MESSAGE_SAVED + activeDocument.getRaceName()
                : infoManager.getRefusal());
    }

    public void revertActiveTemplate() {

        if (activeDocument == null)
            return;

        infoManager.revertDocument(EditorSetting.INFO_SCHEMA_RACES, activeDocument.getRaceName());
        revertDirtyMeshes();
        revertRigFile();
        setStatusMessage(EditorSetting.CHARACTER_EDITOR_MESSAGE_REVERTED + activeDocument.getRaceName());
    }

    public void copyActiveTemplate(String localName) {

        if (activeDocument == null || !isTemplateNameAvailable(localName))
            return;

        characterEditBranch.copyTemplate(getTemplateArpg(), toSiblingName(localName));
        setStatusMessage(EditorSetting.CHARACTER_EDITOR_MESSAGE_COPIED + localName);
    }

    // A copy of a race, every field in order, under a new name; it opens as the race the character tabs show
    public void copyRace(String sourceRaceName, String raceName) {

        InfoEntryStruct sourceEntry = infoManager.findEntry(
                EditorSetting.INFO_SCHEMA_RACES,
                sourceRaceName,
                sourceRaceName);

        if (sourceEntry != null)
            characterEditBranch.copyTemplate(sourceEntry.getArpg(), raceName);
    }

    public void deleteActiveTemplate() {

        if (activeDocument == null)
            return;

        String raceName = activeDocument.getRaceName();

        setStatusMessage(infoManager.deleteDocument(EditorSetting.INFO_SCHEMA_RACES, raceName)
                ? EditorSetting.CHARACTER_EDITOR_MESSAGE_DELETED + raceName
                : infoManager.getRefusal());
    }

    public boolean isTemplateNameAvailable(String localName) {
        return activeDocument != null
                && FileUtility.isValidFileName(localName, EditorSetting.NAME_INPUT_MAX_LENGTH)
                && !infoManager.hasDocument(EditorSetting.INFO_SCHEMA_RACES, toSiblingName(localName));
    }

    public boolean isActiveTemplateName(String localName) {
        return activeDocument != null && toLocalName(activeDocument.getRaceName()).equals(localName);
    }

    public String getActiveLocalName() {
        return activeDocument != null ? toLocalName(activeDocument.getRaceName()) : "";
    }

    // A new race beside the active one, in the same folder
    private String toSiblingName(String localName) {

        String raceName = activeDocument.getRaceName();
        int separator = raceName.lastIndexOf(EditorSetting.INFO_FOLDER_SEPARATOR);

        return separator == EngineSetting.INDEX_NOT_FOUND
                ? localName
                : raceName.substring(0, separator + EditorSetting.INFO_FOLDER_SEPARATOR.length()) + localName;
    }

    private String toLocalName(String raceName) {
        return raceName.substring(raceName.lastIndexOf(EditorSetting.INFO_FOLDER_SEPARATOR) + 1);
    }

    // Modes \\

    public void setMode(CharacterEditorMode mode) {

        characterSculptBranch.cancelTransform();

        if (mode != this.mode)
            characterMeshBranch.endShapeEdits();

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
        characterMeshBranch.endShapeEdits();

        this.activePartKey = partKey;
        this.requestedTransform = null;
        notifyChanged();
    }

    // Regions \\

    // The region of the modelling tab worked in last, its part picked whenever the character wears it
    public void setRegion(CharacterEditorRegion region) {

        if (region != this.region) {
            this.region = region;
            notifyChanged();
        }

        String partKey = resolveRegionPartKey(region);

        if (!partKey.equals(activePartKey) && activeDocument != null && activeDocument.hasPreview()
                && characterMeshBranch.resolvePartMesh(activeDocument, partKey) != null)
            selectPart(partKey);
    }

    // The part a region edits: the part picked while it is one the region holds, else its body, else its feature
    private String resolveRegionPartKey(CharacterEditorRegion region) {

        if (region.isWhole() || holdsPartKey(region, activePartKey))
            return activePartKey;

        return region.holdsBody()
                ? EditorSetting.CHARACTER_EDITOR_PART_BODY
                : ArpgUtility.toEnumName(region.getFeatureSlot());
    }

    // Whether a part, by its key, is one a region holds: the body where it holds a part of the rig, or its feature
    boolean holdsPartKey(CharacterEditorRegion region, String partKey) {

        if (region.isWhole())
            return true;

        if (partKey.equals(EditorSetting.CHARACTER_EDITOR_PART_BODY))
            return region.holdsBody();

        return region.holdsFeature() && partKey.equals(ArpgUtility.toEnumName(region.getFeatureSlot()));
    }

    // Whether a mesh is the part a region edits, the active part when that is one the region holds
    boolean isRegionPart(CharacterMeshDocumentInstance document, CharacterEditorRegion region) {

        if (region.isWhole())
            return true;

        CharacterMeshDocumentInstance active = getActiveMeshDocument();
        return document == active && holdsPartKey(region, activePartKey);
    }

    // Whether the open part is the region's feature rather than its body
    boolean isRegionFeatureOpen(CharacterEditorRegion region) {
        return region.holdsFeature() && activePartKey.equals(ArpgUtility.toEnumName(region.getFeatureSlot()));
    }

    // How far a vertex of a mesh belongs to a region, the share each stroke reaching it is held to
    public float getRegionShare(CharacterMeshDocumentInstance document, CharacterEditorRegion region, int vertexIndex) {
        return Math.max(characterRegionBranch.getShare(document, region, vertexIndex), 0f);
    }

    // Whether a vertex of a mesh is picked and selected in a region
    public boolean isInRegion(CharacterMeshDocumentInstance document, CharacterEditorRegion region, int vertexIndex) {
        return characterRegionBranch.holds(document, region, vertexIndex);
    }

    // Whether a vertex of a mesh is held by the region of the tab worked in last
    public boolean isInRegion(CharacterMeshDocumentInstance document, int vertexIndex) {
        return characterRegionBranch.holds(document, region, vertexIndex);
    }

    // Whether the region's part is the one open, so a tab of a part the character does not wear edits nothing
    public boolean isRegionOpen(CharacterEditorRegion region) {

        CharacterMeshDocumentInstance document = getActiveMeshDocument();
        return document != null && isRegionPart(document, region);
    }

    public boolean isBoneInRegion(CharacterEditorRegion region, RigHandle rigHandle, int boneIndex) {
        return characterRegionBranch.holdsBone(region, rigHandle, boneIndex);
    }

    public boolean isRigBoneInRegion(CharacterEditorRegion region, int boneIndex) {
        return syncRig() && characterRegionBranch.holdsRigBone(region, boneIndex);
    }

    public CharacterEditorRegion getRegion() {
        return region;
    }

    // Sides \\

    // Whether a region's part has a twin across the body, so it is shown and framed one side at a time
    public boolean isSided(CharacterEditorRegion region) {

        RigHandle rigHandle = resolveRegionRig();
        return rigHandle != null && characterRegionBranch.isSided(region, rigHandle);
    }

    public CharacterEditorSide getSide(CharacterEditorRegion region) {
        return region2Side[region.ordinal()];
    }

    void setSide(CharacterEditorRegion region, CharacterEditorSide side) {

        if (region2Side[region.ordinal()] == side)
            return;

        region2Side[region.ordinal()] = side;
        notifyChanged();
    }

    // Whether a vertex stands on the side a sided region shows; every vertex does for any other region
    public boolean isOnRegionSide(CharacterMeshDocumentInstance document, int vertexIndex) {
        return !isSided(region) || getSide(region).holdsX(document.getMesh().getX(vertexIndex));
    }

    // The bone a region is framed about: the first bone of its part on the side shown, the bone its own bones hang
    // from for one that names them, INDEX_NOT_FOUND for the whole character or a rig that names no such part
    public int getRegionFrameBone(CharacterEditorRegion region, RigHandle rigHandle) {
        return characterRegionBranch.resolveFrameBone(region, getSide(region), rigHandle);
    }

    // Whether a bone of a loaded rig is shown in a region: one it holds, on the side shown where it is sided
    public boolean isBoneShown(CharacterEditorRegion region, RigHandle rigHandle, int boneIndex) {
        return characterRegionBranch.holdsBone(region, rigHandle, boneIndex)
                && (!characterRegionBranch.isSided(region, rigHandle)
                || getSide(region).namesBone(rigHandle.getBone(boneIndex).getName()));
    }

    // Whether a bone of the rig being edited is shown in a region: one it holds, on the side shown where it is sided
    public boolean isRigBoneShown(CharacterEditorRegion region, int boneIndex) {

        if (!isRigBoneInRegion(region, boneIndex))
            return false;

        return !isSided(region) || getSide(region).namesBone(characterRigBranch.getBoneName(boneIndex));
    }

    private RigHandle resolveRegionRig() {
        return activeDocument != null && activeDocument.hasPreview()
                ? activeDocument.getPreviewData().getRigHandle()
                : null;
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

        if (document == null || mode != CharacterEditorMode.MESH || refusePosedEdit()
                || !characterSculptBranch.beginTransform(document))
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

    // Vertices are moved only in the bind pose, where they stand as the mesh file holds them; struck in any other
    // pose, the edit is refused and the status says why
    private boolean refusePosedEdit() {

        if (!isPosed())
            return false;

        setStatusMessage(EditorSetting.CHARACTER_EDITOR_MESSAGE_POSED);
        return true;
    }

    public boolean isTransforming() {
        return characterSculptBranch.isTransforming();
    }

    public Vector3 getTransformPivot() {
        return characterSculptBranch.getTransformPivot();
    }

    // Strokes \\

    // A brush stroke on the active part: its selection with the Select tool, combined with it as asked, its shape in
    // Sculpt mode and its weights otherwise
    public void beginStroke(CharacterEditorSelection selection) {

        CharacterMeshDocumentInstance document = getActiveMeshDocument();

        if (document == null)
            return;

        if (isSelectStroke())
            characterSelectionBranch.beginStroke(document, selection);
        else if (mode == CharacterEditorMode.SCULPT) {
            if (!refusePosedEdit())
                characterShapeBranch.beginStroke(document);
        }
        else
            characterWeightBranch.beginStroke(document);
    }

    // One step of the brush over the vertices under it, each at the strength the brush has there
    public void stroke(IntArrayList vertices, FloatArrayList strengths, CharacterStampStruct stamp) {

        CharacterMeshDocumentInstance document = getActiveMeshDocument();

        if (document == null)
            return;

        if (characterSelectionBranch.isStroking()) {
            characterSelectionBranch.stroke(document, vertices, strengths);
            return;
        }

        if (!document.hasOpenStep())
            return;

        EditorTool tool = toolManager.getTool(mode.getToolCanvas());

        this.strokeMoved = true;

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

    // Every stamp a frame's stroke laid shown in one rebuild of the part, once the frame's stamps are done
    public void showStroke() {

        CharacterMeshDocumentInstance document = getActiveMeshDocument();

        if (!strokeMoved || document == null)
            return;

        this.strokeMoved = false;
        characterMeshBranch.preview(document);
    }

    public void endStroke() {

        CharacterMeshDocumentInstance document = getActiveMeshDocument();

        this.strokeMoved = false;

        if (characterSelectionBranch.isStroking()) {
            characterSelectionBranch.endStroke();
            return;
        }

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

    // Digits \\

    // Bone chains laid on every digit found below the region's hand or foot bones in the body's mesh
    void rigDigits() {

        CharacterMeshDocumentInstance document = getActiveMeshDocument();

        if (!region.holdsBody() || document == null || !isRegionPart(document, region)) {
            setStatusMessage(EditorSetting.CHARACTER_RIG_MESSAGE_NO_REGION);
            return;
        }

        if (syncRig() && !characterRigBranch.isGesturing())
            characterRigBranch.addDigits(document.getMesh(), region);
    }

    // Every vertex of the region's hand or foot bones weighed onto the digit bones the rig lays below them
    void weighDigits(CharacterMeshDocumentInstance document) {

        if (!region.holdsBody() || !isRegionPart(document, region)) {
            setStatusMessage(EditorSetting.CHARACTER_RIG_MESSAGE_NO_REGION);
            return;
        }

        if (!characterWeightBranch.weighDigits(document, region))
            setStatusMessage(EditorSetting.CHARACTER_RIG_MESSAGE_UNRIGGED);
    }

    // Hair \\

    // Spring chains laid on every lock of long hair found in the cards of the hair the character wears
    void rigHairLocks() {

        CharacterMeshDocumentInstance document = getActiveMeshDocument();

        if (!region.isRootPinned() || document == null || !isRegionPart(document, region)) {
            setStatusMessage(EditorSetting.CHARACTER_RIG_MESSAGE_HAIR_REGION);
            return;
        }

        if (syncRig() && !characterRigBranch.isGesturing())
            characterRigBranch.addHairLocks(document.getMesh());
    }

    // Every long hair card weighed onto the hair chain of the loaded rig it hangs nearest
    void weighHairLocks(CharacterMeshDocumentInstance document) {

        if (!region.isRootPinned() || !isRegionPart(document, region)) {
            setStatusMessage(EditorSetting.CHARACTER_RIG_MESSAGE_HAIR_REGION);
            return;
        }

        if (!characterWeightBranch.weighLocks(document))
            setStatusMessage(EditorSetting.CHARACTER_RIG_MESSAGE_HAIR_UNRIGGED);
    }

    // The modelling tabs' preview leaves the bind pose to play the pose chosen with its springs simulated, or holds it
    void toggleSpringPreview() {

        this.springPreview = !springPreview;
        setStatusMessage(springPreview
                ? EditorSetting.CHARACTER_RIG_MESSAGE_PREVIEW_ON
                : EditorSetting.CHARACTER_RIG_MESSAGE_PREVIEW_OFF);
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

    // The pose every outliner picks, stepped through Bind, each pose the rig names, then the movements played
    void stepPose(int direction) {

        RigHandle rigHandle = resolveRegionRig();
        int clipCount = rigHandle != null ? rigHandle.getPoseCount() : 0;
        int slot = Math.floorMod(getPoseSlot(clipCount) + direction, clipCount + CharacterEditorPose.VALUES.length);

        this.poseClipIndex = slot >= 1 && slot <= clipCount ? slot - 1 : EngineSetting.INDEX_NOT_FOUND;
        this.pose = slot > clipCount ? CharacterEditorPose.VALUES[slot - clipCount] : CharacterEditorPose.REST;
        notifyChanged();
    }

    private int getPoseSlot(int clipCount) {

        if (poseClipIndex != EngineSetting.INDEX_NOT_FOUND && poseClipIndex < clipCount)
            return poseClipIndex + 1;

        return pose == CharacterEditorPose.REST ? 0 : pose.ordinal() + clipCount;
    }

    // The gender the preview is made as, by its place in the race's genders; it wears that gender's model, so the
    // mesh modes edit that body and its features from then on
    void setGenderIndex(int genderIndex) {

        characterSculptBranch.cancelTransform();
        characterMeshBranch.endShapeEdits();

        this.genderIndex = genderIndex;
        this.requestedTransform = null;
        Arrays.fill(shapeValues, Float.NaN);
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

    // A preview slider's value, the array grown to hold every slider the race lists, untouched sliders left NaN
    public void setShapeValue(int shapeIndex, float value) {

        if (shapeIndex >= shapeValues.length) {

            int length = shapeValues.length;

            this.shapeValues = Arrays.copyOf(shapeValues, shapeIndex + 1);
            Arrays.fill(shapeValues, length, shapeValues.length, Float.NaN);
        }

        shapeValues[shapeIndex] = value;
        notifyChanged();
    }

    // The preview look put on a viewport's appearance: neutral as the mesh is authored, a still face never blinking
    public void applyLook(
            AppearanceHandle appearance,
            AppearanceData appearanceData,
            boolean neutral,
            boolean stillFace) {
        characterLookBranch.apply(appearance, appearanceData, neutral, stillFace);
    }

    // Every preview slider back where the preview's gender starts it
    public void resetShapeValues() {

        Arrays.fill(shapeValues, Float.NaN);
        notifyChanged();
    }

    void setBlink(float blink) {

        this.blink = blink;
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

    // The active race's live ARPG, as InfoManager holds it
    ArpgObjectStruct getTemplateArpg() {

        InfoEntryStruct entry = infoManager.findEntry(
                EditorSetting.INFO_SCHEMA_RACES,
                activeDocument.getRaceName(),
                activeDocument.getRaceName());

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

    // The rig's pose struck, by its place in the rig's poses, INDEX_NOT_FOUND while a movement or Bind is
    public int getPoseClipIndex() {

        RigHandle rigHandle = resolveRegionRig();

        return rigHandle != null && poseClipIndex < rigHandle.getPoseCount()
                ? poseClipIndex
                : EngineSetting.INDEX_NOT_FOUND;
    }

    public String getPoseLabel() {

        int clipIndex = getPoseClipIndex();

        return clipIndex != EngineSetting.INDEX_NOT_FOUND
                ? CharacterViewUtility.toResourceLabel(resolveRegionRig().getPoseName(clipIndex))
                : pose.getLabel();
    }

    // Whether a modelling mode shows the character out of its bind pose, so its vertices may not be moved
    public boolean isPosed() {
        return mode.isMeshMode() && mode.picksPose()
                && (pose != CharacterEditorPose.REST || getPoseClipIndex() != EngineSetting.INDEX_NOT_FOUND);
    }

    public boolean isSpringPreview() {
        return springPreview;
    }

    public int getGenderIndex() {
        return genderIndex;
    }

    // The gender the preview is made as, held to the genders the active race allows
    public short getPreviewGenderID() {

        if (activeDocument == null || !activeDocument.hasPreview())
            return 0;

        RaceData raceData = activeDocument.getPreviewData();
        return raceData.getGenderID(Math.min(genderIndex, raceData.getGenderCount() - 1));
    }

    // Makes the preview the gender given when the active race allows it, the path every other tab takes to pick one
    public void selectPreviewGender(short genderID) {

        if (activeDocument == null || !activeDocument.hasPreview())
            return;

        RaceData raceData = activeDocument.getPreviewData();

        for (int i = 0; i < raceData.getGenderCount(); i++)
            if (raceData.getGenderID(i) == genderID && i != genderIndex)
                setGenderIndex(i);
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

    // A preview slider's value: the one set on it, else where the preview's gender starts it; 0 without a preview
    public float getShapeValue(int shapeIndex) {

        if (shapeIndex < shapeValues.length && !Float.isNaN(shapeValues[shapeIndex]))
            return shapeValues[shapeIndex];

        if (activeDocument == null || !activeDocument.hasPreview() || !activeDocument.getPreviewData().hasAppearance())
            return 0f;

        AppearanceData appearanceData = activeDocument.getPreviewData().getAppearanceData();

        return shapeIndex < appearanceData.getShapeCount()
                ? appearanceData.getStartingShapeValue(getPreviewGenderID(), shapeIndex)
                : 0f;
    }

    public float getBlink() {
        return blink;
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

    // Mirror is held on in a sided region, so an edit to the side shown is always made to its twin
    public boolean isMirror() {
        return mirror || isSided(region);
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

    // How strongly the brush strokes, the Strength option of the mode's tool in use, in full for a tool without one
    public float getBrushStrength() {

        ToolCanvas canvas = mode.getToolCanvas();

        return toolManager.getTool(canvas).hasOption(ToolOption.STRENGTH)
                ? toolManager.getOption(canvas, ToolOption.STRENGTH)
                : 1f;
    }

    // What the viewport's tools work on: the mesh, its sculpt, its weights or the rig, nothing in the other modes
    public ToolCanvas getToolCanvas() {
        return mode.getToolCanvas();
    }

    // True while a mesh mode is open and the tool chosen for it strokes the brush there
    public boolean isBrushTool() {
        return mode.isMeshMode() && mode.getToolCanvas().strokes(toolManager.getTool(mode.getToolCanvas()));
    }

    // True while the brush selects rather than shaping or weighting
    public boolean isSelectStroke() {
        return isBrushTool() && toolManager.getTool(mode.getToolCanvas()) == EditorTool.SELECT;
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
        else if (mode.isMeshMode() && !region.isWhole() && !region.holdsBody() && !isRegionOpen(region))
            status.append(EditorSetting.CHARACTER_REGION_STATUS_NO_PART)
                    .append(ArpgUtility.toEnumName(region.getFeatureSlot()));
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

        status.append(activeDocument.getRaceName());

        if (infoManager.isDocumentDirty(EditorSetting.INFO_SCHEMA_RACES, activeDocument.getRaceName())
                || hasDirtyWornMesh()
                || syncRig() && characterRigBranch.isDirty())
            status.append(EditorSetting.CHARACTER_VIEW_DIRTY_MARKER);

        status.append(EditorSetting.CHARACTER_VIEW_STATUS_SEPARATOR).append(mode.getLabel());

        if (mode.picksPose())
            status.append(EditorSetting.CHARACTER_VIEW_STATUS_SEPARATOR).append(getPoseLabel());

        if (isSided(region))
            status.append(EditorSetting.CHARACTER_VIEW_STATUS_SEPARATOR).append(getSide(region).getLabel());

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
