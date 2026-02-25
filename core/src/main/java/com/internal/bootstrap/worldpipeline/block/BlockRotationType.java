package com.internal.bootstrap.worldpipeline.block;

public enum BlockRotationType {
    NONE, // Rotation palette ignored entirely
    CARDINAL, // Only N/E/S/W — UP/DOWN facing falls back to default
    FULL, // All 6 directions × 4 spins = 24 orientations
    NATURAL_FULL
}