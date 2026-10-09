// CloudManager.java
package application.bootstrap.weatherpipeline.cloudmanager;

import application.bootstrap.shaderpipeline.texturemanager.TextureManager;
import application.bootstrap.weatherpipeline.cloud.CloudHandle;
import application.bootstrap.weatherpipeline.util.CloudNoiseUtility;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class CloudManager extends ManagerPackage {

    /*
     * Owns the shared, immutable cloud archetype palette and assigns every
     * archetype a stable sequential type index the weather map uses to place it
     * in a layer. Also owns the cloud noise texture every cloud shader reads
     * its shapes from, baked once at awake and released on dispose.
     */

    // Internal
    private TextureManager textureManager;

    // Palette
    private Object2IntOpenHashMap<String> cloudName2CloudID;
    private ObjectArrayList<CloudHandle> cloudID2CloudHandle;

    // Cloud Type Registry
    private int nextCloudTypeIndex;

    // Noise
    private int cloudNoiseTexture;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.cloudName2CloudID = RegistryUtility.createNameIndex();
        this.cloudID2CloudHandle = RegistryUtility.createPalette();

        // Cloud Type Registry
        this.nextCloudTypeIndex = 0;

        create(CloudLoader.class);
    }

    @Override
    protected void get() {
        this.textureManager = get(TextureManager.class);
    }

    @Override
    protected void awake() {
        this.cloudNoiseTexture = textureManager.createFloatTexture2D(
                CloudNoiseUtility.bake(),
                EngineSetting.CLOUD_NOISE_SIZE,
                EngineSetting.CLOUD_NOISE_SIZE,
                EngineSetting.GL_REPEAT,
                EngineSetting.GL_LINEAR);
    }

    @Override
    protected void dispose() {
        textureManager.deleteTexture2D(cloudNoiseTexture);
    }

    // Management \\

    short registerCloudName(String cloudName) {
        return (short) RegistryUtility.registerID(
                cloudName2CloudID, cloudID2CloudHandle, cloudName, EngineSetting.REGISTRY_SHORT_ID_COUNT);
    }

    void addCloud(CloudHandle cloudHandle) {

        if (cloudID2CloudHandle.get(cloudHandle.getCloudID()) != null)
            throwException("Duplicate cloud name: '" + cloudHandle.getCloudName()
                    + "' was registered more than once");

        if (nextCloudTypeIndex >= EngineSetting.MAX_CLOUD_TYPES)
            throwException("Exceeded EngineSetting.MAX_CLOUD_TYPES (" + EngineSetting.MAX_CLOUD_TYPES
                    + ") while registering cloud \"" + cloudHandle.getCloudName()
                    + "\" — raise MAX_CLOUD_TYPES or reduce the number of cloud archetypes.");

        cloudHandle.assignCloudTypeIndex(nextCloudTypeIndex);
        nextCloudTypeIndex++;

        cloudID2CloudHandle.set(cloudHandle.getCloudID(), cloudHandle);
    }

    // On-Demand \\

    public void request(String cloudName) {
        ((CloudLoader) internalLoader).request(cloudName);
    }

    // Accessible \\

    public boolean hasCloud(String cloudName) {
        return RegistryUtility.getHandle(cloudName2CloudID, cloudID2CloudHandle, cloudName) != null;
    }

    public short getCloudIDFromCloudName(String cloudName) {

        if (!hasCloud(cloudName))
            request(cloudName);

        if (!hasCloud(cloudName))
            throwException("Cloud \"" + cloudName + "\" was not registered after its on-demand load completed — "
                    + "the loaded file must declare a different cloud name than the one requested. "
                    + "Check for a resource-name/path mismatch between the cloud directory and its declared name.");

        return (short) cloudName2CloudID.getInt(cloudName);
    }

    public CloudHandle getCloudHandleFromCloudID(short cloudID) {

        CloudHandle handle = RegistryUtility.getHandle(cloudID2CloudHandle, cloudID);

        if (handle == null)
            throwException("No handle registered for cloud ID: " + cloudID);

        return handle;
    }

    public CloudHandle getCloudHandleFromCloudName(String cloudName) {
        return getCloudHandleFromCloudID(getCloudIDFromCloudName(cloudName));
    }

    public int getCloudTypeCount() {
        return nextCloudTypeIndex;
    }

    public int getCloudNoiseTexture() {
        return cloudNoiseTexture;
    }
}