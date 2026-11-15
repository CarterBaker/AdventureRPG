package application.bootstrap.dialoguepipeline.relationmanager;

import application.bootstrap.dialoguepipeline.relation.RelationHandle;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class RelationManager extends ManagerPackage {

    /*
     * Owns the relation palette: everything one being can be to another. IDs are
     * assigned in registration order, and a name declared twice is rejected. Every
     * relation is resolved in awake(), where a relation naming an inverse no file
     * declares is caught; the reserved ID stands for a stranger.
     */

    // Palette
    private Object2IntOpenHashMap<String> relationName2RelationID;
    private ObjectArrayList<RelationHandle> relationID2RelationHandle;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.relationName2RelationID = RegistryUtility.createNameIndex();
        this.relationID2RelationHandle = RegistryUtility.createPalette();
        create(RelationLoader.class);
    }

    @Override
    protected void awake() {

        internalLoader.requestAll();

        for (Object2IntMap.Entry<String> entry : relationName2RelationID.object2IntEntrySet())
            if (relationID2RelationHandle.get(entry.getIntValue()) == null)
                throwException("Relation \"" + entry.getKey() + "\" is named but no file declares it.");
    }

    // Management \\

    short registerRelationName(String relationName) {
        return (short) RegistryUtility.registerID(
                relationName2RelationID, relationID2RelationHandle, relationName,
                EngineSetting.REGISTRY_SHORT_ID_COUNT);
    }

    void addRelationHandle(RelationHandle relationHandle) {

        short relationID = relationHandle.getRelationID();

        if (relationID2RelationHandle.get(relationID) != null)
            throwException("Duplicate relation name: '" + relationHandle.getRelationName()
                    + "' is declared more than once — every relation name must be unique");

        relationID2RelationHandle.set(relationID, relationHandle);
    }

    // Accessible \\

    public boolean hasRelation(String relationName) {
        return RegistryUtility.getHandle(relationName2RelationID, relationID2RelationHandle, relationName) != null;
    }

    public short getRelationIDFromRelationName(String relationName) {

        if (!hasRelation(relationName) && internalLoader != null)
            ((RelationLoader) internalLoader).request(relationName);

        if (!hasRelation(relationName))
            throwException("Relation not found after load: \"" + relationName + "\"");

        return (short) relationName2RelationID.getInt(relationName);
    }

    public RelationHandle getRelationHandleFromRelationID(short relationID) {

        RelationHandle handle = RegistryUtility.getHandle(relationID2RelationHandle, relationID);

        if (handle == null)
            throwException("Relation ID not found: " + relationID);

        return handle;
    }

    public RelationHandle getRelationHandleFromRelationName(String relationName) {
        return getRelationHandleFromRelationID(getRelationIDFromRelationName(relationName));
    }

    // Every relation ID lies below this — the length of an array indexed by relation ID
    public int getRelationIDCount() {
        return relationID2RelationHandle.size();
    }
}
