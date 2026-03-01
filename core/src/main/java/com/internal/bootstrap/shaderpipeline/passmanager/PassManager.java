package com.internal.bootstrap.shaderpipeline.passmanager;

import com.internal.bootstrap.renderpipeline.rendersystem.RenderSystem;
import com.internal.core.engine.ManagerPackage;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

/*
 * Owns all compiled PassHandle objects. Delegates bootstrap loading to
 * InternalLoadManager. Passes are pushed into the render system by depth layer.
 */
public class PassManager extends ManagerPackage {

    // Internal
    private InternalLoadManager internalLoadManager;
    private RenderSystem renderSystem;

    // Retrieval Mapping
    private Object2IntOpenHashMap<String> passName2PassID;
    private Int2ObjectOpenHashMap<PassHandle> passID2Pass;

    // Internal \\

    @Override
    protected void create() {
        this.internalLoadManager = create(InternalLoadManager.class);
        this.passName2PassID = new Object2IntOpenHashMap<>();
        this.passID2Pass = new Int2ObjectOpenHashMap<>();
    }

    @Override
    protected void get() {
        this.renderSystem = get(RenderSystem.class);
    }

    @Override
    protected void awake() {
        internalLoadManager.loadPasses();
    }

    @Override
    protected void release() {
        this.internalLoadManager = release(InternalLoadManager.class);
    }

    // Render Management \\

    public void pushPass(PassHandle pass, int depth) {
        renderSystem.pushRenderCall(pass.getModelHandle(), depth);
    }

    // Pass Management \\

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