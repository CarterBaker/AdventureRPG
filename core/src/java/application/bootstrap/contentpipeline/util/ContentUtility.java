package application.bootstrap.contentpipeline.util;

import application.bootstrap.worldpipeline.util.CoverageUtility;
import engine.root.EngineSetting;
import engine.root.EngineUtility;

public class ContentUtility extends EngineUtility {

    /*
     * Packing for contents, the ore, rock or root a cell holds and how much of
     * it. A cell's content is one short in its subchunk's content palette, the
     * content's ID above the amount's bits, and NONE — zero — holds nothing,
     * since content ID zero is the registry's reserved sentinel; an amount of
     * zero holds nothing too, so every empty cell packs to the same NONE. The
     * geometry meta word carries the same word above the quad's flags. An
     * amount shows as coverage does: as the coverage level its share of the
     * content's most reaches, so a content grows over its host through the
     * covering's own growth map and reveal shares. Every system that reads or
     * writes contents resolves them through here.
     */

    // Settings
    public static final short NONE = EngineSetting.CONTENT_NONE;
    public static final int AMOUNT_MAX = EngineSetting.CONTENT_AMOUNT_MAX;

    // Pack \\

    public static short pack(int contentID, int amount) {

        int clampedAmount = Math.min(Math.max(amount, 0), AMOUNT_MAX);

        if (contentID == EngineSetting.REGISTRY_RESERVED_ID || clampedAmount == 0)
            return NONE;

        return (short) ((contentID << EngineSetting.CONTENT_AMOUNT_BITS) | clampedAmount);
    }

    // Unpack \\

    public static int getContentID(short content) {
        return (content & 0xFFFF) >>> EngineSetting.CONTENT_AMOUNT_BITS;
    }

    public static int getAmount(short content) {
        return content & AMOUNT_MAX;
    }

    public static boolean isContained(short content) {
        return content != NONE;
    }

    // Reveal \\

    // The coverage level an amount shows at, the share of the content's most it holds rounded to the nearest level
    public static int toRevealLevel(int amount, int maxAmount) {
        return (amount * CoverageUtility.LEVEL_MAX + maxAmount / 2) / maxAmount;
    }

    // Meta \\

    // The content's bits of a geometry meta word, the packed word shifted above the quad's flags
    public static int toMetaBits(short content, int contentShift) {
        return (content & EngineSetting.CONTENT_WORD_MASK) << contentShift;
    }
}
