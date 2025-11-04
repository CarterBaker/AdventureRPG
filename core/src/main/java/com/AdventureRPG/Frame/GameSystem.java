package com.AdventureRPG.Frame;

public interface GameSystem {

    void awake();

    void start();

    void update();

    void fixedUpdate();

    void lateUpdate();

    void render();

    void dispose();
}
