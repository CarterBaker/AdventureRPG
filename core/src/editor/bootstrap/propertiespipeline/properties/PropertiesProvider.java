package editor.bootstrap.propertiespipeline.properties;

import editor.bootstrap.propertiespipeline.util.PropertiesContent;

public interface PropertiesProvider {

    /*
     * Names what the Properties tab shows while this window is the one worked
     * in last, which may change as the window changes mode. A window that
     * shows Rows supplies them as a CharacterViewRowSource.
     */

    PropertiesContent getPropertiesContent();
}
