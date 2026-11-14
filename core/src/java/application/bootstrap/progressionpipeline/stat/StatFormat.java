package application.bootstrap.progressionpipeline.stat;

public enum StatFormat {

    /*
     * How a stat's value reads on screen, named in lower case by stat data.
     * WHOLE rounds to a whole number, DECIMAL keeps two places, and PERCENT
     * shows a fraction as a percentage.
     */

    WHOLE,
    DECIMAL,
    PERCENT
}
