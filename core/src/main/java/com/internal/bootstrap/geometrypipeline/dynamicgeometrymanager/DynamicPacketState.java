package com.internal.bootstrap.geometrypipeline.dynamicgeometrymanager;

public enum DynamicPacketState {
    EMPTY,
    NEEDS_VERT_DATA,
    GENERATING_VERT_DATA,
    HAS_VERT_DATA,
    UPLOADED_TO_GPU,
    UPLOADED_TO_RENDERER
}
