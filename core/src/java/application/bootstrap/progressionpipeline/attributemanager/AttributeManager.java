package application.bootstrap.progressionpipeline.attributemanager;

import application.bootstrap.progressionpipeline.attribute.AttributeHandle;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class AttributeManager extends ManagerPackage {

    /*
     * Owns the attribute palette: the letters of ADVENTURE every being spends
     * points into. IDs are assigned in registration order, which is also the
     * order attributes are listed in, and a name declared twice is rejected.
     * Every attribute is resolved in awake(), before any being spawns, so a
     * being's attribute values can be held in one array indexed by ID.
     */

    // Palette
    private Object2IntOpenHashMap<String> attributeName2AttributeID;
    private ObjectArrayList<AttributeHandle> attributeID2AttributeHandle;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.attributeName2AttributeID = RegistryUtility.createNameIndex();
        this.attributeID2AttributeHandle = RegistryUtility.createPalette();
        create(AttributeLoader.class);
    }

    @Override
    protected void awake() {
        internalLoader.requestAll();
    }

    // Management \\

    short registerAttributeName(String attributeName) {
        return (short) RegistryUtility.registerID(
                attributeName2AttributeID, attributeID2AttributeHandle, attributeName,
                EngineSetting.REGISTRY_SHORT_ID_COUNT);
    }

    void addAttributeHandle(AttributeHandle attributeHandle) {

        short attributeID = attributeHandle.getAttributeID();

        if (attributeID2AttributeHandle.get(attributeID) != null)
            throwException("Duplicate attribute name: '" + attributeHandle.getAttributeName()
                    + "' is declared more than once — every attribute name must be unique");

        attributeID2AttributeHandle.set(attributeID, attributeHandle);
    }

    // Accessible \\

    public boolean hasAttribute(String attributeName) {
        return RegistryUtility.getHandle(
                attributeName2AttributeID, attributeID2AttributeHandle, attributeName) != null;
    }

    public short getAttributeIDFromAttributeName(String attributeName) {

        if (!hasAttribute(attributeName) && internalLoader != null)
            ((AttributeLoader) internalLoader).request(attributeName);

        if (!hasAttribute(attributeName))
            throwException("Attribute not found after load: \"" + attributeName + "\"");

        return (short) attributeName2AttributeID.getInt(attributeName);
    }

    public AttributeHandle getAttributeHandleFromAttributeID(short attributeID) {

        AttributeHandle handle = RegistryUtility.getHandle(attributeID2AttributeHandle, attributeID);

        if (handle == null)
            throwException("Attribute ID not found: " + attributeID);

        return handle;
    }

    // Every attribute ID lies below this — the length of an array indexed by attribute ID
    public int getAttributeIDCount() {
        return attributeID2AttributeHandle.size();
    }
}
