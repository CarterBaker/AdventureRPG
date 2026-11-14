package editor.bootstrap.treeplannerpipeline.treeplannermanager;

import java.util.Objects;

import editor.bootstrap.infopipeline.infoentry.InfoEntryStruct;
import editor.bootstrap.infopipeline.infomanager.InfoManager;
import editor.bootstrap.treeplannerpipeline.treeplanner.TreePlannerNode;
import editor.bootstrap.treeplannerpipeline.treeplanner.TreePlannerParameter;
import editor.bootstrap.treeplannerpipeline.treeplanner.TreePreviewTierStruct;
import editor.runtime.EditorSetting;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.arpg.ArpgUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class TreePlannerManager extends ManagerPackage {

    /*
     * Owns the Tree Planner: the species it works on — the one selected in
     * the Hierarchy's Trees or in the planner's own list, the two always
     * following each other — the node of its generator open in the planner,
     * and the seed and age the preview grows its tree from. Stepping a field
     * goes through TreeEditBranch into the species' Info entry, so every
     * window sees it live; any change to the species, its seed or its age has
     * TreePreviewBranch build the row the Tree Viewer shows again on the next
     * frame, once the species itself has been rebuilt. Save and Revert act on
     * the species' file.
     */

    // Internal
    private InfoManager infoManager;
    private TreeEditBranch treeEditBranch;
    private TreePreviewBranch treePreviewBranch;

    // Selection
    private String treeName;
    private TreePlannerNode node;

    // Preview
    private long seed;
    private float age;
    private boolean previewStale;

    // Catalogue
    private ObjectArrayList<String> treeNames;
    private int shownInfoRevision;
    private int catalogueRevision;

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

        // Preview
        this.seed = EditorSetting.TREE_PLANNER_FIRST_SEED;
        this.age = EditorSetting.TREE_PLANNER_AGE_DEFAULT;

        // Catalogue
        this.treeNames = new ObjectArrayList<>();
        this.shownInfoRevision = EngineSetting.INDEX_NOT_FOUND;
    }

    @Override
    protected void get() {
        this.infoManager = get(InfoManager.class);
    }

    @Override
    protected void awake() {
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

        if (previewStale) {
            previewStale = false;
            treePreviewBranch.request(treeName, seed, age);
        }

        treePreviewBranch.refresh();
    }

    // Catalogue \\

    // Every tree species the Info files hold, by name, its revision moving only when the names do
    private void refreshCatalogue() {

        ObjectArrayList<InfoEntryStruct> entries = infoManager.getEntries(EditorSetting.INFO_SCHEMA_TREES);
        ObjectArrayList<String> names = new ObjectArrayList<>(entries.size());

        for (int i = 0; i < entries.size(); i++)
            names.add(entries.get(i).getEntryName());

        names.sort(null);

        if (names.equals(treeNames))
            return;

        treeNames = names;
        catalogueRevision++;
    }

    // Selection \\

    // A species opened in the planner, the Hierarchy and the Info Panel alike
    public void selectTree(String treeName) {

        infoManager.selectEntry(EditorSetting.INFO_SCHEMA_TREES, treeName, treeName);
        applySelection(treeName);
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
            setStatusMessage(EditorSetting.TREE_PLANNER_MESSAGE_NO_TREE);
            return false;
        }

        infoManager.selectEntry(EditorSetting.INFO_SCHEMA_TREES, treeName, treeName);
        return true;
    }

    public void selectNode(TreePlannerNode node) {

        this.node = node;
        notifyChanged();
    }

    // Fields \\

    // One field of the species stepped up or down, live in every window
    public void step(TreePlannerParameter parameter, int direction) {

        if (!treeEditBranch.step(treeName, parameter, direction)) {
            setStatusMessage(EditorSetting.TREE_PLANNER_MESSAGE_NO_TREE);
            return;
        }

        setStatusMessage(EditorSetting.TREE_PLANNER_MESSAGE_SET + parameter.label + " " + formatValue(parameter));
    }

    // A field's value as the planner shows it: a choice by its name, a whole number as one, a decimal to two places
    public String formatValue(TreePlannerParameter parameter) {

        float value = treeEditBranch.read(treeName, parameter);

        if (parameter.choices != null)
            return parameter.choices.getEnumConstants()[(int) value].name().toLowerCase();

        if (parameter.whole)
            return Integer.toString(Math.round(value));

        return String.format(EditorSetting.TREE_PLANNER_VALUE_FORMAT, value);
    }

    // Preview \\

    public void reseed() {

        seed++;
        previewStale = true;
        setStatusMessage(EditorSetting.TREE_PLANNER_MESSAGE_RESEEDED);
    }

    // The preview's tree grown one step younger or older, never past full grown nor down to a seed
    public void stepAge(int direction) {

        age = Math.clamp(
                age + EditorSetting.TREE_PLANNER_AGE_STEP * direction,
                EditorSetting.TREE_PLANNER_AGE_MIN,
                EditorSetting.TREE_PLANNER_AGE_DEFAULT);
        previewStale = true;
        setStatusMessage(EditorSetting.TREE_PLANNER_MESSAGE_AGED);
    }

    // File \\

    public void save() {

        if (!hasTree()) {
            setStatusMessage(EditorSetting.TREE_PLANNER_MESSAGE_NO_TREE);
            return;
        }

        String message = infoManager.saveDocument(EditorSetting.INFO_SCHEMA_TREES, treeName)
                ? EditorSetting.TREE_PLANNER_MESSAGE_SAVED
                : EditorSetting.TREE_PLANNER_MESSAGE_SAVE_FAILED;

        setStatusMessage(message + treeName);
    }

    public void revert() {

        if (!hasTree()) {
            setStatusMessage(EditorSetting.TREE_PLANNER_MESSAGE_NO_TREE);
            return;
        }

        infoManager.revertDocument(EditorSetting.INFO_SCHEMA_TREES, treeName);
        previewStale = true;
        setStatusMessage(EditorSetting.TREE_PLANNER_MESSAGE_REVERTED + treeName);
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

    // A species' display name from its file, its resource name when it has none
    public String getDisplayName(String name) {

        InfoEntryStruct entry = infoManager.hasDocument(EditorSetting.INFO_SCHEMA_TREES, name)
                ? infoManager.findEntry(EditorSetting.INFO_SCHEMA_TREES, name, name)
                : null;
        String displayName = entry != null ? ArpgUtility.getString(entry.getArpg(), "display_name", "") : "";

        return displayName.isEmpty() ? name : displayName;
    }

    public TreePlannerNode getNode() {
        return node;
    }

    public ObjectArrayList<String> getTreeNames() {
        return treeNames;
    }

    public int getCatalogueRevision() {
        return catalogueRevision;
    }

    public String getStatusText() {

        String tree = treeName != null
                ? EditorSetting.TREE_PLANNER_STATUS_TREE + getDisplayName(treeName)
                        + EditorSetting.TREE_PLANNER_STATUS_AGE
                        + Math.round(age * EditorSetting.TREE_PLANNER_PERCENT) + EditorSetting.TREE_PLANNER_PERCENT_SIGN
                        + EditorSetting.TREE_PLANNER_STATUS_SEED + seed
                : EditorSetting.TREE_PLANNER_STATUS_NONE;

        return statusMessage != null ? tree + EditorSetting.WORLD_EDITOR_STATUS_SEPARATOR + statusMessage : tree;
    }

    public int getRevision() {
        return revision;
    }

    // Preview \\

    public int getPreviewTierCount() {
        return treePreviewBranch.getTierCount();
    }

    public TreePreviewTierStruct getPreviewTier(int tier) {
        return treePreviewBranch.getTier(tier);
    }

    public float getPreviewTierOffsetX(int tier) {
        return treePreviewBranch.getTierOffsetX(tier);
    }

    public float getPreviewHeight() {
        return treePreviewBranch.getHeight();
    }

    public int getPreviewRevision() {
        return treePreviewBranch.getRevision();
    }
}
