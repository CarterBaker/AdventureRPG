package lwjgl3;

import java.io.File;

import engine.root.EngineSetting;

public class Lwjgl3LauncherBake {

    /*
     * Entry point for the texture bake tool, run from the project root
     * through the lwjgl3:bake Gradle task. Bakes every tile whose source
     * images changed since its last bake, or every tile with force, removes
     * baked files whose sources are gone, and exits non-zero when any tile
     * fails.
     */

    // Entry \\

    public static void main(String[] args) {

        boolean force = args.length == 1 && args[0].equals(EngineSetting.TEXTURE_BAKE_TOOL_FORCE);
        boolean complete = args.length == 0 || force;
        boolean passed = complete
                && Lwjgl3BakeUtility.bake(new File(EngineSetting.TEXTURE_BAKE_TOOL_ASSET_ROOT), force);

        if (!complete)
            System.out.println(EngineSetting.TEXTURE_BAKE_TOOL_USAGE);

        System.exit(passed ? EngineSetting.ARPG_TOOL_EXIT_SUCCESS : EngineSetting.ARPG_TOOL_EXIT_FAILURE);
    }
}
