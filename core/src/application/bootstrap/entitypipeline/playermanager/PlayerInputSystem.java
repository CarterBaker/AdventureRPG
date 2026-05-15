package application.bootstrap.entitypipeline.playermanager;

import application.bootstrap.entitypipeline.util.EntityInputHandle;
import application.bootstrap.inputpipeline.input.RawInputHandle;
import engine.root.SystemPackage;
import engine.settings.KeyBindings;

class PlayerInputSystem extends SystemPackage {

    /*
     * Translates raw hardware state into game-semantic entity input.
     * Called by PlayerManager once per player per frame, before movement runs.
     * The only place in the codebase that maps physical bindings to game actions.
     */

    void translate(RawInputHandle raw, EntityInputHandle entity) {
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
}