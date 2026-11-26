package editor.bootstrap.treeviewerpipeline.treeviewermanager;

import java.util.Objects;

import editor.bootstrap.infopipeline.infomanager.InfoManager;
import editor.bootstrap.treeviewerpipeline.treeviewer.TreePreviewLayerStruct;
import editor.runtime.EditorSetting;
import engine.root.ManagerPackage;

public class TreeViewerManager extends ManagerPackage {

    /*
     * Owns what the Tree Viewer works on: the species selected in the
     * Hierarchy, whose every field the Properties tab edits live, LOD seed
     * included, and the seed and age the preview grows its tree from, which
     * the Tools tab's Preview actions step. Any change to the species, its
     * seed, its age or the shared tree LOD setting has TreePreviewBranch
     * build the preview again on the next frame, once the species itself has
     * been rebuilt. New, Duplicate, Save and Revert act on the species' file.
     */

    // Internal
    private InfoManager infoManager;
    private TreePreviewBranch treePreviewBranch;

    // Selection
    private String treeName;

    // Preview
    private long seed;
    private float age;
    private int sharedLevel;
    private boolean previewStale;

    // Status
    private String statusMessage;
    private int revision;

    // Base \\

    @Override
    protected void create() {

        // Internal
        this.treePreviewBranch = create(TreePreviewBranch.class);

        // Preview
        this.seed = EditorSetting.TREE_VIEWER_FIRST_SEED;
        this.age = EditorSetting.TREE_VIEWER_AGE_DEFAULT;
    }

    @Override
    protected void get() {
        this.infoManager = get(InfoManager.class);
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

    // Selection \\

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

    public float getPreviewHeight() {
        return treePreviewBranch.getHeight();
    }

    public int getPreviewRevision() {
        return treePreviewBranch.getRevision();
    }
}
