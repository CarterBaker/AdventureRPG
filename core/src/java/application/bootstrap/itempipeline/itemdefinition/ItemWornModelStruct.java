package application.bootstrap.itempipeline.itemdefinition;

import application.bootstrap.geometrypipeline.mesh.MeshHandle;
import application.bootstrap.geometrypipeline.rig.RigHandle;
import application.bootstrap.geometrypipeline.skinnedbuffer.SkinnedBoneMaskStruct;
import application.bootstrap.shaderpipeline.material.MaterialInstance;
import engine.root.EngineSetting;
import engine.root.StructPackage;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;

public class ItemWornModelStruct extends StructPackage {

    /*
     * The model an item is worn as on one rig, apart from the sub-voxel model
     * it lies in the world as: a rigged mesh skinned to the rig's own bones,
     * so it bends, stretches and swings with the body as the garment would,
     * fitted to every gender's body or to one gender's alone, drawn with its
     * own material or the body's. The body bones it covers are cut from the
     * body beneath it, so skin never shows through. Each of its morphs plays
     * the body's morph of the same name, so it follows every shape slider the
     * body carries; which body morph each plays is resolved once per body
     * mesh and kept.
     */

    // Mesh
    private final MeshHandle meshHandle;
    private final MaterialInstance material;

    // Fit — the gender it is fitted to, ITEM_WORN_GENDER_ANY for every one
    private final short genderID;

    // Cover — the body bones cut from the body beneath it
    private final SkinnedBoneMaskStruct coveredBones;
    private final boolean covers;

    // Morphs — per body mesh, the body morph each of this mesh's morphs plays, INDEX_NOT_FOUND for none
    private final Reference2ObjectOpenHashMap<MeshHandle, int[]> bodyMesh2MorphSources;

    // Constructor \\

    public ItemWornModelStruct(
            MeshHandle meshHandle,
            MaterialInstance material,
            short genderID,
            SkinnedBoneMaskStruct coveredBones,
            boolean covers) {

        // Mesh
        this.meshHandle = meshHandle;
        this.material = material;

        // Fit
        this.genderID = genderID;

        // Cover
        this.coveredBones = coveredBones;
        this.covers = covers;

        // Morphs
        this.bodyMesh2MorphSources = new Reference2ObjectOpenHashMap<>();
    }

    // Fit \\

    public boolean fits(RigHandle rigHandle) {
        return meshHandle.getRigHandle() == rigHandle;
    }

    public boolean isFittedTo(short genderID) {
        return this.genderID == genderID;
    }

    public boolean isFittedToAny() {
        return genderID == EngineSetting.ITEM_WORN_GENDER_ANY;
    }

    // Morphs \\

    // The body morph each of this mesh's morphs plays on the body given, by name, resolved the first time it is asked
    public int[] getMorphSources(MeshHandle bodyMesh) {

        int[] sources = bodyMesh2MorphSources.get(bodyMesh);

        if (sources != null)
            return sources;

        sources = new int[meshHandle.getMorphCount()];

        for (int m = 0; m < sources.length; m++)
            sources[m] = bodyMesh.getMorphIndex(meshHandle.getMorphName(m));

        bodyMesh2MorphSources.put(bodyMesh, sources);

        return sources;
    }

    // Accessible \\

    public MeshHandle getMeshHandle() {
        return meshHandle;
    }

    public boolean hasMaterial() {
        return material != null;
    }

    public MaterialInstance getMaterial() {
        return material;
    }

    public short getGenderID() {
        return genderID;
    }

    public boolean covers() {
        return covers;
    }

    public SkinnedBoneMaskStruct getCoveredBones() {
        return coveredBones;
    }
}
