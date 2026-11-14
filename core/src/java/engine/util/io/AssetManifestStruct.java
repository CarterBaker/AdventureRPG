package engine.util.io;

import java.io.File;

import engine.root.EngineSetting;
import engine.root.StructPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class AssetManifestStruct extends StructPackage {

    /*
     * The assets an engine queues at bootstrap. Each entry names one asset by
     * its path below the asset root without extension, or a folder whose every
     * asset it admits. EVERYTHING admits all, as the game and editor run; the
     * launcher runs a whitelist, so only what its menus draw with is ever
     * loaded and anything left out fails loudly the moment it is asked for.
     */

    // Everything
    public static final AssetManifestStruct EVERYTHING = new AssetManifestStruct(true, new ObjectArrayList<>());

    // Internal
    private final boolean admitsEverything;
    private final ObjectArrayList<String> assetPaths;
    private final ObjectArrayList<String> folderPrefixes;
    private final File assetRoot;

    // Constructor \\

    public AssetManifestStruct(boolean admitsEverything, ObjectArrayList<String> assetPaths) {

        // Internal
        this.admitsEverything = admitsEverything;
        this.assetPaths = assetPaths;
        this.folderPrefixes = new ObjectArrayList<>(assetPaths.size());
        this.assetRoot = new File(EngineSetting.ASSET_ROOT_PATH);

        for (int i = 0; i < assetPaths.size(); i++)
            folderPrefixes.add(assetPaths.get(i) + EngineSetting.PATH_SEPARATOR);
    }

    // Accessible \\

    public boolean admits(File file) {
        return admitsEverything || matches(FileUtility.getPathWithFileNameWithoutExtension(assetRoot, file));
    }

    private boolean matches(String assetPath) {

        for (int i = 0; i < assetPaths.size(); i++)
            if (assetPath.equals(assetPaths.get(i)) || assetPath.startsWith(folderPrefixes.get(i)))
                return true;

        return false;
    }
}
