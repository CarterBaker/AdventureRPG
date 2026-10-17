package editor.bootstrap.worldeditorpipeline.worldeditormanager;

import application.bootstrap.worldpipeline.util.WorldElevationUtility;
import editor.bootstrap.worldeditorpipeline.worldheight.WorldHeightEntryStruct;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class WorldHeightBranch extends BranchPackage {

    /*
     * Keeps the height layer's palette: one level every level step from the
     * brightest down to the darkest, neutral among them, each named for how
     * far it raises or lowers the ground. The selected level, neutral to
     * begin with, is what the brush and fill paint on the height layer, and
     * may be any level, since picking selects the exact level under the
     * pointer. Every level is described the same way for the status line.
     */

    // Palette
    private ObjectArrayList<WorldHeightEntryStruct> palette;
    private int selectedLevel;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.palette = new ObjectArrayList<>();
        this.selectedLevel = EngineSetting.WORLD_HEIGHT_LAYER_NEUTRAL_LEVEL;

        int maxLevel = EngineSetting.WORLD_HEIGHT_LAYER_MAX_LEVEL;
        int step = EditorSetting.WORLD_EDITOR_HEIGHT_LEVEL_STEP;

        addEntry(maxLevel);

        for (int level = maxLevel / step * step; level >= 0; level -= step)
            if (level != maxLevel)
                addEntry(level);
    }

    private void addEntry(int level) {
        palette.add(new WorldHeightEntryStruct(level, describeLevel(level)));
    }

    // Selection \\

    // False when the level is already selected
    boolean select(int level) {

        int clamped = Math.max(0, Math.min(EngineSetting.WORLD_HEIGHT_LAYER_MAX_LEVEL, level));

        if (clamped == selectedLevel)
            return false;

        this.selectedLevel = clamped;
        return true;
    }

    int getSelectedLevel() {
        return selectedLevel;
    }

    ObjectArrayList<WorldHeightEntryStruct> getPalette() {
        return palette;
    }

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
