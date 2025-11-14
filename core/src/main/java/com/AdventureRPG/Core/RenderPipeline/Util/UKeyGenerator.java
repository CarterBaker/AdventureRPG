package com.AdventureRPG.Core.RenderPipeline.Util;

public final class UKeyGenerator {

    private static int counter = 0;

    private UKeyGenerator() {
    } // Prevent instantiation

    public static synchronized int generate() {
        int time = (int) System.nanoTime();
        int c = counter++;
        return time ^ (c * 0x9E3779B9);
    }
}
