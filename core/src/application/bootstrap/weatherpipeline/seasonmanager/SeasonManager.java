package application.bootstrap.weatherpipeline.seasonmanager;

import application.bootstrap.weatherpipeline.season.SeasonHandle;
import engine.root.ManagerPackage;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public class SeasonManager extends ManagerPackage {

    /*
     * Owns the season climate palette, keyed by the names the active calendar
     * defines and loaded on demand like clouds and weathers. SeasonBlendSystem
     * resolves where the year sits between those seasons every frame, so
     * season-driven values blend instead of stepping.
     */

    // Systems
    private SeasonBlendSystem seasonBlendSystem;

    // Palette
    private Object2ObjectOpenHashMap<String, SeasonHandle> seasonName2SeasonHandle;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.seasonName2SeasonHandle = new Object2ObjectOpenHashMap<>();

        // Systems
        this.seasonBlendSystem = create(SeasonBlendSystem.class);

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

    // Season Blend \\

    public SeasonHandle getPreviousSeason() {
        return seasonBlendSystem.getPreviousSeason();
    }

    public SeasonHandle getNextSeason() {
        return seasonBlendSystem.getNextSeason();
    }

    public float getSeasonBlendFactor() {
        return seasonBlendSystem.getBlendFactor();
    }

    public float getBlendedBaseTemperature() {
        return seasonBlendSystem.getBaseTemperature();
    }

    public float getBlendedTemperatureVariance() {
        return seasonBlendSystem.getTemperatureVariance();
    }
}
