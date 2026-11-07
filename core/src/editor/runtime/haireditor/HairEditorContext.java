package editor.runtime.haireditor;

import editor.bootstrap.charactereditorpipeline.util.CharacterEditorMode;
import editor.bootstrap.charactereditorpipeline.util.CharacterEditorRegion;
import editor.runtime.charactereditor.CharacterModelingContext;

public class HairEditorContext extends CharacterModelingContext {

    /*
     * Editor tab for grooming the hair the previewed gender wears, framed on
     * the head: its cards picked, moved and sculpted with every tool, their
     * roots held to the scalp, so Nudge combs, Inflate puffs, Pinch clumps,
     * Smooth evens and Flatten lays the hair from the root out; Mesh shapes
     * and adds cards and Weights sets what they follow. A gender wearing no
     * hair has nothing to groom.
     */

    // Window \\

    @Override
    public CharacterEditorRegion getRegion() {
        return CharacterEditorRegion.HAIR;
    }

    @Override
    public CharacterEditorMode getStartingMode() {
        return CharacterEditorMode.SCULPT;
    }
}
