package application.bootstrap.geometrypipeline.skinnedbuffer;

import application.bootstrap.shaderpipeline.texturemanager.TextureManager;
import engine.root.EngineUtility;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;

public class SkinnedVariantUtility extends EngineUtility {

    /*
     * Parses a look's material variants from its data: an array of entries,
     * each a "name", the "texture" painting it and how far its "relief" rises
     * in blocks, and the owner's "default" naming the variant worn when none
     * is picked (the first when it names none). One path for an overlay's
     * styles and a feature's variants alike.
     */

    // Parse \\

    public static SkinnedVariantStruct parse(
            ArpgObjectStruct ownerArpg,
            String listKey,
            TextureManager textureManager,
            String ownerName,
            String sourceName) {

        ArpgArrayStruct listArpg = ArpgUtility.validateArray(ownerArpg, listKey);

        if (listArpg.size() == 0)
            throwException("\"" + ownerName + "\" offers no " + listKey + ". Source: " + sourceName);

        String[] names = new String[listArpg.size()];
        int[] tiles = new int[listArpg.size()];
        float[] reliefs = new float[listArpg.size()];

        for (int i = 0; i < names.length; i++)
            parseVariant(listArpg.get(i).getAsObject(), names, tiles, reliefs, i, textureManager, ownerName,
                    sourceName);

        return new SkinnedVariantStruct(names, tiles, reliefs, resolveDefault(ownerArpg, names, ownerName,
                sourceName));
    }

    private static void parseVariant(
            ArpgObjectStruct variantArpg,
            String[] names,
            int[] tiles,
            float[] reliefs,
            int index,
            TextureManager textureManager,
            String ownerName,
            String sourceName) {

        String name = ArpgUtility.validateString(variantArpg, "name");

        for (int i = 0; i < index; i++)
            if (names[i].equals(name))
                throwException("\"" + ownerName + "\" names \"" + name + "\" twice. Source: " + sourceName);

        float relief = ArpgUtility.getFloat(variantArpg, "relief", 0f);

        if (relief < 0f)
            throwException("\"" + ownerName + "\" gives \"" + name + "\" a negative relief. Source: " + sourceName);

        names[index] = name;
        tiles[index] = textureManager.getTileIDFromTextureName(ArpgUtility.validateString(variantArpg, "texture"));
        reliefs[index] = relief;
    }

    private static int resolveDefault(ArpgObjectStruct ownerArpg, String[] names, String ownerName, String sourceName) {

        if (!ArpgUtility.hasString(ownerArpg, "default"))
            return 0;

        String name = ownerArpg.get("default").getAsString();

        for (int i = 0; i < names.length; i++)
            if (names[i].equals(name))
                return i;

        return throwException("\"" + ownerName + "\" defaults to \"" + name + "\", which it does not offer. Source: "
                + sourceName);
    }
}
