package application.bootstrap.weatherpipeline.skymanager;

import application.bootstrap.calendarpipeline.clockmanager.ClockManager;
import engine.root.ManagerPackage;

public class SkyManager extends ManagerPackage {

    /*
     * Owns the sky's live color state. Sky color is a weather concern
     * rather than a calendar one — the day/night curve itself lives in
     * the calendar pipeline, but the colors the horizon and clouds
     * actually render with are driven by season and temperature, both
     * resolved here. SeasonColorBlendBranch resolves the calendar's own
     * season keyframes into a blended tint/sunrise color; SkyColorBranch
     * combines that with time of day and live temperature into the
     * per-grid SkyColorData UBO every frame.
     */

    private ClockManager clockManager;

    private SeasonColorBlendBranch seasonColorBlendBranch;
    private SkyColorBranch skyColorBranch;

    // Internal \\

    @Override
    protected void create() {
        this.seasonColorBlendBranch = create(SeasonColorBlendBranch.class);
        this.skyColorBranch = create(SkyColorBranch.class);
    }

    @Override
    protected void get() {
        this.clockManager = get(ClockManager.class);
    }

    @Override
    protected void awake() {
        seasonColorBlendBranch.assignData(clockManager.getCalendarHandle());
        skyColorBranch.assignData(seasonColorBlendBranch);
    }

    /*
     * Re-resolves the season color keyframes against the active world's
     * calendar. Call after ClockManager.switchWorld() so a world change
     * with a different calendar doesn't leave the sky blending against
     * stale keyframes.
     */
    public void refreshCalendar() {
        seasonColorBlendBranch.assignData(clockManager.getCalendarHandle());
    }
}