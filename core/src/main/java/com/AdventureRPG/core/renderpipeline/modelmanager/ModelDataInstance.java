package com.AdventureRPG.core.renderpipeline.modelmanager;

import java.util.EmptyStackException;

import com.AdventureRPG.core.kernel.InstanceFrame;

abstract class ModelDataInstance extends InstanceFrame {

    // Internal
    protected final int modelID;

    // Base \\

    // Prevent models without ID instantiation
    ModelDataInstance() {

        modelID = 0;

        // TODO: Add my own error
        throw new EmptyStackException();
    }

    ModelDataInstance(int modelID) {

        // Internal
        this.modelID = modelID;
    }
}
