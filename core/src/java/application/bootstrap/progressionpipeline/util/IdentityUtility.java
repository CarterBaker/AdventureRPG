package application.bootstrap.progressionpipeline.util;

import engine.root.EngineSetting;
import engine.root.EngineUtility;

public class IdentityUtility extends EngineUtility {

    /*
     * Builds the stable identities beings remember one another by. A player's
     * comes from its character name, an NPC's from the world spawn it came
     * from — its home chunk and spot — so a resident unloaded with its chunk
     * and loaded again is the same being to everyone who knew it. Identities
     * are 64-bit FNV-1a hashes, salted apart so a name and a spawn never
     * collide by kind. IDENTITY_NONE marks a being nothing remembers.
     */

    // Identity \\

    public static long fromName(String name) {

        long hash = EngineSetting.FNV64_OFFSET_BASIS ^ EngineSetting.IDENTITY_NAME_SALT;

        for (int i = 0; i < name.length(); i++) {
            hash ^= name.charAt(i);
            hash *= EngineSetting.FNV64_PRIME;
        }

        return finish(hash);
    }

    public static long fromSpawn(long chunkCoordinate, int blockX, int blockY, int blockZ) {

        long hash = EngineSetting.FNV64_OFFSET_BASIS ^ EngineSetting.IDENTITY_SPAWN_SALT;

        hash = (hash ^ chunkCoordinate) * EngineSetting.FNV64_PRIME;
        hash = (hash ^ blockX) * EngineSetting.FNV64_PRIME;
        hash = (hash ^ blockY) * EngineSetting.FNV64_PRIME;
        hash = (hash ^ blockZ) * EngineSetting.FNV64_PRIME;

        return finish(hash);
    }

    // A hash never lands on the none marker
    private static long finish(long hash) {
        return hash == EngineSetting.IDENTITY_NONE ? hash + 1L : hash;
    }
}
