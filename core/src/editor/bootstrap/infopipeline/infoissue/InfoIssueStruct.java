package editor.bootstrap.infopipeline.infoissue;

import editor.bootstrap.infopipeline.util.InfoIssueSeverity;
import engine.root.StructPackage;

public class InfoIssueStruct extends StructPackage {

    /*
     * One thing a content file holds that the game would not take as it is:
     * the entry it lies in, the row path of the value inside that entry, empty
     * for the entry as a whole, what is wrong with it, written for the user,
     * and whether it stops the file being saved or is only ignored.
     */

    // Location
    private final String entryName;
    private final String path;

    // Message
    private final String message;
    private final InfoIssueSeverity severity;

    // Constructor \\

    public InfoIssueStruct(String entryName, String path, String message, InfoIssueSeverity severity) {

        // Location
        this.entryName = entryName;
        this.path = path;

        // Message
        this.message = message;
        this.severity = severity;
    }

    // Accessible \\

    public String getEntryName() {
        return entryName;
    }

    public String getPath() {
        return path;
    }

    public String getMessage() {
        return message;
    }

    public InfoIssueSeverity getSeverity() {
        return severity;
    }

    public boolean isError() {
        return severity == InfoIssueSeverity.ERROR;
    }
}
