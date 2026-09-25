package editor.itemeditor.menu;

import application.bootstrap.geometrypipeline.subvoxel.SubVoxelModelStruct;
import application.bootstrap.geometrypipeline.subvoxel.SubVoxelPartStruct;
import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.renderpipeline.fbomanager.FboManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import editor.bootstrap.itemeditorpipeline.itemdocument.ItemDocumentInstance;
import editor.bootstrap.itemeditorpipeline.itemeditormanager.ItemEditorManager;
import editor.itemeditor.ItemEditorSetting;
import engine.editor.EditorSetting;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ItemEditorMenuSystem extends SystemPackage {

    /*
     * Opens the Item Editor toolbar and parts panel over the viewport. When the
     * editor's revision moves it rewrites the status line and lists the active
     * item's parts, one row per part showing its name and texture, with the
     * selected part highlighted and every row carrying its part index.
     */

    // Internal
    private MenuManager menuManager;
    private FboManager fboManager;
    private ItemEditorManager itemEditorManager;

    // Menus
    private MenuInstance toolbarMenu;

    // Parts
    private ObjectArrayList<ElementInstance> partElements;

    // Status
    private int shownRevision;

    // Base \\

    @Override
    protected void create() {

        // Parts
        this.partElements = new ObjectArrayList<>();

        // Status
        this.shownRevision = EngineSetting.INDEX_NOT_FOUND;
    }

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.fboManager = get(FboManager.class);
        this.itemEditorManager = get(ItemEditorManager.class);
    }

    @Override
    protected void awake() {

        WindowInstance window = context.getWindow();

        menuManager.setMenuTargetFbo(window, fboManager.cloneFbo(RuntimeSetting.FBO_UI, window));
        this.toolbarMenu = menuManager.openMenu(ItemEditorSetting.MENU_TOOLBAR, window);
    }

    @Override
    protected void dispose() {

        menuManager.closeMenu(toolbarMenu);
        menuManager.setMenuTargetFbo(context.getWindow(), null);
    }

    // Update \\

    @Override
    protected void update() {

        if (itemEditorManager.getRevision() == shownRevision)
            return;

        shownRevision = itemEditorManager.getRevision();

        ElementInstance statusLabel = toolbarMenu.getEntryPoint(ItemEditorSetting.ENTRY_STATUS);

        if (statusLabel != null)
            statusLabel.setFontText(itemEditorManager.getStatusText());

        refreshParts();
    }

    // Parts \\

    private void refreshParts() {

        for (int i = 0; i < partElements.size(); i++)
            menuManager.eject(toolbarMenu, ItemEditorSetting.ENTRY_PARTS, partElements.get(i));

        partElements.clear();

        ItemDocumentInstance document = itemEditorManager.getActiveDocument();

        if (document == null)
            return;

        SubVoxelModelStruct model = document.getModel();

        for (int partIndex = 0; partIndex < model.getPartCount(); partIndex++)
            partElements.add(injectPart(model.getPart(partIndex), partIndex, document.getSelectedPartIndex()));
    }

    private ElementInstance injectPart(SubVoxelPartStruct part, int partIndex, int selectedPartIndex) {

        String template = partIndex == selectedPartIndex
                ? ItemEditorSetting.TEMPLATE_PART_ROW_SELECTED
                : ItemEditorSetting.TEMPLATE_PART_ROW;
        String text = part.getPartName() + EditorSetting.ITEM_EDITOR_TEXTURE_SEPARATOR + part.getTextureName();

        return menuManager.inject(
                toolbarMenu, ItemEditorSetting.ENTRY_PARTS, template,
                element -> {
                    element.setActionArgOverride(Integer.toString(partIndex));

                    ElementInstance label = element.findChildById(ItemEditorSetting.ELEMENT_PART_LABEL);

                    if (label != null)
                        label.setFontText(text);
                });
    }
}
