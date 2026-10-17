package editor.bootstrap.animationtreeeditorpipeline.animationtreeeditormanager;

import application.bootstrap.entitypipeline.animationtree.AnimationLayerStruct;
import application.bootstrap.entitypipeline.animationtree.AnimationNodeStruct;
import application.bootstrap.entitypipeline.animationtree.AnimationParameter;
import application.bootstrap.entitypipeline.animationtree.AnimationTreeHandle;
import application.bootstrap.entitypipeline.entity.EntityAction;
import application.bootstrap.entitypipeline.entity.EntityState;
import application.bootstrap.entitypipeline.entitymanager.EntityManager;
import editor.bootstrap.animationtreeeditorpipeline.treedocument.TreeDocumentInstance;
import editor.bootstrap.animationtreeeditorpipeline.util.AnimationTreeEditorView;
import editor.bootstrap.infopipeline.infoentry.InfoEntryStruct;
import editor.bootstrap.infopipeline.infomanager.InfoManager;
import editor.characterview.panel.CharacterViewRowStruct;
import editor.runtime.EditorSetting;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.io.FileUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class AnimationTreeEditorManager extends ManagerPackage {

    /*
     * Owns the animation tree editor's shared state — the open trees, the
     * active one, the panel view, the selected layer and node, the character
     * the tree plays on, and the simulation that drives it: the movement
     * state, every animation parameter, and an action played once to its
     * end. Trees are ARPG entries owned by InfoManager: selecting one there
     * opens it here, every edit goes back through it, and every change to an
     * open tree, made here or in the Info Panel, rebuilds its preview live.
     * Edits, previews, panel values and panel rows each live in their own
     * branch.
     */

    // Internal
    private InfoManager infoManager;
    private EntityManager entityManager;
    private AnimationTreeEditBranch animationTreeEditBranch;
    private AnimationTreePreviewBranch animationTreePreviewBranch;
    private AnimationTreePropertyBranch animationTreePropertyBranch;
    private AnimationTreeRowBranch animationTreeRowBranch;

    // Palette
    private Object2ObjectOpenHashMap<String, TreeDocumentInstance> treeName2TreeDocument;

    // Active
    private TreeDocumentInstance activeDocument;
    private AnimationTreeEditorView view;
    private int selectedLayerIndex;
    private String selectedNodeName;
    private String characterName;

    // Simulation
    private EntityState movementState;
    private float[] parameters;
    private EntityAction chosenAction;
    private EntityAction playingAction;
    private float actionPhase;
    private float actionDuration;
    private int actionSequence;

    // Status
    private String statusMessage;
    private int revision;

    // Base \\

    @Override
    protected void create() {

        // Internal
        this.animationTreeEditBranch = create(AnimationTreeEditBranch.class);
        this.animationTreePreviewBranch = create(AnimationTreePreviewBranch.class);
        this.animationTreePropertyBranch = create(AnimationTreePropertyBranch.class);
        this.animationTreeRowBranch = create(AnimationTreeRowBranch.class);

        // Palette
        this.treeName2TreeDocument = new Object2ObjectOpenHashMap<>();

        // Active
        this.view = AnimationTreeEditorView.LAYER;

        // Simulation
        this.movementState = EntityState.IDLE;
        this.parameters = new float[AnimationParameter.VALUES.length];
        this.chosenAction = EntityAction.SWING;
        this.playingAction = EntityAction.NONE;
    }

    @Override
    protected void get() {
        this.infoManager = get(InfoManager.class);
        this.entityManager = get(EntityManager.class);
    }

    @Override
    protected void awake() {
        infoManager.addSelectionListener(EditorSetting.INFO_SCHEMA_ANIMATION_TREES, this::openEntry);
        infoManager.addChangeListener(EditorSetting.INFO_SCHEMA_ANIMATION_TREES, this::refreshEntry);
        infoManager.addDeleteListener(EditorSetting.INFO_SCHEMA_ANIMATION_TREES, this::closeEntry);
    }

    // Update \\

    @Override
    protected void update() {

        if (playingAction == EntityAction.NONE)
            return;

        actionPhase += internal.getDeltaTime() / actionDuration;

        if (actionPhase < 1f)
            return;

        this.playingAction = EntityAction.NONE;
        this.actionPhase = 0f;
        notifyChanged();
    }

    // Trees \\

    private void openEntry(InfoEntryStruct entry) {

        TreeDocumentInstance document = treeName2TreeDocument.get(entry.getEntryName());

        if (document == null) {
            document = create(TreeDocumentInstance.class);
            document.constructor(entry.getEntryName());
            treeName2TreeDocument.put(entry.getEntryName(), document);
            animationTreePreviewBranch.rebuild(document, entry.getArpg());
        }

        if (document != activeDocument)
            activate(document);
    }

    private void refreshEntry(InfoEntryStruct entry) {

        TreeDocumentInstance document = treeName2TreeDocument.get(entry.getEntryName());

        if (document == null)
            return;

        animationTreePreviewBranch.rebuild(document, entry.getArpg());

        if (document == activeDocument)
            resolveSelection();

        notifyChanged();
    }

    private void closeEntry(InfoEntryStruct entry) {

        TreeDocumentInstance document = treeName2TreeDocument.remove(entry.getEntryName());

        if (document != null && document == activeDocument)
            activate(null);
    }

    private void activate(TreeDocumentInstance document) {

        this.activeDocument = document;
        this.selectedLayerIndex = 0;
        this.selectedNodeName = null;
        this.playingAction = EntityAction.NONE;
        this.statusMessage = null;

        if (document != null)
            resolveSelection();

        notifyChanged();
    }

    // Keeps the selected layer and node on ones the tree still has, and the character on its rig
    private void resolveSelection() {

        int layerCount = getLayerCount();

        if (selectedLayerIndex >= layerCount)
            this.selectedLayerIndex = Math.max(0, layerCount - 1);

        ObjectArrayList<String> nodeNames = getNodeNames();

        if (!nodeNames.contains(selectedNodeName))
            this.selectedNodeName = nodeNames.isEmpty() ? null : nodeNames.get(0);

        ObjectArrayList<String> characterNames = getCharacterNames();

        if (!characterNames.contains(characterName))
            this.characterName = characterNames.isEmpty() ? null : characterNames.get(0);
    }

    public void createTree(String localName) {

        if (!isTreeNameAvailable(localName))
            return;

        String rigName = activeDocument != null
                ? ArpgUtility.getString(
                        getTreeArpg(),
                        EditorSetting.ANIMATION_TREE_EDITOR_FIELD_RIG,
                        EditorSetting.ANIMATION_EDITOR_DEFAULT_RIG)
                : EditorSetting.ANIMATION_EDITOR_DEFAULT_RIG;

        infoManager.createDocument(EditorSetting.INFO_SCHEMA_ANIMATION_TREES, toSiblingName(localName), root -> {

            ArpgObjectStruct baseLayer = new ArpgObjectStruct();
            baseLayer.addProperty(
                    EditorSetting.ANIMATION_TREE_EDITOR_FIELD_NAME,
                    EditorSetting.ANIMATION_TREE_EDITOR_BASE_LAYER);
            baseLayer.add(EditorSetting.ANIMATION_TREE_EDITOR_FIELD_NODES, new ArpgObjectStruct());

            ArpgArrayStruct layers = new ArpgArrayStruct();
            layers.add(baseLayer);

            root.addProperty(EditorSetting.ANIMATION_TREE_EDITOR_FIELD_RIG, rigName);
            root.add(EditorSetting.ANIMATION_TREE_EDITOR_FIELD_LAYERS, layers);
        });
        setStatusMessage(EditorSetting.ANIMATION_TREE_EDITOR_MESSAGE_CREATED + localName);
    }

    public void saveActiveTree() {

        if (activeDocument == null)
            return;

        infoManager.saveDocument(EditorSetting.INFO_SCHEMA_ANIMATION_TREES, activeDocument.getTreeName());
        setStatusMessage(EditorSetting.ANIMATION_TREE_EDITOR_MESSAGE_SAVED + activeDocument.getTreeName());
    }

    public void revertActiveTree() {

        if (activeDocument == null)
            return;

        infoManager.revertDocument(EditorSetting.INFO_SCHEMA_ANIMATION_TREES, activeDocument.getTreeName());
        setStatusMessage(EditorSetting.ANIMATION_TREE_EDITOR_MESSAGE_REVERTED + activeDocument.getTreeName());
    }

    public void deleteActiveTree() {

        if (activeDocument == null)
            return;

        String treeName = activeDocument.getTreeName();

        infoManager.deleteDocument(EditorSetting.INFO_SCHEMA_ANIMATION_TREES, treeName);
        setStatusMessage(EditorSetting.ANIMATION_TREE_EDITOR_MESSAGE_DELETED + treeName);
    }

    public boolean isTreeNameAvailable(String localName) {
        return FileUtility.isValidFileName(localName, EditorSetting.NAME_INPUT_MAX_LENGTH)
                && !infoManager.hasDocument(EditorSetting.INFO_SCHEMA_ANIMATION_TREES, toSiblingName(localName));
    }

    public boolean isActiveTreeName(String localName) {
        return activeDocument != null && toLocalName(activeDocument.getTreeName()).equals(localName);
    }

    public String getActiveLocalName() {
        return activeDocument != null ? toLocalName(activeDocument.getTreeName()) : "";
    }

    // A new tree beside the active one, in the same folder, or at the top when none is open
    private String toSiblingName(String localName) {

        if (activeDocument == null)
            return localName;

        String treeName = activeDocument.getTreeName();
        int separator = treeName.lastIndexOf(EditorSetting.INFO_FOLDER_SEPARATOR);

        return separator == EngineSetting.INDEX_NOT_FOUND
                ? localName
                : treeName.substring(0, separator + EditorSetting.INFO_FOLDER_SEPARATOR.length()) + localName;
    }

    private String toLocalName(String treeName) {
        return treeName.substring(treeName.lastIndexOf(EditorSetting.INFO_FOLDER_SEPARATOR) + 1);
    }

    // Layers and Nodes \\

    public boolean isLayerNameValid(String layerName) {
        return activeDocument != null && FileUtility.isValidFileName(layerName, EditorSetting.NAME_INPUT_MAX_LENGTH);
    }

    public void addLayer(String layerName) {

        if (!isLayerNameValid(layerName))
            return;

        animationTreeEditBranch.addLayer(activeDocument, layerName);
        selectLayer(getLayerCount() - 1);
    }

    public void renameSelectedLayer(String layerName) {

        if (isLayerNameValid(layerName) && selectedLayerIndex < getLayerCount())
            animationTreeEditBranch.renameLayer(activeDocument, selectedLayerIndex, layerName);
    }

    public String getSelectedLayerName() {

        ArpgObjectStruct layer = animationTreePropertyBranch.getLayerArpg();
        return layer != null ? ArpgUtility.getString(layer, EditorSetting.ANIMATION_TREE_EDITOR_FIELD_NAME, "") : "";
    }

    public boolean isNodeNameAvailable(String nodeName) {
        return activeDocument != null
                && selectedLayerIndex < getLayerCount()
                && FileUtility.isValidFileName(nodeName, EditorSetting.NAME_INPUT_MAX_LENGTH)
                && !getNodeNames().contains(nodeName);
    }

    // A new node plays the first clip made for the tree's rig, and is selected at once
    public void addNode(String nodeName) {

        if (!isNodeNameAvailable(nodeName))
            return;

        ObjectArrayList<String> clipNames = getClipNames();

        animationTreeEditBranch.addNode(
                activeDocument,
                selectedLayerIndex,
                nodeName,
                clipNames.isEmpty() ? "" : clipNames.get(0));
        selectNode(nodeName);
    }

    public void renameSelectedNode(String nodeName) {

        if (selectedNodeName == null || !isNodeNameAvailable(nodeName))
            return;

        String previous = selectedNodeName;

        this.selectedNodeName = nodeName;
        animationTreeEditBranch.renameNode(activeDocument, selectedLayerIndex, previous, nodeName);
    }

    public String getSelectedNodeName() {
        return selectedNodeName;
    }

    // Views \\

    public void setView(AnimationTreeEditorView view) {

        this.view = view;
        notifyChanged();
    }

    // Selection \\

    void selectLayer(int layerIndex) {

        this.selectedLayerIndex = layerIndex;
        this.selectedNodeName = null;
        resolveSelection();
        notifyChanged();
    }

    void selectNode(String nodeName) {

        this.selectedNodeName = nodeName;
        notifyChanged();
    }

    // Simulation \\

    void setMovementState(EntityState movementState) {

        this.movementState = movementState;
        notifyChanged();
    }

    void setParameter(AnimationParameter parameter, float value) {

        parameters[parameter.ordinal()] = value;
        notifyChanged();
    }

    void chooseAction(EntityAction action) {

        this.chosenAction = action;
        notifyChanged();
    }

    // Plays the chosen action once, for as long as the first node any layer plays it with runs
    void playAction() {

        float duration = resolveActionDuration(chosenAction);

        if (duration <= 0f) {
            setStatusMessage(
                    EditorSetting.ANIMATION_TREE_EDITOR_MESSAGE_NO_ACTION + ArpgUtility.toEnumName(chosenAction));
            return;
        }

        this.playingAction = chosenAction;
        this.actionPhase = 0f;
        this.actionDuration = duration;
        this.actionSequence++;
        notifyChanged();
    }

    private float resolveActionDuration(EntityAction action) {

        if (activeDocument == null || !activeDocument.hasPreviewTree())
            return 0f;

        AnimationTreeHandle tree = activeDocument.getPreviewTree();

        for (int i = 0; i < tree.getLayerCount(); i++) {

            AnimationLayerStruct layer = tree.getLayer(i);

            if (!layer.hasActionNode(action.ordinal()))
                continue;

            AnimationNodeStruct node = layer.getNode(layer.getActionNode(action.ordinal()));
            return node.getClip(0).getDuration() / node.getRate();
        }

        return 0f;
    }

    // Character \\

    public ObjectArrayList<String> getCharacterNames() {

        return activeDocument != null && activeDocument.hasRig()
                ? entityManager.getTemplateNamesForRig(activeDocument.getRigHandle())
                : new ObjectArrayList<>();
    }

    void cycleCharacter(int direction) {

        ObjectArrayList<String> characterNames = getCharacterNames();

        if (characterNames.isEmpty())
            return;

        int current = Math.max(0, characterNames.indexOf(characterName));

        this.characterName = characterNames.get(Math.floorMod(current + direction, characterNames.size()));
        notifyChanged();
    }

    // Every clip made for the tree's rig, by name, saved or not
    ObjectArrayList<String> getClipNames() {

        ObjectArrayList<String> clipNames = new ObjectArrayList<>();
        String rigName = ArpgUtility.getString(getTreeArpg(), EditorSetting.ANIMATION_TREE_EDITOR_FIELD_RIG, "");
        ObjectArrayList<InfoEntryStruct> clips = infoManager.getEntries(EditorSetting.INFO_SCHEMA_ANIMATIONS);

        for (int i = 0; i < clips.size(); i++)
            if (rigName.equals(ArpgUtility.getString(
                    clips.get(i).getArpg(), EditorSetting.ANIMATION_EDITOR_FIELD_RIG, "")))
                clipNames.add(clips.get(i).getEntryName());

        clipNames.sort(String.CASE_INSENSITIVE_ORDER);
        return clipNames;
    }

    // Panel \\

    public void select(String key) {

        if (activeDocument != null)
            animationTreePropertyBranch.select(key);
    }

    public void step(String key, int direction) {

        if (activeDocument != null)
            animationTreePropertyBranch.step(activeDocument, key, direction);
    }

    public void cycle(String key, int direction) {

        if (activeDocument != null)
            animationTreePropertyBranch.cycle(activeDocument, key, direction);
    }

    public void act(String key) {

        if (activeDocument != null)
            animationTreePropertyBranch.act(activeDocument, key);
    }

    public String getValueText(String key) {
        return activeDocument != null ? animationTreePropertyBranch.getValueText(key) : "";
    }

    public boolean isValueValid(String key, String text) {
        return activeDocument != null && animationTreePropertyBranch.isValueValid(key, text);
    }

    public void setValue(String key, String text) {

        if (isValueValid(key, text))
            animationTreePropertyBranch.setValue(activeDocument, key, text);
    }

    public void buildListRows(ObjectArrayList<CharacterViewRowStruct> rows) {

        if (activeDocument != null)
            animationTreeRowBranch.buildListRows(activeDocument, rows);
    }

    public void buildPropertyRows(ObjectArrayList<CharacterViewRowStruct> rows) {

        if (activeDocument != null)
            animationTreeRowBranch.buildPropertyRows(activeDocument, rows);
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

    public TreeDocumentInstance getActiveDocument() {
        return activeDocument;
    }

    public boolean hasActiveDocument() {
        return activeDocument != null;
    }

    // The active tree's live ARPG, as InfoManager holds it
    ArpgObjectStruct getTreeArpg() {

        InfoEntryStruct entry = infoManager.findEntry(
                EditorSetting.INFO_SCHEMA_ANIMATION_TREES,
                activeDocument.getTreeName(),
                activeDocument.getTreeName());

        return entry != null ? entry.getArpg() : null;
    }

    int getLayerCount() {
        return animationTreePropertyBranch.getLayerCount();
    }

    ObjectArrayList<String> getNodeNames() {
        return animationTreePropertyBranch.getNodeNames();
    }

    public AnimationTreeEditorView getView() {
        return view;
    }

    public int getSelectedLayerIndex() {
        return selectedLayerIndex;
    }

    public String getCharacterName() {
        return characterName;
    }

    public EntityState getMovementState() {
        return movementState;
    }

    public float getParameter(AnimationParameter parameter) {
        return parameters[parameter.ordinal()];
    }

    public EntityAction getChosenAction() {
        return chosenAction;
    }

    public EntityAction getPlayingAction() {
        return playingAction;
    }

    public float getActionPhase() {
        return actionPhase;
    }

    public int getActionSequence() {
        return actionSequence;
    }

    public int getRevision() {
        return revision;
    }

    public String getStatusText() {

        StringBuilder status = new StringBuilder();

        if (activeDocument == null)
            status.append(EditorSetting.ANIMATION_TREE_EDITOR_STATUS_NO_TREE);
        else
            appendDocumentStatus(status);

        if (statusMessage != null)
            status.append(EditorSetting.CHARACTER_VIEW_STATUS_SEPARATOR).append(statusMessage);

        return status.toString();
    }

    private void appendDocumentStatus(StringBuilder status) {

        status.append(activeDocument.getTreeName());

        if (infoManager.isDocumentDirty(EditorSetting.INFO_SCHEMA_ANIMATION_TREES, activeDocument.getTreeName()))
            status.append(EditorSetting.CHARACTER_VIEW_DIRTY_MARKER);

        status.append(EditorSetting.CHARACTER_VIEW_STATUS_SEPARATOR).append(view.getLabel());
        status.append(EditorSetting.CHARACTER_VIEW_STATUS_SEPARATOR).append(ArpgUtility.toEnumName(movementState));

        if (playingAction != EntityAction.NONE)
            status.append(EditorSetting.CHARACTER_VIEW_STATUS_SEPARATOR).append(ArpgUtility.toEnumName(playingAction));

        if (!activeDocument.hasRig())
            status.append(EditorSetting.CHARACTER_VIEW_STATUS_SEPARATOR)
                    .append(EditorSetting.ANIMATION_TREE_EDITOR_STATUS_NO_RIG);
        else if (characterName == null)
            status.append(EditorSetting.CHARACTER_VIEW_STATUS_SEPARATOR)
                    .append(EditorSetting.ANIMATION_TREE_EDITOR_STATUS_NO_CHARACTER);

        if (activeDocument.hasPreviewError())
            status.append(EditorSetting.CHARACTER_VIEW_STATUS_SEPARATOR)
                    .append(EditorSetting.CHARACTER_VIEW_MESSAGE_PREVIEW_REFUSED)
                    .append(activeDocument.getPreviewError());
    }
}
