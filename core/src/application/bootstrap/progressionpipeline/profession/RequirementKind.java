package application.bootstrap.progressionpipeline.profession;

public enum RequirementKind {

    /*
     * What a profession requirement reads, named in lower case by profession
     * data. LEVEL, MORALITY, TRADE and SKILL compare a value with the
     * requirement's min and max; DEED compares how many times a counted deed
     * was done; PROFESSION asks that another profession be unlocked; GRANTED
     * asks that the profession itself was granted by an event in the world.
     */

    LEVEL,
    MORALITY,
    TRADE,
    SKILL,
    DEED,
    PROFESSION,
    GRANTED
}
