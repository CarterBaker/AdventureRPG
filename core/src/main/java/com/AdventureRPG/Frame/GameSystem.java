package com.AdventureRPG.Frame;

public abstract class GameSystem {

    // Debug \\

    private final String systemName = getClass().getSimpleName();

    protected final void debug(String input) {
        System.out.println("[" + systemName + "] " + input);
    }

    // Base \\

    protected void awake() {
    }

    protected void start() {
    }

    protected void update() {
    }

    protected void fixedUpdate() {
    }

    protected void lateUpdate() {
    }

    protected void render() {
    }

    protected void dispose() {
    }
}
