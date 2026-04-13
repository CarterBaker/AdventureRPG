package application.bootstrap.inputpipeline.input;

import engine.root.HandlePackage;
import engine.util.mathematics.vectors.Vector3;

public class InputHandle extends HandlePackage {

    /*
     * Universal input container owned by every entity. Holds movement booleans,
     * action booleans, and facing direction. Written each frame by PlayerManager
     * for the player entity or by AI for NPCs. MovementManager reads from this
     * and never cares about the source.
     */

    // Movement
    private boolean forward;
    private boolean back;
    private boolean left;
    private boolean right;
    private boolean jump;
    private boolean walk;
    private boolean sprint;

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

    public void setForward(boolean forward) {
        this.forward = forward;
    }

    public boolean isBack() {
        return back;
    }

    public void setBack(boolean back) {
        this.back = back;
    }

    public boolean isLeft() {
        return left;
    }

    public void setLeft(boolean left) {
        this.left = left;
    }

    public boolean isRight() {
        return right;
    }

    public void setRight(boolean right) {
        this.right = right;
    }

    public boolean isJump() {
        return jump;
    }

    public void setJump(boolean jump) {
        this.jump = jump;
    }

    public boolean isWalk() {
        return walk;
    }

    public void setWalk(boolean walk) {
        this.walk = walk;
    }

    public boolean isSprint() {
        return sprint;
    }

    public void setSprint(boolean sprint) {
        this.sprint = sprint;
    }

    public boolean isPrimaryAction() {
        return primaryAction;
    }

    public void setPrimaryAction(boolean primaryAction) {
        this.primaryAction = primaryAction;
    }

    public boolean isSecondaryAction() {
        return secondaryAction;
    }

    public void setSecondaryAction(boolean secondaryAction) {
        this.secondaryAction = secondaryAction;
    }

    public Vector3 getFacingDirection() {
        return facingDirection;
    }

    public void setFacingDirection(float x, float y, float z) {
        this.facingDirection.set(x, y, z);
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
}