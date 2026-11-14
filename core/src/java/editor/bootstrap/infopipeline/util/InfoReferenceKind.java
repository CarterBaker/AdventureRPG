package editor.bootstrap.infopipeline.util;

public enum InfoReferenceKind {

    /*
     * What a string field names. SCHEMA names an entry of another schema, by
     * the name the game registers it under; RESOURCE names a file the game
     * reads from an asset directory; LOCAL names something inside the same
     * entry, such as another bone of the same rig.
     */

    SCHEMA,
    RESOURCE,
    LOCAL
}
