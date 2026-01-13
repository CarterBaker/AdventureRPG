package com.internal.core.engine;

public abstract class StructPackage extends EngineUtility {

    /*
     * StructPackages are intended to serve as long-lived data containers
     * that are independent of engine timing rules, such as handles or
     * data packages. They exist solely to hold and transfer data and may
     * be used anywhere persistent information is required throughout the
     * game's lifecycle. StructPackages do not encapsulate behavior; their
     * role is to provide simple, accessible structures for durable data.
     */
}
