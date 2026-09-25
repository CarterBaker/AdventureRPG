package editor.bootstrap.infopipeline.infoschema;

import engine.root.HandlePackage;

public class InfoSchemaHandle extends HandlePackage {

    /*
     * Persistent reference to one loaded info schema. Registered and owned by
     * InfoManager. Delegates all accessors through InfoSchemaData.
     */

    // Internal
    private InfoSchemaData infoSchemaData;

    // Constructor \\

    public void constructor(InfoSchemaData infoSchemaData) {

        // Internal
        this.infoSchemaData = infoSchemaData;
    }

    // Accessible \\

    public InfoSchemaData getInfoSchemaData() {
        return infoSchemaData;
    }

    public String getSchemaName() {
        return infoSchemaData.getSchemaName();
    }

    public String getTabName() {
        return infoSchemaData.getTabName();
    }

    public int getOrder() {
        return infoSchemaData.getOrder();
    }

    public String getDirectory() {
        return infoSchemaData.getDirectory();
    }

    public String getEntriesKey() {
        return infoSchemaData.getEntriesKey();
    }

    public String getNameField() {
        return infoSchemaData.getNameField();
    }

    public String getDefaultFile() {
        return infoSchemaData.getDefaultFile();
    }

    public InfoFieldStruct getRootField() {
        return infoSchemaData.getRootField();
    }

    public boolean isArrayLayout() {
        return infoSchemaData.isArrayLayout();
    }
}
