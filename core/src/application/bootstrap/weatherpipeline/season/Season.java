package application.bootstrap.weatherpipeline.season;

public enum Season {

    /*
     * Discrete season bucket derived from the continuous yearProgress value
     * tracked on ClockHandle. Boundaries are quarters of the year, matching
     * the seasonal tint peaks already used by SkyColorBranch.
     */

    SPRING,
    SUMMER,
    FALL,
    WINTER;

    // Resolution \\

    public static Season fromYearProgress(double yearProgress) {

        if (yearProgress < 0.25)
            return SPRING;

        if (yearProgress < 0.50)
            return SUMMER;

        if (yearProgress < 0.75)
            return FALL;

        return WINTER;
    }
}