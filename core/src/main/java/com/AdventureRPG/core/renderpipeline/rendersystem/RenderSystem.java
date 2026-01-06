package com.AdventureRPG.core.renderpipeline.rendersystem;

import com.AdventureRPG.core.engine.SystemPackage;
import com.AdventureRPG.core.engine.WindowInstance;
import com.AdventureRPG.core.geometrypipeline.modelmanager.ModelHandle;
import com.AdventureRPG.core.geometrypipeline.modelmanager.ModelManager;
import com.AdventureRPG.core.shaderpipeline.materialmanager.MaterialHandle;
import com.AdventureRPG.core.shaderpipeline.passmanager.PassHandle;
import com.AdventureRPG.core.shaderpipeline.shadermanager.ShaderHandle;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class RenderSystem extends SystemPackage {

    // Internal
    private ModelManager modelManager;
    private WindowInstance windowInstance;

    // Render Queue: depth -> ordered list of passes
    private Int2ObjectOpenHashMap<ObjectArrayList<PassHandle>> depth2PassList;

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
                ObjectArrayList<PassHandle> passList = depth2PassList.get(depth);

                if (passList != null) {
                    for (PassHandle pass : passList) {
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

    private void drawProcessingPass(PassHandle pass) {

        ModelHandle modelHandle = pass.getModelHandle();
        MaterialHandle material = pass.getMaterial();
        ShaderHandle shaderHandle = material.getShaderHandle();

        // Disable depth testing for full-screen passes
        GLSLUtility.disableDepth();

        // Bind shader
        GLSLUtility.useShader(shaderHandle.getShaderHandle());

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

    public void pushPass(PassHandle pass, int depth) {

        ObjectArrayList<PassHandle> passList = depth2PassList.computeIfAbsent(
                depth,
                k -> new ObjectArrayList<>());

        passList.add(pass);
    }

    public void pullPass(PassHandle pass, int depth) {
        ObjectArrayList<PassHandle> passList = depth2PassList.get(depth);

        if (passList == null)
            return;

        passList.remove(pass);

        // Clean up empty depth levels
        if (passList.isEmpty())
            depth2PassList.remove(depth);
    }

    public void pullPass(PassHandle pass) {
        // Search all depths for this pass
        var iterator = depth2PassList.int2ObjectEntrySet().fastIterator();

        while (iterator.hasNext()) {
            var entry = iterator.next();
            ObjectArrayList<PassHandle> passList = entry.getValue();

            if (passList.remove(pass)) {
                // Clean up empty depth levels
                if (passList.isEmpty())
                    iterator.remove();
                return;
            }
        }
    }
}