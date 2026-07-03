package application.bootstrap.weatherpipeline.seasonmanager;

import application.bootstrap.weatherpipeline.season.Season;
import application.bootstrap.weatherpipeline.season.SeasonHandle;
import engine.root.ManagerPackage;

public class SeasonManager extends ManagerPackage {

    /*
     * Owns the season climate palette for the engine lifetime. Seasons are
     * a fixed four-value set tied to the existing Season enum — not an
     * open-ended named registry — so handles are stored by enum ordinal
     * rather than a hashed ID. Drives loading via InternalLoader; all four
     * seasons are expected to load during bootstrap, never on demand.
     */

    // Palette
    private final SeasonHandle[] season2Handle = new SeasonHandle[Season.values().length];

    // Base \\

    @Override
    protected void create() {
        create(InternalLoader.class);
    }

    // Management \\

    void addSeason(SeasonHandle seasonHandle) {
        season2Handle[seasonHandle.getSeason().ordinal()] = seasonHandle;
    }

    // Accessible \\

    public boolean hasSeason(Season season) {
        return season2Handle[season.ordinal()] != null;
    }

    public SeasonHandle getSeasonHandleFromSeason(Season season) {

        SeasonHandle handle = season2Handle[season.ordinal()];

        if (handle == null)
            throwException("No handle registered for season: " + season);

        return handle;
    }
}