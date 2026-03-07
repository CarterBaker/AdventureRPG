package com.internal.bootstrap.itempipeline.tooltypemanager;

import com.internal.bootstrap.itempipeline.tooltype.ToolTypeHandle;
import com.internal.core.engine.ManagerPackage;
import com.internal.core.util.RegistryUtility;

import it.unimi.dsi.fastutil.objects.Object2ShortOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;

public class ToolTypeManager extends ManagerPackage {

    // Sentinel — no tool required
    public static final short TOOL_NONE = 0;

    // Retrieval Mapping
    private Object2ShortOpenHashMap<String> toolTypeName2ID;
    private Short2ObjectOpenHashMap<ToolTypeHandle> toolTypeID2Handle;

    // Base \\

    @Override
    protected void create() {
        create(InternalLoader.class);
        this.toolTypeName2ID = new Object2ShortOpenHashMap<>();
        this.toolTypeID2Handle = new Short2ObjectOpenHashMap<>();
    }

    // On-Demand Loading \\

    public void request(String toolTypeName) {
        ((InternalLoader) internalLoader).request(toolTypeName);
    }

    // Tool Management \\

    void addToolType(ToolTypeHandle tool) {
        if (toolTypeID2Handle.containsKey(tool.getToolTypeID())) {
            ToolTypeHandle existing = toolTypeID2Handle.get(tool.getToolTypeID());
            if (RegistryUtility.isCollision(
                    tool.getToolTypeName(), existing.getToolTypeName(), tool.getToolTypeID()))
                throwException("ToolType ID collision: '"
                        + tool.getToolTypeName() + "' collides with '"
                        + existing.getToolTypeName()
                        + "' (ID " + tool.getToolTypeID() + ") — rename one to resolve");
        }
        toolTypeName2ID.put(tool.getToolTypeName(), tool.getToolTypeID());
        toolTypeID2Handle.put(tool.getToolTypeID(), tool);
    }

    // Accessible \\

    public short getToolTypeIDFromName(String toolTypeName) {
        if (!toolTypeName2ID.containsKey(toolTypeName))
            request(toolTypeName);
        return toolTypeName2ID.getShort(toolTypeName);
    }

    public ToolTypeHandle getToolTypeFromID(short toolTypeID) {
        ToolTypeHandle handle = toolTypeID2Handle.get(toolTypeID);
        if (handle == null)
            throwException("ToolType ID not found: " + toolTypeID);
        return handle;
    }

    public boolean hasToolType(String toolTypeName) {
        return toolTypeName2ID.containsKey(toolTypeName);
    }
}