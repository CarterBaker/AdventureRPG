package application.core.kernel.syncconsumer;

import application.core.engine.AsyncContainerPackage;

@FunctionalInterface
public interface AsyncStructConsumerMulti {
    void accept(AsyncContainerPackage[] instances);
}