package engine.util.io;

import java.io.File;

import engine.root.EngineSetting;
import engine.root.EngineUtility;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class AssetManifestUtility extends EngineUtility {

    /*
     * Reads an asset manifest from the manifest directory into the whitelist
     * an engine queues its bootstrap assets through. A manifest the engine
     * needs and cannot read is fatal. Never held — all methods static.
     */

    // Load \\

    public static AssetManifestStruct load(String manifestName) {

        File manifestFile = ArpgUtility.resolveFile(new File(EngineSetting.ASSET_MANIFEST_PATH), manifestName);
        ArpgObjectStruct manifestArpg = ArpgUtility.loadObject(manifestFile);
        ArpgArrayStruct assetsArpg = ArpgUtility.validateArray(manifestArpg, EngineSetting.ASSET_MANIFEST_KEY_ASSETS);
        ObjectArrayList<String> assetPaths = new ObjectArrayList<>(assetsArpg.size());

        for (int i = 0; i < assetsArpg.size(); i++)
            assetPaths.add(assetsArpg.get(i).getAsString());

        return new AssetManifestStruct(false, assetPaths);
    }
}
