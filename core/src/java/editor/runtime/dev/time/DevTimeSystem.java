package editor.runtime.dev.time;

import application.bootstrap.calendarpipeline.calendar.CalendarHandle;
import application.bootstrap.calendarpipeline.clock.ClockHandle;
import application.bootstrap.calendarpipeline.clockmanager.ClockManager;
import application.kernel.windowpipeline.window.WindowInstance;
import editor.runtime.EditorSetting;
import engine.root.SystemPackage;

public class DevTimeSystem extends SystemPackage {

    /*
     * Moves the world's clock for testing. Driven by the command console's
     * time command, which advances one unit — the hour, minute, weekday, day,
     * month, season, year, age or moon — by a whole amount, forward or back,
     * keeping every smaller unit and carrying into every larger one, and by
     * its dawn, noon, dusk and midnight commands, which skip ahead to the next
     * time the sun stands there. The clock is shared by every window, so these
     * commands run in one Dev window only, which reports where it now stands.
     */

    // Internal
    private ClockManager clockManager;

    // Base \\

    @Override
    protected void get() {
        this.clockManager = get(ClockManager.class);
    }

    // Management \\

    public void advanceTime(String unitName, String amountText) {

        WindowInstance window = context.getWindow();
        ClockHandle clock = clockManager.getClockHandle();
        CalendarHandle calendar = clockManager.getCalendarHandle();
        DevTimeUnit unit = DevTimeUnit.fromUnitName(unitName);

        if (unit == null) {
            errorLog(window.getTitle() + EditorSetting.COMMAND_MESSAGE_TIME_UNKNOWN_UNIT + unitName
                    + EditorSetting.COMMAND_MESSAGE_TIME_UNIT_HINT + DevTimeUtility.listUnitNames());
            return;
        }

        if (!DevTimeUtility.isAvailable(unit, calendar)) {
            errorLog(window.getTitle() + EditorSetting.COMMAND_MESSAGE_TIME_UNAVAILABLE + unit.getUnitName());
            return;
        }

        int amount;

        try {
            amount = Integer.parseInt(amountText);
        } catch (NumberFormatException e) {
            errorLog(window.getTitle() + EditorSetting.COMMAND_MESSAGE_TIME_INVALID + amountText);
            return;
        }

        if (amount == 0)
            return;

        moveClock(
                DevTimeUtility.resolveTargetDay(unit, amount, clock, calendar),
                DevTimeUtility.resolveTargetMinuteOfDay(unit, amount, clock, calendar));
    }

    // Skips ahead to the next minute at which the sun stands at this raw time of day
    public void skipToTimeOfDay(double rawTimeOfDay) {

        ClockHandle clock = clockManager.getClockHandle();
        CalendarHandle calendar = clockManager.getCalendarHandle();
        int minuteOfDay = DevTimeUtility.resolveMinuteOfDayAtRawTime(rawTimeOfDay, calendar);
        int currentMinuteOfDay = DevTimeUtility.getMinuteOfDay(clock, calendar);

        if (minuteOfDay == currentMinuteOfDay)
            return;

        moveClock(
                clock.getTotalDaysWithOffset() + (minuteOfDay < currentMinuteOfDay ? 1 : 0),
                minuteOfDay);
    }

    private void moveClock(long totalDaysWithOffset, int minuteOfDay) {

        WindowInstance window = context.getWindow();
        CalendarHandle calendar = clockManager.getCalendarHandle();

        if (totalDaysWithOffset < 0) {
            errorLog(window.getTitle() + EditorSetting.COMMAND_MESSAGE_TIME_BEFORE_CALENDAR);
            return;
        }

        if (totalDaysWithOffset > DevTimeUtility.getLastDay(calendar)) {
            errorLog(window.getTitle() + EditorSetting.COMMAND_MESSAGE_TIME_AFTER_CALENDAR
                    + DevTimeUtility.getMaximum(DevTimeUnit.AGE, clockManager.getClockHandle(), calendar));
            return;
        }

        clockManager.moveClock(totalDaysWithOffset, minuteOfDay);
        log(window.getTitle() + EditorSetting.COMMAND_MESSAGE_TIME_MOVED
                + DevTimeUtility.describeClock(clockManager.getClockHandle(), calendar));
    }
}
