package editor.bootstrap.charactereditorpipeline.characterdocument;

import application.bootstrap.entitypipeline.race.RaceData;
import application.bootstrap.entitypipeline.race.RaceHandle;
import engine.root.InstancePackage;

public class CharacterDocumentInstance extends InstancePackage {

    /*
     * One race open in the character editor: its name and the last preview
     * built from its ARPG, which InfoManager owns. The preview
     * revision moves every time a new preview is built so viewports know to
     * pose it again; a refused rebuild keeps the last good preview and
     * remembers why it was refused.
     */

    // Identity
    private String raceName;

    // Preview
    private RaceHandle previewHandle;
    private String previewError;
    private int previewRevision;

    // Constructor \\

    public void constructor(String raceName) {

        // Identity
        this.raceName = raceName;
    }

    // Management \\

    public void setPreview(RaceHandle previewHandle) {

        this.previewHandle = previewHandle;
        this.previewError = null;
        this.previewRevision++;
    }

    public void refusePreview(String previewError) {
        this.previewError = previewError;
    }

    // Accessible \\

    public String getRaceName() {
        return raceName;
    }

    public boolean hasPreview() {
        return previewHandle != null;
    }

    public RaceData getPreviewData() {
        return previewHandle.getRaceData();
    }

    public boolean hasPreviewError() {
        return previewError != null;
    }

    public String getPreviewError() {
        return previewError;
    }

    public int getPreviewRevision() {
        return previewRevision;
    }
}
