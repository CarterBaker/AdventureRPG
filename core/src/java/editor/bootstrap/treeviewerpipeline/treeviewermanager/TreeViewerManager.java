package editor.bootstrap.treeviewerpipeline.treeviewermanager;

import java.util.Objects;

import application.bootstrap.worldpipeline.tree.TreeHandle;
import application.bootstrap.worldpipeline.tree.TreeImpostorStruct;
import application.bootstrap.worldpipeline.treemanager.TreeManager;
import editor.bootstrap.infopipeline.infoentry.InfoEntryStruct;
import editor.bootstrap.infopipeline.infomanager.InfoManager;
import editor.bootstrap.treeviewerpipeline.treeviewer.TreePlannerNode;
import editor.bootstrap.treeviewerpipeline.treeviewer.TreePlannerParameter;
import editor.bootstrap.treeviewerpipeline.treeviewer.TreePreviewLayerStruct;
import editor.runtime.EditorSetting;
import editor.runtime.characterview.panel.CharacterViewRowStruct;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class TreeViewerManager extends ManagerPackage {

    /*
     * Owns what the Tree Planner and Tree Viewer work on: the species picked
     * in the Palette tab or the Hierarchy, the two always following each
     * other, the node of its generator picked in the planner's graph, whose
     * fields the Properties tab edits through TreeEditBranch live in every
     * window, and the seed and age the preview grows its tree from, which the
     * Tools tab's Preview actions step. Any change to the species, its seed,
     * its age, its impostor or the shared tree LOD setting has
     * TreePreviewBranch build the preview again on the next frame, once the
     * species itself has been rebuilt. New, Duplicate, Save and Revert act on the species' file.
     */

    // Internal
    private InfoManager infoManager;
    private TreeManager treeManager;
    private TreeEditBranch treeEditBranch;
    private TreePreviewBranch treePreviewBranch;

    // Selection
    private String treeName;
    private TreePlannerNode node;

    // Catalogue
    private ObjectArrayList<String> treeNames;
    private int shownInfoRevision;

    // Preview
    private long seed;
    private float age;
    private int sharedLevel;
    private TreeImpostorStruct shownImpostor;
    private boolean previewStale;

    // Status
    private String statusMessage;
    private int revision;

    // Base \\

    @Override
    protected void create() {

        // Internal
        this.treeEditBranch = create(TreeEditBranch.class);
        this.treePreviewBranch = create(TreePreviewBranch.class);

        // Selection
        this.node = TreePlannerNode.TREE;

        // Catalogue
        this.treeNames = new ObjectArrayList<>();
        this.shownInfoRevision = EngineSetting.INDEX_NOT_FOUND;

        // Preview
        this.seed = EditorSetting.TREE_VIEWER_FIRST_SEED;
        this.age = EditorSetting.TREE_VIEWER_AGE_DEFAULT;
    }

    @Override
    protected void get() {
        this.infoManager = get(InfoManager.class);
        this.treeManager = get(TreeManager.class);
    }

    @Override
    protected void awake() {

        this.sharedLevel = internal.settings.treeSharedLevel;

        infoManager.addSelectionListener(
                EditorSetting.INFO_SCHEMA_TREES, entry -> applySelection(entry.getEntryName()));
        infoManager.addChangeListener(EditorSetting.INFO_SCHEMA_TREES, entry -> applyChange(entry.getEntryName()));
        infoManager.addDeleteListener(EditorSetting.INFO_SCHEMA_TREES, entry -> applyDelete(entry.getEntryName()));
    }

    // Update \\

    @Override
    protected void update() {

        if (infoManager.getRevision() != shownInfoRevision) {
            shownInfoRevision = infoManager.getRevision();
            refreshCatalogue();
        }

        TreeHandle treeHandle = treeName != null ? treeManager.findTreeHandle(treeName) : null;
        TreeImpostorStruct impostor = treeHandle != null ? treeHandle.getImpostor() : null;

        if (impostor != shownImpostor) {
            shownImpostor = impostor;
            previewStale = true;
            notifyChanged();
        }

        if (internal.settings.treeSharedLevel != sharedLevel) {
            sharedLevel = internal.settings.treeSharedLevel;
            previewStale = true;
            notifyChanged();
        }

        if (previewStale) {
            previewStale = false;
            treePreviewBranch.request(treeName, seed, age, sharedLevel);
        }

        treePreviewBranch.refresh();
    }

    // Catalogue \\

    // Every tree species the Info files hold, by name, the revision moving only when the names do
    private void refreshCatalogue() {

        ObjectArrayList<InfoEntryStruct> entries = infoManager.getEntries(EditorSetting.INFO_SCHEMA_TREES);
        ObjectArrayList<String> names = new ObjectArrayList<>(entries.size());

        for (int i = 0; i < entries.size(); i++)
            names.add(entries.get(i).getEntryName());

        names.sort(null);

        if (names.equals(treeNames))
            return;

        treeNames = names;
        notifyChanged();
    }

    // Every species to pick, the one picked selected
    public void buildTreeRows(ObjectArrayList<CharacterViewRowStruct> rows) {

        rows.add(CharacterViewRowStruct.header(EditorSetting.TREE_PLANNER_PALETTE_HEADER));

        for (int i = 0; i < treeNames.size(); i++) {

            String name = treeNames.get(i);

            rows.add(CharacterViewRowStruct.item(name, name, name.equals(treeName)));
        }
    }

    // Selection \\

    // A species picked in the Palette tab, opened in the Hierarchy and the Info Panel alike
    public void selectTree(String selectedName) {

        infoManager.selectEntry(EditorSetting.INFO_SCHEMA_TREES, selectedName, selectedName);
        applySelection(selectedName);
    }

    public void selectNode(TreePlannerNode selectedNode) {

        this.node = selectedNode;
        notifyChanged();
    }

    private void applySelection(String selectedName) {

        if (Objects.equals(selectedName, treeName))
            return;

        this.treeName = selectedName;
        this.previewStale = true;
        notifyChanged();
    }

    private void applyChange(String changedName) {

        if (!Objects.equals(changedName, treeName))
            return;

        this.previewStale = true;
        notifyChanged();
    }

    private void applyDelete(String deletedName) {

        if (!Objects.equals(deletedName, treeName))
            return;

        this.treeName = null;
        this.previewStale = true;
        notifyChanged();
    }

    // The selected species opened in the Hierarchy and Info Panel, so their tree tools work on it; false with none
    public boolean focusTree() {

        if (treeName == null) {
            setStatusMessage(EditorSetting.TREE_VIEWER_MESSAGE_NO_TREE);
            return false;
        }

        infoManager.selectEntry(EditorSetting.INFO_SCHEMA_TREES, treeName, treeName);
        return true;
    }

    // Fields \\

    // The picked node's fields, a choice stepped round its names and a number slid across its bounds
    public void buildFieldRows(ObjectArrayList<CharacterViewRowStruct> rows) {

        rows.add(CharacterViewRowStruct.header(node.displayName + EditorSetting.TREE_PLANNER_FIELDS_SUFFIX));

        if (treeName == null)
            return;

        TreePlannerParameter[] parameters = TreePlannerParameter.values();

        for (int i = 0; i < parameters.length; i++) {

            TreePlannerParameter parameter = parameters[i];

            if (parameter.node != node)
                continue;

            rows.add(parameter.choices != null
                    ? CharacterViewRowStruct.choice(parameter.name(), parameter.label, formatValue(parameter))
                    : CharacterViewRowStruct.ranged(
                            parameter.name(), parameter.label, formatValue(parameter),
                            treeEditBranch.read(treeName, parameter), parameter.min, parameter.max, parameter.step));
        }
    }

    // A field moved a whole number of its steps, a choice round its names
    public void stepField(String key, int steps) {

        TreePlannerParameter parameter = TreePlannerParameter.valueOf(key);

        if (!treeEditBranch.step(treeName, parameter, steps)) {
            setStatusMessage(EditorSetting.TREE_VIEWER_MESSAGE_NO_TREE);
            return;
        }

        setStatusMessage(EditorSetting.TREE_VIEWER_MESSAGE_SET + parameter.label + " " + formatValue(parameter));
    }

    public String getFieldText(String key) {
        return formatValue(TreePlannerParameter.valueOf(key));
    }

    public boolean isFieldValid(String key, String text) {
        return TreePlannerParameter.valueOf(key).choices == null && parseField(text) != null;
    }

    // A field set to a value typed, held within its bounds
    public void setField(String key, String text) {

        TreePlannerParameter parameter = TreePlannerParameter.valueOf(key);
        Float value = parseField(text);

        if (value == null)
            return;

        if (!treeEditBranch.set(treeName, parameter, value)) {
            setStatusMessage(EditorSetting.TREE_VIEWER_MESSAGE_NO_TREE);
            return;
        }

        setStatusMessage(EditorSetting.TREE_VIEWER_MESSAGE_SET + parameter.label + " " + formatValue(parameter));
    }

    private Float parseField(String text) {

        try {
            float value = Float.parseFloat(text.trim());
            return Float.isFinite(value) ? value : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // A field's value as the tree tabs show it: a choice by its name, a whole number as one, a decimal to two places
    public String formatValue(TreePlannerParameter parameter) {

        float value = treeEditBranch.read(treeName, parameter);

        if (parameter.choices != null)
            return parameter.choices.getEnumConstants()[(int) value].name().toLowerCase();

        if (parameter.whole)
            return Integer.toString(Math.round(value));

        return String.format(EditorSetting.TREE_PLANNER_VALUE_FORMAT, value);
    }

    // Actions \\

    // A Tools tab action of the Tree Viewer run by its key
    public void act(String actionKey) {
        switch (actionKey) {
            case EditorSetting.TREE_VIEWER_KEY_RESEED -> reseed();
            case EditorSetting.TREE_VIEWER_KEY_YOUNGER -> stepAge(EditorSetting.TREE_VIEWER_STEP_DOWN);
            case EditorSetting.TREE_VIEWER_KEY_OLDER -> stepAge(EditorSetting.TREE_VIEWER_STEP_UP);
            default -> {
            }
        }
    }

    private void reseed() {

        seed++;
        previewStale = true;
        setStatusMessage(EditorSetting.TREE_VIEWER_MESSAGE_RESEEDED);
    }

    // The preview's tree grown one step younger or older, never past full grown nor down to a seed
    private void stepAge(int direction) {

        age = Math.clamp(
                age + EditorSetting.TREE_VIEWER_AGE_STEP * direction,
                EditorSetting.TREE_VIEWER_AGE_MIN,
                EditorSetting.TREE_VIEWER_AGE_DEFAULT);
        previewStale = true;
        setStatusMessage(EditorSetting.TREE_VIEWER_MESSAGE_AGED);
    }

    // File \\

    public void save() {

        if (!hasTree()) {
            setStatusMessage(EditorSetting.TREE_VIEWER_MESSAGE_NO_TREE);
            return;
        }

        String message = infoManager.saveDocument(EditorSetting.INFO_SCHEMA_TREES, treeName)
                ? EditorSetting.TREE_VIEWER_MESSAGE_SAVED
                : EditorSetting.TREE_VIEWER_MESSAGE_SAVE_FAILED;

        setStatusMessage(message + treeName);
    }

    public void revert() {

        if (!hasTree()) {
            setStatusMessage(EditorSetting.TREE_VIEWER_MESSAGE_NO_TREE);
            return;
        }

        infoManager.revertDocument(EditorSetting.INFO_SCHEMA_TREES, treeName);
        previewStale = true;
        setStatusMessage(EditorSetting.TREE_VIEWER_MESSAGE_REVERTED + treeName);
    }

    private boolean hasTree() {
        return treeName != null && infoManager.hasDocument(EditorSetting.INFO_SCHEMA_TREES, treeName);
    }

    // Status \\

    private void notifyChanged() {
        revision++;
    }

    private void setStatusMessage(String statusMessage) {

        this.statusMessage = statusMessage;
        notifyChanged();
    }

    // Accessible \\

    public String getTreeName() {
        return treeName;
    }

    public TreePlannerNode getNode() {
        return node;
    }

    public long getSeed() {
        return seed;
    }

    public float getAge() {
        return age;
    }

    public int getSharedLevel() {
        return sharedLevel;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public int getRevision() {
        return revision;
    }

    // Preview \\

    public TreePreviewLayerStruct getPreviewLayer(int layer) {
        return treePreviewBranch.getLayer(layer);
    }

    public boolean isPreviewImpostorLayer(int layer) {
        return treePreviewBranch.isImpostorLayer(layer);
    }

    public float[] getPreviewImpostorInstance(int layer) {
        return treePreviewBranch.getImpostorInstance(layer);
    }

    public float getPreviewHeight() {
        return treePreviewBranch.getHeight();
    }

    public int getPreviewRevision() {
        return treePreviewBranch.getRevision();
    }
}
