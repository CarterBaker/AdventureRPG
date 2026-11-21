package application.bootstrap.entitypipeline.playermanager;

import application.bootstrap.combatpipeline.util.CombatDirectionUtility;
import application.bootstrap.entitypipeline.entity.CombatDirection;
import application.bootstrap.entitypipeline.entity.EntityInputHandle;
import application.kernel.inputpipeline.input.RawInputHandle;
import engine.root.SystemPackage;
import engine.settings.KeyBindings;

class PlayerGestureSystem extends SystemPackage {

    /*
     * Reads the side of the player's combat moves from its movement, once per
     * frame as PlayerInputSystem translates the rest of its input, so a stick
     * fights exactly as the keys do. CombatDirectionUtility names the side the
     * player steps toward: right and left, forward as up, back as low, and
     * standing still as straight ahead. Primary attacks toward that side at
     * once. Secondary raises the guard against the blow a foe makes stepping
     * the same way, its side the step's opposite: backing away meets an
     * overhead, stepping in a low blow, a step to either side the swing a foe
     * steps that way into, and standing still a thrust; the guard follows
     * each new step for as long as it stays up.
     */

    // Read \\

    void read(RawInputHandle raw, EntityInputHandle input) {

        CombatDirection step = CombatDirectionUtility.resolveStep(input.getHorizontalX(), input.getHorizontalZ());
        boolean attacking = raw.isBindingClicked(KeyBindings.PRIMARY);

        input.setAttackAction(attacking);

        if (attacking)
            input.setAttackDirection(step);

        if (raw.isBindingHeld(KeyBindings.SECONDARY))
            input.setGuardDirection(step.opposite());
    }
}
