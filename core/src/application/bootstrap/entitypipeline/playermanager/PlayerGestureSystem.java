package application.bootstrap.entitypipeline.playermanager;

import application.bootstrap.combatpipeline.util.CombatDirectionUtility;
import application.bootstrap.entitypipeline.entity.CombatDirection;
import application.bootstrap.entitypipeline.entity.EntityInputHandle;
import application.bootstrap.entitypipeline.player.PlayerGestureStruct;
import application.kernel.inputpipeline.input.RawInputHandle;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.settings.KeyBindings;

class PlayerGestureSystem extends SystemPackage {

    /*
     * Reads the player's combat gestures from the mouse, once per frame as
     * PlayerInputSystem translates the rest of its input. Mouse travel builds
     * a swipe that fades within a fraction of a second, so a quick flick
     * counts and the slow drift of looking around never does, and
     * CombatDirectionUtility names its side exactly as it would a stick's.
     * Armed with a weapon or bare hands, Primary attacks from the side of a
     * flick made just before it — an item that is no weapon swings plainly
     * and is never read; without
     * one the press keeps reading, the view held still, until a flick names a
     * side, the button is let go or the read runs out, and the attack comes
     * straight ahead. Raising the guard with Secondary points it toward a
     * flick made just before or to the middle, and each flick while it stays
     * up swings it to that side. After every gesture the swipe rests a
     * moment, so the hand drawing back is never read as a flick the other way.
     */

    // Read \\

    void read(RawInputHandle raw, PlayerGestureStruct gesture, EntityInputHandle input, boolean armed) {

        float deltaTime = internal.getDeltaTime();

        accumulate(raw, gesture, deltaTime);

        CombatDirection swipe = CombatDirectionUtility.resolve(
                gesture.getSwipeX(),
                gesture.getSwipeY(),
                EngineSetting.GESTURE_SWIPE_THRESHOLD);

        swipe = readAttack(raw, gesture, input, swipe, armed, deltaTime);
        readGuard(raw, gesture, input, swipe);
    }

    // The swipe fades by the frame and gathers the mouse's travel in pixels, up positive — nothing while it rests
    private void accumulate(RawInputHandle raw, PlayerGestureStruct gesture, float deltaTime) {

        if (gesture.isResting()) {
            gesture.rest(gesture.getRestRemaining() - deltaTime);
            gesture.clearSwipe();
            return;
        }

        float sensitivity = internal.settings.mouseSensitivity;
        float decay = (float) Math.exp(-deltaTime / EngineSetting.GESTURE_SWIPE_DECAY_SECONDS);

        gesture.setSwipe(
                gesture.getSwipeX() * decay + raw.getDeltaX() / sensitivity,
                gesture.getSwipeY() * decay - raw.getDeltaY() / sensitivity);
    }

    // Attack \\

    // Calls for the attack once its side is known — the swipe is spent when it names it
    private CombatDirection readAttack(
            RawInputHandle raw,
            PlayerGestureStruct gesture,
            EntityInputHandle input,
            CombatDirection swipe,
            boolean armed,
            float deltaTime) {

        input.setAttackAction(false);

        if (armed && raw.isBindingClicked(KeyBindings.PRIMARY))
            gesture.beginRead();

        if (!gesture.isReading())
            return swipe;

        gesture.advanceRead(deltaTime);

        if (swipe == CombatDirection.NEUTRAL
                && raw.isBindingHeld(KeyBindings.PRIMARY)
                && gesture.getReadElapsed() < EngineSetting.GESTURE_READ_SECONDS)
            return swipe;

        input.setAttackAction(true);
        input.setAttackDirection(swipe);
        gesture.endRead();
        spend(gesture);

        return CombatDirection.NEUTRAL;
    }

    // Guard \\

    private void readGuard(
            RawInputHandle raw,
            PlayerGestureStruct gesture,
            EntityInputHandle input,
            CombatDirection swipe) {

        if (!raw.isBindingHeld(KeyBindings.SECONDARY))
            return;

        if (!raw.isBindingClicked(KeyBindings.SECONDARY) && swipe == CombatDirection.NEUTRAL)
            return;

        input.setGuardDirection(swipe);

        if (swipe != CombatDirection.NEUTRAL)
            spend(gesture);
    }

    // Utility \\

    private void spend(PlayerGestureStruct gesture) {
        gesture.clearSwipe();
        gesture.rest(EngineSetting.GESTURE_REST_SECONDS);
    }
}
