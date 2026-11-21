package editor.runtime.characterview.gizmo;

import editor.bootstrap.charactereditorpipeline.util.CharacterEditorTransform;
import engine.util.mathematics.vectors.Vector3;

public interface CharacterGizmoSource {

    /*
     * A character viewport's system whose selection the transform gizmo
     * stands on, added to the CharacterGizmoSystem as it wakes: the
     * transform its tool drags while nothing is under way, null while its
     * mode is not open, its tool drags none or a drag is already held, and
     * where the selection stands in the view.
     */

    CharacterEditorTransform getGizmoTransform();

    // Where the gizmo stands; false with nothing selected
    boolean resolveGizmoPivot(Vector3 pivot);
}
