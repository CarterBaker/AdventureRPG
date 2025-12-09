package com.AdventureRPG.core.shaderpipeline.passmanager;

import com.AdventureRPG.core.kernel.ManagerFrame;
import com.AdventureRPG.core.shaderpipeline.processingpass.ProcessingPass;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

public class PassManager extends ManagerFrame {

    // Internal
    private InternalLoadManager internalLoadManager;

    // Retrieval Mapping
    private Object2IntOpenHashMap<String> passName2PassID;
    private Int2ObjectOpenHashMap<ProcessingPass> passID2Pass;

    // Base \\

    @Override
    protected void create() {

        // Internal
        this.internalLoadManager = (InternalLoadManager) register(new InternalLoadManager());

        // Retrieval Mapping
        this.passName2PassID = new Object2IntOpenHashMap<>();
        this.passID2Pass = new Int2ObjectOpenHashMap<>();
    }

    @Override
    protected void awake() {
        compilePasses();
    }

    @Override
    protected void freeMemory() {
        internalLoadManager = (InternalLoadManager) release(internalLoadManager);
    }

    // Pass Management \\

    private void compilePasses() {
        internalLoadManager.loadPasses();
    }

    void addPass(ProcessingPass pass) {
        passName2PassID.put(pass.passName, pass.passID);
        passID2Pass.put(pass.passID, pass);
    }

    // Accessible \\

    public int getPassIDFromPassName(String passName) {
        return passName2PassID.getInt(passName);
    }

    public ProcessingPass getPassFromPassID(int passID) {
        return passID2Pass.get(passID);
    }
}