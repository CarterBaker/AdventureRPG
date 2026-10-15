package application.bootstrap.combatpipeline.movesetmanager;

import application.bootstrap.combatpipeline.moveset.MovesetHandle;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class MovesetManager extends ManagerPackage {

    /*
     * Owns the moveset palette for the engine lifetime, with IDs assigned in
     * registration order. A moveset is how one kind of weapon fights — the
     * attack it makes from each side and the guard it holds toward each —
     * and an item names the one it is wielded with. MOVESET_NONE (0) is the
     * reserved sentinel for an item without one. Auto-triggers an on-demand
     * load via MovesetLoader on a name-based cache miss.
     */

    // Palette
    private Object2IntOpenHashMap<String> movesetName2MovesetID;
    private ObjectArrayList<MovesetHandle> movesetID2MovesetHandle;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.movesetName2MovesetID = RegistryUtility.createNameIndex();
        this.movesetID2MovesetHandle = RegistryUtility.createPalette();
        create(MovesetLoader.class);
    }

    // Management \\

    short registerMovesetName(String movesetName) {
        return (short) RegistryUtility.registerID(
                movesetName2MovesetID, movesetID2MovesetHandle, movesetName,
                EngineSetting.REGISTRY_SHORT_ID_COUNT);
    }

    void addMoveset(MovesetHandle handle) {
        movesetID2MovesetHandle.set(handle.getMovesetID(), handle);
    }

    // Accessible \\

    public boolean hasMoveset(String movesetName) {
        return RegistryUtility.getHandle(movesetName2MovesetID, movesetID2MovesetHandle, movesetName) != null;
    }

    public short getMovesetIDFromMovesetName(String movesetName) {

        if (!hasMoveset(movesetName))
            ((MovesetLoader) internalLoader).request(movesetName);

        if (!hasMoveset(movesetName))
            throwException("Moveset could not be loaded: \"" + movesetName + "\"");

        return (short) movesetName2MovesetID.getInt(movesetName);
    }

    public MovesetHandle getMovesetHandleFromMovesetID(short movesetID) {

        MovesetHandle handle = RegistryUtility.getHandle(movesetID2MovesetHandle, movesetID);

        if (handle == null)
            throwException("Moveset ID not found: " + movesetID);

        return handle;
    }

    public MovesetHandle getMovesetHandleFromMovesetName(String movesetName) {
        return getMovesetHandleFromMovesetID(getMovesetIDFromMovesetName(movesetName));
    }
}
