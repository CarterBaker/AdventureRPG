package program.bootstrap.itempipeline.tooltype;

import program.core.engine.DataPackage;

public class ToolTypeData extends DataPackage {

    /*
     * Immutable tool type definition loaded from JSON. Holds identity and
     * default model path for one tool type. Owned by ToolTypeHandle for
     * the engine lifetime.
     */

    // Identity
    private final String toolTypeName;
    private final short toolTypeID;

    // Model
    private final String defaultModelPath;

    // Constructor \\

    public ToolTypeData(
            String toolTypeName,
            short toolTypeID,
            String defaultModelPath) {

        // Identity
        this.toolTypeName = toolTypeName;
        this.toolTypeID = toolTypeID;

        // Model
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