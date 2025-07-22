package com.AdventureRPG.WorldSystem.Grid;

import com.badlogic.gdx.math.Vector3;

import com.AdventureRPG.Util.*;
import com.AdventureRPG.WorldSystem.WorldSystem;

public class Grid {

    private Vector3 position = new Vector3();
    private Vector3Int currentChunk = new Vector3Int();

    private WorldSystem WorldSystem;

    public Grid(WorldSystem WorldSystem) {
        this.WorldSystem = WorldSystem;
    }

    public void SetPosition(Vector3 input) {
        position = WorldSystem.WrapAroundChunk(input);
        currentChunk = WorldSystem.WrapChunksAroundWorld(input);
    }

}
