package application.bootstrap.worldpipeline.treemanager;

import application.bootstrap.itempipeline.itemdefinitionmanager.ItemDefinitionManager;
import application.bootstrap.itempipeline.tooltypemanager.ToolTypeManager;
import application.bootstrap.shaderpipeline.texture.TextureHandle;
import application.bootstrap.shaderpipeline.texturemanager.TextureManager;
import application.bootstrap.worldpipeline.tree.TreeData;
import application.bootstrap.worldpipeline.tree.TreeHandle;
import application.bootstrap.worldpipeline.tree.TreeInstance;
import engine.root.BranchPackage;
import engine.root.UtilityPackage.InternalException;
import engine.util.arpg.ArpgObjectStruct;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class TreeSpeciesBranch extends BranchPackage {

    /*
     * Main thread — carries a live edit of a tree species into the world.
     * Once the boot loaders are released nothing can be loaded on demand, so
     * every texture, tool type and item the edited species names must already
     * be registered, and the species must already exist; the edit is parsed
     * and proven whole before anything changes, so a refused edit leaves the
     * species as it was. An accepted one replaces the species' data under its
     * own ID, and every tree of it standing in the loaded world grows again
     * from its seed and is redrawn. Every refusal is a catchable
     * InternalException.
     */

    // Internal
    private TreeManager treeManager;
    private TreeRebuildBranch treeRebuildBranch;
    private TextureManager textureManager;
    private ToolTypeManager toolTypeManager;
    private ItemDefinitionManager itemDefinitionManager;

    // Scratch
    private ObjectArrayList<TreeInstance> trees;

    // Base \\

    @Override
    protected void create() {
        this.trees = new ObjectArrayList<>();
    }

    @Override
    protected void get() {
        this.treeManager = get(TreeManager.class);
        this.treeRebuildBranch = get(TreeRebuildBranch.class);
        this.textureManager = get(TextureManager.class);
        this.toolTypeManager = get(ToolTypeManager.class);
        this.itemDefinitionManager = get(ItemDefinitionManager.class);
    }

    // Rebuild \\

    void rebuild(String treeName, ArpgObjectStruct treeArpg) {

        TreeHandle treeHandle = treeManager.findTreeHandle(treeName);

        if (treeHandle == null)
            throw fail(treeName, "is not a loaded tree — only a species the game started with can be edited live.");

        TreeData treeData = TreeArpgUtility.parse(
                treeName, treeHandle.getTreeID(), treeArpg,
                textureName -> resolveTextureCorner(treeName, textureName),
                textureName -> resolveTextureColor(treeName, textureName),
                toolTypeName -> resolveToolTypeID(treeName, toolTypeName),
                itemName -> resolveItemID(treeName, itemName));

        treeHandle.constructor(treeData);
        treeManager.updateMaxReach(treeHandle);
        regrowTrees(treeHandle);
    }

    // Every tree of the species standing in the loaded world grown again and redrawn
    private void regrowTrees(TreeHandle treeHandle) {

        treeManager.collectTrees(trees);

        for (int i = 0; i < trees.size(); i++) {

            TreeInstance tree = trees.get(i);

            if (tree.getTreeHandle() != treeHandle)
                continue;

            tree.regrowSpecies(treeManager.getCurrentDay());
            treeRebuildBranch.rebuildTree(tree, false);
        }

        trees.clear();
    }

    // Resolution \\

    private float[] resolveTextureCorner(String treeName, String textureName) {

        if (!textureManager.hasTexture(textureName))
            throw fail(treeName, "names unknown texture \"" + textureName + "\".");

        TextureHandle textureHandle = textureManager.getTextureHandleFromTextureName(textureName);

        return new float[] { textureHandle.getU0(), textureHandle.getV0() };
    }

    private int resolveTextureColor(String treeName, String textureName) {

        if (!textureManager.hasTexture(textureName))
            throw fail(treeName, "names unknown texture \"" + textureName + "\".");

        return textureManager.getTextureHandleFromTextureName(textureName).getAverageColor();
    }

    private int resolveToolTypeID(String treeName, String toolTypeName) {

        if (!toolTypeManager.hasToolType(toolTypeName))
            throw fail(treeName, "names unknown tool \"" + toolTypeName + "\".");

        return toolTypeManager.getToolTypeIDFromToolTypeName(toolTypeName);
    }

    private int resolveItemID(String treeName, String itemName) {

        if (!itemDefinitionManager.hasItem(itemName))
            throw fail(treeName, "names unknown item \"" + itemName + "\".");

        return itemDefinitionManager.getItemIDFromItemName(itemName);
    }

    private InternalException fail(String treeName, String message) {
        return new InternalException("Tree \"" + treeName + "\" " + message);
    }
}
