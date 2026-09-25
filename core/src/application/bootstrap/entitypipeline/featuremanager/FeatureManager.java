package application.bootstrap.entitypipeline.featuremanager;

import application.bootstrap.entitypipeline.feature.FeatureHandle;
import application.bootstrap.entitypipeline.feature.FeatureSlot;
import engine.root.ManagerPackage;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;

public class FeatureManager extends ManagerPackage {

    /*
     * Owns the appearance feature palette for the engine lifetime — every
     * selectable head shape, hair style, eye, brow, and mouth option. Also
     * indexes features by slot so a character menu can list the choices for
     * one slot. Which feature a given entity wears lives on its
     * AppearanceHandle, never here. Auto-triggers an on-demand load via
     * InternalLoader on a name-based cache miss.
     */

    // Palette
    private Object2ObjectOpenHashMap<String, FeatureHandle> featureName2FeatureHandle;
    private Short2ObjectOpenHashMap<FeatureHandle> featureID2FeatureHandle;
    private Object2ObjectOpenHashMap<FeatureSlot, ObjectArrayList<FeatureHandle>> featureSlot2FeatureHandles;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.featureName2FeatureHandle = new Object2ObjectOpenHashMap<>();
        this.featureID2FeatureHandle = new Short2ObjectOpenHashMap<>();
        this.featureSlot2FeatureHandles = new Object2ObjectOpenHashMap<>();

        for (FeatureSlot featureSlot : FeatureSlot.values())
            featureSlot2FeatureHandles.put(featureSlot, new ObjectArrayList<>());

        create(FeatureLoader.class);
    }

    // Management \\

    void addFeature(FeatureHandle handle) {

        featureName2FeatureHandle.put(handle.getFeatureName(), handle);
        featureID2FeatureHandle.put(handle.getFeatureID(), handle);
        featureSlot2FeatureHandles.get(handle.getFeatureSlot()).add(handle);
    }

    // Accessible \\

    public boolean hasFeature(String featureName) {
        return featureName2FeatureHandle.containsKey(featureName);
    }

    public FeatureHandle getFeatureHandleFromFeatureID(short featureID) {
        return featureID2FeatureHandle.get(featureID);
    }

    public FeatureHandle getFeatureHandleFromFeatureName(String featureName) {

        FeatureHandle handle = featureName2FeatureHandle.get(featureName);

        if (handle == null) {
            ((FeatureLoader) internalLoader).request(featureName);
            handle = featureName2FeatureHandle.get(featureName);
        }

        if (handle == null)
            throwException("Feature could not be loaded: \"" + featureName + "\"");

        return handle;
    }

    public ObjectArrayList<FeatureHandle> getFeatureHandlesForSlot(FeatureSlot featureSlot) {
        return featureSlot2FeatureHandles.get(featureSlot);
    }
}
