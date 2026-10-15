package application.bootstrap.entitypipeline.playermanager;

import application.bootstrap.entitypipeline.entity.EntityInputHandle;
import application.bootstrap.entitypipeline.player.PlayerGestureStruct;
import application.kernel.inputpipeline.input.RawInputHandle;
import engine.root.SystemPackage;
import engine.settings.KeyBindings;

class PlayerInputSystem extends SystemPackage {

    /*
     * Translates raw hardware state into game-semantic entity input once per
     * player per frame, before movement. The only place physical bindings map
     * to game actions; while input is locked the entity input is cleared to
     * neutral and any gesture under way is dropped. Primary attacks,
     * Secondary raises a guard, and the side each comes from is read from
     * the mouse by PlayerGestureSystem while the player is armed with a
     * weapon or bare hands. Activate opens what it faces or sets
     * down what is held. Sprint doubles as the aim: pressed while standing
     * still it raises the held item, Primary then throws it and letting
     * Sprint go lowers it, while held on the move it sprints.
     */

    // Internal
    private PlayerGestureSystem playerGestureSystem;

    // Lock
    private boolean inputLocked = false;

    // Internal \\

    @Override
    protected void get() {
        this.playerGestureSystem = get(PlayerGestureSystem.class);
    }

    // Translate \\

    void translate(RawInputHandle raw, EntityInputHandle entity, PlayerGestureStruct gesture, boolean armed) {
        if (inputLocked) {
            entity.clear();
            gesture.reset();
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
        entity.setActivateAction(raw.isBindingClicked(KeyBindings.ACTIVATE));
        entity.setAimAction(raw.isBindingClicked(KeyBindings.SPRINT) && !entity.hasHorizontalInput());
        entity.setThrowAction(raw.isBindingClicked(KeyBindings.PRIMARY));
        entity.setBlockAction(raw.isBindingHeld(KeyBindings.SECONDARY));

        playerGestureSystem.read(raw, gesture, entity, armed);
    }

    // Lock \\

    void setInputLocked(boolean locked) {
        this.inputLocked = locked;
    }
}
