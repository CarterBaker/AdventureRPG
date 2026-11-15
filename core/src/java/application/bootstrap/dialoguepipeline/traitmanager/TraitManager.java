package application.bootstrap.dialoguepipeline.traitmanager;

import application.bootstrap.dialoguepipeline.trait.TraitHandle;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class TraitManager extends ManagerPackage {

    /*
     * Owns the trait palette: every lasting part of a being's nature and speech.
     * IDs are assigned in registration order, and a name declared twice is
     * rejected. Every trait is resolved in awake(), where a trait naming an
     * opposite no file declares is caught.
     */

    // Palette
    private Object2IntOpenHashMap<String> traitName2TraitID;
    private ObjectArrayList<TraitHandle> traitID2TraitHandle;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.traitName2TraitID = RegistryUtility.createNameIndex();
        this.traitID2TraitHandle = RegistryUtility.createPalette();
        create(TraitLoader.class);
    }

    @Override
    protected void awake() {

        internalLoader.requestAll();

        for (Object2IntMap.Entry<String> entry : traitName2TraitID.object2IntEntrySet())
            if (traitID2TraitHandle.get(entry.getIntValue()) == null)
                throwException("Trait \"" + entry.getKey() + "\" is named but no file declares it.");
    }

    // Management \\

    short registerTraitName(String traitName) {
        return (short) RegistryUtility.registerID(
                traitName2TraitID, traitID2TraitHandle, traitName,
                EngineSetting.REGISTRY_SHORT_ID_COUNT);
    }

    void addTraitHandle(TraitHandle traitHandle) {

        short traitID = traitHandle.getTraitID();

        if (traitID2TraitHandle.get(traitID) != null)
            throwException("Duplicate trait name: '" + traitHandle.getTraitName()
                    + "' is declared more than once — every trait name must be unique");

        traitID2TraitHandle.set(traitID, traitHandle);
    }

    // Accessible \\

    public boolean hasTrait(String traitName) {
        return RegistryUtility.getHandle(traitName2TraitID, traitID2TraitHandle, traitName) != null;
    }

    public short getTraitIDFromTraitName(String traitName) {

        if (!hasTrait(traitName) && internalLoader != null)
            ((TraitLoader) internalLoader).request(traitName);

        if (!hasTrait(traitName))
            throwException("Trait not found after load: \"" + traitName + "\"");

        return (short) traitName2TraitID.getInt(traitName);
    }

    public TraitHandle getTraitHandleFromTraitID(short traitID) {

        TraitHandle handle = RegistryUtility.getHandle(traitID2TraitHandle, traitID);

        if (handle == null)
            throwException("Trait ID not found: " + traitID);

        return handle;
    }

    public TraitHandle getTraitHandleFromTraitName(String traitName) {
        return getTraitHandleFromTraitID(getTraitIDFromTraitName(traitName));
    }

    // Every trait ID lies below this — the length of an array indexed by trait ID
    public int getTraitIDCount() {
        return traitID2TraitHandle.size();
    }

    // True when the two traits can never be held by one being
    public boolean areOpposed(short traitID, short otherTraitID) {
        return getTraitHandleFromTraitID(traitID).getOppositeID() == otherTraitID
                || getTraitHandleFromTraitID(otherTraitID).getOppositeID() == traitID;
    }
}
