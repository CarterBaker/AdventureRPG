package editor.bootstrap.charactereditorpipeline.util;

import application.bootstrap.geometrypipeline.rig.RigHandle;
import application.bootstrap.geometrypipeline.rig.RigPartStruct;
import editor.runtime.EditorSetting;

public enum CharacterBoneKind {

    /*
     * What a bone of a character's rig is for, so each kind is drawn in its
     * own colour as Blender's bone colours set them apart: Hair for every
     * lock bone the hair prefix names, Face for every bone below a part the
     * rig marks as taking the face (the jaw, lips, cheeks, brows, eyes and
     * ears under the head), and Body for the rest. Each names its row key,
     * its label and the colour it starts in, an index into
     * BONE_DISPLAY_COLOR_NAMES.
     */

    BODY(EditorSetting.BONE_DISPLAY_KEY_BODY, EditorSetting.BONE_DISPLAY_LABEL_BODY,
            EditorSetting.BONE_DISPLAY_DEFAULT_BODY),
    FACE(EditorSetting.BONE_DISPLAY_KEY_FACE, EditorSetting.BONE_DISPLAY_LABEL_FACE,
            EditorSetting.BONE_DISPLAY_DEFAULT_FACE),
    HAIR(EditorSetting.BONE_DISPLAY_KEY_HAIR, EditorSetting.BONE_DISPLAY_LABEL_HAIR,
            EditorSetting.BONE_DISPLAY_DEFAULT_HAIR);

    // Values
    public static final CharacterBoneKind[] VALUES = values();

    // Internal
    private final String key;
    private final String label;
    private final int defaultColor;

    // Constructor \\

    CharacterBoneKind(String key, String label, int defaultColor) {
        this.key = key;
        this.label = label;
        this.defaultColor = defaultColor;
    }

    // Utility \\

    // Whether a bone, by name, is a part's own bone where the rig marks the part as taking the face
    public static boolean isFacialPartBone(RigHandle rigHandle, String boneName) {

        for (int p = 0; p < rigHandle.getPartCount(); p++) {

            RigPartStruct part = rigHandle.getPart(p);

            if (!part.takesFace())
                continue;

            int[] bones = part.getBones();

            for (int i = 0; i < bones.length; i++)
                if (rigHandle.getBone(bones[i]).getName().equals(boneName))
                    return true;
        }

        return false;
    }

    // Accessible \\

    public String getKey() {
        return key;
    }

    public String getLabel() {
        return label;
    }

    public int getDefaultColor() {
        return defaultColor;
    }
}
