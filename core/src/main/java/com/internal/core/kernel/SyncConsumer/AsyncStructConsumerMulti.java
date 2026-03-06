package com.internal.core.kernel.SyncConsumer;

import com.internal.core.engine.AsyncContainerPackage;

@FunctionalInterface
public interface AsyncStructConsumerMulti {
    void accept(AsyncContainerPackage[] instances);
}