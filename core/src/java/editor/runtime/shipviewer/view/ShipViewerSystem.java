package editor.runtime.shipviewer.view;

import application.bootstrap.furnishingpipeline.furnishing.FurnishingSlotStruct;
import application.bootstrap.geometrypipeline.mesh.MeshInstance;
import application.bootstrap.shipwrightpipeline.shipwright.ShipDraftStruct;
import application.bootstrap.shipwrightpipeline.shipwright.ShipLayoutStruct;
import application.bootstrap.shipwrightpipeline.shipwright.ShipwrightSessionInstance;
import application.bootstrap.shipwrightpipeline.shipwrightmanager.ShipwrightManager;
import application.bootstrap.vehiclepipeline.util.VehicleSpaceUtility;
import application.bootstrap.vehiclepipeline.vehicle.VehicleHandle;
import application.bootstrap.vehiclepipeline.vehicle.VehiclePartRole;
import application.bootstrap.vehiclepipeline.vehicle.VehiclePartStruct;
import application.bootstrap.vehiclepipeline.vehiclemanager.VehicleManager;
import editor.bootstrap.shipviewerpipeline.shipviewermanager.ShipViewerManager;
import editor.runtime.shipviewer.ShipViewerSetting;
import editor.runtime.viewcamera.ViewCameraSubject;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ShipViewerSystem extends SystemPackage implements ViewCameraSubject {

    /*
     * Finds the ship the Ship Viewer shows and lays out what is drawn of it:
     * a vehicle in the palette, looked up again whenever the choice or the
     * palette changes, as a ship saved again replaces its type and frees the
     * old one's meshes; or the ship on the Shipwright's table, built afresh
     * into a draft of the viewer's own whenever its sheet rests, so every
     * change shows without saving. Either way it keeps the meshes drawn at
     * rest, every part but the anchor cables, the cargo standing in the ship,
     * its bounds and how many decks it has; the
     * draft's meshes are freed once a vehicle is chosen instead. The
     * deck shown is kept here too: every deck, or one and those under it,
     * the rest cut away. Nothing is looked up while the choice rests.
     */

    // Internal
    private VehicleManager vehicleManager;
    private ShipViewerManager shipViewerManager;
    private ShipwrightManager shipwrightManager;

    // Draft — the table's ship as last built for this viewer
    private ShipDraftStruct draft;
    private int draftSurveyRevision;

    // Shown
    private ObjectArrayList<MeshInstance> meshes;
    private ObjectArrayList<FurnishingSlotStruct> cargo;
    private String displayName;
    private int partCount;
    private boolean shown;
    private int minX;
    private int minY;
    private int minZ;
    private int maxX;
    private int maxY;
    private int maxZ;
    private int deckCount;
    private int chosenRevision;
    private int paletteRevision;
    private int revision;

    // Cutaway — the deck shown with every deck under it, DECK_ALL for every deck
    private int deck;

    // Base \\

    @Override
    protected void create() {

        // Draft
        this.draft = new ShipDraftStruct();
        this.draftSurveyRevision = EngineSetting.INDEX_NOT_FOUND;

        // Shown
        this.meshes = new ObjectArrayList<>();
        this.cargo = new ObjectArrayList<>();
        this.chosenRevision = EngineSetting.INDEX_NOT_FOUND;
        this.paletteRevision = EngineSetting.INDEX_NOT_FOUND;

        // Cutaway
        this.deck = ShipViewerSetting.DECK_ALL;
    }

    @Override
    protected void get() {
        this.vehicleManager = get(VehicleManager.class);
        this.shipViewerManager = get(ShipViewerManager.class);
        this.shipwrightManager = get(ShipwrightManager.class);
    }

    // Update \\

    @Override
    protected void update() {

        ShipwrightSessionInstance session = shipViewerManager.getSession();

        if (session != null) {
            updateDraft(session);
            return;
        }

        int chosen = shipViewerManager.getRevision();
        int palette = vehicleManager.getPaletteRevision();

        if (chosen == chosenRevision && palette == paletteRevision)
            return;

        this.chosenRevision = chosen;
        this.paletteRevision = palette;
        this.draftSurveyRevision = EngineSetting.INDEX_NOT_FOUND;

        shipwrightManager.releaseDraft(draft);

        String vehicleName = shipViewerManager.getVehicleName();

        if (vehicleName != null && vehicleManager.hasVehicle(vehicleName))
            showVehicle(vehicleManager.getVehicleHandleFromVehicleName(vehicleName));
        else
            clearShown();

        this.revision++;
    }

    // The table's ship built again once its sheet has rested since it was last built, or the table newly chosen
    private void updateDraft(ShipwrightSessionInstance session) {

        int chosen = shipViewerManager.getRevision();

        if (chosen == chosenRevision && session.getSurveyRevision() == draftSurveyRevision)
            return;

        this.chosenRevision = chosen;
        this.paletteRevision = EngineSetting.INDEX_NOT_FOUND;
        this.draftSurveyRevision = session.getSurveyRevision();

        shipwrightManager.buildDraft(session, draft);
        showDraft();
        this.revision++;
    }

    // Shown \\

    // A vehicle in the palette shown: its hull meshes, every moving part shown under way and the cargo it stows
    private void showVehicle(VehicleHandle vehicleHandle) {

        clearShown();
        meshes.addAll(vehicleHandle.getHullMeshes());

        for (int partIndex = 0; partIndex < vehicleHandle.getPartCount(); partIndex++) {

            VehiclePartStruct part = vehicleHandle.getPart(partIndex);

            if (!part.getRole().isStatic() && VehicleSpaceUtility.isPartShownUnderWay(part))
                meshes.addAll(part.getMeshes());

            includeDeck(part);
        }

        for (int slot = 0; slot < vehicleHandle.getCargoSlotCount(); slot++)
            cargo.add(vehicleHandle.getCargoSlot(slot));

        this.displayName = vehicleHandle.getDisplayName();
        this.partCount = vehicleHandle.getPartCount();
        this.minX = vehicleHandle.getMinX();
        this.minY = vehicleHandle.getMinY();
        this.minZ = vehicleHandle.getMinZ();
        this.maxX = vehicleHandle.getMaxX();
        this.maxY = vehicleHandle.getMaxY();
        this.maxZ = vehicleHandle.getMaxZ();
        this.shown = true;
        clampDeck();
    }

    // The table's draft shown: every mesh it was built with and the cargo it places
    private void showDraft() {

        clearShown();

        ShipLayoutStruct layout = draft.getLayout();
        ObjectArrayList<VehiclePartStruct> parts = layout.getParts();

        if (parts.isEmpty())
            return;

        meshes.addAll(draft.getMeshes());
        cargo.addAll(layout.getCargoSlots());
        this.minX = Integer.MAX_VALUE;
        this.minY = Integer.MAX_VALUE;
        this.minZ = Integer.MAX_VALUE;
        this.maxX = Integer.MIN_VALUE;
        this.maxY = Integer.MIN_VALUE;
        this.maxZ = Integer.MIN_VALUE;

        for (int i = 0; i < parts.size(); i++) {

            VehiclePartStruct part = parts.get(i);

            this.minX = Math.min(minX, part.getMinX());
            this.minY = Math.min(minY, part.getMinY());
            this.minZ = Math.min(minZ, part.getMinZ());
            this.maxX = Math.max(maxX, part.getMaxX());
            this.maxY = Math.max(maxY, part.getMaxY());
            this.maxZ = Math.max(maxZ, part.getMaxZ());
        }

        this.displayName = draft.getDisplayName().isEmpty()
                ? ShipViewerSetting.STATUS_UNNAMED
                : draft.getDisplayName();
        this.partCount = parts.size();
        this.deckCount = draft.getDeckCount();
        this.shown = true;
        clampDeck();
    }

    private void clearShown() {
        meshes.clear();
        cargo.clear();
        this.shown = false;
        this.partCount = 0;
        this.deckCount = 0;
    }

    // A hull part's top counted toward the decks a vehicle has: a deck's floor tops the hull every SHIP_DECK_HEIGHT
    // octants
    private void includeDeck(VehiclePartStruct part) {

        if (part.getRole() != VehiclePartRole.HULL)
            return;

        int layers = part.getMaxY() / EngineSetting.SHIP_OCTANT_SUB_VOXELS;

        this.deckCount = Math.max(deckCount, (Math.max(1, layers) - 1) / EngineSetting.SHIP_DECK_HEIGHT + 1);
    }

    // Cutaway \\

    // Steps the deck shown down toward the bottom, or up to every deck past the top
    public void stepDeck(int step) {

        if (!shown)
            return;

        int top = deckCount - 1;
        int current = deck == ShipViewerSetting.DECK_ALL ? deckCount : deck;

        this.deck = Math.clamp(current + step, 0, deckCount);

        if (deck > top)
            this.deck = ShipViewerSetting.DECK_ALL;

        this.revision++;
    }

    private void clampDeck() {

        if (deck >= deckCount)
            this.deck = ShipViewerSetting.DECK_ALL;
    }

    // Accessible \\

    // True while a ship is shown
    @Override
    public boolean isShown() {
        return shown;
    }

    // The name of the vehicle chosen, null while the table or nothing is
    public String getVehicleName() {
        return shipViewerManager.getSession() != null ? null : shipViewerManager.getVehicleName();
    }

    // True while the ship shown is the one on the Shipwright's table
    public boolean isDraft() {
        return shipViewerManager.getSession() != null;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getPartCount() {
        return partCount;
    }

    // Every mesh drawn, in the model's own space at rest
    public ObjectArrayList<MeshInstance> getMeshes() {
        return meshes;
    }

    // Every cargo slot standing in the ship
    public ObjectArrayList<FurnishingSlotStruct> getCargo() {
        return cargo;
    }

    // The middle of the ship's bounds on an axis, in model blocks
    @Override
    public float getCenter(int axis) {

        float scale = 0.5f / EngineSetting.SUB_VOXEL_RESOLUTION;

        return switch (axis) {
            case EngineSetting.AXIS_X -> (minX + maxX) * scale;
            case EngineSetting.AXIS_Y -> (minY + maxY) * scale;
            default -> (minZ + maxZ) * scale;
        };
    }

    // Half the diagonal of the ship's bounds, in model blocks
    @Override
    public float getBoundingRadius() {

        float scale = 0.5f / EngineSetting.SUB_VOXEL_RESOLUTION;
        float x = (maxX - minX) * scale;
        float y = (maxY - minY) * scale;
        float z = (maxZ - minZ) * scale;

        return (float) Math.sqrt(x * x + y * y + z * z);
    }

    public int getDeckCount() {
        return deckCount;
    }

    // The deck shown with those under it, DECK_ALL for every deck
    public int getDeck() {
        return deck;
    }

    // The height in model blocks above which the cutaway removes the ship, CUTAWAY_NONE showing every deck
    public float getCutawayHeight() {

        if (deck == ShipViewerSetting.DECK_ALL)
            return ShipViewerSetting.CUTAWAY_NONE;

        return (deck + 1) * EngineSetting.SHIP_DECK_HEIGHT * EngineSetting.SUB_BLOCK_SIZE
                - ShipViewerSetting.CUTAWAY_EPSILON;
    }

    // Counted up whenever what is shown is looked up or built again, or the cutaway changes
    public int getRevision() {
        return revision;
    }
}
