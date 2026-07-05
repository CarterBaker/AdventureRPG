package application.bootstrap.weatherpipeline.seasonmanager;

import application.bootstrap.weatherpipeline.season.SeasonHandle;
import engine.root.ManagerPackage;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public class SeasonManager extends ManagerPackage {

    /*
     * Owns the season climate palette for the engine lifetime. Seasons are
     * no longer a fixed enum — the active calendar defines whichever named
     * seasons it wants (see CalendarData.getSeasons()), so this registry is
     * keyed by name exactly like clouds, weathers, and biomes, and supports
     * the same on-demand loading via InternalLoader on a cache miss.
     */

    // Palette
    private Object2ObjectOpenHashMap<String, SeasonHandle> seasonName2SeasonHandle;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.seasonName2SeasonHandle = new Object2ObjectOpenHashMap<>();

        create(SeasonLoader.class);
    }

    // Management \\

    void addSeason(SeasonHandle seasonHandle) {
        seasonName2SeasonHandle.put(seasonHandle.getSeasonName(), seasonHandle);
    }

    // On-Demand \\

    public void request(String seasonName) {
        ((SeasonLoader) internalLoader).request(seasonName);
    }

    // Accessible \\

    public boolean hasSeason(String seasonName) {
        return seasonName2SeasonHandle.containsKey(seasonName);
    }

    public SeasonHandle getSeasonHandleFromSeasonName(String seasonName) {

        SeasonHandle handle = seasonName2SeasonHandle.get(seasonName);

        if (handle == null) {
            request(seasonName);
            handle = seasonName2SeasonHandle.get(seasonName);
        }

        if (handle == null)
            throwException("No handle registered for season: \"" + seasonName + "\"");

        return handle;
    }
}