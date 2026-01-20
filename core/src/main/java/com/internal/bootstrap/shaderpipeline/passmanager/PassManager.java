package com.internal.bootstrap.shaderpipeline.passmanager;

import com.internal.bootstrap.renderpipeline.rendersystem.RenderSystem;
import com.internal.core.engine.ManagerPackage;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

public class PassManager extends ManagerPackage {

    // Internal
    private InternalLoadManager internalLoadManager;
    private RenderSystem renderSystem;

    // Retrieval Mapping
    private Object2IntOpenHashMap<String> passName2PassID;
    private Int2ObjectOpenHashMap<PassHandle> passID2Pass;

    // Base \\

    @Override
    protected void create() {

        // Internal
        this.internalLoadManager = create(InternalLoadManager.class);

        // Retrieval Mapping
        this.passName2PassID = new Object2IntOpenHashMap<>();
        this.passID2Pass = new Int2ObjectOpenHashMap<>();
    }

    @Override
    protected void get() {

        // Internal
        this.renderSystem = get(RenderSystem.class);
    }

    @Override
    protected void awake() {
        compilePasses();
    }

    @Override
    protected void release() {
        internalLoadManager = release(InternalLoadManager.class);
    }

    // Render Management \\

    public void pushPass(PassHandle pass, int depth) {
        renderSystem.pushRenderCall(pass.getModelHandle(), depth);
    }

    // Pass Management \\

    private void compilePasses() {
        internalLoadManager.loadPasses();
    }

    void addPass(PassHandle pass) {
        passName2PassID.put(pass.getPassName(), pass.getPassID());
        passID2Pass.put(pass.getPassID(), pass);
    }

    // Accessible \\

    public int getPassIDFromPassName(String passName) {
        return passName2PassID.getInt(passName);
    }

    public PassHandle getPassFromPassID(int passID) {
        return passID2Pass.get(passID);
    }
}