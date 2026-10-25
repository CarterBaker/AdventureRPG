package application.bootstrap.shipwrightpipeline.shipwrightmanager;

import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.furnishingpipeline.furnishingmanager.FurnishingManager;
import application.bootstrap.shipwrightpipeline.shipwright.ShipBlueprintStruct;
import application.bootstrap.shipwrightpipeline.shipwright.ShipLayoutStruct;
import application.bootstrap.shipwrightpipeline.shipwright.ShipOctantGridStruct;
import application.bootstrap.shipwrightpipeline.shipwright.ShipwrightSessionInstance;
import application.bootstrap.shipwrightpipeline.shipwright.ShipwrightStorage;
import application.bootstrap.shipwrightpipeline.shipwright.ShipwrightTool;
import application.bootstrap.vehiclepipeline.vehiclemanager.VehicleManager;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ShipwrightManager extends ManagerPackage {

    /*
     * Owns the drafting table, the one shipwright the editor and the game
     * share. Every ship in the assets is loaded at boot and every ship in the
     * player's saves once the game is up, each compiled and registered as a
     * vehicle, so any of them can be launched; a ship saved at a table is
     * registered again at once. A drafting table in use is a session opened
     * here, writing to the assets or to the saves, and every change it makes
     * is worked through ShipwrightEditBranch, compiled through
     * ShipwrightCompileBranch at once and drawn through ShipwrightDraftBranch;
     * measuring it through ShipwrightSurveyBranch meshes the whole ship, so
     * that waits until the drawing has rested a moment, keeping every stroke
     * and press free of a hitch. A new ship starts as a blank sheet, its
     * bottom deck waiting to be drawn. A ship must be named, buildable, within the triangle
     * limit — at boot as at the table — and fitted with a helm to be saved,
     * and saved to be launched; a ship that would capsize may still be saved,
     * the table having said so. Opening or starting another ship over unsaved
     * work asks twice. A table writing to the assets hands each ship to the
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

    // Branches
    private ShipwrightCompileBranch shipwrightCompileBranch;
    private ShipwrightSurveyBranch shipwrightSurveyBranch;
    private ShipwrightDraftBranch shipwrightDraftBranch;
    private ShipwrightLibraryBranch shipwrightLibraryBranch;
    private ShipwrightEditBranch shipwrightEditBranch;
    private ShipwrightRigBranch shipwrightRigBranch;

    // Furnishings — the tables furniture is drawn from at a drafting table
    private ObjectArrayList<String> furnishingNames;

    // Scratch — main thread only
    private ShipLayoutStruct contentLayout;

    // Base \\

    @Override
    protected void create() {

        // Branches
        create(ShipwrightHullBranch.class);
        this.shipwrightRigBranch = create(ShipwrightRigBranch.class);
        create(ShipwrightFittingBranch.class);
        this.shipwrightCompileBranch = create(ShipwrightCompileBranch.class);
        this.shipwrightSurveyBranch = create(ShipwrightSurveyBranch.class);
        this.shipwrightDraftBranch = create(ShipwrightDraftBranch.class);
        this.shipwrightLibraryBranch = create(ShipwrightLibraryBranch.class);
        this.shipwrightEditBranch = create(ShipwrightEditBranch.class);
        create(ShipwrightLoader.class);

        // Furnishings
        this.furnishingNames = new ObjectArrayList<>();

        // Scratch
        this.contentLayout = new ShipLayoutStruct(new ShipOctantGridStruct());
    }

    @Override
    protected void get() {
        this.vehicleManager = get(VehicleManager.class);
        this.furnishingManager = get(FurnishingManager.class);
    }

    @Override
    protected void awake() {

        furnishingManager.collectFurnishingNames(EngineSetting.SHIPWRIGHT_FURNISHING_FOLDER, furnishingNames);
        internalLoader.requestAll();
        registerSavedShips();
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
            throwException("Ship '" + blueprint.getShipName() + "' cannot be built: " + problem);

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
                errorLog("Saved ship '" + shipNames.get(i) + "' was skipped: " + problem);
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
                layout,
                toVehicleName(storage, blueprint.getShipName()),
                blueprint.getDisplayName()));
    }

    // The vehicle a ship kept in a storage is registered as
    public String toVehicleName(ShipwrightStorage storage, String shipName) {
        return (storage == ShipwrightStorage.ASSETS
                ? EngineSetting.SHIP_VEHICLE_PREFIX
                : EngineSetting.SHIP_SAVED_VEHICLE_PREFIX) + shipName;
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

    // The octants the sheet spans, written to the session
    public void resolveSheetSize(ShipwrightSessionInstance session) {
        shipwrightDraftBranch.resolveSheetSize(session);
    }

    // File \\

    public void newShip(ShipwrightSessionInstance session) {

        if (!confirmDiscard(session))
            return;

        session.load(new ShipBlueprintStruct(), EngineSetting.SHIP_NAME_NONE);
        session.setLibraryIndex(EngineSetting.INDEX_NOT_FOUND);
        compileSession(session);
        session.setStatus(EngineSetting.SHIPWRIGHT_STATUS_NEW);
    }

    // The next or previous ship kept in the session's storage put on the sheet
    public void openShip(ShipwrightSessionInstance session, int step) {

        ObjectArrayList<String> shipNames = shipwrightLibraryBranch.listShips(session.getStorage());

        if (shipNames.isEmpty()) {
            session.setStatus(EngineSetting.SHIPWRIGHT_STATUS_NONE_KEPT);
            return;
        }

        if (!confirmDiscard(session))
            return;

        int index = session.getLibraryIndex() == EngineSetting.INDEX_NOT_FOUND
                ? (step > 0 ? 0 : shipNames.size() - 1)
                : Math.floorMod(session.getLibraryIndex() + step, shipNames.size());
        ShipBlueprintStruct blueprint = shipwrightLibraryBranch.load(session.getStorage(), shipNames.get(index));

        session.setLibraryIndex(index);

        if (blueprint == null) {
            session.setStatus(EngineSetting.SHIPWRIGHT_STATUS_UNREADABLE + shipNames.get(index));
            return;
        }

        session.load(blueprint, shipNames.get(index));
        compileSession(session);
        session.setStatus(EngineSetting.SHIPWRIGHT_STATUS_OPENED + blueprint.getDisplayName());
    }

    // True when nothing unsaved would be lost, or when the loss was asked about once already
    private boolean confirmDiscard(ShipwrightSessionInstance session) {

        if (!session.isDirty() || session.isDiscardArmed())
            return true;

        session.armDiscard();
        session.setStatus(EngineSetting.SHIPWRIGHT_STATUS_DISCARD);

        return false;
    }

    public void saveShip(ShipwrightSessionInstance session) {

        ShipBlueprintStruct blueprint = session.getBlueprint();
        String shipName = shipwrightLibraryBranch.toShipName(blueprint.getDisplayName());

        if (session.isSurveyDue())
            surveySession(session);

        String refusal = resolveSaveRefusal(session, shipName);

        if (!refusal.isEmpty()) {
            session.setStatus(refusal);
            return;
        }

        blueprint.setShipName(shipName);

        String writeRefusal = shipwrightLibraryBranch.save(session.getStorage(), blueprint);

        if (!writeRefusal.isEmpty()) {
            session.setStatus(writeRefusal);
            return;
        }

        registerVehicle(session.getStorage(), blueprint, session.getLayout());
        compileSession(session);
        surveySession(session);
        session.markSaved(shipName);
        session.setStatus(EngineSetting.SHIPWRIGHT_STATUS_SAVED + blueprint.getDisplayName());
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

        return EngineSetting.SHIPWRIGHT_STATUS_NONE;
    }

    // Launches the ship on the sheet beside an entity once it is saved as it stands, true when it was launched
    public boolean launchShip(ShipwrightSessionInstance session, EntityInstance entity, Vector3 direction) {

        String vehicleName = toVehicleName(session.getStorage(), session.getSavedName());

        if (!session.isSaved() || session.isDirty() || !vehicleManager.hasVehicle(vehicleName)) {
            session.setStatus(EngineSetting.SHIPWRIGHT_REFUSAL_LAUNCH);
            return false;
        }

        vehicleManager.spawnBeside(vehicleManager.getVehicleHandleFromVehicleName(vehicleName), entity, direction);
        session.setStatus(EngineSetting.SHIPWRIGHT_STATUS_LAUNCHED + session.getBlueprint().getDisplayName());

        return true;
    }

    // Edit \\

    public void setTool(ShipwrightSessionInstance session, ShipwrightTool tool) {
        session.setTool(tool);
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

    public void togglePorch(ShipwrightSessionInstance session) {
        shipwrightEditBranch.togglePorch(session);
    }

    public void turn(ShipwrightSessionInstance session) {
        shipwrightEditBranch.turn(session);
    }

    public void stepOption(ShipwrightSessionInstance session, int step) {
        shipwrightEditBranch.stepOption(session, step);
    }

    public void nextTool(ShipwrightSessionInstance session) {
        session.setTool(session.getTool().next());
    }

    public void stepMastHeight(ShipwrightSessionInstance session, int step) {
        shipwrightEditBranch.stepMastHeight(session, step);
    }

    public void toggleCrowsNest(ShipwrightSessionInstance session) {
        shipwrightEditBranch.toggleCrowsNest(session);
    }

    public void toggleRigging(ShipwrightSessionInstance session) {
        shipwrightEditBranch.toggleRigging(session);
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

    // The name of the furniture item at an index of the drafting table's list: the helm, the capstan, then every
    // furnishing table by its own name
    public String getItemName(int itemIndex) {

        int item = Math.floorMod(itemIndex, EngineSetting.SHIPWRIGHT_ITEM_FURNISHINGS + furnishingNames.size());

        if (item == EngineSetting.SHIPWRIGHT_ITEM_HELM)
            return EngineSetting.SHIPWRIGHT_TEXT_HELM;

        if (item == EngineSetting.SHIPWRIGHT_ITEM_CAPSTAN)
            return EngineSetting.SHIPWRIGHT_TEXT_CAPSTAN;

        String furnishingName = furnishingNames.get(item - EngineSetting.SHIPWRIGHT_ITEM_FURNISHINGS);

        return furnishingName.substring(furnishingName.lastIndexOf(EngineSetting.PATH_SEPARATOR) + 1);
    }

    // The height a mast stepped on the session's ship rises above its deck, as the hull's length sets
    public int resolveMastHeight(ShipwrightSessionInstance session) {
        return shipwrightRigBranch.resolveMastHeight(session.getLayout());
    }

    // The furnishing table at an index of the drafting table's list of furnishings
    public String getFurnishingName(int index) {
        return furnishingNames.get(index);
    }

    public String getMastName(ShipwrightSessionInstance session, int mastIndex) {
        return shipwrightRigBranch.resolveMastName(session.getBlueprint().getMasts().size(), mastIndex);
    }
}
