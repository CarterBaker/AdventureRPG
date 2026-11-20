package editor.runtime.characterview.render;

import engine.util.mathematics.matrices.Matrix4;

public interface CharacterBoneSource {

    /*
     * The bones a character viewport draws, read the way the viewport's tool
     * poses them: a posed character's rig, or a rig being edited at its bind
     * joints. Bones are by their index in that rig, parents before children.
     */

    int getBoneCount();

    // The bone's parent, -1 for a root
    int getBoneParent(int boneIndex);

    String getBoneName(int boneIndex);

    // Where the bone's joint stands in the world
    Matrix4 getBoneWorldMatrix(int boneIndex);

    // How wide the bone is, in blocks
    float getBoneSize(int boneIndex);

    // Whether the bone is drawn at all, as the region worked on shows it
    boolean isBoneShown(int boneIndex);

    // Whether the bone is a part's own where the rig marks the part as taking the face
    boolean isFacialPartBone(int boneIndex);

    // The bone selected, -1 for none
    int getSelectedBone();
}
