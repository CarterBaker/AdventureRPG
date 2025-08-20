package com.AdventureRPG.RenderManager;

import java.util.*;

import com.AdventureRPG.GameManager;
import com.AdventureRPG.ShaderManager.ShaderManager;

public class RenderQueue {

    private final ShaderManager shaderManager;
    private final Map<Integer, Queue<RenderPass>> passes;

    public RenderQueue(GameManager gameManager) {
        this.shaderManager = gameManager.shaderManager;
        this.passes = new TreeMap<>();
    }

    // Add a pass into the queue
    public void addPass(RenderPass pass, int sortOrder) {
        passes.computeIfAbsent(sortOrder, k -> new LinkedList<>()).add(pass);
    }

    // Render in order: sorted by ID, FIFO within same ID
    public void renderAll(RenderContext context) {
        Iterator<Map.Entry<Integer, Queue<RenderPass>>> mapIter = passes.entrySet().iterator();

        while (mapIter.hasNext()) {
            Map.Entry<Integer, Queue<RenderPass>> entry = mapIter.next();
            Queue<RenderPass> queue = entry.getValue();

            queue.removeIf(pass -> {
                if (pass.lifetime > 0f) {
                    pass.lifetime -= context.deltaTime;
                    return pass.lifetime <= 0f; // remove if expired
                }
                return false; // permanent pass
            });

            // Render remaining passes
            for (RenderPass pass : queue) {
                pass.render(context, shaderManager);
            }

            if (queue.isEmpty()) {
                mapIter.remove(); // remove empty queue
            }
        }
    }

    // Clear queue after rendering (if you want one-shot passes)
    public void clear() {
        passes.clear();
    }
}
