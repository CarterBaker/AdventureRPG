package com.AdventureRPG.core.renderpipeline.rendersystem;

import com.AdventureRPG.core.engine.SystemPackage;
import com.AdventureRPG.core.engine.WindowInstance;
import com.AdventureRPG.core.geometrypipeline.Models.ModelHandle;
import com.AdventureRPG.core.geometrypipeline.modelmanager.ModelManager;
import com.AdventureRPG.core.shaderpipeline.materials.Material;
import com.AdventureRPG.core.shaderpipeline.processingpass.ProcessingPassHandle;
import com.AdventureRPG.core.shaderpipeline.shaders.Shader;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class RenderSystem extends SystemPackage {

    // Internal
    private ModelManager modelManager;
    private WindowInstance windowInstance;

    // Render Queue: depth -> ordered list of passes
    private Int2ObjectOpenHashMap<ObjectArrayList<ProcessingPassHandle>> depth2PassList;

    // Base \\

    @Override
    protected void create() {
        this.depth2PassList = new Int2ObjectOpenHashMap<>();
    }

    @Override
    protected void get() {
        this.modelManager = get(ModelManager.class);
        this.windowInstance = internal.getWindowInstance();
    }

    @Override
    protected void awake() {
        GLSLUtility.enableDepth();
    }

    // Render Management \\

    public void draw() {

        // Set viewport
        GLSLUtility.setViewport(
                windowInstance.getWidth(),
                windowInstance.getHeight());

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
                ObjectArrayList<ProcessingPassHandle> passList = depth2PassList.get(depth);

                if (passList != null) {
                    for (ProcessingPassHandle pass : passList) {
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

    private void drawProcessingPass(ProcessingPassHandle pass) {

        ModelHandle modelHandle = pass.getModelHandle();
        Material material = pass.getMaterial();
        Shader shader = material.shader;

        // Disable depth testing for full-screen passes
        GLSLUtility.disableDepth();

        // Bind shader
        GLSLUtility.useShader(shader.shaderHandle);

        // Bind VAO
        GLSLUtility.bindVAO(modelHandle.getVaoHandle());

        // Draw
        GLSLUtility.drawElements(modelHandle.getIndexCount());

        // Unbind
        GLSLUtility.unbindVAO();

        // Re-enable depth for subsequent passes
        GLSLUtility.enableDepth();
    }

    // Accessible \\

    public void pushPass(ProcessingPassHandle pass, int depth) {

        ObjectArrayList<ProcessingPassHandle> passList = depth2PassList.computeIfAbsent(
                depth,
                k -> new ObjectArrayList<>());

        passList.add(pass);
    }

    public void pullPass(ProcessingPassHandle pass, int depth) {
        ObjectArrayList<ProcessingPassHandle> passList = depth2PassList.get(depth);

        if (passList == null)
            return;

        passList.remove(pass);

        // Clean up empty depth levels
        if (passList.isEmpty())
            depth2PassList.remove(depth);
    }

    public void pullPass(ProcessingPassHandle pass) {
        // Search all depths for this pass
        var iterator = depth2PassList.int2ObjectEntrySet().fastIterator();

        while (iterator.hasNext()) {
            var entry = iterator.next();
            ObjectArrayList<ProcessingPassHandle> passList = entry.getValue();

            if (passList.remove(pass)) {
                // Clean up empty depth levels
                if (passList.isEmpty())
                    iterator.remove();
                return;
            }
        }
    }
}