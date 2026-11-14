package application.bootstrap.entitypipeline.featuremanager;

import application.bootstrap.entitypipeline.feature.FeatureHandle;
import application.bootstrap.entitypipeline.feature.FeatureSlot;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class FeatureManager extends ManagerPackage {

    /*
     * Owns the appearance feature palette for the engine lifetime — every
     * selectable head shape, hair style, eye, brow, and mouth option. Also
     * indexes features by slot so a character menu can list the choices for
     * one slot. Which feature a given entity wears lives on its
     * AppearanceHandle, never here. Auto-triggers an on-demand load via
     * FeatureLoader on a name-based cache miss.
     */

    // Palette
    private Object2IntOpenHashMap<String> featureName2FeatureID;
    private ObjectArrayList<FeatureHandle> featureID2FeatureHandle;
    private Object2ObjectOpenHashMap<FeatureSlot, ObjectArrayList<FeatureHandle>> featureSlot2FeatureHandles;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.featureName2FeatureID = RegistryUtility.createNameIndex();
        this.featureID2FeatureHandle = RegistryUtility.createPalette();
        this.featureSlot2FeatureHandles = new Object2ObjectOpenHashMap<>();

        for (FeatureSlot featureSlot : FeatureSlot.VALUES)
            featureSlot2FeatureHandles.put(featureSlot, new ObjectArrayList<>());

        create(FeatureLoader.class);
    }

    // Management \\

    short registerFeatureName(String featureName) {
        return (short) RegistryUtility.registerID(
                featureName2FeatureID, featureID2FeatureHandle, featureName, EngineSetting.REGISTRY_SHORT_ID_COUNT);
    }

    void addFeature(FeatureHandle handle) {

        featureID2FeatureHandle.set(handle.getFeatureID(), handle);
        featureSlot2FeatureHandles.get(handle.getFeatureSlot()).add(handle);
    }

    // Accessible \\

    public boolean hasFeature(String featureName) {
        return RegistryUtility.getHandle(featureName2FeatureID, featureID2FeatureHandle, featureName) != null;
    }

    public boolean isFeatureAvailable(String featureName) {
        return hasFeature(featureName)
                || (internalLoader != null && ((FeatureLoader) internalLoader).hasFeatureFile(featureName));
    }

    public FeatureHandle getFeatureHandleFromFeatureID(short featureID) {
        return RegistryUtility.getHandle(featureID2FeatureHandle, featureID);
    }

    public FeatureHandle getFeatureHandleFromFeatureName(String featureName) {

        FeatureHandle handle = RegistryUtility.getHandle(featureName2FeatureID, featureID2FeatureHandle, featureName);

        if (handle == null) {
            ((FeatureLoader) internalLoader).request(featureName);
            handle = RegistryUtility.getHandle(featureName2FeatureID, featureID2FeatureHandle, featureName);
        }

        if (handle == null)
            throwException("Feature could not be loaded: \"" + featureName + "\"");

        return handle;
    }

    public ObjectArrayList<FeatureHandle> getFeatureHandlesForSlot(FeatureSlot featureSlot) {
        return featureSlot2FeatureHandles.get(featureSlot);
    }
}
