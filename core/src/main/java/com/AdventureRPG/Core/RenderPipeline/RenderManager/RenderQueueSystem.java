package com.AdventureRPG.Core.RenderPipeline.RenderManager;

import java.util.*;

import com.AdventureRPG.Core.Bootstrap.SystemFrame;
import com.AdventureRPG.Core.RenderPipeline.PassSystem.PassData;
import com.AdventureRPG.Core.RenderPipeline.ShaderManager.ShaderManager;

public class RenderQueueSystem extends SystemFrame {

    private ShaderManager shaderManager;
    private Map<Integer, Queue<PassData>> passes;

    @Override
    protected void init() {

        this.shaderManager = engineManager.get(ShaderManager.class);
        this.passes = new TreeMap<>();
    }

    // Add a pass into the queue
    public void addPass(PassData pass, int sortOrder) {
        passes.computeIfAbsent(sortOrder, k -> new LinkedList<>()).add(pass);
    }

    public void draw(RenderContext context) {

        Iterator<Map.Entry<Integer, Queue<PassData>>> mapIter = passes.entrySet().iterator();

        while (mapIter.hasNext()) {

            Map.Entry<Integer, Queue<PassData>> entry = mapIter.next();
            Queue<PassData> queue = entry.getValue();

            queue.removeIf(pass -> {

                if (pass.lifetime > 0f) {

                    pass.lifetime -= context.deltaTime();
                    return pass.lifetime <= 0f;
                }

                return false;
            });

            // Render remaining passes
            for (PassData pass : queue)
                pass.draw(context, shaderManager);

            if (queue.isEmpty())
                mapIter.remove(); // remove empty queue
        }
    }
}
