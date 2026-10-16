package application.bootstrap.progressionpipeline.tiermanager;

import application.bootstrap.progressionpipeline.tier.TierHandle;
import application.bootstrap.worldpipeline.util.BiomeFieldUtility;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class TierManager extends ManagerPackage {

    /*
     * Owns the gear tier palette, from no color to every color. A tier's ID is
     * its rank: tiers are registered in file order, lowest first, and a name
     * declared twice is rejected. rollTier() is the one way a tier is drawn
     * for a new piece of gear, its weights tilted toward the rarer tiers by
     * the luck of whoever finds it and the bias of what it is found in, and
     * seeded so the same draw always lands the same tier. Every tier is
     * resolved in awake().
     */

    // Palette
    private Object2IntOpenHashMap<String> tierName2TierID;
    private ObjectArrayList<TierHandle> tierID2TierHandle;

    // Scratch
    private float[] weightScratch;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.tierName2TierID = RegistryUtility.createNameIndex();
        this.tierID2TierHandle = RegistryUtility.createPalette();
        create(TierLoader.class);
    }

    @Override
    protected void awake() {

        internalLoader.requestAll();

        if (getTierIDCount() <= EngineSetting.REGISTRY_RESERVED_ID + 1)
            throwException("No gear tiers are defined — at least one tier is needed to roll gear.");

        // Scratch
        this.weightScratch = new float[getTierIDCount()];
    }

    // Management \\

    short registerTierName(String tierName) {
        return (short) RegistryUtility.registerID(
                tierName2TierID, tierID2TierHandle, tierName,
                EngineSetting.REGISTRY_SHORT_ID_COUNT);
    }

    void addTierHandle(TierHandle tierHandle) {

        short tierID = tierHandle.getTierID();

        if (tierID2TierHandle.get(tierID) != null)
            throwException("Duplicate tier name: '" + tierHandle.getTierName()
                    + "' is declared more than once — every tier name must be unique");

        tierID2TierHandle.set(tierID, tierHandle);
    }

    // Roll \\

    // A tier drawn for new gear — each tier's weight tilted toward the rarer ranks by the finder's luck and the
    // source's bias, the same seed always landing the same tier
    public short rollTier(long seed, float luck, float bias) {

        float tilt = (luck + bias) * EngineSetting.TIER_LUCK_TILT;
        float total = 0f;

        for (int tierID = EngineSetting.REGISTRY_RESERVED_ID + 1; tierID < weightScratch.length; tierID++) {

            TierHandle tierHandle = tierID2TierHandle.get(tierID);
            float weight = tierHandle.getWeight() * (float) Math.exp(tierHandle.getRank() * tilt);

            weightScratch[tierID] = weight;
            total += weight;
        }

        if (total <= 0f)
            return getLowestTierID();

        float pick = BiomeFieldUtility.hash01(seed ^ EngineSetting.TIER_ROLL_SALT) * total;

        for (int tierID = EngineSetting.REGISTRY_RESERVED_ID + 1; tierID < weightScratch.length; tierID++) {

            pick -= weightScratch[tierID];

            if (pick < 0f)
                return (short) tierID;
        }

        return (short) (weightScratch.length - 1);
    }

    public short getLowestTierID() {
        return (short) (EngineSetting.REGISTRY_RESERVED_ID + 1);
    }

    // Accessible \\

    public boolean hasTier(String tierName) {
        return RegistryUtility.getHandle(tierName2TierID, tierID2TierHandle, tierName) != null;
    }

    public short getTierIDFromTierName(String tierName) {

        if (!hasTier(tierName) && internalLoader != null)
            ((TierLoader) internalLoader).request(tierName);

        if (!hasTier(tierName))
            throwException("Tier not found after load: \"" + tierName + "\"");

        return (short) tierName2TierID.getInt(tierName);
    }

    public TierHandle getTierHandleFromTierID(short tierID) {

        TierHandle handle = RegistryUtility.getHandle(tierID2TierHandle, tierID);

        if (handle == null)
            throwException("Tier ID not found: " + tierID);

        return handle;
    }

    public TierHandle getTierHandleFromTierName(String tierName) {
        return getTierHandleFromTierID(getTierIDFromTierName(tierName));
    }

    // Every tier ID lies below this — the length of an array indexed by tier ID
    public int getTierIDCount() {
        return tierID2TierHandle.size();
    }
}
