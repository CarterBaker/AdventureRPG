package com.AdventureRPG.lwjgl3;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3WindowConfiguration;
import com.internal.bootstrap.renderpipeline.window.WindowInstance;
import com.internal.core.engine.WindowPlatform;

public class Lwjgl3WindowPlatform implements WindowPlatform {

    /*
     * LWJGL3 implementation of WindowPlatform. Opens a new OS window backed
     * by the given WindowInstance as its ApplicationListener. LibGDX calls
     * render() on each window's listener every frame and makes that window's
     * GL context current before doing so — context switching is never managed
     * manually here. Instantiated in Lwjgl3Launcher and Lwjgl3LauncherEditor,
     * injected into the engine via Main and MainEditor constructors.
     */

    @Override
    public void openWindow(WindowInstance window) {

        Lwjgl3WindowConfiguration config = new Lwjgl3WindowConfiguration();
        config.setTitle(window.getTitle());
        config.setWindowedMode(window.getWidth(), window.getHeight());

        ((Lwjgl3Application) Gdx.app).newWindow(window, config);
    }
}
