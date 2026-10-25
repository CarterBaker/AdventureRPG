package editor.bootstrap.infopipeline.util;

public enum InfoResourceNaming {

    /*
     * How the game names the files of a resource directory. STEM is the path
     * below the directory without its extension, as meshes are named; FILE is
     * the path with its extension, as shader sources are named; TILE is the
     * folder path and the file name before its first underscore, as texture
     * tiles are named from their alias images.
     */

    STEM("stem"),
    FILE("file"),
    TILE("tile");

    // Internal
    private final String schemaName;

    // Constructor \\

    InfoResourceNaming(String schemaName) {
        this.schemaName = schemaName;
    }

    // Utility \\

    public static InfoResourceNaming fromSchemaName(String schemaName) {

        for (InfoResourceNaming naming : values())
            if (naming.schemaName.equals(schemaName))
                return naming;

        return null;
    }

    // Accessible \\

    public String getSchemaName() {
        return schemaName;
    }
}
