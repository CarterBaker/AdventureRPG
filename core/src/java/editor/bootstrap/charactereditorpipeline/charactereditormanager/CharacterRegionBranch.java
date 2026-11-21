package editor.bootstrap.charactereditorpipeline.charactereditormanager;

import application.bootstrap.entitypipeline.feature.FeatureSlot;
import application.bootstrap.geometrypipeline.mesh.RiggedMeshStruct;
import application.bootstrap.geometrypipeline.rig.RigData;
import application.bootstrap.geometrypipeline.rig.RigHandle;
import application.bootstrap.geometrypipeline.rig.RigPartStruct;
import editor.bootstrap.charactereditorpipeline.charactermesh.CharacterMeshDocumentInstance;
import editor.bootstrap.charactereditorpipeline.util.CharacterRegionStruct;
import editor.bootstrap.charactereditorpipeline.util.CharacterEditorSide;
import editor.bootstrap.charactereditorpipeline.util.CharacterMeshUtility;
import editor.runtime.EditorSetting;
import editor.runtime.label.LabelUtility;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.booleans.BooleanArrayList;
import engine.util.arpg.ArpgUtility;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

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
     * one side at a time. The regions themselves are the rig's: the whole
     * character, one for every part the rig names, in its order, and the
     * worn hair, made again only once the rig's data is replaced.
     */

    // Internal
    private CharacterEditorManager characterEditorManager;
    private CharacterRigBranch characterRigBranch;

    // Regions — the rig's, made for the rig data named, the whole character first; moves whenever they are made again
    private ObjectArrayList<CharacterRegionStruct> regions;
    private RigData regionsRig;
    private int regionsRevision;

    // Shares — one list per region, by its index, with the region, mesh and revision it was worked out for
    private ObjectArrayList<FloatArrayList> region2Shares;
    private ObjectArrayList<CharacterRegionStruct> region2SharesRegion;
    private ObjectArrayList<CharacterMeshDocumentInstance> region2Document;
    private IntArrayList region2Revision;

    // Scratch
    private BooleanArrayList regionBones;

    // Base \\

    @Override
    protected void create() {

        // Regions
        this.regions = new ObjectArrayList<>();
        this.regions.add(CharacterRegionStruct.WHOLE);

        // Shares
        this.region2Shares = new ObjectArrayList<>();
        this.region2SharesRegion = new ObjectArrayList<>();
        this.region2Document = new ObjectArrayList<>();
        this.region2Revision = new IntArrayList();

        // Scratch
        this.regionBones = new BooleanArrayList();
    }

    @Override
    protected void get() {
        this.characterEditorManager = get(CharacterEditorManager.class);
        this.characterRigBranch = get(CharacterRigBranch.class);
    }

    // Regions \\

    // The rig's regions, the whole character alone without a rig, made again only once the rig's data is replaced
    ObjectArrayList<CharacterRegionStruct> resolveRegions(RigHandle rigHandle) {

        RigData rigData = rigHandle != null ? rigHandle.getRigData() : null;

        if (rigData == regionsRig)
            return regions;

        this.regionsRig = rigData;
        this.regionsRevision++;
        regions.clear();
        regions.add(CharacterRegionStruct.WHOLE);

        if (rigHandle == null)
            return regions;

        for (int p = 0; p < rigHandle.getPartCount(); p++)
            regions.add(buildPartRegion(rigHandle, rigHandle.getPart(p), regions.size()));

        regions.add(buildHairRegion(regions.size()));

        for (int i = 1; i < regions.size(); i++)
            regions.get(i).setParent(resolveParent(rigHandle, regions.get(i)));

        return regions;
    }

    private CharacterRegionStruct buildPartRegion(RigHandle rigHandle, RigPartStruct part, int index) {

        String[] shownSlots = part.getShownSlots();
        FeatureSlot[] shownFeatures = new FeatureSlot[shownSlots.length];

        for (int i = 0; i < shownSlots.length; i++)
            shownFeatures[i] = ArpgUtility.toEnum(shownSlots[i], FeatureSlot.class);

        return new CharacterRegionStruct(
                part.getName(),
                LabelUtility.toWords(part.getName()),
                index,
                part.holdsSlot() ? ArpgUtility.toEnum(part.getHeldSlot(), FeatureSlot.class) : null,
                part.getBones(),
                resolvePartBoneNames(rigHandle, part.getBones()),
                false,
                resolveSided(rigHandle, part.getBones()),
                part.takesFace(),
                EditorSetting.CHARACTER_REGION_NO_PREFIX,
                shownFeatures);
    }

    private String[] resolvePartBoneNames(RigHandle rigHandle, int[] partBones) {

        String[] names = new String[partBones.length];

        for (int i = 0; i < partBones.length; i++)
            names[i] = rigHandle.getBone(partBones[i]).getName();

        return names;
    }

    private CharacterRegionStruct buildHairRegion(int index) {
        return new CharacterRegionStruct(
                EngineSetting.RIG_PART_RESERVED_HAIR,
                EditorSetting.CHARACTER_REGION_LABEL_HAIR,
                index,
                FeatureSlot.HAIR,
                new int[0],
                new String[0],
                true,
                false,
                false,
                EditorSetting.CHARACTER_HAIR_LOCK_PREFIX,
                new FeatureSlot[0]);
    }

    // The region a region is listed under: the part holding the nearest bone above its own, the part showing the hair
    // for the hair, the whole character otherwise
    private CharacterRegionStruct resolveParent(RigHandle rigHandle, CharacterRegionStruct region) {

        if (!region.holdsBody()) {

            for (int i = 1; i < regions.size(); i++)
                if (regions.get(i) != region && regions.get(i).showsFeature(region.getFeatureSlot()))
                    return regions.get(i);

            return CharacterRegionStruct.WHOLE;
        }

        for (int bone = rigHandle.getBone(region.getPartBones()[0]).getParentIndex();
                bone != EngineSetting.INDEX_NOT_FOUND;
                bone = rigHandle.getBone(bone).getParentIndex())
            for (int i = 1; i < regions.size(); i++)
                if (regions.get(i) != region && namesPartBone(regions.get(i), bone))
                    return regions.get(i);

        return CharacterRegionStruct.WHOLE;
    }

    private boolean namesPartBone(CharacterRegionStruct region, int boneIndex) {

        int[] partBones = region.getPartBones();

        for (int i = 0; i < partBones.length; i++)
            if (partBones[i] == boneIndex)
                return true;

        return false;
    }

    // A region of the rig by its key, the whole character for a key the rig no longer names
    CharacterRegionStruct findRegion(RigHandle rigHandle, String key) {

        ObjectArrayList<CharacterRegionStruct> listed = resolveRegions(rigHandle);

        for (int i = 0; i < listed.size(); i++)
            if (listed.get(i).getKey().equals(key))
                return listed.get(i);

        return CharacterRegionStruct.WHOLE;
    }

    int getRegionsRevision() {
        return regionsRevision;
    }

    // Shares \\

    // How far a vertex belongs to a region, every vertex wholly to the whole character
    float getShare(CharacterMeshDocumentInstance document, CharacterRegionStruct region, int vertexIndex) {
        return region.isWhole() ? 1f : resolveShares(document, region).getFloat(vertexIndex);
    }

    // Whether a vertex is picked and selected in a region
    boolean holds(CharacterMeshDocumentInstance document, CharacterRegionStruct region, int vertexIndex) {

        if (region.isWhole())
            return true;

        float share = resolveShares(document, region).getFloat(vertexIndex);

        return region.isRootPinned() ? share >= 0f : share >= EditorSetting.CHARACTER_REGION_PICK_SHARE;
    }

    private FloatArrayList resolveShares(CharacterMeshDocumentInstance document, CharacterRegionStruct region) {

        int slot = region.getIndex();

        while (region2Shares.size() <= slot) {
            region2Shares.add(new FloatArrayList());
            region2SharesRegion.add(null);
            region2Document.add(null);
            region2Revision.add(EngineSetting.INDEX_NOT_FOUND);
        }

        FloatArrayList shares = region2Shares.get(slot);

        if (region2SharesRegion.get(slot) == region && region2Document.get(slot) == document
                && region2Revision.getInt(slot) == document.getRevision()
                && shares.size() == document.getMesh().getVertexCount())
            return shares;

        region2SharesRegion.set(slot, region);
        region2Document.set(slot, document);
        region2Revision.set(slot, document.getRevision());
        fillShares(document, region, shares);

        return shares;
    }

    private void fillShares(
            CharacterMeshDocumentInstance document,
            CharacterRegionStruct region,
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
            for (int c = 0; c < mesh.getFaceCornerCount(f); c++) {

                int vertexIndex = mesh.getFaceVertex(f, c);
                float along = mesh.hasAuthoredUVs(f) ? Math.clamp(mesh.getFaceStrand(f, c), 0f, 1f) : 1f;

                shares.set(vertexIndex, Math.max(shares.getFloat(vertexIndex), along));
            }

        for (int v = 0; v < shares.size(); v++)
            if (shares.getFloat(v) < 0f)
                shares.set(v, 1f);
    }

    // The weight each vertex puts on the region's bones and every bone below them
    private void fillBoneShares(RiggedMeshStruct mesh, CharacterRegionStruct region, FloatArrayList shares) {

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
    private void resolveRegionBones(RigHandle rigHandle, CharacterRegionStruct region) {

        regionBones.clear();

        for (int i = 0; i < rigHandle.getBoneCount(); i++) {

            int parent = rigHandle.getBone(i).getParentIndex();

            regionBones.add(isRoot(region, rigHandle.getBone(i).getName())
                    || (parent != EngineSetting.INDEX_NOT_FOUND && regionBones.getBoolean(parent)));
        }
    }

    // Whether a bone of a loaded rig is the region's: named as its own, or lying below one of its part's bones, read up
    // its parents
    boolean holdsBone(CharacterRegionStruct region, RigHandle rigHandle, int boneIndex) {

        if (region.isWhole() || region.namesBone(rigHandle.getBone(boneIndex).getName()))
            return true;

        if (!region.holdsBody())
            return false;

        for (int bone = boneIndex;
                bone != EngineSetting.INDEX_NOT_FOUND;
                bone = rigHandle.getBone(bone).getParentIndex())
            if (isRoot(region, rigHandle.getBone(bone).getName()))
                return true;

        return false;
    }

    // Whether a bone of the rig being edited is the region's: named as its own, or lying below one of its part's bones
    // as the loaded rig names them
    boolean holdsRigBone(CharacterRegionStruct region, int boneIndex) {

        if (region.isWhole() || region.namesBone(characterRigBranch.getBoneName(boneIndex)))
            return true;

        RigHandle rigHandle = characterRigBranch.getRigHandle();

        if (!region.holdsBody() || rigHandle == null)
            return false;

        for (int bone = boneIndex; bone != EngineSetting.INDEX_NOT_FOUND; bone = characterRigBranch.getParent(bone))
            if (isRoot(region, characterRigBranch.getBoneName(bone)))
                return true;

        return false;
    }

    // Whether a bone, by name, is one the rig names for the region's part
    private boolean isRoot(CharacterRegionStruct region, String boneName) {
        return region.holdsBody() && region.isPartBone(boneName);
    }

    // Sides \\

    // Whether every bone of a part has a twin across the body, a left or right bone by its name
    private boolean resolveSided(RigHandle rigHandle, int[] partBones) {

        for (int i = 0; i < partBones.length; i++)
            if (CharacterMeshUtility.toSideSuffix(rigHandle.getBone(partBones[i]).getName()).isEmpty())
                return false;

        return true;
    }

    // The bone a region is framed about: its part's first bone on the side shown, or the bone its own bones hang from
    int resolveFrameBone(CharacterRegionStruct region, CharacterEditorSide side, RigHandle rigHandle) {

        if (region.hasBonePrefix())
            return rigHandle.getBoneIndex(EditorSetting.CHARACTER_REGION_HAIR_FRAME);

        if (!region.holdsBody())
            return EngineSetting.INDEX_NOT_FOUND;

        String[] partBoneNames = region.getPartBoneNames();
        boolean sided = region.isSided();

        for (int i = 0; i < partBoneNames.length; i++)
            if (rigHandle.hasBone(partBoneNames[i]) && (!sided || side.namesBone(partBoneNames[i])))
                return rigHandle.getBoneIndex(partBoneNames[i]);

        return EngineSetting.INDEX_NOT_FOUND;
    }
}
