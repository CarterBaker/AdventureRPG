package editor.bootstrap.charactereditorpipeline.characterdocument;

import application.bootstrap.entitypipeline.entity.EntityData;
import application.bootstrap.entitypipeline.entity.EntityHandle;
import engine.root.InstancePackage;

public class CharacterDocumentInstance extends InstancePackage {

    /*
     * One entity template open in the character editor: its name and the
     * last preview built from its ARPG, which InfoManager owns. The preview
     * revision moves every time a new preview is built so viewports know to
     * pose it again; a refused rebuild keeps the last good preview and
     * remembers why it was refused.
     */

    // Identity
    private String templateName;

    // Preview
    private EntityHandle previewHandle;
    private String previewError;
    private int previewRevision;

    // Constructor \\

    public void constructor(String templateName) {

        // Identity
        this.templateName = templateName;
    }

    // Management \\

    public void setPreview(EntityHandle previewHandle) {

        this.previewHandle = previewHandle;
        this.previewError = null;
        this.previewRevision++;
    }

    public void refusePreview(String previewError) {
        this.previewError = previewError;
    }

    // Accessible \\

    public String getTemplateName() {
        return templateName;
    }

    public boolean hasPreview() {
        return previewHandle != null;
    }

    public EntityData getPreviewData() {
        return previewHandle.getEntityData();
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
