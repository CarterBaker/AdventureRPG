package application.bootstrap.contentpipeline.util;

import engine.root.EngineSetting;
import engine.root.EngineUtility;

public class ContentUtility extends EngineUtility {

    /*
     * The content a cell holds is one short in its subchunk's content
     * palette: the content's ID, and NONE — zero — where the cell holds
     * nothing, since content ID zero is the registry's reserved sentinel.
     * The geometry meta word carries the same ID in the bits above the
     * quad's flags. Every system that reads or writes contents tests and
     * packs them through here.
     */

    // Settings
    public static final short NONE = EngineSetting.CONTENT_NONE;

    // Test \\

    public static boolean isContained(short contentID) {
        return contentID != NONE;
    }

    // Meta \\

    // The content's bits of a geometry meta word, the ID shifted above the quad's flags
    public static int toMetaBits(short contentID, int contentShift) {
        return (contentID & EngineSetting.CONTENT_ID_MASK) << contentShift;
    }
}
