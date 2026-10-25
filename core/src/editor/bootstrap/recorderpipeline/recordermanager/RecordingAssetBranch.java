package editor.bootstrap.recorderpipeline.recordermanager;

import application.bootstrap.animationpipeline.animationmanager.AnimationManager;
import application.bootstrap.entitypipeline.entity.EntityData;
import application.bootstrap.entitypipeline.entitymanager.EntityManager;
import application.bootstrap.entitypipeline.feature.FeatureHandle;
import application.bootstrap.entitypipeline.featuremanager.FeatureManager;
import application.bootstrap.geometrypipeline.mesh.MeshHandle;
import application.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle;
import application.bootstrap.itempipeline.itemdefinitionmanager.ItemDefinitionManager;
import application.bootstrap.progressionpipeline.gendermanager.GenderManager;
import application.bootstrap.shaderpipeline.texture.TextureHandle;
import application.bootstrap.vehiclepipeline.vehicle.VehicleHandle;
import application.bootstrap.vehiclepipeline.vehiclemanager.VehicleManager;
import application.bootstrap.worldpipeline.worldmanager.WorldManager;
import editor.bootstrap.recorderpipeline.recording.RecorderAssetKind;
import editor.bootstrap.recorderpipeline.recording.RecorderOverlay;
import editor.bootstrap.recorderpipeline.recording.RecordingAssetSectionStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingAssetStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingBodyTrackStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingInstance;
import editor.bootstrap.recorderpipeline.recording.RecordingOverlayTrackStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingTakeStruct;
import engine.root.BranchPackage;
import engine.util.arpg.ArpgElementStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class RecordingAssetBranch extends BranchPackage {

    /*
     * Gathers every asset a recording uses, across every take in its bin,
     * into one section per RecorderAssetKind. The recording names its world,
     * and each take its characters' templates, genders and animations, the
     * features and items each look wore, the items thrown, the vehicles and
     * the interface targets it captured. Each named asset is looked up once,
     * the first time it is named, for what it is drawn and moved with — a
     * template's model, material and animation tree, a feature's model and
     * textures, an item's model, a vehicle's textures and the world's
     * calendar — and one the game can no longer find is marked missing.
     */

    // Internal
    private WorldManager worldManager;
    private EntityManager entityManager;
    private GenderManager genderManager;
    private FeatureManager featureManager;
    private ItemDefinitionManager itemDefinitionManager;
    private VehicleManager vehicleManager;
    private AnimationManager animationManager;

    // Sections
    private RecordingAssetSectionStruct[] sections;

    // Base \\

    @Override
    protected void create() {

        // Sections
        this.sections = new RecordingAssetSectionStruct[RecorderAssetKind.VALUES.length];

        for (int i = 0; i < sections.length; i++)
            sections[i] = new RecordingAssetSectionStruct(RecorderAssetKind.VALUES[i]);
    }

    @Override
    protected void get() {
        this.worldManager = get(WorldManager.class);
        this.entityManager = get(EntityManager.class);
        this.genderManager = get(GenderManager.class);
        this.featureManager = get(FeatureManager.class);
        this.itemDefinitionManager = get(ItemDefinitionManager.class);
        this.vehicleManager = get(VehicleManager.class);
        this.animationManager = get(AnimationManager.class);
    }

    // Gather \\

    // Every section filled again from the recording, each in name order; empty for none
    void gather(RecordingInstance recording) {

        for (int i = 0; i < sections.length; i++)
            sections[i].clear();

        if (recording == null)
            return;

        gatherWorld(recording.getWorldName(), recording.getRecordingName());

        ObjectArrayList<RecordingTakeStruct> takes = recording.getTakes();

        for (int i = 0; i < takes.size(); i++)
            gatherTake(takes.get(i));

        for (int i = 0; i < sections.length; i++)
            sections[i].sort();
    }

    private void gatherTake(RecordingTakeStruct take) {

        String owner = take.getName();

        if (take.getPlayerTrack() != null)
            gatherBody(take.getPlayerTrack(), owner);

        for (int i = 0; i < take.getResidentTracks().size(); i++)
            gatherBody(take.getResidentTracks().get(i), owner);

        for (int i = 0; i < take.getProjectileTracks().size(); i++)
            gatherItem(take.getProjectileTracks().get(i).getItemName(), owner);

        for (int i = 0; i < take.getVehicleTracks().size(); i++)
            gatherVehicle(take.getVehicleTracks().get(i).getVehicleName(), owner);

        if (take.getOverlayTrack() != null)
            gatherOverlays(take.getOverlayTrack(), owner);
    }

    // World \\

    // The world the recording was made in, and its calendar when the world is loaded
    private void gatherWorld(String worldName, String owner) {

        sections[RecorderAssetKind.WORLD.ordinal()].add(worldName, owner);

        if (worldManager.hasWorld(worldName))
            add(RecorderAssetKind.CALENDAR,
                    worldManager.getWorldHandleFromWorldName(worldName).getCalendarName(), worldName);
    }

    // Characters \\

    // A character's template, gender and animations, and everything every look it wore names
    private void gatherBody(RecordingBodyTrackStruct track, String owner) {

        RecordingAssetSectionStruct section = sections[RecorderAssetKind.CHARACTERS.ordinal()];
        String characterName = track.getDisplayName();
        boolean known = section.contains(track.getTemplateName());
        RecordingAssetStruct template = section.add(track.getTemplateName(), owner);

        if (!known)
            resolveTemplate(template);

        sections[RecorderAssetKind.GENDERS.ordinal()].add(track.getGenderName(), characterName)
                .setMissing(!genderManager.hasGender(track.getGenderName()));

        for (int i = 0; i < track.getClipNames().size(); i++)
            gatherAnimation(track.getClipNames().get(i), characterName);

        for (int i = 0; i < track.getLooks().size(); i++)
            gatherLook(track.getLooks().get(i), characterName);
    }

    private void resolveTemplate(RecordingAssetStruct template) {

        try {
            EntityData entityData = entityManager.getEntityHandleFromTemplateName(template.getName()).getEntityData();

            if (entityData.hasCharacterModel())
                add(RecorderAssetKind.MODELS, entityData.getCharacterMesh(), template.getName());

            if (entityData.getCharacterMaterial() != null)
                add(RecorderAssetKind.MATERIALS,
                        entityData.getCharacterMaterial().getMaterialName(), template.getName());

            if (entityData.getAnimationTreeHandle() != null)
                add(RecorderAssetKind.ANIMATION_TREES,
                        entityData.getAnimationTreeHandle().getTreeName(), template.getName());
        } catch (InternalException e) {
            template.setMissing(true);
        }
    }

    private void gatherAnimation(String clipName, String owner) {

        RecordingAssetSectionStruct section = sections[RecorderAssetKind.ANIMATIONS.ordinal()];
        boolean known = section.contains(clipName);
        RecordingAssetStruct animation = section.add(clipName, owner);

        if (known)
            return;

        try {
            animationManager.getClipHandleFromClipName(clipName);
        } catch (InternalException e) {
            animation.setMissing(true);
        }
    }

    // The features a look's appearance names and the items its equipment holds
    private void gatherLook(ArpgObjectStruct lookArpg, String owner) {

        ArpgObjectStruct characterArpg = ArpgUtility.hasObject(lookArpg, "character")
                ? lookArpg.getAsObject("character")
                : null;
        ArpgObjectStruct appearanceArpg = characterArpg != null && ArpgUtility.hasObject(characterArpg, "appearance")
                ? characterArpg.getAsObject("appearance")
                : null;

        if (appearanceArpg != null && ArpgUtility.hasObject(appearanceArpg, "features"))
            for (Object2ObjectMap.Entry<String, ArpgElementStruct> entry
                    : appearanceArpg.getAsObject("features").entrySet())
                gatherFeature(entry.getValue().getAsString(), owner);

        ArpgObjectStruct inventoryArpg = ArpgUtility.hasObject(lookArpg, "inventory")
                ? lookArpg.getAsObject("inventory")
                : null;

        if (inventoryArpg == null || !ArpgUtility.hasObject(inventoryArpg, "equipment"))
            return;

        for (Object2ObjectMap.Entry<String, ArpgElementStruct> entry
                : inventoryArpg.getAsObject("equipment").entrySet()) {

            ArpgObjectStruct itemArpg = entry.getValue().getAsObject();

            if (ArpgUtility.hasString(itemArpg, "item"))
                gatherItem(itemArpg.get("item").getAsString(), owner);
        }
    }

    private void gatherFeature(String featureName, String owner) {

        RecordingAssetSectionStruct section = sections[RecorderAssetKind.FEATURES.ordinal()];
        boolean known = section.contains(featureName);
        RecordingAssetStruct feature = section.add(featureName, owner);

        if (known)
            return;

        try {
            FeatureHandle featureHandle = featureManager.getFeatureHandleFromFeatureName(featureName);

            add(RecorderAssetKind.MODELS, featureHandle.getMeshHandle(), featureName);
            add(RecorderAssetKind.TEXTURES, featureHandle.getTextureHandle(), featureName);
            add(RecorderAssetKind.TEXTURES, featureHandle.getFaceTextureHandle(), featureName);
        } catch (InternalException e) {
            feature.setMissing(true);
        }
    }

    // Items \\

    private void gatherItem(String itemName, String owner) {

        RecordingAssetSectionStruct section = sections[RecorderAssetKind.ITEMS.ordinal()];
        boolean known = section.contains(itemName);
        RecordingAssetStruct item = section.add(itemName, owner);

        if (known)
            return;

        if (!itemDefinitionManager.hasItem(itemName)) {
            item.setMissing(true);
            return;
        }

        ItemDefinitionHandle itemHandle = itemDefinitionManager.getItemHandleFromItemName(itemName);

        add(RecorderAssetKind.MODELS, itemHandle.getMeshHandle(), itemName);
    }

    // Vehicles \\

    private void gatherVehicle(String vehicleName, String owner) {

        RecordingAssetSectionStruct section = sections[RecorderAssetKind.VEHICLES.ordinal()];
        boolean known = section.contains(vehicleName);
        RecordingAssetStruct vehicle = section.add(vehicleName, owner);

        if (known)
            return;

        try {
            VehicleHandle vehicleHandle = vehicleManager.getVehicleHandleFromVehicleName(vehicleName);

            for (int i = 0; i < vehicleHandle.getPartCount(); i++)
                add(RecorderAssetKind.TEXTURES, vehicleHandle.getPart(i).getTextureName(), vehicleName);
        } catch (InternalException e) {
            vehicle.setMissing(true);
        }
    }

    // Interface \\

    // Every interface target the take captured a frame of
    private void gatherOverlays(RecordingOverlayTrackStruct overlayTrack, String owner) {
        for (RecorderOverlay overlay : RecorderOverlay.VALUES)
            if (!overlayTrack.getStream(overlay).isEmpty())
                add(RecorderAssetKind.INTERFACE, overlay.getFboName(), owner);
    }

    // Utility \\

    private void add(RecorderAssetKind kind, MeshHandle meshHandle, String owner) {
        if (meshHandle != null)
            add(kind, meshHandle.getMeshName(), owner);
    }

    private void add(RecorderAssetKind kind, TextureHandle textureHandle, String owner) {
        if (textureHandle != null)
            add(kind, textureHandle.getTileName(), owner);
    }

    // An asset named by what it was looked up from, none for a name left empty
    private void add(RecorderAssetKind kind, String assetName, String owner) {
        if (assetName != null && !assetName.isEmpty())
            sections[kind.ordinal()].add(assetName, owner);
    }

    // Accessible \\

    RecordingAssetSectionStruct[] getSections() {
        return sections;
    }
}
