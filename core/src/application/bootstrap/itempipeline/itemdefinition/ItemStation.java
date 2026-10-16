package application.bootstrap.itempipeline.itemdefinition;

public enum ItemStation {

    /*
     * The work an item standing in the world or aboard a vehicle is used for,
     * named in lower case by item data. Activating a station opens it in the
     * runtime, as activating a container opens its contents: a shipwright's
     * drafting table opens the table ships are drawn at. NONE marks an item
     * that is no station.
     */

    NONE,
    SHIPWRIGHT
}
