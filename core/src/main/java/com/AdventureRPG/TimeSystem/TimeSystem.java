package com.AdventureRPG.TimeSystem;

import java.io.File;
import java.time.Instant;

import com.AdventureRPG.GameManager;
import com.AdventureRPG.SaveSystem.UserData;
import com.AdventureRPG.SettingsSystem.Settings;

public class TimeSystem {

    // Game Manager
    public final Settings settings;

    // Settings
    private final int MINUTES_PER_HOUR;
    private final int HOURS_PER_DAY;
    private final int DAYS_PER_DAY;
    private final float MIDDAY_OFFSET;

    private final int STARTING_DAY;
    private final int STARTING_MONTH;
    private final int STARTING_YEAR;
    private final int STARTING_AGE;
    private final int YEARS_PER_AGE;

    // Calender
    private final File calendarFile;
    private final Calendar calendar;

    private final int totalMonths;
    private final int[] daysPerMonth;

    // Time Cache
    private double totalDaysElapsed;
    private long totalYearsElapsed;

    // Real-world time tracking
    private long lastUpdateEpochMilli;

    // Base \\

    public TimeSystem(GameManager gameManager) {

        // Game manager
        this.settings = gameManager.settings;

        // Setting
        this.MINUTES_PER_HOUR = settings.MINUTES_PER_HOUR;
        this.HOURS_PER_DAY = settings.HOURS_PER_DAY;
        this.DAYS_PER_DAY = settings.DAYS_PER_DAY;
        this.MIDDAY_OFFSET = settings.MIDDAY_OFFSET;

        this.STARTING_DAY = settings.STARTING_DAY;
        this.STARTING_MONTH = settings.STARTING_MONTH;
        this.STARTING_YEAR = settings.STARTING_YEAR;
        this.STARTING_AGE = settings.STARTING_AGE;
        this.YEARS_PER_AGE = settings.YEARS_PER_AGE;

        // Calender
        this.calendarFile = new File(settings.CALENDAR_JSON_PATH);
        calendar = Loader.load(calendarFile, gameManager.gson);

        totalMonths = calendar.getTotalMonths();
        daysPerMonth = calendar.getDaysPerMonth();
    }

    public void awake() {
        startGame();
    }

    public void start() {

    }

    public void update() {
        updateTime();
    }

    // Awake \\

    private void startGame() {

        // Base days from starting month/day
        totalDaysElapsed = 0.0;

        for (int i = 0; i < STARTING_MONTH - 1; i++)
            totalDaysElapsed += daysPerMonth[i];

        totalDaysElapsed += (STARTING_DAY - 1);

        // Add fractional day based on real-world clock
        long now = Instant.now().toEpochMilli();
        long millisInDay = now % 86400000L; // milliseconds since midnight
        double realDayFraction = millisInDay / 86400000.0;
        double gameDayFraction = realDayFraction * DAYS_PER_DAY;

        // Align noon
        totalDaysElapsed += gameDayFraction - MIDDAY_OFFSET;

        if (totalDaysElapsed < 0)
            totalDaysElapsed += totalDaysInYear();

        // Total years elapsed
        totalYearsElapsed = (long) ((STARTING_YEAR - 1) + (STARTING_AGE - 1) * YEARS_PER_AGE);

        // Save real-world timestamp for updates
        lastUpdateEpochMilli = now;
    }

    // Save System \\

    public void setTime(UserData userData) {

    }

    public void getTime(UserData userData) {

    }

    // Time \\

    private void updateTime() {

        long now = Instant.now().toEpochMilli();
        double deltaRealDays = (now - lastUpdateEpochMilli) / 86400000.0;
        lastUpdateEpochMilli = now;

        double deltaGameDays = deltaRealDays * DAYS_PER_DAY;
        advanceTime(deltaGameDays);
    }

    // Time Advancement \\

    private void advanceTime(double deltaDays) {
        totalDaysElapsed += deltaDays;

        while (totalDaysElapsed >= totalDaysInYear()) {
            totalDaysElapsed -= totalDaysInYear();
            totalYearsElapsed++;
        }
    }

    private int totalDaysInYear() {
        int sum = 0;
        for (int d : daysPerMonth)
            sum += d;
        return sum;
    }

    // Utility \\

    public double getTimeOfDay() {

        double adjustedTime = totalDaysElapsed % 1.0 + MIDDAY_OFFSET;

        if (adjustedTime >= 1.0)
            adjustedTime -= 1.0;

        return adjustedTime;
    }

    public int getMinute() {
        return (int) ((getTimeOfDay() * HOURS_PER_DAY * MINUTES_PER_HOUR) % MINUTES_PER_HOUR);
    }

    public int getHour() {
        return (int) (getTimeOfDay() * HOURS_PER_DAY);
    }

    public int getCurrentDay() {

        int remainingDays = (int) totalDaysElapsed;

        for (int i = 0; i < totalMonths; i++) {

            if (remainingDays < daysPerMonth[i])
                return remainingDays + 1;

            remainingDays -= daysPerMonth[i];
        }

        return remainingDays + 1; // fallback
    }

    public int getCurrentMonth() {

        int remainingDays = (int) totalDaysElapsed;

        for (int i = 0; i < totalMonths; i++) {

            if (remainingDays < daysPerMonth[i])
                return i + 1;

            remainingDays -= daysPerMonth[i];
        }

        return totalMonths; // fallback
    }

    public int getCurrentYear() {
        return (int) (totalYearsElapsed % YEARS_PER_AGE + 1);
    }

    public int getCurrentAge() {
        return (int) (totalYearsElapsed / YEARS_PER_AGE + 1);
    }
}