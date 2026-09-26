package application.bootstrap.entitypipeline.entity;

import engine.root.HandlePackage;
import engine.util.mathematics.vectors.Vector3;

public class EntityInputHandle extends HandlePackage {

    /*
     * Game-semantic input container owned by every entity. Holds movement
     * booleans, action booleans, and facing direction. Written each frame by
     * PlayerManager for the player entity or by AI for NPCs. MovementManager
     * reads from this and never cares about the source.
     * Disconnected from RawInputHandle by design — the translation from raw
     * hardware state to game intent happens in PlayerManager. strafe holds
     * the body to the facing direction instead of letting it turn toward
     * wherever the entity is heading.
     */

    // Movement
    private boolean forward;
    private boolean back;
    private boolean left;
    private boolean right;
    private boolean jump;
    private boolean walk;
    private boolean sprint;
    private boolean strafe;

    // Actions
    private boolean primaryAction;
    private boolean secondaryAction;

    // Facing
    private Vector3 facingDirection;

    // Internal \\

    @Override
    protected void create() {
        this.facingDirection = new Vector3(0f, 0f, 1f);
    }

    // Accessible \\

    public boolean isForward() {
        return forward;
    }

    public boolean isBack() {
        return back;
    }

    public boolean isLeft() {
        return left;
    }

    public boolean isRight() {
        return right;
    }

    public boolean isJump() {
        return jump;
    }

    public boolean isWalk() {
        return walk;
    }

    public boolean isSprint() {
        return sprint;
    }

    public void setForward(boolean v) {
        forward = v;
    }

    public void setBack(boolean v) {
        back = v;
    }

    public void setLeft(boolean v) {
        left = v;
    }

    public void setRight(boolean v) {
        right = v;
    }

    public void setJump(boolean v) {
        jump = v;
    }

    public void setWalk(boolean v) {
        walk = v;
    }

    public void setSprint(boolean v) {
        sprint = v;
    }

    public boolean isStrafe() {
        return strafe;
    }

    public void setStrafe(boolean v) {
        strafe = v;
    }

    public boolean isPrimaryAction() {
        return primaryAction;
    }

    public boolean isSecondaryAction() {
        return secondaryAction;
    }

    public void setPrimaryAction(boolean v) {
        primaryAction = v;
    }

    public void setSecondaryAction(boolean v) {
        secondaryAction = v;
    }

    public Vector3 getFacingDirection() {
        return facingDirection;
    }

    public void setFacingDirection(float x, float y, float z) {
        facingDirection.set(x, y, z);
    }

    // Utility \\

    public int getHorizontalX() {
        return (right ? 1 : 0) - (left ? 1 : 0);
    }

    public int getHorizontalZ() {
        return (forward ? 1 : 0) - (back ? 1 : 0);
    }

    public int getVertical() {
        return jump ? 1 : 0;
    }

    public boolean hasHorizontalInput() {
        return forward || back || left || right;
    }

    public void clear() {
        forward = back = left = right = false;
        jump = walk = sprint = strafe = false;
        primaryAction = secondaryAction = false;
    }
}