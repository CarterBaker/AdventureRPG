package editor.bootstrap.animationtreeeditorpipeline.treedocument;

import application.bootstrap.entitypipeline.animationtree.AnimationTreeHandle;
import application.bootstrap.geometrypipeline.rig.RigHandle;
import engine.root.InstancePackage;

public class TreeDocumentInstance extends InstancePackage {

    /*
     * One animation tree open in the animation tree editor: its name, the rig
     * its ARPG names, and the last preview tree built from that ARPG, which
     * InfoManager owns. The preview revision moves every time a new tree is
     * built so viewports put the character on it again. A tree edited into a
     * shape the builder refuses — a layer with no node yet, say — keeps
     * playing its last good preview and remembers why the new one was
     * refused.
     */

    // Identity
    private String treeName;

    // Preview
    private RigHandle rigHandle;
    private AnimationTreeHandle previewTree;
    private String previewError;
    private int previewRevision;

    // Constructor \\

    public void constructor(String treeName) {

        // Identity
        this.treeName = treeName;
    }

    // Management \\

    public void setPreview(AnimationTreeHandle previewTree) {

        this.rigHandle = previewTree.getRigHandle();
        this.previewTree = previewTree;
        this.previewError = null;
        this.previewRevision++;
    }

    public void refusePreview(RigHandle rigHandle, String previewError) {

        if (rigHandle != this.rigHandle) {
            this.rigHandle = rigHandle;
            this.previewTree = null;
            this.previewRevision++;
        }

        this.previewError = previewError;
    }

    // Accessible \\

    public String getTreeName() {
        return treeName;
    }

    public boolean hasRig() {
        return rigHandle != null;
    }

    public RigHandle getRigHandle() {
        return rigHandle;
    }

    public boolean hasPreviewTree() {
        return previewTree != null;
    }

    public AnimationTreeHandle getPreviewTree() {
        return previewTree;
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
