package com.AdventureRPG;

import com.AdventureRPG.PlayerSystem.PlayerSystem;
import com.AdventureRPG.UISystem.UISystem;
import com.AdventureRPG.WorldSystem.WorldSystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.ModelBatch;

public class GameRenderer {

    // Game Manager
    private final GameManager gameManager; // TODO: Leaving some unused variables for now
    private final UISystem UISystem;
    private final WorldSystem worldSystem;
    private final PlayerSystem playerSystem;

    public GameRenderer(GameManager GameManager) {

        // Game Manager
        this.gameManager = GameManager;
        this.UISystem = GameManager.UISystem;
        this.worldSystem = GameManager.worldSystem;
        this.playerSystem = GameManager.playerSystem;
    }

    // TODO: This will need to be completely redone to include UI and other elements
    public void draw(SpriteBatch spriteBatch, ModelBatch modelBatch) {

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        modelBatch.begin(playerSystem.getCamera());

        worldSystem.render(modelBatch);
        playerSystem.render();

        modelBatch.end();
    }
}
