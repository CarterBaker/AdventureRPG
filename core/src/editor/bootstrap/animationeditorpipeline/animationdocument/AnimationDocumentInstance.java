package editor.bootstrap.animationeditorpipeline.animationdocument;

import application.bootstrap.animationpipeline.animation.AnimationClipHandle;
import application.bootstrap.geometrypipeline.rig.RigHandle;
import engine.root.InstancePackage;

public class AnimationDocumentInstance extends InstancePackage {

    /*
     * One animation clip open in the animation editor: its name, the rig its
     * ARPG names, and the last preview clip built from that ARPG, which
     * InfoManager owns. The preview revision moves every time it is rebuilt.
     * A refused rebuild — a clip with no keyframe past its start yet, say —
     * drops the clip rather than keep one that no longer matches the ARPG,
     * and remembers why; the rig stays when it could still be resolved.
     */

    // Identity
    private String clipName;

    // Preview
    private RigHandle rigHandle;
    private AnimationClipHandle previewClip;
    private String previewError;
    private int previewRevision;

    // Constructor \\

    public void constructor(String clipName) {

        // Identity
        this.clipName = clipName;
    }

    // Management \\

    public void setPreview(RigHandle rigHandle, AnimationClipHandle previewClip) {

        this.rigHandle = rigHandle;
        this.previewClip = previewClip;
        this.previewError = null;
        this.previewRevision++;
    }

    public void refusePreview(RigHandle rigHandle, String previewError) {

        this.rigHandle = rigHandle;
        this.previewClip = null;
        this.previewError = previewError;
        this.previewRevision++;
    }

    // Accessible \\

    public String getClipName() {
        return clipName;
    }

    public boolean hasRig() {
        return rigHandle != null;
    }

    public RigHandle getRigHandle() {
        return rigHandle;
    }

    public boolean hasPreviewClip() {
        return previewClip != null;
    }

    public AnimationClipHandle getPreviewClip() {
        return previewClip;
    }

    public float getDuration() {
        return previewClip != null ? previewClip.getDuration() : 0f;
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
