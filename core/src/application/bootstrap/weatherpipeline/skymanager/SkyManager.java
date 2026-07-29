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

    private SeasonBlendBranch seasonColorBlendBranch;
    private SkyColorSystem skyColorSystem;

    // Internal \\

    @Override
    protected void create() {
        this.seasonColorBlendBranch = create(SeasonBlendBranch.class);
        this.skyColorSystem = create(SkyColorSystem.class);
    }

    @Override
    protected void get() {
        this.clockManager = get(ClockManager.class);
    }

    @Override
    protected void awake() {
        seasonColorBlendBranch.assignData(clockManager.getCalendarHandle());
        skyColorSystem.assignData(seasonColorBlendBranch);
    }

    /*
     * Re-resolves the season color keyframes against the active world's
     * calendar. Call after ClockManager.switchWorld().
     */
    public void refreshCalendar() {
        seasonColorBlendBranch.assignData(clockManager.getCalendarHandle());
    }
}