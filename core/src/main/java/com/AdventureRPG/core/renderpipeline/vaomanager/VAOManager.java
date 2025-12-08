package com.AdventureRPG.core.renderpipeline.vaomanager;

import com.AdventureRPG.core.kernel.ManagerFrame;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL30;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;

// TODO: Eventually I want to load VAO from json files on start
public class VAOManager extends ManagerFrame {

    // Retrieval Mapping
    private Object2IntOpenHashMap<String> vaoName2VAOID;
    private Int2IntOpenHashMap vaoID2VAOHandle;

    // Base \\

    @Override
    protected void create() {

        this.vaoName2VAOID = new Object2IntOpenHashMap<>();
        this.vaoID2VAOHandle = new Int2IntOpenHashMap();

        loadStaticVAO();
    }

    @Override
    protected void dispose() {

        for (int vaoID : vaoID2VAOHandle.keySet()) {
            int handle = vaoID2VAOHandle.get(vaoID);
            Gdx.gl30.glDeleteVertexArrays(1, new int[] { handle }, 0);
        }

        vaoName2VAOID.clear();
        vaoID2VAOHandle.clear();
    }

    // VAO Management \\

    private void loadStaticVAO() {

        int[] vaoArr = new int[1];
        Gdx.gl30.glGenVertexArrays(1, vaoArr, 0);
        int vaoHandle = vaoArr[0];

        Gdx.gl30.glBindVertexArray(vaoHandle);

        int strideBytes = 9 * Float.BYTES;

        // Position XYZ -> location 0
        Gdx.gl30.glVertexAttribPointer(0, 3, GL30.GL_FLOAT, false, strideBytes, 0);
        Gdx.gl30.glEnableVertexAttribArray(0);

        // Normal XYZ -> location 1
        Gdx.gl30.glVertexAttribPointer(1, 3, GL30.GL_FLOAT, false, strideBytes, (int) (3L * Float.BYTES));
        Gdx.gl30.glEnableVertexAttribArray(1);

        // Color RGB -> location 2
        Gdx.gl30.glVertexAttribPointer(2, 3, GL30.GL_FLOAT, false, strideBytes, (int) (6L * Float.BYTES));
        Gdx.gl30.glEnableVertexAttribArray(2);

        // UV -> location 3
        Gdx.gl30.glVertexAttribPointer(3, 2, GL30.GL_FLOAT, false, strideBytes, (int) (9L * Float.BYTES));
        Gdx.gl30.glEnableVertexAttribArray(3);

        Gdx.gl30.glBindVertexArray(0);

        int vaoID = 0;
        vaoName2VAOID.put("static", vaoID);
        vaoID2VAOHandle.put(vaoID, vaoHandle);
    }

    // Accessible \\

    public int getVAOIDFromName(String vaoName) {
        return vaoName2VAOID.getInt(vaoName);
    }

    public int getVAOHandleFromID(int vaoID) {
        return vaoID2VAOHandle.get(vaoID);
    }
}
