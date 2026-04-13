package application.kernel.threadpipeline.syncconsumer;

import engine.root.AsyncContainerPackage;

@FunctionalInterface
public interface AsyncStructConsumer<T extends AsyncContainerPackage> {
    void accept(T instance);
}