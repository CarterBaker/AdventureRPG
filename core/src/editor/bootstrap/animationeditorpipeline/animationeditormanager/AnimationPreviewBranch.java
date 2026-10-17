package editor.bootstrap.animationeditorpipeline.animationeditormanager;

import application.bootstrap.animationpipeline.animationmanager.AnimationManager;
import application.bootstrap.geometrypipeline.rig.RigHandle;
import application.bootstrap.geometrypipeline.rigmanager.RigManager;
import editor.bootstrap.animationeditorpipeline.animationdocument.AnimationDocumentInstance;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import engine.root.UtilityPackage.InternalException;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;

class AnimationPreviewBranch extends BranchPackage {

    /*
     * Builds the animation editor's preview of a clip from its ARPG as it
     * stands, saved or not, through the same builder the game loads clips
     * with, without registering it. A clip the builder refuses — one with
     * no keyframe past its start yet, say — previews as its rig's bind pose
     * and says why in the editor's status.
     */

    // Internal
    private RigManager rigManager;
    private AnimationManager animationManager;

    // Base \\

    @Override
    protected void get() {
        this.rigManager = get(RigManager.class);
        this.animationManager = get(AnimationManager.class);
    }

    // Preview \\

    void rebuild(AnimationDocumentInstance document, ArpgObjectStruct clipArpg) {

        RigHandle rigHandle = resolveRig(clipArpg);

        try {
            document.setPreview(rigHandle, animationManager.buildPreviewClip(document.getClipName(), clipArpg));
        } catch (InternalException e) {
            document.refusePreview(rigHandle, e.getMessage());
        }
    }

    private RigHandle resolveRig(ArpgObjectStruct clipArpg) {

        try {
            return rigManager.getRigHandleFromRigName(
                    ArpgUtility.getString(clipArpg, EditorSetting.ANIMATION_EDITOR_FIELD_RIG, ""));
        } catch (InternalException e) {
            return null;
        }
    }
}
