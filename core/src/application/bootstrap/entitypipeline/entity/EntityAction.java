package application.bootstrap.entitypipeline.entity;

public enum EntityAction {

    /*
     * Actions an entity performs with its hands, on top of whatever movement
     * state it is in. THROW lets the held item fly, and PLACE and PICK_UP are
     * the short gestures of setting something down and taking it up. ATTACK is
     * a weapon's move from one CombatDirection, played with the clip its
     * weapon has for the entity's rig, whatever is held — a sword, a pickaxe
     * or bare hands all strike this way; RECOIL is the jolt of an attack
     * turned aside by a guard, and STAGGER the flinch of being struck while
     * winding up. AIM and BLOCK are stances, held for as long as the entity
     * keeps them up: AIM draws the held item back ready to throw, and BLOCK
     * raises a guard toward one CombatDirection. An animation tree maps each
     * by its lower-case name to the node it plays; NONE means the hands are
     * free.
     */

    NONE,
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
