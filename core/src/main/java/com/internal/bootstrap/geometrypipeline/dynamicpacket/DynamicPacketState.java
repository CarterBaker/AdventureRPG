package com.internal.bootstrap.geometrypipeline.dynamicpacket;

public enum DynamicPacketState {

    /*
     * Lifecycle states for a DynamicPacketInstance. EMPTY means available for
     * generation, GENERATING means locked by a build thread, READY means
     * geometry is built and waiting to be consumed by the render system.
     */

    EMPTY,
    GENERATING,
    READY
}