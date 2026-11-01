package application.runtime.shipwright;

import java.nio.ByteBuffer;

import application.bootstrap.geometrypipeline.mesh.MeshData;
import application.bootstrap.geometrypipeline.mesh.MeshHandle;
import application.bootstrap.geometrypipeline.meshmanager.MeshManager;
import application.bootstrap.geometrypipeline.model.ModelInstance;
import application.bootstrap.geometrypipeline.modelmanager.ModelManager;
import application.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle;
import application.bootstrap.itempipeline.itemmodelmanager.ItemModelManager;
import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.renderpipeline.fbo.FBOInstance;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.bootstrap.renderpipeline.render.MaskStruct;
import application.bootstrap.renderpipeline.rendermanager.FBORenderSystem;
import application.bootstrap.renderpipeline.rendermanager.RenderManager;
import application.bootstrap.shaderpipeline.material.MaterialInstance;
import application.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import application.bootstrap.shaderpipeline.texturemanager.TextureManager;
import application.bootstrap.shipwrightpipeline.shipwright.ShipPreviewStruct;
import application.bootstrap.shipwrightpipeline.shipwright.ShipwrightSessionInstance;
import application.bootstrap.shipwrightpipeline.shipwrightmanager.ShipwrightManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import application.runtime.inventory.InventoryViewUtility;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.matrices.Matrix4;
import engine.util.memory.BufferUtility;
import engine.util.mathematics.vectors.Vector2;
import engine.util.mathematics.vectors.Vector3;
import engine.util.mathematics.vectors.Vector4;

public class ShipwrightRenderSystem extends SystemPackage {

    /*
     * Draws the drafting table in this window under its menus: the wooden
     * table across the whole window, the sheet in its area ruled in octants,
     * heavier every block, with the ink ShipwrightDraftBranch draws at
     * SHIPWRIGHT_INK_SCALE texels to the octant, the
     * cursor and — while a deck is drawn — its mirror across the centre
     * line, and the side and top views each on a card fitted into its area.
     * Over the menus, each catalogue tile's picture is drawn in its icon area
     * turned to the three-quarter view the inventory's slots show, fitted by
     * the bounds of its own model — an item, or a fitting built as a ship
     * builds it, drawn with the vehicle icon material — and clipped to
     * where the catalogue's list shows, and hidden while a dialog covers the
     * table. The sheet is drawn and uploaded whenever the session changes,
     * the views whenever it compiles. Textures and the upload buffer are made the first
     * time a table opens in this window, so a window that never uses one
     * holds nothing for it.
     */

    // Internal
    private ShipwrightSystem shipwrightSystem;
    private ShipwrightCatalogueSystem shipwrightCatalogueSystem;
    private ShipwrightDialogSystem shipwrightDialogSystem;
    private ItemModelManager itemModelManager;
    private ShipwrightManager shipwrightManager;
    private TextureManager textureManager;
    private MeshManager meshManager;
    private ModelManager modelManager;
    private MaterialManager materialManager;
    private RenderManager renderManager;
    private FBOManager fboManager;
    private FBORenderSystem fboRenderSystem;

    // Render Target
    private FBOInstance fbo;
    private FBOInstance iconFbo;
    private int itemMaterialID;
    private int vehicleMaterialID;

    // Models
    private ModelInstance tableModel;
    private ModelInstance sheetModel;
    private ModelInstance sideModel;
    private ModelInstance topModel;

    // Textures
    private int sheetTexture;
    private int sideTexture;
    private int topTexture;
    private ByteBuffer uploadBuffer;

    // Revisions — what was last drawn and uploaded
    private ShipwrightSessionInstance drawnSession;
    private int drawnRevision;
    private ShipwrightSessionInstance viewSession;
    private int viewRevision;

    // Scratch
    private Vector4 viewRect;
    private Vector4 inkRect;
    private Vector4 contentRect;
    private Vector4 cursorRect;
    private Vector2 cells;
    private Vector2 mirror;
    private Matrix4 iconProjection;
    private Matrix4 iconTransform;
    private MaskStruct iconMask;

    // Base \\

    @Override
    protected void create() {

        // Textures
        this.sheetTexture = EngineSetting.GL_HANDLE_NONE;

        // Scratch
        this.viewRect = new Vector4();
        this.inkRect = new Vector4();
        this.contentRect = new Vector4();
        this.cursorRect = new Vector4();
        this.cells = new Vector2();
        this.mirror = new Vector2();
        this.iconProjection = new Matrix4();
        this.iconTransform = new Matrix4();
        this.iconMask = new MaskStruct();
    }

    @Override
    protected void get() {
        this.shipwrightSystem = get(ShipwrightSystem.class);
        this.shipwrightCatalogueSystem = get(ShipwrightCatalogueSystem.class);
        this.shipwrightDialogSystem = get(ShipwrightDialogSystem.class);
        this.itemModelManager = get(ItemModelManager.class);
        this.shipwrightManager = get(ShipwrightManager.class);
        this.textureManager = get(TextureManager.class);
        this.meshManager = get(MeshManager.class);
        this.modelManager = get(ModelManager.class);
        this.materialManager = get(MaterialManager.class);
        this.renderManager = get(RenderManager.class);
        this.fboManager = get(FBOManager.class);
        this.fboRenderSystem = get(FBORenderSystem.class);
    }

    @Override
    protected void dispose() {

        if (sheetTexture == EngineSetting.GL_HANDLE_NONE)
            return;

        textureManager.deleteTexture2D(sheetTexture);
        textureManager.deleteTexture2D(sideTexture);
        textureManager.deleteTexture2D(topTexture);
    }

    // Resources made the first time a table opens in this window
    private void ensureResources() {

        if (sheetTexture != EngineSetting.GL_HANDLE_NONE)
            return;

        WindowInstance window = context.getWindow();
        MeshHandle quad = meshManager.getMeshHandleFromMeshName(EngineSetting.SHIPWRIGHT_MESH_QUAD);

        this.fbo = fboManager.cloneFbo(EngineSetting.SHIPWRIGHT_FBO, window);
        this.iconFbo = fboManager.cloneFbo(RuntimeSetting.FBO_INVENTORY, window);
        this.itemMaterialID = materialManager.getMaterialIDFromMaterialName(RuntimeSetting.MATERIAL_INVENTORY_ITEM);
        this.vehicleMaterialID = materialManager.getMaterialIDFromMaterialName(EngineSetting.VEHICLE_MATERIAL_ICON);
        this.sheetTexture = createTexture(EngineSetting.SHIPWRIGHT_SHEET_TEXELS_X,
                EngineSetting.SHIPWRIGHT_SHEET_TEXELS_Y);
        this.sideTexture = createTexture(EngineSetting.SHIPWRIGHT_SIDE_TEXELS_X,
                EngineSetting.SHIPWRIGHT_SIDE_TEXELS_Y);
        this.topTexture = createTexture(EngineSetting.SHIPWRIGHT_TOP_TEXELS_X,
                EngineSetting.SHIPWRIGHT_TOP_TEXELS_Y);
        this.uploadBuffer = BufferUtility.newByteBuffer(Math.max(
                EngineSetting.SHIPWRIGHT_SHEET_TEXELS_X * EngineSetting.SHIPWRIGHT_SHEET_TEXELS_Y,
                Math.max(EngineSetting.SHIPWRIGHT_SIDE_TEXELS_X * EngineSetting.SHIPWRIGHT_SIDE_TEXELS_Y,
                        EngineSetting.SHIPWRIGHT_TOP_TEXELS_X * EngineSetting.SHIPWRIGHT_TOP_TEXELS_Y))
                * EngineSetting.COLOR_CHANNEL_COUNT);

        this.tableModel = createModel(quad, EngineSetting.SHIPWRIGHT_MODE_TABLE, sheetTexture);
        this.sheetModel = createModel(quad, EngineSetting.SHIPWRIGHT_MODE_SHEET, sheetTexture);
        this.sideModel = createModel(quad, EngineSetting.SHIPWRIGHT_MODE_CARD, sideTexture);
        this.topModel = createModel(quad, EngineSetting.SHIPWRIGHT_MODE_CARD, topTexture);
    }

    private int createTexture(int width, int height) {
        return textureManager.createTexture2D(width, height, EngineSetting.GL_CLAMP_TO_EDGE, EngineSetting.GL_NEAREST);
    }

    private ModelInstance createModel(MeshHandle quad, float mode, int texture) {

        ModelInstance model = modelManager.createModel(
                quad, materialManager.cloneMaterial(EngineSetting.SHIPWRIGHT_MATERIAL_PAPER));
        MaterialInstance material = model.getMaterial();

        material.setUniform(EngineSetting.SHIPWRIGHT_UNIFORM_MODE, mode);
        material.setUniform(EngineSetting.SHIPWRIGHT_UNIFORM_INK_TEXTURE, texture);

        return model;
    }

    // Render \\

    @Override
    protected void render() {

        if (!shipwrightSystem.isOpen())
            return;

        WindowInstance window = context.getWindow();

        if (window.getWidth() <= 0 || window.getHeight() <= 0)
            return;

        ensureResources();

        ShipwrightSessionInstance session = shipwrightSystem.getSession();

        uploadSheet(session);
        uploadViews(session);
        renderManager.ensureFboRendered(fbo, window);

        pushTable(window);
        pushSheet(session, window);
        pushCard(sideModel, EngineSetting.SHIPWRIGHT_ENTRY_SIDE, session.getSideWidth(), session.getSideHeight(),
                EngineSetting.SHIPWRIGHT_SIDE_TEXELS_X, EngineSetting.SHIPWRIGHT_SIDE_TEXELS_Y, window);
        pushCard(topModel, EngineSetting.SHIPWRIGHT_ENTRY_TOP, session.getTopWidth(), session.getTopHeight(),
                EngineSetting.SHIPWRIGHT_TOP_TEXELS_X, EngineSetting.SHIPWRIGHT_TOP_TEXELS_Y, window);

        fboRenderSystem.pushFbo(fbo, EngineSetting.SHIPWRIGHT_LAYER, window);

        if (!shipwrightDialogSystem.isOpen())
            pushIcons(window);
    }

    // Upload \\

    private void uploadSheet(ShipwrightSessionInstance session) {

        if (session == drawnSession && session.getRevision() == drawnRevision)
            return;

        this.drawnSession = session;
        this.drawnRevision = session.getRevision();

        shipwrightManager.drawSheet(session);
        upload(sheetTexture, session.getSheetPixels(), session.getSheetWidth() * EngineSetting.SHIPWRIGHT_INK_SCALE,
                session.getSheetHeight() * EngineSetting.SHIPWRIGHT_INK_SCALE);
    }

    private void uploadViews(ShipwrightSessionInstance session) {

        if (session == viewSession && session.getViewRevision() == viewRevision)
            return;

        this.viewSession = session;
        this.viewRevision = session.getViewRevision();

        upload(sideTexture, session.getSidePixels(), session.getSideWidth(), session.getSideHeight());
        upload(topTexture, session.getTopPixels(), session.getTopWidth(), session.getTopHeight());
    }

    private void upload(int texture, byte[] pixels, int width, int height) {

        if (width <= 0 || height <= 0)
            return;

        uploadBuffer.clear();
        uploadBuffer.put(pixels, 0, width * height * EngineSetting.COLOR_CHANNEL_COUNT);
        textureManager.updateTexture2D(texture, 0, 0, width, height, uploadBuffer);
    }

    // Draw \\

    private void pushTable(WindowInstance window) {

        viewRect.set(-1f, -1f, 1f, 1f);
        tableModel.getMaterial().setUniform(EngineSetting.SHIPWRIGHT_UNIFORM_VIEW_RECT, viewRect);
        renderManager.pushRenderCall(tableModel, fbo, EngineSetting.SHIPWRIGHT_DEPTH_TABLE, window);
    }

    // The sheet's paper across its area, its octants ruled and inked in the fitted grid, the cursor over them
    private void pushSheet(ShipwrightSessionInstance session, WindowInstance window) {

        ElementInstance area = shipwrightSystem.getTableMenu().getEntryPoint(EngineSetting.SHIPWRIGHT_ENTRY_SHEET);
        float areaWidth = Math.max(1f, area.getComputedW());
        float areaHeight = Math.max(1f, area.getComputedH());
        MaterialInstance material = sheetModel.getMaterial();

        setViewRect(area.getComputedLeft(), area.getComputedTop(), areaWidth, areaHeight, window);
        contentRect.set(
                (shipwrightSystem.getSheetLeft() - area.getComputedLeft()) / areaWidth,
                (shipwrightSystem.getSheetBottom() - area.getComputedTop()) / areaHeight,
                (shipwrightSystem.getSheetLeft() + shipwrightSystem.getSheetWidth() - area.getComputedLeft())
                        / areaWidth,
                (shipwrightSystem.getSheetBottom() + shipwrightSystem.getSheetHeight() - area.getComputedTop())
                        / areaHeight);
        inkRect.set(0f, 0f,
                (float) session.getSheetWidth() * EngineSetting.SHIPWRIGHT_INK_SCALE
                        / EngineSetting.SHIPWRIGHT_SHEET_TEXELS_X,
                (float) session.getSheetHeight() * EngineSetting.SHIPWRIGHT_INK_SCALE
                        / EngineSetting.SHIPWRIGHT_SHEET_TEXELS_Y);
        cells.set(session.getSheetWidth(), session.getSheetHeight());

        cursorRect.set(session.getCursorU(), session.getCursorV(),
                session.getCursorU() + 1f, session.getCursorV() + 1f);
        mirror.set(session.getTool().isMirrored() ? 1f : 0f, session.getSheetHeight() * 0.5f);

        material.setUniform(EngineSetting.SHIPWRIGHT_UNIFORM_VIEW_RECT, viewRect);
        material.setUniform(EngineSetting.SHIPWRIGHT_UNIFORM_CONTENT_RECT, contentRect);
        material.setUniform(EngineSetting.SHIPWRIGHT_UNIFORM_INK_RECT, inkRect);
        material.setUniform(EngineSetting.SHIPWRIGHT_UNIFORM_CELLS, cells);
        material.setUniform(EngineSetting.SHIPWRIGHT_UNIFORM_CURSOR, cursorRect);
        material.setUniform(EngineSetting.SHIPWRIGHT_UNIFORM_MIRROR, mirror);
        renderManager.pushRenderCall(sheetModel, fbo, EngineSetting.SHIPWRIGHT_DEPTH_PAPER, window);
    }

    // A view on a card across its area, fitted inside the card's margin at its own proportions
    private void pushCard(
            ModelInstance model,
            int entryPoint,
            int width,
            int height,
            int texelsX,
            int texelsY,
            WindowInstance window) {

        ElementInstance area = shipwrightSystem.getTableMenu().getEntryPoint(entryPoint);
        float areaWidth = Math.max(1f, area.getComputedW());
        float areaHeight = Math.max(1f, area.getComputedH());
        float margin = EngineSetting.SHIPWRIGHT_CARD_MARGIN;
        float room = 1f - margin * 2f;
        float scale = Math.min(room * areaWidth / Math.max(1, width), room * areaHeight / Math.max(1, height));
        float contentWidth = width * scale / areaWidth;
        float contentHeight = height * scale / areaHeight;
        MaterialInstance material = model.getMaterial();

        setViewRect(area.getComputedLeft(), area.getComputedTop(), areaWidth, areaHeight, window);
        contentRect.set(
                (1f - contentWidth) * 0.5f,
                (1f - contentHeight) * 0.5f,
                (1f + contentWidth) * 0.5f,
                (1f + contentHeight) * 0.5f);
        inkRect.set(0f, 0f, (float) width / texelsX, (float) height / texelsY);

        material.setUniform(EngineSetting.SHIPWRIGHT_UNIFORM_VIEW_RECT, viewRect);
        material.setUniform(EngineSetting.SHIPWRIGHT_UNIFORM_CONTENT_RECT, contentRect);
        material.setUniform(EngineSetting.SHIPWRIGHT_UNIFORM_INK_RECT, inkRect);
        renderManager.pushRenderCall(model, fbo, EngineSetting.SHIPWRIGHT_DEPTH_PAPER, window);
    }

    // Icons \\

    // Every catalogue tile's picture in its icon area, clipped to the list, over the menus
    private void pushIcons(WindowInstance window) {

        ElementInstance list = shipwrightCatalogueSystem.getListElement();

        if (list == null || shipwrightCatalogueSystem.getTileCount() == 0
                || list.getComputedW() <= 0f || list.getComputedH() <= 0f)
            return;

        iconMask.set((int) list.getComputedLeft(), (int) list.getComputedTop(),
                (int) list.getComputedW(), (int) list.getComputedH());
        InventoryViewUtility.composeProjection(window.getWidth(), window.getHeight(), iconProjection);
        renderManager.ensureFboRendered(iconFbo, window);

        for (int i = 0; i < shipwrightCatalogueSystem.getTileCount(); i++) {

            ElementInstance icon = shipwrightCatalogueSystem.getIconElement(i);

            if (icon == null || !isIconVisible(icon))
                continue;

            ItemDefinitionHandle item = shipwrightCatalogueSystem.getTileItem(i);
            ShipPreviewStruct preview = shipwrightCatalogueSystem.getTilePreview(i);

            if (item != null)
                pushItemIcon(item, icon, window);
            else if (preview != null)
                pushPreviewIcon(preview, icon, window);
        }

        fboRenderSystem.pushFbo(iconFbo, RuntimeSetting.LAYER_INVENTORY, window);
    }

    // An item fitted to its icon area by the bounds of its own model, so a piece of any size fills its tile
    private void pushItemIcon(ItemDefinitionHandle item, ElementInstance icon, WindowInstance window) {

        MeshData meshData = item.getMeshHandle().getMeshData();
        Vector3 boundsMin = meshData.getBoundsMin();
        Vector3 boundsMax = meshData.getBoundsMax();
        float resolution = EngineSetting.SUB_VOXEL_RESOLUTION;

        composeIcon(icon,
                Math.round(boundsMin.x * resolution),
                Math.round(boundsMin.y * resolution),
                Math.round(boundsMin.z * resolution),
                Math.max(1, Math.round((boundsMax.x - boundsMin.x) * resolution)),
                Math.max(1, Math.round((boundsMax.y - boundsMin.y) * resolution)),
                Math.max(1, Math.round((boundsMax.z - boundsMin.z) * resolution)));
        pushIconMesh(meshData, itemMaterialID, window);
    }

    // A fitting built as a ship builds it, every mesh of it at rest, fitted to its icon area
    private void pushPreviewIcon(ShipPreviewStruct preview, ElementInstance icon, WindowInstance window) {

        composeIcon(icon, preview.getMinX(), preview.getMinY(), preview.getMinZ(),
                preview.getSizeX(), preview.getSizeY(), preview.getSizeZ());

        for (int i = 0; i < preview.getMeshes().size(); i++)
            pushIconMesh(preview.getMeshes().get(i).getMeshData(), vehicleMaterialID, window);
    }

    // A sub-voxel box turned to the inventory's three-quarter view and fitted inside an icon area, drawn larger than
    // an inventory slot draws it, as a tile has no grid to keep clear of
    private void composeIcon(ElementInstance icon, int minX, int minY, int minZ, int sizeX, int sizeY, int sizeZ) {
        InventoryViewUtility.composeIconMatrix(
                icon.getComputedLeft() + icon.getComputedW() * 0.5f,
                icon.getComputedTop() + icon.getComputedH() * 0.5f,
                Math.min(icon.getComputedW(), icon.getComputedH()) * EngineSetting.SHIPWRIGHT_ICON_ZOOM,
                minX, minY, minZ, sizeX, sizeY, sizeZ,
                iconTransform);
    }

    private void pushIconMesh(MeshData meshData, int materialID, WindowInstance window) {

        ModelInstance model = itemModelManager.acquireModel(meshData, materialID);
        MaterialInstance material = model.getMaterial();

        material.setUniform(RuntimeSetting.UNIFORM_INVENTORY_PROJECTION, iconProjection);
        material.setUniform(RuntimeSetting.UNIFORM_INVENTORY_MODEL, iconTransform);
        material.setUniform(RuntimeSetting.UNIFORM_INVENTORY_TINT, RuntimeSetting.INVENTORY_TINT_NONE);
        renderManager.pushRenderCall(model, iconFbo, EngineSetting.SHIPWRIGHT_DEPTH_ICON, iconMask, window);
    }

    // Whether any of an icon area lies inside the list it scrolls in
    private boolean isIconVisible(ElementInstance icon) {
        return icon.getComputedW() > 0f
                && icon.getComputedH() > 0f
                && icon.getComputedLeft() < iconMask.getX() + iconMask.getW()
                && icon.getComputedLeft() + icon.getComputedW() > iconMask.getX()
                && icon.getComputedTop() < iconMask.getY() + iconMask.getH()
                && icon.getComputedTop() + icon.getComputedH() > iconMask.getY();
    }

    // A window rectangle as (left, bottom, right, top) in normalized device coordinates
    private void setViewRect(float left, float bottom, float width, float height, WindowInstance window) {

        float windowWidth = window.getWidth();
        float windowHeight = window.getHeight();

        viewRect.set(
                left / windowWidth * 2f - 1f,
                bottom / windowHeight * 2f - 1f,
                (left + width) / windowWidth * 2f - 1f,
                (bottom + height) / windowHeight * 2f - 1f);
    }
}
