package com.AdventureRPG.TimeSystem;

import java.io.File;
import java.time.Instant;

import com.AdventureRPG.GameManager;
import com.AdventureRPG.LightingSystem.LightingSystem;
import com.AdventureRPG.SaveSystem.UserData;
import com.AdventureRPG.SettingsSystem.Settings;

public class TimeSystem {

    // Debug
    private boolean debug = false; // TODO: Debug line

    // Game Manager
    public final Settings settings;
    public final GameManager gameManager;
    private LightingSystem lightingSystem;

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

    // Calendar
    private final File calendarFile;
    private final Calendar calendar;

    private final int totalMonths;
    private final int[] daysPerMonth;
    private final int totalDaysInYear; // linear fraction of current day

    // Time State
    private long totalDaysElapsed; // total whole days elapsed
    private double dayProgress;

    private double visualTimeOfDay; // 0..1, warped for visuals (sun, shaders)

    private int currentMinute;
    private int currentHour;
    private int currentDayOfWeek;
    private int currentDayOfMonth;
    private int currentMonth;
    private int currentYear;
    private int currentAge;

    // Time Tracking
    private long lastMinute;
    private long lastHour;
    private long lastDay;
    private long lastMonth;
    private long lastYear;

    // Constructor
    public TimeSystem(GameManager gameManager) {

        // Game manager
        this.settings = gameManager.settings;
        this.gameManager = gameManager;

        // Settings
        this.MINUTES_PER_HOUR = settings.MINUTES_PER_HOUR;
        this.HOURS_PER_DAY = settings.HOURS_PER_DAY;
        this.DAYS_PER_DAY = settings.DAYS_PER_DAY;
        this.MIDDAY_OFFSET = settings.MIDDAY_OFFSET;

        this.STARTING_DAY = settings.STARTING_DAY;
        this.STARTING_MONTH = settings.STARTING_MONTH;
        this.STARTING_YEAR = settings.STARTING_YEAR;
        this.STARTING_AGE = settings.STARTING_AGE;
        this.YEARS_PER_AGE = settings.YEARS_PER_AGE;

        // Calendar
        this.calendarFile = new File(settings.CALENDAR_JSON_PATH);
        this.calendar = Loader.load(calendarFile, gameManager.gson);

        this.totalMonths = calendar.getTotalMonths();
        this.daysPerMonth = calendar.getDaysPerMonth();
        this.totalDaysInYear = calendar.getTotalDaysInYear();

        // Time Tracking
        this.lastMinute = -1;
        this.lastHour = -1;
        this.lastDay = -1;
        this.lastMonth = -1;
        this.lastYear = -1;
    }

    public void awake() {
        this.lightingSystem = gameManager.lightingSystem;
    }

    public void start() {

    }

    public void update() {
        updateFromSystemClock();
    }

    public void dispose() {

    }

    // Save System \\

    public void setTime(UserData userData) {
        // to implement later
    }

    public void getTime(UserData userData) {
        // to implement later
    }

    // Time \\

    private void updateFromSystemClock() {

        // 1. Current system millis
        long now = Instant.now().toEpochMilli();

        // 2. Real-world days elapsed since midnight UTC
        double realDaysElapsed = (now % 86400000L) / 86400000.0;

        // 3. Scale into game time
        double gameDaysElapsed = realDaysElapsed * DAYS_PER_DAY;

        // 4. Extract the fractional part for today
        dayProgress = gameDaysElapsed % 1.0;

        // 5. Offset to align noon
        double rawTimeOfDay = (dayProgress + MIDDAY_OFFSET) % 1.0;
        if (rawTimeOfDay < 0)
            rawTimeOfDay += 1.0;

        // 6. Visual bending
        visualTimeOfDay = bendTimeOfDay(rawTimeOfDay, getDayOfYear());

        // 7. Update calendar
        updateMinutes(rawTimeOfDay);

        if (debug)
            logTimeState();
    }

    // Seasonal bending \\

    private double bendTimeOfDay(double raw, int dayOfYear) {

        double yearProgress = (double) dayOfYear / totalDaysInYear;

        // sine wave → summer = +1, winter = -1
        double seasonEffect = Math.sin(yearProgress * 2 * Math.PI);

        // max effect strength (fraction of bending)
        double amplitude = 0.25; // later can move to Settings

        // warp curve around noon/midnight
        double warped = raw + seasonEffect * amplitude * Math.sin(raw * Math.PI * 2);

        // normalize
        if (warped < 0)
            warped += 1.0;

        if (warped >= 1)
            warped -= 1.0;

        return warped;
    }

    private int getDayOfYear() {

        int sum = 0;

        for (int i = 0; i < currentMonth - 1; i++)
            sum += daysPerMonth[i];

        return sum + (currentDayOfMonth - 1);
    }

    // Minutes \\

    private void updateMinutes(double rawTimeOfDay) {

        currentMinute = calculateMinutes(rawTimeOfDay);

        if (lastMinute == currentMinute)
            return;

        lastMinute = currentMinute;

        updateHours(rawTimeOfDay);
    }

    private int calculateMinutes(double rawTimeOfDay) {
        return (int) ((rawTimeOfDay * HOURS_PER_DAY * MINUTES_PER_HOUR) % MINUTES_PER_HOUR);
    }

    // Hours \\

    private void updateHours(double rawTimeOfDay) {

        currentHour = calculateHours(rawTimeOfDay);

        if (lastHour == currentHour)
            return;

        lastHour = currentHour;

        handleDays(rawTimeOfDay);
    }

    private int calculateHours(double rawTimeOfDay) {
        return currentHour = (int) (rawTimeOfDay * HOURS_PER_DAY);
    }

    // Days \\

    private void handleDays(double rawTimeOfDay) {

        if (lastDay == totalDaysElapsed)
            return;

        lastDay = totalDaysElapsed;

        long totalDaysWithOffset = calculateTotalDaysWithOffset();
        long dayOfAge = totalDaysWithOffset % (YEARS_PER_AGE * totalDaysInYear);

        lightingSystem.sky.generateRandomOffsetFromDay(totalDaysWithOffset);

        calculateDayOfWeek(totalDaysWithOffset);
        calculateDayOfMonth(totalDaysWithOffset, dayOfAge);
    }

    private long calculateTotalDaysWithOffset() {

        long dayOfAgeOffset = STARTING_YEAR * totalDaysInYear + getDayOfYearFromStart();

        return totalDaysElapsed + dayOfAgeOffset;
    }

    private int getDayOfYearFromStart() {

        int dayOfYear = 0;

        for (int i = 0; i < STARTING_MONTH - 1; i++)
            dayOfYear += daysPerMonth[i];

        dayOfYear += STARTING_DAY - 1;

        return dayOfYear;
    }

    private void calculateDayOfWeek(long totalDaysWithOffset) {
        currentDayOfWeek = (int) ((totalDaysWithOffset % calendar.getTotalDaysInWeek()) + 1);
    }

    private void calculateDayOfMonth(long totalDaysWithOffset, long dayOfAge) {

        long dayOfYear = dayOfAge % totalDaysInYear;

        int dayOfMonth = (int) dayOfYear + 1;
        int month = 0;

        for (int i = 0; i < totalMonths; i++) {

            if (dayOfMonth <= daysPerMonth[i]) {

                month = i + 1;
                break;
            }

            dayOfMonth -= daysPerMonth[i];
        }

        currentDayOfMonth = dayOfMonth;
        currentMonth = month;

        handleYear(totalDaysWithOffset, dayOfAge);
    }

    // Years \\

    private void handleYear(long totalDaysWithOffset, long dayOfAge) {

        if (lastMonth == currentMonth)
            return;

        lastMonth = currentMonth;

        calculateYear(dayOfAge);

        handleAge(totalDaysWithOffset);
    }

    private void calculateYear(long dayOfAge) {
        currentYear = (int) (dayOfAge / totalDaysInYear) + STARTING_YEAR;
    }

    // Ages \\

    private void handleAge(long totalDaysWithOffset) {

        if (lastYear == currentYear)
            return;

        lastYear = currentYear;

        calculateAge(totalDaysWithOffset);

    }

    private void calculateAge(long totalDaysWithOffset) {
        currentAge = (int) (totalDaysWithOffset / (YEARS_PER_AGE * totalDaysInYear)) + STARTING_AGE;
    }

    // Utility \\

    // Getters \\

    public double getTimeOfDay() {
        return visualTimeOfDay; // bent value for shaders
    }

    public double getRawTimeOfDay() {
        return dayProgress; // linear internal clock
    }

    public int getMinute() {
        return currentMinute;
    }

    public int getHour() {
        return currentHour;
    }

    public String getCurrentDayOfWeek() {
        return calendar.getDayOfWeek(currentDayOfWeek);
    }

    public String getCurrentMonth() {
        return calendar.getMonth(currentMonth).name;
    }

    public int getCurrentDayOfMonth() {
        return currentDayOfMonth;
    }

    public int getCurrentYear() {
        return currentYear;
    }

    public int getCurrentAge() {
        return currentAge;
    }

    // Debug \\

    // Debug log function
    private void log(String msg) { // TODO: Debug line
        if (debug) {
            System.out.println("[TimeSystem] " + msg);
        }
    }

    // Call this method to output the full current time state
    public void logTimeState() {
        log("Time State -> " +
                "Minute: " + currentMinute + ", " +
                "Hour: " + currentHour + ", " +
                "Day: " + currentDayOfMonth + ", " +
                "Month: " + currentMonth + " (" + getCurrentMonth() + "), " +
                "Year: " + currentYear + ", " +
                "Age: " + currentAge + ", " +
                "Day of Week: " + getCurrentDayOfWeek() + ", " +
                "Day Progress: " + String.format("%.4f", dayProgress) + ", " +
                "Visual Time of Day: " + String.format("%.4f", visualTimeOfDay));
    }
}
