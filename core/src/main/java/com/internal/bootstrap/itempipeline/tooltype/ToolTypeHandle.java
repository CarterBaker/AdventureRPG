package com.internal.bootstrap.itempipeline.tooltype;

import com.internal.core.engine.HandlePackage;

public class ToolTypeHandle extends HandlePackage {

    // Identity
    private String toolTypeName;
    private short toolTypeID;

    // Model
    private String defaultModelPath;

    // Constructor \\

    public void constructor(String toolTypeName, short toolTypeID, String defaultModelPath) {
        this.toolTypeName = toolTypeName;
        this.toolTypeID = toolTypeID;
        this.defaultModelPath = defaultModelPath;
    }

    // Accessible \\

    public String getToolTypeName() {
        return toolTypeName;
    }

    public short getToolTypeID() {
        return toolTypeID;
    }

    public String getDefaultModelPath() {
        return defaultModelPath;
    }
}