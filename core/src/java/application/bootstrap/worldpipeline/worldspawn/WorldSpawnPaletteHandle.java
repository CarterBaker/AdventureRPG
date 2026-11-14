package application.bootstrap.worldpipeline.worldspawn;

import engine.root.HandlePackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class WorldSpawnPaletteHandle extends HandlePackage {

    /*
     * A subchunk's persistent list of world spawn structs, the spots every
     * NPC living in the chunk comes into the world at. A spot whose NPC has
     * fallen is removed, so it stays empty for as long as the chunk keeps its
     * items.
     */

    private ObjectArrayList<WorldSpawnStruct> spawns;

    // Constructor \\

    public void constructor() {
        this.spawns = new ObjectArrayList<>();
    }

    // Management \\

    public void addSpawn(WorldSpawnStruct spawn) {
        spawns.add(spawn);
    }

    public void removeSpawn(WorldSpawnStruct spawn) {
        spawns.remove(spawn);
    }

    public void clear() {
        spawns.clear();
    }

    // Accessible \\

    public ObjectArrayList<WorldSpawnStruct> getSpawns() {
        return spawns;
    }

    public boolean isEmpty() {
        return spawns.isEmpty();
    }

    public int size() {
        return spawns.size();
    }
}
