package application.core.kernel.syncconsumer;

import application.core.engine.AsyncContainerPackage;

@FunctionalInterface
public interface AsyncStructConsumer<T extends AsyncContainerPackage> {
    void accept(T instance);
}