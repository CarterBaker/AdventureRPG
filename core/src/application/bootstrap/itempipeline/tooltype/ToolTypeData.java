package application.bootstrap.itempipeline.tooltype;

import engine.root.DataPackage;

public class ToolTypeData extends DataPackage {

    /*
     * Immutable tool type definition loaded from ARPG. Holds identity, the
     * default model path for one tool type — the mesh a tool item of this
     * type is drawn with unless it names its own — and how the tool is swung.
     * Owned by ToolTypeHandle for the engine lifetime.
     */

    // Identity
    private final String toolTypeName;
    private final String localName;
    private final short toolTypeID;

    // Model
    private final String defaultModelPath;

    // Swing
    private final ToolSwing swing;

    // Constructor \\

    public ToolTypeData(
            String toolTypeName,
            String localName,
            short toolTypeID,
            String defaultModelPath,
            ToolSwing swing) {

        // Identity
        this.toolTypeName = toolTypeName;
        this.localName = localName;
        this.toolTypeID = toolTypeID;

        // Model
        this.defaultModelPath = defaultModelPath;

        // Swing
        this.swing = swing;
    }

    // Accessible \\

    public String getToolTypeName() {
        return toolTypeName;
    }

    public String getLocalName() {
        return localName;
    }

    public short getToolTypeID() {
        return toolTypeID;
    }

    public String getDefaultModelPath() {
        return defaultModelPath;
    }

    public ToolSwing getSwing() {
        return swing;
    }
}
