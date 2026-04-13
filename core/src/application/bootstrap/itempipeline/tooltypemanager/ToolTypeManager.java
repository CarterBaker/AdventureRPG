package application.bootstrap.itempipeline.tooltypemanager;

import application.bootstrap.itempipeline.tooltype.ToolTypeHandle;
import application.core.engine.ManagerPackage;
import application.core.util.RegistryUtility;
import it.unimi.dsi.fastutil.objects.Object2ShortOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;

public class ToolTypeManager extends ManagerPackage {

    /*
     * Owns the tool type palette for the engine lifetime. Detects and rejects
     * ID collisions on registration. Supports on-demand loading via
     * InternalLoader for tool types not yet in the palette at runtime.
     * TOOL_NONE (0) is a reserved sentinel meaning no tool required.
     */

    // Palette
    private Object2ShortOpenHashMap<String> toolTypeName2ToolTypeID;
    private Short2ObjectOpenHashMap<ToolTypeHandle> toolTypeID2ToolTypeHandle;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.toolTypeName2ToolTypeID = new Object2ShortOpenHashMap<>();
        this.toolTypeID2ToolTypeHandle = new Short2ObjectOpenHashMap<>();
        create(InternalLoader.class);
    }

    // Management \\

    void addToolType(ToolTypeHandle tool) {

        short id = tool.getToolTypeID();

        if (toolTypeID2ToolTypeHandle.containsKey(id)) {
            ToolTypeHandle existing = toolTypeID2ToolTypeHandle.get(id);
            if (RegistryUtility.isCollision(
                    tool.getToolTypeName(),
                    existing.getToolTypeName(),
                    id))
                throwException("ToolType ID collision: '"
                        + tool.getToolTypeName() + "' collides with '"
                        + existing.getToolTypeName()
                        + "' (ID " + id + ") — rename one to resolve");
        }

        toolTypeName2ToolTypeID.put(tool.getToolTypeName(), id);
        toolTypeID2ToolTypeHandle.put(id, tool);
    }

    // Accessible \\

    public boolean hasToolType(String toolTypeName) {
        return toolTypeName2ToolTypeID.containsKey(toolTypeName);
    }

    public short getToolTypeIDFromToolTypeName(String toolTypeName) {

        if (!toolTypeName2ToolTypeID.containsKey(toolTypeName))
            request(toolTypeName);

        return toolTypeName2ToolTypeID.getShort(toolTypeName);
    }

    public ToolTypeHandle getToolTypeHandleFromToolTypeID(short toolTypeID) {

        ToolTypeHandle handle = toolTypeID2ToolTypeHandle.get(toolTypeID);

        if (handle == null)
            throwException("ToolType ID not found: " + toolTypeID);

        return handle;
    }

    public ToolTypeHandle getToolTypeHandleFromToolTypeName(String toolTypeName) {
        return getToolTypeHandleFromToolTypeID(getToolTypeIDFromToolTypeName(toolTypeName));
    }

    public void request(String toolTypeName) {
        ((InternalLoader) internalLoader).request(toolTypeName);
    }
}