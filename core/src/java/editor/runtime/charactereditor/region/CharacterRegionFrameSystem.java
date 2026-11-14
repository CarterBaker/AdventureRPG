package editor.runtime.charactereditor.region;

import application.bootstrap.geometrypipeline.mesh.RiggedMeshStruct;
import application.bootstrap.geometrypipeline.rig.RigHandle;
import editor.bootstrap.charactereditorpipeline.charactereditormanager.CharacterEditorManager;
import editor.bootstrap.charactereditorpipeline.charactermesh.CharacterMeshDocumentInstance;
import editor.bootstrap.charactereditorpipeline.util.CharacterEditorRegion;
import editor.runtime.EditorSetting;
import editor.runtime.charactereditor.mode.CharacterEditorModeSystem;
import editor.runtime.characterview.pose.CharacterViewPoseSystem;
import editor.runtime.characterview.util.CharacterPoseUtility;
import editor.runtime.viewcamera.ViewCameraSubject;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.booleans.BooleanArrayList;

public class CharacterRegionFrameSystem extends SystemPackage implements ViewCameraSubject {

    /*
     * What the Character Editor window's view camera frames on every layer:
     * the whole character for the whole region, otherwise the region's frame
     * bone and every bone below it — the head with its face, ears, eyes and
     * hair, the hand shown with its fingers, the foot shown with its toes,
     * the ear on its side, the eye shown in its socket, the jaw with the
     * mouth. While the region's part is open the frame is the box of every
     * vertex those bones carry at least the pick share of, worked out again
     * only once the mesh changes; otherwise it is about the bones' joints as
     * the preview stands. The radius reaches past the farthest point by a
     * margin, never less than a set least.
     */

    // Internal
    private CharacterEditorManager characterEditorManager;
    private CharacterViewPoseSystem characterViewPoseSystem;
    private CharacterEditorModeSystem characterEditorModeSystem;

    // Frame
    private Vector3 center;
    private float radius;
    private boolean framed;
    private CharacterMeshDocumentInstance framedDocument;
    private int framedRevision;
    private int framedBone;

    // Scratch
    private BooleanArrayList chainBones;
    private Vector3 joint;
    private Vector3 low;
    private Vector3 high;

    // Base \\

    @Override
    protected void create() {

        // Frame
        this.center = new Vector3();

        // Scratch
        this.chainBones = new BooleanArrayList();
        this.joint = new Vector3();
        this.low = new Vector3();
        this.high = new Vector3();
    }

    @Override
    protected void get() {
        this.characterEditorManager = get(CharacterEditorManager.class);
        this.characterViewPoseSystem = get(CharacterViewPoseSystem.class);
        this.characterEditorModeSystem = get(CharacterEditorModeSystem.class);
    }

    // Update \\

    @Override
    protected void update() {

        CharacterEditorRegion region = characterEditorModeSystem.getRegion();
        int frameBone = !characterViewPoseSystem.hasSubject()
                ? EngineSetting.INDEX_NOT_FOUND
                : characterEditorManager.getRegionFrameBone(region, characterViewPoseSystem.getRaceData().getRigHandle());

        if (frameBone == EngineSetting.INDEX_NOT_FOUND || characterViewPoseSystem.getAnimationStateHandle() == null) {
            this.framed = false;
            this.framedDocument = null;
            return;
        }

        if (characterEditorManager.isRegionOpen(region)) {
            frameMesh(characterEditorManager.getActiveMeshDocument(), frameBone);
            return;
        }

        this.framedDocument = null;
        this.framed = true;
        frameChain(characterViewPoseSystem.getRaceData().getRigHandle(), frameBone);
    }

    // The box of every vertex the frame bone and the bones below it carry, once per change of the mesh or the bone
    private void frameMesh(CharacterMeshDocumentInstance document, int frameBone) {

        if (document == framedDocument && document.getRevision() == framedRevision && frameBone == framedBone)
            return;

        this.framedDocument = document;
        this.framedRevision = document.getRevision();
        this.framedBone = frameBone;

        RiggedMeshStruct mesh = document.getMesh();

        resolveChain(mesh.getRigHandle(), frameBone);
        low.set(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE);
        high.set(-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE);

        boolean found = false;

        for (int v = 0; v < mesh.getVertexCount(); v++) {

            float share = 0f;

            for (int i = 0; i < mesh.getInfluenceCount(v); i++)
                if (chainBones.getBoolean(mesh.getInfluenceBone(v, i)))
                    share += mesh.getInfluenceWeight(v, i);

            if (share < EditorSetting.CHARACTER_REGION_PICK_SHARE)
                continue;

            low.set(Math.min(low.x, mesh.getX(v)), Math.min(low.y, mesh.getY(v)), Math.min(low.z, mesh.getZ(v)));
            high.set(Math.max(high.x, mesh.getX(v)), Math.max(high.y, mesh.getY(v)), Math.max(high.z, mesh.getZ(v)));
            found = true;
        }

        this.framed = found;

        if (!found)
            return;

        center.set(low).add(high).divide(2f);
        this.radius = Math.max(
                high.subtract(low).length() * EditorSetting.CHARACTER_REGION_FRAME_SIZE_SHARE
                        * EditorSetting.CHARACTER_REGION_FRAME_MARGIN,
                EditorSetting.CHARACTER_REGION_FRAME_MIN_RADIUS);
    }

    // Marks the bone and every bone below it, parents standing before children
    private void resolveChain(RigHandle rigHandle, int root) {

        chainBones.clear();

        for (int i = 0; i < rigHandle.getBoneCount(); i++) {

            int parent = rigHandle.getBone(i).getParentIndex();
            chainBones.add(i == root || (parent != EngineSetting.INDEX_NOT_FOUND && chainBones.getBoolean(parent)));
        }
    }

    // The frame bone and every bone below it framed about their joints as the preview stands
    private void frameChain(RigHandle rigHandle, int root) {

        int count = 0;

        resolveChain(rigHandle, root);
        center.set(0f, 0f, 0f);

        for (int i = 0; i < chainBones.size(); i++)
            if (chainBones.getBoolean(i)) {
                resolveJoint(i);
                center.add(joint);
                count++;
            }

        center.divide(count);

        float farthest = 0f;

        for (int i = 0; i < chainBones.size(); i++)
            if (chainBones.getBoolean(i)) {
                resolveJoint(i);
                farthest = Math.max(farthest, joint.subtract(center).length());
            }

        Vector3 size = rigHandle.getBone(root).getSize();
        float reach = Math.max(farthest, Math.max(size.x, Math.max(size.y, size.z))
                * EditorSetting.CHARACTER_REGION_FRAME_SIZE_SHARE);

        this.radius = Math.max(reach * EditorSetting.CHARACTER_REGION_FRAME_MARGIN,
                EditorSetting.CHARACTER_REGION_FRAME_MIN_RADIUS);
    }

    private void resolveJoint(int boneIndex) {
        CharacterPoseUtility.getTranslation(
                characterViewPoseSystem.getAnimationStateHandle().getWorldMatrix(boneIndex),
                joint);
    }

    // Frame \\

    @Override
    public boolean isShown() {
        return characterViewPoseSystem.isShown();
    }

    @Override
    public float getCenter(int axis) {

        if (!framed)
            return characterViewPoseSystem.getCenter(axis);

        return switch (axis) {
            case EngineSetting.AXIS_X -> center.x;
            case EngineSetting.AXIS_Y -> center.y;
            default -> center.z;
        };
    }

    @Override
    public float getBoundingRadius() {
        return framed ? radius : characterViewPoseSystem.getBoundingRadius();
    }
}
