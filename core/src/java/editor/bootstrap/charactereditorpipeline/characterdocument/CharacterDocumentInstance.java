package editor.bootstrap.charactereditorpipeline.characterdocument;

import application.bootstrap.entitypipeline.race.RaceData;
import application.bootstrap.entitypipeline.race.RaceHandle;
import application.bootstrap.geometrypipeline.rig.RigHandle;
import engine.root.InstancePackage;
import engine.util.arpg.ArpgObjectStruct;

public class CharacterDocumentInstance extends InstancePackage {

    /*
     * One character open in the character editor: a race, by its name, whose
     * ARPG InfoManager owns; or a model from the workspace, a skinned mesh on
     * one of the game's rigs, by its name, previewed through a race built in
     * memory only (the template), which is null while the model is blank, its
     * body not yet started. The last preview built is kept, and the preview
     * revision moves every time a new one is built so viewports know to pose
     * it again; a refused rebuild keeps the last good preview and remembers
     * why it was refused.
     */

    // Identity
    private String raceName;

    // Model — the workspace model's name, its rig, and the race it is previewed through, null while blank
    private String modelName;
    private RigHandle modelRig;
    private ArpgObjectStruct templateArpg;

    // Preview
    private RaceHandle previewHandle;
    private String previewError;
    private int previewRevision;

    // Constructor \\

    public void constructor(String raceName) {

        // Identity
        this.raceName = raceName;
    }

    // A workspace model, previewed under the race name given once its body stands
    public void constructModel(String raceName, String modelName, RigHandle modelRig) {

        // Identity
        this.raceName = raceName;

        // Model
        this.modelName = modelName;
        this.modelRig = modelRig;
    }

    // Management \\

    // The race the model is previewed through, built again whenever its body changes
    public void setTemplate(ArpgObjectStruct templateArpg) {
        this.templateArpg = templateArpg;
    }

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

    // True for a workspace model rather than a race
    public boolean isModel() {
        return modelName != null;
    }

    // True for a workspace model whose body is not yet started, so there is nothing to preview
    public boolean isBlank() {
        return isModel() && templateArpg == null;
    }

    public String getModelName() {
        return modelName;
    }

    // The rig the character stands on: its preview's, or a blank model's own before it has one
    public RigHandle getRigHandle() {
        return hasPreview() ? getPreviewData().getRigHandle() : modelRig;
    }

    public ArpgObjectStruct getTemplateArpg() {
        return templateArpg;
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
