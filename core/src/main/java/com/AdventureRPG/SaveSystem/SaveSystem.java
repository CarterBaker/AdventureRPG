package com.AdventureRPG.SaveSystem;

import com.AdventureRPG.GameManager;

public class SaveSystem {

    // Game
    public final GameManager GameManager;

    //Save System
    public final ChunkData ChunkData;

    public SaveSystem(GameManager GameManager) {

        // Game
        this.GameManager = GameManager;

        //Save System
        this.ChunkData = new ChunkData(this);
    }
}
