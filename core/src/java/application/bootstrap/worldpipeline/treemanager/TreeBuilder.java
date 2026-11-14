package application.bootstrap.worldpipeline.treemanager;

import java.io.File;

import application.bootstrap.itempipeline.itemdefinitionmanager.ItemDefinitionManager;
import application.bootstrap.itempipeline.tooltypemanager.ToolTypeManager;
import application.bootstrap.shaderpipeline.texture.TextureHandle;
import application.bootstrap.shaderpipeline.texturemanager.TextureManager;
import application.bootstrap.worldpipeline.tree.TreeData;
import application.bootstrap.worldpipeline.tree.TreeHandle;
import engine.root.BuilderPackage;
import engine.root.UtilityPackage.InternalException;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;

class TreeBuilder extends BuilderPackage {

    /*
     * Parses a tree species ARPG into a TreeData through TreeArpgUtility and
     * wraps it in a TreeHandle. Every texture, tool type and item the species
     * names is resolved here, once, so a malformed or dangling species fails
     * at boot rather than when its first tree grows.
     */

    // Internal
    private TreeManager treeManager;
    private TextureManager textureManager;
    private ToolTypeManager toolTypeManager;
    private ItemDefinitionManager itemDefinitionManager;

    // Base \\

    @Override
    protected void get() {
        this.treeManager = get(TreeManager.class);
        this.textureManager = get(TextureManager.class);
        this.toolTypeManager = get(ToolTypeManager.class);
        this.itemDefinitionManager = get(ItemDefinitionManager.class);
    }

    // Build \\

    TreeHandle build(File file, String treeName) {

        short treeID = treeManager.registerTreeName(treeName);
        ArpgObjectStruct arpg = ArpgUtility.loadObject(file);
        TreeData treeData;

        try {
            treeData = TreeArpgUtility.parse(
                    treeName, treeID, arpg,
                    this::resolveTextureCorner,
                    this::resolveTextureColor,
                    toolTypeManager::getToolTypeIDFromToolTypeName,
                    itemDefinitionManager::getItemIDFromItemName);
        } catch (InternalException e) {
            return throwException(e.getMessage(), e);
        }

        TreeHandle treeHandle = create(TreeHandle.class);
        treeHandle.constructor(treeData);

        return treeHandle;
    }

    // The corner of a texture's tile in its atlas
    private float[] resolveTextureCorner(String textureName) {

        TextureHandle textureHandle = textureManager.getTextureHandleFromTextureName(textureName);

        return new float[] { textureHandle.getU0(), textureHandle.getV0() };
    }

    private int resolveTextureColor(String textureName) {
        return textureManager.getTextureHandleFromTextureName(textureName).getAverageColor();
    }
}
