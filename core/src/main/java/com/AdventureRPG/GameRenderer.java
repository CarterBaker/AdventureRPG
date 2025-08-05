package com.AdventureRPG;

import com.AdventureRPG.PlayerSystem.PlayerSystem;
import com.AdventureRPG.UISystem.UISystem;
import com.AdventureRPG.WorldSystem.WorldSystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.ModelBatch;

public class GameRenderer {

    // Main
    public final GameManager GameManager;

    // Systems
    public final UISystem UISystem;
    public final PlayerSystem PlayerSystem;
    public final WorldSystem WorldSystem;

    public GameRenderer(GameManager GameManager) {

        // Main
        this.GameManager = GameManager;

        // Systems
        this.UISystem = GameManager.UISystem;
        this.PlayerSystem = GameManager.PlayerSystem;
        this.WorldSystem = GameManager.WorldSystem;
    }

    public void Draw(SpriteBatch spriteBatch, ModelBatch modelBatch) {

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        modelBatch.begin(PlayerSystem.camera.getCamera());

        WorldSystem.Render(modelBatch);
        PlayerSystem.Render();

        modelBatch.end();
    }
}
