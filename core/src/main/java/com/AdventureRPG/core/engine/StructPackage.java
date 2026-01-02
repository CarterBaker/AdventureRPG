package com.AdventureRPG.core.engine;

public abstract class StructPackage extends EngineUtility {

    /*
     * Struct Packages are used mainly for bypassing constructor
     * Timing issues in the engine itself. Intended to extend
     * sub-classes within other classes themselves to aide in
     * assigning public final variables so that constructors can
     * remain easily accessible from anywhere in the internal
     * engine.
     */
}
