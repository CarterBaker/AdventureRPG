package editor.bootstrap.charactereditorpipeline.charactereditormanager;

import java.io.File;
import java.util.Arrays;
import java.util.Objects;

import application.bootstrap.entitypipeline.appearance.AppearanceData;
import application.bootstrap.entitypipeline.appearance.AppearanceHandle;
import application.bootstrap.entitypipeline.feature.FeatureHandle;
import application.bootstrap.entitypipeline.feature.FeatureSlot;
import application.bootstrap.entitypipeline.inventory.EquipmentSlot;
import application.bootstrap.entitypipeline.race.RaceData;
import application.bootstrap.geometrypipeline.mesh.MeshHandle;
import application.bootstrap.geometrypipeline.mesh.RiggedMeshStruct;
import application.bootstrap.geometrypipeline.meshmanager.MeshManager;
import application.bootstrap.geometrypipeline.rig.RigHandle;
import application.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle;
import application.bootstrap.itempipeline.itemdefinition.ItemWornModelStruct;
import application.bootstrap.itempipeline.itemdefinitionmanager.ItemDefinitionManager;
import editor.bootstrap.charactereditorpipeline.characterdocument.CharacterDocumentInstance;
import editor.bootstrap.charactereditorpipeline.charactermesh.CharacterGroomStruct;
import editor.bootstrap.charactereditorpipeline.charactermesh.CharacterMeshDocumentInstance;
import editor.bootstrap.charactereditorpipeline.charactermesh.CharacterPaintCanvasStruct;
import editor.bootstrap.charactereditorpipeline.charactermesh.CharacterPaintStampStruct;
import editor.bootstrap.charactereditorpipeline.charactermesh.CharacterPartPlacementStruct;
import editor.bootstrap.charactereditorpipeline.charactermesh.CharacterStampStruct;
import editor.bootstrap.charactereditorpipeline.util.CharacterBoneDisplay;
import editor.bootstrap.charactereditorpipeline.util.CharacterBoneKind;
import editor.bootstrap.charactereditorpipeline.util.CharacterEditorColor;
import editor.bootstrap.charactereditorpipeline.util.CharacterEditorMode;
import editor.bootstrap.charactereditorpipeline.util.CharacterEditorPose;
import editor.bootstrap.charactereditorpipeline.util.CharacterRegionStruct;
import editor.bootstrap.charactereditorpipeline.util.CharacterEditorSelection;
import editor.bootstrap.charactereditorpipeline.util.CharacterEditorShape;
import editor.bootstrap.charactereditorpipeline.util.CharacterEditorSide;
import editor.bootstrap.charactereditorpipeline.util.CharacterEditorTransform;
import editor.bootstrap.charactereditorpipeline.util.CharacterMeshUtility;
import editor.bootstrap.commandpipeline.command.CommandStruct;
import editor.bootstrap.infopipeline.infoentry.InfoEntryStruct;
import editor.bootstrap.infopipeline.infomanager.InfoManager;
import editor.bootstrap.stamppipeline.stamp.StampHandle;
import editor.bootstrap.stamppipeline.stampmanager.StampManager;
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
import engine.util.mathematics.vectors.Vector4;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

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
     * bones; the Stamp mode joins body parts from the Stamps tab to the
     * body, their bones grafted onto the rig, and lays fur. Save and Revert
     * carry every open mesh's changes and the rig's with the race's. The
     * rig's spring chains — the hair's laid on the locks
     * its cards are found in — are previewed live on the modelling layers while
     * Preview Springs is on, the preview then playing the pose chosen and
     * travelling at its pace. Edits, previews, mesh files, selection,
     * geometry, sculpting, weights, the rig, hair locks, history, panel
     * values and panel rows each live in their own branch.
     */

    // Internal
    private InfoManager infoManager;
    private ToolManager toolManager;
    private MeshManager meshManager;
    private ItemDefinitionManager itemDefinitionManager;
    private CharacterEditBranch characterEditBranch;
    private CharacterPreviewBranch characterPreviewBranch;
    private CharacterLookBranch characterLookBranch;
    private CharacterColliderBranch characterColliderBranch;
    private CharacterPaintBranch characterPaintBranch;
    private CharacterPaintPropertyBranch characterPaintPropertyBranch;
    private CharacterBoneDisplayBranch characterBoneDisplayBranch;
    private CharacterUVBranch characterUVBranch;
    private CharacterControlBranch characterControlBranch;
    private CharacterClipBranch characterClipBranch;
    private CharacterSliderBranch characterSliderBranch;
    private CharacterPaintRowBranch characterPaintRowBranch;
    private CharacterPropertyBranch characterPropertyBranch;
    private CharacterRowBranch characterRowBranch;
    private CharacterHistoryBranch characterHistoryBranch;
    private CharacterMeshBranch characterMeshBranch;
    private CharacterSelectionBranch characterSelectionBranch;
    private CharacterWeightBranch characterWeightBranch;
    private CharacterSculptBranch characterSculptBranch;
    private CharacterShapeBranch characterShapeBranch;
    private CharacterTopologyBranch characterTopologyBranch;
    private CharacterEdgeBranch characterEdgeBranch;
    private CharacterGroomBranch characterGroomBranch;
    private CharacterCommandBranch characterCommandBranch;
    private CharacterRigBranch characterRigBranch;
    private CharacterRigPropertyBranch characterRigPropertyBranch;
    private CharacterRigRowBranch characterRigRowBranch;
    private CharacterMeshPropertyBranch characterMeshPropertyBranch;
    private CharacterMeshRowBranch characterMeshRowBranch;
    private CharacterRegionBranch characterRegionBranch;
    private CharacterDigitBranch characterDigitBranch;
    private CharacterHairBranch characterHairBranch;
    private CharacterPlacementBranch characterPlacementBranch;
    private CharacterGraftBranch characterGraftBranch;
    private CharacterWorkspaceBranch characterWorkspaceBranch;
    private CharacterFurBranch characterFurBranch;
    private StampManager stampManager;

    // Palette
    private Object2ObjectOpenHashMap<String, CharacterDocumentInstance> raceName2CharacterDocument;
    private Object2ObjectOpenHashMap<String, CharacterDocumentInstance> modelName2CharacterDocument;

    // Active
    private CharacterDocumentInstance activeDocument;
    private CharacterEditorMode mode;
    private int selectedBoneIndex;
    private int selectedAnchorIndex;

    // Gear — indexed by EquipmentSlot ordinal, the item every preview wears in the slot, null for none
    private ItemDefinitionHandle[] previewGear;

    // Preview — the pose struck: Bind, one of the rig's own poses, or a movement played
    private CharacterEditorPose pose;
    private int poseClipIndex;
    private boolean springPreview;

    // Edit Pose — a turn per bone in Euler degrees within its own frame, laid over the pose of every modelling layer
    // to move what a bone carries out of the way; never saved
    private ObjectArrayList<Vector3> editPoseTurns;
    private boolean editPosed;
    private Vector3 restTurn;
    private int genderIndex;
    private float weightRatio;
    private Vector3 headProportion;
    private int[] toneIndices;
    private float[] shapeValues;
    private float blink;

    // Mesh
    private String activePartKey;
    private CharacterRegionStruct region;

    // Picked Region — the region every tool tab that takes regions works on, kept by its key and found again among
    // the rig's regions whenever they are made again; the revision moves with the pick or the regions
    private String pickedRegionKey;
    private CharacterRegionStruct pickedRegion;
    private int pickedRegionsRevision;
    private int regionPickRevision;
    private Object2ObjectOpenHashMap<String, CharacterEditorSide> regionKey2Side;
    private boolean proportional;
    private float proportionalRadius;
    private float snapStep;

    // Hidden Bones — the bones whose eye in the Properties tab is shut, by name, each hiding every bone under it too
    private ObjectOpenHashSet<String> hiddenBoneNames;

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
    private String placementMessage;
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
        create(CharacterModelBranch.class);
        this.characterWorkspaceBranch = create(CharacterWorkspaceBranch.class);
        this.characterSelectionBranch = create(CharacterSelectionBranch.class);
        this.characterWeightBranch = create(CharacterWeightBranch.class);
        this.characterSculptBranch = create(CharacterSculptBranch.class);
        this.characterShapeBranch = create(CharacterShapeBranch.class);
        this.characterTopologyBranch = create(CharacterTopologyBranch.class);
        this.characterEdgeBranch = create(CharacterEdgeBranch.class);
        this.characterGroomBranch = create(CharacterGroomBranch.class);
        create(CharacterSymmetryBranch.class);
        this.characterCommandBranch = create(CharacterCommandBranch.class);
        this.characterRigBranch = create(CharacterRigBranch.class);
        this.characterColliderBranch = create(CharacterColliderBranch.class);
        this.characterPaintBranch = create(CharacterPaintBranch.class);
        this.characterPaintPropertyBranch = create(CharacterPaintPropertyBranch.class);
        this.characterBoneDisplayBranch = create(CharacterBoneDisplayBranch.class);
        this.characterUVBranch = create(CharacterUVBranch.class);
        this.characterControlBranch = create(CharacterControlBranch.class);
        this.characterClipBranch = create(CharacterClipBranch.class);
        this.characterSliderBranch = create(CharacterSliderBranch.class);
        this.characterPaintRowBranch = create(CharacterPaintRowBranch.class);
        create(CharacterMapBranch.class);
        this.characterRigPropertyBranch = create(CharacterRigPropertyBranch.class);
        this.characterRigRowBranch = create(CharacterRigRowBranch.class);
        this.characterMeshPropertyBranch = create(CharacterMeshPropertyBranch.class);
        this.characterMeshRowBranch = create(CharacterMeshRowBranch.class);
        this.characterRegionBranch = create(CharacterRegionBranch.class);
        this.characterDigitBranch = create(CharacterDigitBranch.class);
        this.characterHairBranch = create(CharacterHairBranch.class);
        this.characterPlacementBranch = create(CharacterPlacementBranch.class);
        this.characterGraftBranch = create(CharacterGraftBranch.class);
        this.characterFurBranch = create(CharacterFurBranch.class);

        // Palette
        this.raceName2CharacterDocument = new Object2ObjectOpenHashMap<>();
        this.modelName2CharacterDocument = new Object2ObjectOpenHashMap<>();

        // Active
        this.mode = CharacterEditorMode.APPEARANCE;
        this.selectedAnchorIndex = EngineSetting.INDEX_NOT_FOUND;

        // Gear
        this.previewGear = new ItemDefinitionHandle[EquipmentSlot.VALUES.length];

        // Preview
        this.pose = CharacterEditorPose.REST;
        this.poseClipIndex = EngineSetting.INDEX_NOT_FOUND;

        // Edit Pose
        this.editPoseTurns = new ObjectArrayList<>();
        this.restTurn = new Vector3();
        this.weightRatio = EditorSetting.CHARACTER_VIEW_WEIGHT_RATIO;
        this.headProportion = new Vector3(1f, 1f, 1f);
        this.toneIndices = new int[CharacterEditorColor.VALUES.length];
        Arrays.fill(toneIndices, EngineSetting.INDEX_NOT_FOUND);
        this.shapeValues = new float[0];

        // Mesh
        this.activePartKey = EditorSetting.CHARACTER_EDITOR_PART_BODY;
        this.region = CharacterRegionStruct.WHOLE;
        this.pickedRegion = CharacterRegionStruct.WHOLE;
        this.pickedRegionKey = pickedRegion.getKey();
        this.pickedRegionsRevision = EngineSetting.INDEX_NOT_FOUND;
        this.regionKey2Side = new Object2ObjectOpenHashMap<>();
        this.proportionalRadius = EditorSetting.CHARACTER_EDITOR_MESH_RADIUS_DEFAULT;
        this.snapStep = EditorSetting.CHARACTER_EDITOR_MESH_SNAP_DEFAULT;

        // Hidden Bones
        this.hiddenBoneNames = new ObjectOpenHashSet<>();

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
        this.meshManager = get(MeshManager.class);
        this.itemDefinitionManager = get(ItemDefinitionManager.class);
        this.stampManager = get(StampManager.class);
    }

    @Override
    protected void awake() {
        infoManager.addSelectionListener(EditorSetting.INFO_SCHEMA_RACES, this::openEntry);
        infoManager.addChangeListener(EditorSetting.INFO_SCHEMA_RACES, this::refreshEntry);
        infoManager.addDeleteListener(EditorSetting.INFO_SCHEMA_RACES, this::closeEntry);
        infoManager.addSelectionListener(EditorSetting.INFO_SCHEMA_ENTITIES, this::openEntityRace);
        infoManager.addChangeListener(EditorSetting.INFO_SCHEMA_ENTITIES, this::openEntityRace);
        infoManager.addChangeListener(EditorSetting.INFO_SCHEMA_RIGS, characterRigBranch::refreshEntry);
        infoManager.addChangeListener(EditorSetting.INFO_SCHEMA_BONE_CONTROLS, characterControlBranch::refreshEntry);
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

    // The active race's preview built again, as its rig's slider channels or controls now stand
    void rebuildActivePreview() {

        if (activeDocument == null)
            return;

        if (activeDocument.isModel()) {
            rebuildModelPreview(activeDocument);
            return;
        }

        InfoEntryStruct raceEntry = infoManager.findEntry(
                EditorSetting.INFO_SCHEMA_RACES,
                activeDocument.getRaceName(),
                activeDocument.getRaceName());

        if (raceEntry != null)
            refreshEntry(raceEntry);
    }

    private void closeEntry(InfoEntryStruct entry) {

        CharacterDocumentInstance document = raceName2CharacterDocument.remove(entry.getEntryName());

        if (document != null && document == activeDocument)
            activate(null);
    }

    // Models \\

    // A blank model made in the workspace on the rig, then opened in every character tab
    public void newModel(String modelName, String rigName) {

        if (!isModelNameAvailable(modelName) || !isModelRig(rigName))
            return;

        if (!characterWorkspaceBranch.create(modelName, rigName)) {
            setStatusMessage(EditorSetting.CHARACTER_MODEL_MESSAGE_NOT_WRITTEN + modelName);
            return;
        }

        openModel(modelName);
        setStatusMessage(EditorSetting.CHARACTER_MODEL_MESSAGE_CREATED + modelName);
    }

    public boolean isModelNameAvailable(String modelName) {
        return characterWorkspaceBranch.isNameAvailable(modelName);
    }

    public boolean isModelRig(String rigName) {
        return characterWorkspaceBranch.isRig(rigName);
    }

    // The model a file chosen in the open dialog holds, when it lies in the workspace's Characters folder
    public void openModelFile(File file) {

        String modelName = characterWorkspaceBranch.toModelName(file);

        if (modelName == null) {
            setStatusMessage(EditorSetting.WORKSPACE_MESSAGE_OUTSIDE + characterWorkspaceBranch.getFolder().getPath());
            return;
        }

        openModel(modelName);
    }

    private void openModel(String modelName) {

        CharacterDocumentInstance document = modelName2CharacterDocument.get(modelName);

        if (document == null) {

            document = characterWorkspaceBranch.open(modelName);

            if (document == null) {
                setStatusMessage(characterWorkspaceBranch.getRefusal());
                return;
            }

            modelName2CharacterDocument.put(modelName, document);
            rebuildModelPreview(document);
        }

        activate(document);
        setStatusMessage(document.isBlank()
                ? EditorSetting.CHARACTER_MODEL_MESSAGE_BLANK
                : EditorSetting.CHARACTER_MODEL_MESSAGE_OPENED + modelName);
    }

    // A workspace model shown in the character tabs when it is open or on disk, true when it is; nothing is written
    public boolean showWorkspaceModel(String modelName) {

        if (!modelName2CharacterDocument.containsKey(modelName) && !characterWorkspaceBranch.hasModel(modelName))
            return false;

        if (activeDocument == null || !modelName.equals(activeDocument.getModelName()))
            openModel(modelName);

        return true;
    }

    // A mesh made outside the character tabs (an item baked for them) opened as a workspace model: written and opened
    // when no model of that name is open with a body, or, when one is, its body replaced by the mesh as one edit, as a
    // model brought in from a file replaces it
    public void openBakedModel(String modelName, RiggedMeshStruct mesh) {

        CharacterDocumentInstance document = modelName2CharacterDocument.get(modelName);

        if (document == null || document.isBlank()) {

            if (!characterWorkspaceBranch.writeModel(modelName, mesh)) {
                setStatusMessage(characterWorkspaceBranch.getRefusal());
                return;
            }

            modelName2CharacterDocument.remove(modelName);
            openModel(modelName);
            return;
        }

        activate(document);

        CharacterMeshDocumentInstance meshDocument = characterMeshBranch.open(
                characterMeshBranch.resolvePartMesh(document, EditorSetting.CHARACTER_EDITOR_PART_BODY));

        if (meshDocument == null) {
            setStatusMessage(EditorSetting.CHARACTER_MODEL_MESSAGE_NOT_READ + modelName);
            return;
        }

        characterHistoryBranch.begin(meshDocument);
        meshDocument.getMesh().set(mesh);

        if (characterMeshBranch.finishEdit(meshDocument))
            setStatusMessage(EditorSetting.CHARACTER_MODEL_MESSAGE_OPENED + modelName);
    }

    private void rebuildModelPreview(CharacterDocumentInstance document) {

        if (document.isBlank())
            return;

        characterPreviewBranch.rebuild(document, document.getTemplateArpg());

        if (document == activeDocument)
            resolveSelection();

        notifyChanged();
    }

    // The folder the open dialog starts in for models
    public File getModelFolder() {
        return characterWorkspaceBranch.getFolder();
    }

    // True while the character open is a model whose body is not yet started
    public boolean isBlankModel() {
        return activeDocument != null && activeDocument.isBlank();
    }

    // A blank model's body started from the root part armed in the Stamps tab, at the rig's root, then the model
    // opened as any other; a part that joins a host cannot start one
    public void startModelBody() {

        StampHandle stampHandle = getActiveStamp();

        if (!isBlankModel() || stampHandle == null || !syncRig())
            return;

        if (stampHandle.hasPlug() || stampHandle.hasFur()) {
            setStatusMessage(EditorSetting.CHARACTER_MODEL_MESSAGE_ROOT_ONLY);
            return;
        }

        CharacterPartPlacementStruct placement = new CharacterPartPlacementStruct();

        characterPlacementBranch.resolveStart(stampHandle, placement);

        if (placement.getRefusal() != null) {
            setStatusMessage(placement.getRefusal());
            return;
        }

        RiggedMeshStruct mesh = characterGraftBranch.startBody(placement);

        if (mesh == null)
            return;

        disarmStamp();

        if (!characterWorkspaceBranch.writeBody(activeDocument, mesh)) {
            setStatusMessage(characterWorkspaceBranch.getRefusal());
            return;
        }

        rebuildModelPreview(activeDocument);
        setStatusMessage(EditorSetting.CHARACTER_STAMP_MESSAGE_PLACED + stampHandle.getDisplayName());
    }

    // True while the character open is a workspace model with a body, the one Import brings into the game
    public boolean canImportModel() {

        if (activeDocument != null && activeDocument.isModel() && !activeDocument.isBlank())
            return true;

        setStatusMessage(EditorSetting.CHARACTER_MODEL_MESSAGE_IMPORT_MODEL);
        return false;
    }

    public boolean isImportRace(String raceName) {
        return canImportModel() && characterWorkspaceBranch.isImportRace(activeDocument, raceName);
    }

    public boolean isImportGender(String genderName) {
        return characterWorkspaceBranch.isImportGender(genderName);
    }

    public boolean isImportEntity(String entityName) {
        return characterWorkspaceBranch.isImportEntity(entityName);
    }

    // The model open, saved first, brought into the game as the gender of the race, with an entity when one is named;
    // the race then opens in every character tab
    public void importModel(String raceName, String genderName, String entityName) {

        if (!isImportRace(raceName) || !isImportGender(genderName) || !isImportEntity(entityName))
            return;

        saveActiveTemplate();

        String refusal = characterWorkspaceBranch.importModel(activeDocument, raceName, genderName, entityName);

        if (refusal != null) {
            setStatusMessage(refusal);
            return;
        }

        openRace(raceName);
        setStatusMessage(EditorSetting.CHARACTER_MODEL_MESSAGE_IMPORTED + raceName);
    }

    // Whether the model open may be worn as the item: a model with a body, not baked from an item, and an item of
    // that full name worn on the body
    public boolean isImportWorn(String itemName) {
        return canImportModel() && !isItemModel() && characterWorkspaceBranch.isImportWorn(itemName);
    }

    // The gender whose body a worn model fits: a registered gender, or none for every gender
    public boolean isImportWornGender(String genderName) {
        return genderName.isEmpty() || isImportGender(genderName);
    }

    // The side of the body a worn model is worn on: left, right, or none for either
    public boolean isImportWornSide(String sideName) {
        return sideName.isEmpty() || characterWorkspaceBranch.isImportWornSide(sideName);
    }

    // The model open, saved first, brought into the game as the model the item is worn as on its rig, fitted to the
    // gender named or every gender and to the side named or either
    public void importWornModel(String itemName, String genderName, String sideName) {

        if (!isImportWorn(itemName) || !isImportWornGender(genderName) || !isImportWornSide(sideName))
            return;

        saveActiveTemplate();

        String refusal = characterWorkspaceBranch.importWorn(activeDocument, itemName, genderName, sideName);

        setStatusMessage(refusal != null ? refusal : EditorSetting.CHARACTER_MODEL_MESSAGE_IMPORTED_WORN + itemName);
    }

    // True while the model open was baked from an item, which Import paints rather than makes a race of
    public boolean isItemModel() {
        return activeDocument != null && activeDocument.isModel() && !activeDocument.isBlank()
                && activeDocument.getModelName().startsWith(EditorSetting.ITEM_BAKE_MODEL_FOLDER);
    }

    // The item an item model's import offers first: the one whose sub-voxels go by the model's own name
    public String getImportItemName() {
        return characterWorkspaceBranch.findItemByMesh(EditorSetting.ITEM_EDITOR_MESH_DIRECTORY
                + EditorSetting.INFO_FOLDER_SEPARATOR + getActiveModelStem());
    }

    public boolean isImportItem(String itemName) {
        return isItemModel() && characterWorkspaceBranch.isImportItem(itemName);
    }

    // The item model open, saved first, brought into the game as the mesh the item is drawn with, painted as it is
    public void importItemModel(String itemName) {

        if (!isImportItem(itemName))
            return;

        saveActiveTemplate();

        String refusal = characterWorkspaceBranch.importItem(activeDocument, itemName);

        setStatusMessage(refusal != null ? refusal : EditorSetting.CHARACTER_MODEL_MESSAGE_IMPORTED_ITEM + itemName);
    }

    // The model open moved onto a rig of its own, a copy of the one it stands on, saved first, then opened again on it
    public void newModelRig() {

        if (activeDocument == null || !activeDocument.isModel() || activeDocument.isBlank()) {
            setStatusMessage(EditorSetting.CHARACTER_RIG_MESSAGE_NEW_MODEL_ONLY);
            return;
        }

        saveActiveTemplate();

        String modelName = activeDocument.getModelName();
        String refusal = characterWorkspaceBranch.moveToNewRig(activeDocument);

        if (refusal != null) {
            setStatusMessage(refusal);
            return;
        }

        modelName2CharacterDocument.remove(modelName);
        activate(null);
        openModel(modelName);

        if (activeDocument != null)
            setStatusMessage(EditorSetting.CHARACTER_RIG_MESSAGE_NEW + activeDocument.getRigHandle().getRigName());
    }

    // The name a model's import offers for its race and entity first: the model's own
    public String getActiveModelStem() {
        return activeDocument != null && activeDocument.isModel()
                ? FileUtility.getFileName(activeDocument.getModelName())
                : "";
    }

    private void activate(CharacterDocumentInstance document) {

        characterSculptBranch.cancelTransform();

        this.activeDocument = document;
        this.selectedBoneIndex = 0;
        clearEditPose();
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

        if (!characterControlBranch.save() || !characterClipBranch.save()) {
            setStatusMessage(infoManager.getRefusal());
            return;
        }

        if (!characterPaintBranch.save()) {
            setStatusMessage(EditorSetting.CHARACTER_PAINT_MESSAGE_NOT_SAVED);
            return;
        }

        if (activeDocument.isModel()) {
            setStatusMessage(EditorSetting.CHARACTER_EDITOR_MESSAGE_SAVED + activeDocument.getModelName());
            return;
        }

        setStatusMessage(infoManager.saveDocument(EditorSetting.INFO_SCHEMA_RACES, activeDocument.getRaceName())
                ? EditorSetting.CHARACTER_EDITOR_MESSAGE_SAVED + activeDocument.getRaceName()
                : infoManager.getRefusal());
    }

    public void revertActiveTemplate() {

        if (activeDocument == null)
            return;

        if (!activeDocument.isModel())
            infoManager.revertDocument(EditorSetting.INFO_SCHEMA_RACES, activeDocument.getRaceName());

        revertDirtyMeshes();
        revertRigFile();
        characterControlBranch.revert();
        characterClipBranch.revert();
        characterPaintBranch.revert();
        setStatusMessage(EditorSetting.CHARACTER_EDITOR_MESSAGE_REVERTED + getActiveDocumentName());
    }

    public void copyActiveTemplate(String localName) {

        if (activeDocument == null || !isTemplateNameAvailable(localName))
            return;

        if (activeDocument.isModel()) {
            setStatusMessage(EditorSetting.CHARACTER_MODEL_MESSAGE_NOT_RACE);
            return;
        }

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

        if (activeDocument.isModel()) {
            setStatusMessage(EditorSetting.CHARACTER_MODEL_MESSAGE_NOT_RACE);
            return;
        }

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

        if (isColorKey(key))
            characterPaintPropertyBranch.step(key, direction);
        else if (activeDocument != null)
            characterPropertyBranch.step(activeDocument, key, direction);
    }

    public void cycle(String key, int direction) {

        if (isBoneDisplayKey(key))
            cycleBoneDisplay(key, direction);
        else if (activeDocument != null)
            characterPropertyBranch.cycle(activeDocument, key, direction);
    }

    public void act(String key) {

        if (activeDocument != null)
            characterPropertyBranch.act(activeDocument, key);
    }

    public String getValueText(String key) {

        if (isColorKey(key))
            return characterPaintPropertyBranch.getValueText(key);

        if (isBoneDisplayKey(key))
            return characterBoneDisplayBranch.getValueText(key);

        return activeDocument != null ? characterPropertyBranch.getValueText(activeDocument, key) : "";
    }

    public boolean isValueValid(String key, String text) {

        if (isColorKey(key))
            return characterPaintPropertyBranch.isValueValid(key, text);

        return activeDocument != null && characterPropertyBranch.isValueValid(activeDocument, key, text);
    }

    public void setValue(String key, String text) {

        if (isColorKey(key))
            characterPaintPropertyBranch.setValue(key, text);
        else if (isValueValid(key, text))
            characterPropertyBranch.setValue(activeDocument, key, text);
    }

    // The paint colour is every painting window's, the Image Editor's too, so its keys work with no character open
    private boolean isColorKey(String key) {

        String keyName = CharacterViewUtility.getKeyName(key);

        return keyName.equals(EditorSetting.CHARACTER_PAINT_KEY_COLOR)
                || keyName.equals(EditorSetting.CHARACTER_PAINT_KEY_HSB);
    }

    public void buildSettingRows(ObjectArrayList<CharacterViewRowStruct> rows) {

        if (activeDocument != null)
            characterRowBranch.buildSettingRows(activeDocument, rows);
    }

    public void buildPickRows(ObjectArrayList<CharacterViewRowStruct> rows) {

        if (activeDocument != null)
            characterRowBranch.buildPickRows(activeDocument, rows);
    }

    public void buildPropertyRows(ObjectArrayList<CharacterViewRowStruct> rows) {

        if (activeDocument != null)
            characterRowBranch.buildPropertyRows(activeDocument, rows);
    }

    // The paint colour's rows, as the Color Wheel lists them; the colour is every painting window's, so it lists
    // whether or not a character is open
    public void buildColorRows(ObjectArrayList<CharacterViewRowStruct> rows) {
        characterPaintRowBranch.buildColorRows(rows);
    }

    // The maps of the texture painted and the settings of the one painted, as the Maps tab lists them
    public void buildMapRows(ObjectArrayList<CharacterViewRowStruct> rows) {

        if (activeDocument != null)
            characterPaintRowBranch.buildMapRows(rows);
    }

    // The paint colour's hue, saturation or brightness, 0 to 1
    public float getPaintHSB(int index) {
        return characterPaintPropertyBranch.getHSB(index);
    }

    // The paint colour from a hue, saturation and brightness, as the Color Wheel picks it
    public void setPaintHSB(float hue, float saturation, float brightness) {

        if (!characterPaintBranch.isStroking())
            characterPaintPropertyBranch.setHSB(hue, saturation, brightness);
    }

    // The paint colour's red, green or blue, 0 to 1
    public float getPaintColor(int channel) {
        return characterPaintBranch.getColor(channel);
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

    // The region picked in the Layers tab for every tool tab that takes regions
    public void pickRegion(CharacterRegionStruct picked) {

        if (picked == getPickedRegion())
            return;

        this.pickedRegion = picked;
        this.pickedRegionKey = picked.getKey();
        regionPickRevision++;
        notifyChanged();
    }

    // The region picked among the rig's regions as they stand, found again by its key once they are made again
    public CharacterRegionStruct getPickedRegion() {

        int regionsRevision = getRegionsRevision();

        if (regionsRevision != pickedRegionsRevision) {
            this.pickedRegionsRevision = regionsRevision;
            this.pickedRegion = findRegion(pickedRegionKey);
            regionPickRevision++;
        }

        return pickedRegion;
    }

    // Moves whenever the region picked or the regions listed change, or a region is hidden or shown
    public int getRegionPickRevision() {
        getPickedRegion();
        return regionPickRevision;
    }

    // A region's eye in the Layers tab: open while any of its vertices on the part edited shows
    public boolean isRegionShown(CharacterRegionStruct region) {

        CharacterMeshDocumentInstance document = getActiveMeshDocument();

        return document == null || !characterSelectionBranch.isRegionHidden(document, region);
    }

    // A region's eye turned: every vertex of it on the part edited hidden, or all shown again
    public void toggleRegionShown(String regionKey) {

        CharacterMeshDocumentInstance document = getActiveMeshDocument();
        CharacterRegionStruct region = findRegion(regionKey);

        if (document == null || region == null || characterSculptBranch.isTransforming())
            return;

        if (!characterSelectionBranch.setRegionHidden(
                document, region, !characterSelectionBranch.isRegionHidden(document, region)))
            return;

        characterMeshBranch.upload(document);
        notifyRegionsShown();
    }

    // What shows of the regions changed, so the Layers tab lays their eyes out again
    void notifyRegionsShown() {
        regionPickRevision++;
        notifyChanged();
    }

    // The region of the tab worked in last, its part picked whenever the character wears it
    public void setRegion(CharacterRegionStruct region) {

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
    private String resolveRegionPartKey(CharacterRegionStruct region) {

        if (region.isWhole() || holdsPartKey(region, activePartKey))
            return activePartKey;

        return region.holdsBody()
                ? EditorSetting.CHARACTER_EDITOR_PART_BODY
                : ArpgUtility.toEnumName(region.getFeatureSlot());
    }

    // Whether a part, by its key, is one a region holds: the body where it holds a part of the rig, or its feature
    boolean holdsPartKey(CharacterRegionStruct region, String partKey) {

        if (region.isWhole())
            return true;

        if (partKey.equals(EditorSetting.CHARACTER_EDITOR_PART_BODY))
            return region.holdsBody();

        return region.holdsFeature() && partKey.equals(ArpgUtility.toEnumName(region.getFeatureSlot()));
    }

    // Whether a mesh is the part a region edits, the active part when that is one the region holds
    boolean isRegionPart(CharacterMeshDocumentInstance document, CharacterRegionStruct region) {

        if (region.isWhole())
            return true;

        CharacterMeshDocumentInstance active = getActiveMeshDocument();
        return document == active && holdsPartKey(region, activePartKey);
    }

    // Whether the open part is the region's feature rather than its body
    boolean isRegionFeatureOpen(CharacterRegionStruct region) {
        return region.holdsFeature() && activePartKey.equals(ArpgUtility.toEnumName(region.getFeatureSlot()));
    }

    // How far a vertex of a mesh belongs to a region, the share each stroke reaching it is held to
    public float getRegionShare(CharacterMeshDocumentInstance document, CharacterRegionStruct region, int vertexIndex) {
        return Math.max(characterRegionBranch.getShare(document, region, vertexIndex), 0f);
    }

    // Whether a vertex of a mesh is picked and selected in a region
    public boolean isInRegion(CharacterMeshDocumentInstance document, CharacterRegionStruct region, int vertexIndex) {
        return characterRegionBranch.holds(document, region, vertexIndex);
    }

    // Whether a vertex of a mesh is held by the region of the tab worked in last
    public boolean isInRegion(CharacterMeshDocumentInstance document, int vertexIndex) {
        return characterRegionBranch.holds(document, region, vertexIndex);
    }

    // Whether the region's part is the one open, so a tab of a part the character does not wear edits nothing
    public boolean isRegionOpen(CharacterRegionStruct region) {

        CharacterMeshDocumentInstance document = getActiveMeshDocument();
        return document != null && isRegionPart(document, region);
    }

    public boolean isBoneInRegion(CharacterRegionStruct region, RigHandle rigHandle, int boneIndex) {
        return characterRegionBranch.holdsBone(region, rigHandle, boneIndex);
    }

    public boolean isRigBoneInRegion(CharacterRegionStruct region, int boneIndex) {
        return syncRig() && characterRegionBranch.holdsRigBone(region, boneIndex);
    }

    public CharacterRegionStruct getRegion() {
        return region;
    }

    // The regions of the active race's rig, the Body first, each a layer of the tool tabs that take regions
    public ObjectArrayList<CharacterRegionStruct> getRegions() {
        return characterRegionBranch.resolveRegions(resolveRegionRig());
    }


    // A region of the active race's rig by its layer key, the Body for one the rig no longer names
    public CharacterRegionStruct findRegion(String key) {
        return characterRegionBranch.findRegion(resolveRegionRig(), key);
    }

    // Moves whenever the regions are made again for another rig or a rig changed in place
    public int getRegionsRevision() {
        characterRegionBranch.resolveRegions(resolveRegionRig());
        return characterRegionBranch.getRegionsRevision();
    }

    // Sides \\

    // Whether a region's part has a twin across the body, so it is shown and framed one side at a time
    public boolean isSided(CharacterRegionStruct region) {
        return region.isSided();
    }

    // The side a region shows, kept by its key, the left until another is picked
    public CharacterEditorSide getSide(CharacterRegionStruct region) {
        return regionKey2Side.getOrDefault(region.getKey(), CharacterEditorSide.LEFT);
    }

    void setSide(CharacterRegionStruct region, CharacterEditorSide side) {

        if (getSide(region) == side)
            return;

        regionKey2Side.put(region.getKey(), side);
        notifyChanged();
    }

    // Whether a vertex stands on the side a sided region shows; every vertex does for any other region
    public boolean isOnRegionSide(CharacterMeshDocumentInstance document, int vertexIndex) {
        return !isSided(region) || getSide(region).holdsX(document.getMesh().getX(vertexIndex));
    }

    // The bone a region is framed about: the first bone of its part on the side shown, the bone its own bones hang
    // from for one that names them, INDEX_NOT_FOUND for the whole character or a rig that names no such part
    public int getRegionFrameBone(CharacterRegionStruct region, RigHandle rigHandle) {
        return characterRegionBranch.resolveFrameBone(region, getSide(region), rigHandle);
    }

    // Whether a bone of a loaded rig is shown in a region: one it holds, on the side shown where it is sided
    public boolean isBoneShown(CharacterRegionStruct region, RigHandle rigHandle, int boneIndex) {
        return characterRegionBranch.holdsBone(region, rigHandle, boneIndex)
                && (!region.isSided()
                || getSide(region).namesBone(rigHandle.getBone(boneIndex).getName()))
                && !isBoneHidden(rigHandle, boneIndex);
    }

    // Whether a bone's eye or the eye of any bone above it is shut
    boolean isBoneHidden(RigHandle rigHandle, int boneIndex) {

        for (int bone = boneIndex; bone != EngineSetting.INDEX_NOT_FOUND;
                bone = rigHandle.getBone(bone).getParentIndex())
            if (hiddenBoneNames.contains(rigHandle.getBone(bone).getName()))
                return true;

        return false;
    }

    private boolean isRigBoneHidden(int boneIndex) {

        for (int bone = boneIndex; bone != EngineSetting.INDEX_NOT_FOUND; bone = characterRigBranch.getParent(bone))
            if (hiddenBoneNames.contains(characterRigBranch.getBoneName(bone)))
                return true;

        return false;
    }

    // The bone an eye item names: a bone of the rig being edited in the Rig tab, of the posed rig elsewhere; null for
    // a row naming no bone
    private String resolveEyeBoneName(String key, RigHandle rigHandle) {

        String keyName = CharacterViewUtility.getKeyName(key);
        int boneIndex = CharacterViewUtility.getKeyIndex(key);

        if (keyName.equals(EditorSetting.CHARACTER_RIG_KEY_BONE))
            return characterRigBranch.getBoneName(boneIndex);

        if (keyName.equals(EditorSetting.CHARACTER_EDITOR_KEY_BONE) && rigHandle != null)
            return rigHandle.getBone(boneIndex).getName();

        return null;
    }

    // A bone's own eye in the Properties tab: open unless it was shut, whatever the bones above it show
    public boolean isBoneEyeOpen(String boneName) {
        return !hiddenBoneNames.contains(boneName);
    }

    // An eye item's eye turned in the Properties tab — a bone's: it and every bone under it hidden in the viewports
    // with the vertices they carry most, or shown again, the bones below keeping their own eyes as a hidden group's
    // layers do
    public void toggleRowEye(String key) {

        RigHandle rigHandle = resolveRegionRig();
        String boneName = resolveEyeBoneName(key, rigHandle);

        if (boneName == null || characterSculptBranch.isTransforming())
            return;

        if (!hiddenBoneNames.remove(boneName))
            hiddenBoneNames.add(boneName);

        CharacterMeshDocumentInstance document = getActiveMeshDocument();

        if (document != null && rigHandle != null && rigHandle.hasBone(boneName)
                && characterSelectionBranch.setCarriedHidden(document, rigHandle, rigHandle.getBoneIndex(boneName)))
            characterMeshBranch.upload(document);

        notifyRegionsShown();
    }

    // Whether a bone of the rig being edited is shown in a region: one it holds, on the side shown where it is sided
    public boolean isRigBoneShown(CharacterRegionStruct region, int boneIndex) {

        if (!isRigBoneInRegion(region, boneIndex) || isRigBoneHidden(boneIndex))
            return false;

        return !isSided(region) || getSide(region).namesBone(characterRigBranch.getBoneName(boneIndex));
    }

    // The name of the rig the active race is built on, null before one is open
    String getActiveRigName() {

        RigHandle rigHandle = resolveRegionRig();
        return rigHandle != null ? rigHandle.getRigName() : null;
    }

    private RigHandle resolveRegionRig() {
        return activeDocument != null ? activeDocument.getRigHandle() : null;
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

    // The open mesh of one part the character wears, null without a character or when the part has no rigged mesh
    // file of its own
    CharacterMeshDocumentInstance getPartMeshDocument(String partKey) {
        return activeDocument != null
                ? characterMeshBranch.open(characterMeshBranch.resolvePartMesh(activeDocument, partKey))
                : null;
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

    // The edge loop through an edge, combined with the selection as asked
    public void selectLoop(int vertexA, int vertexB, CharacterEditorSelection selection) {

        CharacterMeshDocumentInstance document = getActiveMeshDocument();

        if (document != null && !characterSculptBranch.isTransforming())
            characterSelectionBranch.selectLoop(document, vertexA, vertexB, selection);
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

    // A transform of the selection, on the cage the view poses it on while posed (skinningMatrices, the pose struck
    // this frame), in the bind pose otherwise
    public boolean beginTransform(Matrix4[] skinningMatrices) {

        CharacterMeshDocumentInstance document = getActiveMeshDocument();

        if (document == null || mode != CharacterEditorMode.MESH
                || !characterSculptBranch.beginTransform(document, isPosed() ? skinningMatrices : null))
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

    // Sculpt strokes work only in the bind pose, where vertices stand as the mesh file holds them; struck in any other
    // pose, a stroke is refused and the status says why (transforms work posed, through the skinning)
    private boolean refusePosedEdit() {

        if (!isPosed())
            return false;

        setStatusMessage(EditorSetting.CHARACTER_EDITOR_MESSAGE_POSED);
        return true;
    }

    public boolean isTransforming() {
        return characterSculptBranch.isTransforming();
    }

    // Where the transform gizmo stands: the Mesh tab's selection's weighted centre as the view shows it, posed or in
    // the bind pose; false with nothing selected or no mesh open
    public boolean resolveGizmoPivot(Matrix4[] skinningMatrices, Vector3 pivot) {

        CharacterMeshDocumentInstance document = getActiveMeshDocument();

        return document != null && mode == CharacterEditorMode.MESH
                && characterSculptBranch.resolveSelectionCenter(document, isPosed() ? skinningMatrices : null, pivot);
    }

    public Vector3 getTransformPivot() {
        return characterSculptBranch.getTransformPivot();
    }

    // Strokes \\

    // A brush stroke on the active part: its selection with the Select tool, combined with it as asked, its faces with
    // the Topology tool, its shape in Sculpt mode and its weights otherwise
    public void beginStroke(CharacterEditorSelection selection) {

        CharacterMeshDocumentInstance document = getActiveMeshDocument();

        if (document == null)
            return;

        if (isSelectStroke())
            characterSelectionBranch.beginStroke(document, selection);
        else if (toolManager.getTool(mode.getToolCanvas()) == EditorTool.TOPOLOGY) {
            if (!refusePosedEdit())
                characterTopologyBranch.beginStroke(document);
        }
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

        if (characterTopologyBranch.isStroking()) {
            characterTopologyBranch.stroke(document, vertices, strengths, stamp);
            return;
        }

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

    // Every stamp a frame's stroke laid shown in one rebuild of the part, once the frame's stamps are done; a
    // topology stroke changes the faces it covered first
    public void showStroke() {

        CharacterMeshDocumentInstance document = getActiveMeshDocument();

        if (characterTopologyBranch.isStroking() && characterTopologyBranch.apply())
            this.strokeMoved = true;

        if (!strokeMoved || document == null)
            return;

        this.strokeMoved = false;
        characterMeshBranch.preview(document);
    }

    // Edges \\

    // The Topology tab's edge tool in use on the edge from a face's corner to the next: Flip Edge turns it, backward
    // when inverted, Loop Cut cuts the ring across it, Dissolve Loop takes its loop away
    public void applyEdgeTool(int faceIndex, int corner, boolean inverted) {

        CharacterMeshDocumentInstance document = getActiveMeshDocument();

        if (document == null || !isEdgeTool())
            return;

        if (document.isEditingShape()) {
            setStatusMessage(EditorSetting.CHARACTER_EDITOR_MESSAGE_TOPOLOGY_SHAPE);
            return;
        }

        switch (toolManager.getTool(mode.getToolCanvas())) {
            case FLIP_EDGE -> characterEdgeBranch.flip(document, faceIndex, corner, inverted);
            case LOOP_CUT -> characterEdgeBranch.cutLoop(document, faceIndex, corner);
            default -> characterEdgeBranch.dissolveLoop(document, faceIndex, corner);
        }
    }

    // The Properties tab's count of the active part's quads, triangles and poles
    String getTopologyStatsText(CharacterMeshDocumentInstance document) {
        return characterEdgeBranch.getStatsText(document);
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

        if (characterTopologyBranch.isStroking())
            characterTopologyBranch.endStroke(document);
        else if (mode == CharacterEditorMode.SCULPT)
            characterShapeBranch.endStroke(document);
        else
            characterWeightBranch.endStroke(document);
    }

    // Paint \\

    // A paint stroke on the active part's texture maps, false where nothing is open to paint
    public boolean beginPaint() {

        if (!mode.paintsMaps() || getActiveMeshDocument() == null)
            return false;

        characterPaintBranch.beginStroke();
        return true;
    }

    // One stamp of the paint brush, every face under it painted by the tool in use at its strength
    public void paint(CharacterPaintStampStruct stamp) {

        CharacterMeshDocumentInstance document = getActiveMeshDocument();

        if (document != null)
            characterPaintBranch.stamp(document, stamp, getPaintTool(), getBrushStrength());
    }

    public void endPaint() {
        characterPaintBranch.endStroke();
    }

    // The island of the face clicked filled, or the colour under the pointer taken, by the tool in use
    public void clickPaint(int faceIndex, float u, float v) {

        CharacterMeshDocumentInstance document = getActiveMeshDocument();

        if (!mode.paintsMaps() || document == null || faceIndex == EngineSetting.INDEX_NOT_FOUND)
            return;

        if (getPaintTool() == EditorTool.PICK)
            characterPaintBranch.pick(document, faceIndex, u, v);
        else
            characterPaintBranch.fill(document, faceIndex, getBrushStrength());
    }

    public EditorTool getPaintTool() {
        return toolManager.getTool(ToolCanvas.CHARACTER_PAINT);
    }

    // The canvas of the active part's texture under the map painted, its layers the Paint tab's; null with none open
    public CharacterPaintCanvasStruct getPaintCanvas() {
        return characterPaintPropertyBranch.getCanvas();
    }

    public void selectPaintLayer(int layerIndex) {
        characterPaintPropertyBranch.selectLayer(layerIndex);
    }

    public void togglePaintLayerVisible(int layerIndex) {
        characterPaintPropertyBranch.toggleLayerVisible(layerIndex);
    }

    // UV \\

    // The faces the UV Editor shows, every face of the part open whose four vertices are selected
    public void collectUVFaces(IntArrayList faces) {

        CharacterMeshDocumentInstance document = getActiveMeshDocument();

        if (document != null)
            characterUVBranch.collectShownFaces(document, faces);
        else
            faces.clear();
    }

    // A corner picked in the UV Editor, or none to clear, combined with its selection by mode
    public void pickUV(int faceIndex, int corner, CharacterEditorSelection mode) {

        CharacterMeshDocumentInstance document = getActiveMeshDocument();

        if (document != null && !characterUVBranch.isTransforming())
            characterUVBranch.pick(document, faceIndex, corner, mode);
    }

    public void boxSelectUV(float minU, float minV, float maxU, float maxV, CharacterEditorSelection mode) {

        CharacterMeshDocumentInstance document = getActiveMeshDocument();

        if (document != null && !characterUVBranch.isTransforming())
            characterUVBranch.box(document, minU, minV, maxU, maxV, mode);
    }

    // The UV Editor's Move, Rotate or Scale taking the selected corners from a point in UV; false with none selected
    public boolean beginUVTransform(float u, float v) {

        CharacterMeshDocumentInstance document = getActiveMeshDocument();

        return document != null && !characterUVBranch.isTransforming()
                && characterUVBranch.beginTransform(document, getUVTool(), u, v);
    }

    public void updateUVTransform(float u, float v) {
        characterUVBranch.updateTransform(u, v);
    }

    public void endUVTransform() {
        characterUVBranch.endTransform();
    }

    public void cancelUVTransform() {
        characterUVBranch.cancelTransform();
    }

    public boolean isUVTransforming() {
        return characterUVBranch.isTransforming();
    }

    public EditorTool getUVTool() {
        return toolManager.getTool(ToolCanvas.UV_EDITOR);
    }

    // History \\

    // A step back in the history of what the mode edits: the maps in Paint mode, the rig in Rig mode, the active
    // part's mesh otherwise
    public void undo() {

        if (mode.paintsMaps()) {
            reportHistory(!characterPaintBranch.isStroking() && characterPaintBranch.undo(), true);
            return;
        }

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

        if (mode.paintsMaps()) {
            reportHistory(!characterPaintBranch.isStroking() && characterPaintBranch.redo(), false);
            return;
        }

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
    boolean syncRig() {

        characterRigBranch.sync(activeDocument != null ? activeDocument.getRigHandle() : null);

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

    public void rotateRigBone(Matrix4 worldTurn) {

        if (characterRigBranch.isGesturing())
            characterRigBranch.rotateGesture(worldTurn, isMirror());
    }

    // The selected bone's size scaled by a factor along each of its axes
    public void scaleRigBone(float factorX, float factorY, float factorZ) {

        if (characterRigBranch.isGesturing())
            characterRigBranch.scaleGesture(factorX, factorY, factorZ);
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

        if (syncRig() && !characterRigBranch.isGesturing() && !characterRigBranch.refuseShared())
            characterRigBranch.addBone(worldPoint);
    }

    public void removeRigBone(int boneIndex) {

        if (syncRig() && !characterRigBranch.isGesturing() && !characterRigBranch.refuseShared())
            characterRigBranch.removeBone(boneIndex);
    }

    // Bones a model brought with it, added to the rig in one edit
    void addRigBones(ObjectArrayList<String> names, ObjectArrayList<String> parents, float[] worldPoints) {

        if (syncRig() && !characterRigBranch.isGesturing())
            characterRigBranch.addBones(names, parents, worldPoints);
    }

    // Stamps \\

    // The part chosen in the Stamps tab, the one the Brush places in place of a stroke, null when none is
    public StampHandle getActiveStamp() {
        return stampManager.getActiveStamp();
    }

    // True while the Brush places the part chosen in the Stamps tab rather than stroking: a part is chosen and the
    // Brush is the tool of the mode that places parts
    public boolean armsStamp() {
        return mode.placesParts() && toolManager.getTool(mode.getToolCanvas()) == EditorTool.BRUSH
                && getActiveStamp() != null;
    }

    // True while the part armed can be placed: parts join the body alone in the bind pose, so the body must be the
    // part open and the view unposed
    public boolean placesStamp() {
        return armsStamp() && activePartKey.equals(EditorSetting.CHARACTER_EDITOR_PART_BODY) && !isPosed();
    }

    // The part let go once it is placed, or when Escape puts it down, so the Brush strokes again
    public void disarmStamp() {
        stampManager.clearActiveStamp();
    }

    // The attach points shown on the body while a part is armed: every joint of the rig, then every open loop's
    // centre
    public int getAttachPointCount() {

        CharacterMeshDocumentInstance document = getActiveMeshDocument();

        if (!placesStamp() || document == null || !syncRig())
            return 0;

        characterPlacementBranch.sync(document);

        return characterPlacementBranch.getAttachPointCount();
    }

    public void getAttachPoint(int attachPoint, Vector3 out) {
        characterPlacementBranch.getAttachPoint(attachPoint, out);
    }

    // Whether an attach point is an open loop's centre, which a part snaps to and joins, rather than a joint
    public boolean isLoopAttachPoint(int attachPoint) {
        return characterPlacementBranch.isLoopPoint(attachPoint);
    }

    // Where the part chosen would join: at an open loop by its attach point, or where a ray met a face of the body,
    // tilted as the pointer turned it once set down; nothing is shown without a part or a body
    public void resolvePlacement(
            int attachPoint,
            int faceIndex,
            Vector3 point,
            Vector3 normal,
            Matrix4 tilt,
            CharacterPartPlacementStruct out) {

        CharacterMeshDocumentInstance document = getActiveMeshDocument();
        StampHandle stampHandle = getActiveStamp();

        if (!placesStamp() || document == null || stampHandle == null || !syncRig()) {
            out.clear();
            return;
        }

        characterPlacementBranch.resolve(document, stampHandle, attachPoint, faceIndex, point, normal, tilt, out);
    }

    // Whether a placement joins its twin across the body too: while mirroring, for a part placed off the centre
    public boolean placesTwin(CharacterPartPlacementStruct placement) {
        return isMirror() && placement.isShown() && !placement.isRoot() && !placement.getSide().isEmpty();
    }

    public void resolvePlacementTwin(CharacterPartPlacementStruct placement, CharacterPartPlacementStruct out) {

        CharacterMeshDocumentInstance document = getActiveMeshDocument();

        if (document != null)
            characterPlacementBranch.resolveTwin(document, placement, out);
    }

    // Joins the part where it is placed, or lays the fur, and its twin with it when one is given, as one edit
    public void placeStamp(CharacterPartPlacementStruct placement, CharacterPartPlacementStruct twin) {

        CharacterMeshDocumentInstance document = getActiveMeshDocument();

        if (document == null || !placement.isValid() || twin != null && !twin.isValid() || !syncRig()
                || characterRigBranch.isGesturing())
            return;

        characterSculptBranch.cancelTransform();
        characterMeshBranch.endShapeEdits();

        if (placement.getStampHandle().hasFur())
            characterFurBranch.lay(document, placement, twin);
        else
            characterGraftBranch.graft(document, placement, twin);

        disarmStamp();
    }

    // What the Brush says about the part it aims, said only when it changes and taken back once there is nothing
    // to say, unless something else has been said since
    public void showPlacementMessage(String message) {

        if (Objects.equals(message, placementMessage))
            return;

        boolean showing = placementMessage != null && placementMessage.equals(statusMessage);

        this.placementMessage = message;

        if (message != null)
            setStatusMessage(message);
        else if (showing)
            setStatusMessage(null);
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

    // Grooming \\

    // The groom open in the Hair tab, null while none is, and the revision that moves with every change to it
    public CharacterGroomStruct getGroom() {
        return characterGroomBranch.getGroom();
    }

    public int getGroomRevision() {
        return characterGroomBranch.getRevision();
    }

    public boolean hasGroom() {
        return characterGroomBranch.hasGroom();
    }

    // The Hair tab's tool in use
    public EditorTool getHairTool() {
        return toolManager.getTool(ToolCanvas.CHARACTER_HAIR);
    }

    // A strand drawn from where the active part's face was pressed, as wide as the Width option, rooted on the bone
    // weighing most on the face
    public void beginHairDraw(Vector3 point, Vector3 normal, int faceIndex) {

        if (!refuseNoGroom())
            characterGroomBranch.beginDraw(point, normal, resolveRootBone(faceIndex), getHairWidth());
    }

    public void extendHairDraw(Vector3 point) {
        characterGroomBranch.extendDraw(point);
    }

    public void endHairDraw() {
        characterGroomBranch.endDraw(isMirror());
    }

    public boolean isDrawingHair() {
        return characterGroomBranch.isDrawing();
    }

    // A straight strand as long as the Length option standing out of the active part's face where it was clicked
    public void placeHairStrand(Vector3 point, Vector3 normal, int faceIndex) {

        if (!refuseNoGroom())
            characterGroomBranch.place(
                    point,
                    normal,
                    resolveRootBone(faceIndex),
                    toolManager.getOption(ToolCanvas.CHARACTER_HAIR, ToolOption.LENGTH)
                            * EditorSetting.CHARACTER_HAIR_LENGTH_UNIT,
                    getHairWidth(),
                    isMirror());
    }

    public void pickHairMaterial(int strandIndex) {
        characterGroomBranch.pickMaterial(strandIndex);
    }

    // A brush stroke over the strands with the Hair tab's tool, the selection combined as asked by a selecting one
    public void beginHairStroke(CharacterEditorSelection selection) {

        if (!refuseNoGroom())
            characterGroomBranch.beginStroke(isMirror(), getHairTool() == EditorTool.SELECT ? selection : null);
    }

    public void strokeHair(IntArrayList strands, IntArrayList points, FloatArrayList weights, Vector3 drag) {
        characterGroomBranch.stroke(getHairTool(), strands, points, weights, drag);
    }

    public void endHairStroke() {
        characterGroomBranch.endStroke();
    }

    // Every card of the hair the character wears made a strand of the groom, a groom made from it when none is open
    void takeHairFromWorn() {

        String partKey = ArpgUtility.toEnumName(FeatureSlot.HAIR);
        CharacterMeshDocumentInstance document = activeDocument != null
                ? characterMeshBranch.open(characterMeshBranch.resolvePartMesh(activeDocument, partKey))
                : null;

        if (document == null) {
            setStatusMessage(EditorSetting.CHARACTER_HAIR_MESSAGE_NO_WORN);
            return;
        }

        FeatureHandle feature = activeDocument.getPreviewData().getAppearanceData()
                .getStartingFeature(getPreviewGenderID(), FeatureSlot.HAIR);

        characterGroomBranch.takeFromWorn(document.getMesh(), feature.getFeatureName());
    }

    private float getHairWidth() {
        return toolManager.getOption(ToolCanvas.CHARACTER_HAIR, ToolOption.WIDTH)
                * EditorSetting.CHARACTER_HAIR_WIDTH_UNIT;
    }

    private boolean refuseNoGroom() {

        if (characterGroomBranch.hasGroom())
            return false;

        setStatusMessage(EditorSetting.CHARACTER_HAIR_MESSAGE_NO_GROOM);
        return true;
    }

    // The bone weighing most on the first corner of the active part's face, the bone a strand rooted there grows from
    private String resolveRootBone(int faceIndex) {

        CharacterMeshDocumentInstance document = getActiveMeshDocument();

        if (document == null || faceIndex == EngineSetting.INDEX_NOT_FOUND)
            return EditorSetting.CHARACTER_HAIR_DEFAULT_BONE;

        RiggedMeshStruct mesh = document.getMesh();
        int vertexIndex = mesh.getFaceVertex(faceIndex, 0);
        int heaviest = EngineSetting.INDEX_NOT_FOUND;
        float most = 0f;

        for (int i = 0; i < mesh.getInfluenceCount(vertexIndex); i++)
            if (mesh.getInfluenceWeight(vertexIndex, i) > most) {
                most = mesh.getInfluenceWeight(vertexIndex, i);
                heaviest = mesh.getInfluenceBone(vertexIndex, i);
            }

        return heaviest != EngineSetting.INDEX_NOT_FOUND
                ? mesh.getRigHandle().getBone(heaviest).getName()
                : EditorSetting.CHARACTER_HAIR_DEFAULT_BONE;
    }

    // Hair Files \\

    public File getHairFolder() {
        return characterGroomBranch.getFolder();
    }

    public File getHairExportFolder() {
        return characterGroomBranch.getExportFolder();
    }

    public boolean isHairNameAvailable(String name) {
        return characterGroomBranch.isNameAvailable(name);
    }

    public void newHair(String name, String rigName) {

        if (characterGroomBranch.create(name, rigName))
            setStatusMessage(EditorSetting.CHARACTER_HAIR_MESSAGE_MADE + name);
        else
            setStatusMessage(EditorSetting.CHARACTER_HAIR_MESSAGE_NOT_WRITTEN + name);
    }

    public void openHairFile(File file) {
        setStatusMessage(characterGroomBranch.open(file)
                ? EditorSetting.CHARACTER_HAIR_MESSAGE_OPENED + characterGroomBranch.getGroomName()
                : characterGroomBranch.getRefusal());
    }

    public void saveHair() {

        if (refuseNoGroom())
            return;

        setStatusMessage(characterGroomBranch.save()
                ? EditorSetting.CHARACTER_HAIR_MESSAGE_SAVED + characterGroomBranch.getGroomName()
                : characterGroomBranch.getRefusal());
    }

    public void revertHair() {

        if (refuseNoGroom())
            return;

        setStatusMessage(characterGroomBranch.revert()
                ? EditorSetting.CHARACTER_HAIR_MESSAGE_REVERTED + characterGroomBranch.getGroomName()
                : characterGroomBranch.getRefusal());
    }

    public void undoHair() {
        setStatusMessage(characterGroomBranch.undo()
                ? EditorSetting.CHARACTER_EDITOR_MESSAGE_UNDONE
                : EditorSetting.CHARACTER_EDITOR_MESSAGE_NOTHING_TO_UNDO);
    }

    public void redoHair() {
        setStatusMessage(characterGroomBranch.redo()
                ? EditorSetting.CHARACTER_EDITOR_MESSAGE_REDONE
                : EditorSetting.CHARACTER_EDITOR_MESSAGE_NOTHING_TO_REDO);
    }

    public void exportHair() {

        if (!characterGroomBranch.export())
            setStatusMessage(characterGroomBranch.getRefusal());
    }

    public boolean isHairFeatureName(String featureName) {
        return characterGroomBranch.isFeatureName(featureName);
    }

    public String getHairFeatureDefault() {
        return characterGroomBranch.getDefaultFeatureName();
    }

    // An export brought into the game as the hair feature named, its mesh shown again where it was loaded already,
    // and with springs asked its long strands given spring chains on the open character's rig and weighed onto them
    public void importHair(File file, String featureName, boolean springs) {

        String meshName = characterGroomBranch.importExport(file, featureName);

        if (meshName == null) {
            setStatusMessage(characterGroomBranch.getRefusal());
            return;
        }

        boolean loaded = meshManager.hasMesh(meshName);
        MeshHandle meshHandle = meshManager.getMeshHandleFromMeshName(meshName);
        CharacterMeshDocumentInstance document = characterMeshBranch.open(meshHandle);

        if (loaded && document != null)
            characterMeshBranch.revert(document);

        if (springs && document != null) {

            if (!syncRig() || !characterRigBranch.getRigName().equals(meshHandle.getRigHandle().getRigName())) {
                setStatusMessage(EditorSetting.CHARACTER_HAIR_MESSAGE_SPRINGS_NO_RIG);
                return;
            }

            characterGroomBranch.laySprings(document);
            saveRigFile();
        }

        setStatusMessage(EditorSetting.CHARACTER_HAIR_MESSAGE_IMPORTED + featureName);
    }

    // Modelling layers' preview leaves the bind pose to play the pose chosen with its springs simulated, or holds it
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

    // The bone every weight action works on, the rig tab's selection following it, so one bone is picked everywhere
    void selectBone(int boneIndex) {

        this.selectedBoneIndex = boneIndex;
        characterRigBranch.followBone(getSelectedBoneName());
        notifyChanged();
    }

    // The bone the rig tab selected, picked for the weights too while the loaded rig holds it
    void followRigBone(String boneName) {

        RigHandle rigHandle = resolveRegionRig();

        if (rigHandle != null && rigHandle.hasBone(boneName))
            this.selectedBoneIndex = rigHandle.getBoneIndex(boneName);
    }

    void selectAnchor(int anchorIndex) {

        this.selectedAnchorIndex = anchorIndex;
        notifyChanged();
    }

    // Gear \\

    // Steps the item a slot's preview wears through none and every item made for the slot with a model for the
    // previewed rig, gender and side
    void stepGear(EquipmentSlot equipmentSlot, int direction) {

        RigHandle rigHandle = activeDocument != null && activeDocument.hasPreview()
                ? activeDocument.getPreviewData().getRigHandle()
                : null;

        if (rigHandle == null)
            return;

        ObjectArrayList<ItemDefinitionHandle> items = itemDefinitionManager.getItemHandles();
        ItemDefinitionHandle current = previewGear[equipmentSlot.ordinal()];
        int start = current != null ? items.indexOf(current) : items.size();
        short genderID = getPreviewGenderID();

        for (int step = 1; step <= items.size(); step++) {

            int index = Math.floorMod(start + step * direction, items.size() + 1);
            ItemDefinitionHandle item = index < items.size() ? items.get(index) : null;

            if (item == null || equipmentSlot.accepts(item)
                    && item.getWornModel(rigHandle, genderID, equipmentSlot.getEquipmentSide()) != null) {
                previewGear[equipmentSlot.ordinal()] = item;
                notifyChanged();
                return;
            }
        }
    }

    // The item a slot's preview wears, null for none
    public ItemDefinitionHandle getPreviewGear(EquipmentSlot equipmentSlot) {
        return previewGear[equipmentSlot.ordinal()];
    }

    // The model a preview on the rig and gender given wears in a slot for the item chosen there, null for none
    ItemWornModelStruct resolvePreviewWornModel(EquipmentSlot equipmentSlot, RigHandle rigHandle, short genderID) {

        ItemDefinitionHandle item = previewGear[equipmentSlot.ordinal()];

        return item != null ? item.getWornModel(rigHandle, genderID, equipmentSlot.getEquipmentSide()) : null;
    }

    // Preview \\

    // The pose every mode's rows pick, stepped through Bind, each pose the rig names, then the movements played
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

    String getStatusMessage() {
        return statusMessage;
    }

    // Commands \\

    // Runs a command the command console sends the character tabs
    public void executeCommand(CommandStruct command) {
        characterCommandBranch.executeCommand(command);
    }

    // Accessible \\

    public CharacterDocumentInstance getActiveDocument() {
        return activeDocument;
    }

    public boolean hasActiveDocument() {
        return activeDocument != null;
    }

    // The active race's live ARPG, as InfoManager holds it, or the race a model is previewed through
    ArpgObjectStruct getTemplateArpg() {

        if (activeDocument.isModel())
            return activeDocument.getTemplateArpg();

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
                && (pose != CharacterEditorPose.REST || getPoseClipIndex() != EngineSetting.INDEX_NOT_FOUND
                || editPosed);
    }

    // Edit Pose \\

    // The turn the edit pose lays on a bone, none for a bone it never turned
    public Vector3 getEditPoseTurn(int boneIndex) {
        return boneIndex < editPoseTurns.size() ? editPoseTurns.get(boneIndex) : restTurn;
    }

    public boolean hasEditPose() {
        return editPosed;
    }

    // Turns a bone in the edit pose, and its twin across the body the mirrored way while mirroring
    public void setEditPoseTurn(int boneIndex, Vector3 turn) {

        if (activeDocument == null || !activeDocument.hasPreview())
            return;

        RigHandle rigHandle = activeDocument.getPreviewData().getRigHandle();

        while (editPoseTurns.size() < rigHandle.getBoneCount())
            editPoseTurns.add(new Vector3());

        editPoseTurns.get(boneIndex).set(turn);

        String twinName = CharacterMeshUtility.toMirrorBoneName(rigHandle.getBone(boneIndex).getName());

        if (isMirror() && rigHandle.hasBone(twinName) && rigHandle.getBoneIndex(twinName) != boneIndex)
            editPoseTurns.get(rigHandle.getBoneIndex(twinName)).set(turn.x, -turn.y, -turn.z);

        resolveEditPosed();
        notifyChanged();
    }

    // Every bone back in the pose chosen
    void resetEditPose() {

        if (!editPosed)
            return;

        clearEditPose();
        notifyChanged();
    }

    private void clearEditPose() {

        for (int i = 0; i < editPoseTurns.size(); i++)
            editPoseTurns.get(i).set(0f, 0f, 0f);

        this.editPosed = false;
    }

    private void resolveEditPosed() {

        this.editPosed = false;

        for (int i = 0; i < editPoseTurns.size() && !editPosed; i++) {

            Vector3 turn = editPoseTurns.get(i);

            this.editPosed = turn.x != 0f || turn.y != 0f || turn.z != 0f;
        }
    }

    // The bone the Pick tool picked, the one every weight action works on
    public void pickBone(int boneIndex) {
        selectBone(boneIndex);
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

    // The Tools panel's Mirror on the mode's canvas, held on in a sided region so an edit to the side shown is always
    // made to its twin
    public boolean isMirror() {
        return isCanvasSwitchOn(ToolOption.MIRROR) || isSided(region);
    }

    // Each vertex's twin across the body on the part worked on while mirroring, null otherwise
    public IntArrayList getMirrorVertices() {

        CharacterMeshDocumentInstance document = getActiveMeshDocument();

        return document != null && isMirror() ? characterSelectionBranch.resolveMirror(document) : null;
    }

    // The Tools panel's X-Ray on the mode's canvas
    public boolean isXray() {
        return isCanvasSwitchOn(ToolOption.XRAY);
    }

    // The Tools panel's Wireframe on the mode's canvas: the edges of the mesh worked drawn over its surface
    public boolean isWireframe() {
        return isCanvasSwitchOn(ToolOption.WIREFRAME);
    }

    // The face size, in metres, the topology brush evens the faces it covers toward: the Tools panel's Detail
    float getTopologyDetail() {
        return toolManager.getOption(ToolCanvas.CHARACTER_TOPOLOGY, ToolOption.DETAIL)
                * EditorSetting.CHARACTER_TOPOLOGY_DETAIL_UNIT;
    }

    // Bone Display \\

    // How every character viewport draws the bones, one choice for the editor
    public CharacterBoneDisplay getBoneDisplay() {
        return characterBoneDisplayBranch.getDisplay();
    }

    // Each kind of bone's colour, by the kind's ordinal
    public Vector4[] getBoneColors() {
        return characterBoneDisplayBranch.getColors();
    }

    public int getBoneDisplayRevision() {
        return characterBoneDisplayBranch.getRevision();
    }

    // The bone display's rows, listed in the Properties tab of every window that draws bones
    public void buildBoneDisplayRows(ObjectArrayList<CharacterViewRowStruct> rows) {
        characterBoneDisplayBranch.buildRows(rows);
    }

    public boolean isBoneDisplayKey(String key) {
        return characterBoneDisplayBranch.isKey(key);
    }

    // The style or a kind's colour stepped, every window drawing bones and listing the rows refreshed
    public void cycleBoneDisplay(String key, int direction) {
        characterBoneDisplayBranch.cycle(key, direction);
        notifyChanged();
    }

    // Whether a bone, by name, is a part's own where the rig being edited marks the part as taking the face
    public boolean isFacialPartBone(String boneName) {

        RigHandle rigHandle = resolveRegionRig();

        return rigHandle != null && CharacterBoneKind.isFacialPartBone(rigHandle, boneName);
    }

    // Whether an on-or-off option of the mode's canvas is on; off on a canvas that has none
    private boolean isCanvasSwitchOn(ToolOption option) {

        ToolCanvas canvas = mode.getToolCanvas();

        return canvas != null && canvas.findOption(option) != null
                && Math.round(toolManager.getOption(canvas, option)) == EditorSetting.TOOL_OPTION_SWITCH_ON;
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
    // True while a modelling mode's tool works on the bones rather than the vertices: Pick and Pose, never Paint's
    // Pick, which takes a colour
    public boolean isBoneTool() {

        if (!mode.editsVertices())
            return false;

        EditorTool tool = toolManager.getTool(mode.getToolCanvas());

        return tool == EditorTool.PICK || tool == EditorTool.POSE;
    }

    // True while the Topology tab's tool works on the edge clicked rather than the vertices: Flip Edge, Loop Cut and
    // Dissolve Loop
    public boolean isEdgeTool() {

        if (!mode.editsEdges())
            return false;

        EditorTool tool = toolManager.getTool(mode.getToolCanvas());

        return tool == EditorTool.FLIP_EDGE || tool == EditorTool.LOOP_CUT || tool == EditorTool.DISSOLVE_LOOP;
    }

    // True while a modelling mode's tool is Pose
    public boolean isPoseTool() {
        return isBoneTool() && toolManager.getTool(mode.getToolCanvas()) == EditorTool.POSE;
    }

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

    // The bone a bone of the rig being edited hangs from, INDEX_NOT_FOUND for a root
    public int getRigBoneParent(int boneIndex) {
        return characterRigBranch.getParent(boneIndex);
    }

    public String getRigBoneName(int boneIndex) {
        return characterRigBranch.getBoneName(boneIndex);
    }

    public Vector3 getRigBoneSize(int boneIndex) {
        return characterRigBranch.getSize(boneIndex);
    }

    public int getRigColliderCount() {
        return syncRig() ? characterColliderBranch.getColliderCount() : 0;
    }

    public int getSelectedRigColliderIndex() {
        return syncRig() ? characterColliderBranch.getSelectedCollider() : EngineSetting.INDEX_NOT_FOUND;
    }

    public boolean isRigColliderCapsule(int colliderIndex) {
        return characterColliderBranch.isCapsule(colliderIndex);
    }

    public float getRigColliderRadius(int colliderIndex) {
        return characterColliderBranch.getRadius(colliderIndex);
    }

    // One end of a collider where the rig as it is being edited places it in the world
    public void resolveRigColliderPoint(int colliderIndex, boolean end, Vector3 out) {
        characterColliderBranch.resolvePoint(colliderIndex, end, out);
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

    // The name the character open goes by: its race's, or its model's
    private String getActiveDocumentName() {
        return activeDocument.isModel() ? activeDocument.getModelName() : activeDocument.getRaceName();
    }

    private void appendDocumentStatus(StringBuilder status) {

        status.append(getActiveDocumentName());

        if (activeDocument.isModel() && !mode.isModeling())
            status.append(EditorSetting.CHARACTER_VIEW_STATUS_SEPARATOR)
                    .append(EditorSetting.CHARACTER_MODEL_MESSAGE_RACE_TABS);

        if (!activeDocument.isModel()
                && infoManager.isDocumentDirty(EditorSetting.INFO_SCHEMA_RACES, activeDocument.getRaceName())
                || hasDirtyWornMesh()
                || syncRig() && characterRigBranch.isDirty()
                || characterControlBranch.isDirty()
                || characterClipBranch.isDirty()
                || characterPaintBranch.isDirty())
            status.append(EditorSetting.CHARACTER_VIEW_DIRTY_MARKER);

        status.append(EditorSetting.CHARACTER_VIEW_STATUS_SEPARATOR).append(mode.getLabel());

        if (mode.picksPose())
            status.append(EditorSetting.CHARACTER_VIEW_STATUS_SEPARATOR).append(getPoseLabel());

        if (isSided(region))
            status.append(EditorSetting.CHARACTER_VIEW_STATUS_SEPARATOR).append(getSide(region).getLabel());

        if (mode.isMeshMode())
            appendMeshStatus(status);

        if (mode.paintsMaps())
            characterPaintBranch.appendStatus(
                    status, characterPaintBranch.resolveActiveCanvas(getActiveMeshDocument()));
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
