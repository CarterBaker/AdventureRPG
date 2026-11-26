package editor.runtime.materials.panel;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.menupipeline.util.DimensionValueStruct;
import application.bootstrap.menupipeline.util.DimensionVector2Struct;
import application.bootstrap.menupipeline.util.StripLayoutUtility;
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
     * Binds this window to a UI render target and shows the materials as a
     * material library's grid. When the materials' revision moves, or the
     * tab's width changes how many tiles fit a row, it refills the list — the
     * workspace material open, then a header for every pack and under it a
     * grid of its materials, each a tile showing what the material looks like
     * (its albedo) over its name, as many to a row as the width holds
     * (StripLayoutUtility), the previews made a few a frame until every tile
     * has its own, the chosen one marked, each picked up to drag it onto an
     * item's part — and the chosen material's details and how it is laid. The status line says what the last file action did, else names
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

    // Grid — how many tiles a row held when laid out, the row being filled, its size and each tile's
    private int shownPerRow;
    private ElementInstance gridRow;
    private int gridCount;
    private DimensionVector2Struct gridRowSize;
    private DimensionVector2Struct tileSize;

    // Thumbnails — the tiles still waiting on theirs, by the material each shows, made a few a frame
    private ObjectArrayList<ElementInstance> pendingRows;
    private ObjectArrayList<String> pendingNames;

    // Base \\

    @Override
    protected void create() {

        // State
        this.shownRevision = EngineSetting.INDEX_NOT_FOUND;

        // Grid
        this.shownPerRow = EngineSetting.INDEX_NOT_FOUND;

        // Thumbnails
        this.pendingRows = new ObjectArrayList<>();
        this.pendingNames = new ObjectArrayList<>();
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
        } else if (resolvePerRow() != shownPerRow) {
            refreshMaterials();
        }

        if (materialsManager.isDragging() != shownDragging) {
            this.shownDragging = materialsManager.isDragging();
            refreshStatus();
        }

        fillThumbnails();
    }

    // The tiles still waiting on a preview given theirs, a few a frame from the end of the list
    private void fillThumbnails() {

        for (int i = 0; i < EditorSetting.MATERIALS_THUMBNAILS_PER_FRAME && !pendingRows.isEmpty(); i++) {

            int last = pendingRows.size() - 1;

            setThumbnail(pendingRows.remove(last), materialsManager.resolveThumbnail(pendingNames.remove(last)));
        }
    }

    private void setThumbnail(ElementInstance row, int texture) {

        ElementInstance thumbnail = row.findChildById(MaterialsSetting.ELEMENT_ROW_THUMBNAIL);

        if (thumbnail != null && thumbnail.hasSprite())
            thumbnail.getSpriteInstance().setTexture(texture);
    }

    // Materials \\

    // A header for every pack, then a grid of its materials, each picked up to drag it onto a part
    private void refreshMaterials() {

        menuManager.ejectAll(materialsMenu, MaterialsSetting.ENTRY_MATERIALS);
        pendingRows.clear();
        pendingNames.clear();
        sizeGrid(resolvePerRow());

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
            this.gridRow = null;

            for (int i = 0; i < materialNames.size(); i++)
                injectMaterial(materialNames.get(i), materialNames.get(i).equals(selected));

            count += materialNames.size();
        }

        listTitle.setFontText(String.format(MaterialsSetting.FORMAT_LIST_TITLE, count));

        if (count == 0)
            injectText(MaterialsSetting.ENTRY_MATERIALS, MaterialsSetting.TEXT_NO_MATERIALS);
    }

    // A material's tile, in a new grid row once the one being filled holds as many as fit
    private void injectMaterial(String materialName, boolean selected) {

        if (gridRow == null || gridCount == shownPerRow) {
            this.gridRow = menuManager.inject(
                    materialsMenu,
                    MaterialsSetting.ENTRY_MATERIALS,
                    MaterialsSetting.TEMPLATE_GRID_ROW,
                    row -> row.setSizeOverride(gridRowSize));
            this.gridCount = 0;
        }

        ElementInstance tile = menuManager.inject(
                gridRow,
                selected ? MaterialsSetting.TEMPLATE_MATERIAL_TILE_SELECTED : MaterialsSetting.TEMPLATE_MATERIAL_TILE,
                element -> {
                    element.setSizeOverride(tileSize);
                    element.setOnDragArgOverride(materialName);
                    element.findChildById(MaterialsSetting.ELEMENT_ROW_LABEL).setFontText(
                            CharacterViewUtility.toResourceLabel(materialName));
                });

        gridCount++;

        if (materialsManager.hasThumbnail(materialName)) {
            setThumbnail(tile, materialsManager.resolveThumbnail(materialName));
            return;
        }

        pendingRows.add(tile);
        pendingNames.add(materialName);
    }

    // Grid \\

    // How many tiles a row of the tab's width holds
    private int resolvePerRow() {

        ElementInstance list = materialsMenu.getEntryPoint(MaterialsSetting.ENTRY_MATERIALS);

        return StripLayoutUtility.resolvePerRow(
                list.getComputedW(),
                Integer.MAX_VALUE,
                MaterialsSetting.TILE_MIN_WIDTH_PIXELS,
                MaterialsSetting.TILE_SPACING_PIXELS);
    }

    // Every tile as wide as a row of that many shares, its preview square above its name
    private void sizeGrid(int perRow) {

        float width = StripLayoutUtility.resolveWidth(
                materialsMenu.getEntryPoint(MaterialsSetting.ENTRY_MATERIALS).getComputedW(),
                perRow,
                MaterialsSetting.TILE_MAX_WIDTH_PIXELS,
                MaterialsSetting.TILE_SPACING_PIXELS);
        float height = width + MaterialsSetting.TILE_LABEL_PIXELS;

        this.shownPerRow = perRow;
        this.gridRow = null;
        this.tileSize = new DimensionVector2Struct(
                DimensionValueStruct.ofAbsolute(width),
                DimensionValueStruct.ofAbsolute(height));
        this.gridRowSize = new DimensionVector2Struct(
                DimensionValueStruct.ofPercent(EngineSetting.PERCENT_MAX),
                DimensionValueStruct.ofAbsolute(height));
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
