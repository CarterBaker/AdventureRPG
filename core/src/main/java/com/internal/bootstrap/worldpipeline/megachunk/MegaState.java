package com.internal.bootstrap.worldpipeline.megachunk;

public enum MegaState {
    UNINITIALIZED, // Just created, no chunks yet
    PARTIAL, // Some chunks batched, waiting for more
    NEEDS_MERGE, // All chunks present, needs CPU merge
    MERGING, // Worker thread is merging geometry
    MERGED, // CPU merge complete, ready for GPU upload
    UPLOADED; // Uploaded to GPU, ready to render
}