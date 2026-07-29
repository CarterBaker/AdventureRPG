package application.bootstrap.weatherpipeline.skymanager;

import application.bootstrap.calendarpipeline.clockmanager.ClockManager;
import engine.root.ManagerPackage;

public class SkyManager extends ManagerPackage {

    /*
     * Owns the sky's live color state. SeasonColorBlendBranch resolves the
     * calendar's own season keyframes into a blended tint/sunrise color;
     * SkyColorSystem combines that with time of day and live temperature
     * into the per-grid SkyColorData UBO every frame.
     */

    private ClockManager clockManager;

    private SeasonBlendSystem seasonBlendSystem;
    private SkyColorSystem skyColorSystem;

    // Internal \\

    @Override
    protected void create() {
        this.seasonBlendSystem = create(SeasonBlendSystem.class);
        this.skyColorSystem = create(SkyColorSystem.class);
    }

    @Override
    protected void get() {
        this.clockManager = get(ClockManager.class);
    }

    @Override
    protected void awake() {
        seasonBlendSystem.assignData(clockManager.getCalendarHandle());
        skyColorSystem.assignData(seasonBlendSystem);
    }

    /*
     * Re-resolves the season color keyframes against the active world's
     * calendar. Call after ClockManager.switchWorld().
     */
    public void refreshCalendar() {
        seasonBlendSystem.assignData(clockManager.getCalendarHandle());
    }
}