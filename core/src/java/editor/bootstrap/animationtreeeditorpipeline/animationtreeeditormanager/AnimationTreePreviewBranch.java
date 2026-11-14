package editor.bootstrap.animationtreeeditorpipeline.animationtreeeditormanager;

import application.bootstrap.entitypipeline.animationtreemanager.AnimationTreeManager;
import application.bootstrap.geometrypipeline.rig.RigHandle;
import application.bootstrap.geometrypipeline.rigmanager.RigManager;
import editor.bootstrap.animationtreeeditorpipeline.treedocument.TreeDocumentInstance;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import engine.root.UtilityPackage.InternalException;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;

class AnimationTreePreviewBranch extends BranchPackage {

    /*
     * Builds the animation tree editor's preview of a tree from its ARPG as
     * it stands, saved or not, through the same builder the game loads trees
     * with, without registering it. Its clips resolve as the game loads them,
     * from their saved files. A tree the builder refuses keeps playing its
     * last good preview while it names the same rig, and says why in the
     * editor's status.
     */

    // Internal
    private RigManager rigManager;
    private AnimationTreeManager animationTreeManager;

    // Base \\

    @Override
    protected void get() {
        this.rigManager = get(RigManager.class);
        this.animationTreeManager = get(AnimationTreeManager.class);
    }

    // Preview \\

    void rebuild(TreeDocumentInstance document, ArpgObjectStruct treeArpg) {

        try {
            document.setPreview(animationTreeManager.buildPreviewTree(document.getTreeName(), treeArpg));
        } catch (InternalException e) {
            document.refusePreview(resolveRig(treeArpg), e.getMessage());
        }
    }

    private RigHandle resolveRig(ArpgObjectStruct treeArpg) {

        try {
            return rigManager.getRigHandleFromRigName(
                    ArpgUtility.getString(treeArpg, EditorSetting.ANIMATION_TREE_EDITOR_FIELD_RIG, ""));
        } catch (InternalException e) {
            return null;
        }
    }
}
