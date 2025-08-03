package com.AdventureRPG;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.AdventureRPG.UISystem.UISystem;
import com.AdventureRPG.WorldSystem.WorldSystem;
import com.AdventureRPG.PlayerSystem.Player;

public class GameUpdate {

    // Main
    public final GameManager GameManager;

    // Systems
    public final UISystem UISystem;
    public final WorldSystem WorldSystem;
    public final Player Player;

    public GameUpdate(GameManager GameManager) {

        // Main
        this.GameManager = GameManager;

        // Systems
        this.UISystem = GameManager.UISystem;
        this.WorldSystem = GameManager.WorldSystem;
        this.Player = GameManager.Player;
    }

    public void Draw(SpriteBatch spriteBatch, ModelBatch modelBatch) {

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        WorldSystem.Update();
        Player.Update();

        modelBatch.begin(Player.camera.getCamera());

        WorldSystem.Render(modelBatch);
        Player.Render();

        modelBatch.end();

        spriteBatch.begin();

        Update();

        spriteBatch.end();
    }

    private void Update() {
        UISystem.Update();
    }
}
