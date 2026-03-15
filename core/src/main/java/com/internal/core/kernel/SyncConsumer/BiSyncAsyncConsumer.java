package com.internal.core.kernel.syncconsumer;

import com.internal.core.engine.AsyncContainerPackage;
import com.internal.core.engine.SyncContainerPackage;

@FunctionalInterface
public interface BiSyncAsyncConsumer<T extends AsyncContainerPackage, S extends SyncContainerPackage> {
    void accept(T asyncInstance, S syncInstance);
}