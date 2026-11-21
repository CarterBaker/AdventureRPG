package application.bootstrap.combatpipeline.util;

import application.bootstrap.entitypipeline.entity.CombatDirection;
import engine.root.EngineSetting;
import engine.root.EngineUtility;

public class CombatDirectionUtility extends EngineUtility {

    /*
     * The one place a two-axis input — a step on the keys or an analog stick,
     * x to the right and y forward or up — becomes the CombatDirection it
     * points toward. Anything inside the deadzone is NEUTRAL; past it,
     * whichever axis leans further names the side, a diagonal step its
     * sideways half.
     */

    // Resolve \\

    public static CombatDirection resolve(float x, float y, float deadzone) {

        if (x * x + y * y < deadzone * deadzone)
            return CombatDirection.NEUTRAL;

        if (Math.abs(x) >= Math.abs(y))
            return x > 0f ? CombatDirection.RIGHT : CombatDirection.LEFT;

        return y > 0f ? CombatDirection.UP : CombatDirection.DOWN;
    }

    // The side a step toward points — right and left, forward as up and back as low, none as straight ahead
    public static CombatDirection resolveStep(int horizontalX, int horizontalZ) {
        return resolve(horizontalX, horizontalZ, EngineSetting.COMBAT_STEP_DEADZONE);
    }
}
