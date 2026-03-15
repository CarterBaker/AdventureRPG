package com.internal.core.kernel.syncconsumer;

import com.internal.core.engine.SyncContainerPackage;

@FunctionalInterface
public interface SyncStructConsumer<T extends SyncContainerPackage> {
    void accept(T instance);
}