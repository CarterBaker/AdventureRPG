package com.AdventureRPG.TimeSystem;

import java.util.Arrays;

public class Calendar {

    // Calendar
    private final String[] daysOfWeek;
    private final Month[] months;
    private final int[] daysPerMonth;
    private final int totalDaysInYear;

    // Base \\

    public Calendar(Month[] months, String[] daysOfWeek) {

        if (daysOfWeek == null || daysOfWeek.length == 0)
            throw new IllegalArgumentException("Calendar must have at least one day of week.");

        if (months == null || months.length == 0)
            throw new IllegalArgumentException("Calendar must have at least one month.");

        this.daysOfWeek = Arrays.copyOf(daysOfWeek, daysOfWeek.length);
        this.months = Arrays.copyOf(months, months.length);

        int total = 0;
        int[] perMonth = new int[months.length];

        for (int i = 0; i < months.length; i++) {

            if (months[i].days <= 0)
                throw new IllegalArgumentException("Month " + months[i].name + " must have > 0 days.");

            total += months[i].days;
            perMonth[i] = months[i].days;
        }

        this.totalDaysInYear = total;

        daysPerMonth = calculateDaysPerMonth();
    }

    private int[] calculateDaysPerMonth() {

        int[] ouput = new int[months.length];

        for (int i = 0; i < months.length; i++)
            ouput[i] = months[i].days;

        return ouput;
    }

    public static class Month {

        public final String name;
        public final int days;

        public Month(String name, int days) {

            if (days <= 0)
                throw new IllegalArgumentException("Month " + name + " must have > 0 days.");

            if (name == null || name.isBlank())
                throw new IllegalArgumentException("Month must have a valid name.");

            this.days = days;
            this.name = name;
        }
    }

    // Accessible \\

    public int[] getDaysPerMonth() {
        return daysPerMonth;
    }

    public int getTotalMonths() {
        return months.length; // alias
    }

    public int getTotalDaysInWeek() {
        return daysOfWeek.length;
    }

    public int getTotalMonthsInYear() {
        return months.length;
    }

    public int getTotalDaysInYear() {
        return totalDaysInYear;
    }

    public String getDayOfWeek(int input) {
        return daysOfWeek[input - 1];
    }

    public Month getMonth(int input) {
        return months[input - 1];
    }
}
