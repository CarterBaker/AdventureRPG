package application.bootstrap.entitypipeline.player;

import engine.root.StructPackage;

public class PlayerGestureStruct extends StructPackage {

    /*
     * Per-window state of the combat gesture PlayerGestureSystem reads from a
     * player's mouse: the fading swipe in mouse pixels, x to the right and y
     * up; whether an attack press is still waiting on a swipe to name its
     * side, and for how long it has waited; and how long the swipe still
     * rests after the last gesture it named. Owned by PlayerManager for as
     * long as its window's player lives.
     */

    // Swipe
    private float swipeX;
    private float swipeY;

    // Read
    private boolean reading;
    private float readElapsed;

    // Rest
    private float restRemaining;

    // Management \\

    public void setSwipe(float swipeX, float swipeY) {
        this.swipeX = swipeX;
        this.swipeY = swipeY;
    }

    public void clearSwipe() {
        swipeX = swipeY = 0f;
    }

    public void beginRead() {
        reading = true;
        readElapsed = 0f;
    }

    public void advanceRead(float deltaTime) {
        readElapsed += deltaTime;
    }

    public void endRead() {
        reading = false;
    }

    public void rest(float seconds) {
        restRemaining = seconds;
    }

    public void reset() {
        clearSwipe();
        endRead();
        restRemaining = 0f;
    }

    // Accessible \\

    public float getSwipeX() {
        return swipeX;
    }

    public float getSwipeY() {
        return swipeY;
    }

    public boolean isReading() {
        return reading;
    }

    public float getReadElapsed() {
        return readElapsed;
    }

    public boolean isResting() {
        return restRemaining > 0f;
    }

    public float getRestRemaining() {
        return restRemaining;
    }
}
