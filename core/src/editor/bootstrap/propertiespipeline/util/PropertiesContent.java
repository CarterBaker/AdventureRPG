package editor.bootstrap.propertiespipeline.util;

public enum PropertiesContent {

    /*
     * What the Properties tab shows for the window worked in last: Rows, the
     * value rows of a character, face or animation window, each number on a
     * slider; or Info, the content entry selected in the Hierarchy with every
     * field to edit. The ordinal indexes the tab's views, so the order is fixed.
     */

    ROWS,
    INFO;

    // Values
    public static final PropertiesContent[] VALUES = values();
}
