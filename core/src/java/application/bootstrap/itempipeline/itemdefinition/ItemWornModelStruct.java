package application.bootstrap.itempipeline.itemdefinition;

import application.bootstrap.entitypipeline.feature.FeatureSlot;
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
     * fitted to every gender's body or to one gender's alone, to one side of
     * the body for a piece of a pair or to either side, drawn with its
     * own material or the body's. The body bones it covers are cut from the
     * body beneath it, so skin never shows through, and the features it
     * covers are left off, as a helmet leaves off the hair beneath it. Each of its morphs plays
     * the body's morph of the same name, so it follows every shape slider the
     * body carries; which body morph each plays is resolved once per body
     * mesh and kept.
     */

    // Mesh
    private final MeshHandle meshHandle;
    private final MaterialInstance material;

    // Fit — the gender it is fitted to, ITEM_WORN_GENDER_ANY for every one, and the side it is worn on, NONE for either
    private final short genderID;
    private final EquipmentSide equipmentSide;

    // Cover — the body bones cut from the body beneath it
    private final SkinnedBoneMaskStruct coveredBones;
    private final boolean covers;
    private final boolean[] coveredFeatures;

    // Morphs — per body mesh, the body morph each of this mesh's morphs plays, INDEX_NOT_FOUND for none
    private final Reference2ObjectOpenHashMap<MeshHandle, int[]> bodyMesh2MorphSources;

    // Constructor \\

    public ItemWornModelStruct(
            MeshHandle meshHandle,
            MaterialInstance material,
            short genderID,
            EquipmentSide equipmentSide,
            SkinnedBoneMaskStruct coveredBones,
            boolean covers,
            boolean[] coveredFeatures) {

        // Mesh
        this.meshHandle = meshHandle;
        this.material = material;

        // Fit
        this.genderID = genderID;
        this.equipmentSide = equipmentSide;

        // Cover
        this.coveredBones = coveredBones;
        this.covers = covers;
        this.coveredFeatures = coveredFeatures;

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

    // Whether it may be worn on a slot of the side given: one fitted to that side, or to either
    public boolean suits(EquipmentSide equipmentSide) {
        return this.equipmentSide == EquipmentSide.NONE || this.equipmentSide == equipmentSide;
    }

    public boolean isOnSide(EquipmentSide equipmentSide) {
        return this.equipmentSide == equipmentSide;
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

    public EquipmentSide getEquipmentSide() {
        return equipmentSide;
    }

    public boolean covers() {
        return covers;
    }

    public SkinnedBoneMaskStruct getCoveredBones() {
        return coveredBones;
    }

    public boolean coversFeature(FeatureSlot featureSlot) {
        return coveredFeatures[featureSlot.ordinal()];
    }
}
