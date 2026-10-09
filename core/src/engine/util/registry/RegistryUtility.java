package engine.util.registry;

import engine.root.EngineSetting;
import engine.root.EngineUtility;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class RegistryUtility extends EngineUtility {

    /*
     * Assigns registry IDs. A manager keeps a name-to-ID map and a palette
     * list indexed by ID, and registerID hands each new name the next slot in
     * registration order, so IDs are dense, unique within their manager and
     * never collide; slot 0 is the reserved sentinel. IDs last one run, so
     * anything persisted stores names. A name seed is the one stable number
     * derived from a name, an FNV-1a hash world generation salts its noise
     * with, so the same content always generates the same world.
     */

    // Constants \\

    private static final int FNV_OFFSET_BASIS = EngineSetting.FNV_OFFSET_BASIS;
    private static final int FNV_PRIME = EngineSetting.FNV_PRIME;

    public static final short RESERVED_ID = EngineSetting.REGISTRY_RESERVED_ID;
    public static final int ID_NONE = EngineSetting.REGISTRY_ID_NONE;

    // Palette \\

    public static <T> ObjectArrayList<T> createPalette() {

        ObjectArrayList<T> palette = new ObjectArrayList<>();
        palette.add(null);

        return palette;
    }

    public static Object2IntOpenHashMap<String> createNameIndex() {

        Object2IntOpenHashMap<String> name2ID = new Object2IntOpenHashMap<>();
        name2ID.defaultReturnValue(ID_NONE);

        return name2ID;
    }

    public static void clearPalette(Object2IntOpenHashMap<String> name2ID, ObjectArrayList<?> palette) {

        name2ID.clear();
        palette.clear();
        palette.add(null);
    }

    // Registration \\

    public static int registerID(
            Object2IntOpenHashMap<String> name2ID,
            ObjectArrayList<?> palette,
            String name,
            int idCount) {

        if (name == null || name.isEmpty())
            throwException("Registry name cannot be null or empty");

        int id = name2ID.getInt(name);

        if (id != ID_NONE)
            return id;

        id = palette.size();

        if (id >= idCount)
            throwException("Registry is full: \"" + name + "\" needs ID " + id
                    + ", past the last available ID " + (idCount - 1));

        palette.add(null);
        name2ID.put(name, id);

        return id;
    }

    public static <T> int registerHandle(
            Object2IntOpenHashMap<String> name2ID,
            ObjectArrayList<T> palette,
            String name,
            T handle,
            int idCount) {

        int id = registerID(name2ID, palette, name, idCount);
        palette.set(id, handle);

        return id;
    }

    // Lookup \\

    public static <T> T getHandle(ObjectArrayList<T> palette, int id) {
        return id > RESERVED_ID && id < palette.size() ? palette.get(id) : null;
    }

    public static <T> T getHandle(
            Object2IntOpenHashMap<String> name2ID,
            ObjectArrayList<T> palette,
            String name) {
        return getHandle(palette, name2ID.getInt(name));
    }

    // Name Seed \\

    public static int toNameSeed(String name) {

        if (name == null || name.isEmpty())
            throwException("Name seed source cannot be null or empty");

        int hash = FNV_OFFSET_BASIS;

        for (int i = 0; i < name.length(); i++) {
            hash ^= name.charAt(i);
            hash *= FNV_PRIME;
        }

        return hash & EngineSetting.REGISTRY_NAME_SEED_MASK;
    }
}
