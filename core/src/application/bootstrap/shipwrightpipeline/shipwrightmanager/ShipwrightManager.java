package application.bootstrap.shipwrightpipeline.shipwrightmanager;

import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.furnishingpipeline.furnishingmanager.FurnishingManager;
import application.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle;
import application.bootstrap.shipwrightpipeline.shipwright.ShipBlueprintStruct;
import application.bootstrap.shipwrightpipeline.shipwright.ShipClimb;
import application.bootstrap.shipwrightpipeline.shipwright.ShipLayoutStruct;
import application.bootstrap.shipwrightpipeline.shipwright.ShipOctantGridStruct;
import application.bootstrap.shipwrightpipeline.shipwright.ShipOpening;
import application.bootstrap.shipwrightpipeline.shipwright.ShipPreviewStruct;
import application.bootstrap.shipwrightpipeline.shipwright.ShipWall;
import application.bootstrap.shipwrightpipeline.shipwright.ShipwrightSessionInstance;
import application.bootstrap.shipwrightpipeline.shipwright.ShipwrightStorage;
import application.bootstrap.shipwrightpipeline.shipwright.ShipwrightTab;
import application.bootstrap.shipwrightpipeline.shipwright.ShipwrightTool;
import application.bootstrap.soundpipeline.soundcuemanager.SoundCueManager;
import application.bootstrap.vehiclepipeline.vehicle.VehicleInstance;
import application.bootstrap.vehiclepipeline.vehicle.VehiclePartStruct;
import application.bootstrap.vehiclepipeline.vehiclemanager.VehicleManager;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ShipwrightManager extends ManagerPackage {

    /*
     * Owns the drafting table, the one shipwright the editor and the game
     * share, and with it every vehicle there is: no vehicle is written by
     * hand. Every vehicle the editor drew into the assets is loaded at boot
     * and every one the player drew into their saves once the game is up,
     * each compiled and registered as a vehicle, so any of them can be
     * launched; a ship saved at a table is
     * registered again at once. A drafting table in use is a session opened
     * here, writing to the assets or to the saves, and every change it makes
     * is worked through ShipwrightEditBranch, compiled through
     * ShipwrightCompileBranch at once and drawn through ShipwrightDraftBranch;
     * what stands under the cursor, and why the thing in hand could not be
     * set down there, is found through ShipwrightInspectBranch whenever the
     * table changes;
     * measuring it through ShipwrightSurveyBranch meshes the whole ship, so
     * that waits until the drawing has rested a moment, keeping every stroke
     * and press free of a hitch. A new ship starts as a blank sheet, its
     * bottom deck waiting to be drawn. A ship must be named, buildable, within the triangle
     * limit — at boot as at the table — and fitted with a helm to steer by and
     * a capstan to weigh the anchors it always carries to be saved,
     * and saved to be launched; a ship that would capsize may still be saved,
     * the table having said so; a save says where the file was written, or
     * why it was not. Whether unsaved work may be thrown away is asked by the
     * table before it starts or opens another ship. A table writing to the assets hands each ship to the
     * asset writer set on it, so the editor checks and records the write as it
     * does every content file; with none set the file is written directly.
     */

    @FunctionalInterface
    public interface ShipwrightAssetWriter {
        String write(String shipName, ArpgObjectStruct shipArpg);
    }

    // Internal
    private VehicleManager vehicleManager;
    private FurnishingManager furnishingManager;
    private SoundCueManager soundCueManager;

    // Cues
    private int launchCueID;

    // Branches
    private ShipwrightCompileBranch shipwrightCompileBranch;
    private ShipwrightSurveyBranch shipwrightSurveyBranch;
    private ShipwrightDraftBranch shipwrightDraftBranch;
    private ShipwrightLibraryBranch shipwrightLibraryBranch;
    private ShipwrightEditBranch shipwrightEditBranch;
    private ShipwrightRigBranch shipwrightRigBranch;
    private ShipwrightInspectBranch shipwrightInspectBranch;
    private ShipwrightFittingBranch shipwrightFittingBranch;

    // Furnishings — every furnishing table in the game, by section then by the name of the item it furnishes, and the
    // section each is listed under
    private ObjectArrayList<String> furnishingNames;
    private ObjectArrayList<String> furnishingSections;

    // Previews — the helm and the capstan as the catalogue pictures them
    private ShipPreviewStruct helmPreview;
    private ShipPreviewStruct capstanPreview;

    // Scratch — main thread only
    private ShipLayoutStruct contentLayout;

    // Base \\

    @Override
    protected void create() {

        // Branches
        create(ShipwrightHullBranch.class);
        this.shipwrightRigBranch = create(ShipwrightRigBranch.class);
        this.shipwrightFittingBranch = create(ShipwrightFittingBranch.class);
        this.shipwrightInspectBranch = create(ShipwrightInspectBranch.class);
        this.shipwrightCompileBranch = create(ShipwrightCompileBranch.class);
        this.shipwrightSurveyBranch = create(ShipwrightSurveyBranch.class);
        this.shipwrightDraftBranch = create(ShipwrightDraftBranch.class);
        this.shipwrightLibraryBranch = create(ShipwrightLibraryBranch.class);
        this.shipwrightEditBranch = create(ShipwrightEditBranch.class);
        create(ShipwrightLoader.class);

        // Furnishings
        this.furnishingNames = new ObjectArrayList<>();
        this.furnishingSections = new ObjectArrayList<>();

        // Scratch
        this.contentLayout = new ShipLayoutStruct(new ShipOctantGridStruct());
    }

    @Override
    protected void get() {
        this.vehicleManager = get(VehicleManager.class);
        this.furnishingManager = get(FurnishingManager.class);
        this.soundCueManager = get(SoundCueManager.class);
    }

    @Override
    protected void awake() {

        // Cues
        this.launchCueID = soundCueManager.getCueIDFromCueName(EngineSetting.SOUND_CUE_SHIP_LAUNCH);

        collectFurnishings();
        buildPreviews();
        internalLoader.requestAll();
        registerSavedShips();
    }

    // Every furnishing table in the game listed by the folder it is kept in, then by the name of the item it furnishes
    private void collectFurnishings() {

        furnishingManager.collectFurnishingNames(EngineSetting.SHIPWRIGHT_FURNISHING_FOLDER, furnishingNames);
        furnishingNames.sort((first, second) -> {
            int section = resolveSection(first).compareTo(resolveSection(second));
            return section != 0 ? section : resolveDisplayName(first).compareTo(resolveDisplayName(second));
        });

        for (int i = 0; i < furnishingNames.size(); i++)
            furnishingSections.add(resolveSection(furnishingNames.get(i)));
    }

    // The helm and the capstan built and meshed apart from any ship, each from the origin the previews stand at
    private void buildPreviews() {

        int origin = EngineSetting.SHIPWRIGHT_PREVIEW_ORIGIN_SUB_VOXELS;
        ObjectArrayList<VehiclePartStruct> helmParts = new ObjectArrayList<>();
        ObjectArrayList<VehiclePartStruct> capstanParts = new ObjectArrayList<>();

        shipwrightFittingBranch.buildHelm(helmParts, origin, 0, origin);
        shipwrightFittingBranch.buildCapstan(capstanParts, origin, 0, origin);

        this.helmPreview = new ShipPreviewStruct(helmParts, vehicleManager.buildRestingMeshes(helmParts));
        this.capstanPreview = new ShipPreviewStruct(capstanParts, vehicleManager.buildRestingMeshes(capstanParts));
    }

    // The section a furnishing table is listed under: the folder it is kept in, capitalised
    private static String resolveSection(String furnishingName) {

        int separator = furnishingName.indexOf(EngineSetting.PATH_SEPARATOR);

        if (separator <= 0)
            return EngineSetting.SHIPWRIGHT_TEXT_OTHER_FURNITURE;

        return Character.toUpperCase(furnishingName.charAt(0)) + furnishingName.substring(1, separator);
    }

    private String resolveDisplayName(String furnishingName) {
        return furnishingManager.getFurnishingHandleFromFurnishingName(furnishingName).getItems()[0]
                .getDisplayName();
    }

    // Writes every ship a table keeps in the assets through the writer given, in place of writing its file directly
    public void setAssetWriter(ShipwrightAssetWriter assetWriter) {
        shipwrightLibraryBranch.setAssetWriter(assetWriter);
    }

    // Registration \\

    // A ship in the assets compiled and registered as a vehicle, stopping the game when it cannot be built
    void addContentShip(ShipBlueprintStruct blueprint) {

        String problem = compileForRegistration(blueprint);

        if (!problem.isEmpty())
            throwException("Vehicle '" + blueprint.getShipName() + "' cannot be built: " + problem);

        registerVehicle(ShipwrightStorage.ASSETS, blueprint, contentLayout);
    }

    // Every ship in the player's saves registered, those that cannot be read or built logged and passed over
    private void registerSavedShips() {

        ObjectArrayList<String> shipNames = shipwrightLibraryBranch.listShips(ShipwrightStorage.SAVES);

        for (int i = 0; i < shipNames.size(); i++) {

            ShipBlueprintStruct blueprint = shipwrightLibraryBranch.load(ShipwrightStorage.SAVES, shipNames.get(i));

            if (blueprint == null)
                continue;

            String problem = compileForRegistration(blueprint);

            if (!problem.isEmpty()) {
                errorLog("Saved vehicle '" + shipNames.get(i) + "' was skipped: " + problem);
                continue;
            }

            registerVehicle(ShipwrightStorage.SAVES, blueprint, contentLayout);
        }
    }

    // A ship compiled into the registration layout, the reason it cannot be registered or SHIP_PROBLEM_NONE
    private String compileForRegistration(ShipBlueprintStruct blueprint) {

        shipwrightCompileBranch.compile(blueprint, contentLayout);

        if (contentLayout.hasProblem())
            return contentLayout.getProblem();

        if (vehicleManager.countTriangles(contentLayout.getParts()) > EngineSetting.SHIP_TRIANGLE_LIMIT)
            return EngineSetting.SHIPWRIGHT_REFUSAL_TRIANGLES + EngineSetting.SHIP_TRIANGLE_LIMIT;

        return EngineSetting.SHIP_PROBLEM_NONE;
    }

    private void registerVehicle(ShipwrightStorage storage, ShipBlueprintStruct blueprint, ShipLayoutStruct layout) {
        vehicleManager.registerVehicle(shipwrightCompileBranch.toDesign(
                blueprint,
                layout,
                toVehicleName(storage, blueprint.getShipName())));
    }

    // The vehicle a design kept in a storage is registered as: the game's own by its path below the vehicle
    // directory, as every resource is named, the player's apart from them
    public String toVehicleName(ShipwrightStorage storage, String shipName) {
        return storage == ShipwrightStorage.ASSETS ? shipName : EngineSetting.VEHICLE_SAVED_PREFIX + shipName;
    }

    // Session \\

    // A drafting table writing to a storage, a new ship on its sheet
    public ShipwrightSessionInstance openSession(ShipwrightStorage storage) {

        ShipwrightSessionInstance session = create(ShipwrightSessionInstance.class);

        session.constructor(storage, new ShipBlueprintStruct());
        compileSession(session);
        surveySession(session);

        return session;
    }

    // The blueprint on a session's sheet compiled and its views drawn, its survey due once it rests
    void compileSession(ShipwrightSessionInstance session) {

        shipwrightCompileBranch.compile(session.getBlueprint(), session.getLayout());
        shipwrightDraftBranch.drawViews(session);
        session.scheduleSurvey(EngineSetting.SHIPWRIGHT_SURVEY_DELAY);
        session.markChanged();
    }

    // Surveys a session's ship once it has rested since it last compiled, meshing it as the game will
    public void updateSurvey(ShipwrightSessionInstance session, float deltaTime) {

        if (session.tickSurvey(deltaTime))
            surveySession(session);
    }

    private void surveySession(ShipwrightSessionInstance session) {

        shipwrightSurveyBranch.survey(session.getBlueprint(), session.getLayout(), session.getSurvey());
        session.clearSurvey();

        if (session.getSurvey().getTriangles() > EngineSetting.SHIP_TRIANGLE_LIMIT)
            session.setStatus(EngineSetting.SHIPWRIGHT_REFUSAL_TRIANGLES + EngineSetting.SHIP_TRIANGLE_LIMIT);

        session.markChanged();
    }

    public void drawSheet(ShipwrightSessionInstance session) {
        shipwrightDraftBranch.drawSheet(session);
    }

    // Whatever stands under the cursor, and why the thing in hand could not be set down there, written to the session
    public void inspect(ShipwrightSessionInstance session) {

        int x = ShipwrightEditBranch.toSheetX(session.getCursorU());
        int z = ShipwrightEditBranch.toSheetZ(session.getCursorV());

        shipwrightInspectBranch.pick(session, x, z, session.getHover());

        if (shipwrightEditBranch.isMounting(session, session.getHover()))
            session.getHover().clear();

        session.setPlacementProblem(session.getHover().isEmpty()
                ? shipwrightEditBranch.findPlacementProblem(session, x, z)
                : EngineSetting.SHIP_PROBLEM_NONE);
    }

    // The octants the sheet spans, written to the session
    public void resolveSheetSize(ShipwrightSessionInstance session) {
        shipwrightDraftBranch.resolveSheetSize(session);
    }

    // File \\

    public void newShip(ShipwrightSessionInstance session) {
        session.load(new ShipBlueprintStruct(), EngineSetting.SHIP_NAME_NONE);
        compileSession(session);
        session.setStatus(EngineSetting.SHIPWRIGHT_STATUS_NEW);
    }

    // Every ship kept in the session's storage, by resource name, sorted
    public ObjectArrayList<String> listShips(ShipwrightSessionInstance session) {
        return shipwrightLibraryBranch.listShips(session.getStorage());
    }

    // A ship kept in the session's storage put on the sheet by its resource name
    public void openShip(ShipwrightSessionInstance session, String shipName) {

        ShipBlueprintStruct blueprint = shipwrightLibraryBranch.hasShip(session.getStorage(), shipName)
                ? shipwrightLibraryBranch.load(session.getStorage(), shipName)
                : null;

        if (blueprint == null) {
            session.setStatus(EngineSetting.SHIPWRIGHT_STATUS_UNREADABLE + shipName);
            return;
        }

        session.load(blueprint, shipName);
        compileSession(session);
        session.setStatus(EngineSetting.SHIPWRIGHT_STATUS_OPENED + blueprint.getDisplayName());
    }

    // Writes the ship on the sheet to its storage and registers it, true once it is; otherwise the status says why
    // it was not saved
    public boolean saveShip(ShipwrightSessionInstance session) {

        ShipBlueprintStruct blueprint = session.getBlueprint();
        String shipName = shipwrightLibraryBranch.toShipName(blueprint.getDisplayName());

        if (session.isSurveyDue())
            surveySession(session);

        String refusal = resolveSaveRefusal(session, shipName);

        if (!refusal.isEmpty()) {
            session.setStatus(EngineSetting.SHIPWRIGHT_STATUS_NOT_SAVED + refusal);
            return false;
        }

        blueprint.setShipName(shipName);

        String writeRefusal = shipwrightLibraryBranch.save(session.getStorage(), blueprint);

        if (!writeRefusal.isEmpty()) {
            session.setStatus(EngineSetting.SHIPWRIGHT_STATUS_NOT_SAVED + writeRefusal);
            return false;
        }

        registerVehicle(session.getStorage(), blueprint, session.getLayout());
        compileSession(session);
        surveySession(session);
        session.markSaved(shipName);
        session.setStatus(EngineSetting.SHIPWRIGHT_STATUS_SAVED
                + shipwrightLibraryBranch.describeFile(session.getStorage(), shipName));

        return true;
    }

    private String resolveSaveRefusal(ShipwrightSessionInstance session, String shipName) {

        ShipBlueprintStruct blueprint = session.getBlueprint();

        if (shipName.isEmpty())
            return EngineSetting.SHIPWRIGHT_REFUSAL_NAME;

        if (!shipName.equals(session.getSavedName())
                && shipwrightLibraryBranch.hasShip(session.getStorage(), shipName))
            return EngineSetting.SHIPWRIGHT_REFUSAL_NAME_TAKEN;

        if (session.getLayout().hasProblem())
            return session.getLayout().getProblem();

        if (session.getSurvey().getTriangles() > EngineSetting.SHIP_TRIANGLE_LIMIT)
            return EngineSetting.SHIPWRIGHT_REFUSAL_TRIANGLES + EngineSetting.SHIP_TRIANGLE_LIMIT;

        if (!blueprint.hasHelm())
            return EngineSetting.SHIPWRIGHT_REFUSAL_HELM;

        if (!blueprint.hasCapstan())
            return EngineSetting.SHIPWRIGHT_REFUSAL_CAPSTAN;

        return EngineSetting.SHIPWRIGHT_STATUS_NONE;
    }

    // Launches the ship on the sheet beside an entity once it is saved as it stands, true when it was launched
    public boolean launchShip(ShipwrightSessionInstance session, EntityInstance entity, Vector3 direction) {

        String vehicleName = toVehicleName(session.getStorage(), session.getSavedName());

        if (!session.isSaved() || session.isDirty() || !vehicleManager.hasVehicle(vehicleName)) {
            session.setStatus(EngineSetting.SHIPWRIGHT_REFUSAL_LAUNCH);
            return false;
        }

        VehicleInstance vehicle = vehicleManager.spawnBeside(
                vehicleManager.getVehicleHandleFromVehicleName(vehicleName), entity, direction);

        soundCueManager.playCueAt(launchCueID, vehicle.getWorldHandle(), vehicle.getWorldPositionStruct());
        session.setStatus(EngineSetting.SHIPWRIGHT_STATUS_LAUNCHED + session.getBlueprint().getDisplayName());

        return true;
    }

    // Edit \\

    public void selectTab(ShipwrightSessionInstance session, ShipwrightTab tab) {
        shipwrightEditBranch.selectTab(session, tab);
    }

    public void beginStroke(ShipwrightSessionInstance session, boolean erasing) {
        shipwrightEditBranch.beginStroke(session, erasing);
    }

    public void strokeAt(ShipwrightSessionInstance session, int u, int v) {
        shipwrightEditBranch.strokeAt(session, u, v);
    }

    public void endStroke(ShipwrightSessionInstance session) {
        shipwrightEditBranch.endStroke(session);
    }

    public void stepDeck(ShipwrightSessionInstance session, int step) {
        shipwrightEditBranch.stepDeck(session, step);
    }

    public void addDeck(ShipwrightSessionInstance session) {
        shipwrightEditBranch.addDeck(session);
    }

    public void removeDeck(ShipwrightSessionInstance session) {
        shipwrightEditBranch.removeDeck(session);
    }

    public void turn(ShipwrightSessionInstance session, int step) {
        shipwrightEditBranch.turn(session, step);
    }

    public void stepOption(ShipwrightSessionInstance session, int step) {
        shipwrightEditBranch.stepOption(session, step);
    }

    public void pickOption(ShipwrightSessionInstance session, int index) {
        shipwrightEditBranch.pickOption(session, index);
    }

    public void nextTab(ShipwrightSessionInstance session) {
        shipwrightEditBranch.selectTab(session, session.getTab().next());
    }

    public void undo(ShipwrightSessionInstance session) {
        shipwrightEditBranch.undo(session);
    }

    public void redo(ShipwrightSessionInstance session) {
        shipwrightEditBranch.redo(session);
    }

    public void confirmName(ShipwrightSessionInstance session, String displayName) {
        shipwrightEditBranch.confirmName(session, displayName);
    }

    // Accessible \\

    public int getFurnishingCount() {
        return furnishingNames.size();
    }

    // How many options the tab open lists in its catalogue
    public int getOptionCount(ShipwrightSessionInstance session) {
        return shipwrightEditBranch.getOptionCount(session);
    }

    // The entry of the tab open the option in hand is listed as, INDEX_NOT_FOUND when none is
    public int getOptionIndex(ShipwrightSessionInstance session) {
        return shipwrightEditBranch.getOptionIndex(session);
    }

    // The option of its tool in hand, INDEX_NOT_FOUND for no mast picked
    public int getHeldOption(ShipwrightSessionInstance session) {
        return shipwrightEditBranch.getHeldOption(session);
    }

    // The tool an entry of the tab open works
    public ShipwrightTool getEntryTool(ShipwrightSessionInstance session, int entry) {
        return shipwrightEditBranch.getEntryTool(session, entry);
    }

    // The option of its tool an entry of the tab open stands for
    public int getEntryOption(ShipwrightSessionInstance session, int entry) {
        return shipwrightEditBranch.getEntryOption(session, entry);
    }

    // The section of the tab open an entry is listed under: the tab's own for its fixed entries and masts, furniture
    // by the folder its table is kept in
    public String getEntrySection(ShipwrightSessionInstance session, int entry) {

        ShipwrightTab tab = session.getTab();

        if (entry < tab.getEntryCount())
            return tab.getEntrySection(entry);

        if (tab.getFollowingTool() == ShipwrightTool.MASTS)
            return EngineSetting.SHIPWRIGHT_SECTION_MASTS;

        return furnishingSections.get(getEntryOption(session, entry) - EngineSetting.SHIPWRIGHT_ITEM_FURNISHINGS);
    }

    // The name an option of a tool's catalogue is shown by
    public String getOptionName(ShipwrightSessionInstance session, ShipwrightTool tool, int option) {

        if (option < 0)
            return tool == ShipwrightTool.FURNITURE ? EngineSetting.SHIPWRIGHT_TEXT_UNKNOWN_ITEM : tool.getTitle();

        return switch (tool) {
            case WALLS -> ShipWall.VALUES[option].getTitle();
            case OPENINGS -> ShipOpening.VALUES[option].getTitle();
            case STAIRS -> ShipClimb.VALUES[option].getTitle();
            case FURNITURE -> getItemName(option);
            case MASTS -> getMastName(session, option);
            default -> tool.getTitle();
        };
    }

    // The name of the furniture item at an index of the drafting table's list: the helm, the capstan, then every
    // furnishing by the name of the item it furnishes
    public String getItemName(int itemIndex) {

        int item = Math.floorMod(itemIndex, EngineSetting.SHIPWRIGHT_ITEM_FURNISHINGS + furnishingNames.size());

        if (item == EngineSetting.SHIPWRIGHT_ITEM_HELM)
            return EngineSetting.SHIPWRIGHT_TEXT_HELM;

        if (item == EngineSetting.SHIPWRIGHT_ITEM_CAPSTAN)
            return EngineSetting.SHIPWRIGHT_TEXT_CAPSTAN;

        return resolveDisplayName(getItemFurnishingName(item));
    }

    // The built fitting an option of a tool's catalogue is pictured by where no item stands for it, null for none
    public ShipPreviewStruct getOptionPreview(ShipwrightTool tool, int option) {

        if (tool != ShipwrightTool.FURNITURE)
            return null;

        return option == EngineSetting.SHIPWRIGHT_ITEM_HELM ? helmPreview
                : option == EngineSetting.SHIPWRIGHT_ITEM_CAPSTAN ? capstanPreview : null;
    }

    // The item an option of a tool's catalogue is pictured by, null for one no item stands for
    public ItemDefinitionHandle getOptionItem(ShipwrightTool tool, int option) {

        String furnishingName = tool == ShipwrightTool.FURNITURE && option >= EngineSetting.SHIPWRIGHT_ITEM_FURNISHINGS
                ? getItemFurnishingName(option)
                : tool == ShipwrightTool.OPENINGS && option == ShipOpening.DOOR.ordinal()
                        ? EngineSetting.SHIP_DOOR_FURNISHING
                        : null;

        if (furnishingName == null || !furnishingManager.hasFurnishing(furnishingName))
            return null;

        return furnishingManager.getFurnishingHandleFromFurnishingName(furnishingName).getItems()[0];
    }

    // The furnishing table a furniture item past the helm and the capstan is drawn from
    public String getItemFurnishingName(int itemIndex) {
        return furnishingNames.get(itemIndex - EngineSetting.SHIPWRIGHT_ITEM_FURNISHINGS);
    }

    // The furniture item a furnishing table is listed as, INDEX_NOT_FOUND for one the drafting table does not offer
    public int findItemIndex(String furnishingName) {

        int index = furnishingNames.indexOf(furnishingName);

        return index == EngineSetting.INDEX_NOT_FOUND ? index : EngineSetting.SHIPWRIGHT_ITEM_FURNISHINGS + index;
    }

    public String getMastName(ShipwrightSessionInstance session, int mastIndex) {
        return shipwrightRigBranch.resolveMastName(session.getBlueprint().getMasts().size(), mastIndex);
    }
}
