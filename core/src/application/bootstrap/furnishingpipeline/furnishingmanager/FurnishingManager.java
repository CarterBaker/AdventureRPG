package application.bootstrap.furnishingpipeline.furnishingmanager;

import application.bootstrap.furnishingpipeline.furnishing.FurnishingHandle;
import application.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.mathematics.extras.WeightedTableUtility;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class FurnishingManager extends ManagerPackage {

    /*
     * Owns the furnishing palette, the shared tables of furniture every
     * furnished place draws from, and is the one way an item is drawn from
     * one. Every table is resolved in awake(), before any vehicle enters the
     * world or any chunk generates, so worker threads only ever read the
     * palette. Furnishing IDs are assigned in registration order.
     */

    // Palette
    private Object2IntOpenHashMap<String> furnishingName2FurnishingID;
    private ObjectArrayList<FurnishingHandle> furnishingID2FurnishingHandle;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.furnishingName2FurnishingID = RegistryUtility.createNameIndex();
        this.furnishingID2FurnishingHandle = RegistryUtility.createPalette();

        create(FurnishingLoader.class);
    }

    @Override
    protected void awake() {
        internalLoader.requestAll();
    }

    // Management \\

    short registerFurnishingName(String furnishingName) {
        return (short) RegistryUtility.registerID(
                furnishingName2FurnishingID, furnishingID2FurnishingHandle, furnishingName,
                EngineSetting.REGISTRY_SHORT_ID_COUNT);
    }

    void addFurnishingHandle(FurnishingHandle furnishingHandle) {

        short furnishingID = furnishingHandle.getFurnishingID();

        if (furnishingID2FurnishingHandle.get(furnishingID) != null)
            throwException("Duplicate furnishing name: '" + furnishingHandle.getFurnishingName()
                    + "' was registered more than once");

        furnishingID2FurnishingHandle.set(furnishingID, furnishingHandle);
    }

    // On-Demand \\

    public void request(String furnishingName) {
        ((FurnishingLoader) internalLoader).request(furnishingName);
    }

    // Draw \\

    // The item a roll from 0 up to 1 draws from a table, each item as likely as its weight
    public ItemDefinitionHandle drawItem(short furnishingID, float roll) {

        FurnishingHandle furnishingHandle = getFurnishingHandleFromFurnishingID(furnishingID);

        return furnishingHandle.getItems()[WeightedTableUtility.pick(furnishingHandle.getCumulativeWeights(), roll)];
    }

    // Accessible \\

    public boolean hasFurnishing(String furnishingName) {
        return RegistryUtility.getHandle(
                furnishingName2FurnishingID, furnishingID2FurnishingHandle, furnishingName) != null;
    }

    public FurnishingHandle getFurnishingHandleFromFurnishingName(String furnishingName) {

        FurnishingHandle handle = RegistryUtility.getHandle(
                furnishingName2FurnishingID, furnishingID2FurnishingHandle, furnishingName);

        if (handle == null) {
            request(furnishingName);
            handle = RegistryUtility.getHandle(
                    furnishingName2FurnishingID, furnishingID2FurnishingHandle, furnishingName);
        }

        if (handle == null)
            throwException("Furnishing \"" + furnishingName + "\" was not registered after its on-demand load "
                    + "completed — check for a resource-name/path mismatch in the furnishing directory.");

        return handle;
    }

    public short getFurnishingIDFromFurnishingName(String furnishingName) {
        return getFurnishingHandleFromFurnishingName(furnishingName).getFurnishingID();
    }

    public FurnishingHandle getFurnishingHandleFromFurnishingID(short furnishingID) {

        FurnishingHandle handle = RegistryUtility.getHandle(furnishingID2FurnishingHandle, furnishingID);

        if (handle == null)
            throwException("No handle registered for furnishing ID: " + furnishingID);

        return handle;
    }
}
