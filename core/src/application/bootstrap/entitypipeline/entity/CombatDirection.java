package application.bootstrap.entitypipeline.entity;

public enum CombatDirection {

    /*
     * The side an attack comes from or a guard is held toward, always in the
     * frame of the entity doing it: LEFT is its own left. NEUTRAL is straight
     * ahead — the thrust, and the middle guard that meets it. Two entities
     * face each other, so a blow from the attacker's left arrives on the
     * defender's right; mirror() turns one into the other, and a guard
     * catches a blow only when it is held toward the attack's mirror. A
     * moveset names each by its lower-case name.
     */

    NEUTRAL,
    UP,
    DOWN,
    LEFT,
    RIGHT;

    // Values
    public static final CombatDirection[] VALUES = values();

    // Accessible \\

    // The same side seen by whoever faces this one
    public CombatDirection mirror() {
        return switch (this) {
            case LEFT -> RIGHT;
            case RIGHT -> LEFT;
            default -> this;
        };
    }
}
