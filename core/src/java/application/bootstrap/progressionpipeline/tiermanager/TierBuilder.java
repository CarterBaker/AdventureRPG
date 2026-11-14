package application.bootstrap.progressionpipeline.tiermanager;

import java.io.File;

import application.bootstrap.progressionpipeline.tier.TierData;
import application.bootstrap.progressionpipeline.tier.TierHandle;
import engine.graphics.color.Color;
import engine.root.BuilderPackage;
import engine.root.EngineSetting;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class TierBuilder extends BuilderPackage {

    /*
     * Parses tier ARPG files and builds TierHandle instances. Each file lists
     * its tiers under a "tiers" array, lowest first, each named by its "name"
     * field: its "color" as red, green and blue, the "multiplier" it lends
     * gear stats, the "bonuses" it rolls, the "weight" it is drawn by, and a
     * "frame" object of glow, pulse, sweep, motes and sheen. A tier's rank is
     * its place in the list. Bootstrap-only.
     */

    // Internal
    private TierManager tierManager;

    // Base \\

    @Override
    protected void get() {
        this.tierManager = get(TierManager.class);
    }

    // Build \\

    ObjectArrayList<TierHandle> build(File arpgFile) {

        ArpgObjectStruct rootArpg = ArpgUtility.loadObject(arpgFile);
        ArpgArrayStruct tierArray = ArpgUtility.validateArray(rootArpg, "tiers");
        ObjectArrayList<TierHandle> tiers = new ObjectArrayList<>();

        for (int i = 0; i < tierArray.size(); i++)
            tiers.add(parseTier(tierArray.get(i).getAsObject(), arpgFile));

        return tiers;
    }

    // Parse \\

    private TierHandle parseTier(ArpgObjectStruct tierArpg, File arpgFile) {

        String tierName = ArpgUtility.validateString(tierArpg, "name");
        String owner = "Tier '" + tierName + "' in " + arpgFile.getPath();
        short tierID = tierManager.registerTierName(tierName);
        float multiplier = ArpgUtility.getFloat(tierArpg, "multiplier", EngineSetting.TIER_DEFAULT_MULTIPLIER);
        int bonusCount = ArpgUtility.getInt(tierArpg, "bonuses", EngineSetting.TIER_DEFAULT_BONUS_COUNT);
        float weight = ArpgUtility.getFloat(tierArpg, "weight", EngineSetting.TIER_DEFAULT_WEIGHT);
        ArpgObjectStruct frameArpg = ArpgUtility.hasObject(tierArpg, "frame")
                ? tierArpg.getAsObject("frame")
                : new ArpgObjectStruct();

        if (multiplier <= 0f || bonusCount < 0 || weight < 0f)
            throwException(owner + " needs a positive \"multiplier\", and \"bonuses\" and \"weight\" "
                    + "of at least 0.");

        TierData tierData = new TierData(
                tierName,
                tierID,
                tierID - 1,
                ArpgUtility.getString(tierArpg, "display_name", tierName),
                parseColor(tierArpg, owner),
                multiplier,
                bonusCount,
                weight,
                parseFrame(frameArpg, "glow"),
                parseFrame(frameArpg, "pulse"),
                parseFrame(frameArpg, "sweep"),
                parseFrame(frameArpg, "motes"),
                parseFrame(frameArpg, "sheen"));

        TierHandle tierHandle = create(TierHandle.class);
        tierHandle.constructor(tierData);

        return tierHandle;
    }

    private Color parseColor(ArpgObjectStruct tierArpg, String owner) {

        if (!ArpgUtility.hasArray(tierArpg, "color"))
            throwException(owner + " needs a \"color\" of red, green and blue.");

        ArpgArrayStruct colorArpg = ArpgUtility.validateArray(
                tierArpg, "color", EngineSetting.TIER_COLOR_CHANNEL_COUNT);

        return new Color(
                colorArpg.get(0).getAsFloat(),
                colorArpg.get(1).getAsFloat(),
                colorArpg.get(2).getAsFloat());
    }

    private float parseFrame(ArpgObjectStruct frameArpg, String key) {
        return Math.max(0f, Math.min(1f, ArpgUtility.getFloat(frameArpg, key, 0f)));
    }
}
