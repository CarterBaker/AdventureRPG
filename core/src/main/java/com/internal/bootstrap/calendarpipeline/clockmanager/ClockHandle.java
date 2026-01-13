package com.internal.bootstrap.calendarpipeline.clockmanager;

import com.internal.core.engine.HandlePackage;

public class ClockHandle extends HandlePackage {

    // Time State
    private long gameEpochStart;

    private long totalDaysElapsed;
    private long totalDaysWithOffset;

    private double dayProgress;
    private double visualDayProgress;

    private double yearProgress;
    private double visualYearProgress;

    // Current Time Values
    private int currentMinute;
    private int currentHour;
    private int currentDayOfWeek;
    private int currentDayOfMonth;
    private int currentMonth;
    private int currentYear;
    private int currentAge;

    // Random Noise
    private float randomNoiseFromDay;

    // Constructor \\

    public void constructor(
            long gameEpochStart,
            long totalDaysElapsed,
            long totalDaysWithOffset,
            double dayProgress,
            double visualDayProgress,
            double yearProgress,
            double visualYearProgress,
            int currentMinute,
            int currentHour,
            int currentDayOfWeek,
            int currentDayOfMonth,
            int currentMonth,
            int currentYear,
            int currentAge,
            float randomNoiseFromDay) {

        this.gameEpochStart = gameEpochStart;

        this.totalDaysElapsed = totalDaysElapsed;
        this.totalDaysWithOffset = totalDaysWithOffset;

        this.dayProgress = dayProgress;
        this.visualDayProgress = visualDayProgress;

        this.yearProgress = yearProgress;
        this.visualYearProgress = visualYearProgress;

        this.currentMinute = currentMinute;
        this.currentHour = currentHour;
        this.currentDayOfWeek = currentDayOfWeek;
        this.currentDayOfMonth = currentDayOfMonth;
        this.currentMonth = currentMonth;
        this.currentYear = currentYear;
        this.currentAge = currentAge;

        this.randomNoiseFromDay = randomNoiseFromDay;
    }

    // Getters \\

    public long getGameEpochStart() {
        return gameEpochStart;
    }

    public long getTotalDaysElapsed() {
        return totalDaysElapsed;
    }

    public long getTotalDaysWithOffset() {
        return totalDaysWithOffset;
    }

    public double getDayProgress() {
        return dayProgress;
    }

    public double getVisualTimeOfDay() {
        return visualDayProgress;
    }

    public double getYearProgress() {
        return yearProgress;
    }

    public double getVisualYearProgress() {
        return visualYearProgress;
    }

    public int getCurrentMinute() {
        return currentMinute;
    }

    public int getCurrentHour() {
        return currentHour;
    }

    public int getCurrentDayOfWeek() {
        return currentDayOfWeek;
    }

    public int getCurrentDayOfMonth() {
        return currentDayOfMonth;
    }

    public int getCurrentMonth() {
        return currentMonth;
    }

    public int getCurrentYear() {
        return currentYear;
    }

    public int getCurrentAge() {
        return currentAge;
    }

    public float getRandomNoiseFromDay() {
        return randomNoiseFromDay;
    }

    // Setters \\

    public void setGameEpochStart(long gameEpochStart) {
        this.gameEpochStart = gameEpochStart;
    }

    public void setTotalDaysElapsed(long totalDaysElapsed) {
        this.totalDaysElapsed = totalDaysElapsed;
    }

    public void setTotalDaysWithOffset(long totalDaysWithOffset) {
        this.totalDaysWithOffset = totalDaysWithOffset;
    }

    public void setDayProgress(double dayProgress) {
        this.dayProgress = dayProgress;
    }

    public void setVisualTimeOfDay(double visualDayProgress) {
        this.visualDayProgress = visualDayProgress;
    }

    public void setYearProgress(double yearProgress) {
        this.yearProgress = yearProgress;
    }

    public void setVisualYearProgress(double visualYearProgress) {
        this.visualYearProgress = visualYearProgress;
    }

    public void setCurrentMinute(int currentMinute) {
        this.currentMinute = currentMinute;
    }

    public void setCurrentHour(int currentHour) {
        this.currentHour = currentHour;
    }

    public void setCurrentDayOfWeek(int currentDayOfWeek) {
        this.currentDayOfWeek = currentDayOfWeek;
    }

    public void setCurrentDayOfMonth(int currentDayOfMonth) {
        this.currentDayOfMonth = currentDayOfMonth;
    }

    public void setCurrentMonth(int currentMonth) {
        this.currentMonth = currentMonth;
    }

    public void setCurrentYear(int currentYear) {
        this.currentYear = currentYear;
    }

    public void setCurrentAge(int currentAge) {
        this.currentAge = currentAge;
    }

    public void setRandomNoiseFromDay(float randomNoiseFromDay) {
        this.randomNoiseFromDay = randomNoiseFromDay;
    }
}
