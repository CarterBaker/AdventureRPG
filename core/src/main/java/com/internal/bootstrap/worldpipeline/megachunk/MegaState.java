package com.internal.bootstrap.worldpipeline.megachunk;

import com.internal.bootstrap.worldpipeline.worldrendersystem.RenderOperation;

public enum MegaState {

    UNINITIALIZED(RenderOperation.NONE),

    PARTIAL(RenderOperation.NONE),

    NEEDS_MERGE_DATA(RenderOperation.NONE),
    MERGING(RenderOperation.NONE),

    COMPLETE(RenderOperation.NEEDS_BATCH_RENDER),
    RENDERING_BATCHED(RenderOperation.HAS_BATCH_RENDER),

    ACTIVE(RenderOperation.NEEDS_INDIVIDUAL_RENDER),
    RENDERING_INDIVIDUAL(RenderOperation.HAS_INDIVIDUAL_RENDER);

    // Internal
    private final RenderOperation renderOperation;

    // Internal \\

    MegaState(RenderOperation renderOperation) {

        // Internal
        this.renderOperation = renderOperation;
    }

    // Accessible \\

    public RenderOperation getRenderOperation() {
        return renderOperation;
    }
}
