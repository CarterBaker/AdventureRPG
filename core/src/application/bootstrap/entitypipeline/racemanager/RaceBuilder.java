package application.bootstrap.entitypipeline.racemanager;

import java.io.File;

import application.bootstrap.entitypipeline.animationtree.AnimationTreeHandle;
import application.bootstrap.entitypipeline.animationtreemanager.AnimationTreeManager;
import application.bootstrap.entitypipeline.appearance.AppearanceData;
import application.bootstrap.entitypipeline.inventory.EquipmentAnchorStruct;
import application.bootstrap.entitypipeline.inventory.EquipmentSlot;
import application.bootstrap.entitypipeline.race.RaceData;
import application.bootstrap.entitypipeline.race.RaceGenderStruct;
import application.bootstrap.entitypipeline.race.RaceHandle;
import application.bootstrap.geometrypipeline.mesh.MeshHandle;
import application.bootstrap.geometrypipeline.meshmanager.MeshManager;
import application.bootstrap.geometrypipeline.rig.RigHandle;
import application.bootstrap.geometrypipeline.rigmanager.RigManager;
import application.bootstrap.geometrypipeline.skinnedbuffer.SkinnedOverlay;
import application.bootstrap.geometrypipeline.skinnedbuffer.SkinnedOverlayStruct;
import application.bootstrap.geometrypipeline.skinnedbuffer.SkinnedProjection;
import application.bootstrap.progressionpipeline.gendermanager.GenderManager;
import application.bootstrap.shaderpipeline.material.MaterialInstance;
import application.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import application.bootstrap.shaderpipeline.texture.TextureHandle;
import application.bootstrap.shaderpipeline.texturemanager.TextureManager;
import engine.root.BuilderPackage;
import engine.root.EngineSetting;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class RaceBuilder extends BuilderPackage {

    /*
     * Parses race ARPG into RaceData wrapped in a RaceHandle. "genders" maps
     * every gender the race may be made as, at least one, the first its
     * default, to the character model it requires: a rigged "mesh", the
     * "rig" that mesh is skinned to, the "material" it is drawn with and the
     * "animation_tree" it moves by, on that rig, beside the "shapes" and
     * "features" it starts with. Every gender must be on one rig, the race's,
     * since its "equipment" anchors, its optional "appearance" and every clip
     * it plays are laid out by those bones. Genders naming one material share
     * one clone. Bootstrap only, apart from the unregistered preview builds
     * an editor makes of a race it is changing.
     */

    // Internal
    private MeshManager meshManager;
    private RigManager rigManager;
    private MaterialManager materialManager;
    private TextureManager textureManager;
    private AnimationTreeManager animationTreeManager;
    private GenderManager genderManager;
    private AppearanceBuilder appearanceBuilder;

    // Base \\

    @Override
    protected void get() {

        // Internal
        this.meshManager = get(MeshManager.class);
        this.rigManager = get(RigManager.class);
        this.materialManager = get(MaterialManager.class);
        this.textureManager = get(TextureManager.class);
        this.animationTreeManager = get(AnimationTreeManager.class);
        this.genderManager = get(GenderManager.class);
        this.appearanceBuilder = get(AppearanceBuilder.class);
    }

    // Build \\

    RaceHandle build(File file, String raceName) {
        return build(ArpgUtility.loadObject(file), raceName);
    }

    RaceHandle build(ArpgObjectStruct arpg, String raceName) {

        ArpgObjectStruct gendersArpg = ArpgUtility.validateObject(arpg, "genders");
        RaceGenderStruct[] genders = parseGenders(gendersArpg, raceName);
        RigHandle rigHandle = genders[0].getRigHandle();
        ObjectArrayList<EquipmentAnchorStruct> equipmentAnchors = new ObjectArrayList<>();

        if (ArpgUtility.hasArray(arpg, "equipment"))
            parseEquipmentAnchors(arpg.getAsArray("equipment"), rigHandle, equipmentAnchors, raceName);

        AppearanceData appearanceData = ArpgUtility.hasObject(arpg, "appearance")
                ? appearanceBuilder.build(arpg.getAsObject("appearance"), gendersArpg, genders, raceName)
                : null;

        RaceHandle raceHandle = create(RaceHandle.class);
        raceHandle.constructor(new RaceData(raceName, rigHandle, genders, appearanceData, equipmentAnchors));

        return raceHandle;
    }

    // Genders \\

    // Every gender in the order listed, each on the first's rig
    private RaceGenderStruct[] parseGenders(ArpgObjectStruct gendersArpg, String raceName) {

        if (gendersArpg.size() == 0)
            throwException("Race must list at least one gender under \"genders\". Source: " + raceName);

        RaceGenderStruct[] genders = new RaceGenderStruct[gendersArpg.size()];
        Object2ObjectOpenHashMap<String, MaterialInstance> materialName2Material = new Object2ObjectOpenHashMap<>();
        int index = 0;

        for (String genderName : gendersArpg.keySet()) {

            RaceGenderStruct gender = parseGender(
                    genderName,
                    gendersArpg.getAsObject(genderName),
                    materialName2Material,
                    raceName);

            if (index > 0 && gender.getRigHandle() != genders[0].getRigHandle())
                throwException("Race gender \"" + genderName + "\" is on rig \"" + gender.getRigHandle().getRigName()
                        + "\" but the race is on \"" + genders[0].getRigHandle().getRigName()
                        + "\" — every gender of a race must share one rig, since its sliders, anchors and clips "
                        + "are laid out by its bones. Source: " + raceName);

            genders[index++] = gender;
        }

        return genders;
    }

    private RaceGenderStruct parseGender(
            String genderName,
            ArpgObjectStruct genderArpg,
            Object2ObjectOpenHashMap<String, MaterialInstance> materialName2Material,
            String raceName) {

        String meshName = ArpgUtility.validateString(genderArpg, "mesh");
        String rigName = ArpgUtility.validateString(genderArpg, "rig");
        String materialName = ArpgUtility.validateString(genderArpg, "material");
        String treeName = ArpgUtility.validateString(genderArpg, "animation_tree");

        MeshHandle meshHandle = meshManager.getMeshHandleFromMeshName(meshName);
        RigHandle rigHandle = rigManager.getRigHandleFromRigName(rigName);
        AnimationTreeHandle animationTreeHandle = animationTreeManager.getAnimationTreeHandleFromTreeName(treeName);

        if (!meshHandle.hasRig() || meshHandle.getRigHandle() != rigHandle)
            throwException("Race gender \"" + genderName + "\" wears mesh \"" + meshName
                    + "\", which is not skinned to its rig \"" + rigName + "\". Source: " + raceName);

        if (animationTreeHandle.getRigHandle() != rigHandle)
            throwException("Race gender \"" + genderName + "\" moves by animation tree \"" + treeName
                    + "\", which targets a different rig than its rig \"" + rigName + "\". Source: " + raceName);

        MaterialInstance material = materialName2Material.get(materialName);

        if (material == null) {
            material = materialManager.cloneMaterial(materialName);
            materialName2Material.put(materialName, material);
        }

        return new RaceGenderStruct(
                genderManager.getGenderIDFromGenderName(genderName),
                meshHandle,
                rigHandle,
                material,
                animationTreeHandle,
                parseOverlays(genderArpg, genderName, raceName));
    }

    // Overlays \\

    // Where each overlay the gender's model names lies on it: its "texture", "projection", box "center" and "size",
    // whether it is "mirror"ed across the body and, for a wrapped overlay, how often its image "repeat"s per block
    private SkinnedOverlayStruct[] parseOverlays(ArpgObjectStruct genderArpg, String genderName, String raceName) {

        SkinnedOverlayStruct[] overlays = new SkinnedOverlayStruct[SkinnedOverlay.VALUES.length];

        if (!ArpgUtility.hasObject(genderArpg, "overlays"))
            return overlays;

        ArpgObjectStruct overlaysArpg = genderArpg.getAsObject("overlays");

        for (String key : overlaysArpg.keySet()) {

            SkinnedOverlay skinnedOverlay = ArpgUtility.toEnum(key, SkinnedOverlay.class);
            ArpgObjectStruct overlayArpg = overlaysArpg.getAsObject(key);
            TextureHandle textureHandle = textureManager.getTextureHandleFromTextureName(
                    ArpgUtility.validateString(overlayArpg, "texture"));
            Vector3 size = toVector3(ArpgUtility.validateArray(overlayArpg, "size", 3));

            if (size.x <= 0f || size.y <= 0f || size.z <= 0f)
                throwException("Race gender \"" + genderName + "\" gives overlay \"" + key
                        + "\" a size that is not positive. Source: " + raceName);

            overlays[skinnedOverlay.ordinal()] = new SkinnedOverlayStruct(
                    textureHandle.getU0(),
                    textureHandle.getU1(),
                    textureHandle.getV0(),
                    textureHandle.getV1(),
                    toVector3(ArpgUtility.validateArray(overlayArpg, "center", 3)),
                    size,
                    ArpgUtility.getEnum(overlayArpg, "projection", SkinnedProjection.class, SkinnedProjection.FRONT),
                    ArpgUtility.getBoolean(overlayArpg, "mirror", false),
                    ArpgUtility.getFloat(overlayArpg, "repeat", EngineSetting.SKINNED_OVERLAY_REPEAT_DEFAULT));
        }

        return overlays;
    }

    // Equipment \\

    private void parseEquipmentAnchors(
            ArpgArrayStruct anchorsArpg,
            RigHandle rigHandle,
            ObjectArrayList<EquipmentAnchorStruct> equipmentAnchors,
            String raceName) {

        for (int i = 0; i < anchorsArpg.size(); i++) {

            ArpgObjectStruct anchorArpg = anchorsArpg.get(i).getAsObject();
            EquipmentSlot equipmentSlot = ArpgUtility.toEnum(
                    ArpgUtility.validateString(anchorArpg, "slot"), EquipmentSlot.class);
            String boneName = ArpgUtility.validateString(anchorArpg, "bone");

            if (!rigHandle.hasBone(boneName))
                throwException("Race equipment anchor for slot \"" + equipmentSlot
                        + "\" names bone \"" + boneName + "\", which its rig does not have. Source: " + raceName);

            equipmentAnchors.add(new EquipmentAnchorStruct(
                    equipmentSlot,
                    rigHandle.getBoneIndex(boneName),
                    parseAnchorVector(anchorArpg, "position", 0f),
                    parseAnchorVector(anchorArpg, "rotation", 0f),
                    parseAnchorVector(anchorArpg, "size", EngineSetting.DEFAULT_ENTITY_SIZE),
                    ArpgUtility.getBoolean(anchorArpg, "hold", false)));
        }
    }

    private Vector3 parseAnchorVector(ArpgObjectStruct anchorArpg, String key, float defaultValue) {

        if (!ArpgUtility.hasArray(anchorArpg, key))
            return new Vector3(defaultValue);

        return toVector3(ArpgUtility.validateArray(anchorArpg, key, 3));
    }

    private Vector3 toVector3(ArpgArrayStruct vectorArpg) {
        return new Vector3(
                vectorArpg.get(0).getAsFloat(),
                vectorArpg.get(1).getAsFloat(),
                vectorArpg.get(2).getAsFloat());
    }
}
