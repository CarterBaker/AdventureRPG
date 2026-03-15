package com.internal.core.kernel.syncconsumer;

import com.internal.core.engine.AsyncContainerPackage;

@FunctionalInterface
public interface AsyncStructConsumerMulti {
    void accept(AsyncContainerPackage[] instances);
}