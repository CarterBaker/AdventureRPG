package application.bootstrap.calendarpipeline.clock;

import application.bootstrap.calendarpipeline.calendar.CalendarHandle;
import engine.root.DataPackage;

public class ClockData extends DataPackage {

    /*
     * Global clock state for one world: the persisted epoch, the active
     * calendar, and every current time value derived from them each frame.
     * Location-specific time of day lives in each grid's ClockInstance.
     */

    // Epoch
    private long worldEpochStart;

    // Calendar
    private CalendarHandle calendarHandle;

    // Time State
    private double worldSecondsElapsed;
    private long totalDaysElapsed;
    private long totalDaysWithOffset;
    private double dayProgress;
    private double rawTimeOfDay;
    private double yearProgress;
    private double visualYearProgress;

    // Current Time
    private int currentMinute;
    private int currentHour;
    private int currentDayOfWeek;
    private int currentDayOfMonth;
    private int currentMonth;
    private int currentYear;
    private int currentAge;

    // Day Seed
    private long currentDaySeed;

    // Noise
    private float randomNoiseFromDay;

    // Constructor \\

    public ClockData(long worldEpochStart) {

        // Epoch
        this.worldEpochStart = worldEpochStart;
    }

    // Accessible \\

    public long getWorldEpochStart() {
        return worldEpochStart;
    }

    public void setWorldEpochStart(long worldEpochStart) {
        this.worldEpochStart = worldEpochStart;
    }

    public CalendarHandle getCalendarHandle() {
        return calendarHandle;
    }

    public void setCalendarHandle(CalendarHandle calendarHandle) {
        this.calendarHandle = calendarHandle;
    }

    public double getWorldSecondsElapsed() {
        return worldSecondsElapsed;
    }

    public void setWorldSecondsElapsed(double worldSecondsElapsed) {
        this.worldSecondsElapsed = worldSecondsElapsed;
    }

    public long getTotalDaysElapsed() {
        return totalDaysElapsed;
    }

    public void setTotalDaysElapsed(long totalDaysElapsed) {
        this.totalDaysElapsed = totalDaysElapsed;
    }

    public long getTotalDaysWithOffset() {
        return totalDaysWithOffset;
    }

    public void setTotalDaysWithOffset(long totalDaysWithOffset) {
        this.totalDaysWithOffset = totalDaysWithOffset;
    }

    public double getDayProgress() {
        return dayProgress;
    }

    public void setDayProgress(double dayProgress) {
        this.dayProgress = dayProgress;
    }

    public double getRawTimeOfDay() {
        return rawTimeOfDay;
    }

    public void setRawTimeOfDay(double rawTimeOfDay) {
        this.rawTimeOfDay = rawTimeOfDay;
    }

    public double getYearProgress() {
        return yearProgress;
    }

    public void setYearProgress(double yearProgress) {
        this.yearProgress = yearProgress;
    }

    public double getVisualYearProgress() {
        return visualYearProgress;
    }

    public void setVisualYearProgress(double visualYearProgress) {
        this.visualYearProgress = visualYearProgress;
    }

    public int getCurrentMinute() {
        return currentMinute;
    }

    public void setCurrentMinute(int currentMinute) {
        this.currentMinute = currentMinute;
    }

    public int getCurrentHour() {
        return currentHour;
    }

    public void setCurrentHour(int currentHour) {
        this.currentHour = currentHour;
    }

    public int getCurrentDayOfWeek() {
        return currentDayOfWeek;
    }

    public void setCurrentDayOfWeek(int currentDayOfWeek) {
        this.currentDayOfWeek = currentDayOfWeek;
    }

    public int getCurrentDayOfMonth() {
        return currentDayOfMonth;
    }

    public void setCurrentDayOfMonth(int currentDayOfMonth) {
        this.currentDayOfMonth = currentDayOfMonth;
    }

    public int getCurrentMonth() {
        return currentMonth;
    }

    public void setCurrentMonth(int currentMonth) {
        this.currentMonth = currentMonth;
    }

    public int getCurrentYear() {
        return currentYear;
    }

    public void setCurrentYear(int currentYear) {
        this.currentYear = currentYear;
    }

    public int getCurrentAge() {
        return currentAge;
    }

    public void setCurrentAge(int currentAge) {
        this.currentAge = currentAge;
    }

    public long getCurrentDaySeed() {
        return currentDaySeed;
    }

    public void setCurrentDaySeed(long currentDaySeed) {
        this.currentDaySeed = currentDaySeed;
    }

    public float getRandomNoiseFromDay() {
        return randomNoiseFromDay;
    }

    public void setRandomNoiseFromDay(float randomNoiseFromDay) {
        this.randomNoiseFromDay = randomNoiseFromDay;
    }
}