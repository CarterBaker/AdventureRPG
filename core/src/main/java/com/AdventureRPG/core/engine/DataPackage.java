package com.AdventureRPG.core.engine;

public class DataPackage extends EngineUtility {

    /*
     * DataPackages are intended to be used as a temporary data storage
     * container designed specifically for use within the internal boot
     * kernel of the engine itself. Typically the data contained within
     * is used to create a permanant HandlePackage.
     */

    // Internal
    public final String name;
    public final int ID;

    public DataPackage(
            String name,
            int ID) {

        // Internal
        this.name = name;
        this.ID = ID;
    }
}
