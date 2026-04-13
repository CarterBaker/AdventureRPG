package application.bootstrap.calendarpipeline.clock;

import application.core.engine.DataPackage;

public class ClockData extends DataPackage {

    /*
     * Mutable runtime clock state for one world. Holds the epoch anchor used
     * to derive all time values, plus all calculated current time fields updated
     * each frame by the clock branches. The epoch is the only field that needs
     * to be persisted to disk to fully restore session state.
     */

    // Epoch
    private long worldEpochStart;

    // Time State
    private long totalDaysElapsed;
    private long totalDaysWithOffset;
    private double dayProgress;
    private double visualTimeOfDay;
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

    public double getVisualTimeOfDay() {
        return visualTimeOfDay;
    }

    public void setVisualTimeOfDay(double visualTimeOfDay) {
        this.visualTimeOfDay = visualTimeOfDay;
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

    public float getRandomNoiseFromDay() {
        return randomNoiseFromDay;
    }

    public void setRandomNoiseFromDay(float randomNoiseFromDay) {
        this.randomNoiseFromDay = randomNoiseFromDay;
    }
}