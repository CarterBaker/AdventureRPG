package editor.runtime.charactereditor;

import editor.bootstrap.charactereditorpipeline.util.CharacterEditorMode;
import editor.runtime.EditorSetting;

public class CharacterPoseContext extends CharacterToolContext {

    /*
     * The Pose character tool tab, as a modeller's pose mode: the bones of the
     * character open turned about their joints, the mesh following, picked
     * here, in the Palette tab or with Pick, each turn typed or slid in the
     * Properties tab. The pose is held only to work in, never saved, and every
     * modelling tab shows it, so a thumb posed out of the way here stays out
     * of the way in the Mesh and Weights tabs until Reset Pose.
     */

    // Tool \\

    @Override
    public CharacterEditorMode getMode() {
        return CharacterEditorMode.POSE;
    }

    @Override
    protected String getTabTitle() {
        return EditorSetting.TAB_TITLE_CHARACTER_POSE;
    }
}
