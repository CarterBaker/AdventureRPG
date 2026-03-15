package com.internal.bootstrap.itempipeline.tooltype;

import com.internal.core.engine.HandlePackage;

public class ToolTypeHandle extends HandlePackage {

    /*
     * Persistent reference to a loaded tool type definition. Registered and
     * owned by ToolTypeManager. Delegates all accessors through ToolTypeData.
     */

    // Internal
    private ToolTypeData toolTypeData;

    // Constructor \\

    public void constructor(ToolTypeData toolTypeData) {

        // Internal
        this.toolTypeData = toolTypeData;
    }

    // Accessible \\

    public ToolTypeData getToolTypeData() {
        return toolTypeData;
    }

    public String getToolTypeName() {
        return toolTypeData.getToolTypeName();
    }

    public short getToolTypeID() {
        return toolTypeData.getToolTypeID();
    }

    public String getDefaultModelPath() {
        return toolTypeData.getDefaultModelPath();
    }
}