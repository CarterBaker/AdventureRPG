package com.AdventureRPG;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import com.AdventureRPG.UISystem.UISystem;
import com.AdventureRPG.WorldSystem.WorldSystem;

public class GameRenderer {

    // Main
    public GameManager GameManager;

    // Systems
    public UISystem UISystem;
    public WorldSystem WorldSystem;

    public GameRenderer(GameManager GameManager) {

        // Main
        this.GameManager = GameManager;

        // Systems
        this.UISystem = GameManager.UISystem;
        this.WorldSystem = GameManager.WorldSystem;
    }

    public void Draw(SpriteBatch SpriteBatch) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        SpriteBatch.begin();

        Render();

        SpriteBatch.end();
    }

    private void Render() {
        UISystem.Render();
        WorldSystem.Render();
    }
}
