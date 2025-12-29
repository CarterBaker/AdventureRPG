package com.AdventureRPG.core.renderpipeline.rendersystem;

import com.AdventureRPG.core.engine.SystemPackage;
import com.AdventureRPG.core.engine.WindowSystem;
import com.AdventureRPG.core.geometrypipeline.Models.ModelHandle;
import com.AdventureRPG.core.geometrypipeline.modelmanager.ModelManager;
import com.AdventureRPG.core.shaderpipeline.materials.Material;
import com.AdventureRPG.core.shaderpipeline.processingpass.ProcessingPass;
import com.AdventureRPG.core.shaderpipeline.shaders.Shader;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class RenderSystem extends SystemPackage {

    // Internal
    private ModelManager modelManager;
    private WindowSystem windowSystem;

    // Render Queue: depth -> ordered list of passes
    private Int2ObjectOpenHashMap<ObjectArrayList<ProcessingPass>> depth2PassList;

    // Base \\

    @Override
    protected void create() {
        this.depth2PassList = new Int2ObjectOpenHashMap<>();
    }

    @Override
    protected void init() {
        this.modelManager = internal.get(ModelManager.class);
        this.windowSystem = internal.get(WindowSystem.class);
    }

    @Override
    protected void awake() {
        GLSLUtility.enableDepth();
    }

    // Render Management \\

    public void draw() {

        // Set viewport
        GLSLUtility.setViewport(
                windowSystem.getWidth(),
                windowSystem.getHeight());

        // Clear buffers
        GLSLUtility.clearBuffer();

        // Get all depth keys and sort them (negative to positive)
        int[] depths = depth2PassList.keySet().toIntArray();
        java.util.Arrays.sort(depths);

        // Draw passes in depth order
        for (int depth : depths) {

            if (depth == 0) {
                // Draw model batch system at depth 0
                modelManager.draw();
            } else {
                // Draw processing passes at this depth
                ObjectArrayList<ProcessingPass> passList = depth2PassList.get(depth);

                if (passList != null) {
                    for (ProcessingPass pass : passList) {
                        drawProcessingPass(pass);
                    }
                }
            }
        }

        // Handle depth 0 if it wasn't in the queue
        if (!depth2PassList.containsKey(0)) {
            modelManager.draw();
        }
    }

    private void drawProcessingPass(ProcessingPass pass) {

        ModelHandle modelHandle = pass.modelHandle;
        Material material = pass.material;
        Shader shader = material.shader;

        // Disable depth testing for full-screen passes
        GLSLUtility.disableDepth();

        // Bind shader
        GLSLUtility.useShader(shader.shaderHandle);

        // Bind VAO
        GLSLUtility.bindVAO(modelHandle.vao);

        // Draw
        GLSLUtility.drawElements(modelHandle.indexCount);

        // Unbind
        GLSLUtility.unbindVAO();

        // Re-enable depth for subsequent passes
        GLSLUtility.enableDepth();
    }

    // Accessible \\

    public void pushPass(ProcessingPass pass, int depth) {
        ObjectArrayList<ProcessingPass> passList = depth2PassList.computeIfAbsent(
                depth,
                k -> new ObjectArrayList<>());

        passList.add(pass);
    }

    public void pullPass(ProcessingPass pass, int depth) {
        ObjectArrayList<ProcessingPass> passList = depth2PassList.get(depth);

        if (passList == null)
            return;

        passList.remove(pass);

        // Clean up empty depth levels
        if (passList.isEmpty())
            depth2PassList.remove(depth);
    }

    public void pullPass(ProcessingPass pass) {
        // Search all depths for this pass
        var iterator = depth2PassList.int2ObjectEntrySet().fastIterator();

        while (iterator.hasNext()) {
            var entry = iterator.next();
            ObjectArrayList<ProcessingPass> passList = entry.getValue();

            if (passList.remove(pass)) {
                // Clean up empty depth levels
                if (passList.isEmpty())
                    iterator.remove();
                return;
            }
        }
    }
}