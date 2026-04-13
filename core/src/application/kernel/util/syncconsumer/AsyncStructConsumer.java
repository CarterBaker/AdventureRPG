package application.kernel.util.syncconsumer;

import engine.root.AsyncContainerPackage;

@FunctionalInterface
public interface AsyncStructConsumer<T extends AsyncContainerPackage> {
    void accept(T instance);
}