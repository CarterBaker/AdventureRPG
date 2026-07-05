package application.bootstrap.weatherpipeline.cloudmanager;

import application.bootstrap.weatherpipeline.cloud.CloudHandle;
import engine.root.ManagerPackage;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.objects.Object2ShortOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;

public class CloudManager extends ManagerPackage {

    /*
     * Owns the cloud archetype palette for the engine lifetime. Drives
     * loading via InternalLoader and exposes the standard registry API.
     * Cloud archetypes are immutable and shared — weathers and overhead
     * cells hold CloudHandle references directly, never clones.
     */

    // Palette
    private Object2ShortOpenHashMap<String> cloudName2CloudID;
    private Short2ObjectOpenHashMap<CloudHandle> cloudID2CloudHandle;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.cloudName2CloudID = new Object2ShortOpenHashMap<>();
        this.cloudID2CloudHandle = new Short2ObjectOpenHashMap<>();

        create(CloudLoader.class);
    }

    // Management \\

    void addCloud(CloudHandle cloudHandle) {

        if (cloudID2CloudHandle.containsKey(cloudHandle.getCloudID())) {
            CloudHandle existing = cloudID2CloudHandle.get(cloudHandle.getCloudID());
            if (RegistryUtility.isCollision(cloudHandle.getCloudName(), existing.getCloudName(),
                    cloudHandle.getCloudID()))
                throwException("Cloud ID collision: '"
                        + cloudHandle.getCloudName() + "' collides with '"
                        + existing.getCloudName() + "' (ID " + cloudHandle.getCloudID()
                        + ") — rename one cloud to resolve");
        }

        cloudName2CloudID.put(cloudHandle.getCloudName(), cloudHandle.getCloudID());
        cloudID2CloudHandle.put(cloudHandle.getCloudID(), cloudHandle);
    }

    // On-Demand \\

    public void request(String cloudName) {
        ((CloudLoader) internalLoader).request(cloudName);
    }

    // Accessible \\

    public boolean hasCloud(String cloudName) {
        return cloudName2CloudID.containsKey(cloudName);
    }

    public short getCloudIDFromCloudName(String cloudName) {

        if (!cloudName2CloudID.containsKey(cloudName))
            request(cloudName);

        return cloudName2CloudID.getShort(cloudName);
    }

    public CloudHandle getCloudHandleFromCloudID(short cloudID) {

        CloudHandle handle = cloudID2CloudHandle.get(cloudID);

        if (handle == null)
            throwException("No handle registered for cloud ID: " + cloudID);

        return handle;
    }

    public CloudHandle getCloudHandleFromCloudName(String cloudName) {
        return getCloudHandleFromCloudID(getCloudIDFromCloudName(cloudName));
    }
}