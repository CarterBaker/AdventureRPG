package com.AdventureRPG.Core.RenderPipeline.RenderManager;

import com.AdventureRPG.Core.Bootstrap.SystemFrame;
import com.AdventureRPG.Core.RenderPipeline.Util.UKeyGenerator;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

public class UHandleSystem extends SystemFrame {

    private final IntSet keys = new IntOpenHashSet();
    private int totalKeys = 0;

    public int createKey() {

        int key;

        do
            key = UKeyGenerator.generate();

        while (keys.contains(key));

        keys.add(key);
        totalKeys++;

        return key;
    }

    public void removeKey(int key) {
        keys.remove(key);
    }

    public boolean exists(int key) {
        return keys.contains(key);
    }

    public int getTotalKeys() {
        return totalKeys;
    }
}
