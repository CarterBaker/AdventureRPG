package application.bootstrap.entitypipeline.entity;

public enum EntityAction {

    /*
     * Actions an entity performs with its hands, on top of whatever movement
     * state it is in. SWING strikes with whatever it holds, CHOP strikes the
     * same way in a level sweep from the side, as an axe is swung, THROW lets
     * the held item fly, and PLACE and PICK_UP are the short gestures of
     * setting something down and taking it up. AIM and BLOCK are stances, held for as
     * long as the entity keeps them up: AIM draws the held item back ready to
     * throw, and BLOCK raises whatever it holds as a guard. An animation tree
     * maps each by its lower-case name to the node it plays; NONE means the
     * hands are free.
     */

    NONE,
    SWING,
    CHOP,
    THROW,
    PLACE,
    PICK_UP,
    AIM,
    BLOCK;

    // Values
    public static final EntityAction[] VALUES = values();
}
