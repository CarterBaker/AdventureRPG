package application.kernel.util.syncconsumer;

import engine.root.AsyncContainerPackage;

@FunctionalInterface
public interface AsyncStructConsumerMulti {
    void accept(AsyncContainerPackage[] instances);
}