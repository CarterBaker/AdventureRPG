package application.bootstrap.itempipeline.tooltypemanager;

import application.bootstrap.itempipeline.tooltype.ToolTypeHandle;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ToolTypeManager extends ManagerPackage {

    /*
     * Owns the tool type palette for the engine lifetime, assigning IDs in
     * registration order and rejecting a tool type name declared twice.
     * Supports on-demand loading via ToolTypeLoader for tool types not yet in
     * the palette at runtime. TOOL_NONE (0) is the reserved sentinel meaning
     * no tool required.
     */

    // Palette
    private Object2IntOpenHashMap<String> toolTypeName2ToolTypeID;
    private ObjectArrayList<ToolTypeHandle> toolTypeID2ToolTypeHandle;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.toolTypeName2ToolTypeID = RegistryUtility.createNameIndex();
        this.toolTypeID2ToolTypeHandle = RegistryUtility.createPalette();
        create(ToolTypeLoader.class);
    }

    // Management \\

    short registerToolTypeName(String toolTypeName) {
        return (short) RegistryUtility.registerID(
                toolTypeName2ToolTypeID, toolTypeID2ToolTypeHandle, toolTypeName,
                EngineSetting.REGISTRY_SHORT_ID_COUNT);
    }

    void addToolType(ToolTypeHandle tool) {

        short id = tool.getToolTypeID();

        if (toolTypeID2ToolTypeHandle.get(id) != null)
            throwException("Duplicate tool type name: '" + tool.getToolTypeName()
                    + "' is declared more than once — every tool type name must be unique");

        toolTypeID2ToolTypeHandle.set(id, tool);
    }

    // Accessible \\

    public boolean hasToolType(String toolTypeName) {
        return RegistryUtility.getHandle(toolTypeName2ToolTypeID, toolTypeID2ToolTypeHandle, toolTypeName) != null;
    }

    public short getToolTypeIDFromToolTypeName(String toolTypeName) {

        if (!hasToolType(toolTypeName))
            request(toolTypeName);

        if (!hasToolType(toolTypeName))
            throwException("ToolType not found after load: \"" + toolTypeName + "\"");

        return (short) toolTypeName2ToolTypeID.getInt(toolTypeName);
    }

    public ToolTypeHandle getToolTypeHandleFromToolTypeID(short toolTypeID) {

        ToolTypeHandle handle = RegistryUtility.getHandle(toolTypeID2ToolTypeHandle, toolTypeID);

        if (handle == null)
            throwException("ToolType ID not found: " + toolTypeID);

        return handle;
    }

    public ToolTypeHandle getToolTypeHandleFromToolTypeName(String toolTypeName) {
        return getToolTypeHandleFromToolTypeID(getToolTypeIDFromToolTypeName(toolTypeName));
    }

    public void request(String toolTypeName) {
        ((ToolTypeLoader) internalLoader).request(toolTypeName);
    }
}
