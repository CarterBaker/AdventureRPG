package editor.bootstrap.charactereditorpipeline.charactereditormanager;

import application.bootstrap.geometrypipeline.mesh.RiggedMeshStruct;
import application.bootstrap.geometrypipeline.rig.RigHandle;
import editor.bootstrap.charactereditorpipeline.charactermesh.CharacterMeshDocumentInstance;
import editor.bootstrap.charactereditorpipeline.util.CharacterEditorRegion;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.booleans.BooleanArrayList;
import it.unimi.dsi.fastutil.floats.FloatArrayList;

class CharacterRegionBranch extends BranchPackage {

    /*
     * How far each vertex of a mesh being modelled belongs to a region, the
     * share every pick, box, brush, transform and mirror of a focused tab is
     * held to. A body region's share is the weight the vertex puts on the
     * region's bones and every bone below them, so a stroke fades out across
     * the wrist or ankle; a vertex weighing at least the pick share on them
     * is picked and selected there. A root-pinned region's share is how far
     * along its card the vertex stands, root to tip, so a stroke leaves the
     * roots on the scalp; every one of its vertices is picked. A mesh that is
     * not the part a region edits holds none of it (a negative share). Shares
     * are kept per region and worked out again only once the mesh changes.
     */

    // Internal
    private CharacterEditorManager characterEditorManager;
    private CharacterRigBranch characterRigBranch;

    // Shares — one list per region, by ordinal
    private FloatArrayList[] region2Shares;
    private CharacterMeshDocumentInstance[] region2Document;
    private int[] region2Revision;

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

        if (region.isRootPinned())
            fillCardShares(mesh, shares);
        else
            fillBoneShares(mesh, region, shares);
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

    // Every bone of a rig the region holds: one it names, or any bone below one, parents standing before children
    private void resolveRegionBones(RigHandle rigHandle, CharacterEditorRegion region) {

        regionBones.clear();

        for (int i = 0; i < rigHandle.getBoneCount(); i++) {

            int parent = rigHandle.getBone(i).getParentIndex();

            regionBones.add(isRoot(region, rigHandle.getBone(i).getName())
                    || (parent != EngineSetting.INDEX_NOT_FOUND && regionBones.getBoolean(parent)));
        }
    }

    // Whether a bone of a loaded rig is the region's or lies below one of its bones, read up its parents
    boolean holdsBone(CharacterEditorRegion region, RigHandle rigHandle, int boneIndex) {

        if (!region.isBound())
            return region.isWhole();

        for (int bone = boneIndex;
                bone != EngineSetting.INDEX_NOT_FOUND;
                bone = rigHandle.getBone(bone).getParentIndex())
            if (isRoot(region, rigHandle.getBone(bone).getName()))
                return true;

        return false;
    }

    // Whether a bone of the rig being edited is the region's or lies below one of its bones
    boolean holdsRigBone(CharacterEditorRegion region, int boneIndex) {

        if (!region.isBound())
            return region.isWhole();

        for (int bone = boneIndex; bone != EngineSetting.INDEX_NOT_FOUND; bone = characterRigBranch.getParent(bone))
            if (isRoot(region, characterRigBranch.getBoneName(bone)))
                return true;

        return false;
    }

    private boolean isRoot(CharacterEditorRegion region, String boneName) {

        for (String root : region.getRootBones())
            if (root.equals(boneName))
                return true;

        return false;
    }
}
