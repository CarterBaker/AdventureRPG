package com.internal.core.kernel.SyncConsumer;

import com.internal.core.engine.AsyncContainerPackage;

@FunctionalInterface
public interface AsyncStructConsumer<T extends AsyncContainerPackage> {
    void accept(T instance);
}