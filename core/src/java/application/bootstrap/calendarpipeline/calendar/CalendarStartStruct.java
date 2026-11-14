package application.bootstrap.calendarpipeline.calendar;

import engine.root.StructPackage;

public class CalendarStartStruct extends StructPackage {

    /*
     * The exact point in this calendar's own year — and its own units of
     * time — that a fresh world starts at. Everything here is calendar-
     * relative: an alien calendar with a 30-hour day or a 10-month year
     * still just plugs its own numbers in here.
     */

    // Internal
    private final int year;
    private final int age;
    private final int month;
    private final int dayOfMonth;
    private final int hour;
    private final int minute;

    // Constructor \\

    public CalendarStartStruct(int year, int age, int month, int dayOfMonth, int hour, int minute) {

        // Internal
        this.year = year;
        this.age = age;
        this.month = month;
        this.dayOfMonth = dayOfMonth;
        this.hour = hour;
        this.minute = minute;
    }

    // Accessible \\

    public int getYear() {
        return year;
    }

    public int getAge() {
        return age;
    }

    public int getMonth() {
        return month;
    }

    public int getDayOfMonth() {
        return dayOfMonth;
    }

    public int getHour() {
        return hour;
    }

    public int getMinute() {
        return minute;
    }
}