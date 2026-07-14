package application.bootstrap.weatherpipeline.overheadmanager;

import application.bootstrap.weatherpipeline.weatherpatternmanager.WeatherPatternLobeStruct;
import application.bootstrap.weatherpipeline.weatherpatternmanager.WeatherPatternManager;
import application.bootstrap.weatherpipeline.weatherpatternmanager.WeatherPatternStruct;
import engine.root.ManagerPackage;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class OverheadManager extends ManagerPackage {

    /*
     * Flattens WeatherPatternManager's active patterns into one renderable
     * cell per lobe — the exact shape CloudRenderSystem consumes. Owns no
     * simulation of its own; reacts each frame to whichever patterns
     * WeatherPatternManager streamed in, retired, or had their lobes
     * rebuilt following a weather change, that same frame.
     */

    private WeatherPatternManager weatherPatternManager;

    private Long2ObjectOpenHashMap<OverheadCellStruct> activeCells;

    @Override
    protected void create() {
        this.activeCells = new Long2ObjectOpenHashMap<>();
    }

    @Override
    protected void get() {
        this.weatherPatternManager = get(WeatherPatternManager.class);
    }

    @Override
    protected void update() {

        ObjectArrayList<WeatherPatternStruct> streamedIn = weatherPatternManager.getPatternsStreamedInThisFrame();
        ObjectArrayList<WeatherPatternStruct> refreshed = weatherPatternManager.getPatternsRefreshedThisFrame();
        ObjectArrayList<WeatherPatternStruct> retired = weatherPatternManager.getPatternsRetiredThisFrame();

        for (int i = 0; i < streamedIn.size(); i++)
            addCellsForPattern(streamedIn.get(i));

        for (int i = 0; i < refreshed.size(); i++)
            refreshCellsForPattern(refreshed.get(i));

        for (int i = 0; i < retired.size(); i++)
            removeCellsForPattern(retired.get(i));
    }

    // Cell Management \\

    private void addCellsForPattern(WeatherPatternStruct pattern) {

        WeatherPatternLobeStruct[] lobes = pattern.getLobes();

        for (int i = 0; i < lobes.length; i++) {
            long lobeKey = computeLobeKey(pattern.getPatternKey(), i);
            activeCells.put(lobeKey, new OverheadCellStruct(lobeKey, pattern, lobes[i]));
        }
    }

    /*
     * A pattern's lobes were just replaced after a weather transition.
     * Removes every cell keyed under the old lobe indices, then adds fresh
     * cells for the new lobe array under the same pattern and slot.
     */
    private void refreshCellsForPattern(WeatherPatternStruct pattern) {

        for (int i = 0; i < pattern.getPreviousLobeCount(); i++)
            activeCells.remove(computeLobeKey(pattern.getPatternKey(), i));

        addCellsForPattern(pattern);
    }

    private void removeCellsForPattern(WeatherPatternStruct pattern) {

        for (int i = 0; i < pattern.getLobeCount(); i++)
            activeCells.remove(computeLobeKey(pattern.getPatternKey(), i));
    }

    private static long computeLobeKey(long patternKey, int lobeIndex) {

        long h = patternKey ^ (0x9E3779B97F4A7C15L * (lobeIndex + 1));
        h ^= (h >>> 33);
        h *= 0xff51afd7ed558ccdL;
        h ^= (h >>> 33);
        h *= 0xc4ceb9fe1a85ec53L;
        h ^= (h >>> 33);

        return h;
    }

    // Accessible \\

    public Long2ObjectOpenHashMap<OverheadCellStruct> getActiveCells() {
        return activeCells;
    }

    public int getActiveCellCount() {
        return activeCells.size();
    }

    public int getActivePatternCount() {
        return weatherPatternManager.getActivePatternCount();
    }
}