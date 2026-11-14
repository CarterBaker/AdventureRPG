package editor.bootstrap.worldeditorpipeline.worldeditormanager;

import application.bootstrap.worldpipeline.util.WorldElevationUtility;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;

class WorldHeightBranch extends BranchPackage {

    /*
     * Names the height layer's levels for the status line: each by how far it
     * raises or lowers the ground as a share of the most it can, the level
     * that leaves the ground as generated named neutral. The layer itself is
     * raised and lowered with the Height Editor's tools, never painted from a
     * palette of levels.
     */

    // Description \\

    String describeLevel(int level) {

        float elevation = WorldElevationUtility.toElevation(level);
        int percent = Math.round(Math.abs(elevation) * EditorSetting.WORLD_EDITOR_HEIGHT_PERCENT_SCALE);

        if (percent == 0)
            return EditorSetting.WORLD_EDITOR_HEIGHT_NEUTRAL;

        return (elevation > 0f ? EditorSetting.WORLD_EDITOR_HEIGHT_RAISE : EditorSetting.WORLD_EDITOR_HEIGHT_LOWER)
                + percent + EditorSetting.WORLD_EDITOR_HEIGHT_PERCENT;
    }
}
