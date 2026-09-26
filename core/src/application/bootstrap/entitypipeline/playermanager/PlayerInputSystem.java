package application.bootstrap.entitypipeline.playermanager;

import application.bootstrap.entitypipeline.entity.EntityInputHandle;
import application.kernel.inputpipeline.input.RawInputHandle;
import engine.root.SystemPackage;
import engine.settings.KeyBindings;

class PlayerInputSystem extends SystemPackage {

    /*
     * Translates raw hardware state into game-semantic entity input once per
     * player per frame, before movement. The only place physical bindings map
     * to game actions; while input is locked the entity input is cleared to
     * neutral.
     */

    // Lock
    private boolean inputLocked = false;

    // Translate \\

    void translate(RawInputHandle raw, EntityInputHandle entity) {
        if (inputLocked) {
            entity.clear();
            return;
        }

        entity.setForward(raw.isBindingHeld(KeyBindings.MOVE_FORWARD));
        entity.setBack(raw.isBindingHeld(KeyBindings.MOVE_BACK));
        entity.setLeft(raw.isBindingHeld(KeyBindings.MOVE_LEFT));
        entity.setRight(raw.isBindingHeld(KeyBindings.MOVE_RIGHT));
        entity.setJump(raw.isBindingHeld(KeyBindings.JUMP));
        entity.setWalk(raw.isBindingHeld(KeyBindings.WALK));
        entity.setSprint(raw.isBindingHeld(KeyBindings.SPRINT));
        entity.setPrimaryAction(raw.isBindingHeld(KeyBindings.PRIMARY));
        entity.setSecondaryAction(raw.isBindingHeld(KeyBindings.SECONDARY));
    }

    // Lock \\

    void setInputLocked(boolean locked) {
        this.inputLocked = locked;
    }
}