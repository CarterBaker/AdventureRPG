package program.core.kernel.syncconsumer;

import program.core.engine.AsyncContainerPackage;

@FunctionalInterface
public interface AsyncStructConsumerMulti {
    void accept(AsyncContainerPackage[] instances);
}