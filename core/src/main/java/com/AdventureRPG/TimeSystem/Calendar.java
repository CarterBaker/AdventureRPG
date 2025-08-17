package com.AdventureRPG.TimeSystem;

public class Calendar {

    public Month[] months;

    public static class Month {
        public String name;
        public int days;
    }

    public int getTotalMonths() {
        return months.length;
    }

    public int getTotalDay() {

        int totalDays = 0;

        for (Month month : months)
            totalDays += month.days;

        return totalDays;
    }

    public int[] getDaysPerMonth() {

        int[] daysPerMonth = new int[months.length];

        for (int month = 0; month < months.length; month++)
            daysPerMonth[month] = months[month].days;

        return daysPerMonth;
    }
}
