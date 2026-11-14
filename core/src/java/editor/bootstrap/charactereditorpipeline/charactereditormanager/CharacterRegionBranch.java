package editor.bootstrap.charactereditorpipeline.charactereditormanager;

import application.bootstrap.geometrypipeline.mesh.RiggedMeshStruct;
import application.bootstrap.geometrypipeline.rig.RigData;
import application.bootstrap.geometrypipeline.rig.RigHandle;
import editor.bootstrap.charactereditorpipeline.charactermesh.CharacterMeshDocumentInstance;
import editor.bootstrap.charactereditorpipeline.util.CharacterEditorRegion;
import editor.bootstrap.charactereditorpipeline.util.CharacterEditorSide;
import editor.bootstrap.charactereditorpipeline.util.CharacterMeshUtility;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.booleans.BooleanArrayList;
import it.unimi.dsi.fastutil.floats.FloatArrayList;

class CharacterRegionBranch extends BranchPackage {

    /*
     * How far each vertex of a mesh being modelled belongs to a region, the
     * share every pick, box, brush, transform and mirror of a focused tab is
     * held to. On the body, a region's share is the weight the vertex puts on
     * the bones the rig names for the region's part and every bone below
     * them, so a stroke fades out across the wrist, the ankle or the edge of
     * the socket; a vertex weighing at least the pick share on them is picked
     * and selected there. The region's own worn feature is held whole, but a
     * root-pinned one's share is how far along its card the vertex stands,
     * root to tip, so a stroke leaves the roots on the scalp. A mesh that is
     * not a part the region edits holds none of it (a negative share). Shares
     * are kept per region and worked out again only once the mesh changes. A
     * region's bones are the ones below its part's, or the ones it names as
     * its own, the hair's spring chains and the head they hang from; a part
     * whose every bone has a twin across the body is sided, shown and framed
     * one side at a time.
     */

    // Internal
    private CharacterEditorManager characterEditorManager;
    private CharacterRigBranch characterRigBranch;

    // Shares — one list per region, by ordinal
    private FloatArrayList[] region2Shares;
    private CharacterMeshDocumentInstance[] region2Document;
    private int[] region2Revision;

    // Sides — per region, whether its part is sided on the rig data it was worked out for
    private boolean[] region2Sided;
    private RigData[] region2SidedRig;

    // Scratch
    private BooleanArrayList regionBones;

    // Base \\

    @Override
    protected void create() {

        int regionCount = CharacterEditorRegion.VALUES.length;

        // Shares
        this.region2Shares = new FloatArrayList[regionCount];
        this.region2Document = new CharacterMeshDocumentInstance[regionCount];
        this.region2Revision = new int[regionCount];

        for (int i = 0; i < regionCount; i++)
            region2Shares[i] = new FloatArrayList();

        // Sides
        this.region2Sided = new boolean[regionCount];
        this.region2SidedRig = new RigData[regionCount];

        // Scratch
        this.regionBones = new BooleanArrayList();
    }

    @Override
    protected void get() {
        this.characterEditorManager = get(CharacterEditorManager.class);
        this.characterRigBranch = get(CharacterRigBranch.class);
    }

    // Shares \\

    // How far a vertex belongs to a region, every vertex wholly to the whole character
    float getShare(CharacterMeshDocumentInstance document, CharacterEditorRegion region, int vertexIndex) {
        return region.isWhole() ? 1f : resolveShares(document, region).getFloat(vertexIndex);
    }

    // Whether a vertex is picked and selected in a region
    boolean holds(CharacterMeshDocumentInstance document, CharacterEditorRegion region, int vertexIndex) {

        if (region.isWhole())
            return true;

        float share = resolveShares(document, region).getFloat(vertexIndex);

        return region.isRootPinned() ? share >= 0f : share >= EditorSetting.CHARACTER_REGION_PICK_SHARE;
    }

    private FloatArrayList resolveShares(CharacterMeshDocumentInstance document, CharacterEditorRegion region) {

        int slot = region.ordinal();
        FloatArrayList shares = region2Shares[slot];

        if (region2Document[slot] == document && region2Revision[slot] == document.getRevision()
                && shares.size() == document.getMesh().getVertexCount())
            return shares;

        region2Document[slot] = document;
        region2Revision[slot] = document.getRevision();
        fillShares(document, region, shares);

        return shares;
    }

    private void fillShares(
            CharacterMeshDocumentInstance document,
            CharacterEditorRegion region,
            FloatArrayList shares) {

        RiggedMeshStruct mesh = document.getMesh();
        int vertexCount = mesh.getVertexCount();

        shares.clear();
        shares.ensureCapacity(vertexCount);

        if (!characterEditorManager.isRegionPart(document, region)) {

            for (int v = 0; v < vertexCount; v++)
                shares.add(-1f);

            return;
        }

        if (!characterEditorManager.isRegionFeatureOpen(region))
            fillBoneShares(mesh, region, shares);
        else if (region.isRootPinned())
            fillCardShares(mesh, shares);
        else
            for (int v = 0; v < vertexCount; v++)
                shares.add(1f);
    }

    // How far along its card each vertex stands, the farthest of the corners it is; a vertex on no card stays whole
    private void fillCardShares(RiggedMeshStruct mesh, FloatArrayList shares) {

        for (int v = 0; v < mesh.getVertexCount(); v++)
            shares.add(-1f);

        for (int f = 0; f < mesh.getFaceCount(); f++)
            for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++) {

                int vertexIndex = mesh.getFaceVertex(f, c);
                float along = mesh.hasAuthoredUVs(f) ? Math.clamp(mesh.getFaceV(f, c), 0f, 1f) : 1f;

                shares.set(vertexIndex, Math.max(shares.getFloat(vertexIndex), along));
            }

        for (int v = 0; v < shares.size(); v++)
            if (shares.getFloat(v) < 0f)
                shares.set(v, 1f);
    }

    // The weight each vertex puts on the region's bones and every bone below them
    private void fillBoneShares(RiggedMeshStruct mesh, CharacterEditorRegion region, FloatArrayList shares) {

        resolveRegionBones(mesh.getRigHandle(), region);

        for (int v = 0; v < mesh.getVertexCount(); v++) {

            float share = 0f;

            for (int i = 0; i < mesh.getInfluenceCount(v); i++)
                if (regionBones.getBoolean(mesh.getInfluenceBone(v, i)))
                    share += mesh.getInfluenceWeight(v, i);

            shares.add(share);
        }
    }

    // Bones \\

    // Every bone of a rig the region holds: one of its part's, or any bone below one, parents standing before children
    private void resolveRegionBones(RigHandle rigHandle, CharacterEditorRegion region) {

        regionBones.clear();

        for (int i = 0; i < rigHandle.getBoneCount(); i++) {

            int parent = rigHandle.getBone(i).getParentIndex();

            regionBones.add(isRoot(region, rigHandle, rigHandle.getBone(i).getName())
                    || (parent != EngineSetting.INDEX_NOT_FOUND && regionBones.getBoolean(parent)));
        }
    }

    // Whether a bone of a loaded rig is the region's: named as its own, or lying below one of its part's bones, read up
    // its parents
    boolean holdsBone(CharacterEditorRegion region, RigHandle rigHandle, int boneIndex) {

        if (region.isWhole() || region.namesBone(rigHandle.getBone(boneIndex).getName()))
            return true;

        if (!region.holdsBody())
            return false;

        for (int bone = boneIndex;
                bone != EngineSetting.INDEX_NOT_FOUND;
                bone = rigHandle.getBone(bone).getParentIndex())
            if (isRoot(region, rigHandle, rigHandle.getBone(bone).getName()))
                return true;

        return false;
    }

    // Whether a bone of the rig being edited is the region's: named as its own, or lying below one of its part's bones
    // as the loaded rig names them
    boolean holdsRigBone(CharacterEditorRegion region, int boneIndex) {

        if (region.isWhole() || region.namesBone(characterRigBranch.getBoneName(boneIndex)))
            return true;

        RigHandle rigHandle = characterRigBranch.getRigHandle();

        if (!region.holdsBody() || rigHandle == null)
            return false;

        for (int bone = boneIndex; bone != EngineSetting.INDEX_NOT_FOUND; bone = characterRigBranch.getParent(bone))
            if (isRoot(region, rigHandle, characterRigBranch.getBoneName(bone)))
                return true;

        return false;
    }

    // Whether a bone, by name, is one the rig names for the region's part
    private boolean isRoot(CharacterEditorRegion region, RigHandle rigHandle, String boneName) {

        if (!region.holdsBody())
            return false;

        int[] partBones = rigHandle.getPartBones(region.getRigPart());

        for (int i = 0; i < partBones.length; i++)
            if (rigHandle.getBone(partBones[i]).getName().equals(boneName))
                return true;

        return false;
    }

    // Sides \\

    // Whether every bone of the region's part has a twin across the body, a left or right bone by its name, worked out
    // again only once the rig's data is replaced
    boolean isSided(CharacterEditorRegion region, RigHandle rigHandle) {

        int slot = region.ordinal();

        if (region2SidedRig[slot] != rigHandle.getRigData()) {
            region2SidedRig[slot] = rigHandle.getRigData();
            region2Sided[slot] = resolveSided(region, rigHandle);
        }

        return region2Sided[slot];
    }

    private boolean resolveSided(CharacterEditorRegion region, RigHandle rigHandle) {

        if (!region.holdsBody())
            return false;

        int[] partBones = rigHandle.getPartBones(region.getRigPart());

        if (partBones.length == 0)
            return false;

        for (int i = 0; i < partBones.length; i++)
            if (CharacterMeshUtility.toSideSuffix(rigHandle.getBone(partBones[i]).getName()).isEmpty())
                return false;

        return true;
    }

    // The bone a region is framed about: its part's first bone on the side shown, or the bone its own bones hang from
    int resolveFrameBone(CharacterEditorRegion region, CharacterEditorSide side, RigHandle rigHandle) {

        if (region.hasBonePrefix())
            return rigHandle.getBoneIndex(EditorSetting.CHARACTER_REGION_HAIR_FRAME);

        if (!region.holdsBody())
            return EngineSetting.INDEX_NOT_FOUND;

        int[] partBones = rigHandle.getPartBones(region.getRigPart());
        boolean sided = isSided(region, rigHandle);

        for (int i = 0; i < partBones.length; i++)
            if (!sided || side.namesBone(rigHandle.getBone(partBones[i]).getName()))
                return partBones[i];

        return EngineSetting.INDEX_NOT_FOUND;
    }
}
