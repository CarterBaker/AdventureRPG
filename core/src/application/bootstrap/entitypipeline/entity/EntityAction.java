package application.bootstrap.entitypipeline.entity;

public enum EntityAction {

    /*
     * Actions an entity performs with its hands, on top of whatever movement
     * state it is in. SWING strikes with whatever it holds, CHOP strikes the
     * same way in a level sweep from the side, as an axe is swung, THROW lets
     * the held item fly, and PLACE and PICK_UP are the short gestures of
     * setting something down and taking it up. ATTACK is a weapon's move from
     * one CombatDirection, played from its moveset; RECOIL is the jolt of an
     * attack turned aside by a guard, and STAGGER the flinch of being struck
     * while winding up. AIM and BLOCK are stances, held for as long as the
     * entity keeps them up: AIM draws the held item back ready to throw, and
     * BLOCK raises a guard toward one CombatDirection. An animation tree maps
     * each by its lower-case name to the node it plays; NONE means the hands
     * are free.
     */

    NONE,
    SWING,
    CHOP,
    THROW,
    PLACE,
    PICK_UP,
    ATTACK,
    RECOIL,
    STAGGER,
    AIM,
    BLOCK;

    // Values
    public static final EntityAction[] VALUES = values();
}
