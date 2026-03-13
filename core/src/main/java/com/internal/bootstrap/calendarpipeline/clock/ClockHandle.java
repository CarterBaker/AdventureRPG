package com.internal.bootstrap.calendarpipeline.clock;

import com.internal.core.engine.HandlePackage;

public class ClockHandle extends HandlePackage {

    // Internal
    private ClockData clockData;

    // Constructor \\

    public void constructor(ClockData clockData) {

        // Internal
        this.clockData = clockData;
    }

    // Accessible \\

    public ClockData getClockData() {
        return clockData;
    }

    public long getWorldEpochStart() {
        return clockData.getWorldEpochStart();
    }

    public void setWorldEpochStart(long worldEpochStart) {
        clockData.setWorldEpochStart(worldEpochStart);
    }

    public long getTotalDaysElapsed() {
        return clockData.getTotalDaysElapsed();
    }

    public void setTotalDaysElapsed(long totalDaysElapsed) {
        clockData.setTotalDaysElapsed(totalDaysElapsed);
    }

    public long getTotalDaysWithOffset() {
        return clockData.getTotalDaysWithOffset();
    }

    public void setTotalDaysWithOffset(long totalDaysWithOffset) {
        clockData.setTotalDaysWithOffset(totalDaysWithOffset);
    }

    public double getDayProgress() {
        return clockData.getDayProgress();
    }

    public void setDayProgress(double dayProgress) {
        clockData.setDayProgress(dayProgress);
    }

    public double getVisualTimeOfDay() {
        return clockData.getVisualTimeOfDay();
    }

    public void setVisualTimeOfDay(double visualTimeOfDay) {
        clockData.setVisualTimeOfDay(visualTimeOfDay);
    }

    public double getYearProgress() {
        return clockData.getYearProgress();
    }

    public void setYearProgress(double yearProgress) {
        clockData.setYearProgress(yearProgress);
    }

    public double getVisualYearProgress() {
        return clockData.getVisualYearProgress();
    }

    public void setVisualYearProgress(double visualYearProgress) {
        clockData.setVisualYearProgress(visualYearProgress);
    }

    public int getCurrentMinute() {
        return clockData.getCurrentMinute();
    }

    public void setCurrentMinute(int currentMinute) {
        clockData.setCurrentMinute(currentMinute);
    }

    public int getCurrentHour() {
        return clockData.getCurrentHour();
    }

    public void setCurrentHour(int currentHour) {
        clockData.setCurrentHour(currentHour);
    }

    public int getCurrentDayOfWeek() {
        return clockData.getCurrentDayOfWeek();
    }

    public void setCurrentDayOfWeek(int currentDayOfWeek) {
        clockData.setCurrentDayOfWeek(currentDayOfWeek);
    }

    public int getCurrentDayOfMonth() {
        return clockData.getCurrentDayOfMonth();
    }

    public void setCurrentDayOfMonth(int currentDayOfMonth) {
        clockData.setCurrentDayOfMonth(currentDayOfMonth);
    }

    public int getCurrentMonth() {
        return clockData.getCurrentMonth();
    }

    public void setCurrentMonth(int currentMonth) {
        clockData.setCurrentMonth(currentMonth);
    }

    public int getCurrentYear() {
        return clockData.getCurrentYear();
    }

    public void setCurrentYear(int currentYear) {
        clockData.setCurrentYear(currentYear);
    }

    public int getCurrentAge() {
        return clockData.getCurrentAge();
    }

    public void setCurrentAge(int currentAge) {
        clockData.setCurrentAge(currentAge);
    }

    public float getRandomNoiseFromDay() {
        return clockData.getRandomNoiseFromDay();
    }

    public void setRandomNoiseFromDay(float randomNoiseFromDay) {
        clockData.setRandomNoiseFromDay(randomNoiseFromDay);
    }
}