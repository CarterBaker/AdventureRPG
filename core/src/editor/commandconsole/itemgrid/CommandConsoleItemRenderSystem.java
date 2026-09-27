package editor.commandconsole.itemgrid;

import application.bootstrap.geometrypipeline.model.ModelInstance;
import application.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle;
import application.bootstrap.itempipeline.itemmodelmanager.ItemModelManager;
import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.renderpipeline.fbo.FboInstance;
import application.bootstrap.renderpipeline.fbomanager.FboManager;
import application.bootstrap.renderpipeline.fborendersystem.FboRenderSystem;
import application.bootstrap.renderpipeline.rendermanager.RenderManager;
import application.bootstrap.renderpipeline.util.MaskStruct;
import application.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import application.runtime.inventory.InventoryViewUtility;
import editor.commandconsole.CommandConsoleSetting;
import editor.commandconsole.panel.CommandConsolePanelSystem;
import engine.root.SystemPackage;
import engine.util.mathematics.matrices.Matrix4;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class CommandConsoleItemRenderSystem extends SystemPackage {

    /*
     * Draws each item tile's icon into this window's own target, composited
     * over the command console's menu — the item turned to the same three
     * quarter view the inventory's slots show, through the same transforms
     * and item material. Icons are clipped to where their grid shows inside
     * the command tree, so a scrolled grid never draws past its edges, and
     * every draw takes a model of its own from ItemModelManager.
     */

    // Internal
    private ItemModelManager itemModelManager;
    private MaterialManager materialManager;
    private RenderManager renderManager;
    private FboManager fboManager;
    private FboRenderSystem fboRenderSystem;
    private CommandConsolePanelSystem commandConsolePanelSystem;
    private CommandConsoleItemGridSystem commandConsoleItemGridSystem;

    // Render Target
    private FboInstance iconFbo;

    // Resources
    private int itemMaterialID;

    // Scratch
    private Matrix4 projection;
    private Matrix4 transform;
    private MaskStruct mask;

    // Base \\

    @Override
    protected void create() {

        // Scratch
        this.projection = new Matrix4();
        this.transform = new Matrix4();
        this.mask = new MaskStruct();
    }

    @Override
    protected void get() {
        this.itemModelManager = get(ItemModelManager.class);
        this.materialManager = get(MaterialManager.class);
        this.renderManager = get(RenderManager.class);
        this.fboManager = get(FboManager.class);
        this.fboRenderSystem = get(FboRenderSystem.class);
        this.commandConsolePanelSystem = get(CommandConsolePanelSystem.class);
        this.commandConsoleItemGridSystem = get(CommandConsoleItemGridSystem.class);
    }

    @Override
    protected void awake() {

        // Render Target
        this.iconFbo = fboManager.cloneFbo(RuntimeSetting.FBO_INVENTORY, context.getWindow());

        // Resources
        this.itemMaterialID = materialManager.getMaterialIDFromMaterialName(RuntimeSetting.MATERIAL_INVENTORY_ITEM);
    }

    // Render \\

    @Override
    protected void render() {

        ObjectArrayList<CommandConsoleItemGridStruct> grids = commandConsoleItemGridSystem.getGrids();
        WindowInstance window = context.getWindow();

        if (grids.isEmpty())
            return;

        InventoryViewUtility.composeProjection(window.getWidth(), window.getHeight(), projection);
        renderManager.ensureFboRendered(iconFbo, window);

        ElementInstance tree = commandConsolePanelSystem.getCommandConsoleMenu()
                .getEntryPoint(CommandConsoleSetting.ENTRY_COMMAND_TREE);

        for (int i = 0; i < grids.size(); i++)
            if (clipTo(grids.get(i).getGridElement(), tree))
                pushGrid(grids.get(i), window);

        fboRenderSystem.pushFbo(iconFbo, RuntimeSetting.LAYER_INVENTORY, window);
    }

    private void pushGrid(CommandConsoleItemGridStruct grid, WindowInstance window) {

        for (int i = 0; i < grid.getTileCount(); i++) {

            ElementInstance icon = grid.getIconElement(i);

            if (!isVisible(icon))
                continue;

            pushIcon(
                    grid.getTileItem(i),
                    icon.getComputedLeft() + icon.getComputedW() * 0.5f,
                    icon.getComputedTop() + icon.getComputedH() * 0.5f,
                    Math.min(icon.getComputedW(), icon.getComputedH()),
                    window);
        }
    }

    // Draw \\

    private void pushIcon(ItemDefinitionHandle item, float centerX, float centerY, float size, WindowInstance window) {

        ModelInstance model = itemModelManager.acquireModel(item.getMeshHandle(), itemMaterialID);

        InventoryViewUtility.composeIconMatrix(centerX, centerY, size, item.getShape(), transform);
        model.getMaterial().setUniform(RuntimeSetting.UNIFORM_INVENTORY_PROJECTION, projection);
        model.getMaterial().setUniform(RuntimeSetting.UNIFORM_INVENTORY_MODEL, transform);
        model.getMaterial().setUniform(RuntimeSetting.UNIFORM_INVENTORY_TINT, RuntimeSetting.INVENTORY_TINT_NONE);

        renderManager.pushRenderCall(
                model, iconFbo, CommandConsoleSetting.DEPTH_ITEM_ICON, mask, window);
    }

    // Utility \\

    // Sets the mask to where the grid shows inside the tree, false when none of it does
    private boolean clipTo(ElementInstance grid, ElementInstance tree) {

        int left = (int) Math.max(grid.getComputedLeft(), tree.getComputedLeft());
        int top = (int) Math.max(grid.getComputedTop(), tree.getComputedTop());
        int right = (int) Math.min(
                grid.getComputedLeft() + grid.getComputedW(), tree.getComputedLeft() + tree.getComputedW());
        int bottom = (int) Math.min(
                grid.getComputedTop() + grid.getComputedH(), tree.getComputedTop() + tree.getComputedH());

        if (right <= left || bottom <= top)
            return false;

        mask.set(left, top, right - left, bottom - top);

        return true;
    }

    // Whether any of an icon lies inside the current mask
    private boolean isVisible(ElementInstance icon) {
        return icon.getComputedW() > 0f
                && icon.getComputedH() > 0f
                && icon.getComputedLeft() < mask.getX() + mask.getW()
                && icon.getComputedLeft() + icon.getComputedW() > mask.getX()
                && icon.getComputedTop() < mask.getY() + mask.getH()
                && icon.getComputedTop() + icon.getComputedH() > mask.getY();
    }
}
