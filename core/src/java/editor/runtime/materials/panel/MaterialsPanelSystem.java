package editor.runtime.materials.panel;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import editor.bootstrap.materialspipeline.materials.MaterialDocumentStruct;
import editor.bootstrap.materialspipeline.materialsmanager.MaterialsManager;
import editor.runtime.EditorSetting;
import editor.runtime.characterview.util.CharacterViewUtility;
import editor.runtime.materials.MaterialsSetting;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class MaterialsPanelSystem extends SystemPackage {

    /*
     * Binds this window to a UI render target and shows the materials. When
     * the materials' revision moves it refills the list — the workspace
     * material open, then a header for every pack and a row for each of its
     * materials by name, the chosen one marked, each picked up to drag it
     * onto an item's part — and the chosen material's details and how it is
     * laid. The status line says what the last file action did, else names
     * the material chosen, or what letting go will do while one is carried.
     */

    // Internal
    private MenuManager menuManager;
    private FBOManager fboManager;
    private MaterialsManager materialsManager;

    // Menus
    private MenuInstance materialsMenu;
    private ElementInstance statusLabel;
    private ElementInstance listTitle;

    // State
    private int shownRevision;
    private boolean shownDragging;

    // Base \\

    @Override
    protected void create() {

        // State
        this.shownRevision = EngineSetting.INDEX_NOT_FOUND;
    }

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.fboManager = get(FBOManager.class);
        this.materialsManager = get(MaterialsManager.class);
    }

    @Override
    protected void awake() {

        WindowInstance window = context.getWindow();

        menuManager.setMenuTargetFbo(window, fboManager.cloneFbo(RuntimeSetting.FBO_UI, window));
        this.materialsMenu = menuManager.openMenu(MaterialsSetting.MENU_MATERIALS, window);

        this.statusLabel = requireEntryPoint(MaterialsSetting.ENTRY_STATUS);
        this.listTitle = requireEntryPoint(MaterialsSetting.ENTRY_LIST_TITLE);

        requireEntryPoint(MaterialsSetting.ENTRY_MATERIALS);
        requireEntryPoint(MaterialsSetting.ENTRY_DETAILS);
    }

    private ElementInstance requireEntryPoint(int entryPoint) {

        ElementInstance element = materialsMenu.getEntryPoint(entryPoint);

        if (element == null)
            throwException("Materials menu '" + MaterialsSetting.MENU_MATERIALS + "' has no entry point "
                    + entryPoint + ".");

        return element;
    }

    // Update \\

    @Override
    protected void update() {

        if (materialsManager.getRevision() != shownRevision) {
            this.shownRevision = materialsManager.getRevision();
            refreshMaterials();
            refreshDetails();
            refreshStatus();
        }

        if (materialsManager.isDragging() != shownDragging) {
            this.shownDragging = materialsManager.isDragging();
            refreshStatus();
        }
    }

    // Materials \\

    // A header for every pack, then a row for each of its materials, each picked up to drag it onto a part
    private void refreshMaterials() {

        menuManager.ejectAll(materialsMenu, MaterialsSetting.ENTRY_MATERIALS);

        ObjectArrayList<String> packNames = materialsManager.getPackNames();
        MaterialDocumentStruct document = materialsManager.getDocument();
        boolean documentChosen = materialsManager.getChosenDocument() != null;
        String selected = documentChosen ? "" : materialsManager.getSelectedMaterial();
        int count = 0;

        if (document != null) {
            injectHeader(MaterialsSetting.ENTRY_MATERIALS, MaterialsSetting.TEXT_WORKSPACE);
            injectDocument(document, documentChosen);
        }

        for (int p = 0; p < packNames.size(); p++) {

            ObjectArrayList<String> materialNames = materialsManager.getMaterialNames(packNames.get(p));

            if (materialNames.isEmpty())
                continue;

            injectHeader(MaterialsSetting.ENTRY_MATERIALS, packNames.get(p));

            for (int i = 0; i < materialNames.size(); i++)
                injectMaterial(materialNames.get(i), materialNames.get(i).equals(selected));

            count += materialNames.size();
        }

        listTitle.setFontText(String.format(MaterialsSetting.FORMAT_LIST_TITLE, count));

        if (count == 0)
            injectText(MaterialsSetting.ENTRY_MATERIALS, MaterialsSetting.TEXT_NO_MATERIALS);
    }

    private void injectMaterial(String materialName, boolean selected) {

        ElementInstance row = menuManager.inject(
                materialsMenu,
                MaterialsSetting.ENTRY_MATERIALS,
                selected ? MaterialsSetting.TEMPLATE_MATERIAL_ROW_SELECTED : MaterialsSetting.TEMPLATE_MATERIAL_ROW);

        row.setOnDragArgOverride(materialName);
        row.findChildById(MaterialsSetting.ELEMENT_ROW_LABEL).setFontText(
                CharacterViewUtility.toResourceLabel(materialName));
    }

    // The open material of the workspace, chosen by a press
    private void injectDocument(MaterialDocumentStruct document, boolean chosen) {

        ElementInstance row = menuManager.inject(
                materialsMenu,
                MaterialsSetting.ENTRY_MATERIALS,
                chosen ? MaterialsSetting.TEMPLATE_DOCUMENT_ROW_SELECTED : MaterialsSetting.TEMPLATE_DOCUMENT_ROW);

        row.findChildById(MaterialsSetting.ELEMENT_ROW_LABEL).setFontText(document.isDirty()
                ? document.getMaterialName() + EditorSetting.CHARACTER_VIEW_DIRTY_MARKER
                : document.getMaterialName());
    }

    // Details \\

    // The material chosen, by name and pack or by name and base, and how it is laid
    private void refreshDetails() {

        menuManager.ejectAll(materialsMenu, MaterialsSetting.ENTRY_DETAILS);

        MaterialDocumentStruct document = materialsManager.getChosenDocument();
        String materialName = materialsManager.getSelectedMaterial();
        int separator = materialName.lastIndexOf(EngineSetting.TEXTURE_TILE_NAME_SEPARATOR);

        if (document != null) {
            injectHeader(MaterialsSetting.ENTRY_DETAILS, document.getMaterialName());
            injectDetail(MaterialsSetting.TEXT_BASE, materialName);
            injectText(MaterialsSetting.ENTRY_DETAILS, MaterialsSetting.TEXT_PAINT_HINT);
            injectText(MaterialsSetting.ENTRY_DETAILS, MaterialsSetting.TEXT_DOCUMENT_HINT);
            return;
        }

        injectHeader(MaterialsSetting.ENTRY_DETAILS, CharacterViewUtility.toResourceLabel(materialName));
        injectDetail(MaterialsSetting.TEXT_NAME, materialName);

        if (separator > 0)
            injectDetail(MaterialsSetting.TEXT_PACK, materialName.substring(0, separator));

        injectText(MaterialsSetting.ENTRY_DETAILS, MaterialsSetting.TEXT_PICK_HINT);
        injectText(MaterialsSetting.ENTRY_DETAILS, MaterialsSetting.TEXT_PAINT_HINT);
        injectText(MaterialsSetting.ENTRY_DETAILS, MaterialsSetting.TEXT_DRAG_HINT);
        injectText(MaterialsSetting.ENTRY_DETAILS, MaterialsSetting.TEXT_HAIR_HINT);
    }

    private void injectDetail(String label, String value) {

        ElementInstance row = menuManager.inject(
                materialsMenu, MaterialsSetting.ENTRY_DETAILS, MaterialsSetting.TEMPLATE_DETAIL_ROW);

        row.findChildById(MaterialsSetting.ELEMENT_ROW_LABEL).setFontText(label);
        row.findChildById(MaterialsSetting.ELEMENT_ROW_VALUE).setFontText(value);
    }

    // Status \\

    private void refreshStatus() {

        if (materialsManager.isDragging())
            statusLabel.setFontText(MaterialsSetting.TEXT_DRAGGING);
        else if (!materialsManager.getStatusMessage().isEmpty())
            statusLabel.setFontText(materialsManager.getStatusMessage());
        else
            statusLabel.setFontText(String.format(
                    MaterialsSetting.FORMAT_STATUS,
                    CharacterViewUtility.toResourceLabel(materialsManager.getChosenName())));
    }

    // Utility \\

    private void injectHeader(int entryPoint, String text) {
        menuManager.inject(materialsMenu, entryPoint, MaterialsSetting.TEMPLATE_HEADER_ROW)
                .findChildById(MaterialsSetting.ELEMENT_ROW_LABEL).setFontText(text);
    }

    private void injectText(int entryPoint, String text) {
        menuManager.inject(materialsMenu, entryPoint, MaterialsSetting.TEMPLATE_TEXT_ROW)
                .findChildById(MaterialsSetting.ELEMENT_ROW_LABEL).setFontText(text);
    }
}
