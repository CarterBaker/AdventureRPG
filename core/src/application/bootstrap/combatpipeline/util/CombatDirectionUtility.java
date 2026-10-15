package application.bootstrap.combatpipeline.util;

import application.bootstrap.entitypipeline.entity.CombatDirection;
import engine.root.EngineUtility;

public class CombatDirectionUtility extends EngineUtility {

    /*
     * The one place a two-axis input — a mouse swipe or an analog stick, x to
     * the right and y up — becomes the CombatDirection it points toward.
     * Anything inside the deadzone is NEUTRAL; past it, whichever axis leans
     * further names the side.
     */

    // Resolve \\

    public static CombatDirection resolve(float x, float y, float deadzone) {

        if (x * x + y * y < deadzone * deadzone)
            return CombatDirection.NEUTRAL;

        if (Math.abs(x) >= Math.abs(y))
            return x > 0f ? CombatDirection.RIGHT : CombatDirection.LEFT;

        return y > 0f ? CombatDirection.UP : CombatDirection.DOWN;
    }
}
