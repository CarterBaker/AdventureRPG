package application.bootstrap.itempipeline.itemdefinitionmanager;

import application.bootstrap.geometrypipeline.mesh.MeshHandle;
import application.bootstrap.geometrypipeline.meshmanager.MeshManager;
import application.bootstrap.geometrypipeline.rig.RigHandle;
import application.bootstrap.geometrypipeline.skinnedbuffer.SkinnedBoneMaskStruct;
import application.bootstrap.geometrypipeline.skinnedbuffer.SkinnedMaskMode;
import application.bootstrap.itempipeline.itemdefinition.EquipmentType;
import application.bootstrap.itempipeline.itemdefinition.ItemWornModelStruct;
import application.bootstrap.progressionpipeline.gendermanager.GenderManager;
import application.bootstrap.shaderpipeline.material.MaterialInstance;
import application.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class WornModelBranch extends BranchPackage {

    /*
     * Reads the models an item is worn as from its "worn" list, the way
     * outfit pieces are skinned to the skeleton of the body that wears them:
     * each names a rigged "mesh" skinned to a rig, the "gender" whose body it
     * is fitted to (none for every gender on that rig), the "material" it is
     * drawn with (the body's when it names none, else its own clone) and the
     * body bones it "hides" beneath it. Only an item worn on the body takes
     * worn models, and no two may fit the same rig and gender, so which one a
     * character wears is never ambiguous.
     */

    // Internal
    private MeshManager meshManager;
    private MaterialManager materialManager;
    private GenderManager genderManager;

    // Base \\

    @Override
    protected void get() {
        this.meshManager = get(MeshManager.class);
        this.materialManager = get(MaterialManager.class);
        this.genderManager = get(GenderManager.class);
    }

    // Parse \\

    ObjectArrayList<ItemWornModelStruct> parse(
            ArpgObjectStruct itemArpg,
            EquipmentType equipmentType,
            String itemName) {

        ObjectArrayList<ItemWornModelStruct> wornModels = new ObjectArrayList<>();

        if (!ArpgUtility.hasArray(itemArpg, EngineSetting.ITEM_WORN_FIELD))
            return wornModels;

        if (!equipmentType.isWornOnBody())
            throwException("Item '" + itemName + "' names worn models but is not worn on the body — only an item "
                    + "equipped in a body slot is drawn as a worn model; anything held keeps its own model.");

        ArpgArrayStruct wornArray = itemArpg.getAsArray(EngineSetting.ITEM_WORN_FIELD);

        for (int i = 0; i < wornArray.size(); i++) {

            ItemWornModelStruct wornModel = parseWornModel(wornArray.get(i).getAsObject(), itemName);

            if (findFit(wornModels, wornModel) != null)
                throwException("Item '" + itemName + "' names two worn models for the rig \""
                        + wornModel.getMeshHandle().getRigHandle().getRigName()
                        + "\" and the same gender — each rig and gender is worn as one model.");

            wornModels.add(wornModel);
        }

        return wornModels;
    }

    private ItemWornModelStruct parseWornModel(ArpgObjectStruct wornArpg, String itemName) {

        String meshName = ArpgUtility.validateString(wornArpg, "mesh");
        MeshHandle meshHandle = meshManager.getMeshHandleFromMeshName(meshName);

        if (!meshHandle.hasRig())
            throwException("Item '" + itemName + "' is worn as the mesh \"" + meshName
                    + "\", which has no rig — a worn model is skinned onto the rig of the body wearing it.");

        SkinnedBoneMaskStruct coveredBones = new SkinnedBoneMaskStruct();
        boolean covers = parseCoveredBones(wornArpg, meshHandle.getRigHandle(), coveredBones, itemName);

        return new ItemWornModelStruct(
                meshHandle,
                parseMaterial(wornArpg),
                parseGender(wornArpg),
                coveredBones,
                covers);
    }

    private short parseGender(ArpgObjectStruct wornArpg) {

        String genderName = ArpgUtility.getString(wornArpg, "gender", EngineSetting.ITEM_WORN_GENDER_NONE);

        return genderName.isEmpty()
                ? EngineSetting.ITEM_WORN_GENDER_ANY
                : genderManager.getGenderIDFromGenderName(genderName);
    }

    private MaterialInstance parseMaterial(ArpgObjectStruct wornArpg) {

        String materialName = ArpgUtility.getString(wornArpg, "material", EngineSetting.ITEM_WORN_MATERIAL_BODY);

        return materialName.isEmpty() ? null : materialManager.cloneMaterial(materialName);
    }

    // The body bones the model hides, each one its rig holds; true when it hides any
    private boolean parseCoveredBones(
            ArpgObjectStruct wornArpg,
            RigHandle rigHandle,
            SkinnedBoneMaskStruct coveredBones,
            String itemName) {

        coveredBones.setMode(SkinnedMaskMode.HIDE);

        if (!ArpgUtility.hasArray(wornArpg, "hides"))
            return false;

        ArpgArrayStruct hidesArray = wornArpg.getAsArray("hides");

        for (int i = 0; i < hidesArray.size(); i++) {

            String boneName = hidesArray.get(i).getAsString();

            if (!rigHandle.hasBone(boneName))
                throwException("Item '" + itemName + "' hides the bone \"" + boneName + "\", which the rig \""
                        + rigHandle.getRigName() + "\" of its worn model does not have.");

            coveredBones.addBone(rigHandle.getBoneIndex(boneName));
        }

        return hidesArray.size() > 0;
    }

    // Utility \\

    // The model already listed that fits the same rig and gender as the one given, null for none
    private static ItemWornModelStruct findFit(
            ObjectArrayList<ItemWornModelStruct> wornModels,
            ItemWornModelStruct wornModel) {

        for (int i = 0; i < wornModels.size(); i++)
            if (wornModels.get(i).fits(wornModel.getMeshHandle().getRigHandle())
                    && wornModels.get(i).isFittedTo(wornModel.getGenderID()))
                return wornModels.get(i);

        return null;
    }
}
