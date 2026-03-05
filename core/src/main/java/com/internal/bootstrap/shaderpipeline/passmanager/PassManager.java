package com.internal.bootstrap.shaderpipeline.passmanager;

import com.internal.bootstrap.shaderpipeline.material.MaterialInstance;
import com.internal.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import com.internal.bootstrap.shaderpipeline.pass.PassInstance;
import com.internal.bootstrap.renderpipeline.rendersystem.RenderSystem;
import com.internal.core.engine.ManagerPackage;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

/*
 * Owns all compiled PassHandle objects. Delegates bootstrap loading to
 * InternalLoadManager. Passes are pushed into the render system by depth layer.
 * Cloning produces a PassInstance with a shared mesh reference and a deep-copied
 * MaterialInstance, mirroring the material cloning contract.
 */
public class PassManager extends ManagerPackage {

    // Internal
    private MaterialManager materialManager;
    private RenderSystem renderSystem;

    // Retrieval Mapping
    private Object2IntOpenHashMap<String> passName2PassID;
    private Int2ObjectOpenHashMap<PassHandle> passID2Pass;

    // Base \\

    @Override
    protected void create() {
        create(InternalLoadManager.class);
        this.passName2PassID = new Object2IntOpenHashMap<>();
        this.passID2Pass = new Int2ObjectOpenHashMap<>();
    }

    @Override
    protected void get() {
        this.materialManager = get(MaterialManager.class);
        this.renderSystem = get(RenderSystem.class);
    }

    // On-Demand Loading \\

    public void request(String passName) {
        ((InternalLoadManager) internalLoader).request(passName);
    }

    // Pass Management \\

    void addPass(PassHandle pass) {
        passName2PassID.put(pass.getPassName(), pass.getPassID());
        passID2Pass.put(pass.getPassID(), pass);
    }

    // Render Management \\

    public void pushPass(PassHandle pass, int depth) {
        renderSystem.pushRenderCall(pass.getModelInstance(), depth);
    }

    public void pushPass(PassInstance pass, int depth) {
        renderSystem.pushRenderCall(pass.getModelInstance(), depth);
    }

    // Accessible \\

    public PassInstance clonePass(int passID) {
        PassHandle original = passID2Pass.get(passID);
        if (original == null)
            throwException("Cannot clone pass — passID " + passID + " not found");
        MaterialInstance clonedMaterial = materialManager.cloneMaterial(
                original.getMaterial().getMaterialID());
        PassInstance instance = create(PassInstance.class);
        instance.constructor(
                original.getPassName(),
                original.getPassID(),
                original.getMeshHandle(),
                clonedMaterial);
        return instance;
    }

    public int getPassIDFromPassName(String passName) {
        if (!passName2PassID.containsKey(passName)) {
            request(passName);
        }
        return passName2PassID.getInt(passName);
    }

    public PassHandle getPassFromPassID(int passID) {
        return passID2Pass.get(passID);
    }
}