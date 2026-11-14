package application.bootstrap.progressionpipeline.util;

import java.util.function.ToIntFunction;

import engine.root.EngineUtility;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;

public class ProgressionArpgUtility extends EngineUtility {

    /*
     * Shared parsing for the progression data files. parseValueMap() reads an
     * optional object of names to numbers into a ValueMapStruct, resolving
     * every name to its registry ID through the owning manager, so an unknown
     * name fails at load with the file and key that named it. parseNames()
     * reads an optional array of names the same way. Bootstrap-only.
     */

    // Parse \\

    public static ValueMapStruct parseValueMap(
            ArpgObjectStruct ownerArpg,
            String key,
            ToIntFunction<String> resolver,
            String owner) {

        if (!ArpgUtility.hasObject(ownerArpg, key))
            return ValueMapStruct.EMPTY;

        ArpgObjectStruct mapArpg = ownerArpg.getAsObject(key);
        short[] ids = new short[mapArpg.size()];
        float[] values = new float[mapArpg.size()];
        int index = 0;

        for (String name : mapArpg.keySet()) {

            if (!mapArpg.get(name).isValue() || !mapArpg.get(name).getAsValue().isNumber())
                throwException(owner + " lists \"" + name + "\" under \"" + key + "\" without a number.");

            ids[index] = (short) resolver.applyAsInt(name);
            values[index] = mapArpg.get(name).getAsFloat();
            index++;
        }

        return new ValueMapStruct(ids, values);
    }

    public static short[] parseNames(
            ArpgObjectStruct ownerArpg,
            String key,
            ToIntFunction<String> resolver) {

        if (!ArpgUtility.hasArray(ownerArpg, key))
            return new short[0];

        ArpgArrayStruct namesArpg = ownerArpg.getAsArray(key);
        short[] ids = new short[namesArpg.size()];

        for (int i = 0; i < ids.length; i++)
            ids[i] = (short) resolver.applyAsInt(namesArpg.get(i).getAsString());

        return ids;
    }
}
